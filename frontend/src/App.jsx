import { useState } from 'react'
import IndicadorSalud from './componentes/IndicadorSalud.jsx'
import Mapa from './componentes/Mapa.jsx'
import VistaCliente from './componentes/VistaCliente.jsx'
import VistaBodega from './componentes/VistaBodega.jsx'
import VistaChofer from './componentes/VistaChofer.jsx'
import { guardarLocal, leerLocal } from './api.js'

// Hasta tener login (Unidad 2), se cambia de rol con estas pestañas.
const VISTAS = [
  { id: 'cliente', nombre: 'Cliente' },
  { id: 'bodega', nombre: 'Bodega' },
  { id: 'chofer', nombre: 'Chofer' },
  { id: 'mapa', nombre: 'Mapa' },
]

export default function App() {
  const [vista, setVista] = useState(() => leerLocal('pediruta.vista', 'cliente'))

  const cambiarVista = (id) => {
    setVista(id)
    guardarLocal('pediruta.vista', id)
  }

  return (
    <div className="app">
      <header className="cabecera">
        <h1>PediRuta</h1>
        <nav className="pestanas">
          {VISTAS.map((v) => (
            <button
              key={v.id}
              className={`pestana ${vista === v.id ? 'pestana-activa' : ''}`}
              onClick={() => cambiarVista(v.id)}
            >
              {v.nombre}
            </button>
          ))}
        </nav>
        <IndicadorSalud />
      </header>
      <main className={`contenido ${vista === 'mapa' ? 'contenido-mapa' : ''}`}>
        {vista === 'cliente' && <VistaCliente />}
        {vista === 'bodega' && <VistaBodega />}
        {vista === 'chofer' && <VistaChofer />}
        {vista === 'mapa' && <Mapa />}
      </main>
    </div>
  )
}
