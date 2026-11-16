// Conexión WebSocket única para toda la aplicación (tema 1.4).
// - Se reconecta sola si se cae, esperando cada vez más (0,5 s, 1 s, 2 s... hasta 10 s).
// - Cada pantalla se suscribe con useEventos(funcion) y recibe los mensajes del servidor.
import { useEffect, useRef, useState } from 'react'

let socket = null
let estado = 'conectando' // 'conectando' | 'conectado' | 'desconectado'
let intentos = 0
let temporizador = null
const oyentes = new Set()
const oyentesEstado = new Set()

function direccion() {
  const protocolo = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${protocolo}://${window.location.host}/ws/eventos`
}

function cambiarEstado(nuevo) {
  estado = nuevo
  oyentesEstado.forEach((f) => f(nuevo))
}

function conectar() {
  if (socket && (socket.readyState === WebSocket.CONNECTING || socket.readyState === WebSocket.OPEN)) return
  cambiarEstado('conectando')
  socket = new WebSocket(direccion())
  socket.onopen = () => {
    intentos = 0
    cambiarEstado('conectado')
  }
  socket.onmessage = (evento) => {
    let mensaje
    try {
      mensaje = JSON.parse(evento.data)
    } catch {
      return
    }
    oyentes.forEach((f) => f(mensaje))
  }
  socket.onclose = () => {
    cambiarEstado('desconectado')
    clearTimeout(temporizador)
    const espera = Math.min(10000, 500 * 2 ** intentos++)
    temporizador = setTimeout(conectar, espera)
  }
  socket.onerror = () => socket.close()
}

/** Envía un objeto al servidor. Devuelve false si no hay conexión en este momento. */
export function enviar(objeto) {
  if (socket?.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify(objeto))
    return true
  }
  return false
}

/**
 * Llama a `accion` cuando llega un mensaje del `tipo` indicado, pero agrupando:
 * si llegan 20 avisos seguidos, ejecuta la acción una sola vez (300 ms después del último).
 */
export function useAlRecibir(tipo, accion, esperaMs = 300) {
  const temporizadorLocal = useRef(null)
  useEffect(() => () => clearTimeout(temporizadorLocal.current), [])
  return useEventos((m) => {
    if (m.tipo !== tipo) return
    clearTimeout(temporizadorLocal.current)
    temporizadorLocal.current = setTimeout(accion, esperaMs)
  })
}

/** Suscribe la pantalla a los mensajes del servidor. Devuelve el estado de la conexión. */
export function useEventos(alRecibir) {
  const funcion = useRef(alRecibir)
  funcion.current = alRecibir
  const [estadoActual, setEstadoActual] = useState(estado)

  useEffect(() => {
    const oyente = (m) => funcion.current?.(m)
    oyentes.add(oyente)
    oyentesEstado.add(setEstadoActual)
    conectar()
    setEstadoActual(estado)
    return () => {
      oyentes.delete(oyente)
      oyentesEstado.delete(setEstadoActual)
    }
  }, [])

  return estadoActual
}
