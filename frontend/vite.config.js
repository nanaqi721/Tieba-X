import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 前端开发服务器配置
// 统一入口是网关（gateway，端口 8080），按服务名分流到各服务
// /api 前缀代理到 8080，注意不要 rewrite 去掉 /api（后端接口路径本身带 /api）
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
