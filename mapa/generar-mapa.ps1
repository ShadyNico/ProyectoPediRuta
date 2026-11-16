# PediRuta - Genera el mapa propio de Santo Domingo (Sprint 0, H02)
#
# Se ejecuta UNA sola vez, en casa y CON internet:
#   cd mapa
#   powershell -ExecutionPolicy Bypass -File .\generar-mapa.ps1
#
# Requisitos: Java 21 o superior en el PATH (java -version).
# La primera vez descarga mas de 1 GB (datos de Ecuador y del mundo que Planetiler necesita).

$ErrorActionPreference = 'Stop'
$aqui = $PSScriptRoot
$destinoMapa = Join-Path $aqui '..\frontend\public\mapa'
$destinoFuentes = Join-Path $aqui '..\frontend\public\fonts'

# Recuadro de la ciudad de Santo Domingo: oeste, sur, este, norte (longitud, latitud).
# Para ajustarlo, dibuja un rectangulo en https://bboxfinder.com
$recuadro = '-79.25,-0.33,-79.10,-0.18'

Write-Host '== 1. Verificando Java ==' -ForegroundColor Cyan
# java -version escribe en stderr; en Windows PowerShell eso no debe detener el script
$ErrorActionPreference = 'Continue'
$versionJava = (cmd /c "java -version 2>&1" | Out-String)
$ErrorActionPreference = 'Stop'
Write-Host $versionJava
if ($versionJava -notmatch 'version "(2[1-9]|[3-9]\d)') {
    Write-Host 'Planetiler necesita Java 21 o superior. Instala Eclipse Temurin 21.' -ForegroundColor Red
    exit 1
}

Write-Host '== 2. Descargando Planetiler (si falta) ==' -ForegroundColor Cyan
$jar = Join-Path $aqui 'planetiler.jar'
if (-not (Test-Path $jar)) {
    Invoke-WebRequest -Uri 'https://github.com/onthegomap/planetiler/releases/latest/download/planetiler.jar' -OutFile $jar
}

Write-Host '== 3. Generando santo_domingo.pmtiles (puede tardar varios minutos) ==' -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $destinoMapa | Out-Null
$salida = Join-Path $destinoMapa 'santo_domingo.pmtiles'
Push-Location $aqui
try {
    & java -Xmx2g -jar $jar --download --download_threads=8 --download_chunk_size_mb=20 --area=ecuador --bounds=$recuadro --output=$salida --force
    if ($LASTEXITCODE -ne 0) { throw "Planetiler termino con error $LASTEXITCODE" }
} finally {
    Pop-Location
}
# Planetiler guarda los datos descargados en mapa/data/ (incluye ecuador.osm.pbf,
# que se reutiliza en el Sprint 6 para GraphHopper). No lo borres.

Write-Host '== 4. Descargando fuentes para los nombres de calles ==' -ForegroundColor Cyan
$carpetaNoto = Join-Path $destinoFuentes 'Noto Sans Regular'
if (-not (Test-Path $carpetaNoto)) {
    try {
        $zip = Join-Path $env:TEMP 'fuentes-mapa.zip'
        Invoke-WebRequest -Uri 'https://github.com/openmaptiles/fonts/releases/download/v2.0/v2.0.zip' -OutFile $zip
        $temporal = Join-Path $env:TEMP 'fuentes-mapa'
        Expand-Archive -Path $zip -DestinationPath $temporal -Force
        $origen = Get-ChildItem -Path $temporal -Directory -Recurse -Filter 'Noto Sans Regular' | Select-Object -First 1
        if (-not $origen) { throw 'No se encontro la carpeta Noto Sans Regular en el zip' }
        New-Item -ItemType Directory -Force -Path $destinoFuentes | Out-Null
        Copy-Item -Path $origen.FullName -Destination $destinoFuentes -Recurse -Force
    } catch {
        Write-Host "No se pudieron descargar las fuentes: $($_.Exception.Message)" -ForegroundColor Yellow
        Write-Host 'El mapa se vera igual, pero sin nombres de calles. Avisale al equipo.' -ForegroundColor Yellow
    }
}

Write-Host ''
Write-Host "Listo: $salida" -ForegroundColor Green
Write-Host 'Abre el frontend (npm run dev) y recarga la pagina. Desde ahora funciona sin internet.' -ForegroundColor Green
