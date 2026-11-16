package ec.pediruta.pedidos;

/**
 * Evento interno: un pedido se creó o cambió de estado.
 * Se entrega a los oyentes solo después de que la transacción se confirma.
 */
public record PedidoCambiado(PedidoDto pedido) {
}
