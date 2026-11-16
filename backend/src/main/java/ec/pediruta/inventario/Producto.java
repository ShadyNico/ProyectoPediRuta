package ec.pediruta.inventario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Producto de la bodega con su stock.
 *
 * La anotación @Version es el bloqueo optimista (tema 1.2): Hibernate guarda
 * con "UPDATE ... WHERE id = ? AND version = ?". Si otro pedido cambió el
 * producto primero, la versión ya no coincide, no se actualiza ninguna fila
 * y la operación falla en lugar de sobrescribir el stock.
 */
@Entity
@Table(name = "producto")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 120)
    private String presentacion;

    @Column(nullable = false, length = 60)
    private String categoria;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "peso_kg", nullable = false, precision = 8, scale = 2)
    private BigDecimal pesoKg;

    @Column(nullable = false)
    private int stock;

    @Version
    private Long version;

    public Producto(String nombre, String presentacion, String categoria,
                    BigDecimal precio, BigDecimal pesoKg, int stock) {
        this.nombre = nombre;
        this.presentacion = presentacion;
        this.categoria = categoria;
        this.precio = precio;
        this.pesoKg = pesoKg;
        this.stock = stock;
    }

    /** Resta stock; si no alcanza, lanza error y no cambia nada. */
    public void descontar(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        if (stock < cantidad) {
            throw new StockInsuficienteException(nombre, stock, cantidad);
        }
        stock -= cantidad;
    }

    /** Devuelve stock, por ejemplo al cancelar un pedido. */
    public void devolver(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        stock += cantidad;
    }

    public void ajustarStock(int nuevoStock) {
        if (nuevoStock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
        stock = nuevoStock;
    }
}
