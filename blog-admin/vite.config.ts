import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  // 部署在 /admin/ 子路径下时由构建环境注入 VITE_BASE=/admin/（Dockerfile 默认已设置），
  // 本地 npm run dev / build 不设置时仍为根路径 '/'，开发体验不变
  base: process.env.VITE_BASE || '/',
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3001,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
