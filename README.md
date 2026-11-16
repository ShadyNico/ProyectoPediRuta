# PediRuta

Sistema distribuido de pedidos y despachos en tiempo real para una distribuidora de Santo Domingo, Ecuador.
Proyecto de **Aplicaciones Distribuidas** – ESPE Santo Domingo.

Estado: **Sprint 2** (camión en vivo con sockets TCP y WebSockets). Sprints 0 y 1 cerrados.

| Sprint | Contenido | Estado |
|---|---|---|
| 0 | Base del proyecto, `/api/salud`, mapa propio sin internet | ✅ |
| 1 | Catálogo, pedidos, vista Cliente y Bodega, bloqueo optimista | ✅ · `docs/sprint1.md` |
| 2 | Camión en vivo: simulador GPS (socket TCP) y WebSocket | En curso · `docs/sprint2.md` |
| 3 | Despacho: zonas con PostGIS, asignación de chofer y camión | Pendiente |

## Estructura

```
pediruta/
├── docker-compose.yml      PostgreSQL 16 + PostGIS 3.4
├── infra/db/init.sql       Activa PostGIS
├── backend/                Spring Boot 3.5 · Java 21 (monolito modular)
├── frontend/               React 18 + Vite 5 + MapLibre
├── mapa/generar-mapa.ps1   Genera el mapa de Santo Domingo con Planetiler
└── docs/arquitectura.md    Temas 1.1.1 a 1.1.5: falacias y CAP
```

## Requisitos

| Programa | Versión | Para qué |
|---|---|---|
| Java (Eclipse Temurin) | 21 | Backend y Planetiler |
| Docker Desktop | Reciente | Base de datos |
| Node.js | 20 o superior | Frontend |
| IntelliJ IDEA | Cualquiera | Abrir el backend (trae Maven incluido) |

## Cómo levantarlo (PowerShell)

### 1. Base de datos

```powershell
cd pediruta
docker compose up -d
docker compose ps        # debe decir "healthy"
```

### 2. Mapa de Santo Domingo (una sola vez, con internet)

```powershell
cd mapa
powershell -ExecutionPolicy Bypass -File .\generar-mapa.ps1
```

Descarga más de 1 GB la primera vez. Deja `frontend/public/mapa/santo_domingo.pmtiles` y las fuentes en `frontend/public/fonts/`.

### 3. Backend

En IntelliJ: **File → Open → carpeta `backend`**. Espera a que descargue las dependencias de Maven y ejecuta `PediRutaApplication`.

Prueba: abre http://localhost:8080/api/salud. Debe responder algo así:

```json
{ "servicio": "pediruta-backend", "baseDeDatos": "conectada", "postgis": "3.4 USE_GEOS=1 ..." }
```

Para correr las pruebas: en IntelliJ, clic derecho en `src/test/java` → **Run 'All Tests'**.

### 4. Frontend

```powershell
cd frontend
npm install
npm run dev
```

Abre http://localhost:5173. Arriba a la derecha el indicador debe estar en **verde** y debe verse el mapa de Santo Domingo.

### 5. Prueba sin internet

Apaga el WiFi, recarga la página y revisa que el mapa y el indicador sigan funcionando. Si pasa, H01 y H02 están terminadas.

## Criterios de aceptación (Definition of Done del Sprint 0)

- [ ] `docker compose up -d` levanta PostgreSQL + PostGIS (healthy).
- [ ] `GET /api/salud` responde con la versión de PostGIS.
- [ ] Con la base apagada (`docker compose stop`), `/api/salud` responde "sin conexión" y no un error 500.
- [ ] Las pruebas del backend pasan.
- [ ] El frontend muestra el indicador en verde y el mapa de Santo Domingo.
- [ ] Todo funciona con el WiFi apagado.
- [ ] `docs/arquitectura.md` está revisado por el equipo.

## Problemas comunes

| Problema | Solución |
|---|---|
| `port 5432 is already allocated` | Hay otro PostgreSQL instalado. Detenlo o cambia el puerto en `docker-compose.yml` y en `application.yml` |
| El backend no arranca: `Connection refused` | La base no está levantada: `docker compose up -d` |
| Indicador rojo "Backend sin respuesta" | El backend no está corriendo en el puerto 8080 |
| Pantalla "Falta el mapa" | Ejecuta `mapa/generar-mapa.ps1` |
| El script dice que necesita Java 21 | Instala Temurin 21 y abre una nueva ventana de PowerShell |
| El mapa se ve pero sin nombres de calles | Faltan las fuentes en `frontend/public/fonts/Noto Sans Regular/` |
| `running scripts is disabled on this system` | Usa el comando con `-ExecutionPolicy Bypass` como está arriba |
| Error de Planetiler por memoria | Cierra programas o cambia `-Xmx2g` por `-Xmx3g` en el script |

## Sprint 1: cómo probarlo

1. Reinicia el backend: al arrancar crea las tablas y carga 8 productos de ejemplo.
2. En http://localhost:5173 usa las pestañas **Cliente**, **Bodega** y **Mapa**.
3. Prueba de concurrencia (con el backend encendido):

```powershell
cd scripts
powershell -ExecutionPolicy Bypass -File .\prueba-concurrencia.ps1
```

Explicación, endpoints y tabla del experimento en `docs/sprint1.md`.

## Sprint 2: cómo probarlo

1. Reinicia el backend (abre también el servidor GPS en el puerto 9090).
2. Desde la carpeta `pediruta`, en otra terminal:

```powershell
java simulador/SimuladorGps.java
```

3. Pestaña **Mapa**: los camiones se mueven en vivo. Pestaña **Chofer**: comparte la ubicación del navegador.

Detalles, protocolo y experimento en `docs/sprint2.md`.
