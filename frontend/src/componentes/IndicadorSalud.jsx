import { useEffect, useState } from 'react'

// Historia H01: muestra si el backend y PostGIS responden.
// Consulta /api/salud cada 10 segundos.
export default function IndicadorSalud() {
  const [estado, setEstado] = useState({ tipo: 'cargando', texto: 'Comprobando...' })

  useEffect(() => {
    let activo = true

    async function consultar() {
      try {
        const respuesta = await fetch('/api/salud')
        if (!respuesta.ok) throw new Error(`HTTP ${respuesta.status}`)
        const datos = await respuesta.json()
        if (!activo) return
        if (datos.baseDeDatos === 'conectada') {
          setEstado({ tipo: 'ok', texto: `Backend y PostGIS conectados (PostGIS ${datos.postgis.split(' ')[0]})` })
        } else {
          setEstado({ tipo: 'aviso', texto: 'Backend activo, base de datos sin conexión' })
        }
      } catch {
        if (activo) setEstado({ tipo: 'error', texto: 'Backend sin respuesta' })
      }
    }

    consultar()
    const intervalo = setInterval(consultar, 10000)
    return () => {
      activo = false
      clearInterval(intervalo)
    }
  }, [])

  return (
    <div className={`indicador indicador-${estado.tipo}`}>
      <span className="punto" />
      {estado.texto}
    </div>
  )
}
