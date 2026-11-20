import { splitParameters } from '../../../shared/parameters';
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
  downloadReportDocx, downloadResultFile, regenerateResultReport, getResultRelease, openReportPreview, saveResultReport, getResultReleaseEvents, publishResultRelease, removeResultFile, removeReportFile, uploadReportFile,
  returnResultRelease, reviseResultRelease, revokeResultRelease, submitResultRelease, uploadResultFiles,
} from '../api/resultsApi';

export const resultStatusLabels = labelsOf('release');
const eventLabels = { CREATED: 'Lập hồ sơ', REPORT_UPDATED: 'Cập nhật nội dung phiếu', REPORT_FILE_UPLOADED: 'Tải file phiếu kết quả', REPORT_FILE_REMOVED: 'Gỡ file phiếu kết quả', FILE_ADDED: 'Thêm file', FILE_REMOVED: 'Gỡ file', SUBMITTED: 'Gửi duyệt', RETURNED: 'Trả lại', PUBLISHED: 'Phát hành', REVOKED: 'Thu hồi', REVISION_CREATED: 'Lập phiên bản mới', REPORT_REGENERATED: 'Tạo lại PDF từ mẫu Word' };
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
  const [reportInputMode, setReportInputMode] = useState('file');
  const [signers, setSigners] = useState({ qualityHeadName: '', directorName: '' });
  const [confirm, setConfirm] = useState(null);

  const load = useCallback(async () => {
    setError('');
    try {
      const [detail, history] = await Promise.all([getResultRelease(id), getResultReleaseEvents(id)]);
      setRelease(detail); setEvents(history); setPublicIds([]);
      if (detail.files.some((file) => file.kind === 'REPORT_PDF')) setReportInputMode('file');
      else if (detail.items.length) setReportInputMode('table');
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

  // The creator edits the dossier. The current file-first flow lets Admin publish directly after checking the PDF.
  const isTech = ['TECHNICIAN', 'ADMIN'].includes(user?.role) && user.id === release.createdByUserId;
  const isAdmin = user?.role === 'ADMIN';
  const editable = release.status === 'DRAFT' || release.status === 'RETURNED';
  const selected = publicIds;
  const updateRow = (index, field, value) => setRows(rows.map((row, position) => position === index ? { ...row, [field]: value } : row));
  const toggle = (fileId) => setPublicIds(selected.includes(fileId) ? selected.filter((value) => value !== fileId) : [...selected, fileId]);

  const attachments = release.files.filter((file) => file.kind === 'UPLOAD');
  const reportPdf = release.files.find((file) => file.kind === 'REPORT_PDF');
  const reportSource = release.files.find((file) => file.kind === 'REPORT_SOURCE');
  const report = release.files.find((file) => file.kind === 'GENERATED_REPORT');
  const canEdit = isTech && editable;
  const directFileReady = Boolean(reportPdf) && ['DRAFT', 'SUBMITTED'].includes(release.status);
  const showTable = !reportPdf && (!canEdit || reportInputMode === 'table');

  function saveTableReport() {
    const completeRows = rows.filter((row) => row.parameter.trim() || row.result.trim());
    if (!completeRows.length || completeRows.some((row) => !row.parameter.trim() || !row.result.trim())) {
      setError('Mỗi dòng nhập tay cần có cả chỉ tiêu và kết quả. Nếu bạn đã có phiếu KQKN, hãy chọn “Tải file phiếu” — không cần nhập bảng.');
      return;
    }
    run(() => saveResultReport(release.id, { rows: completeRows.map(cleanRow), ...signers }));
  }

  return <section className="staff-page testing-detail-page result-detail-page">
    <header className="admin-heading staff-heading"><div><button className="back-link" onClick={() => navigate('/admin/results')}>← Danh sách hồ sơ kết quả</button><p className="section-kicker">{release.testingRecordCode} / {release.sampleCode}</p><h1>Hồ sơ kết quả v{release.version}</h1><p className="heading-copy">Lập bởi {release.createdByName}{release.documentNumber || release.resultNumber ? ` · Số phiếu ${release.documentNumber || release.resultNumber}` : ''}</p></div><StatusBadge domain="release" value={release.status} size="large" /></header>
    {error && <p className="form-error intake-toast" role="alert">{error}</p>}
    {release.decisionNote && release.status === 'RETURNED' && <p className="handoff-result rejected"><strong>Lý do trả lại ({release.decidedByName})</strong><span>{release.decisionNote}</span></p>}
    {release.revokeReason && release.status === 'REVOKED' && <p className="handoff-result rejected"><strong>Lý do thu hồi ({release.revokedByName})</strong><span>{release.revokeReason}</span></p>}
    <div className="testing-detail-grid"><main className="receipt-form-main">
      <section className="panel-card intake-form-card report-editor"><FormHeading number="I" title="Phiếu kết quả (KQKN-DV)" copy="Tải lên file DOCX hoặc PDF đã soạn, hoặc nhập bảng kết quả. DOCX được chuyển sang PDF; hãy xem trước và đối chiếu kỹ bố cục, nội dung trước khi gửi duyệt. Nếu không tìm thấy ô QR, mã sẽ được đặt ở trang riêng." />
        {canEdit && !reportPdf && <div className="report-mode-switch" role="group" aria-label="Cách tạo phiếu kết quả">
          <button type="button" className={reportInputMode === 'file' ? 'primary-button' : 'outline-button'} aria-pressed={reportInputMode === 'file'} onClick={() => { setReportInputMode('file'); setError(''); }}>Tải file phiếu</button>
          <button type="button" className={reportInputMode === 'table' ? 'primary-button' : 'outline-button'} aria-pressed={reportInputMode === 'table'} onClick={() => { setReportInputMode('table'); setError(''); }}>Nhập bảng thủ công</button>
        </div>}
        {reportPdf ? <div className="report-file-card"><span className="report-file-icon"><Icon name="file" size={22} /></span>
          <div><strong>{reportSource ? reportSource.originalName : reportPdf.originalName}</strong><small>{release.documentNumber ? `Số phiếu đọc từ file: ${release.documentNumber}` : 'Không đọc được “Số/ No.” trong file — kiểm tra lại file'} · PDF {formatSize(reportPdf.size)}</small></div>
          <div className="table-actions"><button type="button" className="table-action" disabled={busy} onClick={() => run(() => openReportPreview(release.id), { reload: false })}>Xem trước PDF</button>
            {canEdit && <button type="button" className="table-action danger" disabled={busy} onClick={() => setConfirm({ title: 'Gỡ file phiếu kết quả?', description: 'Bạn có thể tải lên file khác hoặc nhập bảng kết quả.', confirmLabel: 'Gỡ file', tone: 'danger', run: () => run(() => removeReportFile(release.id)) })}>Gỡ</button>}</div></div> : null}
        {canEdit && (!reportPdf ? reportInputMode === 'file' : true) && <form className="result-upload" onSubmit={(event) => { event.preventDefault(); run(async () => { await uploadReportFile(release.id, reportChoice); setReportChoice(null); }); }}>
          <PhotoPicker label={reportPdf ? 'Thay bằng file khác' : 'Tải file phiếu kết quả'} hint="DOCX hoặc PDF, tối đa 10 MB. Không có macro hoặc liên kết ngoài." camera={false} multiple={false} accept=".docx,.pdf,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document" files={reportChoice ? [reportChoice] : []} onChange={(picked) => setReportChoice(picked[0] ?? null)} />
          <div className="form-actions"><button className="primary-button" type="submit" disabled={busy || !reportChoice}>{busy ? 'Đang xử lý (chuyển PDF)…' : 'Tải phiếu lên'}</button></div></form>}
        {reportPdf ? <p className="field-help report-mode-help">Đang dùng file KQKN đã tải lên. Chỉ cần kiểm tra bản xem trước rồi gửi duyệt; không phải nhập bảng kết quả.</p>
          : reportInputMode === 'file' ? <p className="field-help report-mode-help">Chọn file DOCX/PDF và bấm “Tải phiếu lên”. Khi file đã tải thành công, bạn có thể gửi duyệt mà không nhập bảng.</p>
            : <p className="field-help report-mode-help">Nhập bảng chỉ khi không có file KQKN. Mỗi dòng cần có chỉ tiêu và kết quả.</p>}
      </section>
      {showTable && <section className="panel-card intake-form-card report-editor"><FormHeading number="II" title="Nhập bảng kết quả" copy="Dùng khi không có file phiếu. Số phiếu, ngày và mã QR được cấp khi phát hành." />
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
          {release.items.length > 0 && <button type="button" className="outline-button" disabled={busy} onClick={() => run(() => downloadReportDocx(release.id), { reload: false })}>Tải file Word (DOCX)</button>}
          {report && <button type="button" className="outline-button" disabled={busy} onClick={() => run(() => downloadResultFile(release.id, report), { reload: false })}>Tải phiếu PDF đã phát hành</button>}
          {canEdit && <button type="button" className="primary-button" disabled={busy} onClick={saveTableReport}>{busy ? 'Đang lưu…' : 'Lưu bảng kết quả'}</button>}
        </div>
      </section>}
      <section className="panel-card intake-form-card sample-photo-card"><FormHeading number="II" title={`Ảnh mẫu từ phiếu tiếp nhận (${release.samplePhotos?.length ?? 0})`} copy="Ảnh chụp khi tiếp nhận mẫu (Số/No. này). Các ảnh này được in ở trang “Hình ảnh mẫu” của phiếu KQKN (Word, PDF và bản khách tra cứu)." />
        {release.samplePhotos?.length ? <div className="sample-photo-grid">{release.samplePhotos.map((photo, index) => <figure key={photo.id}>
          <a href={`/api/v1/result-releases/${release.id}/sample-photos/${photo.id}`} target="_blank" rel="noreferrer"><img src={`/api/v1/result-releases/${release.id}/sample-photos/${photo.id}`} alt={photo.caption} loading="lazy" /></a>
          <figcaption><strong>Hình {index + 1}</strong>{photo.caption}</figcaption></figure>)}</div>
          : <p className="muted">Phiếu tiếp nhận của mẫu này chưa có ảnh. Thêm ảnh ở phiếu tiếp nhận (Phiếu 3) để in kèm phiếu kết quả.</p>}
      </section>
      {release.reference && <section className="panel-card intake-form-card reference-card"><FormHeading number="III" title="Số liệu từ biên bản thử nghiệm" copy={<>Tham chiếu từ biên bản <Link className="inline-link" to={`/admin/testing/${release.reference.testingRecordId}`}>{release.reference.recordCode}</Link>. Đây là hồ sơ làm việc nội bộ; kết quả cuối cùng nhập vào bảng 01 phía trên.</>} />
        <div className="test-general-grid"><Data label="Chỉ tiêu yêu cầu" value={splitParameters(release.reference.requestedParameters).join('\n')} /><Data label="Phương pháp" value={release.reference.testMethod} /><Data label="Bắt đầu thử nghiệm" value={formatDateTime(release.reference.analysisStart)} /><Data label="Hiệu suất thu hồi" value={release.reference.recoveryPercent == null ? null : `${release.reference.recoveryPercent}%`} /></div>
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
        <Timeline complete={directFileReady || Boolean(release.submittedAt) || release.status === 'RETURNED'} title={directFileReady ? 'File đã kiểm tra' : release.status === 'RETURNED' ? 'Đã bị trả lại' : 'Gửi duyệt'} detail={directFileReady ? 'Đã có file KQKN để Admin kiểm tra' : release.status === 'RETURNED' ? `${release.decidedByName ?? ''} · ${formatDateTime(release.decidedAt)}` : release.submittedAt ? `${release.submittedByName ?? ''} · ${formatDateTime(release.submittedAt)}` : 'Chưa gửi'} />
        <Timeline complete={Boolean(release.publishedAt)} title="Phát hành" detail={release.publishedAt ? `${release.decidedByName ?? ''} · ${formatDateTime(release.publishedAt)}` : 'Admin kiểm tra và phát hành'} />
        {release.status === 'REVOKED' && <Timeline complete title="Đã thu hồi" detail={`${release.revokedByName ?? ''} · ${formatDateTime(release.revokedAt)}`} />}
      </section>
        {release.status === 'PUBLISHED' && <section className="panel-card custody-card"><p className="section-kicker">Tra cứu công khai</p><Info label="Mã tra cứu" value={release.lookupCode} mono /><Info label="Số phiếu" value={release.resultNumber} /><small className="muted">Mã QR trên phiếu PDF trỏ tới trang tra cứu (<code>PUBLIC_LOOKUP_URL</code> + <code>?code=…</code>). Khách quét QR rồi nhập 4 số cuối điện thoại để xem và tải PDF.</small></section>}
        {isTech && editable && !reportPdf && <section className="form-submit-panel"><p><strong>Gửi Admin duyệt</strong><span>Lưu bảng kết quả (hoặc tải file KQKN) rồi gửi. Admin kiểm tra và phát hành; nếu sai, hồ sơ được trả lại kèm lý do.</span></p><button className="primary-button" disabled={busy || release.items.length === 0} onClick={() => setConfirm({ title: 'Gửi hồ sơ?', description: 'Sau khi gửi, hồ sơ sẽ chuyển sang trạng thái chờ Admin kiểm tra.', confirmLabel: 'Gửi duyệt', run: () => run(() => submitResultRelease(release.id)) })}>Gửi Admin duyệt</button></section>}
        {isTech && editable && reportPdf && !isAdmin && <section className="form-submit-panel"><p><strong>Đã tải file kết quả</strong><span>Không cần gửi duyệt. Admin sẽ kiểm tra file và phát hành khi hồ sơ đủ điều kiện.</span></p></section>}
        {isAdmin && directFileReady && <section className="panel-card intake-form-card"><div className="form-card-heading"><span className="form-number"><Icon name="check" size={14} /></span><div><h2>Kiểm tra và phát hành</h2><p>File KQKN đã có sẵn. Kiểm tra bản PDF, sau đó Admin phát hành trực tiếp; không cần người duyệt thứ hai.</p></div></div>
          <button className="primary-button" disabled={busy} onClick={() => setConfirm({ title: 'Phát hành kết quả?', description: 'Hệ thống sẽ cấp số phiếu, đóng dấu QR và mở tra cứu công khai sau khi phát hành.', confirmLabel: 'Phát hành', run: () => run(() => publishResultRelease(release.id, selected)) })}>Phát hành kết quả</button></section>}
        {isAdmin && release.status === 'SUBMITTED' && <section className="panel-card intake-form-card"><div className="form-card-heading"><span className="form-number"><Icon name="check" size={14} /></span><div><h2>Duyệt và phát hành</h2><p>Kiểm tra bảng kết quả, bản xem trước PDF hoặc file Word. Phát hành nếu đúng, hoặc trả lại kèm lý do để kỹ thuật viên sửa.</p></div></div>
          <label>Lý do (bắt buộc khi trả lại)<textarea value={reason} onChange={(event) => setReason(event.target.value)} maxLength={500} /></label>
          <div className="transfer-decision-actions"><button className="outline-button reject-button" disabled={busy || !reason.trim()} onClick={() => setConfirm({ title: 'Trả lại hồ sơ?', description: 'Hồ sơ quay về kỹ thuật viên để bổ sung.', confirmLabel: 'Trả lại', tone: 'danger', run: () => run(() => returnResultRelease(release.id, reason.trim())) })}>Trả lại</button>
            <button className="primary-button" disabled={busy} onClick={() => setConfirm({ title: 'Phát hành kết quả?', description: 'Hệ thống sẽ cấp số phiếu, tạo PDF phiếu KQKN và mã tra cứu; khách tra cứu bằng mã (hoặc QR) + 4 số cuối điện thoại. Chỉ mẫu đã xác minh mã mới được phát hành.', confirmLabel: 'Phát hành', run: () => run(() => publishResultRelease(release.id, selected)) })}>Phát hành</button></div></section>}
        {isAdmin && release.status === 'PUBLISHED' && !reportPdf && <section className="panel-card intake-form-card"><div className="form-card-heading"><span className="form-number"><Icon name="file" size={14} /></span><div><h2>Tạo lại phiếu PDF</h2><p>Tạo lại PDF từ mẫu Word hiện tại, giữ nguyên số phiếu, ngày phát hành, mã tra cứu và QR. Khách tải về sẽ nhận bản mới.</p></div></div>
          <button className="outline-button" disabled={busy} onClick={() => setConfirm({ title: 'Tạo lại phiếu PDF?', description: 'PDF đã phát hành được thay bằng bản tạo lại từ mẫu Word. Bản cũ vẫn được lưu trong nhật ký hồ sơ.', confirmLabel: 'Tạo lại PDF', run: () => run(() => regenerateResultReport(release.id)) })}>Tạo lại PDF từ mẫu Word</button></section>}
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
function Data({ label, value }) { return <span><small>{label}</small><strong style={{ whiteSpace: 'pre-line' }}>{value || '—'}</strong></span>; }
function Timeline({ complete, title, detail }) { return <div className={`custody-event ${complete ? 'complete' : ''}`}><i /><span><strong>{title}</strong><small>{detail}</small></span></div>; }
function formatDateTime(value) { return value ? new Date(value).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' }) : '—'; }

function emptyRow() { return { parameter: '', unit: '', method: '', result: '', note: '' }; }
function cleanRow(row) { return { parameter: row.parameter.trim(), unit: row.unit.trim() || null, method: row.method.trim() || null, result: row.result.trim(), note: row.note.trim() || null }; }
function Info({ label, value, mono }) { return <span className="info-row"><small>{label}</small><strong className={mono ? 'mono' : ''}>{value || '—'}</strong></span>; }
function formatSize(value) { return value < 1024 * 1024 ? `${Math.ceil(value / 1024)} KB` : `${(value / 1024 / 1024).toFixed(1)} MB`; }
