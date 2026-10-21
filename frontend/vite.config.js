import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // During development the React app runs on port 5173 and Spring Boot on 8080.
    // The dev server forwards every /api request to Spring Boot, so the browser only
    // ever talks to one origin (no CORS problems).
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
