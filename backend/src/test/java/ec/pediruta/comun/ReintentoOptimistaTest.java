package ec.pediruta.comun;

import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReintentoOptimistaTest {

    private final ReintentoOptimista reintento = new ReintentoOptimista();

    private static ObjectOptimisticLockingFailureException conflicto() {
        return new ObjectOptimisticLockingFailureException("Producto", 1L);
    }

    @Test
    void siHayConflictoReintentaYTermina() {
        AtomicInteger llamadas = new AtomicInteger();

        String resultado = reintento.ejecutar(() -> {
            if (llamadas.incrementAndGet() < 3) {
                throw conflicto();
            }
            return "pedido guardado";
        });

        assertThat(resultado).isEqualTo("pedido guardado");
        assertThat(llamadas).hasValue(3);
        assertThat(reintento.metricas().conflictos()).isEqualTo(2);
        assertThat(reintento.metricas().recuperados()).isEqualTo(1);
    }

    @Test
    void trasTresConflictosSeRinde() {
        AtomicInteger llamadas = new AtomicInteger();

        assertThatThrownBy(() -> reintento.ejecutar(() -> {
            llamadas.incrementAndGet();
            throw conflicto();
        })).isInstanceOf(ConflictoConcurrenciaException.class);

        assertThat(llamadas).hasValue(ReintentoOptimista.MAX_INTENTOS);
        assertThat(reintento.metricas().fallidos()).isEqualTo(1);
    }

    @Test
    void otrosErroresNoSeReintentan() {
        AtomicInteger llamadas = new AtomicInteger();

        assertThatThrownBy(() -> reintento.ejecutar(() -> {
            llamadas.incrementAndGet();
            throw new IllegalStateException("otro error");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(llamadas).hasValue(1);
    }
}
