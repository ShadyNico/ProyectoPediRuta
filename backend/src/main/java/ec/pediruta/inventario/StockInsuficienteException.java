package ec.pediruta.inventario;

import ec.pediruta.comun.NegocioException;
import org.springframework.http.HttpStatus;

public class StockInsuficienteException extends NegocioException {

    public StockInsuficienteException(String producto, int disponible, int pedido) {
        super(HttpStatus.CONFLICT, disponible == 0
                ? "No queda stock de " + producto
                : "Solo quedan " + disponible + " de " + producto + " y pediste " + pedido);
    }
}
