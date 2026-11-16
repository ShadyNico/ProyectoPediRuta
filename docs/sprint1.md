# Sprint 1 – Pedidos, stock y concurrencia (tema 1.2)

## Historias

| Historia | Descripción | Dónde |
|---|---|---|
| H03 | El cliente ve el catálogo con precio y stock real | Vista **Cliente** · `GET /api/productos` |
| H04 | El cliente arma su pedido y lo confirma | Vista **Cliente** · `POST /api/pedidos` |
| H05 | Dos clientes no pueden comprar la última unidad a la vez | `Producto.@Version` + `ReintentoOptimista` |
| H06 | La bodega ve los pedidos y los prepara | Vista **Bodega** · `GET /api/pedidos`, `PATCH /api/pedidos/{id}/estado` |

## Endpoints

| Método | Ruta | Qué hace | Respuestas |
|---|---|---|---|
| GET | `/api/productos` | Catálogo con stock | 200 |
| PUT | `/api/productos/{id}/stock` | Fija el stock (`{"stock": 5}`) | 200, 400, 404 |
| POST | `/api/pedidos` | Crea un pedido y descuenta stock | 201, 400, 409 sin stock |
| GET | `/api/pedidos?estado=PENDIENTE,PREPARANDO` | Lista pedidos | 200 |
| GET | `/api/pedidos/{id}` | Detalle | 200, 404 |
| PATCH | `/api/pedidos/{id}/estado` | Avanza el estado (`{"estado": "LISTO"}`) | 200, 409 transición inválida |
| GET / DELETE | `/api/metricas/concurrencia` | Contadores del experimento | 200 / 204 |

Ejemplo de pedido:

```json
{
  "cliente": "Tienda La Esquina",
  "direccion": "Av. Quito y Río Toachi",
  "items": [ { "productoId": 1, "cantidad": 2 }, { "productoId": 4, "cantidad": 1 } ]
}
```

## Estados del pedido

```
PENDIENTE ──► PREPARANDO ──► LISTO ──► EN_CAMINO ──► ENTREGADO
    └──► CANCELADO (devuelve el stock)
```

Cualquier otro salto responde **409**. EN_CAMINO y ENTREGADO llegan en los sprints 2 y 3.

## Cómo se evita vender de más (explicado simple)

Imagina dos cajeros que miran la misma pizarra: "quedan 1 caja". Los dos venden y escriben "quedan 0". Se vendieron 2 cajas que no existían.

**Bloqueo optimista** = la pizarra tiene un número de versión.

1. El pedido A lee el producto: stock 1, **versión 7**.
2. El pedido B lee lo mismo: stock 1, **versión 7**.
3. A guarda: `UPDATE producto SET stock = 0, version = 8 WHERE id = 3 AND version = 7` → 1 fila. ✅
4. B guarda: `... WHERE id = 3 AND version = 7` → **0 filas** (ya es 8). Hibernate lanza un conflicto.
5. `ReintentoOptimista` repite el pedido B desde cero: lee stock 0 → responde **409 "No queda stock"**.

Se llama *optimista* porque no bloquea a nadie mientras lee; solo comprueba al guardar. Es lo adecuado cuando los choques son poco frecuentes.

**Transacción (ACID):** el pedido y el descuento de todos sus productos ocurren juntos. Si el tercer producto no alcanza, se deshacen también los dos primeros.

## Experimento

```powershell
cd scripts
powershell -ExecutionPolicy Bypass -File .\prueba-concurrencia.ps1 -Stock 5 -Clientes 20
```

Qué medir y anotar (sirve para el informe):

| Clientes simultáneos | Stock | Aceptados | Rechazados | Conflictos | Recuperados | Fallidos | Tiempo (ms) |
|---|---|---|---|---|---|---|---|
| 20 | 5 | | | | | | |
| 50 | 5 | | | | | | |
| 100 | 50 | | | | | | |

Resultado correcto siempre: **aceptados ≤ stock** y **stock final = stock − aceptados**.

Pregunta para comparar después: ¿qué pasa con los conflictos y el tiempo si se usa bloqueo **pesimista** (`SELECT ... FOR UPDATE`) en lugar de `@Version`?

## Pendiente para sprints siguientes

- Ubicación del cliente en el mapa y zona automática (PostGIS) → Sprint 3.
- Avisos instantáneos a bodega por WebSocket (hoy consulta cada 5 s) → Sprint 2.
- Login y roles (hoy se cambia de rol con pestañas) → Unidad 2.
