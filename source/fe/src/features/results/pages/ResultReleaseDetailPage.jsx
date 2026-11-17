import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { PageLoader } from '../../../shared/components/Loading';
import {
  downloadResultFile, getResultRelease, getResultReleaseEvents, publishResultRelease, removeResultFile,
  returnResultRelease, reviseResultRelease, revokeResultRelease, submitResultRelease, uploadResultFiles,
} from '../api/resultsApi';

export const resultStatusLabels = { DRAFT: 'Bản nháp', SUBMITTED: 'Chờ duyệt', RETURNED: 'Đã trả lại', PUBLISHED: 'Đã phát hành', REVOKED: 'Đã thu hồi' };
const eventLabels = { CREATED: 'Lập hồ sơ', FILE_ADDED: 'Thêm file', FILE_REMOVED: 'Gỡ file', SUBMITTED: 'Gửi duyệt', RETURNED: 'Trả lại', PUBLISHED: 'Phát hành', REVOKED: 'Thu hồi', REVISION_CREATED: 'Lập phiên bản mới' };
const ACCEPT = '.pdf,.docx,.xlsx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

export function ResultReleaseDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [release, setRelease] = useState(null);
  const [events, setEvents] = useState([]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [reason, setReason] = useState('');
  const [chosen, setChosen] = useState([]);
  const [publicIds, setPublicIds] = useState(null);
  const [confirm, setConfirm] = useState(null);

  const load = useCallback(async () => {
    setError('');
    try {
      const [detail, history] = await Promise.all([getResultRelease(id), getResultReleaseEvents(id)]);
      setRelease(detail); setEvents(history); setPublicIds(null);
    } catch (exception) { setError(exception.message); }
  }, [id]);
  useEffect(() => { load(); }, [load]);

  async function run(action, { reload = true } = {}) {
    setBusy(true); setError('');
    try { const result = await action(); if (reload) await load(); return result; }
    catch (exception) { setError(exception.message); }
    finally { setBusy(false); }
  }

  if (!release) return <section className="admin-page staff-page">{error ? <p className="form-error" role="alert">{error}</p> : <PageLoader label="Đang tải hồ sơ kết quả…" rows={4} />}</section>;

  const isTech = user?.role === 'TECHNICIAN' && user.id === release.createdByUserId;
  const isAdmin = user?.role === 'ADMIN';
  const editable = release.status === 'DRAFT' || release.status === 'RETURNED';
  const selected = publicIds ?? release.files.filter((file) => file.contentType === 'application/pdf').map((file) => file.id);
  const toggle = (fileId) => setPublicIds(selected.includes(fileId) ? selected.filter((value) => value !== fileId) : [...selected, fileId]);

  return <section className="admin-page staff-page result-detail-page">
    <header className="admin-heading staff-heading"><div>
      <button className="back-link" onClick={() => navigate('/admin/results')}>← Hồ sơ kết quả</button>
      <p className="section-kicker">Biên bản {release.testingRecordCode} · Mẫu {release.sampleCode}</p>
      <h1>Hồ sơ kết quả v{release.version} <span className={`result-status result-${release.status.toLowerCase()}`}>{resultStatusLabels[release.status]}</span></h1>
      <p className="heading-copy">Lập bởi {release.createdByName}{release.resultNumber ? ` · Số phiếu ${release.resultNumber}` : ''}</p></div></header>
    {error && <p className="form-error intake-toast" role="alert">{error}</p>}
    {release.decisionNote && release.status === 'RETURNED' && <p className="handoff-result rejected"><strong>Lý do trả lại ({release.decidedByName})</strong><span>{release.decisionNote}</span></p>}
    {release.revokeReason && release.status === 'REVOKED' && <p className="handoff-result rejected"><strong>Lý do thu hồi ({release.revokedByName})</strong><span>{release.revokeReason}</span></p>}
    <div className="receipt-detail-grid">
      <main className="receipt-form-main">
        <section className="panel-card intake-form-card"><div className="form-card-heading"><span className="form-number">•</span><div><h2>File kết quả ({release.files.length}/10)</h2><p>PDF, DOCX hoặc XLSX, tối đa 10 MB mỗi file. Hệ thống từ chối macro, đối tượng nhúng và file giả mạo.</p></div></div>
          {release.files.length === 0 ? <p className="muted">Chưa có file.</p> : <ul className="result-file-list">{release.files.map((file) => <li key={file.id}>
            <div><strong>{file.originalName}</strong><small>{formatSize(file.size)} · {file.uploadedByName} · SHA-256 {file.checksum.slice(0, 12)}…</small></div>
            {isAdmin && release.status === 'SUBMITTED' && <label className="check-inline"><input type="checkbox" checked={selected.includes(file.id)} onChange={() => toggle(file.id)} /> Công khai{file.contentType !== 'application/pdf' ? ' (Word/Excel)' : ''}</label>}
            {release.status === 'PUBLISHED' && <span className={file.publicDownload ? 'badge-public' : 'badge-private'}>{file.publicDownload ? 'Công khai' : 'Nội bộ'}</span>}
            <button type="button" className="outline-button small-button" disabled={busy} onClick={() => run(() => downloadResultFile(release.id, file), { reload: false })}>Tải</button>
            {isTech && editable && <button type="button" className="outline-button reject-button small-button" disabled={busy} onClick={() => setConfirm({ title: 'Gỡ file khỏi hồ sơ?', description: file.originalName, confirmLabel: 'Gỡ file', tone: 'danger', run: () => run(() => removeResultFile(release.id, file.id)) })}>Gỡ</button>}
          </li>)}</ul>}
          {isTech && editable && <form className="result-upload" onSubmit={(event) => { event.preventDefault(); run(async () => { await uploadResultFiles(release.id, chosen); setChosen([]); event.target.reset(); }); }}>
            <label className="file-drop compact-file-drop"><input type="file" multiple accept={ACCEPT} onChange={(event) => setChosen(Array.from(event.target.files ?? []))} /><span className="upload-symbol">↑</span><strong>{chosen.length ? `${chosen.length} file đã chọn` : 'Chọn một hoặc nhiều file'}</strong><small>Nếu một file không hợp lệ, không file nào được lưu.</small></label>
            <button className="primary-button" type="submit" disabled={busy || chosen.length === 0}>{busy ? 'Đang tải lên…' : 'Tải lên'}</button></form>}
        </section>
        <section className="panel-card intake-form-card"><div className="form-card-heading"><span className="form-number">⟲</span><div><h2>Lịch sử (không thể sửa)</h2></div></div>
          <ol className="coding-history">{events.map((event) => <li key={event.id}><strong>{eventLabels[event.eventType] ?? event.eventType}</strong> · {event.actor} · {new Date(event.createdAt).toLocaleString('vi-VN')}{event.note && <> · {event.note}</>}</li>)}</ol></section>
      </main>
      <aside className="receipt-form-aside">
        {release.status === 'PUBLISHED' && <section className="panel-card custody-card"><p className="section-kicker">Tra cứu công khai</p><Info label="Mã tra cứu" value={release.lookupCode} mono /><Info label="Số phiếu" value={release.resultNumber} /><small className="muted">Mã QR/đường dẫn: <code>/tracuu/?code={release.lookupCode}</code>. Khách vẫn phải nhập 4 số cuối điện thoại. Chưa triển khai public.</small></section>}
        {isTech && editable && <section className="form-submit-panel"><p><strong>Gửi người duyệt</strong><span>Cần ít nhất một file. Sau khi gửi bạn không thể sửa.</span></p><button className="primary-button" disabled={busy || release.files.length === 0} onClick={() => setConfirm({ title: 'Gửi hồ sơ để duyệt?', description: 'Sau khi gửi, bạn không thể thêm hoặc gỡ file cho tới khi được trả lại.', confirmLabel: 'Gửi duyệt', run: () => run(() => submitResultRelease(release.id)) })}>Gửi duyệt</button></section>}
        {isAdmin && release.status === 'SUBMITTED' && <section className="panel-card intake-form-card"><div className="form-card-heading"><span className="form-number">✓</span><div><h2>Duyệt hồ sơ</h2><p>Người duyệt phải khác người lập hồ sơ và người phân tích. Mặc định chỉ PDF được công khai.</p></div></div>
          <label>Lý do (bắt buộc khi trả lại)<textarea value={reason} onChange={(event) => setReason(event.target.value)} maxLength={500} /></label>
          <div className="transfer-decision-actions"><button className="outline-button reject-button" disabled={busy || !reason.trim()} onClick={() => setConfirm({ title: 'Trả lại hồ sơ?', description: 'Hồ sơ quay về kỹ thuật viên để bổ sung.', confirmLabel: 'Trả lại', tone: 'danger', run: () => run(() => returnResultRelease(release.id, reason.trim())) })}>Trả lại</button>
            <button className="primary-button" disabled={busy || selected.length === 0} onClick={() => setConfirm({ title: 'Phát hành kết quả?', description: 'Hồ sơ sẽ tra cứu được bằng mã tra cứu và 4 số cuối điện thoại. Chỉ mẫu đã xác minh mã mới được phát hành.', confirmLabel: 'Phát hành', run: () => run(() => publishResultRelease(release.id, selected)) })}>Phát hành</button></div></section>}
        {isAdmin && release.status === 'PUBLISHED' && <section className="panel-card intake-form-card"><div className="form-card-heading"><span className="form-number">!</span><div><h2>Thu hồi</h2><p>Kết quả sẽ biến mất khỏi tra cứu công khai ngay lập tức.</p></div></div>
          <label>Lý do thu hồi<textarea value={reason} onChange={(event) => setReason(event.target.value)} maxLength={500} /></label>
          <button className="outline-button reject-button" disabled={busy || !reason.trim()} onClick={() => setConfirm({ title: 'Thu hồi kết quả đã phát hành?', description: 'Khách sẽ không còn tra cứu hoặc tải được kết quả này.', confirmLabel: 'Thu hồi', tone: 'danger', run: () => run(() => revokeResultRelease(release.id, reason.trim())) })}>Thu hồi</button></section>}
        {user?.role === 'TECHNICIAN' && release.status === 'REVOKED' && <section className="form-submit-panel"><p><strong>Lập phiên bản mới</strong><span>Tạo bản nháp v{release.version + 1} để sửa và phát hành lại.</span></p><button className="primary-button" disabled={busy} onClick={() => run(async () => { const next = await reviseResultRelease(release.id); navigate(`/admin/results/${next.id}`); }, { reload: false })}>Lập phiên bản mới</button></section>}
      </aside>
    </div>
    {confirm && <ConfirmDialog title={confirm.title} description={confirm.description} confirmLabel={confirm.confirmLabel} tone={confirm.tone} onCancel={() => setConfirm(null)} onConfirm={() => { const action = confirm.run; setConfirm(null); action(); }} />}
  </section>;
}

function Info({ label, value, mono }) { return <span className="info-row"><small>{label}</small><strong className={mono ? 'mono' : ''}>{value || '—'}</strong></span>; }
function formatSize(value) { return value < 1024 * 1024 ? `${Math.ceil(value / 1024)} KB` : `${(value / 1024 / 1024).toFixed(1)} MB`; }
