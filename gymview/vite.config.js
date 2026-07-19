import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      '/api/login': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        ws: true,
      },
      '/api/member': {
        target: 'http://localhost:8084',
        changeOrigin: true,
        ws: true,
      },
      '/api/employee': {
        target: 'http://localhost:8084',
        changeOrigin: true,
        ws: true,
      },
      '/api/equipment': {
        target: 'http://localhost:8084',
        changeOrigin: true,
        ws: true,
      },
      '/api/class': {
        target: 'http://localhost:8084',
        changeOrigin: true,
        ws: true,
      },
      '/api/order': {
        target: 'http://localhost:8084',
        changeOrigin: true,
        ws: true,
      },
      '/api/admin': {
        target: 'http://localhost:8084',
        changeOrigin: true,
        ws: true,
      },
    },
  },
})
