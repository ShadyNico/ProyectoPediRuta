import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// El proxy evita problemas de CORS: /api y /ws van al backend Spring Boot.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: true, // permite abrirlo desde el celular en la misma red (Sprint 2)
    proxy: {
      '/api': 'http://localhost:8080',
      '/ws': { target: 'ws://localhost:8080', ws: true },
    },
  },
})
