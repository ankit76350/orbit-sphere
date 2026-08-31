import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

/**
 * The battleground runs on its own port, separate from the school application in ../frontend.
 *
 * The backend is on port 3456. Sending the API paths through this dev server means the browser
 * sees same-origin calls, so there is no CORS to deal with and every response header can be
 * read — which the tester needs, because showing the headers is half the point.
 *
 * The tester can also be pointed straight at http://localhost:3456 instead. That needs the
 * dev CORS config on the backend, and the browser will hide some headers.
 */
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 1300,
    host: '0.0.0.0',
    proxy: {
      // Anchored to a whole path segment, so a page in this app whose name merely starts
      // with one of these words is not sent to the backend by mistake.
      '^/platform($|/)': { target: 'http://localhost:3456', changeOrigin: true },
      '^/schools($|/)': { target: 'http://localhost:3456', changeOrigin: true },
      '^/api/': { target: 'http://localhost:3456', changeOrigin: true },
    },
  },
});
