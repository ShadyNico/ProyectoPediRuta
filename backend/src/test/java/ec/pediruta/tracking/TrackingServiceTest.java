package ec.pediruta.tracking;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrackingServiceTest {

    private final List<Object> eventos = new ArrayList<>();
    private final TrackingService tracking = new TrackingService(eventos::add);

    private static Posicion posicion(String placa, double lat, long recibido, Long enviado) {
        return new Posicion(placa, lat, -79.17, 30, recibido, enviado, "simulador");
    }

    @Test
    void guardaSoloLaUltimaPosicionDeCadaCamion() {
        tracking.registrar(posicion("PBA-1234", -0.25, 1000, null));
        tracking.registrar(posicion("PBA-1234", -0.26, 2000, null));
        tracking.registrar(posicion("PBD-9012", -0.27, 2000, null));

        assertThat(tracking.ultimasPosiciones()).hasSize(2);
        assertThat(tracking.ultimasPosiciones().get(0).lat()).isEqualTo(-0.26);
    }

    @Test
    void avisaCadaPosicionComoEvento() {
        tracking.registrar(posicion("PBA-1234", -0.25, 1000, null));

        assertThat(eventos).hasSize(1).first().isInstanceOf(PosicionRecibida.class);
    }

    @Test
    void calculaLatencia() {
        tracking.registrar(posicion("PBA-1234", -0.25, 1010, 1000L)); // 10 ms
        tracking.registrar(posicion("PBA-1234", -0.25, 2030, 2000L)); // 30 ms

        TrackingService.Metricas m = tracking.metricas();
        assertThat(m.mensajes()).isEqualTo(2);
        assertThat(m.latenciaPromedioMs()).isEqualTo(20.0);
        assertThat(m.latenciaMaxMs()).isEqualTo(30);
    }
}
