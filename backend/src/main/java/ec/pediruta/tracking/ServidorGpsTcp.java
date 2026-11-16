package ec.pediruta.tracking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tema 1.3 – Sockets TCP.
 *
 * Servidor que escucha en el puerto 9090. Cada dispositivo GPS (o el simulador)
 * abre una conexión y envía una línea por posición; el servidor contesta "OK"
 * o "ERROR motivo" (ver {@link ProtocoloGps}).
 *
 * Concurrencia: cada conexión se atiende en su propio hilo virtual de Java 21,
 * así 100 camiones conectados no necesitan 100 hilos pesados del sistema.
 *
 * Se arranca y detiene junto con Spring (SmartLifecycle).
 */
@Slf4j
@Component
public class ServidorGpsTcp implements SmartLifecycle {

    private final TrackingService tracking;
    private final int puerto;
    private final int tiempoEsperaMs;
    private final AtomicInteger conexionesActivas = new AtomicInteger();

    private volatile boolean activo;
    private ServerSocket servidor;
    private ExecutorService hilos;

    public ServidorGpsTcp(TrackingService tracking,
                          @Value("${pediruta.gps.puerto:9090}") int puerto,
                          @Value("${pediruta.gps.tiempo-espera-ms:30000}") int tiempoEsperaMs) {
        this.tracking = tracking;
        this.puerto = puerto;
        this.tiempoEsperaMs = tiempoEsperaMs;
    }

    @Override
    public void start() {
        try {
            servidor = new ServerSocket(puerto);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo abrir el puerto GPS " + puerto
                    + ". ¿Hay otra instancia del backend encendida?", e);
        }
        hilos = Executors.newVirtualThreadPerTaskExecutor();
        activo = true;
        Thread.ofVirtual().name("gps-aceptador").start(this::aceptarConexiones);
        log.info("Servidor GPS TCP escuchando en el puerto {}", puertoReal());
    }

    private void aceptarConexiones() {
        while (activo) {
            try {
                Socket socket = servidor.accept();
                socket.setSoTimeout(tiempoEsperaMs);
                hilos.submit(() -> atender(socket));
            } catch (SocketException e) {
                if (activo) {
                    log.warn("Error aceptando conexión GPS: {}", e.getMessage());
                }
            } catch (IOException e) {
                log.warn("Error aceptando conexión GPS: {}", e.getMessage());
            }
        }
    }

    private void atender(Socket socket) {
        String remoto = socket.getRemoteSocketAddress().toString();
        int activas = conexionesActivas.incrementAndGet();
        log.info("Dispositivo GPS conectado desde {} ({} conectados)", remoto, activas);
        try (socket;
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
            String linea;
            while (activo && (linea = entrada.readLine()) != null) {
                try {
                    tracking.registrar(ProtocoloGps.leerLinea(linea, "simulador"));
                    salida.print("OK\n");
                } catch (IllegalArgumentException e) {
                    salida.print("ERROR " + e.getMessage() + "\n");
                }
                salida.flush();
            }
        } catch (SocketTimeoutException e) {
            log.info("Dispositivo {} sin enviar datos por {} ms: se cierra la conexión", remoto, tiempoEsperaMs);
        } catch (IOException e) {
            log.info("Conexión con {} perdida: {}", remoto, e.getMessage());
        } finally {
            log.info("Dispositivo GPS desconectado: {} ({} conectados)", remoto, conexionesActivas.decrementAndGet());
        }
    }

    @Override
    public void stop() {
        activo = false;
        try {
            if (servidor != null) {
                servidor.close();
            }
        } catch (IOException e) {
            log.debug("Error cerrando el servidor GPS", e);
        }
        if (hilos != null) {
            hilos.shutdownNow();
        }
        log.info("Servidor GPS TCP detenido");
    }

    @Override
    public boolean isRunning() {
        return activo;
    }

    /** Puerto en uso (útil en pruebas, donde se pide el puerto 0 = cualquiera libre). */
    public int puertoReal() {
        return servidor.getLocalPort();
    }

    public int conexionesActivas() {
        return conexionesActivas.get();
    }
}
