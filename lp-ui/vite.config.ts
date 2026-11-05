/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: Vite 构建配置：别名、开发代理、打包优化
 */

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    open: true,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        ws: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      '/ws': {
        target: 'ws://127.0.0.1:8080',
        changeOrigin: true,
        ws: true
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 2000
  }
})
