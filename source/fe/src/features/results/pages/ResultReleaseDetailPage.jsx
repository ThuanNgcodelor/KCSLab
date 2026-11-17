import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { StatusBadge } from '../../../shared/components/StatusBadge';
import { labelsOf } from '../../../shared/status';
import { PageLoader } from '../../../shared/components/Loading';
import { Icon } from '../../../shared/components/Icon';
import {
  downloadResultFile, getResultRelease, openReportPreview, saveResultReport, getResultReleaseEvents, publishResultRelease, removeResultFile,
  returnResultRelease, reviseResultRelease, revokeResultRelease, submitResultRelease, uploadResultFiles,
} from '../api/resultsApi';

export const resultStatusLabels = labelsOf('release');
const eventLabels = { CREATED: 'Lập hồ sơ', REPORT_UPDATED: 'Cập nhật nội dung phiếu', FILE_ADDED: 'Thêm file', FILE_REMOVED: 'Gỡ file', SUBMITTED: 'Gửi duyệt', RETURNED: 'Trả lại', PUBLISHED: 'Phát hành', REVOKED: 'Thu hồi', REVISION_CREATED: 'Lập phiên bản mới' };
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
  const [publicIds, setPublicIds] = useState([]);
  const [rows, setRows] = useState([]);
  const [signers, setSigners] = useState({ qualityHeadName: '', directorName: '' });
  const [confirm, setConfirm] = useState(null);

  const load = useCallback(async () => {
    setError('');
    try {
      const [detail, history] = await Promise.all([getResultRelease(id), getResultReleaseEvents(id)]);
      setRelease(detail); setEvents(history); setPublicIds([]);
      const source = detail.items.length ? detail.items : detail.suggestedItems;
      setRows(source.length ? source.map((item) => ({ parameter: item.parameter, unit: item.unit ?? '', method: item.method ?? '', result: item.result ?? '', note: item.note ?? '' })) : [emptyRow()]);
      setSigners({ qualityHeadName: detail.qualityHeadName ?? '', directorName: detail.directorName ?? '' });
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

  // The creator (Technician or Admin) edits; approval, return and revoke belong to an Admin who is not the creator.
  const isTech = ['TECHNICIAN', 'ADMIN'].includes(user?.role) && user.id === release.createdByUserId;
  const isAdmin = user?.role === 'ADMIN' && user.id !== release.createdByUserId;
  const editable = release.status === 'DRAFT' || release.status === 'RETURNED';
  const selected = publicIds;
  const updateRow = (index, field, value) => setRows(rows.map((row, position) => position === index ? { ...row, [field]: value } : row));
  const toggle = (fileId) => setPublicIds(selected.includes(fileId) ? selected.filter((value) => value !== fileId) : [...selected, fileId]);

  return <section className="admin-page staff-page result-detail-page">
    <header className="admin-heading staff-heading"><div>
      <button className="back-link" onClick={() => navigate('/admin/results')}>← Hồ sơ kết quả</button>
      <p className="section-kicker">Biên bản {release.testingRecordCode} · Mẫu {release.sampleCode}</p>
      <h1>Hồ sơ kết quả v{release.version} <StatusBadge domain="release" value={release.status} size="large" /></h1>
      <p className="heading-copy">Lập bởi {release.createdByName}{release.resultNumber ? ` · Số phiếu ${release.resultNumber}` : ''}</p></div></header>
    {error && <p className="form-error intake-toast" role="alert">{error}</p>}
    {release.decisionNote && release.status === 'RETURNED' && <p className="handoff-result rejected"><strong>Lý do trả lại ({release.decidedByName})</strong><span>{release.decisionNote}</span></p>}
    {release.revokeReason && release.status === 'REVOKED' && <p className="handoff-result rejected"><strong>Lý do thu hồi ({release.revokedByName})</strong><span>{release.revokeReason}</span></p>}
    <div className="receipt-detail-grid">
      <main className="receipt-form-main">
        <section className="panel-card intake-form-card report-editor"><div className="form-card-heading"><span className="form-number">§</span><div><h2>Nội dung phiếu kết quả (KQKN-DV)</h2><p>Phiếu PDF song ngữ được hệ thống tạo từ dữ liệu có cấu trúc: thông tin mẫu lấy từ Phiếu 3/4/5, bảng kết quả nhập tại đây. Số phiếu, ngày và mã QR được cấp khi phát hành.</p></div></div>
          <div className="report-rows" role="table" aria-label="Bảng kết quả thử nghiệm">
            <div className="report-row report-head" role="row"><span>STT</span><span>Chỉ tiêu</span><span>Đơn vị</span><span>Phương pháp</span><span>Kết quả</span><span>Ghi chú</span><span /></div>
            {rows.map((row, index) => <div className="report-row" role="row" key={index}>
              <span className="report-no">{index + 1}</span>
              <input aria-label={`Chỉ tiêu ${index + 1}`} disabled={!(isTech && editable)} value={row.parameter} onChange={(event) => updateRow(index, 'parameter', event.target.value)} placeholder="Cadimium (Cd)" maxLength={200} />
              <input aria-label={`Đơn vị ${index + 1}`} disabled={!(isTech && editable)} value={row.unit} onChange={(event) => updateRow(index, 'unit', event.target.value)} placeholder="µg/kg" maxLength={60} />
              <input aria-label={`Phương pháp ${index + 1}`} disabled={!(isTech && editable)} value={row.method} onChange={(event) => updateRow(index, 'method', event.target.value)} placeholder="CFC-02:2026 (1)(2)(3)" maxLength={200} />
              <input aria-label={`Kết quả ${index + 1}`} disabled={!(isTech && editable)} value={row.result} onChange={(event) => updateRow(index, 'result', event.target.value)} placeholder="KPH / < LOQ / 1,2" maxLength={120} />
              <input aria-label={`Ghi chú ${index + 1}`} disabled={!(isTech && editable)} value={row.note} onChange={(event) => updateRow(index, 'note', event.target.value)} placeholder="LOQ = 30 µg/kg" maxLength={255} />
              {isTech && editable ? <button type="button" className="outline-button reject-button small-button" aria-label={`Xóa dòng ${index + 1}`} disabled={rows.length === 1} onClick={() => setRows(rows.filter((_, position) => position !== index))}>×</button> : <span />}
            </div>)}
          </div>
          <div className="report-signers"><label>Trưởng phòng Quản lý chất lượng sản phẩm<input disabled={!(isTech && editable)} value={signers.qualityHeadName} onChange={(event) => setSigners({ ...signers, qualityHeadName: event.target.value })} maxLength={120} /></label>
            <label>Tổng Giám đốc<input disabled={!(isTech && editable)} value={signers.directorName} onChange={(event) => setSigners({ ...signers, directorName: event.target.value })} maxLength={120} /></label></div>
          <div className="report-actions">
            {isTech && editable && <button type="button" className="outline-button small-button" onClick={() => setRows([...rows, emptyRow()])} disabled={rows.length >= 40}>＋ Thêm chỉ tiêu</button>}
            {isTech && editable && <button type="button" className="primary-button small-button" disabled={busy} onClick={() => run(() => saveResultReport(release.id, { rows: rows.filter((row) => row.parameter.trim() || row.result.trim()).map(cleanRow), ...signers }))}>Lưu nội dung</button>}
            {release.items.length > 0 && release.status !== 'PUBLISHED' && release.status !== 'REVOKED' && <button type="button" className="outline-button small-button" disabled={busy} onClick={() => run(() => openReportPreview(release.id), { reload: false })}>Xem trước PDF</button>}
          </div>
          {isTech && editable && <small className="muted">Lưu nội dung trước khi xem trước hoặc gửi duyệt. Tên người ký lấy mặc định từ biểu mẫu LAB, có thể sửa; cần LAB xác nhận người ký.</small>}
        </section>
        {release.reference && <section className="panel-card intake-form-card reference-card"><div className="form-card-heading"><span className="form-number">5</span><div><h2>Số liệu từ biên bản thử nghiệm (Phiếu 5)</h2><p>Biên bản <Link to={`/admin/testing/${release.reference.testingRecordId}`}>{release.reference.recordCode}</Link> là hồ sơ làm việc nội bộ (thiết bị, hóa chất, QC, số liệu thô). Kết quả cuối cùng bạn chuyển sang bảng phiếu KQKN phía trên.</p></div></div>
          <div className="reference-meta"><span><small>Chỉ tiêu yêu cầu (Phiếu 4)</small><strong>{release.reference.requestedParameters || '—'}</strong></span><span><small>Phương pháp</small><strong>{release.reference.testMethod || '—'}</strong></span><span><small>Thời gian thử nghiệm</small><strong>{release.reference.analysisStart ? new Date(release.reference.analysisStart).toLocaleString('vi-VN') : '—'}</strong></span><span><small>Độ thu hồi</small><strong>{release.reference.recoveryPercent ?? '—'}</strong></span></div>
          {release.reference.measurements.length === 0 ? <p className="muted">Biên bản chưa có dòng số liệu.</p> : <div className="reference-table" role="table" aria-label="Số liệu từ Phiếu 5"><div className="reference-row reference-head" role="row"><span>Loại</span><span>Lần lặp</span><span>Kết quả/lần</span><span>Kết quả</span><span>Độ lệch</span></div>{release.reference.measurements.map((measurement, index) => <div className="reference-row" role="row" key={index}><span>{measurement.type}</span><span>{measurement.replicate}</span><span>{measurement.resultPerReplicate || '—'}</span><span>{measurement.result || '—'}</span><span>{measurement.deviation || '—'}</span></div>)}</div>}
        </section>}
        <section className="panel-card intake-form-card"><div className="form-card-heading"><span className="form-number">•</span><div><h2>File đính kèm nội bộ ({release.files.filter((file) => file.kind !== 'GENERATED_REPORT').length}/10)</h2><p>Tùy chọn: dữ liệu thô, bảng tính… PDF, DOCX hoặc XLSX, tối đa 10 MB. Mặc định chỉ nội bộ; Admin có thể chọn công khai khi phát hành.</p></div></div>
          {release.files.length === 0 ? <p className="muted">Chưa có file.</p> : <ul className="result-file-list">{release.files.map((file) => <li key={file.id}>
            <div><strong>{file.originalName}</strong><small>{formatSize(file.size)} · {file.uploadedByName} · SHA-256 {file.checksum.slice(0, 12)}…</small></div>
            {file.kind === 'GENERATED_REPORT' && <span className="badge-public">Phiếu KQKN (PDF) — do hệ thống tạo</span>}
            {isAdmin && release.status === 'SUBMITTED' && file.kind !== 'GENERATED_REPORT' && <label className="check-inline"><input type="checkbox" checked={selected.includes(file.id)} onChange={() => toggle(file.id)} /> Công khai kèm theo{file.contentType !== 'application/pdf' ? ' (Word/Excel)' : ''}</label>}
            {release.status === 'PUBLISHED' && file.kind !== 'GENERATED_REPORT' && <span className={file.publicDownload ? 'badge-public' : 'badge-private'}>{file.publicDownload ? 'Công khai' : 'Nội bộ'}</span>}
            <button type="button" className="outline-button small-button" disabled={busy} onClick={() => run(() => downloadResultFile(release.id, file), { reload: false })}>Tải</button>
            {isTech && editable && <button type="button" className="outline-button reject-button small-button" disabled={busy} onClick={() => setConfirm({ title: 'Gỡ file khỏi hồ sơ?', description: file.originalName, confirmLabel: 'Gỡ file', tone: 'danger', run: () => run(() => removeResultFile(release.id, file.id)) })}>Gỡ</button>}
          </li>)}</ul>}
          {isTech && editable && <form className="result-upload" onSubmit={(event) => { event.preventDefault(); run(async () => { await uploadResultFiles(release.id, chosen); setChosen([]); event.target.reset(); }); }}>
            <label className="file-drop compact-file-drop"><input type="file" multiple accept={ACCEPT} onChange={(event) => setChosen(Array.from(event.target.files ?? []))} /><span className="upload-symbol"><Icon name="upload" size={20} /></span><strong>{chosen.length ? `${chosen.length} file đã chọn` : 'Chọn một hoặc nhiều file'}</strong><small>Nếu một file không hợp lệ, không file nào được lưu.</small></label>
            <button className="primary-button" type="submit" disabled={busy || chosen.length === 0}>{busy ? 'Đang tải lên…' : 'Tải lên'}</button></form>}
        </section>
        <section className="panel-card intake-form-card"><div className="form-card-heading"><span className="form-number">⟲</span><div><h2>Lịch sử (không thể sửa)</h2></div></div>
          <ol className="coding-history">{events.map((event) => <li key={event.id}><strong>{eventLabels[event.eventType] ?? event.eventType}</strong> · {event.actor} · {new Date(event.createdAt).toLocaleString('vi-VN')}{event.note && <> · {event.note}</>}</li>)}</ol></section>
      </main>
      <aside className="receipt-form-aside">
        {release.status === 'PUBLISHED' && <section className="panel-card custody-card"><p className="section-kicker">Tra cứu công khai</p><Info label="Mã tra cứu" value={release.lookupCode} mono /><Info label="Số phiếu" value={release.resultNumber} /><small className="muted">Mã QR/đường dẫn: <code>/tracuu/?code={release.lookupCode}</code>. Khách vẫn phải nhập 4 số cuối điện thoại. Chưa triển khai public.</small></section>}
        {release.status === 'SUBMITTED' && release.createdByUserId === user?.id && <section className="panel-card custody-card"><p className="section-kicker">Đang chờ duyệt</p><small className="muted">Hồ sơ do bạn lập nên cần một Admin khác duyệt và phát hành (người duyệt phải độc lập).</small></section>}
        {isTech && editable && <section className="form-submit-panel"><p><strong>Gửi người duyệt</strong><span>Cần lưu ít nhất một chỉ tiêu và tên người ký. Sau khi gửi bạn không thể sửa.</span></p><button className="primary-button" disabled={busy || release.items.length === 0} onClick={() => setConfirm({ title: 'Gửi hồ sơ để duyệt?', description: 'Sau khi gửi, bạn không thể thêm hoặc gỡ file cho tới khi được trả lại.', confirmLabel: 'Gửi duyệt', run: () => run(() => submitResultRelease(release.id)) })}>Gửi duyệt</button></section>}
        {isAdmin && release.status === 'SUBMITTED' && <section className="panel-card intake-form-card"><div className="form-card-heading"><span className="form-number"><Icon name="check" size={14} /></span><div><h2>Duyệt hồ sơ</h2><p>Người duyệt phải khác người lập hồ sơ và người phân tích. Phiếu KQKN (PDF) luôn công khai; file đính kèm chỉ công khai nếu bạn tích chọn.</p></div></div>
          <label>Lý do (bắt buộc khi trả lại)<textarea value={reason} onChange={(event) => setReason(event.target.value)} maxLength={500} /></label>
          <div className="transfer-decision-actions"><button className="outline-button reject-button" disabled={busy || !reason.trim()} onClick={() => setConfirm({ title: 'Trả lại hồ sơ?', description: 'Hồ sơ quay về kỹ thuật viên để bổ sung.', confirmLabel: 'Trả lại', tone: 'danger', run: () => run(() => returnResultRelease(release.id, reason.trim())) })}>Trả lại</button>
            <button className="primary-button" disabled={busy || selected.length === 0} onClick={() => setConfirm({ title: 'Phát hành kết quả?', description: 'Hệ thống sẽ cấp số phiếu, tạo PDF phiếu KQKN và mã tra cứu; khách tra cứu bằng mã (hoặc QR) + 4 số cuối điện thoại. Chỉ mẫu đã xác minh mã mới được phát hành.', confirmLabel: 'Phát hành', run: () => run(() => publishResultRelease(release.id, selected)) })}>Phát hành</button></div></section>}
        {isAdmin && release.status === 'PUBLISHED' && <section className="panel-card intake-form-card"><div className="form-card-heading"><span className="form-number">!</span><div><h2>Thu hồi</h2><p>Kết quả sẽ biến mất khỏi tra cứu công khai ngay lập tức.</p></div></div>
          <label>Lý do thu hồi<textarea value={reason} onChange={(event) => setReason(event.target.value)} maxLength={500} /></label>
          <button className="outline-button reject-button" disabled={busy || !reason.trim()} onClick={() => setConfirm({ title: 'Thu hồi kết quả đã phát hành?', description: 'Khách sẽ không còn tra cứu hoặc tải được kết quả này.', confirmLabel: 'Thu hồi', tone: 'danger', run: () => run(() => revokeResultRelease(release.id, reason.trim())) })}>Thu hồi</button></section>}
        {['TECHNICIAN', 'ADMIN'].includes(user?.role) && release.status === 'REVOKED' && <section className="form-submit-panel"><p><strong>Lập phiên bản mới</strong><span>Tạo bản nháp v{release.version + 1} để sửa và phát hành lại.</span></p><button className="primary-button" disabled={busy} onClick={() => run(async () => { const next = await reviseResultRelease(release.id); navigate(`/admin/results/${next.id}`); }, { reload: false })}>Lập phiên bản mới</button></section>}
      </aside>
    </div>
    {confirm && <ConfirmDialog title={confirm.title} description={confirm.description} confirmLabel={confirm.confirmLabel} tone={confirm.tone} onCancel={() => setConfirm(null)} onConfirm={() => { const action = confirm.run; setConfirm(null); action(); }} />}
  </section>;
}

function emptyRow() { return { parameter: '', unit: '', method: '', result: '', note: '' }; }
function cleanRow(row) { return { parameter: row.parameter.trim(), unit: row.unit.trim() || null, method: row.method.trim() || null, result: row.result.trim(), note: row.note.trim() || null }; }
function Info({ label, value, mono }) { return <span className="info-row"><small>{label}</small><strong className={mono ? 'mono' : ''}>{value || '—'}</strong></span>; }
function formatSize(value) { return value < 1024 * 1024 ? `${Math.ceil(value / 1024)} KB` : `${(value / 1024 / 1024).toFixed(1)} MB`; }
