package ec.pediruta.tracking;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba con un socket TCP real: abre el servidor en un puerto libre,
 * se conecta como si fuera el simulador y conversa con él.
 */
class ServidorGpsTcpTest {

    private TrackingService tracking;
    private ServidorGpsTcp servidor;

    @BeforeEach
    void iniciar() {
        tracking = new TrackingService(evento -> { });
        servidor = new ServidorGpsTcp(tracking, 0, 5000); // puerto 0 = cualquiera libre
        servidor.start();
    }

    @AfterEach
    void detener() {
        servidor.stop();
    }

    @Test
    void recibePosicionesYRespondeOkOError() throws Exception {
        try (Socket socket = new Socket("localhost", servidor.puertoReal());
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            salida.print("PBA-1234;-0.2531;-79.1720;35;" + System.currentTimeMillis() + "\n");
            salida.flush();
            assertThat(entrada.readLine()).isEqualTo("OK");

            salida.print("esto no es una posicion\n");
            salida.flush();
            assertThat(entrada.readLine()).startsWith("ERROR");

            salida.print("PBA-1234;-0.2540;-79.1710\n");
            salida.flush();
            assertThat(entrada.readLine()).isEqualTo("OK");
        }

        assertThat(tracking.ultimasPosiciones()).singleElement()
                .satisfies(p -> assertThat(p.lat()).isEqualTo(-0.2540));
        assertThat(tracking.metricas().mensajes()).isEqualTo(2);
    }

    @Test
    void atiendeVariosCamionesALaVez() throws Exception {
        int camiones = 20;
        Thread[] hilos = new Thread[camiones];
        for (int i = 0; i < camiones; i++) {
            String placa = String.format("SIM-%03d", i);
            hilos[i] = Thread.ofVirtual().start(() -> {
                try (Socket socket = new Socket("localhost", servidor.puertoReal());
                     PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                     BufferedReader entrada = new BufferedReader(
                             new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                    for (int n = 0; n < 5; n++) {
                        salida.print(placa + ";-0.25;-79.17;30\n");
                        salida.flush();
                        entrada.readLine();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        for (Thread hilo : hilos) {
            hilo.join();
        }

        assertThat(tracking.ultimasPosiciones()).hasSize(camiones);
        assertThat(tracking.metricas().mensajes()).isEqualTo(camiones * 5L);
    }
}
