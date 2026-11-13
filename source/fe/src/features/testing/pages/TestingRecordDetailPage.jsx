import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { getTestingRecord, reviewTestingRecord, submitTestingRecord } from '../api/testingApi';

const statusLabels = { DRAFT: 'Bản nháp', SUBMITTED: 'Chờ kiểm tra', REVIEWED: 'Đã kiểm tra', RETURNED: 'Cần bổ sung' };

export function TestingRecordDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [record, setRecord] = useState(null);
  const [reviewNote, setReviewNote] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => { getTestingRecord(id).then(setRecord).catch((exception) => setError(exception.message)); }, [id]);

  async function submit() {
    setSaving(true); setError('');
    try { setRecord(await submitTestingRecord(id)); }
    catch (exception) { setError(exception.message); }
    finally { setSaving(false); }
  }

  async function review(decision) {
    setSaving(true); setError('');
    try { setRecord(await reviewTestingRecord(id, { decision, reviewNote })); }
    catch (exception) { setError(exception.message); }
    finally { setSaving(false); }
  }

  if (!record) return <section className="staff-page">{error ? <p className="form-error">{error}</p> : <p className="muted">Đang tải biên bản…</p>}</section>;
  const editable = ['DRAFT', 'RETURNED'].includes(record.status) && (user?.role === 'ADMIN' || user?.id === record.analystId);
  const qcRows = record.measurements.filter((row) => row.type === 'QC');
  const rawRows = record.measurements.filter((row) => row.type === 'RAW');

  return <section className="staff-page testing-detail-page">
    <header className="admin-heading staff-heading"><div><button className="back-link" onClick={() => navigate('/admin/testing')}>← Danh sách biên bản</button><p className="section-kicker">{record.recordCode} / {record.sampleCode}</p><h1>{statusLabels[record.status]}</h1><p className="heading-copy">{record.sampleName} · {record.testMethod}</p></div><span className={`testing-status large ${record.status.toLowerCase()}`}>{statusLabels[record.status]}</span></header>
    {location.state?.message && <p className="success-note intake-toast">✓ {location.state.message}</p>}{error && <p className="form-error intake-toast">{error}</p>}
    <div className="testing-detail-grid"><main className="receipt-form-main">
      <section className="panel-card test-record-sheet"><div className="test-record-title"><div><small>TEST RECORD</small><h2>Biên bản thử nghiệm</h2></div><strong>{record.recordCode}</strong></div><div className="test-general-grid"><Data label="Phiếu yêu cầu" value={record.receiptCode} /><Data label="Mã số mẫu" value={record.sampleCode} /><Data label="Tên mẫu" value={record.sampleName} /><Data label="Số lượng" value={record.quantity} /><Data label="Ngày PTN nhận mẫu" value={formatDate(record.receivedDate)} /><Data label="Ngày yêu cầu trả KQ" value={formatDate(record.dueDate)} /><Data label="Bắt đầu thử nghiệm" value={formatDateTime(record.analysisStart)} /><Data label="Kết thúc thử nghiệm" value={formatDateTime(record.analysisEnd)} /><Data label="Người phân tích" value={record.analyst} /><Data label="Người kiểm tra" value={record.checkedBy || 'Chưa kiểm tra'} /></div></section>
      <RecordTable title="II. Thiết bị phân tích" columns={['Thiết bị', 'Mã hiệu / Serial', 'Nhà sản xuất', 'Mã nhận dạng', 'Hiệu chuẩn']} rows={record.equipment.map((item) => [item.name, item.serialNumber, item.manufacturer, item.identificationCode, formatDate(item.calibrationDate)])} empty="Không ghi nhận thiết bị." />
      <RecordTable title="III. Hóa chất và thuốc thử" columns={['Hóa chất', 'Mã', 'Nguồn gốc', 'Ngày pha / Hạn dùng', 'Bảo quản / Nồng độ']} rows={record.reagents.map((item) => [item.name, item.code, item.origin, `${formatDate(item.preparationDate)} / ${formatDate(item.expiryDate)}`, `${item.storageCondition || '—'} / ${item.concentration || '—'}`])} empty="Không ghi nhận hóa chất." />
      <section className="panel-card test-text-sections"><div><h3>IV. Đường chuẩn</h3><p>{record.calibrationCurve || 'Không áp dụng.'}</p></div><div><h3>V. Công thức tính</h3><p>{record.calculationFormula || 'Chưa ghi công thức.'}</p></div></section>
      <MeasurementTable title="VI. Đảm bảo chất lượng" rows={qcRows} empty="Không có dòng QC." />
      <section className="panel-card qc-summary"><Data label="Hiệu suất thu hồi" value={record.recoveryPercent == null ? '—' : `${record.recoveryPercent}%`} /><Data label="Biểu đồ / nhận xét QC" value={record.qcChartNote || 'Chưa ghi nhận.'} /></section>
      <MeasurementTable title="VII. Số liệu thực nghiệm" rows={rawRows} empty="Chưa có số liệu thực nghiệm." />
    </main><aside className="receipt-form-aside testing-review-aside">
      <section className="panel-card custody-card"><p className="section-kicker">Luồng kiểm tra</p><Timeline complete title="Đã lập biên bản" detail={`${record.analyst} · ${formatDateTime(record.createdAt)}`} /><Timeline complete={record.status !== 'DRAFT' && record.status !== 'RETURNED'} title="Gửi kiểm tra" detail={record.submittedAt ? formatDateTime(record.submittedAt) : 'Chưa gửi'} /><Timeline complete={record.status === 'REVIEWED'} title={record.status === 'RETURNED' ? 'Đã trả lại' : 'Kiểm tra hoàn tất'} detail={record.reviewedAt ? `${record.checkedBy} · ${formatDateTime(record.reviewedAt)}` : 'Đang chờ'} /></section>
      {record.reviewNote && <section className={`handoff-result ${record.status === 'RETURNED' ? 'rejected' : 'accepted'}`}><strong>Ý kiến kiểm tra</strong><span>{record.reviewNote}</span></section>}
      {editable && <section className="form-submit-panel testing-action-panel"><p><strong>Biên bản đang mở</strong><span>Cập nhật số liệu nếu cần, sau đó gửi người kiểm tra.</span></p><button className="outline-button light-outline" onClick={() => navigate(`/admin/testing/${id}/edit`)}>Sửa biên bản</button><button className="primary-button" disabled={saving} onClick={submit}>{saving ? 'Đang gửi…' : 'Gửi kiểm tra'}</button></section>}
      {record.status === 'SUBMITTED' && user?.role === 'ADMIN' && <section className="panel-card intake-form-card testing-review-panel"><div className="form-card-heading"><span className="form-number">✓</span><div><h2>Kiểm tra biên bản</h2><p>Duyệt hoặc trả lại kỹ thuật viên bổ sung.</p></div></div><label>Ý kiến kiểm tra<textarea value={reviewNote} onChange={(event) => setReviewNote(event.target.value)} placeholder="Bắt buộc khi trả lại" /></label><div className="transfer-decision-actions"><button className="outline-button reject-button" disabled={saving} onClick={() => review('RETURNED')}>Trả lại</button><button className="primary-button" disabled={saving} onClick={() => review('REVIEWED')}>{saving ? 'Đang lưu…' : 'Xác nhận đạt'}</button></div></section>}
    </aside></div>
  </section>;
}

function Data({ label, value }) { return <span><small>{label}</small><strong>{value || '—'}</strong></span>; }
function Timeline({ complete, title, detail }) { return <div className={`custody-event ${complete ? 'complete' : ''}`}><i /><span><strong>{title}</strong><small>{detail}</small></span></div>; }

function RecordTable({ title, columns, rows, empty }) { return <section className="panel-card record-table-card"><h3>{title}</h3>{rows.length === 0 ? <p className="muted">{empty}</p> : <div className="table-wrap"><table className="staff-table record-data-table"><thead><tr>{columns.map((column) => <th key={column}>{column}</th>)}</tr></thead><tbody>{rows.map((row, rowIndex) => <tr key={rowIndex}>{row.map((value, columnIndex) => <td key={columns[columnIndex]} data-label={columns[columnIndex]}>{value || '—'}</td>)}</tr>)}</tbody></table></div>}</section>; }

function MeasurementTable({ title, rows, empty }) { const columns = ['Lần lặp', 'Lượng mẫu', 'Thể tích', 'Pha loãng', 'Nồng độ', 'KQ / lần', 'Kết quả', 'Độ lệch']; return <RecordTable title={title} columns={columns} rows={rows.map((row) => [row.replicate, row.sampleAmount, row.finalVolume, row.dilutionFactor, row.measuredConcentration, row.resultPerReplicate, row.result, row.deviation])} empty={empty} />; }
function formatDate(value) { return value ? new Date(`${value}T00:00:00`).toLocaleDateString('vi-VN') : '—'; }
function formatDateTime(value) { return value ? new Date(value).toLocaleString('vi-VN') : '—'; }
