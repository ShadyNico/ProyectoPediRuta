package ec.pediruta.pedidos;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /** Trae los items en la misma consulta (evita una consulta extra por pedido). */
    @EntityGraph(attributePaths = "items")
    List<Pedido> findByEstadoInOrderByCreadoEnAsc(Collection<EstadoPedido> estados);

    /** Sin EntityGraph: con límite + colección, Hibernate tendría que paginar en memoria. */
    List<Pedido> findTop50ByOrderByCreadoEnDesc();
}
