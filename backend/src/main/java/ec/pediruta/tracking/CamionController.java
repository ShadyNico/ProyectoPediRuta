package ec.pediruta.tracking;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/camiones")
public class CamionController {

    private final TrackingService tracking;
    private final ServidorGpsTcp servidorGps;

    public CamionController(TrackingService tracking, ServidorGpsTcp servidorGps) {
        this.tracking = tracking;
        this.servidorGps = servidorGps;
    }

    /** Última posición de cada camión: el mapa la usa al abrirse. Después llegan por WebSocket. */
    @GetMapping("/posiciones")
    public List<Posicion> posiciones() {
        return tracking.ultimasPosiciones();
    }

    /** Datos del experimento: mensajes, latencia y conexiones TCP abiertas. */
    @GetMapping("/metricas")
    public Map<String, Object> metricas() {
        TrackingService.Metricas m = tracking.metricas();
        return Map.of(
                "mensajes", m.mensajes(),
                "camiones", m.camiones(),
                "latenciaPromedioMs", m.latenciaPromedioMs(),
                "latenciaMaxMs", m.latenciaMaxMs(),
                "conexionesTcp", servidorGps.conexionesActivas());
    }

    @DeleteMapping("/metricas")
    public ResponseEntity<Void> reiniciarMetricas() {
        tracking.reiniciarMetricas();
        return ResponseEntity.noContent().build();
    }
}
