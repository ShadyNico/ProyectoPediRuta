package ec.pediruta.inventario;

import ec.pediruta.comun.NoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Único lugar que cambia el stock (principio de responsabilidad única, SOLID).
 */
@Service
public class InventarioService {

    private final ProductoRepository productos;

    public InventarioService(ProductoRepository productos) {
        this.productos = productos;
    }

    @Transactional(readOnly = true)
    public List<ProductoDto> listar() {
        return productos.findAllByOrderByCategoriaAscNombreAsc().stream()
                .map(ProductoDto::de)
                .toList();
    }

    /**
     * Resta stock dentro de la transacción del pedido (MANDATORY: si no hay
     * transacción abierta, falla). Así, si un producto del pedido no alcanza,
     * se deshace también lo descontado de los demás productos.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Producto descontar(Long productoId, int cantidad) {
        Producto producto = buscar(productoId);
        producto.descontar(cantidad);
        return producto;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void devolver(Long productoId, int cantidad) {
        buscar(productoId).devolver(cantidad);
    }

    @Transactional
    public ProductoDto ajustarStock(Long productoId, int nuevoStock) {
        Producto producto = buscar(productoId);
        producto.ajustarStock(nuevoStock);
        return ProductoDto.de(producto);
    }

    private Producto buscar(Long productoId) {
        return productos.findById(productoId)
                .orElseThrow(() -> new NoEncontradoException("El producto", productoId));
    }
}
