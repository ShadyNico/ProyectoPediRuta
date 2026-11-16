# Sprint 2 – Camión en vivo: sockets TCP y WebSockets (temas 1.3 y 1.4)

## Historias

| Historia | Descripción | Dónde |
|---|---|---|
| H07 | El simulador GPS envía la posición de varios camiones por socket TCP | `simulador/SimuladorGps.java` → `ServidorGpsTcp` (puerto 9090) |
| H08 | El chofer comparte su ubicación desde el navegador del celular | Pestaña **Chofer** → WebSocket |
| H09 | El mapa muestra los camiones moviéndose en tiempo real | Pestaña **Mapa** |
| H10 | La bodega y el catálogo se actualizan al instante | Evento `pedido` por WebSocket |

## Cómo viaja la información

```
 Simulador GPS (Java)                     Celular del chofer (navegador)
  3 hilos virtuales,                        watchPosition() del GPS
  1 conexión TCP por camión                          │
        │  "PBA-1234;-0.25;-79.17;40;1763..."         │ {"tipo":"posicion",...}
        ▼  socket TCP :9090  ◄── "OK"                 ▼ WebSocket /ws/eventos
 ┌──────────────────────────────── Backend Spring Boot ──────────────────────────┐
 │ ServidorGpsTcp ──► TrackingService ──(evento PosicionRecibida)──► CanalEventos │
 │ PedidoService ─────────────(evento PedidoCambiado, tras el commit)──┘          │
 └────────────────────────────────────────────────────────────────────┬──────────┘
                                                                      │ WebSocket
                                       Mapa · Bodega · Cliente ◄──────┘
```

## Socket TCP vs WebSocket

| | Socket TCP (simulador) | WebSocket (navegador) |
|---|---|---|
| Quién lo usa | Aparatos GPS, programas | Navegadores (no pueden abrir sockets TCP puros) |
| Formato | Protocolo de texto propio, una línea por mensaje | JSON dentro de "frames" WebSocket |
| Cómo empieza | Conexión directa al puerto 9090 | Empieza como HTTP y se "actualiza" a WebSocket |
| Dirección | Dispositivo → servidor, con respuesta OK/ERROR | En ambos sentidos, el servidor envía cuando quiere |
| Concurrencia | Un hilo virtual por conexión | Envíos seguros desde varios hilos con `ConcurrentWebSocketSessionDecorator` |

### Protocolo TCP propio

```
Cliente → PLACA;LATITUD;LONGITUD[;VELOCIDAD_KMH][;MARCA_DE_TIEMPO_MS]
Servidor → OK            |  ERROR motivo
```

### Mensajes WebSocket

| Dirección | Mensaje |
|---|---|
| Servidor → navegadores | `{"tipo":"posicion","datos":{"placa":"PBA-1234","lat":..,"lon":..,"velocidadKmh":..,"recibidoEnMs":..,"origen":"simulador"}}` |
| Servidor → navegadores | `{"tipo":"pedido","datos":{ ...pedido completo... }}` |
| Celular → servidor | `{"tipo":"posicion","placa":"PBA-1234","lat":..,"lon":..,"velocidad":..,"enviadoEnMs":..}` |
| Servidor → celular | `{"tipo":"error","datos":{"mensaje":"placa inválida"}}` |

## Decisiones de diseño (para el informe)

- **Hilos virtuales (Java 21):** cada camión conectado tiene su hilo, pero son hilos ligeros; 100 camiones no consumen 100 hilos del sistema operativo.
- **Patrón Observer:** `TrackingService` y `PedidoService` solo publican eventos. `CanalEventos` los escucha. Si mañana se agrega RabbitMQ, los servicios no cambian.
- **Eventos después del commit:** el aviso de un pedido sale solo si la transacción se confirmó (`@TransactionalEventListener`). Nunca se avisa de un pedido que se deshizo.
- **CAP:** las posiciones viven en memoria y priorizan disponibilidad; si se pierde una, la siguiente llega en 1 s.
- **Falacias:** el simulador y el navegador se reconectan solos con espera creciente (1, 2, 4, 8, 10 s). La bodega mantiene una consulta cada 30 s como respaldo si el WebSocket se cae.
- **Tiempo de espera:** si un dispositivo no envía nada en 30 s, el servidor cierra su conexión y libera el hilo.

## Cómo probarlo

1. Reinicia el backend. En la consola debe salir `Servidor GPS TCP escuchando en el puerto 9090`.
2. En otra terminal, desde la carpeta `pediruta`:
   ```powershell
   java simulador/SimuladorGps.java
   ```
3. Abre la pestaña **Mapa**: aparecen 3 camiones naranjas moviéndose y el panel de flota.
4. Abre **Bodega** en otra ventana y haz un pedido en **Cliente**: aparece al instante.
5. **Prueba de resiliencia:** detén el backend con el simulador encendido. El simulador muestra "sin conexión, reintento en 1 s, 2 s, 4 s…". Enciende el backend: los camiones se reconectan solos.

### Con el celular (opcional)

El navegador solo entrega el GPS en **https** o en **localhost**. Para probar en la misma red WiFi sin https:

1. Mira la IP de la laptop: `ipconfig` → "Dirección IPv4", por ejemplo `192.168.1.20`.
2. Permite el puerto 5173 en el Firewall de Windows si lo pregunta.
3. En el Chrome del celular abre `chrome://flags/#unsafely-treat-insecure-origin-as-secure`, escribe `http://192.168.1.20:5173`, actívalo y reinicia Chrome.
4. Abre `http://192.168.1.20:5173`, pestaña **Chofer**, escribe una placa y pulsa **Empezar a compartir**. Aparece en el mapa en azul.

En el Sprint 4 (seguridad) se configura https con un certificado local y ya no hará falta este paso.

## Experimento: ¿cuántos camiones aguanta?

Métricas en `http://localhost:8080/api/camiones/metricas` (reiniciar con `DELETE` al mismo endpoint):

- `latenciaPromedioMs`: tiempo desde que el simulador envía hasta que el servidor recibe.
- `conexionesTcp`: dispositivos conectados ahora mismo.
- El panel del mapa muestra el **retraso** desde que el servidor recibe hasta que el navegador dibuja.

```powershell
java simulador/SimuladorGps.java --camiones=10
java simulador/SimuladorGps.java --camiones=100 --intervalo=500
```

| Camiones | Intervalo | Mensajes/s | Latencia promedio TCP (ms) | Latencia máx (ms) | Retraso hasta el mapa (ms) |
|---|---|---|---|---|---|
| 3 | 1000 ms | 3 | | | |
| 10 | 1000 ms | 10 | — | — | 3 |
| 100 | 500 ms | 200 | 0,5 | 10 | 20 |

Resultado (16/11/2026, 11 042 mensajes, 100 conexiones TCP abiertas): el servidor recibe cada posición en 0,5 ms de promedio aun con 100 camiones. El retraso que crece es el del navegador al dibujar 100 marcadores (de 3 ms a 20 ms): el cuello de botella está en el cliente, no en los sockets.

Pregunta para comparar: ¿la latencia crece de forma lineal con la cantidad de camiones? ¿En qué punto el mapa del navegador empieza a ir lento?
