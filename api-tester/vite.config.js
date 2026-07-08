import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

// Dev server proxies /api to the Spring backend so the app can call it
// same-origin (no CORS setup needed on the backend).
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 3100,
    proxy: {
      '/api': { target: 'http://localhost:5030', changeOrigin: true },
    },
  },
});
