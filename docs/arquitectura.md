# PediRuta – Documento de arquitectura (Unidad 1, temas 1.1.1 a 1.1.5)

Sistema distribuido de pedidos y despachos en tiempo real para una distribuidora de Santo Domingo, Ecuador. La distribuidora tiene su propio stock, su bodega, sus camiones y sus choferes.

## 1. ¿Qué es una aplicación distribuida y por qué PediRuta lo es? (1.1.1)

Una aplicación distribuida está formada por varios componentes que corren en procesos o equipos distintos y cooperan comunicándose por la red. Para el usuario parece un solo sistema.

PediRuta lo es porque sus partes viven en dispositivos distintos y se comunican por red:

| Componente | Dónde corre | Se comunica con | Cómo |
|---|---|---|---|
| Vista Cliente | Navegador (PC o celular) | Backend | HTTP REST + WebSocket |
| Vista Bodega | Navegador (PC) | Backend | HTTP REST + WebSocket |
| Vista Chofer | Navegador del celular del chofer | Backend | WebSocket (envía su GPS) |
| Vista Administrador | Navegador (PC) | Backend | HTTP REST + WebSocket |
| Simulador GPS | Programa Java | Backend | Socket TCP |
| Backend (Spring Boot) | Servidor / laptop | Base de datos, navegadores | JDBC, HTTP, WebSocket |
| PostgreSQL + PostGIS | Contenedor Docker | Backend | JDBC (TCP 5432) |
| RabbitMQ (Unidad 3) | Contenedor Docker | Servicios | AMQP |

## 2. Evolución de la arquitectura (1.1.2)

| Etapa | Unidad | Arquitectura |
|---|---|---|
| 1 | Unidades 1 y 2 | **Monolito modular**: una sola app Spring Boot con paquetes separados (`pedidos`, `inventario`, `tracking`, `salud`, `comun`) |
| 2 | Unidad 3 | **Servicios con eventos**: Pedidos+Inventario, Despacho y Notificaciones, cada uno con su base, comunicados por RPC (OpenFeign) y colas (RabbitMQ) |
| 3 | Unidad 3 | **Réplicas con balanceo**: 2-3 instancias de cada servicio crítico detrás de Nginx |

Se empieza como monolito para tener funcionalidad pronto; los paquetes ya marcan por dónde se cortará después.

## 3. Ventajas y desafíos (1.1.3)

| Ventajas | Ejemplo en PediRuta |
|---|---|
| Escalabilidad | Si crecen los pedidos, se agregan réplicas solo del servicio de Pedidos |
| Tolerancia a fallos | Si cae una réplica, Nginx envía las peticiones a otra |
| Independencia | Notificaciones puede caerse sin impedir que se creen pedidos (con colas) |
| Tiempo real | Cliente, bodega y chofer ven los cambios al instante |

| Desafíos | Ejemplo en PediRuta |
|---|---|
| Concurrencia | Dos clientes compran la última caja al mismo tiempo |
| Consistencia | El stock debe ser correcto aunque haya varias réplicas |
| Fallos parciales | La base responde pero RabbitMQ no |
| Depuración | Un pedido pasa por varios servicios: se necesita un CorrelationId en los logs |
| Red inestable | El celular del chofer pierde señal en algunas calles |

## 4. Las 8 falacias de la computación distribuida (1.1.4)

| # | Falacia | Riesgo en PediRuta | Decisión tomada |
|---|---|---|---|
| 1 | La red es confiable | La base de datos o un servicio deja de responder | `/api/salud` informa "sin conexión" en lugar de un error 500; reintentos; colas que guardan los mensajes |
| 2 | La latencia es cero | Consultar stock a otro servicio tarda | Timeouts cortos (3 s); tareas lentas por cola, no en la petición del cliente |
| 3 | El ancho de banda es infinito | El GPS de muchos camiones satura la red | Enviar la posición cada 5 s con un mensaje pequeño (id, lat, lon, hora) |
| 4 | La red es segura | Alguien envía posiciones falsas o ve pedidos ajenos | JWT con roles (Unidad 2); HTTPS en la demo con mkcert |
| 5 | La topología no cambia | Cambia la IP de la laptop o se agregan réplicas | Nombres de servicio en Docker y Nginx como punto de entrada único |
| 6 | Hay un solo administrador | Equipo de varias personas configurando distinto | Todo en `docker-compose.yml` y en el repositorio |
| 7 | El costo de transporte es cero | Usar APIs pagadas de mapas y rutas | Mapa propio (OpenStreetMap + Planetiler) y rutas locales; costo $0 |
| 8 | La red es homogénea | Celulares Android, iPhone, PCs Windows | Interfaz web responsive con estándares (HTTP, WebSocket, JSON) |

## 5. Teorema CAP (1.1.5)

Ante una partición de red, un sistema distribuido debe elegir entre **Consistencia** (todos ven el mismo dato) y **Disponibilidad** (siempre responde). PediRuta decide por servicio:

| Servicio | Prioriza | Justificación |
|---|---|---|
| Inventario | **Consistencia (CP)** | No se puede vender 10 cajas si hay 8. Si no se puede confirmar el stock, es mejor rechazar el pedido |
| Pedidos | **Consistencia (CP)** | Un pedido con dinero y productos debe guardarse completo o no guardarse (ACID) |
| Tracking GPS | **Disponibilidad (AP)** | Si una posición llega 5 s tarde o se pierde una, la siguiente la corrige |
| Notificaciones | **Disponibilidad (AP)** | Un aviso atrasado es aceptable; lo importante es que el sistema siga funcionando |

## 6. Requisito de diseño: sin dependencia de servicios externos

El mapa (`santo_domingo.pmtiles`), las fuentes, la base de datos y las rutas corren localmente. Internet solo se usa una vez para descargar dependencias y datos. La prueba de aceptación de cada historia es que funcione con el WiFi de la laptop apagado.
