package ec.pediruta.inventario;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductoTest {

    private Producto jugo(int stock) {
        return new Producto("Jugo de naranja 1 L", "Caja x 12", "Bebidas",
                new BigDecimal("14.20"), new BigDecimal("13"), stock);
    }

    @Test
    void descontarRestaElStock() {
        Producto p = jugo(8);
        p.descontar(3);
        assertThat(p.getStock()).isEqualTo(5);
    }

    @Test
    void noSePuedeVenderMasDeLoQueHay() {
        Producto p = jugo(2);
        assertThatThrownBy(() -> p.descontar(3))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("Solo quedan 2");
        assertThat(p.getStock()).isEqualTo(2);
    }

    @Test
    void sinStockElMensajeLoDiceClaro() {
        assertThatThrownBy(() -> jugo(0).descontar(1))
                .hasMessage("No queda stock de Jugo de naranja 1 L");
    }

    @Test
    void cantidadCeroONegativaNoSeAcepta() {
        assertThatThrownBy(() -> jugo(5).descontar(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> jugo(5).devolver(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
