import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({

  plugins: [
    vue(),
    ],
  server: {
    port: 5173,   //设定前端运行的端口
    open: true,
  },

  base: './'
})
