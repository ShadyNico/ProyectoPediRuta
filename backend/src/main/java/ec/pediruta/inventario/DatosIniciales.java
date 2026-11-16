package ec.pediruta.inventario;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Carga un catálogo de ejemplo la primera vez que arranca el sistema
 * (cuando la tabla de productos está vacía).
 */
@Slf4j
@Component
public class DatosIniciales implements CommandLineRunner {

    private final ProductoRepository productos;

    public DatosIniciales(ProductoRepository productos) {
        this.productos = productos;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (productos.count() > 0) {
            return;
        }
        List<Producto> catalogo = List.of(
                p("Agua mineral 500 ml", "Paquete x 24", "Bebidas", "6.50", "12", 120),
                p("Cola 3 L", "Paquete x 6", "Bebidas", "11.40", "19", 64),
                p("Jugo de naranja 1 L", "Caja x 12", "Bebidas", "14.20", "13", 8),
                p("Galletas surtidas", "Caja x 30", "Snacks", "9.80", "6", 45),
                p("Detergente 1 kg", "Funda x 10", "Limpieza", "16.00", "10", 30),
                p("Arroz 2 kg", "Saco x 10", "Abarrotes", "18.50", "20", 0),
                p("Aceite 1 L", "Caja x 12", "Abarrotes", "21.60", "11", 52),
                p("Atún en lata", "Caja x 48", "Abarrotes", "52.80", "8", 17));
        productos.saveAll(catalogo);
        log.info("Catálogo inicial cargado: {} productos", catalogo.size());
    }

    private static Producto p(String nombre, String presentacion, String categoria,
                              String precio, String pesoKg, int stock) {
        return new Producto(nombre, presentacion, categoria,
                new BigDecimal(precio), new BigDecimal(pesoKg), stock);
    }
}
