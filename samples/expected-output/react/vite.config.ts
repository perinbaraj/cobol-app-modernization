import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Vite config for Customer Inquiry React app
// Converted from CUSTINQ BMS screen map (CUSTINQ.bms)
// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    open: true,
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
  },
})
