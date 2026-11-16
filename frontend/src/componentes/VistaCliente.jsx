import { useEffect, useMemo, useState } from 'react'
import { api, dinero, guardarLocal, leerLocal } from '../api.js'
import { useAlRecibir } from '../tiemporeal.js'

// Historias H03 y H04: el cliente ve el catálogo con stock real y hace su pedido.
export default function VistaCliente() {
  const [productos, setProductos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [errorCarga, setErrorCarga] = useState(null)
  const [categoria, setCategoria] = useState('Todos')
  const [busqueda, setBusqueda] = useState('')
  const [carrito, setCarrito] = useState({}) // { productoId: cantidad }
  const [cliente, setCliente] = useState(() => leerLocal('pediruta.cliente', ''))
  const [direccion, setDireccion] = useState(() => leerLocal('pediruta.direccion', ''))
  const [enviando, setEnviando] = useState(false)
  const [aviso, setAviso] = useState(null) // { tipo: 'ok' | 'error', texto }

  const cargar = async () => {
    try {
      setProductos(await api.productos())
      setErrorCarga(null)
    } catch (e) {
      setErrorCarga(e.message)
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    cargar()
  }, [])

  // Si otro cliente compra o se cancela un pedido, el stock se actualiza solo
  useAlRecibir('pedido', cargar)

  const categorias = useMemo(
    () => ['Todos', ...new Set(productos.map((p) => p.categoria))],
    [productos],
  )

  const visibles = productos.filter(
    (p) =>
      (categoria === 'Todos' || p.categoria === categoria) &&
      p.nombre.toLowerCase().includes(busqueda.trim().toLowerCase()),
  )

  const lineas = Object.entries(carrito)
    .map(([id, cantidad]) => ({ producto: productos.find((p) => p.id === Number(id)), cantidad }))
    .filter((l) => l.producto)
  const total = lineas.reduce((s, l) => s + l.producto.precio * l.cantidad, 0)
  const peso = lineas.reduce((s, l) => s + l.producto.pesoKg * l.cantidad, 0)

  const cambiarCantidad = (producto, delta) => {
    setAviso(null)
    setCarrito((actual) => {
      const nueva = Math.max(0, Math.min(producto.stock, (actual[producto.id] || 0) + delta))
      const copia = { ...actual }
      if (nueva === 0) delete copia[producto.id]
      else copia[producto.id] = nueva
      return copia
    })
  }

  const puedeConfirmar = lineas.length > 0 && cliente.trim() && direccion.trim() && !enviando

  const confirmar = async () => {
    setEnviando(true)
    setAviso(null)
    guardarLocal('pediruta.cliente', cliente)
    guardarLocal('pediruta.direccion', direccion)
    try {
      const pedido = await api.crearPedido({
        cliente,
        direccion,
        items: lineas.map((l) => ({ productoId: l.producto.id, cantidad: l.cantidad })),
      })
      setCarrito({})
      setAviso({ tipo: 'ok', texto: `Pedido #${pedido.id} enviado a bodega · ${dinero(pedido.total)}` })
    } catch (e) {
      setAviso({ tipo: 'error', texto: e.message })
    } finally {
      setEnviando(false)
      cargar() // el stock cambió: se vuelve a pedir
    }
  }

  return (
    <div className="vista-dos-columnas">
      <section className="panel-principal">
        <div className="encabezado-vista">
          <div>
            <h2>Catálogo</h2>
            <p className="texto-suave">Precios y stock en tiempo real de la bodega</p>
          </div>
          <label className="campo campo-busqueda">
            <span>Buscar producto</span>
            <input
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              placeholder="Ej.: agua, cola, galletas"
            />
          </label>
        </div>

        <div className="chips">
          {categorias.map((c) => (
            <button
              key={c}
              className={`chip ${categoria === c ? 'chip-activo' : ''}`}
              onClick={() => setCategoria(c)}
            >
              {c}
            </button>
          ))}
        </div>

        {cargando && <p className="texto-suave">Cargando productos...</p>}
        {errorCarga && <div className="aviso aviso-error">{errorCarga}</div>}

        <div className="grilla-productos">
          {visibles.map((p) => {
            const enCarrito = carrito[p.id] || 0
            const agotado = p.stock === 0
            return (
              <article key={p.id} className={`tarjeta-producto ${agotado ? 'agotado' : ''}`}>
                <div className="imagen-producto">
                  <IconoCaja />
                </div>
                <h3>{p.nombre}</h3>
                <p className="texto-suave">
                  {p.presentacion} · {Number(p.pesoKg)} kg
                </p>
                <div className="fila-precio">
                  <strong>{dinero(p.precio)}</strong>
                  <span className={`stock ${agotado ? 'stock-cero' : p.stock < 10 ? 'stock-bajo' : ''}`}>
                    {agotado ? 'Agotado' : `Stock: ${p.stock}`}
                  </span>
                </div>
                {enCarrito === 0 ? (
                  <button className="boton boton-secundario" disabled={agotado} onClick={() => cambiarCantidad(p, 1)}>
                    {agotado ? 'Sin stock' : 'Agregar'}
                  </button>
                ) : (
                  <Contador
                    cantidad={enCarrito}
                    maximo={p.stock}
                    onMenos={() => cambiarCantidad(p, -1)}
                    onMas={() => cambiarCantidad(p, 1)}
                  />
                )}
              </article>
            )
          })}
        </div>
      </section>

      <aside className="panel-lateral">
        <h2>Tu pedido</h2>
        {lineas.length === 0 && <p className="texto-suave">Agrega productos del catálogo.</p>}
        {lineas.map(({ producto, cantidad }) => (
          <div key={producto.id} className="linea-carrito">
            <div>
              <strong>{producto.nombre}</strong>
              <span className="texto-suave">{dinero(producto.precio * cantidad)}</span>
            </div>
            <Contador
              cantidad={cantidad}
              maximo={producto.stock}
              onMenos={() => cambiarCantidad(producto, -1)}
              onMas={() => cambiarCantidad(producto, 1)}
            />
          </div>
        ))}
        <hr />
        <div className="fila-total">
          <span>Peso total</span>
          <span>{peso.toFixed(0)} kg</span>
        </div>
        <div className="fila-total fila-total-grande">
          <span>Total</span>
          <strong>{dinero(total)}</strong>
        </div>

        <label className="campo">
          <span>Negocio</span>
          <input value={cliente} onChange={(e) => setCliente(e.target.value)} placeholder="Tienda La Esquina" />
        </label>
        <label className="campo">
          <span>Dirección de entrega</span>
          <input
            value={direccion}
            onChange={(e) => setDireccion(e.target.value)}
            placeholder="Av. Quito y Río Toachi"
          />
        </label>

        {aviso && <div className={`aviso aviso-${aviso.tipo}`}>{aviso.texto}</div>}

        <div className="espacio" />
        <button className="boton boton-primario" disabled={!puedeConfirmar} onClick={confirmar}>
          {enviando ? 'Enviando...' : 'Confirmar pedido'}
        </button>
      </aside>
    </div>
  )
}

function Contador({ cantidad, maximo, onMenos, onMas }) {
  return (
    <div className="contador">
      <button onClick={onMenos} aria-label="Quitar uno">−</button>
      <span>{cantidad}</span>
      <button onClick={onMas} disabled={cantidad >= maximo} aria-label="Agregar uno">+</button>
    </div>
  )
}

function IconoCaja() {
  return (
    <svg width="44" height="44" viewBox="0 0 48 48" fill="none" aria-hidden="true">
      <path d="M8 16 L24 8 L40 16 V34 L24 42 L8 34 Z" stroke="#1E3A5F" strokeWidth="2.5" strokeLinejoin="round" />
      <path d="M8 16 L24 24 L40 16 M24 24 V42" stroke="#1E3A5F" strokeWidth="2.5" />
    </svg>
  )
}
