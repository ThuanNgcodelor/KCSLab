import { spawn } from 'node:child_process';
import { mkdirSync, mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const email = process.env.RECEPTION_EMAIL;
const password = process.env.RECEPTION_PASSWORD;
if (!email || !password) throw new Error('Missing RECEPTION_EMAIL or RECEPTION_PASSWORD');

const scriptDir = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(scriptDir, '..');
const artifactsDir = join(repositoryRoot, 'artifacts', 'intake-demo');
mkdirSync(artifactsDir, { recursive: true });

const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const debugPort = 9700 + Math.floor(Math.random() * 200);
const profileDir = mkdtempSync(join(tmpdir(), 'cfc-intake-demo-'));
const chrome = spawn(chromePath, [
  '--no-first-run', '--no-default-browser-check',
  `--remote-debugging-port=${debugPort}`, `--user-data-dir=${profileDir}`,
  '--window-size=1440,900', 'about:blank',
], { stdio: 'ignore', windowsHide: false, detached: true });
chrome.unref();

const delay = (milliseconds) => new Promise((resolveDelay) => setTimeout(resolveDelay, milliseconds));

async function pageTarget() {
  for (let attempt = 0; attempt < 100; attempt += 1) {
    try {
      const targets = await fetch(`http://127.0.0.1:${debugPort}/json/list`).then((response) => response.json());
      const page = targets.find((target) => target.type === 'page');
      if (page) return page;
    } catch { /* Chrome is still starting. */ }
    await delay(120);
  }
  throw new Error('Google Chrome did not expose the DevTools endpoint');
}

const target = await pageTarget();
const socket = new WebSocket(target.webSocketDebuggerUrl);
await new Promise((resolveOpen, rejectOpen) => {
  socket.addEventListener('open', resolveOpen, { once: true });
  socket.addEventListener('error', rejectOpen, { once: true });
});

let messageId = 0;
const pending = new Map();
socket.addEventListener('message', (event) => {
  const message = JSON.parse(String(event.data));
  if (!message.id || !pending.has(message.id)) return;
  const { resolve: resolveMessage, reject } = pending.get(message.id);
  pending.delete(message.id);
  if (message.error) reject(new Error(message.error.message));
  else resolveMessage(message.result);
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
  if (response.exceptionDetails) throw new Error(response.exceptionDetails.text ?? 'Browser evaluation failed');
  return response.result.value;
}

async function waitFor(expression, description, timeout = 15000) {
  const startedAt = Date.now();
  while (Date.now() - startedAt < timeout) {
    if (await evaluate(expression)) return;
    await delay(150);
  }
  throw new Error(`Timed out waiting for ${description}`);
}

async function navigate(path) {
  await send('Page.navigate', { url: `http://localhost:5173${path}` });
  await waitFor("document.readyState === 'complete'", `${path} ready`);
}

try {
  await Promise.all([send('Page.enable'), send('Runtime.enable')]);
  await navigate('/login');
  await waitFor("document.querySelectorAll('input').length >= 2", 'login form');
  await evaluate(`(() => {
    const [emailInput, passwordInput] = document.querySelectorAll('input');
    const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
    setter.call(emailInput, ${JSON.stringify(email)});
    emailInput.dispatchEvent(new Event('input', { bubbles: true }));
    setter.call(passwordInput, ${JSON.stringify(password)});
    passwordInput.dispatchEvent(new Event('input', { bubbles: true }));
    document.querySelector('form').requestSubmit();
    return true;
  })()`);
  await waitFor("location.pathname !== '/login'", 'receptionist login');

  await navigate('/admin/intake/new');
  await waitFor("Boolean(document.querySelector('.receipt-form-layout'))", 'intake form');
  const pcFormCapture = await send('Page.captureScreenshot', { format: 'png', fromSurface: true });
  const pcFormPath = join(artifactsDir, 'receipt-form-pc.png');
  writeFileSync(pcFormPath, Buffer.from(pcFormCapture.data, 'base64'));
  await send('Emulation.setDeviceMetricsOverride', {
    width: 390, height: 844, screenWidth: 390, screenHeight: 844, deviceScaleFactor: 1, mobile: true,
  });
  await delay(300);
  const phoneFormCapture = await send('Page.captureScreenshot', { format: 'png', fromSurface: true });
  const phoneFormPath = join(artifactsDir, 'receipt-form-phone.png');
  writeFileSync(phoneFormPath, Buffer.from(phoneFormCapture.data, 'base64'));
  await send('Emulation.clearDeviceMetricsOverride');
  await delay(300);
  const demoPhone = `09${String(Date.now()).slice(-8)}`;
  await evaluate(`(() => {
    const setValue = (labelText, value) => {
      const label = Array.from(document.querySelectorAll('label')).find((node) => node.textContent.includes(labelText));
      const input = label?.querySelector('input, textarea');
      if (!input) throw new Error('Missing field: ' + labelText);
      const prototype = input instanceof HTMLTextAreaElement ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
      Object.getOwnPropertyDescriptor(prototype, 'value').set.call(input, value);
      input.dispatchEvent(new Event('input', { bubbles: true }));
      input.dispatchEvent(new Event('change', { bubbles: true }));
    };
    setValue('Họ tên người giao', 'Nguyễn Văn Demo');
    setValue('Số điện thoại', ${JSON.stringify(demoPhone)});
    setValue('Tên khách hàng / đơn vị', 'Khách demo tiếp nhận');
    setValue('Địa chỉ', 'Cần Thơ');
    setValue('Vùng trồng / nguồn mẫu', 'CT001 - Cần Thơ');
    setValue('Tên mẫu', 'Sầu riêng demo');
    setValue('Loại sản phẩm', 'Sầu riêng');
    setValue('Mã nguồn', 'CT001');
    setValue('Số lượng / khối lượng', '01 quả');
    setValue('Bao bì', 'Túi PE');
    setValue('Tình trạng', 'Nguyên vẹn');
    setValue('Chỉ tiêu yêu cầu', 'Cadimi (Cd)');
    setValue('Nhiệt độ khi nhận', '25');
    setValue('Điều kiện bảo quản', 'Nhiệt độ thường');
    setValue('Mô tả tình trạng', 'Mẫu nguyên vẹn, bao bì sạch, đủ điều kiện tiếp nhận.');
    document.querySelector('form').requestSubmit();
    return true;
  })()`);

  await waitFor("Boolean(document.querySelector('.receipt-success'))", 'receipt success', 20000);
  const result = await evaluate(`(() => ({
    receiptCode: document.querySelectorAll('.code-display strong')[0]?.textContent.trim(),
    sampleCode: document.querySelectorAll('.code-display strong')[1]?.textContent.trim(),
    title: document.querySelector('h1')?.textContent.trim(),
    path: location.pathname,
    alerts: Array.from(document.querySelectorAll('[role="alert"]')).map((node) => node.textContent.trim())
  }))()`);
  if (result.alerts.length) throw new Error(result.alerts.join('; '));

  const screenshot = await send('Page.captureScreenshot', { format: 'png', fromSurface: true });
  const screenshotPath = join(artifactsDir, 'receipt-created.png');
  writeFileSync(screenshotPath, Buffer.from(screenshot.data, 'base64'));
  process.stdout.write(`${JSON.stringify({ ...result, demoPhone, screenshotPath, pcFormPath, phoneFormPath, chromePid: chrome.pid }, null, 2)}\n`);
} finally {
  socket.close();
}
