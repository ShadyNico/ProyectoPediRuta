package ec.pediruta.inventario;

import java.math.BigDecimal;

/** Lo que el frontend ve de un producto (sin la columna version). */
public record ProductoDto(
        Long id,
        String nombre,
        String presentacion,
        String categoria,
        BigDecimal precio,
        BigDecimal pesoKg,
        int stock) {

    public static ProductoDto de(Producto p) {
        return new ProductoDto(p.getId(), p.getNombre(), p.getPresentacion(), p.getCategoria(),
                p.getPrecio(), p.getPesoKg(), p.getStock());
    }
}
