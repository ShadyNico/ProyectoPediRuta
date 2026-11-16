package ec.pediruta.tiemporeal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.pediruta.pedidos.PedidoCambiado;
import ec.pediruta.tracking.PosicionRecibida;
import ec.pediruta.tracking.ProtocoloGps;
import ec.pediruta.tracking.TrackingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tema 1.4 – WebSockets.
 *
 * Un solo canal bidireccional para todo el sistema:
 * - El servidor ENVÍA a todos los navegadores conectados:
 *     {"tipo":"posicion","datos":{...}}  cuando un camión se mueve
 *     {"tipo":"pedido","datos":{...}}    cuando un pedido se crea o cambia
 * - El navegador del chofer ENVÍA su ubicación:
 *     {"tipo":"posicion","placa":"PBA-1234","lat":-0.25,"lon":-79.17,"velocidad":30}
 *
 * Patrón Observer: este canal escucha los eventos internos (PosicionRecibida,
 * PedidoCambiado) sin que TrackingService ni PedidoService sepan que existe.
 */
@Slf4j
@Component
public class CanalEventos extends TextWebSocketHandler {

    private static final int TIEMPO_ENVIO_MS = 5000;
    private static final int BUFFER_BYTES = 256 * 1024;

    private final Map<String, WebSocketSession> sesiones = new ConcurrentHashMap<>();
    private final ObjectMapper json;
    private final TrackingService tracking;

    public CanalEventos(ObjectMapper json, TrackingService tracking) {
        this.json = json;
        this.tracking = tracking;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession sesion) {
        // El decorador permite enviar desde varios hilos a la vez sin mezclar mensajes,
        // y descarta a un cliente demasiado lento en lugar de frenar a todos.
        sesiones.put(sesion.getId(), new ConcurrentWebSocketSessionDecorator(sesion, TIEMPO_ENVIO_MS, BUFFER_BYTES));
        log.info("Navegador conectado por WebSocket ({} conectados)", sesiones.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession sesion, CloseStatus estado) {
        sesiones.remove(sesion.getId());
        log.info("Navegador desconectado ({} conectados)", sesiones.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession sesion, TextMessage mensaje) {
        try {
            JsonNode nodo = json.readTree(mensaje.getPayload());
            if ("posicion".equals(nodo.path("tipo").asText())) {
                Long enviado = nodo.hasNonNull("enviadoEnMs") ? nodo.get("enviadoEnMs").asLong() : null;
                tracking.registrar(ProtocoloGps.crear(
                        nodo.path("placa").asText(),
                        nodo.path("lat").asDouble(Double.NaN),
                        nodo.path("lon").asDouble(Double.NaN),
                        nodo.path("velocidad").asDouble(0),
                        enviado,
                        "celular"));
            }
        } catch (JsonProcessingException | IllegalArgumentException e) {
            enviarA(sesion, "error", Map.of("mensaje", e.getMessage()));
        }
    }

    @EventListener
    public void alRecibirPosicion(PosicionRecibida evento) {
        publicar("posicion", evento.posicion());
    }

    /** Se ejecuta después del commit: nunca se avisa de un pedido que luego se deshizo. */
    @TransactionalEventListener(fallbackExecution = true)
    public void alCambiarPedido(PedidoCambiado evento) {
        publicar("pedido", evento.pedido());
    }

    public void publicar(String tipo, Object datos) {
        TextMessage mensaje = armar(tipo, datos);
        if (mensaje == null) {
            return;
        }
        sesiones.values().forEach(s -> enviar(s, mensaje));
    }

    public int conectados() {
        return sesiones.size();
    }

    private void enviarA(WebSocketSession sesion, String tipo, Object datos) {
        TextMessage mensaje = armar(tipo, datos);
        WebSocketSession segura = sesiones.getOrDefault(sesion.getId(), sesion);
        if (mensaje != null) {
            enviar(segura, mensaje);
        }
    }

    private TextMessage armar(String tipo, Object datos) {
        try {
            return new TextMessage(json.writeValueAsString(Map.of("tipo", tipo, "datos", datos)));
        } catch (JsonProcessingException e) {
            log.error("No se pudo convertir el evento {} a JSON", tipo, e);
            return null;
        }
    }

    private void enviar(WebSocketSession sesion, TextMessage mensaje) {
        try {
            if (sesion.isOpen()) {
                sesion.sendMessage(mensaje);
            }
        } catch (Exception e) {
            // La red no es confiable: si un navegador falla, los demás siguen recibiendo
            log.debug("No se pudo enviar a la sesión {}: {}", sesion.getId(), e.getMessage());
        }
    }
}
