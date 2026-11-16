package ec.pediruta.salud;

import java.time.OffsetDateTime;

/**
 * Respuesta de GET /api/salud.
 *
 * @param servicio      nombre del servicio que responde
 * @param hora          momento de la consulta
 * @param baseDeDatos   "conectada" o "sin conexión"
 * @param postgis       versión de PostGIS, o null si no hay conexión
 * @param detalle       mensaje para el usuario cuando algo falla
 */
public record EstadoSistema(
        String servicio,
        OffsetDateTime hora,
        String baseDeDatos,
        String postgis,
        String detalle) {
}
