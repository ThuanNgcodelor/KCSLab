import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve(import.meta.dirname, '..');
const env = Object.fromEntries(readFileSync(resolve(root, '.env'), 'utf8')
  .split(/\r?\n/)
  .filter((line) => /^[A-Za-z_][A-Za-z0-9_]*=/.test(line))
  .map((line) => {
    const separator = line.indexOf('=');
    return [line.slice(0, separator), line.slice(separator + 1)];
  }));
const base = process.env.AUTH_SMOKE_BASE ?? 'http://127.0.0.1:8081/api/v1';
const jar = new Map();

function cookieHeader() {
  return [...jar].map(([name, value]) => `${name}=${value}`).join('; ');
}

function acceptCookies(response) {
  const values = response.headers.getSetCookie?.() ?? [];
  for (const value of values) {
    const [pair] = value.split(';');
    const separator = pair.indexOf('=');
    jar.set(pair.slice(0, separator), pair.slice(separator + 1));
  }
  return values;
}

async function call(path, options = {}) {
  const response = await fetch(`${base}${path}`, {
    ...options,
    headers: { Cookie: cookieHeader(), ...(options.headers ?? {}) },
  });
  const cookies = acceptCookies(response);
  return { response, cookies };
}

const csrfCall = await call('/auth/csrf');
const csrf = (await csrfCall.response.json()).token;
const login = async (rememberMe) => call('/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': csrf, Origin: 'http://localhost:5173' },
  body: JSON.stringify({ email: env.ADMIN_EMAIL, password: env.ADMIN_PASSWORD, rememberMe }),
});

const remembered = await login(true);
if (remembered.response.status !== 200) throw new Error(`Remembered login failed: ${remembered.response.status}`);
const persistentAccess = remembered.cookies.some((value) => /^access_token=.*Max-Age=3/i.test(value));
const persistentRefresh = remembered.cookies.some((value) => /^refresh_token=.*Max-Age=2592000/i.test(value));
const oldRefreshToken = jar.get('refresh_token');
jar.set('access_token', 'invalid-access-token');
const unauthorized = await call('/auth/me');
const refresh = await call('/auth/refresh', { method: 'POST' });
const restored = await call('/auth/me');
const reused = await fetch(`${base}/auth/refresh`, {
  method: 'POST',
  headers: { Cookie: `refresh_token=${oldRefreshToken}` },
});
await call('/auth/logout', { method: 'POST', headers: { 'X-XSRF-TOKEN': csrf } });

const session = await login(false);
const sessionAccess = session.cookies.some((value) => /^access_token=/i.test(value) && !/Max-Age=/i.test(value));
const sessionRefresh = session.cookies.some((value) => /^refresh_token=/i.test(value) && !/Max-Age=/i.test(value));
await call('/auth/logout', { method: 'POST', headers: { 'X-XSRF-TOKEN': csrf } });

const result = {
  rememberedLogin: remembered.response.status,
  persistentAccess,
  persistentRefresh,
  meWithInvalidAccess: unauthorized.response.status,
  refresh: refresh.response.status,
  meAfterRefresh: restored.response.status,
  reusedRefreshToken: reused.status,
  browserSessionLogin: session.response.status,
  sessionAccess,
  sessionRefresh,
};
if (Object.values(result).some((value) => value === false)
    || unauthorized.response.status !== 401 || refresh.response.status !== 200
    || restored.response.status !== 200 || reused.status !== 401) {
  throw new Error(JSON.stringify(result));
}
process.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
