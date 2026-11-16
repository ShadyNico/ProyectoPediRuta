package ec.pediruta.comun;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Datos para el experimento de concurrencia (scripts/prueba-concurrencia.ps1).
 */
@RestController
@RequestMapping("/api/metricas/concurrencia")
public class MetricasController {

    private final ReintentoOptimista reintento;

    public MetricasController(ReintentoOptimista reintento) {
        this.reintento = reintento;
    }

    @GetMapping
    public ReintentoOptimista.Metricas consultar() {
        return reintento.metricas();
    }

    @DeleteMapping
    public ResponseEntity<Void> reiniciar() {
        reintento.reiniciar();
        return ResponseEntity.noContent().build();
    }
}
