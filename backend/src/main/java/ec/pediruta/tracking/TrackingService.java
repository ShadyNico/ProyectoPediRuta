package ec.pediruta.tracking;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Guarda la última posición de cada camión y avisa a quien escuche.
 *
 * Las posiciones viven en memoria (ConcurrentHashMap, seguro con muchos hilos):
 * según el teorema CAP, el rastreo prioriza Disponibilidad. Si se pierde una
 * posición, la siguiente llega en 1 segundo y la corrige.
 */
@Service
public class TrackingService {

    private final Map<String, Posicion> ultimas = new ConcurrentHashMap<>();
    private final ApplicationEventPublisher eventos;

    private final AtomicLong mensajes = new AtomicLong();
    private final AtomicLong conLatencia = new AtomicLong();
    private final AtomicLong sumaLatenciaMs = new AtomicLong();
    private final AtomicLong maxLatenciaMs = new AtomicLong();

    public TrackingService(ApplicationEventPublisher eventos) {
        this.eventos = eventos;
    }

    public Posicion registrar(Posicion posicion) {
        ultimas.put(posicion.placa(), posicion);
        mensajes.incrementAndGet();
        if (posicion.enviadoEnMs() != null) {
            long latencia = Math.max(0, posicion.recibidoEnMs() - posicion.enviadoEnMs());
            conLatencia.incrementAndGet();
            sumaLatenciaMs.addAndGet(latencia);
            maxLatenciaMs.accumulateAndGet(latencia, Math::max);
        }
        eventos.publishEvent(new PosicionRecibida(posicion));
        return posicion;
    }

    public List<Posicion> ultimasPosiciones() {
        return ultimas.values().stream()
                .sorted(Comparator.comparing(Posicion::placa))
                .toList();
    }

    public Metricas metricas() {
        long n = conLatencia.get();
        double promedio = n == 0 ? 0 : (double) sumaLatenciaMs.get() / n;
        return new Metricas(mensajes.get(), ultimas.size(), Math.round(promedio * 10) / 10.0, maxLatenciaMs.get());
    }

    public void reiniciarMetricas() {
        mensajes.set(0);
        conLatencia.set(0);
        sumaLatenciaMs.set(0);
        maxLatenciaMs.set(0);
    }

    /**
     * @param mensajes           posiciones recibidas
     * @param camiones           camiones distintos vistos
     * @param latenciaPromedioMs tiempo medio desde que el dispositivo envía hasta que el servidor recibe
     * @param latenciaMaxMs      peor caso de esa latencia
     */
    public record Metricas(long mensajes, int camiones, double latenciaPromedioMs, long latenciaMaxMs) {
    }
}
