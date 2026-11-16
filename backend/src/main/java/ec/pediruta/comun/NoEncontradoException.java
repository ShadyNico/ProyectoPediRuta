package ec.pediruta.comun;

import org.springframework.http.HttpStatus;

public class NoEncontradoException extends NegocioException {

    public NoEncontradoException(String recurso, Object id) {
        super(HttpStatus.NOT_FOUND, recurso + " " + id + " no existe");
    }
}
