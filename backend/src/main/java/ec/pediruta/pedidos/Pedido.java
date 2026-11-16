package ec.pediruta.pedidos;

import ec.pediruta.inventario.Producto;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "pedido")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String cliente;

    @Column(nullable = false, length = 250)
    private String direccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado = EstadoPedido.PENDIENTE;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn = OffsetDateTime.now();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "peso_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal pesoKg = BigDecimal.ZERO;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<PedidoItem> items = new ArrayList<>();

    /** Evita que dos bodegueros cambien el mismo pedido al mismo tiempo. */
    @Version
    private Long version;

    public Pedido(String cliente, String direccion) {
        this.cliente = cliente.trim();
        this.direccion = direccion.trim();
    }

    public void agregarItem(Producto producto, int cantidad) {
        PedidoItem item = new PedidoItem(this, producto.getId(), producto.getNombre(),
                cantidad, producto.getPrecio());
        items.add(item);
        total = total.add(item.subtotal());
        pesoKg = pesoKg.add(producto.getPesoKg().multiply(BigDecimal.valueOf(cantidad)));
    }

    public void cambiarEstado(EstadoPedido nuevo) {
        if (!estado.puedePasarA(nuevo)) {
            throw new TransicionInvalidaException(id, estado, nuevo);
        }
        estado = nuevo;
        actualizadoEn = OffsetDateTime.now();
    }

    public List<PedidoItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
