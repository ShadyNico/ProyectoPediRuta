package ec.pediruta.pedidos;

import ec.pediruta.comun.NegocioException;
import org.springframework.http.HttpStatus;

public class TransicionInvalidaException extends NegocioException {

    public TransicionInvalidaException(Long pedidoId, EstadoPedido actual, EstadoPedido nuevo) {
        super(HttpStatus.CONFLICT,
                "El pedido #" + pedidoId + " está " + actual + " y no puede pasar a " + nuevo);
    }
}
