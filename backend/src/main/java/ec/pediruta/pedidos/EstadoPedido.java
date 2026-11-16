package ec.pediruta.pedidos;

/**
 * Ciclo de vida de un pedido:
 *
 * PENDIENTE → PREPARANDO → LISTO → EN_CAMINO → ENTREGADO
 *     └→ CANCELADO
 */
public enum EstadoPedido {
    PENDIENTE,
    PREPARANDO,
    LISTO,
    EN_CAMINO,
    ENTREGADO,
    CANCELADO;

    public boolean puedePasarA(EstadoPedido nuevo) {
        return switch (this) {
            case PENDIENTE -> nuevo == PREPARANDO || nuevo == CANCELADO;
            case PREPARANDO -> nuevo == LISTO;
            case LISTO -> nuevo == EN_CAMINO;
            case EN_CAMINO -> nuevo == ENTREGADO;
            case ENTREGADO, CANCELADO -> false;
        };
    }
}
