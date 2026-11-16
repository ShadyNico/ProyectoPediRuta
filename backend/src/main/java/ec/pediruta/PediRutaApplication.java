package ec.pediruta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de PediRuta.
 *
 * Por ahora es un monolito modular: una sola aplicación con paquetes bien
 * separados (pedidos, inventario, tracking...). En la Unidad 3 cada paquete
 * se convertirá en un servicio independiente comunicado por RabbitMQ.
 */
@SpringBootApplication
public class PediRutaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PediRutaApplication.class, args);
    }
}
