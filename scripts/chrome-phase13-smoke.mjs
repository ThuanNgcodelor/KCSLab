import { spawn } from 'node:child_process';
import { mkdirSync, mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(scriptDir, '..');
const receiptId = process.argv[2];
const transferId = process.argv[3];
if (!receiptId || !transferId) throw new Error('Usage: node chrome-phase13-smoke.mjs <receiptId> <transferId>');

const env = Object.fromEntries(readFileSync(join(repositoryRoot, '.env'), 'utf8')
  .split(/\r?\n/)
  .filter((line) => /^[A-Za-z_][A-Za-z0-9_]*=/.test(line))
  .map((line) => {
    const separator = line.indexOf('=');
    return [line.slice(0, separator), line.slice(separator + 1)];
  }));
if (!env.ADMIN_EMAIL || !env.ADMIN_PASSWORD) throw new Error('Missing admin credentials in .env');

const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const debugPort = 9300 + Math.floor(Math.random() * 400);
const profileDir = mkdtempSync(join(tmpdir(), 'cfc-phase13-chrome-'));
const artifactsDir = join(repositoryRoot, 'artifacts', 'phase13-chrome');
mkdirSync(artifactsDir, { recursive: true });

const chrome = spawn(chromePath, [
  '--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
  `--remote-debugging-port=${debugPort}`, `--user-data-dir=${profileDir}`,
  '--window-size=1440,900', 'about:blank',
], { stdio: 'ignore', windowsHide: true });

const delay = (milliseconds) => new Promise((resolveDelay) => setTimeout(resolveDelay, milliseconds));

async function getPageTarget() {
  for (let attempt = 0; attempt < 80; attempt += 1) {
    try {
      const targets = await fetch(`http://127.0.0.1:${debugPort}/json/list`).then((response) => response.json());
      const page = targets.find((target) => target.type === 'page');
      if (page) return page;
    } catch { /* Chrome is still starting. */ }
    await delay(125);
  }
  throw new Error('Chrome DevTools endpoint did not become ready');
}

const target = await getPageTarget();
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
    const { resolve: resolveMessage, reject } = pending.get(message.id);
    pending.delete(message.id);
    if (message.error) reject(new Error(`${message.error.message}: ${message.error.data ?? ''}`));
    else resolveMessage(message.result);
    return;
  }
  if (message.method === 'Runtime.exceptionThrown') {
    browserErrors.push(message.params.exceptionDetails?.text ?? 'Runtime exception');
  }
  if (message.method === 'Log.entryAdded' && message.params.entry?.level === 'error') {
    browserErrors.push(message.params.entry.text);
  }
  if (message.method === 'Runtime.consoleAPICalled' && message.params.type === 'error') {
    browserErrors.push(message.params.args?.map((argument) => argument.value ?? argument.description).join(' '));
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

async function waitFor(expression, description, timeout = 12000) {
  const startedAt = Date.now();
  while (Date.now() - startedAt < timeout) {
    if (await evaluate(expression)) return;
    await delay(150);
  }
  throw new Error(`Timed out waiting for ${description}`);
}

async function navigate(path) {
  await send('Page.navigate', { url: `http://localhost:5173${path}` });
  await waitFor("document.readyState === 'complete'", `${path} document ready`);
}

async function screenshot(name) {
  const result = await send('Page.captureScreenshot', { format: 'png', fromSurface: true, captureBeyondViewport: false });
  const path = join(artifactsDir, name);
  writeFileSync(path, Buffer.from(result.data, 'base64'));
  return path;
}

async function pageMetrics() {
  return evaluate(`(() => ({
    path: location.pathname,
    title: document.querySelector('h1')?.textContent?.trim() ?? '',
    width: innerWidth,
    scrollWidth: Math.max(document.documentElement.scrollWidth, document.body.scrollWidth),
    alerts: Array.from(document.querySelectorAll('[role="alert"]')).map((node) => node.textContent.trim())
  }))()`);
}

try {
  await Promise.all([send('Page.enable'), send('Runtime.enable'), send('Log.enable'), send('Network.enable')]);
  await navigate('/login');
  await waitFor("document.querySelectorAll('input').length >= 2", 'login fields');
  const loginResult = await evaluate(`(() => {
    const [email, password] = document.querySelectorAll('input');
    const setValue = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
    setValue.call(email, ${JSON.stringify(env.ADMIN_EMAIL)});
    email.dispatchEvent(new Event('input', { bubbles: true }));
    setValue.call(password, ${JSON.stringify(env.ADMIN_PASSWORD)});
    password.dispatchEvent(new Event('input', { bubbles: true }));
    document.querySelector('form').requestSubmit();
    return true;
  })()`);
  if (!loginResult) throw new Error('Could not submit login form');
  await waitFor("location.pathname !== '/login'", 'authenticated redirect');
  browserErrors.length = 0;

  await navigate('/admin/intake');
  await waitFor("document.body.textContent.includes('Tiếp nhận mẫu') && Boolean(document.querySelector('table')) && Boolean(document.querySelector('.intake-list-tools select'))", 'paginated intake list');
  const intakePagination = await evaluate(`(() => ({
    sortOptions: document.querySelectorAll('.intake-list-tools select option').length,
    pageButtons: document.querySelectorAll('.list-pagination .pagination-button').length,
    summary: document.querySelector('.list-pagination p')?.textContent?.trim() ?? ''
  }))()`);
  if (intakePagination.sortOptions < 9) throw new Error(`Expected at least 9 receipt sort options, got ${intakePagination.sortOptions}`);
  if (intakePagination.pageButtons < 3) throw new Error('Receipt pagination controls are missing');
  if (!intakePagination.summary.includes('20 phiếu/trang')) throw new Error('Receipt page-size summary is missing');
  const intakeDesktop = await pageMetrics();
  const intakeDesktopShot = await screenshot('01-intake-list-desktop.png');

  await navigate(`/admin/intake/${receiptId}`);
  await waitFor("document.body.textContent.includes('Nguồn và chuỗi tiếp nhận') && document.body.textContent.includes('Ảnh và hồ sơ')", 'receipt detail');
  const receiptDetail = await pageMetrics();
  const receiptDetailShot = await screenshot('02-receipt-detail-desktop.png');

  await navigate('/admin/intake/new');
  await waitFor("document.querySelectorAll('.sample-editor-card').length === 1", 'single initial sample editor');
  await evaluate(`(() => {
    const button = Array.from(document.querySelectorAll('button')).find((node) => node.textContent.includes('Thêm mẫu vào phiếu'));
    button?.click();
    return Boolean(button);
  })()`);
  await waitFor("document.querySelectorAll('.sample-editor-card').length === 2", 'second sample editor');
  const intakeFormDesktop = await pageMetrics();
  const intakeFormDesktopShot = await screenshot('03-intake-form-two-samples-desktop.png');

  await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true });
  await delay(300);
  const intakeFormMobile = await pageMetrics();
  const intakeFormMobileShot = await screenshot('04-intake-form-mobile.png');

  await navigate('/admin/intake');
  await waitFor("Boolean(document.querySelector('.intake-list-tools select')) && Boolean(document.querySelector('.list-pagination'))", 'mobile intake pagination');
  const intakeListMobile = await pageMetrics();
  const intakeListMobileShot = await screenshot('06-intake-list-mobile.png');

  await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 900, deviceScaleFactor: 1, mobile: false });
  await navigate('/admin/transfers/new');
  await waitFor("document.body.textContent.includes('Lập phiếu chuyển mẫu') && (Boolean(document.querySelector('.transfer-form-layout')) || Boolean(document.querySelector('.empty-intake')))", 'transfer create after receipt pagination contract');
  const transferCreate = await pageMetrics();
  const transferCreateShot = await screenshot('07-transfer-create-pagination-contract.png');

  await navigate(`/admin/transfers/${transferId}`);
  await waitFor("document.body.textContent.includes('Phiếu chuyển mẫu') && document.body.textContent.includes('Đã nhận mẫu')", 'accepted transfer detail');
  const transferDetail = await pageMetrics();
  const transferDetailShot = await screenshot('05-transfer-accepted-desktop.png');

  const pages = { intakeDesktop, receiptDetail, intakeFormDesktop, intakeFormMobile, intakeListMobile, transferCreate, transferDetail };
  for (const [name, metrics] of Object.entries(pages)) {
    if (metrics.alerts.length) throw new Error(`${name} has visible errors: ${metrics.alerts.join('; ')}`);
    if (metrics.scrollWidth > metrics.width + 1) throw new Error(`${name} has horizontal overflow: ${metrics.scrollWidth} > ${metrics.width}`);
  }
  if (browserErrors.length) throw new Error(`Browser console errors: ${browserErrors.join(' | ')}`);

  process.stdout.write(`${JSON.stringify({
    browser: 'Google Chrome headless (DevTools Protocol)',
    routes: Object.fromEntries(Object.entries(pages).map(([name, metrics]) => [name, metrics.path])),
    responsive: { desktop: `${intakeDesktop.width}px`, mobile: `${intakeFormMobile.width}px`, horizontalOverflow: false },
    pagination: intakePagination,
    screenshots: [intakeDesktopShot, receiptDetailShot, intakeFormDesktopShot, intakeFormMobileShot, intakeListMobileShot, transferCreateShot, transferDetailShot],
    consoleErrors: browserErrors.length,
  }, null, 2)}\n`);
} finally {
  socket.close();
  chrome.kill();
}
