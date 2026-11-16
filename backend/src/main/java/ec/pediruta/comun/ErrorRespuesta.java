package ec.pediruta.comun;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Formato único de error que recibe el frontend.
 *
 * @param codigo   código HTTP (400, 404, 409...)
 * @param mensaje  texto para mostrar al usuario
 * @param detalles errores por campo, cuando la validación falla
 * @param hora     momento del error
 */
public record ErrorRespuesta(int codigo, String mensaje, List<String> detalles, OffsetDateTime hora) {

    public static ErrorRespuesta de(int codigo, String mensaje) {
        return new ErrorRespuesta(codigo, mensaje, List.of(), OffsetDateTime.now());
    }

    public static ErrorRespuesta de(int codigo, String mensaje, List<String> detalles) {
        return new ErrorRespuesta(codigo, mensaje, detalles, OffsetDateTime.now());
    }
}
