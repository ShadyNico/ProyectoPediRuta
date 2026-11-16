import { useEffect, useRef, useState } from 'react'
import { enviar, useEventos } from '../tiemporeal.js'
import { guardarLocal, leerLocal } from '../api.js'

// H08: el chofer comparte su ubicación desde el navegador del celular, sin instalar nada.
// El navegador solo entrega el GPS en https o en localhost (ver docs/sprint2.md).
const MINIMO_ENTRE_ENVIOS_MS = 3000

export default function VistaChofer() {
  const [placa, setPlaca] = useState(() => leerLocal('pediruta.placa', 'PBA-1234'))
  const [compartiendo, setCompartiendo] = useState(false)
  const [ultima, setUltima] = useState(null)
  const [enviados, setEnviados] = useState(0)
  const [error, setError] = useState(null)
  const vigilancia = useRef(null)
  const ultimoEnvio = useRef(0)

  const conexion = useEventos((m) => {
    if (m.tipo === 'error') setError(m.datos.mensaje)
  })

  const detener = () => {
    if (vigilancia.current !== null) navigator.geolocation.clearWatch(vigilancia.current)
    vigilancia.current = null
    setCompartiendo(false)
  }

  useEffect(() => detener, []) // al salir de la pantalla, deja de usar el GPS

  const iniciar = () => {
    setError(null)
    if (!window.isSecureContext) {
      setError('El navegador solo permite el GPS en https o en localhost.')
      return
    }
    if (!('geolocation' in navigator)) {
      setError('Este navegador no tiene GPS.')
      return
    }
    guardarLocal('pediruta.placa', placa.trim().toUpperCase())
    setCompartiendo(true)
    vigilancia.current = navigator.geolocation.watchPosition(
      (pos) => {
        const ahora = Date.now()
        if (ahora - ultimoEnvio.current < MINIMO_ENTRE_ENVIOS_MS) return
        ultimoEnvio.current = ahora
        const datos = {
          tipo: 'posicion',
          placa: placa.trim().toUpperCase(),
          lat: pos.coords.latitude,
          lon: pos.coords.longitude,
          velocidad: Math.max(0, (pos.coords.speed ?? 0) * 3.6),
          enviadoEnMs: ahora,
        }
        setUltima({ ...datos, precision: Math.round(pos.coords.accuracy) })
        if (enviar(datos)) setEnviados((n) => n + 1)
      },
      (e) => {
        setError(e.code === e.PERMISSION_DENIED ? 'Debes permitir el acceso a la ubicación.' : e.message)
        detener()
      },
      { enableHighAccuracy: true, maximumAge: 2000, timeout: 20000 },
    )
  }

  return (
    <div className="vista-chofer">
      <div className="tarjeta-chofer">
        <h2>Compartir mi ubicación</h2>
        <p className="texto-suave">
          Mantén esta pantalla abierta mientras haces la entrega. Tu posición se envía cada {MINIMO_ENTRE_ENVIOS_MS / 1000} s.
        </p>

        <label className="campo">
          <span>Placa del camión</span>
          <input
            value={placa}
            disabled={compartiendo}
            onChange={(e) => setPlaca(e.target.value)}
            placeholder="PBA-1234"
          />
        </label>

        <div className={`estado-gps ${compartiendo ? 'estado-gps-activo' : ''}`}>
          <span className="punto-vivo vivo" />
          <div>
            <strong>{compartiendo ? 'Compartiendo ubicación' : 'Ubicación apagada'}</strong>
            <span className="texto-suave">
              {conexion === 'conectado' ? 'Conectado al servidor' : 'Sin conexión al servidor, reintentando...'}
            </span>
          </div>
        </div>

        {ultima && (
          <div className="datos-gps">
            <span>Lat {ultima.lat.toFixed(5)} · Lon {ultima.lon.toFixed(5)}</span>
            <span>
              Precisión ±{ultima.precision} m · {Math.round(ultima.velocidad)} km/h · {enviados} enviadas
            </span>
          </div>
        )}

        {error && <div className="aviso aviso-error">{error}</div>}

        {compartiendo ? (
          <button className="boton boton-secundario" onClick={detener}>
            Dejar de compartir
          </button>
        ) : (
          <button className="boton boton-exito" disabled={!placa.trim()} onClick={iniciar}>
            Empezar a compartir
          </button>
        )}
      </div>
    </div>
  )
}
