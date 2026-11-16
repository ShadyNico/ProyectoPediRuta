package ec.pediruta.pedidos;

import ec.pediruta.comun.NoEncontradoException;
import ec.pediruta.inventario.InventarioService;
import ec.pediruta.inventario.Producto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
public class PedidoService {

    private final PedidoRepository pedidos;
    private final InventarioService inventario;
    private final ApplicationEventPublisher eventos;

    public PedidoService(PedidoRepository pedidos, InventarioService inventario, ApplicationEventPublisher eventos) {
        this.pedidos = pedidos;
        this.inventario = inventario;
        this.eventos = eventos;
    }

    /**
     * Crea el pedido y descuenta el stock en UNA sola transacción (ACID):
     * si un producto no alcanza, se deshace todo y no queda un pedido a medias.
     *
     * Los productos se procesan ordenados por id para que dos pedidos con los
     * mismos productos los bloqueen en el mismo orden.
     */
    @Transactional
    public PedidoDto crear(CrearPedidoRequest solicitud) {
        Pedido pedido = new Pedido(solicitud.cliente(), solicitud.direccion());
        for (Map.Entry<Long, Integer> linea : agruparPorProducto(solicitud).entrySet()) {
            Producto producto = inventario.descontar(linea.getKey(), linea.getValue());
            pedido.agregarItem(producto, linea.getValue());
        }
        Pedido guardado = pedidos.save(pedido);
        log.info("Pedido #{} creado: {} productos, total {}", guardado.getId(),
                guardado.getItems().size(), guardado.getTotal());
        PedidoDto dto = PedidoDto.de(guardado);
        eventos.publishEvent(new PedidoCambiado(dto)); // la bodega lo ve al instante
        return dto;
    }

    @Transactional(readOnly = true)
    public List<PedidoDto> listar(Collection<EstadoPedido> estados) {
        List<Pedido> lista = (estados == null || estados.isEmpty())
                ? pedidos.findTop50ByOrderByCreadoEnDesc()
                : pedidos.findByEstadoInOrderByCreadoEnAsc(estados);
        return lista.stream().map(PedidoDto::de).toList();
    }

    @Transactional(readOnly = true)
    public PedidoDto obtener(Long id) {
        return PedidoDto.de(buscar(id));
    }

    /** Cambia el estado; si se cancela, devuelve el stock en la misma transacción. */
    @Transactional
    public PedidoDto cambiarEstado(Long id, EstadoPedido nuevo) {
        Pedido pedido = buscar(id);
        pedido.cambiarEstado(nuevo);
        if (nuevo == EstadoPedido.CANCELADO) {
            pedido.getItems().forEach(i -> inventario.devolver(i.getProductoId(), i.getCantidad()));
        }
        log.info("Pedido #{} ahora está {}", id, nuevo);
        PedidoDto dto = PedidoDto.de(pedido);
        eventos.publishEvent(new PedidoCambiado(dto));
        return dto;
    }

    private Pedido buscar(Long id) {
        return pedidos.findById(id).orElseThrow(() -> new NoEncontradoException("El pedido", id));
    }

    /** Si el mismo producto viene en dos líneas, suma las cantidades. */
    private static Map<Long, Integer> agruparPorProducto(CrearPedidoRequest solicitud) {
        Map<Long, Integer> agrupado = new TreeMap<>();
        solicitud.items().forEach(l -> agrupado.merge(l.productoId(), l.cantidad(), Integer::sum));
        return agrupado;
    }
}
