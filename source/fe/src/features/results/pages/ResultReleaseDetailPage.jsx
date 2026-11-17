import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { StatusBadge } from '../../../shared/components/StatusBadge';
import { labelsOf } from '../../../shared/status';
import { PageLoader } from '../../../shared/components/Loading';
import { Icon } from '../../../shared/components/Icon';
import { PhotoPicker } from '../../../shared/components/PhotoPicker';
import {
  downloadResultFile, getResultRelease, openReportPreview, saveResultReport, getResultReleaseEvents, publishResultRelease, removeResultFile, removeReportFile, uploadReportFile,
  returnResultRelease, reviseResultRelease, revokeResultRelease, submitResultRelease, uploadResultFiles,
} from '../api/resultsApi';

export const resultStatusLabels = labelsOf('release');
const eventLabels = { CREATED: 'Lập hồ sơ', REPORT_UPDATED: 'Cập nhật nội dung phiếu', REPORT_FILE_UPLOADED: 'Tải file phiếu kết quả', REPORT_FILE_REMOVED: 'Gỡ file phiếu kết quả', FILE_ADDED: 'Thêm file', FILE_REMOVED: 'Gỡ file', SUBMITTED: 'Gửi duyệt', RETURNED: 'Trả lại', PUBLISHED: 'Phát hành', REVOKED: 'Thu hồi', REVISION_CREATED: 'Lập phiên bản mới' };
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
  const [reportChoice, setReportChoice] = useState(null);
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

  if (!release) return <section className="staff-page">{error ? <p className="form-error" role="alert">{error}</p> : <PageLoader label="Đang tải hồ sơ kết quả…" rows={4} />}</section>;

  // The creator (Technician or Admin) edits; approval, return and revoke belong to an Admin who is not the creator.
  const isTech = ['TECHNICIAN', 'ADMIN'].includes(user?.role) && user.id === release.createdByUserId;
  const isAdmin = user?.role === 'ADMIN' && user.id !== release.createdByUserId;
  const editable = release.status === 'DRAFT' || release.status === 'RETURNED';
  const selected = publicIds;
  const updateRow = (index, field, value) => setRows(rows.map((row, position) => position === index ? { ...row, [field]: value } : row));
  const toggle = (fileId) => setPublicIds(selected.includes(fileId) ? selected.filter((value) => value !== fileId) : [...selected, fileId]);

  const attachments = release.files.filter((file) => file.kind === 'UPLOAD');
  const reportPdf = release.files.find((file) => file.kind === 'REPORT_PDF');
  const reportSource = release.files.find((file) => file.kind === 'REPORT_SOURCE');
  const report = release.files.find((file) => file.kind === 'GENERATED_REPORT');
  const canEdit = isTech && editable;

  return <section className="staff-page testing-detail-page result-detail-page">
    <header className="admin-heading staff-heading"><div><button className="back-link" onClick={() => navigate('/admin/results')}>← Danh sách hồ sơ kết quả</button><p className="section-kicker">{release.testingRecordCode} / {release.sampleCode}</p><h1>Hồ sơ kết quả v{release.version}</h1><p className="heading-copy">Lập bởi {release.createdByName}{release.documentNumber || release.resultNumber ? ` · Số phiếu ${release.documentNumber || release.resultNumber}` : ''}</p></div><StatusBadge domain="release" value={release.status} size="large" /></header>
    {error && <p className="form-error intake-toast" role="alert">{error}</p>}
    {release.decisionNote && release.status === 'RETURNED' && <p className="handoff-result rejected"><strong>Lý do trả lại ({release.decidedByName})</strong><span>{release.decisionNote}</span></p>}
    {release.revokeReason && release.status === 'REVOKED' && <p className="handoff-result rejected"><strong>Lý do thu hồi ({release.revokedByName})</strong><span>{release.revokeReason}</span></p>}
    <div className="testing-detail-grid"><main className="receipt-form-main">
      <section className="panel-card intake-form-card report-editor"><FormHeading number="01" title="Phiếu kết quả (KQKN-DV)" copy="Tải lên file DOCX hoặc PDF đã soạn, hoặc nhập bảng kết quả. DOCX được chuyển sang PDF; hãy xem trước và đối chiếu kỹ bố cục, nội dung trước khi gửi duyệt. Nếu không tìm thấy ô QR, mã sẽ được đặt ở trang riêng." />
        {reportPdf ? <div className="report-file-card"><span className="report-file-icon"><Icon name="file" size={22} /></span>
          <div><strong>{reportSource ? reportSource.originalName : reportPdf.originalName}</strong><small>{release.documentNumber ? `Số phiếu đọc từ file: ${release.documentNumber}` : 'Không đọc được “Số/ No.” trong file — kiểm tra lại file'} · PDF {formatSize(reportPdf.size)}</small></div>
          <div className="table-actions"><button type="button" className="table-action" disabled={busy} onClick={() => run(() => openReportPreview(release.id), { reload: false })}>Xem trước PDF</button>
            {canEdit && <button type="button" className="table-action danger" disabled={busy} onClick={() => setConfirm({ title: 'Gỡ file phiếu kết quả?', description: 'Bạn có thể tải lên file khác hoặc nhập bảng kết quả.', confirmLabel: 'Gỡ file', tone: 'danger', run: () => run(() => removeReportFile(release.id)) })}>Gỡ</button>}</div></div> : null}
        {canEdit && <form className="result-upload" onSubmit={(event) => { event.preventDefault(); run(async () => { await uploadReportFile(release.id, reportChoice); setReportChoice(null); }); }}>
          <PhotoPicker label={reportPdf ? 'Thay bằng file khác' : 'Tải file phiếu kết quả'} hint="DOCX hoặc PDF, tối đa 10 MB. Không có macro hoặc liên kết ngoài." camera={false} multiple={false} accept=".docx,.pdf,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document" files={reportChoice ? [reportChoice] : []} onChange={(picked) => setReportChoice(picked[0] ?? null)} />
          <div className="form-actions"><button className="primary-button" type="submit" disabled={busy || !reportChoice}>{busy ? 'Đang xử lý (chuyển PDF)…' : 'Tải phiếu lên'}</button></div></form>}
        {reportPdf ? <p className="field-help">Đang dùng file đã tải lên. Bảng nhập tay bên dưới không được dùng.</p> : <><div className="form-divider" /><p className="field-help">Hoặc nhập bảng kết quả để hệ thống tự lập phiếu PDF:</p></>}
      </section>
      {!reportPdf && <section className="panel-card intake-form-card report-editor"><FormHeading number="01b" title="Nhập bảng kết quả" copy="Dùng khi không có file phiếu. Số phiếu, ngày và mã QR được cấp khi phát hành." />
        <div className="table-wrap"><table className="staff-table report-table"><thead><tr><th>STT</th><th>Chỉ tiêu</th><th>Đơn vị</th><th>Phương pháp</th><th>Kết quả</th><th>Ghi chú</th>{canEdit && <th />}</tr></thead><tbody>
          {rows.map((row, index) => <tr key={index}>
            <td className="report-no" data-label="STT">{index + 1}</td>
            <td data-label="Chỉ tiêu">{canEdit ? <input aria-label={`Chỉ tiêu ${index + 1}`} disabled={!canEdit} value={row.parameter} onChange={(event) => updateRow(index, 'parameter', event.target.value)} placeholder="Cadimium (Cd)" maxLength={200} /> : <span className="report-value">{row.parameter || '—'}</span>}</td>
            <td data-label="Đơn vị">{canEdit ? <input aria-label={`Đơn vị ${index + 1}`} disabled={!canEdit} value={row.unit} onChange={(event) => updateRow(index, 'unit', event.target.value)} placeholder="µg/kg" maxLength={60} /> : <span className="report-value">{row.unit || '—'}</span>}</td>
            <td data-label="Phương pháp">{canEdit ? <input aria-label={`Phương pháp ${index + 1}`} disabled={!canEdit} value={row.method} onChange={(event) => updateRow(index, 'method', event.target.value)} placeholder="CFC-02:2026" maxLength={200} /> : <span className="report-value">{row.method || '—'}</span>}</td>
            <td data-label="Kết quả">{canEdit ? <input aria-label={`Kết quả ${index + 1}`} disabled={!canEdit} value={row.result} onChange={(event) => updateRow(index, 'result', event.target.value)} placeholder="KPH / < LOQ / 1,2" maxLength={120} /> : <span className="report-value">{row.result || '—'}</span>}</td>
            <td data-label="Ghi chú">{canEdit ? <input aria-label={`Ghi chú ${index + 1}`} disabled={!canEdit} value={row.note} onChange={(event) => updateRow(index, 'note', event.target.value)} placeholder="LOQ = 30 µg/kg" maxLength={255} /> : <span className="report-value">{row.note || '—'}</span>}</td>
            {canEdit && <td><button type="button" className="row-remove" aria-label={`Xóa dòng ${index + 1}`} disabled={rows.length === 1} onClick={() => setRows(rows.filter((_, position) => position !== index))}><Icon name="close" size={14} /></button></td>}
          </tr>)}
        </tbody></table></div>
        {canEdit && <button type="button" className="outline-button add-row-button" onClick={() => setRows([...rows, emptyRow()])} disabled={rows.length >= 40}>＋ Thêm chỉ tiêu</button>}
        <div className="form-divider" />
        <div className="form-two-columns"><label>Trưởng phòng Quản lý chất lượng<input disabled={!canEdit} value={signers.qualityHeadName} onChange={(event) => setSigners({ ...signers, qualityHeadName: event.target.value })} maxLength={120} /></label>
          <label>Tổng Giám đốc<input disabled={!canEdit} value={signers.directorName} onChange={(event) => setSigners({ ...signers, directorName: event.target.value })} maxLength={120} /></label></div>
        {canEdit && <span className="field-help">Tên người ký lấy mặc định từ biểu mẫu LAB, có thể sửa (cần LAB xác nhận). Lưu nội dung trước khi xem trước hoặc gửi duyệt.</span>}
        <div className="form-actions">
          {(release.items.length > 0 || reportPdf) && release.status !== 'PUBLISHED' && release.status !== 'REVOKED' && <button type="button" className="outline-button" disabled={busy} onClick={() => run(() => openReportPreview(release.id), { reload: false })}>Xem trước PDF</button>}
          {report && <button type="button" className="outline-button" disabled={busy} onClick={() => run(() => downloadResultFile(release.id, report), { reload: false })}>Tải phiếu PDF đã phát hành</button>}
          {canEdit && <button type="button" className="primary-button" disabled={busy} onClick={() => run(() => saveResultReport(release.id, { rows: rows.filter((row) => row.parameter.trim() || row.result.trim()).map(cleanRow), ...signers }))}>{busy ? 'Đang lưu…' : 'Lưu nội dung'}</button>}
        </div>
      </section>}
      {release.reference && <section className="panel-card intake-form-card reference-card"><FormHeading number="02" title="Số liệu từ biên bản thử nghiệm (Phiếu 5)" copy={<>Tham chiếu từ biên bản <Link className="inline-link" to={`/admin/testing/${release.reference.testingRecordId}`}>{release.reference.recordCode}</Link>. Đây là hồ sơ làm việc nội bộ; kết quả cuối cùng nhập vào bảng 01 phía trên.</>} />
        <div className="test-general-grid"><Data label="Chỉ tiêu yêu cầu (Phiếu 4)" value={release.reference.requestedParameters} /><Data label="Phương pháp" value={release.reference.testMethod} /><Data label="Bắt đầu thử nghiệm" value={formatDateTime(release.reference.analysisStart)} /><Data label="Hiệu suất thu hồi" value={release.reference.recoveryPercent == null ? null : `${release.reference.recoveryPercent}%`} /></div>
        {release.reference.measurements.length === 0 ? <p className="muted">Biên bản chưa có dòng số liệu.</p> : <div className="table-wrap"><table className="staff-table record-data-table"><thead><tr><th>Loại</th><th>Lần lặp</th><th>KQ / lần</th><th>Kết quả</th><th>Độ lệch</th></tr></thead><tbody>{release.reference.measurements.map((measurement, index) => <tr key={index}><td data-label="Loại">{measurement.type === 'QC' ? 'QC' : 'Thực nghiệm'}</td><td data-label="Lần lặp">{measurement.replicate || '—'}</td><td data-label="KQ / lần">{measurement.resultPerReplicate || '—'}</td><td data-label="Kết quả">{measurement.result || '—'}</td><td data-label="Độ lệch">{measurement.deviation || '—'}</td></tr>)}</tbody></table></div>}
      </section>}
      <section className="panel-card intake-form-card"><FormHeading number={release.reference ? '03' : '02'} title={`File đính kèm nội bộ (${attachments.length}/10)`} copy="Không bắt buộc: dữ liệu thô, bảng tính… PDF, DOCX hoặc XLSX, tối đa 10 MB mỗi file. Mặc định chỉ dùng nội bộ; Admin duyệt có thể chọn công khai kèm phiếu." />
        {release.files.length === 0 ? <p className="muted">Chưa có file nào.</p> : <div className="table-wrap"><table className="staff-table"><thead><tr><th>Tên file</th><th>Người tải</th><th>Phạm vi</th><th /></tr></thead><tbody>{release.files.map((file) => <tr key={file.id}>
          <td><strong>{file.originalName}</strong><small>{formatSize(file.size)} · SHA-256 {file.checksum.slice(0, 12)}…</small></td>
          <td>{file.uploadedByName}</td>
          <td>{file.kind === 'GENERATED_REPORT' ? <span className="scope-pill public">Phiếu KQKN · công khai</span>
            : isAdmin && release.status === 'SUBMITTED' ? <label className="check-inline"><input type="checkbox" checked={selected.includes(file.id)} onChange={() => toggle(file.id)} /> Công khai kèm phiếu</label>
            : <span className={`scope-pill ${file.publicDownload ? 'public' : ''}`}>{file.publicDownload ? 'Công khai' : 'Nội bộ'}</span>}</td>
          <td><div className="table-actions"><button type="button" className="table-action" disabled={busy} onClick={() => run(() => downloadResultFile(release.id, file), { reload: false })}>Tải</button>
            {canEdit && file.kind !== 'GENERATED_REPORT' && <button type="button" className="table-action danger" disabled={busy} onClick={() => setConfirm({ title: 'Gỡ file khỏi hồ sơ?', description: file.originalName, confirmLabel: 'Gỡ file', tone: 'danger', run: () => run(() => removeResultFile(release.id, file.id)) })}>Gỡ</button>}</div></td>
        </tr>)}</tbody></table></div>}
        {canEdit && <form className="result-upload" onSubmit={(event) => { event.preventDefault(); run(async () => { await uploadResultFiles(release.id, chosen); setChosen([]); }); }}>
          <PhotoPicker label="Thêm file" hint="Nếu một file không hợp lệ, không file nào được lưu." camera={false} accept={ACCEPT} files={chosen} onChange={setChosen} />
          <div className="form-actions"><button className="primary-button" type="submit" disabled={busy || chosen.length === 0}>{busy ? 'Đang tải lên…' : `Tải lên${chosen.length ? ` ${chosen.length} file` : ''}`}</button></div></form>}
      </section>
    </main><aside className="receipt-form-aside testing-review-aside">
      <section className="panel-card custody-card"><p className="section-kicker">Luồng phát hành</p>
        <Timeline complete title="Lập hồ sơ" detail={`${release.createdByName} · ${formatDateTime(release.createdAt)}`} />
        <Timeline complete={Boolean(release.submittedAt) && release.status !== 'DRAFT'} title={release.status === 'RETURNED' ? 'Đã bị trả lại' : 'Gửi duyệt'} detail={release.status === 'RETURNED' ? `${release.decidedByName ?? ''} · ${formatDateTime(release.decidedAt)}` : release.submittedAt ? `${release.submittedByName ?? ''} · ${formatDateTime(release.submittedAt)}` : 'Chưa gửi'} />
        <Timeline complete={Boolean(release.publishedAt)} title="Phát hành" detail={release.publishedAt ? `${release.decidedByName ?? ''} · ${formatDateTime(release.publishedAt)}` : 'Admin khác người lập duyệt'} />
        {release.status === 'REVOKED' && <Timeline complete title="Đã thu hồi" detail={`${release.revokedByName ?? ''} · ${formatDateTime(release.revokedAt)}`} />}
      </section>
        {release.status === 'PUBLISHED' && <section className="panel-card custody-card"><p className="section-kicker">Tra cứu công khai</p><Info label="Mã tra cứu" value={release.lookupCode} mono /><Info label="Số phiếu" value={release.resultNumber} /><small className="muted">Mã QR trên phiếu PDF trỏ tới trang tra cứu (<code>PUBLIC_LOOKUP_URL</code> + <code>?code=…</code>). Khách quét QR rồi nhập 4 số cuối điện thoại để xem và tải PDF.</small></section>}
        {release.status === 'SUBMITTED' && release.createdByUserId === user?.id && <section className="panel-card custody-card"><p className="section-kicker">Đang chờ duyệt</p><small className="muted">Hồ sơ do bạn lập nên cần một Admin khác duyệt và phát hành (người duyệt phải độc lập).</small></section>}
        {isTech && editable && <section className="form-submit-panel"><p><strong>Gửi người duyệt</strong><span>Cần tải lên file phiếu kết quả, hoặc lưu ít nhất một chỉ tiêu và tên người ký. Sau khi gửi bạn không thể sửa.</span></p><button className="primary-button" disabled={busy || (release.items.length === 0 && !reportPdf)} onClick={() => setConfirm({ title: 'Gửi hồ sơ để duyệt?', description: 'Sau khi gửi, bạn không thể thêm hoặc gỡ file cho tới khi được trả lại.', confirmLabel: 'Gửi duyệt', run: () => run(() => submitResultRelease(release.id)) })}>Gửi duyệt</button></section>}
        {isAdmin && release.status === 'SUBMITTED' && <section className="panel-card intake-form-card"><div className="form-card-heading"><span className="form-number"><Icon name="check" size={14} /></span><div><h2>Duyệt hồ sơ</h2><p>Người duyệt phải khác người lập hồ sơ và người phân tích. Phiếu KQKN (PDF) luôn công khai; file đính kèm chỉ công khai nếu bạn tích chọn.</p></div></div>
          <label>Lý do (bắt buộc khi trả lại)<textarea value={reason} onChange={(event) => setReason(event.target.value)} maxLength={500} /></label>
          <div className="transfer-decision-actions"><button className="outline-button reject-button" disabled={busy || !reason.trim()} onClick={() => setConfirm({ title: 'Trả lại hồ sơ?', description: 'Hồ sơ quay về kỹ thuật viên để bổ sung.', confirmLabel: 'Trả lại', tone: 'danger', run: () => run(() => returnResultRelease(release.id, reason.trim())) })}>Trả lại</button>
            <button className="primary-button" disabled={busy} onClick={() => setConfirm({ title: 'Phát hành kết quả?', description: 'Hệ thống sẽ cấp số phiếu, tạo PDF phiếu KQKN và mã tra cứu; khách tra cứu bằng mã (hoặc QR) + 4 số cuối điện thoại. Chỉ mẫu đã xác minh mã mới được phát hành.', confirmLabel: 'Phát hành', run: () => run(() => publishResultRelease(release.id, selected)) })}>Phát hành</button></div></section>}
        {isAdmin && release.status === 'PUBLISHED' && <section className="panel-card intake-form-card"><div className="form-card-heading"><span className="form-number">!</span><div><h2>Thu hồi</h2><p>Kết quả sẽ biến mất khỏi tra cứu công khai ngay lập tức.</p></div></div>
          <label>Lý do thu hồi<textarea value={reason} onChange={(event) => setReason(event.target.value)} maxLength={500} /></label>
          <button className="outline-button reject-button" disabled={busy || !reason.trim()} onClick={() => setConfirm({ title: 'Thu hồi kết quả đã phát hành?', description: 'Khách sẽ không còn tra cứu hoặc tải được kết quả này.', confirmLabel: 'Thu hồi', tone: 'danger', run: () => run(() => revokeResultRelease(release.id, reason.trim())) })}>Thu hồi</button></section>}
        {['TECHNICIAN', 'ADMIN'].includes(user?.role) && release.status === 'REVOKED' && <section className="form-submit-panel"><p><strong>Lập phiên bản mới</strong><span>Tạo bản nháp v{release.version + 1} để sửa và phát hành lại.</span></p><button className="primary-button" disabled={busy} onClick={() => run(async () => { const next = await reviseResultRelease(release.id); navigate(`/admin/results/${next.id}`); }, { reload: false })}>Lập phiên bản mới</button></section>}
      <section className="panel-card custody-card history-card"><p className="section-kicker">Nhật ký (không thể sửa)</p>
        {events.length === 0 ? <p className="muted">Chưa có sự kiện.</p> : <ol className="history-list">{events.map((event) => <li key={event.id}><strong>{eventLabels[event.eventType] ?? event.eventType}</strong><small>{event.actor} · {formatDateTime(event.createdAt)}</small>{event.note && <span>{event.note}</span>}</li>)}</ol>}
      </section>
    </aside></div>
    {confirm && <ConfirmDialog title={confirm.title} description={confirm.description} confirmLabel={confirm.confirmLabel} tone={confirm.tone} onCancel={() => setConfirm(null)} onConfirm={() => { const action = confirm.run; setConfirm(null); action(); }} />}
  </section>;
}

function FormHeading({ number, title, copy }) { return <div className="form-card-heading"><span className="form-number">{number}</span><div><h2>{title}</h2><p>{copy}</p></div></div>; }
function Data({ label, value }) { return <span><small>{label}</small><strong>{value || '—'}</strong></span>; }
function Timeline({ complete, title, detail }) { return <div className={`custody-event ${complete ? 'complete' : ''}`}><i /><span><strong>{title}</strong><small>{detail}</small></span></div>; }
function formatDateTime(value) { return value ? new Date(value).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' }) : '—'; }

function emptyRow() { return { parameter: '', unit: '', method: '', result: '', note: '' }; }
function cleanRow(row) { return { parameter: row.parameter.trim(), unit: row.unit.trim() || null, method: row.method.trim() || null, result: row.result.trim(), note: row.note.trim() || null }; }
function Info({ label, value, mono }) { return <span className="info-row"><small>{label}</small><strong className={mono ? 'mono' : ''}>{value || '—'}</strong></span>; }
function formatSize(value) { return value < 1024 * 1024 ? `${Math.ceil(value / 1024)} KB` : `${(value / 1024 / 1024).toFixed(1)} MB`; }
