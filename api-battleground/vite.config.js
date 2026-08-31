import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

/**
 * This app runs on its own port, separate from the school application in ../frontend.
 *
 * The backend is on port 3456. Sending the API paths through this dev server means the browser
 * sees same-origin calls, so there is no CORS to deal with and every response header can be
 * read — which matters, because the details pop-up shows them.
 *
 * It can also be pointed straight at http://localhost:3456 instead. That needs the dev CORS
 * config on the backend, and the browser will hide some headers.
 */
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 1300,
    // Fail rather than quietly starting a second copy on 1301. Two dev servers on two ports
    // means two browser tabs both loading, which reads in the log as the app asking for the
    // same thing over and over.
    strictPort: true,
    host: '0.0.0.0',
    watch: {
      // The test runners live in the project root and are not part of the app. Editing one
      // should not reload the page in the browser.
      ignored: ['**/render-test.mjs', '**/e2e.mjs', '**/render-entry.js', '**/.render-bundle.mjs'],
    },
    proxy: {
      // Anchored to a whole path segment, so a page in this app whose name merely starts
      // with one of these words is not sent to the backend by mistake.
      '^/platform($|/)': { target: 'http://localhost:3456', changeOrigin: true },
      '^/schools($|/)': { target: 'http://localhost:3456', changeOrigin: true },
      '^/api/': { target: 'http://localhost:3456', changeOrigin: true },
    },
  },
});
