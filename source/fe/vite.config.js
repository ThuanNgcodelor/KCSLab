import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// A browser tab closing or reloading while a WebSocket/HTTP proxy is writing raises EPIPE/ECONNRESET.
// These are harmless in development, so they are swallowed instead of flooding the terminal.
const harmless = new Set(['EPIPE', 'ECONNRESET', 'ECONNABORTED']);
function quietProxy(proxy) {
  proxy.on('error', (error, _request, target) => {
    if (harmless.has(error.code)) return;
    console.error('[proxy]', error.message);
    if (target && typeof target.end === 'function' && !target.writableEnded) target.end();
  });
  proxy.on('proxyReqWs', (_proxyReq, _req, socket) => socket.on('error', () => {}));
  proxy.on('open', (socket) => socket.on('error', () => {}));
}

export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        configure: quietProxy,
      },
      '/ws': {
        target: 'ws://127.0.0.1:8080',
        ws: true,
        configure: quietProxy,
      },
    },
  },
});
