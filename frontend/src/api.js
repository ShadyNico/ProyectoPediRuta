// Llamadas al backend. Si algo falla, lanza un Error con el mensaje del servidor
// (que viene del ManejadorErrores de Spring: { codigo, mensaje, detalles }).

async function pedir(url, opciones = {}) {
  let respuesta
  try {
    respuesta = await fetch(url, {
      ...opciones,
      headers: { 'Content-Type': 'application/json', ...(opciones.headers || {}) },
    })
  } catch {
    throw new Error('No hay conexión con el servidor')
  }
  const datos = respuesta.status === 204 ? null : await respuesta.json().catch(() => null)
  if (!respuesta.ok) {
    const detalle = datos?.detalles?.length ? ` (${datos.detalles.join('; ')})` : ''
    throw new Error((datos?.mensaje || `Error ${respuesta.status}`) + detalle)
  }
  return datos
}

export const api = {
  productos: () => pedir('/api/productos'),
  crearPedido: (pedido) => pedir('/api/pedidos', { method: 'POST', body: JSON.stringify(pedido) }),
  pedidos: (estados) => pedir('/api/pedidos' + (estados ? `?estado=${estados.join(',')}` : '')),
  cambiarEstado: (id, estado) =>
    pedir(`/api/pedidos/${id}/estado`, { method: 'PATCH', body: JSON.stringify({ estado }) }),
}

export const dinero = (n) => `$ ${Number(n).toFixed(2).replace('.', ',')}`

export const hora = (fechaIso) =>
  new Date(fechaIso).toLocaleTimeString('es-EC', { hour: '2-digit', minute: '2-digit' })

// localStorage puede fallar (modo privado, vista previa): nunca debe romper la página
export function leerLocal(clave, porDefecto) {
  try {
    return localStorage.getItem(clave) ?? porDefecto
  } catch {
    return porDefecto
  }
}

export function guardarLocal(clave, valor) {
  try {
    localStorage.setItem(clave, valor)
  } catch {
    /* sin almacenamiento: no pasa nada */
  }
}
