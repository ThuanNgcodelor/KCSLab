import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { createReadStream, cpSync, existsSync, statSync } from 'node:fs';
import { extname, join, normalize, resolve } from 'node:path';

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

// Public lookup landing (source/public/tracuu) is served at /tracuu/ by the same origin as the app,
// so the QR printed on the PDF (PUBLIC_LOOKUP_URL, e.g. http://192.168.1.132:5173/tracuu/) works on the LAN
// and its relative /api calls go through the proxy below. `vite build` copies it to dist/tracuu.
const landingRoot = resolve(__dirname, '../public/tracuu');
const landingTypes = { '.html': 'text/html; charset=utf-8', '.css': 'text/css; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.png': 'image/png', '.webp': 'image/webp', '.svg': 'image/svg+xml' };
// Only the public lookup landing (and the files crawlers need to discover it) may be indexed.
// Everything else - staff login/workspace, internal API, WebSocket - is sent with noindex.
function isIndexablePath(path) {
  return path === '/tracuu' || path.startsWith('/tracuu/') || path === '/robots.txt' || path === '/sitemap.xml' || path.startsWith('/favicon');
}
function noindexInternal(req, res, next) {
  const path = (req.url || '').split('?')[0];
  if (!isIndexablePath(path)) res.setHeader('X-Robots-Tag', 'noindex, nofollow');
  next();
}
function cacheControlFor(file) {
  const type = extname(file);
  if (type === '.html') return 'public, max-age=0, must-revalidate';
  if (type === '.css' || type === '.js') return 'public, max-age=3600';
  return 'public, max-age=86400';
}
function serveLanding(req, res, next) {
  const path = (req.url || '').split('?')[0];
  if (path === '/tracuu') { res.statusCode = 301; res.setHeader('Location', '/tracuu/' + (req.url.includes('?') ? req.url.slice(req.url.indexOf('?')) : '')); res.end(); return; }
  if (!path.startsWith('/tracuu/')) return next();
  let relative = decodeURIComponent(path.slice('/tracuu/'.length)) || 'index.html';
  const file = normalize(join(landingRoot, relative));
  if (!file.startsWith(landingRoot) || !existsSync(file) || !statSync(file).isFile()) { res.statusCode = 404; res.end('Not found'); return; }
  res.setHeader('Content-Type', landingTypes[extname(file)] || 'application/octet-stream');
  // The landing page is the only SEO-indexed page: it must NOT send X-Robots-Tag (a noindex header overrides the
  // <meta robots="index"> in the HTML). Cache so crawlers/Cloudflare can reuse it; HTML is always revalidated.
  res.setHeader('Cache-Control', cacheControlFor(file));
  res.setHeader('Referrer-Policy', 'no-referrer');
  createReadStream(file).pipe(res);
}
const publicLanding = {
  name: 'cfc-public-landing',
  configureServer(server) { server.middlewares.use(noindexInternal); server.middlewares.use(serveLanding); },
  configurePreviewServer(server) { server.middlewares.use(noindexInternal); server.middlewares.use(serveLanding); },
  closeBundle() { if (existsSync(landingRoot)) cpSync(landingRoot, resolve(__dirname, 'dist/tracuu'), { recursive: true }); },
};

export default defineConfig({
  plugins: [react(), publicLanding],
  server: {
    host: '0.0.0.0',
    port: 5173,
    allowedHosts: ['lab.cfc.io.vn'],
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        xfwd: true, // adds X-Forwarded-For; backend reads it when PUBLIC_CLIENT_IP_HEADER=X-Forwarded-For
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
