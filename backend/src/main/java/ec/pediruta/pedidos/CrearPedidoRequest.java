package ec.pediruta.pedidos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Lo que envía el cliente para hacer un pedido.
 * Solo manda productos y cantidades: el precio lo pone el servidor,
 * así nadie puede comprar con un precio inventado.
 */
public record CrearPedidoRequest(
        @NotBlank(message = "escribe el nombre del negocio") @Size(max = 120) String cliente,
        @NotBlank(message = "escribe la dirección de entrega") @Size(max = 250) String direccion,
        @NotEmpty(message = "el pedido no tiene productos") @Size(max = 50) List<@Valid Linea> items) {

    public record Linea(
            @NotNull Long productoId,
            @Min(value = 1, message = "la cantidad mínima es 1") @Max(1000) int cantidad) {
    }
}
