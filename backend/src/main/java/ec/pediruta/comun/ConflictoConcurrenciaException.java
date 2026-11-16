package ec.pediruta.comun;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando, tras varios reintentos, otra petición sigue modificando
 * el mismo dato al mismo tiempo (bloqueo optimista con @Version).
 */
public class ConflictoConcurrenciaException extends NegocioException {

    public ConflictoConcurrenciaException() {
        super(HttpStatus.CONFLICT,
                "Muchas personas están comprando lo mismo en este momento. Intenta de nuevo.");
    }
}
