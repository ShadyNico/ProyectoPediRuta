package ec.pediruta.tracking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProtocoloGpsTest {

    @Test
    void leeUnaLineaCompleta() {
        Posicion p = ProtocoloGps.leerLinea("PBA-1234;-0.2531;-79.1720;38.46;1763322000123", "simulador");

        assertThat(p.placa()).isEqualTo("PBA-1234");
        assertThat(p.lat()).isEqualTo(-0.2531);
        assertThat(p.lon()).isEqualTo(-79.1720);
        assertThat(p.velocidadKmh()).isEqualTo(38.5);
        assertThat(p.enviadoEnMs()).isEqualTo(1763322000123L);
        assertThat(p.origen()).isEqualTo("simulador");
    }

    @Test
    void velocidadYMarcaSonOpcionales() {
        Posicion p = ProtocoloGps.leerLinea("pba-1234;-0.25;-79.17", "simulador");

        assertThat(p.placa()).isEqualTo("PBA-1234");
        assertThat(p.velocidadKmh()).isZero();
        assertThat(p.enviadoEnMs()).isNull();
    }

    @Test
    void rechazaLineasMalFormadas() {
        assertThatThrownBy(() -> ProtocoloGps.leerLinea("", "x")).hasMessageContaining("vacía");
        assertThatThrownBy(() -> ProtocoloGps.leerLinea("PBA-1234;-0.25", "x")).hasMessageContaining("formato");
        assertThatThrownBy(() -> ProtocoloGps.leerLinea("PBA-1234;abc;-79.17", "x")).hasMessageContaining("latitud");
        assertThatThrownBy(() -> ProtocoloGps.leerLinea("1234;-0.25;-79.17", "x")).hasMessageContaining("placa");
        assertThatThrownBy(() -> ProtocoloGps.leerLinea("PBA-1234;-95;-79.17", "x")).hasMessageContaining("latitud");
        assertThatThrownBy(() -> ProtocoloGps.leerLinea("PBA-1234;-0.25;-79.17;500", "x")).hasMessageContaining("velocidad");
    }
}
