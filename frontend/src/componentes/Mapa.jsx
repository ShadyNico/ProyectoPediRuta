import { useEffect, useRef, useState } from 'react'
import maplibregl from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import { Protocol } from 'pmtiles'
import { crearEstilo } from './estiloMapa.js'
import { useEventos } from '../tiemporeal.js'

// H02: mapa propio de Santo Domingo, servido desde la laptop (sin internet).
// H07-H08 (Sprint 2): los camiones se mueven en vivo con las posiciones que llegan por WebSocket.
const ARCHIVO_MAPA = '/mapa/santo_domingo.pmtiles'
const CENTRO_SANTO_DOMINGO = [-79.172, -0.253] // [longitud, latitud]
const SIN_SENAL_MS = 15000 // si un camión no reporta en 15 s, se muestra en gris

export default function Mapa() {
  const contenedor = useRef(null)
  const mapaRef = useRef(null)
  const marcadores = useRef(new Map()) // placa -> maplibregl.Marker
  const [falta, setFalta] = useState(false)
  const [listo, setListo] = useState(false)
  const [camiones, setCamiones] = useState({}) // placa -> última posición
  const [ahora, setAhora] = useState(Date.now())
  const [retrasoMs, setRetrasoMs] = useState(null)

  const conexion = useEventos((m) => {
    if (m.tipo !== 'posicion') return
    const p = m.datos
    setRetrasoMs(Math.max(0, Date.now() - p.recibidoEnMs))
    setCamiones((actual) => ({ ...actual, [p.placa]: p }))
  })

  // Crear el mapa (una sola vez)
  useEffect(() => {
    let mapa
    let cancelado = false
    const protocolo = new Protocol()
    maplibregl.addProtocol('pmtiles', protocolo.tile)

    fetch(ARCHIVO_MAPA, { method: 'HEAD' })
      .then((r) => {
        const tipo = r.headers.get('content-type') || ''
        // Vite responde index.html (text/html) cuando el archivo no existe
        if (!r.ok || tipo.includes('text/html')) throw new Error('sin mapa')
        if (cancelado) return
        mapa = new maplibregl.Map({
          container: contenedor.current,
          style: crearEstilo(`pmtiles://${window.location.origin}${ARCHIVO_MAPA}`),
          center: CENTRO_SANTO_DOMINGO,
          zoom: 13,
          minZoom: 10,
          maxZoom: 18,
        })
        mapa.addControl(new maplibregl.NavigationControl(), 'top-right')
        mapa.addControl(new maplibregl.ScaleControl({ unit: 'metric' }))
        mapaRef.current = mapa
        setListo(true)

        // Posiciones conocidas hasta ahora; las nuevas llegan por WebSocket
        fetch('/api/camiones/posiciones')
          .then((res) => (res.ok ? res.json() : []))
          .then((lista) =>
            setCamiones((actual) => {
              const copia = { ...actual }
              lista.forEach((p) => {
                if (!copia[p.placa]) copia[p.placa] = p
              })
              return copia
            }),
          )
          .catch(() => {})
      })
      .catch(() => !cancelado && setFalta(true))

    const marcadoresActuales = marcadores.current
    return () => {
      cancelado = true
      marcadoresActuales.forEach((m) => m.remove())
      marcadoresActuales.clear()
      mapaRef.current = null
      if (mapa) mapa.remove()
      maplibregl.removeProtocol('pmtiles')
    }
  }, [])

  // Reloj para saber qué camiones dejaron de reportar
  useEffect(() => {
    const id = setInterval(() => setAhora(Date.now()), 1000)
    return () => clearInterval(id)
  }, [])

  // Crear o mover el marcador de cada camión
  useEffect(() => {
    const mapa = mapaRef.current
    if (!listo || !mapa) return
    Object.values(camiones).forEach((p) => {
      let marcador = marcadores.current.get(p.placa)
      if (!marcador) {
        marcador = new maplibregl.Marker({ element: crearElementoCamion(p) }).setLngLat([p.lon, p.lat]).addTo(mapa)
        marcadores.current.set(p.placa, marcador)
      } else {
        marcador.setLngLat([p.lon, p.lat])
      }
      const elemento = marcador.getElement()
      elemento.classList.toggle('marcador-sin-senal', ahora - p.recibidoEnMs > SIN_SENAL_MS)
      elemento.classList.toggle('marcador-celular', p.origen === 'celular')
    })
  }, [camiones, ahora, listo])

  const centrarEn = (p) => mapaRef.current?.flyTo({ center: [p.lon, p.lat], zoom: 16 })

  if (falta) {
    return (
      <div className="mapa-faltante">
        <h2>Falta el mapa de Santo Domingo</h2>
        <p>Genéralo una sola vez (con internet) desde la carpeta <code>mapa</code>:</p>
        <pre>powershell -ExecutionPolicy Bypass -File .\generar-mapa.ps1</pre>
        <p>Después recarga esta página. A partir de ahí funciona sin internet.</p>
      </div>
    )
  }

  const lista = Object.values(camiones).sort((a, b) => a.placa.localeCompare(b.placa))
  const enVivo = lista.filter((p) => ahora - p.recibidoEnMs <= SIN_SENAL_MS).length

  return (
    <div className="mapa-envoltura">
      <div ref={contenedor} className="mapa" />
      <aside className="panel-flota">
        <div className="flota-cabecera">
          <span className={`punto-vivo ${conexion === 'conectado' ? 'vivo' : ''}`} />
          <strong>
            {enVivo} {enVivo === 1 ? 'camión' : 'camiones'} en vivo
          </strong>
        </div>
        <p className="texto-suave">
          {conexion === 'conectado' ? 'WebSocket conectado' : 'Reconectando WebSocket...'}
          {retrasoMs !== null && conexion === 'conectado' && ` · retraso ${retrasoMs} ms`}
        </p>
        {lista.length === 0 && (
          <p className="texto-suave flota-vacia">
            Sin camiones todavía. Ejecuta el simulador:
            <code>java simulador/SimuladorGps.java</code>
          </p>
        )}
        {lista.map((p) => {
          const segundos = Math.max(0, Math.round((ahora - p.recibidoEnMs) / 1000))
          const sinSenal = ahora - p.recibidoEnMs > SIN_SENAL_MS
          return (
            <button key={p.placa} className="fila-camion" onClick={() => centrarEn(p)}>
              <span className={`icono-camion ${sinSenal ? 'apagado' : ''}`}>
                <SvgCamion />
              </span>
              <span className="info-camion">
                <strong>{p.placa}</strong>
                <span className="texto-suave">
                  {p.origen === 'celular' ? 'Celular' : 'GPS'} · {Math.round(p.velocidadKmh)} km/h
                </span>
              </span>
              <span className={`texto-suave ${sinSenal ? 'sin-senal' : ''}`}>
                {sinSenal ? 'sin señal' : `hace ${segundos} s`}
              </span>
            </button>
          )
        })}
      </aside>
    </div>
  )
}

function crearElementoCamion(p) {
  const el = document.createElement('div')
  el.className = 'marcador-camion'
  el.innerHTML = `<svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true">
      <path d="M2 6h12v9H2z M14 9h4l3 4v2h-7z" fill="#fff"/>
      <circle cx="6" cy="17" r="2" fill="#fff"/><circle cx="17" cy="17" r="2" fill="#fff"/></svg>`
  const etiqueta = document.createElement('span')
  etiqueta.textContent = p.placa
  el.appendChild(etiqueta)
  return el
}

function SvgCamion() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true">
      <path d="M2 6h12v9H2z M14 9h4l3 4v2h-7z" fill="currentColor" />
      <circle cx="6" cy="17" r="2" fill="currentColor" />
      <circle cx="17" cy="17" r="2" fill="currentColor" />
    </svg>
  )
}
