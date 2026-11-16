package ec.pediruta.pedidos;

import ec.pediruta.comun.ReintentoOptimista;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService servicio;
    private final ReintentoOptimista reintento;

    public PedidoController(PedidoService servicio, ReintentoOptimista reintento) {
        this.servicio = servicio;
        this.reintento = reintento;
    }

    /** El cliente confirma su pedido. Responde 201, o 409 si no hay stock. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoDto crear(@Valid @RequestBody CrearPedidoRequest solicitud) {
        return reintento.ejecutar(() -> servicio.crear(solicitud));
    }

    /** Ejemplo: /api/pedidos?estado=PENDIENTE,PREPARANDO (sin filtro: los últimos 50). */
    @GetMapping
    public List<PedidoDto> listar(@RequestParam(name = "estado", required = false) List<EstadoPedido> estados) {
        return servicio.listar(estados);
    }

    @GetMapping("/{id}")
    public PedidoDto obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    /** La bodega avanza el pedido: PREPARANDO, LISTO o CANCELADO. */
    @PatchMapping("/{id}/estado")
    public PedidoDto cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstado cuerpo) {
        return reintento.ejecutar(() -> servicio.cambiarEstado(id, cuerpo.estado()));
    }

    public record CambiarEstado(@NotNull EstadoPedido estado) {
    }
}
