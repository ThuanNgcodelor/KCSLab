import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { getReceptionistDashboard } from '../api/intakeApi';
import { PageLoader } from '../../../shared/components/Loading';

const TIME_ZONE = 'Asia/Ho_Chi_Minh';
const statusLabels = {
  DRAFT: 'Bản nháp', RECEIVED: 'Đã tiếp nhận', ACCEPTED_WITH_CONDITION: 'Có điều kiện', REJECTED: 'Từ chối',
  TRANSFER_PENDING: 'Chờ KCS nhận', TRANSFERRED: 'Đã chuyển KCS', IN_TESTING: 'Đang thử nghiệm',
  AWAITING_REVIEW: 'Chờ kiểm tra', TEST_REVIEWED: 'Đã kiểm tra',
};
const presets = [['today', 'Hôm nay'], ['week', 'Tuần này'], ['month', 'Tháng này'], ['custom', 'Tùy chọn']];

export function ReceptionistDashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [preset, setPreset] = useState('today');
  const [custom, setCustom] = useState({ from: '', to: '' });
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [refreshKey, setRefreshKey] = useState(0);

  const range = preset === 'custom' ? custom : presetRange(preset);
  const rangeReady = preset !== 'custom' || Boolean(custom.from && custom.to);

  useEffect(() => {
    if (!rangeReady) { setLoading(false); return undefined; }
    let active = true;
    setLoading(true); setError('');
    getReceptionistDashboard({ from: range.from, to: range.to })
      .then((data) => { if (active) setDashboard(data); })
      .catch((exception) => { if (active) { setDashboard(null); setError(exception.message); } })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [preset, custom.from, custom.to, refreshKey, rangeReady]);

  const stats = [
    { label: 'Đã ghi nhận', value: dashboard?.totalReceipts, detail: `${dashboard?.totalSamples ?? 0} mẫu`, tone: 'mint' },
    { label: 'Chờ lập/gửi Phiếu 4', value: dashboard?.awaitingTransfer, detail: 'Cần bàn giao sang KCS', tone: 'amber' },
    { label: 'Có điều kiện', value: dashboard?.conditional, detail: 'Cần theo dõi hồ sơ', tone: 'blue' },
    { label: 'Bị từ chối', value: dashboard?.rejected, detail: 'Có lý do trên hồ sơ', tone: 'violet' },
  ];
  const recent = dashboard?.recentReceipts ?? [];
  const firstName = user?.displayName?.split(' ').slice(-1)[0] ?? 'bạn';

  return <section className="admin-page receptionist-dashboard">
    <header className="admin-heading">
      <div><p className="section-kicker">{formatLongDate(new Date())}</p><h1>Chào {firstName}.</h1><p className="heading-copy">Công việc tiếp nhận của riêng bạn, tính theo giờ Việt Nam.</p></div>
      <button className="primary-button" onClick={() => navigate('/admin/intake/new')}>＋ Tạo phiếu tiếp nhận</button>
    </header>

    <section className="panel-card intake-filter-card" aria-label="Khoảng thời gian">
      <div role="group" aria-label="Chọn khoảng thời gian" style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        {presets.map(([value, label]) => <button type="button" key={value} className={preset === value ? 'primary-button small-button' : 'outline-button small-button'} aria-pressed={preset === value} onClick={() => setPreset(value)}>{label}</button>)}
      </div>
      {preset === 'custom' && <>
        <label><small>Từ ngày</small><input type="date" max={custom.to || undefined} value={custom.from} onChange={(event) => setCustom((c) => ({ ...c, from: event.target.value }))} /></label>
        <label><small>Đến ngày</small><input type="date" min={custom.from || undefined} value={custom.to} onChange={(event) => setCustom((c) => ({ ...c, to: event.target.value }))} /></label>
      </>}
      <button type="button" className="outline-button small-button" onClick={() => setRefreshKey((v) => v + 1)}>↻ Làm mới</button>
    </section>

    {error && <p className="form-error" role="alert">{error}</p>}
    {!rangeReady && <p className="muted" role="status">Chọn cả ngày bắt đầu và ngày kết thúc để xem số liệu.</p>}

    <section className="stats-grid" aria-label="Chỉ số tiếp nhận">{stats.map((stat) => <article className={`stat-card ${stat.tone}`} key={stat.label}><div className="stat-label">{stat.label}<span className="stat-dot" /></div><strong>{loading ? '…' : stat.value ?? '—'}</strong><span className="stat-detail">{stat.detail}</span></article>)}</section>

    <section className="dashboard-grid intake-dashboard-grid">
      <article className="panel-card activity-panel">
        <div className="panel-heading"><div><p className="section-kicker">Hoạt động của bạn</p><h2>Phiếu tiếp nhận gần đây</h2></div><button className="text-button" onClick={() => navigate('/admin/intake/receipts')}>Mở danh sách phiếu <span>→</span></button></div>
        {loading ? <PageLoader label="Đang tải dữ liệu…" compact /> : recent.length
          ? <div className="activity-list">{recent.map((receipt) => <button className="activity-row activity-row-button" key={receipt.id} onClick={() => navigate(`/admin/intake/${receipt.id}`)}><time>{formatShortDate(receipt.receivedAt)}</time><span className="activity-avatar">{receipt.sampleCount}</span><div><p><strong>{receipt.receiptCode}</strong> · {receipt.customerName}</p><small>{receipt.sampleName} · {statusLabels[receipt.status] ?? receipt.status}{receipt.expectedDate ? ` · Hạn ${formatShortDate(receipt.expectedDate)}` : ''}</small></div></button>)}</div>
          : <div className="dashboard-empty"><span>01</span><h3>Chưa có phiếu trong khoảng này</h3><p>Tạo phiếu tiếp nhận mới hoặc chọn khoảng thời gian khác.</p></div>}
      </article>
      <article className="panel-card intake-health-panel">
        <div className="panel-heading"><div><p className="section-kicker">Thao tác nhanh</p><h2>Việc tiếp theo</h2></div></div>
        <div style={{ display: 'grid', gap: 10 }}>
          <button className="primary-button" onClick={() => navigate('/admin/intake/new')}>＋ Tạo phiếu tiếp nhận</button>
          <button className="outline-button" onClick={() => navigate('/admin/intake/receipts')}>Mở danh sách phiếu</button>
          <button className="outline-button" onClick={() => navigate('/admin/transfers/new')}>Lập Phiếu 4</button>
        </div>
      </article>
    </section>
  </section>;
}

function vnToday() {
  // en-CA gives YYYY-MM-DD in the requested time zone.
  return new Date().toLocaleDateString('en-CA', { timeZone: TIME_ZONE });
}
export function presetRange(preset) {
  const today = vnToday();
  const [y, m, d] = today.split('-').map(Number);
  if (preset === 'month') return { from: `${today.slice(0, 8)}01`, to: today };
  if (preset === 'week') {
    const utc = new Date(Date.UTC(y, m - 1, d));
    utc.setUTCDate(utc.getUTCDate() - ((utc.getUTCDay() + 6) % 7)); // Monday
    return { from: utc.toISOString().slice(0, 10), to: today };
  }
  return { from: today, to: today };
}
function formatShortDate(value) { return value ? new Date(value).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', timeZone: TIME_ZONE }) : '—'; }
function formatLongDate(value) { return value.toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric', timeZone: TIME_ZONE }); }
