package ec.pediruta.comun;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Convierte cualquier error en una respuesta JSON clara (tema 2.1.4).
 * Así el frontend siempre recibe {codigo, mensaje} y nunca una página de error.
 */
@Slf4j
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorRespuesta> negocio(NegocioException e) {
        log.info("Regla de negocio: {}", e.getMessage());
        return ResponseEntity.status(e.getEstadoHttp())
                .body(ErrorRespuesta.de(e.getEstadoHttp().value(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException e) {
        List<String> detalles = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorRespuesta.de(400, "Hay datos incompletos o inválidos", detalles));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> jsonInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(ErrorRespuesta.de(400, "El formato de los datos enviados no es válido"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> inesperado(Exception e) {
        log.error("Error inesperado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorRespuesta.de(500, "Ocurrió un error inesperado en el servidor"));
    }
}
