import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'path'

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: resolve(__dirname, '../src/main/resources/static'),
    emptyOutDir: false
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/oauth2': 'http://localhost:8080'
    }
  }
})
