import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    // フロント(:5173) と GraphQL サーバ(:8888) はポートが違うので
    // そのまま fetch すると別オリジン扱いになり CORS で弾かれる。
    // ここで中継することで、ブラウザからは同一オリジンに見える。
    proxy: {
      '/api': 'http://localhost:8888',
    },
  },
})
