import autoprefixer from "autoprefixer"
import { defineConfig } from "vite"
import path from "path"
import tailwind from "tailwindcss"
import vue from "@vitejs/plugin-vue"

export default defineConfig({
  server: {
    port: 3000,
    host: true,
    proxy: {
      '/api': {
        target: process.env.VITE_BACKEND_URL || 'http://localhost:9090',
        changeOrigin: true,
      },
    },
  },
  css: {
    postcss: {
      plugins: [tailwind(), autoprefixer()],
    },
  },
  plugins: [vue()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
})
