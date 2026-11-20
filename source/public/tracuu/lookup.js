// Keep the credentials for downloads in memory until the customer starts a new lookup.
const api = document.documentElement.dataset.apiBase || '/api/v1/public/results';
const form = document.querySelector('#lookup-form');
const codeInput = document.querySelector('#lookup-code');
const phoneInput = document.querySelector('#phone-tail');
const submit = document.querySelector('#lookup-submit');
const submitLabel = document.querySelector('#lookup-submit-label');
const lookupMessage = document.querySelector('#lookup-message');
const downloadMessage = document.querySelector('#download-message');
const panel = document.querySelector('#lookup-result');
const resultTitle = document.querySelector('#result-title');
const resultNumber = document.querySelector('#result-number');
const metaList = document.querySelector('#result-meta');
const fileList = document.querySelector('#result-files');
const measurements = document.querySelector('#result-measurements');
const resultItems = document.querySelector('#result-items');
const codeError = document.querySelector('#code-error');
const phoneError = document.querySelector('#phone-error');
const dateFormat = new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'Asia/Ho_Chi_Minh' });
const sizeFormat = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 });
let credentials = null;

const locationCode = new URLSearchParams(location.search).get('code');
if (locationCode !== null) {
  history.replaceState(null, '', location.pathname + location.hash);
  codeInput.value = locationCode.slice(0, codeInput.maxLength);
  if (codeInput.value) show(lookupMessage, 'Đã điền mã từ mã QR. Nhập 4 số cuối điện thoại để tiếp tục.');
}

function show(element, text, error = false) {
  element.dataset.tone = error ? 'error' : 'info';
  element.textContent = text;
}

function fieldError(input, errorElement, text) {
  input.setAttribute('aria-invalid', String(Boolean(text)));
  errorElement.textContent = text;
  errorElement.hidden = !text;
}

function setBusy(busy) {
  submit.disabled = busy;
  submit.setAttribute('aria-busy', String(busy));
  codeInput.readOnly = busy;
  phoneInput.readOnly = busy;
  submitLabel.textContent = busy ? 'Đang tra cứu…' : 'Tra cứu kết quả';
}

function clearResult() {
  panel.hidden = true;
  credentials = null;
  resultNumber.textContent = '';
  metaList.replaceChildren();
  resultItems.replaceChildren();
  fileList.replaceChildren();
  show(downloadMessage, '');
}

async function post(path, body) {
  const response = await fetch(api + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
    credentials: 'omit',
    cache: 'no-store',
    referrerPolicy: 'no-referrer',
  });
  if (!response.ok) {
    const error = new Error('Yêu cầu tra cứu thất bại');
    error.status = response.status;
    error.retryAfter = response.headers.get('Retry-After');
    throw error;
  }
  return response;
}

function friendly(error, downloading = false) {
  if (error.status === 429) {
    const minutes = Math.max(1, Math.ceil((Number.parseInt(error.retryAfter, 10) || 60) / 60));
    return 'Bạn đã thử nhiều lần. Vui lòng thử lại sau khoảng ' + minutes + ' phút.';
  }
  if (error.status === 404) {
    return downloading
      ? 'Tài liệu hiện không tải được. Hãy tra cứu lại phiếu hoặc liên hệ CFC.'
      : 'Không tìm thấy kết quả phù hợp. Hãy kiểm tra mã tra cứu và 4 số cuối điện thoại.';
  }
  if (error.status === 400) return 'Hãy kiểm tra mã tra cứu và nhập đúng 4 số cuối điện thoại.';
  return 'Không thể kết nối với CFC Lab. Hãy kiểm tra mạng rồi thử lại.';
}

function textElement(tag, text, className) {
  const element = document.createElement(tag);
  element.textContent = text;
  if (className) element.className = className;
  return element;
}

function formatSize(bytes) {
  const kb = bytes / 1024;
  return kb < 1024 ? sizeFormat.format(Math.max(1, Math.ceil(kb))) + ' KB' : sizeFormat.format(kb / 1024) + ' MB';
}

function renderResult(result) {
  resultNumber.textContent = result.resultNumber ? 'Số phiếu ' + result.resultNumber : '';
  const published = new Date(result.publishedAt ?? undefined);
  const date = Number.isNaN(published.getTime()) ? null : dateFormat.format(published);
  for (const [name, value, identifier] of [
    ['Tên mẫu', result.sampleName], ['Mã mẫu', result.sampleCode, true],
    ['Ngày phát hành', date], ['Phiên bản', result.version],
  ]) {
    const item = document.createElement('div');
    const description = textElement('dd', value ?? '—');
    if (identifier) description.setAttribute('translate', 'no');
    item.append(textElement('dt', name), description);
    metaList.append(item);
  }

  const items = result.items ?? [];
  measurements.hidden = items.length === 0;
  for (const item of items) {
    const row = resultItems.insertRow();
    for (const value of [item.parameter, item.result, item.unit, item.method, item.note]) {
      row.insertCell().textContent = value || '—';
    }
  }

  const files = result.files ?? [];
  for (const file of files) {
    const pdf = file.contentType === 'application/pdf';
    const kind = textElement('span', pdf ? 'PDF' : 'Tệp', 'file-kind');
    kind.setAttribute('aria-hidden', 'true');
    const info = document.createElement('span');
    info.className = 'file-info';
    info.append(textElement('strong', file.name), textElement('small', formatSize(file.size)));
    const button = textElement('button', pdf ? 'Tải PDF' : 'Tải tệp', pdf ? 'primary-button' : 'outline-button');
    button.type = 'button';
    button.setAttribute('aria-label', button.textContent + ' ' + file.name);
    button.addEventListener('click', () => download(file, button));
    const row = document.createElement('li');
    row.append(kind, info, button);
    fileList.append(row);
  }
  if (!files.length) fileList.append(textElement('li', 'Phiếu này chưa có tệp công khai. Hãy liên hệ CFC nếu bạn cần bản sao.', 'no-files'));

  panel.hidden = false;
  resultTitle.focus({ preventScroll: true });
  panel.scrollIntoView({ block: 'start' });
}

async function download(file, button) {
  const activeCredentials = credentials;
  if (!activeCredentials) return;
  const buttonLabel = button.textContent;
  show(downloadMessage, '');
  button.disabled = true;
  button.textContent = 'Đang tải…';
  try {
    const response = await post('/download', { ...activeCredentials, fileId: file.id });
    const blob = await response.blob();
    if (credentials !== activeCredentials) return;
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = file.name;
    document.body.append(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 10000);
  } catch (error) {
    if (credentials === activeCredentials) show(downloadMessage, friendly(error, true), true);
  } finally {
    button.disabled = false;
    button.textContent = buttonLabel;
  }
}

for (const [input, errorElement] of [[codeInput, codeError], [phoneInput, phoneError]]) {
  input.addEventListener('input', () => {
    fieldError(input, errorElement, '');
    show(lookupMessage, '');
    if (credentials) clearResult();
  });
}

document.querySelector('#new-lookup').addEventListener('click', () => {
  form.reset();
  clearResult();
  show(lookupMessage, '');
  codeInput.focus();
});

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (submit.disabled) return;
  const code = codeInput.value.trim();
  const phoneLast4 = phoneInput.value.trim();
  const codeValid = /^[a-z0-9]{16}$/i.test(code.replace(/[\s-]/g, ''));
  const phoneValid = /^\d{4}$/.test(phoneLast4);
  show(lookupMessage, '');
  fieldError(codeInput, codeError, codeValid ? '' : 'Nhập mã 16 ký tự in trên phiếu kết quả.');
  fieldError(phoneInput, phoneError, phoneValid ? '' : 'Nhập đúng 4 chữ số cuối điện thoại.');
  if (!codeValid || !phoneValid) {
    (codeValid ? phoneInput : codeInput).focus();
    return;
  }

  setBusy(true);
  clearResult();
  try {
    const response = await post('/lookup', { code, phoneLast4 });
    const result = await response.json();
    credentials = { code, phoneLast4 };
    renderResult(result);
  } catch (error) {
    clearResult();
    show(lookupMessage, friendly(error), true);
  } finally {
    setBusy(false);
  }
});
