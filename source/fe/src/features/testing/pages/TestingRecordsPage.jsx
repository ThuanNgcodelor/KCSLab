import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listTestingRecords } from '../api/testingApi';

const statusLabels = {
  DRAFT: 'Bản nháp',
  SUBMITTED: 'Chờ kiểm tra',
  REVIEWED: 'Đã kiểm tra',
  RETURNED: 'Cần bổ sung',
};

export function TestingRecordsPage() {
  const navigate = useNavigate();
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function loadRecords() {
    setLoading(true); setError('');
    try { setRecords(await listTestingRecords()); }
    catch (exception) { setError(exception.message); }
    finally { setLoading(false); }
  }

  useEffect(() => { loadRecords(); }, []);

  return <section className="staff-page testing-page">
    <header className="admin-heading staff-heading"><div><p className="section-kicker">Phòng phân tích / Biên bản số 5</p><h1>Biên bản thử nghiệm</h1><p className="heading-copy">Ghi thiết bị, hóa chất, QC và số liệu thô theo từng mẫu.</p></div><button className="primary-button add-staff-button" onClick={() => navigate('/admin/testing/new')}>＋ Lập biên bản</button></header>
    <div className="intake-guide testing-guide" aria-label="Tiến độ xử lý mẫu"><div className="guide-step done"><span>✓</span><div><strong>Tiếp nhận</strong><small>Phiếu số 3</small></div></div><i className="guide-line done" /><div className="guide-step done"><span>✓</span><div><strong>Chuyển mẫu</strong><small>Phiếu số 4</small></div></div><i className="guide-line done" /><div className="guide-step current"><span>03</span><div><strong>Thử nghiệm</strong><small>Biên bản số 5</small></div></div></div>
    {error && <p className="form-error intake-toast">{error}</p>}
    <section className="panel-card intake-list-card"><div className="panel-heading"><div><p className="section-kicker">Hồ sơ chuyên môn</p><h2>Biên bản gần đây</h2></div><button className="outline-button small-button" onClick={loadRecords}>↻ Làm mới</button></div>
      {loading ? <p className="muted">Đang tải biên bản…</p> : records.length === 0 ? <div className="empty-intake"><span>⌁</span><h3>Chưa có biên bản thử nghiệm</h3><p>Chọn một mẫu đã bàn giao để bắt đầu ghi nhận số liệu.</p><button className="primary-button" onClick={() => navigate('/admin/testing/new')}>Lập biên bản đầu tiên</button></div> : <div className="table-wrap"><table className="staff-table testing-table"><thead><tr><th>Biên bản / mẫu</th><th>Phương pháp</th><th>Người phân tích</th><th>Thời hạn</th><th>Trạng thái</th><th /></tr></thead><tbody>{records.map((record) => <tr key={record.id}><td><strong className="receipt-code">{record.recordCode}</strong><small>{record.sampleCode} · {record.sampleName}</small></td><td>{record.testMethod}<small>{record.requestedParameters}</small></td><td>{record.analyst}</td><td>{formatDate(record.dueDate)}</td><td><span className={`testing-status ${record.status.toLowerCase()}`}>{statusLabels[record.status]}</span></td><td><button className="table-action" onClick={() => navigate(`/admin/testing/${record.id}`)}>Xem biên bản</button></td></tr>)}</tbody></table></div>}
    </section>
  </section>;
}

function formatDate(value) { return value ? new Date(`${value}T00:00:00`).toLocaleDateString('vi-VN') : '—'; }
