-- Se ejecuta una sola vez, cuando el volumen de la base está vacío.
-- Activa PostGIS para guardar puntos (clientes, camiones) y polígonos (zonas).
CREATE EXTENSION IF NOT EXISTS postgis;
