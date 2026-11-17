// Public result lookup. The lookup code and the last four phone digits travel only in POST bodies, are never
// stored in the page URL, localStorage or cookies, and are cleared from memory when the user starts over.
(function () {
  'use strict';
  var API = (document.documentElement.getAttribute('data-api-base') || '/api/v1/public/results');
  var form = document.getElementById('lookup-form');
  var codeInput = document.getElementById('lookup-code');
  var phoneInput = document.getElementById('phone-tail');
  var submit = document.getElementById('lookup-submit');
  var label = document.getElementById('lookup-submit-label');
  var message = document.getElementById('lookup-message');
  var panel = document.getElementById('lookup-result');
  if (!form) return;

  // QR links carry only the code (?code=...). The phone digits are always typed by the customer.
  var params = new URLSearchParams(window.location.search);
  if (params.get('code')) {
    codeInput.value = params.get('code').slice(0, 40);
    history.replaceState(null, '', window.location.pathname); // keep the code out of history/referrers
    phoneInput.focus();
  }

  var credentials = null;

  function show(text, isError) {
    message.className = 'notice' + (isError ? ' error' : '');
    message.lastElementChild.textContent = text;
  }

  function setBusy(busy) {
    submit.disabled = busy;
    label.textContent = busy ? 'Đang tra cứu…' : 'Tra cứu kết quả';
  }

  async function post(path, body) {
    var response = await fetch(API + path, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body), credentials: 'omit', cache: 'no-store', referrerPolicy: 'no-referrer'
    });
    if (response.ok) return response;
    var detail = '';
    try { detail = (await response.json()).detail || ''; } catch (e) { /* ignore */ }
    var error = new Error(detail);
    error.status = response.status;
    error.retryAfter = response.headers.get('Retry-After');
    throw error;
  }

  function friendly(error) {
    if (error.status === 429) {
      var minutes = Math.max(1, Math.ceil((parseInt(error.retryAfter, 10) || 60) / 60));
      return 'Bạn đã thử quá nhiều lần. Vui lòng thử lại sau khoảng ' + minutes + ' phút.';
    }
    if (error.status === 404) return 'Không tìm thấy kết quả phù hợp với thông tin đã nhập. Vui lòng kiểm tra lại mã và 4 số cuối điện thoại, hoặc liên hệ CFC.';
    if (error.status === 400) return 'Vui lòng nhập mã tra cứu và đúng 4 chữ số cuối điện thoại.';
    return 'Không thể kết nối. Vui lòng thử lại sau hoặc liên hệ CFC.';
  }

  function render(result) {
    panel.textContent = '';
    var title = document.createElement('h3');
    title.textContent = 'Kết quả kiểm nghiệm';
    var dl = document.createElement('dl');
    [['Số phiếu', result.resultNumber], ['Mã mẫu', result.sampleCode],
     ['Phiên bản', String(result.version)],
     ['Ngày phát hành', result.publishedAt ? new Date(result.publishedAt).toLocaleDateString('vi-VN') : '—']
    ].forEach(function (pair) {
      var dt = document.createElement('dt'); dt.textContent = pair[0];
      var dd = document.createElement('dd'); dd.textContent = pair[1];
      dl.appendChild(dt); dl.appendChild(dd);
    });
    var list = document.createElement('ul');
    result.files.forEach(function (file) {
      var li = document.createElement('li');
      var name = document.createElement('span');
      name.textContent = file.name + ' (' + formatSize(file.size) + ')';
      var button = document.createElement('button');
      button.type = 'button'; button.textContent = 'Tải xuống';
      button.addEventListener('click', function () { download(file, button); });
      li.appendChild(name); li.appendChild(button); list.appendChild(li);
    });
    panel.appendChild(title); panel.appendChild(dl);
    if (result.files.length) panel.appendChild(list);
    else { var none = document.createElement('p'); none.textContent = 'Hồ sơ chưa có tệp công khai. Vui lòng liên hệ CFC.'; panel.appendChild(none); }
    panel.hidden = false;
  }

  function formatSize(bytes) {
    return bytes < 1048576 ? Math.max(1, Math.ceil(bytes / 1024)) + ' KB' : (bytes / 1048576).toFixed(1) + ' MB';
  }

  async function download(file, button) {
    button.disabled = true;
    try {
      var response = await post('/download', { code: credentials.code, phoneLast4: credentials.phoneLast4, fileId: file.id });
      var blob = await response.blob();
      var url = URL.createObjectURL(blob);
      var anchor = document.createElement('a');
      anchor.href = url; anchor.download = file.name; document.body.appendChild(anchor); anchor.click();
      anchor.remove(); setTimeout(function () { URL.revokeObjectURL(url); }, 10000);
    } catch (error) { show(friendly(error), true); }
    finally { button.disabled = false; }
  }

  form.addEventListener('submit', async function (event) {
    event.preventDefault();
    var code = codeInput.value.trim();
    var phoneLast4 = phoneInput.value.trim();
    var ok = true;
    codeInput.classList.toggle('invalid', !code); if (!code) ok = false;
    var phoneOk = /^[0-9]{4}$/.test(phoneLast4);
    phoneInput.classList.toggle('invalid', !phoneOk); if (!phoneOk) ok = false;
    if (!ok) { show('Vui lòng nhập mã tra cứu và đúng 4 chữ số cuối điện thoại.', true); return; }

    setBusy(true); panel.hidden = true; credentials = null;
    try {
      var response = await post('/lookup', { code: code, phoneLast4: phoneLast4 });
      var result = await response.json();
      credentials = { code: code, phoneLast4: phoneLast4 };
      show('Đã tìm thấy kết quả. Thông tin của bạn không được lưu cùng kết quả.', false);
      render(result);
    } catch (error) { show(friendly(error), true); }
    finally { setBusy(false); }
  });
})();
