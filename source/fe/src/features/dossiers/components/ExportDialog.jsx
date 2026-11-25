import { useEffect, useRef, useState } from 'react';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import * as api from '../api/dossierApi';
import { bytes, count, scopeLabel } from '../dossierFormat';

/** Shows the size of the chosen scope before anything is queued. */
export function ExportDialog({ scope, onClose, onCreated }) {
  const [summary, setSummary] = useState(null);
  const [allowMissing, setAllowMissing] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const operation = useRef(null);
  const filters = { year: scope.year, month: scope.month, receiptId: scope.receiptId, q: scope.q || null };
  const key = JSON.stringify(filters);

  useEffect(() => {
    const controller = new AbortController();
    api.getSummary(JSON.parse(key), controller.signal).then(setSummary).catch((e) => { if (e.name !== 'AbortError') setError(e.message); });
    return () => controller.abort();
  }, [key]);

  async function create() {
    if (busy) return;
    setBusy(true); setError('');
    // The same request id is reused when the Admin retries after a network error, so no duplicate job is created.
    const fingerprint = `${key}|${allowMissing}`;
    if (operation.current?.fingerprint !== fingerprint) operation.current = { fingerprint, id: api.operationId() };
    try { await api.createExport({ ...filters, allowMissing, requestId: operation.current.id }); onCreated(); }
    catch (e) { setError(e.message); setBusy(false); }
  }

  return <ConfirmDialog title={`Xuất hồ sơ: ${scope.label ?? scopeLabel(scope)}`} confirmLabel={busy ? 'Đang tạo…' : 'Tạo lượt xuất'} confirmDisabled={busy || !summary?.dossiers}
    description="Gồm mọi phiên bản còn lưu và dữ liệu phiếu. Hệ thống đóng gói ở nền thành các file ZIP; gói tải giữ 7 ngày."
    onCancel={onClose} onConfirm={create}>
    {!summary && !error && <p role="status">Đang ước tính dung lượng…</p>}
    {summary && <p className="dossier-estimate"><strong>{count(summary.dossiers)}</strong> hồ sơ · <strong>{count(summary.files)}</strong> file · khoảng <strong>{bytes(summary.bytes)}</strong></p>}
    <label className="dossier-check"><input type="checkbox" checked={allowMissing} onChange={(e) => setAllowMissing(e.target.checked)} />
      <span>Cho phép thiếu file: vẫn đóng gói khi có file không đọc được hoặc sai checksum, ghi rõ trong danh mục.</span></label>
    {error && <p className="dossier-dialog-error" role="alert">{error}</p>}
  </ConfirmDialog>;
}
