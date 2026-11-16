# PediRuta - Prueba de concurrencia (Sprint 1, tema 1.2)
#
# Simula que muchos clientes compran el MISMO producto al MISMO tiempo,
# cuando quedan pocas unidades. Comprueba que el sistema nunca vende
# mas de lo que hay (gracias a @Version, el bloqueo optimista).
#
# Uso (con el backend encendido):
#   cd scripts
#   powershell -ExecutionPolicy Bypass -File .\prueba-concurrencia.ps1
#   powershell -ExecutionPolicy Bypass -File .\prueba-concurrencia.ps1 -Stock 5 -Clientes 50

param(
    [string]$Producto = 'Jugo de naranja 1 L',
    [int]$Stock = 5,
    [int]$Clientes = 20,
    [string]$Api = 'http://localhost:8080'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http

Write-Host "== Preparando la prueba ==" -ForegroundColor Cyan
$catalogo = Invoke-RestMethod -Uri "$Api/api/productos"
$item = $catalogo | Where-Object { $_.nombre -eq $Producto } | Select-Object -First 1
if (-not $item) { throw "No existe el producto '$Producto'" }
$id = $item.id

Invoke-RestMethod -Method Put -Uri "$Api/api/productos/$id/stock" -ContentType 'application/json' `
    -Body (@{ stock = $Stock } | ConvertTo-Json) | Out-Null
Invoke-RestMethod -Method Delete -Uri "$Api/api/metricas/concurrencia" | Out-Null
Write-Host "Producto: $Producto (id $id). Stock fijado en $Stock."
Write-Host "Lanzando $Clientes pedidos de 1 unidad al mismo tiempo..."

$http = New-Object System.Net.Http.HttpClient
$tareas = New-Object 'System.Collections.Generic.List[System.Threading.Tasks.Task[System.Net.Http.HttpResponseMessage]]'
$reloj = [System.Diagnostics.Stopwatch]::StartNew()
for ($i = 1; $i -le $Clientes; $i++) {
    $json = @{
        cliente   = "Cliente de prueba $i"
        direccion = 'Prueba de concurrencia'
        items     = @(@{ productoId = $id; cantidad = 1 })
    } | ConvertTo-Json -Depth 4
    $cuerpo = New-Object System.Net.Http.StringContent($json, [System.Text.Encoding]::UTF8, 'application/json')
    $tareas.Add($http.PostAsync("$Api/api/pedidos", $cuerpo))
}
[System.Threading.Tasks.Task]::WaitAll($tareas.ToArray())
$reloj.Stop()

$aceptados = @($tareas | Where-Object { [int]$_.Result.StatusCode -eq 201 }).Count
$rechazados = @($tareas | Where-Object { [int]$_.Result.StatusCode -eq 409 }).Count
$otros = $Clientes - $aceptados - $rechazados

$final = (Invoke-RestMethod -Uri "$Api/api/productos") | Where-Object { $_.id -eq $id }
$metricas = Invoke-RestMethod -Uri "$Api/api/metricas/concurrencia"

Write-Host ""
Write-Host "== Resultado ==" -ForegroundColor Cyan
Write-Host ("Pedidos aceptados (201):          {0}" -f $aceptados)
Write-Host ("Pedidos rechazados sin stock (409): {0}" -f $rechazados)
Write-Host ("Otras respuestas:                 {0}" -f $otros)
Write-Host ("Stock final:                      {0}" -f $final.stock)
Write-Host ("Conflictos de version detectados: {0}" -f $metricas.conflictos)
Write-Host ("Resueltos al reintentar:          {0}" -f $metricas.recuperados)
Write-Host ("Fallidos tras 3 intentos:         {0}" -f $metricas.fallidos)
Write-Host ("Tiempo total:                     {0} ms" -f $reloj.ElapsedMilliseconds)
Write-Host ""

if ($aceptados -le $Stock -and $final.stock -eq ($Stock - $aceptados) -and $final.stock -ge 0) {
    Write-Host "CORRECTO: no se vendio mas de lo que habia ($aceptados de $Stock unidades)." -ForegroundColor Green
} else {
    Write-Host "ERROR: el stock no cuadra. Revisa el bloqueo optimista." -ForegroundColor Red
}
Write-Host "Los pedidos de prueba aparecen en la vista Bodega; puedes cancelarlos para devolver el stock."
$http.Dispose()
