import { useEffect, useState } from 'react'
import { api, dinero, hora } from '../api.js'
import { useAlRecibir } from '../tiemporeal.js'

// Historia H06: la bodega ve los pedidos nuevos y los prepara.
// Sprint 2: los cambios llegan al instante por WebSocket. Si el WebSocket se cae,
// igual se consulta cada 30 s como respaldo (la red no es confiable).
const ESTADOS_ACTIVOS = ['PENDIENTE', 'PREPARANDO', 'LISTO']
const INTERVALO_MS = 30000

const ESTADO = {
  PENDIENTE: { texto: 'Pendiente', clase: 'insignia-pendiente' },
  PREPARANDO: { texto: 'Preparando', clase: 'insignia-preparando' },
  LISTO: { texto: 'Listo · espera chofer', clase: 'insignia-listo' },
}

export default function VistaBodega() {
  const [pedidos, setPedidos] = useState([])
  const [error, setError] = useState(null)
  const [seleccionado, setSeleccionado] = useState(null)
  const [marcados, setMarcados] = useState({}) // { pedidoId: Set de productoId }
  const [ocupado, setOcupado] = useState(false)

  const cargar = async () => {
    try {
      setPedidos(await api.pedidos(ESTADOS_ACTIVOS))
      setError(null)
    } catch (e) {
      setError(e.message)
    }
  }

  const conexion = useAlRecibir('pedido', cargar)

  useEffect(() => {
    cargar()
    const id = setInterval(cargar, INTERVALO_MS)
    return () => clearInterval(id)
  }, [])

  const pedido = pedidos.find((p) => p.id === seleccionado) || pedidos[0] || null
  const cuenta = (estado) => pedidos.filter((p) => p.estado === estado).length
  const marcadosDe = (id) => marcados[id] || new Set()

  const alternar = (pedidoId, productoId) =>
    setMarcados((actual) => {
      const conjunto = new Set(actual[pedidoId] || [])
      if (conjunto.has(productoId)) conjunto.delete(productoId)
      else conjunto.add(productoId)
      return { ...actual, [pedidoId]: conjunto }
    })

  const avanzar = async (p, nuevoEstado) => {
    setOcupado(true)
    try {
      await api.cambiarEstado(p.id, nuevoEstado)
      setError(null)
      await cargar()
    } catch (e) {
      setError(e.message)
    } finally {
      setOcupado(false)
    }
  }

  const todoMarcado = pedido && pedido.items.every((i) => marcadosDe(pedido.id).has(i.productoId))

  return (
    <div className="vista-dos-columnas">
      <section className="panel-principal panel-con-lista">
        <div className="encabezado-vista">
          <div>
            <h2>Pedidos por preparar</h2>
            <p className="texto-suave estado-vivo">
              <span className={`punto-vivo ${conexion === 'conectado' ? 'vivo' : ''}`} />
              {conexion === 'conectado'
                ? 'En vivo: los pedidos nuevos aparecen al instante'
                : `Sin conexión en vivo: se revisa cada ${INTERVALO_MS / 1000} s`}
            </p>
          </div>
        </div>

        <div className="metricas">
          <Metrica valor={cuenta('PENDIENTE')} etiqueta="Pendientes" />
          <Metrica valor={cuenta('PREPARANDO')} etiqueta="Preparando" />
          <Metrica valor={cuenta('LISTO')} etiqueta="Listos, esperando chofer" />
        </div>

        {error && <div className="aviso aviso-error">{error}</div>}

        <div className="lista-pedidos">
          {pedidos.length === 0 && <p className="texto-suave lista-vacia">No hay pedidos activos.</p>}
          {pedidos.map((p) => (
            <button
              key={p.id}
              className={`fila-pedido ${pedido?.id === p.id ? 'fila-seleccionada' : ''}`}
              onClick={() => setSeleccionado(p.id)}
            >
              <span className="numero-pedido">#{p.id}</span>
              <span className="info-pedido">
                <strong>{p.cliente}</strong>
                <span className="texto-suave">
                  {p.items.length} {p.items.length === 1 ? 'producto' : 'productos'} · {Number(p.pesoKg)} kg ·{' '}
                  {p.direccion}
                </span>
              </span>
              <span className="texto-suave">{hora(p.creadoEn)}</span>
              <span className={`insignia ${ESTADO[p.estado].clase}`}>{ESTADO[p.estado].texto}</span>
            </button>
          ))}
        </div>
      </section>

      <aside className="panel-lateral">
        {!pedido ? (
          <p className="texto-suave">Selecciona un pedido para ver su detalle.</p>
        ) : (
          <>
            <div className="fila-total">
              <h2>Pedido #{pedido.id}</h2>
              <span className={`insignia ${ESTADO[pedido.estado].clase}`}>{ESTADO[pedido.estado].texto}</span>
            </div>
            <p className="texto-suave">
              {pedido.cliente} · {pedido.direccion}
            </p>
            <h3 className="subtitulo-panel">Lista de preparación</h3>
            {pedido.items.map((i) => (
              <label key={i.productoId} className="item-preparacion">
                <input
                  type="checkbox"
                  disabled={pedido.estado !== 'PREPARANDO'}
                  checked={pedido.estado === 'LISTO' || marcadosDe(pedido.id).has(i.productoId)}
                  onChange={() => alternar(pedido.id, i.productoId)}
                />
                <span>{i.nombre}</span>
                <strong>x {i.cantidad}</strong>
              </label>
            ))}
            <hr />
            <div className="fila-total">
              <span>Peso total</span>
              <span>{Number(pedido.pesoKg)} kg</span>
            </div>
            <div className="fila-total">
              <span>Total</span>
              <strong>{dinero(pedido.total)}</strong>
            </div>

            <div className="espacio" />

            {pedido.estado === 'PENDIENTE' && (
              <>
                <button className="boton boton-primario" disabled={ocupado} onClick={() => avanzar(pedido, 'PREPARANDO')}>
                  Empezar a preparar
                </button>
                <button className="boton boton-enlace" disabled={ocupado} onClick={() => avanzar(pedido, 'CANCELADO')}>
                  Cancelar pedido (devuelve el stock)
                </button>
              </>
            )}
            {pedido.estado === 'PREPARANDO' && (
              <>
                {!todoMarcado && <p className="texto-suave">Marca cada producto a medida que lo cargues.</p>}
                <button
                  className="boton boton-exito"
                  disabled={ocupado || !todoMarcado}
                  onClick={() => avanzar(pedido, 'LISTO')}
                >
                  Marcar pedido como listo
                </button>
              </>
            )}
            {pedido.estado === 'LISTO' && (
              <div className="aviso aviso-info">
                Listo para despacho. En el Sprint 3 el sistema le asignará chofer y camión automáticamente.
              </div>
            )}
          </>
        )}
      </aside>
    </div>
  )
}

function Metrica({ valor, etiqueta }) {
  return (
    <div className="metrica">
      <strong>{valor}</strong>
      <span>{etiqueta}</span>
    </div>
  )
}
