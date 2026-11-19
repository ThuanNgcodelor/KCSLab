import { useEffect, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { getAdminOperationsDashboard } from '../../intake/api/intakeApi';
import { PageLoader } from '../../../shared/components/Loading';

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
    getAdminOperationsDashboard()
      .then((data) => { if (active) setDashboard(data); })
      .catch((exception) => { if (active) setError(exception.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [user?.role]);

  if (user?.role === 'RECEPTIONIST') return <Navigate to="/admin/intake" replace />;
  if (user?.role === 'TECHNICIAN') return <Navigate to="/admin/receiving" replace />;

  const stats = [
    { label: 'Phiếu tiếp nhận', value: dashboard?.receiptsInPeriod ?? '—', detail: 'Trong 7 ngày gần nhất', tone: 'mint' },
    { label: 'Tổng số mẫu', value: dashboard?.samplesInPeriod ?? '—', detail: 'Trong cùng kỳ', tone: 'blue' },
    { label: 'Chờ KCS nhận', value: dashboard?.transfersPending ?? '—', detail: 'Toàn hệ thống', tone: 'amber' },
    { label: 'Đã phát hành', value: dashboard?.resultPublished ?? '—', detail: 'Hồ sơ kết quả', tone: 'violet' },
  ];

  return <div className="dashboard-page">
    <header className="admin-heading"><div><p className="section-kicker">{formatLongDate(new Date())}</p><h1>Chào {user?.displayName?.split(' ').slice(-1)[0] ?? 'quản trị viên'}.</h1><p className="heading-copy">Số liệu vận hành được lấy trực tiếp từ hồ sơ tiếp nhận, không dùng dữ liệu minh họa.</p></div><button className="primary-button" onClick={() => navigate('/admin/intake/new')}>＋ Tạo hồ sơ tiếp nhận</button></header>
    {error && <p className="form-error" role="alert">{error}</p>}
    <section className="stats-grid" aria-label="Tổng quan tiếp nhận">{stats.map((stat) => <article className={`stat-card ${stat.tone}`} key={stat.label}><div className="stat-label">{stat.label}<span className="stat-dot" /></div><strong>{loading ? '…' : stat.value}</strong><span className="stat-detail">{stat.detail}</span></article>)}</section>
    <section className="dashboard-grid intake-dashboard-grid">
      <article className="panel-card activity-panel"><div className="panel-heading"><div><p className="section-kicker">Dòng công việc</p><h2>Tiếp nhận gần đây</h2></div><button className="text-button" onClick={() => navigate('/admin/intake/receipts')}>Xem danh sách <span>→</span></button></div>
        {loading ? <PageLoader label="Đang tải dữ liệu…" compact /> : dashboard?.receiptsInPeriod != null
          ? <div className="admin-flow-summary"><FlowRow label="Chuyển mẫu chờ KCS" value={dashboard.transfersPending} tone="warning" /><FlowRow label="Mẫu đã KCS nhận" value={dashboard.transfersAccepted} tone="success" /><FlowRow label="Đang thử nghiệm" value={(dashboard.testingDraft ?? 0) + (dashboard.testingSubmitted ?? 0)} tone="info" /><FlowRow label="Hồ sơ kết quả chờ phát hành" value={dashboard.resultDraft + dashboard.resultSubmitted} tone="violet" /></div>
          : <div className="dashboard-empty"><span>01</span><h3>Chưa có phiếu tiếp nhận</h3><p>Tạo hồ sơ đầu tiên để bắt đầu theo dõi.</p></div>}
      </article>
      <article className="panel-card intake-health-panel"><div className="panel-heading"><div><p className="section-kicker">Kết luận tiếp nhận</p><h2>Chất lượng đầu vào</h2></div><span className="ready-badge"><i /> Dữ liệu thật</span></div>
        <div className="intake-health-number"><strong>{(dashboard?.transfersAccepted ?? 0) + (dashboard?.resultPublished ?? 0)}</strong><span>mốc đã hoàn tất</span></div>
        <div className="intake-health-bars"><span><i style={{ '--bar-value': `${ratio(dashboard?.transfersAccepted, dashboard?.transfersAccepted + dashboard?.transfersPending + dashboard?.transfersRejected)}%` }} />KCS đã nhận <strong>{dashboard?.transfersAccepted ?? 0}</strong></span><span><i style={{ '--bar-value': `${ratio(dashboard?.resultPublished, dashboard?.resultPublished + dashboard?.resultDraft + dashboard?.resultSubmitted + dashboard?.resultRevoked)}%` }} />Đã phát hành <strong>{dashboard?.resultPublished ?? 0}</strong></span><span><i style={{ '--bar-value': `${ratio(dashboard?.transfersRejected, dashboard?.transfersAccepted + dashboard?.transfersPending + dashboard?.transfersRejected)}%` }} />Từ chối bàn giao <strong>{dashboard?.transfersRejected ?? 0}</strong></span></div>
      </article>
    </section>
  </div>;
}

function FlowRow({ label, value, tone }) {
  return <div className={`admin-flow-row ${tone}`}><span><i />{label}</span><strong>{value ?? '—'}</strong></div>;
}

function ratio(value = 0, total = 0) { return total ? Math.max(4, Math.round((value / total) * 100)) : 0; }
function formatShortDate(value) { return value ? new Date(value).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }) : '—'; }
function formatLongDate(value) { return value.toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' }); }
