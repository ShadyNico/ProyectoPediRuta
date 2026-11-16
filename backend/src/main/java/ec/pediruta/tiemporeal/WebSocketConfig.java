package ec.pediruta.tiemporeal;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** Publica el canal WebSocket en ws://localhost:8080/ws/eventos (tema 1.4). */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final CanalEventos canal;

    public WebSocketConfig(CanalEventos canal) {
        this.canal = canal;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registro) {
        // En la Unidad 2 se restringirán los orígenes y se exigirá el token del usuario
        registro.addHandler(canal, "/ws/eventos").setAllowedOriginPatterns("*");
    }
}
