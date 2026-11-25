import { useEffect, useRef, useState } from 'react';
import * as api from '../api/dossierApi';
import { bytes, count, scopeLabel } from '../dossierFormat';

/** Shows the size of the chosen scope before anything is queued. */
export function ExportPanel({ scope, onClose, onCreated }) {
  const [summary, setSummary] = useState(null);
  const [allowMissing, setAllowMissing] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const operation = useRef(null);
  const panel = useRef(null);
  const { label, ...filters } = scope; // eslint-disable-line no-unused-vars
  const key = JSON.stringify(filters);

  useEffect(() => {
    const controller = new AbortController(); setSummary(null); setError('');
    panel.current?.scrollIntoView({ block: 'nearest' });
    api.getSummary(JSON.parse(key), controller.signal).then(setSummary).catch((e) => { if (e.name !== 'AbortError') setError(e.message); });
    return () => controller.abort();
  }, [key]);

  async function create() {
    setBusy(true); setError('');
    // The same request id is reused when the Admin retries after a network error, so no duplicate job is created.
    const fingerprint = `${key}|${allowMissing}`;
    if (operation.current?.fingerprint !== fingerprint) operation.current = { fingerprint, id: api.operationId() };
    try { await api.createExport({ ...filters, allowMissing, requestId: operation.current.id }); onCreated(); }
    catch (e) { setError(e.message); setBusy(false); }
  }

  return <section ref={panel} className="panel-card dossier-export-panel" aria-label="Tạo lượt xuất">
    <div className="dossier-toolbar"><h2>Xuất: {scopeLabel(scope)}</h2><button type="button" className="outline-button" onClick={onClose}>Đóng</button></div>
    {!summary && !error && <p role="status">Đang ước tính dung lượng…</p>}
    {summary && <p className="dossier-estimate"><strong>{count(summary.dossiers)}</strong> hồ sơ · <strong>{count(summary.files)}</strong> file · khoảng <strong>{bytes(summary.bytes)}</strong></p>}
    <p className="dossier-muted">Gồm mọi phiên bản còn lưu (bản cũ và nháp nằm riêng trong 04-lich-su) và dữ liệu phiếu. Hệ thống đóng gói ở nền, chia thành các ZIP; bản tải tạm giữ 7 ngày.</p>
    <label className="dossier-check"><input type="checkbox" checked={allowMissing} onChange={(e) => setAllowMissing(e.target.checked)} />
      <span>Cho phép thiếu file: vẫn đóng gói khi có file không đọc được hoặc sai checksum, ghi rõ trong danh mục.</span></label>
    {error && <p className="form-error" role="alert">{error}</p>}
    <button type="button" className="primary-button" disabled={busy || !summary || !summary.dossiers} onClick={create}>{busy ? 'Đang tạo…' : 'Tạo lượt xuất'}</button>
  </section>;
}
