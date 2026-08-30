import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  server: {
    // H5 预览端口（npm run dev:h5），5173 已被用户端网站占用
    port: 5175,
  },
})
