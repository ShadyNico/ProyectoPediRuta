package ec.pediruta.inventario;

import ec.pediruta.comun.ReintentoOptimista;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final InventarioService inventario;
    private final ReintentoOptimista reintento;

    public ProductoController(InventarioService inventario, ReintentoOptimista reintento) {
        this.inventario = inventario;
        this.reintento = reintento;
    }

    /** Catálogo con stock actual. */
    @GetMapping
    public List<ProductoDto> listar() {
        return inventario.listar();
    }

    /** Fija el stock de un producto (bodega/administrador y pruebas de concurrencia). */
    @PutMapping("/{id}/stock")
    public ProductoDto ajustarStock(@PathVariable Long id, @Valid @RequestBody AjustarStock cuerpo) {
        return reintento.ejecutar(() -> inventario.ajustarStock(id, cuerpo.stock()));
    }

    public record AjustarStock(@NotNull @Min(0) Integer stock) {
    }
}
