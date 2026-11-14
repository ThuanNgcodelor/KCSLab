import { useEffect, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { getAdminDashboard } from '../../intake/api/intakeApi';

export function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (user?.role !== 'ADMIN') return;
    let active = true;
    setLoading(true);
    getAdminDashboard()
      .then((data) => { if (active) setDashboard(data); })
      .catch((exception) => { if (active) setError(exception.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [user?.role]);

  if (user?.role === 'RECEPTIONIST') return <Navigate to="/admin/intake" replace />;
  if (user?.role === 'TECHNICIAN') return <Navigate to="/admin/transfers" replace />;

  const stats = [
    { label: 'Phiếu tiếp nhận', value: dashboard?.totalReceipts ?? '—', detail: 'Trong khoảng 7 ngày', tone: 'mint' },
    { label: 'Tổng số mẫu', value: dashboard?.totalSamples ?? '—', detail: 'Gắn với các phiếu tiếp nhận', tone: 'blue' },
    { label: 'Chờ lập phiếu chuyển', value: dashboard?.awaitingTransfer ?? '—', detail: 'Cần bàn giao sang KCS', tone: 'amber' },
    { label: 'Từ chối tiếp nhận', value: dashboard?.rejected ?? '—', detail: 'Có lưu lý do trên hồ sơ', tone: 'violet' },
  ];

  return <div className="dashboard-page">
    <header className="admin-heading"><div><p className="section-kicker">{formatLongDate(new Date())}</p><h1>Chào {user?.displayName?.split(' ').slice(-1)[0] ?? 'quản trị viên'}.</h1><p className="heading-copy">Số liệu vận hành được lấy trực tiếp từ hồ sơ tiếp nhận, không dùng dữ liệu minh họa.</p></div><button className="primary-button" onClick={() => navigate('/admin/intake/new')}>＋ Tạo phiếu tiếp nhận</button></header>
    {error && <p className="form-error" role="alert">{error}</p>}
    <section className="stats-grid" aria-label="Tổng quan tiếp nhận">{stats.map((stat) => <article className={`stat-card ${stat.tone}`} key={stat.label}><div className="stat-label">{stat.label}<span className="stat-dot" /></div><strong>{loading ? '…' : stat.value}</strong><span className="stat-detail">{stat.detail}</span></article>)}</section>
    <section className="dashboard-grid intake-dashboard-grid">
      <article className="panel-card activity-panel"><div className="panel-heading"><div><p className="section-kicker">Dòng công việc</p><h2>Tiếp nhận gần đây</h2></div><button className="text-button" onClick={() => navigate('/admin/intake')}>Xem danh sách <span>→</span></button></div>
        {loading ? <p className="muted">Đang tải dữ liệu…</p> : dashboard?.recentReceipts?.length
          ? <div className="activity-list">{dashboard.recentReceipts.map((receipt) => <button className="activity-row activity-row-button" key={receipt.id} onClick={() => navigate(`/admin/intake/${receipt.id}`)}><time>{formatShortDate(receipt.receivedAt)}</time><span className="activity-avatar">{receipt.sampleCount}</span><div><p><strong>{receipt.receiptCode}</strong> · {receipt.customerName}</p><small>{receipt.sampleName} · {receipt.receivedByName}</small></div></button>)}</div>
          : <div className="dashboard-empty"><span>01</span><h3>Chưa có phiếu tiếp nhận</h3><p>Tạo hồ sơ đầu tiên để bắt đầu theo dõi.</p></div>}
      </article>
      <article className="panel-card intake-health-panel"><div className="panel-heading"><div><p className="section-kicker">Kết luận tiếp nhận</p><h2>Chất lượng đầu vào</h2></div><span className="ready-badge"><i /> Dữ liệu thật</span></div>
        <div className="intake-health-number"><strong>{dashboard?.accepted ?? 0}</strong><span>phiếu chấp nhận</span></div>
        <div className="intake-health-bars"><span><i style={{ '--bar-value': `${ratio(dashboard?.accepted, dashboard?.totalReceipts)}%` }} />Chấp nhận <strong>{dashboard?.accepted ?? 0}</strong></span><span><i style={{ '--bar-value': `${ratio(dashboard?.conditional, dashboard?.totalReceipts)}%` }} />Có điều kiện <strong>{dashboard?.conditional ?? 0}</strong></span><span><i style={{ '--bar-value': `${ratio(dashboard?.rejected, dashboard?.totalReceipts)}%` }} />Từ chối <strong>{dashboard?.rejected ?? 0}</strong></span></div>
      </article>
    </section>
  </div>;
}

function ratio(value = 0, total = 0) { return total ? Math.max(4, Math.round((value / total) * 100)) : 0; }
function formatShortDate(value) { return value ? new Date(value).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }) : '—'; }
function formatLongDate(value) { return value.toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' }); }
