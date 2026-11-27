import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createResultFromEntry, getReportHeader } from '../../results/api/resultsApi';
import { DateInput } from '../../../shared/components/DateInput';
import { splitMethods, splitParameters } from '../../../shared/parameters';

// Đơn vị và ghi chú (LOQ…) thường lặp lại theo chỉ tiêu, nên trình duyệt nhớ lần nhập trước để điền sẵn.
const MEMORY_KEY = 'cfc-kqkn-row-defaults';
const UNITS = ['µg/kg', 'mg/kg', 'mg/L', 'µg/L', '%', 'CFU/g'];
const RESULTS = ['KPH', '< LOQ', 'Phát hiện'];

function readMemory() { try { return JSON.parse(localStorage.getItem(MEMORY_KEY) || '{}') || {}; } catch { return {}; } }
function writeMemory(rows) {
  try {
    const memory = readMemory();
    rows.forEach((row) => { if (row.parameter.trim()) memory[row.parameter.trim().toLowerCase()] = { unit: row.unit.trim(), note: row.note.trim() }; });
    localStorage.setItem(MEMORY_KEY, JSON.stringify(memory));
  } catch { /* chỉ là tiện ích, bỏ qua nếu trình duyệt chặn lưu trữ */ }
}

function rowsFromHeader(header) {
  const parameters = splitParameters(header.requestedParameters);
  const methods = splitMethods(header.testMethod);
  const memory = readMemory();
  return parameters.map((parameter, index) => {
    const remembered = memory[parameter.toLowerCase()] ?? {};
    return {
      parameter, fixed: true,
      method: methods.length === parameters.length ? methods[index] : methods.join(', '),
      unit: remembered.unit ?? '', result: '', note: remembered.note ?? '',
    };
  });
}

const blankRow = () => ({ parameter: '', method: '', unit: '', result: '', note: '', fixed: false });
const todayInput = () => new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date());

/** Nhập kết quả trên hệ thống: phần thông tin mẫu của phiếu KQKN tự điền, kỹ thuật viên chỉ nhập Đơn vị, Kết quả, Ghi chú. */
export function ResultEntryForm({ options, transferItemId, setTransferItemId, error, setError }) {
  const navigate = useNavigate();
  const [header, setHeader] = useState(null);
  const [loadingHeader, setLoadingHeader] = useState(false);
  const [rows, setRows] = useState([]);
  const [testingDate, setTestingDate] = useState(todayInput);
  const [saving, setSaving] = useState('');

  useEffect(() => {
    if (!transferItemId) { setHeader(null); setRows([]); return undefined; }
    let active = true;
    setLoadingHeader(true); setError('');
    getReportHeader(transferItemId)
      .then((value) => { if (!active) return; setHeader(value); const suggested = rowsFromHeader(value); setRows(suggested.length ? suggested : [blankRow()]); })
      .catch((exception) => { if (active) { setHeader(null); setError(exception.message); } })
      .finally(() => { if (active) setLoadingHeader(false); });
    return () => { active = false; };
  }, [transferItemId, setError]);

  const missing = useMemo(() => rows.filter((row) => !row.result.trim() || !row.parameter.trim()).length, [rows]);
  const updateRow = (index, field, value) => setRows((current) => current.map((row, position) => position === index ? { ...row, [field]: value } : row));

  async function save(submit) {
    if (!rows.length || missing) { setError('Mỗi chỉ tiêu cần có Kết quả (và tên chỉ tiêu với dòng thêm mới).'); return; }
    setSaving(submit ? 'submit' : 'draft'); setError('');
    try {
      const payload = rows.map((row) => ({ parameter: row.parameter.trim(), unit: row.unit.trim() || null, method: row.method.trim() || null, result: row.result.trim(), note: row.note.trim() || null }));
      const release = await createResultFromEntry({ transferItemId: Number(transferItemId), testingDate: testingDate || null, rows: payload, submit });
      writeMemory(rows);
      navigate(`/admin/results/${release.id}`, { state: { message: submit ? 'Đã tạo biên bản và gửi Admin duyệt.' : 'Đã lưu nháp phiếu kết quả.' } });
    } catch (exception) { setError(exception.message); }
    finally { setSaving(''); }
  }

  const selected = options.find((item) => String(item.id) === String(transferItemId));
  // Gom theo Số/No. để chọn Số/No. trước rồi chọn từng mẫu của nó (một Số/No. thường có nhiều mẫu).
  const groups = useMemo(() => {
    const map = new Map();
    options.forEach((item) => { const key = item.requestNumber || '—'; map.set(key, [...(map.get(key) ?? []), item]); });
    return map;
  }, [options]);
  const requestKey = selected ? (selected.requestNumber || '—') : [...groups.keys()][0] ?? '';
  const requestItems = groups.get(requestKey) ?? [];

  return <form className="result-entry-form" onSubmit={(event) => { event.preventDefault(); save(true); }}>
    <section className="panel-card intake-form-card"><FormHeading number="I" title="Mẫu thử nghiệm" en="Sample" copy="Chỉ mẫu đã được KCS nhận mới có trong danh sách." />
      <label>Số/No.<select value={requestKey} onChange={(event) => { const first = groups.get(event.target.value)?.[0]; if (first) setTransferItemId(String(first.id)); }} required>
        {[...groups].map(([key, items]) => <option key={key} value={key}>{key} · {items.length} mẫu</option>)}</select></label>
      <fieldset className="sample-choice-group"><legend>Chọn mẫu của {requestKey || 'Số/No.'} ({requestItems.length})</legend>
        <div className="sample-choice-list">{requestItems.map((item) => <label className={`sample-choice ${String(item.id) === String(transferItemId) ? 'checked' : ''}`} key={item.id}>
          <input type="radio" name="sample-choice" value={item.id} checked={String(item.id) === String(transferItemId)} onChange={() => setTransferItemId(String(item.id))} />
          <span className="sample-choice-body"><strong>{item.sampleName || 'Mẫu'}</strong><code>{item.sampleCode}</code>
            <small>{[item.quantity, item.weight].filter(Boolean).join(' · ') || 'Chưa có số lượng'}</small>
            <span className="sample-choice-params">{splitParameters(item.requestedParameters).map((parameter) => <em key={parameter}>{parameter}</em>)}</span></span>
        </label>)}</div>
      </fieldset>
      {selected && <div className="testing-sample-summary"><span><small>Số/No.</small><strong>{selected.requestNumber || '—'}</strong></span><span><small>Mã hóa mẫu</small><strong>{selected.sampleCode}</strong></span><span><small>Hạn trả</small><strong>{formatDate(selected.dueDate) || '—'}</strong></span><span><small>Chỉ tiêu</small><strong>{splitParameters(selected.requestedParameters).length || '—'}</strong></span></div>}
    </section>

    <section className="panel-card intake-form-card kqkn-sheet"><FormHeading number="II" title="Thông tin mẫu" en="Sample information" copy="Tự điền từ phiếu tiếp nhận và phiếu chuyển mẫu. Cần sửa thì sửa ở phiếu tiếp nhận." />
      {loadingHeader ? <p className="muted">Đang lấy thông tin mẫu…</p> : header && <ol className="kqkn-info">
        <Info no="1" label="Đơn vị/ Khách hàng" en="Customer" value={header.customerName} />
        <Info no="2" label="Địa chỉ" en="Address" value={header.customerAddress} />
        <Info no="3" label="Tên mẫu" en="Name of sample" value={<>
          <strong>{header.sampleName || '—'}</strong>
          {[['Mã vùng trồng', header.orchardCode], ['Mã cơ sở đóng gói', header.packingFacility], ['Biển số xe', header.truckNumber], ['Biển số mooc', header.moocNumber], ['Số container', header.containerNumber], ['Số seal container', header.sealNumber]]
            .filter(([, value]) => value).map(([label, value]) => <small key={label}>{label}: {value}</small>)}
        </>} />
        <Info no="4" label="Mã số mẫu" en="Sample code" value={header.sampleCode} mono />
        <Info no="5" label="Người lấy mẫu" en="Sample collector" value={header.sampleCollector} />
        <Info no="6" label="Ngày lấy mẫu" en="Sampling date" value={formatDate(header.samplingDate)} />
        <Info no="7" label="Tình trạng mẫu" en="Sample condition" value={header.sampleCondition} />
        <Info no="8" label="Lượng mẫu" en="Sample quantity" value={header.sampleQuantity} />
        <Info no="9" label="Ngày nhận mẫu" en="Date sample received" value={formatDate(header.dateReceived)} />
        <li className="kqkn-info-row editable"><span className="kqkn-no">10.</span><span className="kqkn-label">Ngày thử nghiệm<i>Date of testing</i></span><span className="kqkn-value"><DateInput value={testingDate} onChange={(event) => setTestingDate(event.target.value)} /></span></li>
      </ol>}
    </section>

    <section className="panel-card intake-form-card kqkn-sheet"><FormHeading number="III" title="Kết quả thử nghiệm" en="Test results" copy="Chỉ cần nhập Đơn vị, Kết quả và Ghi chú. Chỉ tiêu và phương pháp lấy từ phiếu chuyển mẫu." />
      <div className="table-wrap"><table className="staff-table kqkn-table"><thead><tr><th>STT<i>No.</i></th><th>Chỉ tiêu thử nghiệm<i>Parameters</i></th><th>Đơn vị<i>Unit</i></th><th>Phương pháp<i>Test method</i></th><th>Kết quả<i>Results</i></th><th>Ghi chú<i>Note</i></th><th aria-label="Thao tác" /></tr></thead><tbody>
        {rows.map((row, index) => <tr key={index}>
          <td data-label="STT" className="report-no">{index + 1}</td>
          <td data-label="Chỉ tiêu">{row.fixed ? <strong>{row.parameter}</strong> : <input aria-label={`Chỉ tiêu ${index + 1}`} value={row.parameter} onChange={(event) => updateRow(index, 'parameter', event.target.value)} placeholder="Tên chỉ tiêu" maxLength={200} required />}</td>
          <td data-label="Đơn vị"><input aria-label={`Đơn vị ${row.parameter || index + 1}`} list="kqkn-units" value={row.unit} onChange={(event) => updateRow(index, 'unit', event.target.value)} placeholder="VD: µg/kg" maxLength={60} /></td>
          <td data-label="Phương pháp">{row.fixed && row.method ? <span>{row.method}</span> : <input aria-label={`Phương pháp ${index + 1}`} value={row.method} onChange={(event) => updateRow(index, 'method', event.target.value)} placeholder="CFC-01:2026" maxLength={200} />}</td>
          <td data-label="Kết quả"><input className="kqkn-result" aria-label={`Kết quả ${row.parameter || index + 1}`} list="kqkn-results" value={row.result} onChange={(event) => updateRow(index, 'result', event.target.value)} placeholder="KPH, < LOQ, 0,012…" maxLength={120} required /></td>
          <td data-label="Ghi chú"><input aria-label={`Ghi chú ${row.parameter || index + 1}`} value={row.note} onChange={(event) => updateRow(index, 'note', event.target.value)} placeholder="VD: LOQ = …" maxLength={255} /></td>
          <td>{!row.fixed && <button type="button" className="row-remove" aria-label={`Xóa dòng ${index + 1}`} onClick={() => setRows(rows.filter((_, position) => position !== index))}>✕</button>}</td>
        </tr>)}
      </tbody></table></div>
      <datalist id="kqkn-units">{UNITS.map((unit) => <option key={unit} value={unit} />)}</datalist>
      <datalist id="kqkn-results">{RESULTS.map((value) => <option key={value} value={value} />)}</datalist>
      <button type="button" className="outline-button add-row-button" onClick={() => setRows([...rows, blankRow()])} disabled={rows.length >= 40}>＋ Thêm chỉ tiêu</button>
      <p className="field-help">KPH: không phát hiện. Số phiếu, ngày phát hành và mã QR được cấp khi Admin phát hành.</p>
    </section>

    <aside className="form-submit-panel result-entry-submit"><p><strong>{missing ? `Còn ${missing} chỉ tiêu chưa có kết quả` : 'Sẵn sàng gửi duyệt'}</strong><span>Hệ thống tạo biên bản thử nghiệm và phiếu KQKN (xem trước PDF, tải Word). Admin kiểm tra, duyệt và phát hành.</span></p>
      <div className="result-entry-actions">
        <button type="button" className="outline-button" disabled={Boolean(saving) || !header} onClick={() => save(false)}>{saving === 'draft' ? 'Đang lưu…' : 'Lưu nháp'}</button>
        <button className="primary-button" disabled={Boolean(saving) || !header || missing > 0}>{saving === 'submit' ? 'Đang gửi…' : 'Tạo biên bản & gửi Admin duyệt'}</button>
      </div>
    </aside>
  </form>;
}

function FormHeading({ number, title, en, copy }) { return <div className="form-card-heading"><span className="form-number">{number}</span><div><h2>{title} <span className="heading-en">/ {en}</span></h2><p>{copy}</p></div></div>; }
function Info({ no, label, en, value, mono }) {
  return <li className="kqkn-info-row"><span className="kqkn-no">{no}.</span><span className="kqkn-label">{label}<i>{en}</i></span><span className={`kqkn-value${mono ? ' mono' : ''}`}>{value || '—'}</span></li>;
}
function formatDate(value) {
  if (!value) return '';
  const [year, month, day] = String(value).slice(0, 10).split('-');
  return day && month && year ? `${day}/${month}/${year}` : value;
}
