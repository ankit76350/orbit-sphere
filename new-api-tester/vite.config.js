import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

/**
 * Its own port, so this and ../api-battleground (1300) can run side by side.
 *
 * The API paths are proxied to the backend on 3456. Going through the dev server rather than
 * straight at the backend means no CORS, and the browser lets the page read every response
 * header — which matters, because `Location` is where a newly created id comes back.
 */
export default defineConfig({
  plugins: [react()],
  server: {
    port: 1400,
    // Fail rather than quietly starting a second copy on 1401. Two dev servers on two ports
    // means two tabs both loading, which reads in the log as the app asking twice for everything.
    strictPort: true,
    proxy: {
      // Anchored to a whole path segment, so a route in this app whose name merely starts with
      // one of these words is not sent to the backend by mistake.
      '^/platform($|/)': { target: 'http://localhost:3456', changeOrigin: true },
      '^/schools($|/)': { target: 'http://localhost:3456', changeOrigin: true },
    },
  },
})
