import { useAuth } from '../../auth/hooks/useAuth';
import { useNavigate } from 'react-router-dom';

const stats = [
  { label: 'Nhân viên đang hoạt động', value: '03', detail: '+1 trong tháng này', tone: 'mint' },
  { label: 'Mẫu đang xử lý', value: '128', detail: 'Cập nhật 5 phút trước', tone: 'blue' },
  { label: 'Kết quả chờ duyệt', value: '12', detail: 'Cần được xử lý hôm nay', tone: 'amber' },
  { label: 'Thiết bị đang sử dụng', value: '08', detail: 'Trên tổng số 14 thiết bị', tone: 'violet' },
];

const activity = [
  ['08:42', 'Nguyễn Minh Anh', 'đã tiếp nhận mẫu mới', 'Mẫu CFC-26018'],
  ['08:17', 'Trần Quốc Bảo', 'đã hoàn thành phân tích', 'Mẫu CFC-26011'],
  ['07:55', 'Hệ thống', 'đã ghi nhận phiên đăng nhập', 'Quản trị viên'],
  ['Hôm qua', 'Lê Thu Hà', 'đã cập nhật trạng thái thiết bị', 'Máy ly tâm C-04'],
];

export function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  return <div className="dashboard-page">
    <header className="admin-heading"><div><p className="section-kicker">Thứ Tư, 12 tháng 11, 2026</p><h1>Chào buổi sáng, {user?.displayName?.split(' ').slice(-1)[0] ?? 'quản trị viên'}.</h1><p className="heading-copy">Đây là những gì đang diễn ra trong CFC Lab hôm nay.</p></div><button className="outline-button" onClick={() => navigate('/admin/users')}>Quản lý nhân viên <span>↗</span></button></header>
    <section className="stats-grid" aria-label="Tổng quan hệ thống">{stats.map((stat) => <article className={`stat-card ${stat.tone}`} key={stat.label}><div className="stat-label">{stat.label}<span className="stat-dot" /></div><strong>{stat.value}</strong><span className="stat-detail">{stat.detail}</span></article>)}</section>
    <section className="dashboard-grid"><article className="panel-card activity-panel"><div className="panel-heading"><div><p className="section-kicker">Theo dõi hoạt động</p><h2>Hoạt động gần đây</h2></div><button className="text-button">Xem tất cả <span>→</span></button></div><div className="activity-list">{activity.map(([time, name, action, target]) => <div className="activity-row" key={`${time}-${name}`}><time>{time}</time><span className="activity-avatar">{name === 'Hệ thống' ? '✦' : name.split(' ').map((word) => word[0]).slice(-2).join('')}</span><div><p><strong>{name}</strong> {action}</p><small>{target}</small></div></div>)}</div></article><article className="panel-card readiness-panel"><div className="panel-heading"><div><p className="section-kicker">Tình trạng phòng lab</p><h2>Mức sẵn sàng</h2></div><span className="ready-badge"><i /> Tốt</span></div><div className="readiness-ring"><div><strong>86</strong><span>%</span><small>Sẵn sàng</small></div></div><div className="readiness-legend"><span><i className="legend-green" /> Đang hoạt động <strong>8</strong></span><span><i className="legend-yellow" /> Bảo trì <strong>2</strong></span><span><i className="legend-grey" /> Chưa sử dụng <strong>4</strong></span></div></article></section>
    <section className="quick-actions"><p className="section-kicker">Thao tác nhanh</p><div className="quick-grid"><button onClick={() => navigate('/admin/users')}><span className="quick-icon">＋</span><span><strong>Thêm nhân viên</strong><small>Tạo tài khoản truy cập mới</small></span><b>→</b></button><button><span className="quick-icon blue-icon">⌁</span><span><strong>Kiểm tra thiết bị</strong><small>Xem toàn bộ thiết bị trong lab</small></span><b>→</b></button><button><span className="quick-icon amber-icon">▣</span><span><strong>Xem báo cáo</strong><small>Tổng hợp hoạt động tháng này</small></span><b>→</b></button></div></section>
  </div>;
}
