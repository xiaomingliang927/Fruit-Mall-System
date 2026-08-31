import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5174,
    proxy: {
      '/api': 'http://localhost:8080',
      '/images': 'http://localhost:8080',
      '/uploads': 'http://localhost:8080',
    },
  },
})
