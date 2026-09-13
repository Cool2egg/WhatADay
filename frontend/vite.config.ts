import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { fileURLToPath, URL } from 'node:url'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    // 开发环境下将 /api 代理到后端，避免跨域
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    // 把体积大且更新频率低的依赖拆成独立的 vendor 包：
    // 版本没变时浏览器可以直接命中缓存，不必随业务代码重新下载。
    rollupOptions: {
      output: {
        manualChunks: {
          react: ['react', 'react-dom', 'react-router-dom'],
          antd: ['antd', '@ant-design/icons'],
          echarts: ['echarts', 'echarts-for-react'],
        },
      },
    },
    // ECharts 的 vendor 包约 1MB（gzip 后约 350KB），antd 约 900KB（gzip 后约 286KB）。
    // 这两者都是引入完整库的固有成本：echarts 的 exports 只映射到 .js、没有配套类型，
    // 子路径按需引入在 bundler 解析模式下拿不到类型，因此暂不进一步裁剪。
    // 已通过 manualChunks 把它们拆成独立 vendor 包，版本不变时可长期命中浏览器缓存。
    chunkSizeWarningLimit: 1100,
  },
})
