import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { listReceipts } from '../api/intakeApi';

const statusLabels = { RECEIVED: 'Đã tiếp nhận', ACCEPTED_WITH_CONDITION: 'Có điều kiện', REJECTED: 'Từ chối', DRAFT: 'Bản nháp' };

export function IntakePage() {
  const navigate = useNavigate(); const location = useLocation();
  const [receipts, setReceipts] = useState([]); const [loading, setLoading] = useState(true); const [error, setError] = useState('');
  useEffect(() => { listReceipts().then(setReceipts).catch((exception) => setError(exception.message)).finally(() => setLoading(false)); }, []);
  return <section className="intake-page">
    <header className="admin-heading"><div><p className="section-kicker">Vận hành / Tiếp nhận mẫu</p><h1>Tiếp nhận mẫu</h1><p className="heading-copy">Ghi nhận mẫu giấy, tạo mã truy xuất và bàn giao đúng người.</p></div><button className="primary-button" onClick={() => navigate('/admin/intake/new')}>＋ Tạo phiếu tiếp nhận</button></header>
    {location.state?.message && <p className="success-note intake-toast" role="status">✓ {location.state.message}</p>}{error && <p className="form-error intake-toast" role="alert">{error}</p>}
    <div className="intake-guide"><div className="guide-step current"><span>01</span><div><strong>Tiếp nhận</strong><small>Phiếu số 3</small></div></div><div className="guide-line" /><div className="guide-step"><span>02</span><div><strong>Chuyển mẫu</strong><small>Phiếu số 4</small></div></div><div className="guide-line" /><div className="guide-step"><span>03</span><div><strong>Thử nghiệm</strong><small>Biên bản số 5</small></div></div></div>
    <section className="panel-card intake-list-card"><div className="panel-heading"><div><p className="section-kicker">Hồ sơ mới nhất</p><h2>Phiếu tiếp nhận</h2></div><button className="outline-button small-button" onClick={() => { setLoading(true); listReceipts().then(setReceipts).catch((exception) => setError(exception.message)).finally(() => setLoading(false)); }}>↻ Làm mới</button></div>{loading ? <p className="muted">Đang tải phiếu…</p> : receipts.length === 0 ? <div className="empty-intake"><span>＋</span><h3>Chưa có phiếu tiếp nhận</h3><p>Tạo phiếu đầu tiên để bắt đầu theo dõi mẫu.</p><button className="outline-button" onClick={() => navigate('/admin/intake/new')}>Tạo phiếu mới</button></div> : <div className="table-wrap"><table className="staff-table intake-table"><thead><tr><th>Mã phiếu / khách hàng</th><th>Mẫu</th><th>Người giao</th><th>Trạng thái</th><th>Ngày nhận</th></tr></thead><tbody>{receipts.map((receipt) => <tr key={receipt.id}><td><strong className="receipt-code">{receipt.receiptCode}</strong><small>{receipt.customerName} · {receipt.customerPhone}</small></td><td><strong>{receipt.sampleName}</strong><small>{receipt.samples?.map((sample) => sample.sampleCode).join(', ')}</small></td><td>{receipt.deliveredBy}</td><td><span className={`status-pill ${receipt.status === 'REJECTED' ? 'disabled' : 'active'}`}><i />{statusLabels[receipt.status] ?? receipt.status}</span></td><td className="muted-cell">{new Date(receipt.receivedAt).toLocaleDateString('vi-VN')}</td></tr>)}</tbody></table></div>}</section>
  </section>;
}
