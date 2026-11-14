import { spawn } from 'node:child_process';
import { mkdirSync, mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';

const root = resolve(import.meta.dirname, '..');
const env = Object.fromEntries(readFileSync(join(root, '.env'), 'utf8')
  .split(/\r?\n/)
  .filter((line) => /^[A-Za-z_][A-Za-z0-9_]*=/.test(line))
  .map((line) => {
    const separator = line.indexOf('=');
    return [line.slice(0, separator), line.slice(separator + 1)];
  }));
const port = 9700 + Math.floor(Math.random() * 200);
const profile = mkdtempSync(join(tmpdir(), 'cfc-transfer-responsive-'));
const artifacts = join(root, 'artifacts', 'transfer-responsive');
mkdirSync(artifacts, { recursive: true });
const chrome = spawn('C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe', [
  '--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
  `--remote-debugging-port=${port}`, `--user-data-dir=${profile}`,
  '--window-size=390,844', 'about:blank',
], { stdio: 'ignore', windowsHide: true });
const delay = (milliseconds) => new Promise((resolveDelay) => setTimeout(resolveDelay, milliseconds));

async function pageTarget() {
  for (let attempt = 0; attempt < 80; attempt += 1) {
    try {
      const targets = await fetch(`http://127.0.0.1:${port}/json/list`).then((response) => response.json());
      const target = targets.find((entry) => entry.type === 'page');
      if (target) return target;
    } catch { /* Chrome is starting. */ }
    await delay(125);
  }
  throw new Error('Chrome DevTools endpoint did not start');
}

const target = await pageTarget();
const socket = new WebSocket(target.webSocketDebuggerUrl);
await new Promise((resolveOpen, rejectOpen) => {
  socket.addEventListener('open', resolveOpen, { once: true });
  socket.addEventListener('error', rejectOpen, { once: true });
});
let nextId = 0;
const pending = new Map();
const browserErrors = [];
socket.addEventListener('message', (event) => {
  const message = JSON.parse(String(event.data));
  if (message.id && pending.has(message.id)) {
    const callback = pending.get(message.id);
    pending.delete(message.id);
    if (message.error) callback.reject(new Error(message.error.message));
    else callback.resolve(message.result);
  } else if (message.method === 'Runtime.exceptionThrown') {
    browserErrors.push(message.params.exceptionDetails?.text ?? 'Runtime exception');
  }
});
function send(method, params = {}) {
  const id = ++nextId;
  return new Promise((resolveMessage, reject) => {
    pending.set(id, { resolve: resolveMessage, reject });
    socket.send(JSON.stringify({ id, method, params }));
  });
}
async function evaluate(expression) {
  const result = await send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true });
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.text ?? 'Evaluation failed');
  return result.result.value;
}
async function waitFor(expression, label) {
  for (let attempt = 0; attempt < 80; attempt += 1) {
    if (await evaluate(expression)) return;
    await delay(150);
  }
  throw new Error(`Timed out waiting for ${label}`);
}
async function navigate(path) {
  await send('Page.navigate', { url: `http://localhost:5173${path}` });
  await waitFor("document.readyState === 'complete'", path);
}
async function screenshot(name) {
  const capture = await send('Page.captureScreenshot', { format: 'png', fromSurface: true });
  const path = join(artifacts, name);
  writeFileSync(path, Buffer.from(capture.data, 'base64'));
  return path;
}

try {
  await Promise.all([send('Page.enable'), send('Runtime.enable')]);
  await navigate('/login');
  await waitFor("document.querySelectorAll('input').length >= 2", 'login form');
  await evaluate(`(() => {
    const [email, password] = document.querySelectorAll('input');
    const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
    setter.call(email, ${JSON.stringify(env.ADMIN_EMAIL)});
    email.dispatchEvent(new Event('input', { bubbles: true }));
    setter.call(password, ${JSON.stringify(env.ADMIN_PASSWORD)});
    password.dispatchEvent(new Event('input', { bubbles: true }));
    document.querySelector('form').requestSubmit();
  })()`);
  await waitFor("location.pathname !== '/login'", 'authenticated route');
  await navigate('/admin/transfers');
  await waitFor("Boolean(document.querySelector('.transfer-table, .empty-intake'))", 'transfer list');
  const listMetrics = await evaluate(`(() => ({
    path: location.pathname,
    width: innerWidth,
    scrollWidth: Math.max(document.documentElement.scrollWidth, document.body.scrollWidth),
    cardRows: document.querySelectorAll('.transfer-table tbody tr').length,
    firstLabels: Array.from(document.querySelectorAll('.transfer-table tbody tr:first-child td')).map((cell) => getComputedStyle(cell, '::before').content),
    alerts: Array.from(document.querySelectorAll('[role="alert"]')).map((node) => node.textContent.trim())
  }))()`);
  const listScreenshot = await screenshot('transfer-list-mobile.png');

  await navigate('/admin/transfers/new');
  await waitFor("Boolean(document.querySelector('#receipt-search'))", 'receipt search');
  const pickerMetrics = await evaluate(`(() => ({
    path: location.pathname,
    width: innerWidth,
    scrollWidth: Math.max(document.documentElement.scrollWidth, document.body.scrollWidth),
    pageSizeLabel: document.querySelector('.receipt-picker-pagination span')?.textContent ?? '',
    resultCount: document.querySelectorAll('.receipt-picker-result').length,
    alerts: Array.from(document.querySelectorAll('[role="alert"]')).map((node) => node.textContent.trim())
  }))()`);
  const pickerScreenshot = await screenshot('transfer-picker-mobile.png');

  for (const metrics of [listMetrics, pickerMetrics]) {
    if (metrics.scrollWidth > metrics.width + 1) throw new Error(`Horizontal overflow: ${metrics.scrollWidth} > ${metrics.width}`);
    if (metrics.alerts.length) throw new Error(`Visible alerts: ${metrics.alerts.join('; ')}`);
  }
  if (listMetrics.cardRows && listMetrics.firstLabels.some((label) => label === 'none' || label === '""')) {
    throw new Error('Mobile transfer cards are missing field labels');
  }
  if (pickerMetrics.resultCount > 20) throw new Error('Receipt picker loaded more than 20 results');
  if (browserErrors.length) throw new Error(browserErrors.join(' | '));
  process.stdout.write(JSON.stringify({ listMetrics, pickerMetrics, screenshots: [listScreenshot, pickerScreenshot] }, null, 2));
} finally {
  socket.close();
  chrome.kill();
}
