import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发期代理：/api 与 /images 都转发到 Spring Boot 后端
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/images': 'http://localhost:8080',
    },
  },
})
