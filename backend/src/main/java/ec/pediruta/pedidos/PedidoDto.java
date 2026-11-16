package ec.pediruta.pedidos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PedidoDto(
        Long id,
        String cliente,
        String direccion,
        EstadoPedido estado,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn,
        BigDecimal total,
        BigDecimal pesoKg,
        List<Item> items) {

    public record Item(Long productoId, String nombre, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {
    }

    public static PedidoDto de(Pedido p) {
        List<Item> items = p.getItems().stream()
                .map(i -> new Item(i.getProductoId(), i.getNombreProducto(), i.getCantidad(),
                        i.getPrecioUnitario(), i.subtotal()))
                .toList();
        return new PedidoDto(p.getId(), p.getCliente(), p.getDireccion(), p.getEstado(),
                p.getCreadoEn(), p.getActualizadoEn(), p.getTotal(), p.getPesoKg(), items);
    }
}
