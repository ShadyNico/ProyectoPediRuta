# PediRuta - Cancela los pedidos creados por prueba-concurrencia.ps1
# (los que tienen la direccion "Prueba de concurrencia" y siguen PENDIENTES).
# Al cancelarlos, el stock vuelve al inventario.
#
# Uso (con el backend encendido):
#   cd scripts
#   powershell -ExecutionPolicy Bypass -File .\limpiar-pruebas.ps1

param([string]$Api = 'http://localhost:8080')

$ErrorActionPreference = 'Stop'

$pendientes = Invoke-RestMethod -Uri "$Api/api/pedidos?estado=PENDIENTE"
$pruebas = @($pendientes | Where-Object { $_.direccion -eq 'Prueba de concurrencia' })

if ($pruebas.Count -eq 0) {
    Write-Host "No hay pedidos de prueba pendientes." -ForegroundColor Green
    return
}

Write-Host "Cancelando $($pruebas.Count) pedidos de prueba..."
$cancelados = 0
foreach ($p in $pruebas) {
    try {
        Invoke-RestMethod -Method Patch -Uri "$Api/api/pedidos/$($p.id)/estado" `
            -ContentType 'application/json' -Body '{"estado":"CANCELADO"}' | Out-Null
        $cancelados++
    } catch {
        Write-Host "No se pudo cancelar el pedido #$($p.id): $($_.Exception.Message)" -ForegroundColor Yellow
    }
}
Write-Host "Listo: $cancelados pedidos cancelados y su stock devuelto." -ForegroundColor Green
