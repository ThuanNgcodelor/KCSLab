import { useCallback, useEffect, useState } from 'react';
import { StatusBadge } from '../../../shared/components/StatusBadge';
import { labelsOf } from '../../../shared/status';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import {
  correctSampleCoding, getSampleCoding, getSampleCodingEvents, rejectSampleCoding,
  replaceManualSampleCode, submitSampleCoding, verifySampleCoding,
} from '../api/intakeApi';
import { useAuth } from '../../auth/hooks/useAuth';

export const codingLabels = labelsOf('coding');
const eventLabels = { SUBMITTED: 'Gửi đối chiếu', VERIFIED: 'Xác minh', REJECTED: 'Đối chiếu không đạt', CORRECTED: 'Hiệu chỉnh mã' };

/** Manual coding is the current flow. Older states remain visible so historical records are not lost. */
export function SampleCodingPanel({ sample, onChanged }) {
  const { user } = useAuth();
  const [coding, setCoding] = useState(null);
  const [events, setEvents] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [reason, setReason] = useState('');
  const [source, setSource] = useState('');
  const [product, setProduct] = useState('');
  const [confirm, setConfirm] = useState(null);
  const [manualCode, setManualCode] = useState('');

  const load = useCallback(async () => {
    try { setCoding(await getSampleCoding(sample.id)); setError(''); }
    catch (exception) { setError(exception.message); }
  }, [sample.id]);
  useEffect(() => { load(); }, [load]);

  async function run(action) {
    setBusy(true); setError('');
    try {
      const next = await action();
      setCoding(next); setEvents(null); setReason('');
      onChanged?.(next);
    } catch (exception) { setError(exception.message); }
    finally { setBusy(false); }
  }
  async function toggleHistory() {
    if (events) { setEvents(null); return; }
    try { setEvents(await getSampleCodingEvents(sample.id)); } catch (exception) { setError(exception.message); }
  }

  if (!coding) return <div className="coding-panel"><small className="muted">{error || 'Đang tải trạng thái mã hóa…'}</small></div>;
  const status = coding.codingStatus;
  const manualFlow = status === 'VERIFIED' && !coding.verifiedBy && !coding.legacy;
  return <div className="coding-panel">
    <div className="coding-line">
      <StatusBadge domain="coding" value={status} />
      {coding.legacy && <small className="muted">Mã cũ trước khi có bước xác minh (cần LAB xác nhận)</small>}
      {coding.codedBy && <small>Mã hóa: {coding.codedBy}</small>}
      {manualFlow && <small>Mã hóa mẫu do bộ phận tiếp nhận nhập · không cần đối chiếu lại</small>}
      {!manualFlow && coding.verifiedBy && <small>Xác minh: {coding.verifiedBy}</small>}
      <button type="button" className="link-button" onClick={toggleHistory}>{events ? 'Ẩn lịch sử' : 'Lịch sử mã'}</button>
    </div>
    {error && <p className="form-error" role="alert">{error}</p>}
    {manualFlow && user?.role === 'ADMIN' && <div className="coding-actions coding-correction">
      <input value={manualCode} onChange={(event) => setManualCode(event.target.value)} placeholder="Mã mẫu mới" maxLength={100} aria-label="Mã mẫu mới" />
      <input value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Lý do sửa mã" maxLength={500} aria-label="Lý do sửa mã" />
      <button type="button" className="outline-button small-button" disabled={busy || !manualCode.trim() || !reason.trim()}
        onClick={() => setConfirm({ title: 'Sửa mã mẫu?', description: `Mã cũ ${coding.sampleCode} được giữ trong lịch sử. Hồ sơ chuyển mẫu chưa được lập mới có thể sửa mã.`, confirmLabel: 'Sửa mã', run: () => run(() => replaceManualSampleCode(sample.id, manualCode.trim(), reason.trim())) })}>Sửa mã</button>
    </div>}
    {status === 'PENDING_CODING' && <div className="coding-actions">
      <button type="button" className="outline-button small-button" disabled={busy}
        onClick={() => setConfirm({ title: 'Xác nhận đã mã hóa và dán nhãn?', description: `Mã ${coding.sampleCode} sẽ chuyển sang chờ người thứ hai đối chiếu. Bạn không thể tự xác minh mã này.`, confirmLabel: 'Gửi đối chiếu', run: () => run(() => submitSampleCoding(sample.id)) })}>
        Đã mã hóa và dán nhãn</button></div>}
    {status === 'PENDING_VERIFICATION' && <div className="coding-actions">
      <input value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Lý do (bắt buộc khi không đạt)" maxLength={500} aria-label="Lý do đối chiếu không đạt" />
      <button type="button" className="primary-button small-button" disabled={busy}
        onClick={() => setConfirm({ title: 'Xác minh mã đúng?', description: `Bạn xác nhận nhãn, Sổ theo dõi mẫu và phiếu yêu cầu khớp mã ${coding.sampleCode}.`, confirmLabel: 'Xác minh', run: () => run(() => verifySampleCoding(sample.id)) })}>Xác minh</button>
      <button type="button" className="outline-button reject-button small-button" disabled={busy || !reason.trim()}
        onClick={() => setConfirm({ title: 'Đối chiếu không đạt?', description: 'Mẫu cần hiệu chỉnh mã và sẽ chưa thể chuyển đi.', confirmLabel: 'Không đạt', tone: 'danger', run: () => run(() => rejectSampleCoding(sample.id, reason.trim())) })}>Không đạt</button></div>}
    {status === 'CORRECTION_REQUIRED' && <div className="coding-actions coding-correction">
      <input value={source} onChange={(event) => setSource(event.target.value)} placeholder="Mã nguồn mới (nếu đổi)" maxLength={20} aria-label="Mã nguồn mới" />
      <input value={product} onChange={(event) => setProduct(event.target.value)} placeholder="Loại sản phẩm mới (nếu đổi)" maxLength={80} aria-label="Loại sản phẩm mới" />
      <input value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Lý do hiệu chỉnh" maxLength={500} aria-label="Lý do hiệu chỉnh" />
      <button type="button" className="primary-button small-button" disabled={busy || !reason.trim() || (!source.trim() && !product.trim())}
        onClick={() => setConfirm({ title: 'Cấp mã mới cho mẫu?', description: `Mã cũ ${coding.sampleCode} được giữ trong lịch sử và không bao giờ cấp lại.`, confirmLabel: 'Cấp mã mới', run: () => run(() => correctSampleCoding(sample.id, { sourceCode: source.trim() || null, productType: product.trim() || null, reason: reason.trim() })) })}>Cấp mã mới</button></div>}
    {events && <ol className="coding-history">{events.length === 0 ? <li>Chưa có sự kiện.</li> : events.map((event) => <li key={event.id}><strong>{eventLabels[event.eventType] ?? event.eventType}</strong> · {event.actor} · {new Date(event.createdAt).toLocaleString('vi-VN')}{event.eventType === 'CORRECTED' && <> · {event.oldCode} → {event.newCode}</>}{event.reason && <> · {event.reason}</>}</li>)}</ol>}
    {confirm && <ConfirmDialog title={confirm.title} description={confirm.description} confirmLabel={confirm.confirmLabel} tone={confirm.tone}
      onCancel={() => setConfirm(null)} onConfirm={() => { const action = confirm.run; setConfirm(null); action(); }} />}
  </div>;
}
