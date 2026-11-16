package ec.pediruta.salud;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * Historia H01: comprobar que backend, base de datos y PostGIS están conectados.
 *
 * Ilustra la falacia "la red es confiable": si la base no responde, el endpoint
 * NO devuelve un error 500, sino un estado claro ("sin conexión") que el
 * frontend puede mostrar al usuario.
 */
@Slf4j
@RestController
@RequestMapping("/api/salud")
public class SaludController {

    static final String SERVICIO = "pediruta-backend";

    private final JdbcTemplate jdbc;

    public SaludController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public EstadoSistema consultar() {
        try {
            String version = jdbc.queryForObject("SELECT postgis_version()", String.class);
            return new EstadoSistema(SERVICIO, OffsetDateTime.now(), "conectada", version, null);
        } catch (DataAccessException e) {
            log.warn("La base de datos no responde: {}", e.getMostSpecificCause().getMessage());
            return new EstadoSistema(SERVICIO, OffsetDateTime.now(), "sin conexión", null,
                    "No se pudo consultar PostgreSQL/PostGIS. ¿Está levantado con docker compose up -d?");
        }
    }
}
