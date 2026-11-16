package ec.pediruta.comun;

import org.springframework.http.HttpStatus;

/**
 * Error esperado del negocio (no es un fallo del programa).
 * Cada subclase define qué código HTTP recibe el cliente.
 */
public abstract class NegocioException extends RuntimeException {

    private final HttpStatus estadoHttp;

    protected NegocioException(HttpStatus estadoHttp, String mensaje) {
        super(mensaje);
        this.estadoHttp = estadoHttp;
    }

    public HttpStatus getEstadoHttp() {
        return estadoHttp;
    }
}
