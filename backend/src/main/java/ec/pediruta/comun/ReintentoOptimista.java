package ec.pediruta.comun;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Tema 1.2 – Concurrencia con bloqueo optimista.
 *
 * Cada producto tiene una columna "version". Si dos pedidos leen el mismo
 * producto y ambos intentan guardarlo, la base de datos solo acepta al
 * primero; el segundo recibe un conflicto de versión.
 *
 * Esta clase repite la operación completa (una transacción nueva cada vez)
 * hasta {@value #MAX_INTENTOS} veces, esperando un tiempo al azar entre
 * intentos para que las peticiones no vuelvan a chocar al mismo tiempo.
 *
 * También cuenta los conflictos: son los datos del experimento de concurrencia.
 */
@Slf4j
@Component
public class ReintentoOptimista {

    static final int MAX_INTENTOS = 3;

    private final AtomicLong conflictos = new AtomicLong();
    private final AtomicLong recuperados = new AtomicLong();
    private final AtomicLong fallidos = new AtomicLong();

    public <T> T ejecutar(Supplier<T> operacion) {
        for (int intento = 1; ; intento++) {
            try {
                T resultado = operacion.get();
                if (intento > 1) {
                    recuperados.incrementAndGet();
                }
                return resultado;
            } catch (OptimisticLockingFailureException e) {
                conflictos.incrementAndGet();
                if (intento >= MAX_INTENTOS) {
                    fallidos.incrementAndGet();
                    log.warn("Conflicto de versión sin resolver tras {} intentos", intento);
                    throw new ConflictoConcurrenciaException();
                }
                log.info("Conflicto de versión, reintento {}/{}", intento + 1, MAX_INTENTOS);
                esperar();
            }
        }
    }

    private void esperar() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(20, 80));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConflictoConcurrenciaException();
        }
    }

    public Metricas metricas() {
        return new Metricas(conflictos.get(), recuperados.get(), fallidos.get());
    }

    public void reiniciar() {
        conflictos.set(0);
        recuperados.set(0);
        fallidos.set(0);
    }

    /**
     * @param conflictos  veces que la base de datos detectó dos escrituras a la vez
     * @param recuperados operaciones que tuvieron conflicto pero terminaron bien al reintentar
     * @param fallidos    operaciones que se rindieron tras agotar los reintentos
     */
    public record Metricas(long conflictos, long recuperados, long fallidos) {
    }
}
