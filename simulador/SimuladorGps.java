import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PediRuta - Simulador de dispositivos GPS (Sprint 2, tema 1.3: sockets TCP).
 *
 * Cada camion es un hilo virtual con SU PROPIA conexion TCP al backend,
 * igual que si fueran aparatos GPS separados. Recorren rutas aproximadas
 * de Santo Domingo, ida y vuelta, y envian una posicion por segundo:
 *
 *     PBA-1234;-0.25310;-79.17200;40.0;1763322000123
 *
 * Si el backend se cae, cada camion reintenta conectarse solo
 * (falacia: "la red es confiable").
 *
 * No necesita compilarse ni Maven. Desde la carpeta del proyecto:
 *
 *     java simulador/SimuladorGps.java
 *     java simulador/SimuladorGps.java --camiones=10 --intervalo=500
 *
 * Opciones:
 *     --host=localhost   --puerto=9090   --camiones=3
 *     --intervalo=1000   (ms entre posiciones)
 *     --rapidez=5        (cuantas veces mas rapido que la realidad se mueven en el mapa)
 *
 * Nota: las rutas son puntos aproximados unidos en linea recta. En el Sprint 6
 * se reemplazan por rutas reales calculadas sobre las calles (A* / GraphHopper).
 */
public class SimuladorGps {

    record Punto(double lat, double lon) { }

    static final Punto BODEGA = new Punto(-0.2560, -79.1680);

    // Rutas aproximadas: bodega -> centro -> afueras (ida y vuelta)
    static final List<List<Punto>> RUTAS = List.of(
            // Norte, por la Av. Tsachila
            List.of(BODEGA, new Punto(-0.2532, -79.1718), new Punto(-0.2440, -79.1735),
                    new Punto(-0.2330, -79.1745), new Punto(-0.2250, -79.1700)),
            // Este, por la Av. Quito
            List.of(BODEGA, new Punto(-0.2525, -79.1640), new Punto(-0.2490, -79.1520),
                    new Punto(-0.2465, -79.1400), new Punto(-0.2440, -79.1300)),
            // Suroeste, por la Av. Quevedo
            List.of(BODEGA, new Punto(-0.2590, -79.1730), new Punto(-0.2680, -79.1800),
                    new Punto(-0.2780, -79.1880), new Punto(-0.2870, -79.1950)));

    static final String[] PLACAS = {"PBA-1234", "PBD-9012", "PBC-5678"};

    static final AtomicLong enviados = new AtomicLong();
    static final AtomicLong errores = new AtomicLong();
    static final AtomicLong reconexiones = new AtomicLong();

    public static void main(String[] args) throws Exception {
        Map<String, String> op = leerOpciones(args);
        String host = op.getOrDefault("host", "localhost");
        int puerto = Integer.parseInt(op.getOrDefault("puerto", "9090"));
        int camiones = Integer.parseInt(op.getOrDefault("camiones", "3"));
        int intervalo = Integer.parseInt(op.getOrDefault("intervalo", "1000"));
        double rapidez = Double.parseDouble(op.getOrDefault("rapidez", "5"));

        System.out.printf("Simulador GPS -> %s:%d | %d camiones | una posicion cada %d ms%n",
                host, puerto, camiones, intervalo);
        System.out.println("Ctrl + C para detener.");

        for (int i = 0; i < camiones; i++) {
            String placa = i < PLACAS.length ? PLACAS[i] : String.format("SIM-%03d", i + 1);
            List<Punto> recorrido = idaYVuelta(RUTAS.get(i % RUTAS.size()));
            double velocidadKmh = 30 + (i * 7) % 20;           // entre 30 y 49 km/h
            int inicio = (i / RUTAS.size()) * 7;                // reparte camiones de la misma ruta
            Thread.ofVirtual().name("camion-" + placa)
                    .start(() -> manejar(placa, recorrido, velocidadKmh, inicio, host, puerto, intervalo, rapidez));
        }

        while (true) {
            Thread.sleep(10_000);
            System.out.printf("[resumen] enviadas: %d | errores: %d | reconexiones: %d%n",
                    enviados.get(), errores.get(), reconexiones.get());
        }
    }

    /** Un camion: se conecta, envia posiciones y, si se cae la red, vuelve a intentar. */
    static void manejar(String placa, List<Punto> recorrido, double velocidadKmh, int inicio,
                        String host, int puerto, int intervaloMs, double rapidez) {
        double metrosPorPaso = velocidadKmh / 3.6 * (intervaloMs / 1000.0) * rapidez;
        List<Punto> pasos = interpolar(recorrido, metrosPorPaso);
        int indice = inicio % pasos.size();
        long espera = 1000;

        while (true) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, puerto), 3000);
                socket.setSoTimeout(5000);
                PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                System.out.println(placa + " conectado");
                espera = 1000;

                while (true) {
                    Punto p = pasos.get(indice);
                    String linea = String.format(Locale.ROOT, "%s;%.6f;%.6f;%.1f;%d",
                            placa, p.lat(), p.lon(), velocidadKmh, System.currentTimeMillis());
                    salida.print(linea + "\n");
                    salida.flush();
                    String respuesta = entrada.readLine();
                    if (respuesta == null) {
                        throw new IOException("el servidor cerro la conexion");
                    }
                    if (respuesta.startsWith("OK")) {
                        enviados.incrementAndGet();
                    } else {
                        errores.incrementAndGet();
                        System.out.println(placa + " -> " + respuesta);
                    }
                    indice = (indice + 1) % pasos.size();
                    Thread.sleep(intervaloMs);
                }
            } catch (IOException e) {
                reconexiones.incrementAndGet();
                System.out.printf("%s sin conexion (%s). Reintento en %d s%n", placa, e.getMessage(), espera / 1000);
                dormir(espera);
                espera = Math.min(espera * 2, 10_000); // espera creciente: 1, 2, 4, 8, 10 s
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    static List<Punto> idaYVuelta(List<Punto> ruta) {
        List<Punto> completa = new ArrayList<>(ruta);
        for (int i = ruta.size() - 2; i > 0; i--) {
            completa.add(ruta.get(i));
        }
        return completa;
    }

    /** Divide cada tramo en pasos de igual longitud para que el movimiento sea suave. */
    static List<Punto> interpolar(List<Punto> recorrido, double metrosPorPaso) {
        List<Punto> pasos = new ArrayList<>();
        for (int i = 0; i < recorrido.size(); i++) {
            Punto a = recorrido.get(i);
            Punto b = recorrido.get((i + 1) % recorrido.size());
            int n = Math.max(1, (int) Math.round(distanciaMetros(a, b) / metrosPorPaso));
            for (int k = 0; k < n; k++) {
                double t = (double) k / n;
                pasos.add(new Punto(a.lat() + (b.lat() - a.lat()) * t, a.lon() + (b.lon() - a.lon()) * t));
            }
        }
        return pasos;
    }

    /** Distancia sobre la Tierra (formula de Haversine). La misma se usara en A*. */
    static double distanciaMetros(Punto a, Punto b) {
        double r = 6_371_000;
        double dLat = Math.toRadians(b.lat() - a.lat());
        double dLon = Math.toRadians(b.lon() - a.lon());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(a.lat())) * Math.cos(Math.toRadians(b.lat()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(h));
    }

    static Map<String, String> leerOpciones(String[] args) {
        Map<String, String> op = new HashMap<>();
        for (String a : args) {
            if (a.startsWith("--") && a.contains("=")) {
                op.put(a.substring(2, a.indexOf('=')), a.substring(a.indexOf('=') + 1));
            }
        }
        return op;
    }

    static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
