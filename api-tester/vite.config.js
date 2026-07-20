import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

// Dev server proxies /api to the Spring backend so the app can call it
// same-origin (no CORS setup needed on the backend).
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 9876,
    proxy: {
      '/api': { target: 'http://localhost:3456', changeOrigin: true },
    },
  },
});
