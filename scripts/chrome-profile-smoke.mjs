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
const email = 'nguyentrungthuan4177@gmail.com';
const port = 9900 + Math.floor(Math.random() * 80);
const profileDir = mkdtempSync(join(tmpdir(), 'cfc-profile-smoke-'));
const artifacts = join(root, 'artifacts', 'profile');
mkdirSync(artifacts, { recursive: true });
const chrome = spawn('C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe', [
  '--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
  `--remote-debugging-port=${port}`, `--user-data-dir=${profileDir}`,
  '--window-size=1440,900', 'about:blank',
], { stdio: 'ignore', windowsHide: true });
const delay = (milliseconds) => new Promise((resolveDelay) => setTimeout(resolveDelay, milliseconds));

async function targetPage() {
  for (let attempt = 0; attempt < 80; attempt += 1) {
    try {
      const targets = await fetch(`http://127.0.0.1:${port}/json/list`).then((response) => response.json());
      const page = targets.find((entry) => entry.type === 'page');
      if (page) return page;
    } catch { /* Chrome is starting. */ }
    await delay(125);
  }
  throw new Error('Chrome DevTools endpoint did not start');
}

const target = await targetPage();
const socket = new WebSocket(target.webSocketDebuggerUrl);
await new Promise((resolveOpen, rejectOpen) => {
  socket.addEventListener('open', resolveOpen, { once: true });
  socket.addEventListener('error', rejectOpen, { once: true });
});
let messageId = 0;
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
  } else if (message.method === 'Runtime.consoleAPICalled' && message.params.type === 'error') {
    browserErrors.push(message.params.args?.map((item) => item.value ?? item.description).join(' '));
  }
});
function send(method, params = {}) {
  const id = ++messageId;
  return new Promise((resolveMessage, reject) => {
    pending.set(id, { resolve: resolveMessage, reject });
    socket.send(JSON.stringify({ id, method, params }));
  });
}
async function evaluate(expression) {
  const response = await send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true });
  if (response.exceptionDetails) throw new Error(response.exceptionDetails.text ?? 'Evaluation failed');
  return response.result.value;
}
async function waitFor(expression, label, timeout = 15000) {
  const started = Date.now();
  while (Date.now() - started < timeout) {
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
async function metrics() {
  return evaluate(`(() => ({
    path: location.pathname,
    width: innerWidth,
    scrollWidth: Math.max(document.documentElement.scrollWidth, document.body.scrollWidth),
    title: document.querySelector('h1')?.textContent?.trim() ?? '',
    alerts: Array.from(document.querySelectorAll('[role="alert"]')).map((node) => node.textContent.trim()),
    avatarImages: document.querySelectorAll('.user-avatar img').length
  }))()`);
}

try {
  await Promise.all([send('Page.enable'), send('Runtime.enable'), send('Log.enable')]);
  await navigate('/login');
  await waitFor("document.querySelectorAll('input').length >= 2", 'login form');
  await evaluate(`(() => {
    const [emailInput, password] = document.querySelectorAll('input');
    const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
    setter.call(emailInput, ${JSON.stringify(email)});
    emailInput.dispatchEvent(new Event('input', { bubbles: true }));
    setter.call(password, ${JSON.stringify(env.ADMIN_PASSWORD)});
    password.dispatchEvent(new Event('input', { bubbles: true }));
    document.querySelector('form').requestSubmit();
  })()`);
  await waitFor("location.pathname !== '/login'", 'authenticated redirect');
  browserErrors.length = 0;

  await navigate('/admin/profile');
  await waitFor("document.body.textContent.includes('Thông tin của tôi') && Boolean(document.querySelector('input[type=file]'))", 'profile page');
  const desktop = await metrics();
  const desktopShot = await screenshot('profile-desktop.png');

  await evaluate(`(async () => {
    const canvas = document.createElement('canvas');
    canvas.width = 1600;
    canvas.height = 1000;
    const context = canvas.getContext('2d');
    const gradient = context.createLinearGradient(0, 0, 1600, 1000);
    gradient.addColorStop(0, '#0f5d32');
    gradient.addColorStop(1, '#b7edc2');
    context.fillStyle = gradient;
    context.fillRect(0, 0, 1600, 1000);
    context.fillStyle = '#ffffff';
    context.font = 'bold 280px sans-serif';
    context.textAlign = 'center';
    context.textBaseline = 'middle';
    context.fillText('TN', 800, 500);
    const blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/png'));
    const file = new File([blob], 'test-avatar.png', { type: 'image/png' });
    const transfer = new DataTransfer();
    transfer.items.add(file);
    const input = document.querySelector('input[type=file]');
    Object.defineProperty(input, 'files', { value: transfer.files, configurable: true });
    input.dispatchEvent(new Event('change', { bubbles: true }));
  })()`);
  await waitFor("document.querySelector('[role=status]')?.textContent.includes('320×320')", 'optimized avatar upload');
  await waitFor("document.querySelectorAll('.user-avatar img').length >= 2", 'avatar in identity and sidebar');
  const avatarDimensions = await evaluate(`(async () => {
    const image = document.querySelector('.profile-avatar img');
    if (!image.complete) await new Promise((resolve) => image.addEventListener('load', resolve, { once: true }));
    return { naturalWidth: image.naturalWidth, naturalHeight: image.naturalHeight };
  })()`);
  if (avatarDimensions.naturalWidth !== 320 || avatarDimensions.naturalHeight !== 320) {
    throw new Error(`Stored avatar dimensions are ${avatarDimensions.naturalWidth}×${avatarDimensions.naturalHeight}`);
  }

  await send('Emulation.setDeviceMetricsOverride', {
    width: 390, height: 844, screenWidth: 390, screenHeight: 844, deviceScaleFactor: 1, mobile: true,
  });
  await delay(300);
  const mobile = await metrics();
  const mobileShot = await screenshot('profile-mobile.png');

  await evaluate(`(() => {
    const button = Array.from(document.querySelectorAll('button')).find((node) => node.textContent.trim() === 'Xóa avatar');
    button?.click();
  })()`);
  await waitFor("document.querySelector('[role=status]')?.textContent.includes('Đã xóa avatar')", 'avatar cleanup');

  for (const page of [desktop, mobile]) {
    if (page.scrollWidth > page.width + 1) throw new Error(`Horizontal overflow: ${page.scrollWidth} > ${page.width}`);
    if (page.alerts.length) throw new Error(page.alerts.join('; '));
  }
  if (browserErrors.length) throw new Error(browserErrors.join(' | '));
  process.stdout.write(JSON.stringify({ desktop, mobile, avatarDimensions, screenshots: [desktopShot, mobileShot] }, null, 2));
} finally {
  socket.close();
  chrome.kill();
}
