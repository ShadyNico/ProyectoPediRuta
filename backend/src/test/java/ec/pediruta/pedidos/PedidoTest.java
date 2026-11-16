package ec.pediruta.pedidos;

import ec.pediruta.inventario.Producto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PedidoTest {

    private static Producto producto(String nombre, String precio, String peso) {
        return new Producto(nombre, "", "Bebidas", new BigDecimal(precio), new BigDecimal(peso), 100);
    }

    @Test
    void calculaTotalYPeso() {
        Pedido pedido = new Pedido("Tienda La Esquina", "Av. Quito");
        pedido.agregarItem(producto("Agua", "6.50", "12"), 2);
        pedido.agregarItem(producto("Cola", "11.40", "19"), 1);

        assertThat(pedido.getTotal()).isEqualByComparingTo("24.40");
        assertThat(pedido.getPesoKg()).isEqualByComparingTo("43");
        assertThat(pedido.getItems()).hasSize(2);
    }

    @Test
    void empiezaPendienteYAvanzaEnOrden() {
        Pedido pedido = new Pedido("Tienda", "Dirección");
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PENDIENTE);

        pedido.cambiarEstado(EstadoPedido.PREPARANDO);
        pedido.cambiarEstado(EstadoPedido.LISTO);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.LISTO);
    }

    @Test
    void noSePuedeSaltarEstados() {
        Pedido pedido = new Pedido("Tienda", "Dirección");
        assertThatThrownBy(() -> pedido.cambiarEstado(EstadoPedido.ENTREGADO))
                .isInstanceOf(TransicionInvalidaException.class);
    }

    @Test
    void soloSeCancelaMientrasEstaPendiente() {
        Pedido pedido = new Pedido("Tienda", "Dirección");
        pedido.cambiarEstado(EstadoPedido.PREPARANDO);
        assertThatThrownBy(() -> pedido.cambiarEstado(EstadoPedido.CANCELADO))
                .isInstanceOf(TransicionInvalidaException.class);
    }
}
