import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { listTransfers } from '../api/transferApi';

const statusLabels = { PENDING: 'Chờ phòng PT nhận', ACCEPTED: 'Đã nhận mẫu', REJECTED: 'Từ chối nhận' };

export function TransfersPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [transfers, setTransfers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const canCreate = ['ADMIN', 'RECEPTIONIST'].includes(user?.role);

  async function loadTransfers() {
    setLoading(true);
    setError('');
    try { setTransfers(await listTransfers()); }
    catch (exception) { setError(exception.message); }
    finally { setLoading(false); }
  }

  useEffect(() => { loadTransfers(); }, []);

  return <section className="intake-page transfer-page">
    <header className="admin-heading">
      <div><p className="section-kicker">Vận hành / Chuyển mẫu</p><h1>Chuyển mẫu KCS/PTN</h1><p className="heading-copy">Theo dõi bàn giao từ nơi tiếp nhận đến phòng phân tích.</p></div>
      {canCreate && <button className="primary-button" onClick={() => navigate('/admin/transfers/new')}>＋ Lập phiếu chuyển mẫu</button>}
    </header>
    {location.state?.message && <p className="success-note intake-toast" role="status">✓ {location.state.message}</p>}
    {error && <p className="form-error intake-toast" role="alert">{error}</p>}
    <div className="intake-guide">
      <div className="guide-step done"><span>✓</span><div><strong>Tiếp nhận</strong><small>Phiếu số 3</small></div></div><div className="guide-line done" />
      <div className="guide-step current"><span>02</span><div><strong>Chuyển mẫu</strong><small>Phiếu số 4</small></div></div><div className="guide-line" />
      <div className="guide-step"><span>03</span><div><strong>Thử nghiệm</strong><small>Biên bản số 5</small></div></div>
    </div>
    <section className="panel-card intake-list-card">
      <div className="panel-heading"><div><p className="section-kicker">Chuỗi bàn giao</p><h2>Phiếu chuyển mẫu</h2></div><button className="outline-button small-button" onClick={loadTransfers}>↻ Làm mới</button></div>
      {loading ? <p className="muted">Đang tải phiếu chuyển…</p> : transfers.length === 0 ? <div className="empty-intake"><span>⇄</span><h3>Chưa có phiếu chuyển mẫu</h3><p>Lập phiếu từ hồ sơ tiếp nhận để bàn giao mẫu lên phòng phân tích.</p>{canCreate && <button className="outline-button" onClick={() => navigate('/admin/transfers/new')}>Lập phiếu đầu tiên</button>}</div> : <div className="table-wrap"><table className="staff-table transfer-table"><thead><tr><th>Mã phiếu</th><th>Phiếu tiếp nhận</th><th>Mẫu</th><th>Hạn trả</th><th>Trạng thái</th><th /></tr></thead><tbody>{transfers.map((transfer) => <tr key={transfer.id}><td><strong className="receipt-code">{transfer.transferCode}</strong><small>{transfer.customerName}</small></td><td><strong>{transfer.receiptCode}</strong><small>{transfer.deliveredBy}</small></td><td>{transfer.items.length} mẫu</td><td>{formatDate(transfer.resultsDueDate)}</td><td><span className={`transfer-status ${transfer.status.toLowerCase()}`}><i />{statusLabels[transfer.status]}</span></td><td><button className="table-action" onClick={() => navigate(`/admin/transfers/${transfer.id}`)}>Xem phiếu</button></td></tr>)}</tbody></table></div>}
    </section>
  </section>;
}

function formatDate(value) { return value ? new Date(`${value}T00:00:00`).toLocaleDateString('vi-VN') : '—'; }
