import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { presetRange } from '../../intake/pages/ReceptionistDashboardPage';
import { getReceivingDashboard } from '../api/receivingApi';
import { useNotifications } from '../../notifications/hooks/useNotifications';
import { useAuth } from '../../auth/hooks/useAuth';
import { PageLoader } from '../../../shared/components/Loading';

export function ReceivingDashboardPage() {
  const [preset, setPreset] = useState('today');
  const [custom, setCustom] = useState({ from: '', to: '' });
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [refreshKey, setRefreshKey] = useState(0);
  const navigate = useNavigate();
  const { user } = useAuth();
  const { notifications, connectionState } = useNotifications();
  const range = preset === 'custom' ? custom : presetRange(preset);
  useEffect(() => {
    if (!range.from || !range.to) { setData(null); return undefined; }
    let active = true;
    setLoading(true); setError('');
    getReceivingDashboard(range).then(value => { if (active) setData(value); })
      .catch(e => { if (active) { setData(null); setError(e.message); } })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [range.from, range.to, notifications, connectionState, refreshKey]);
  const stats = data && [
    { label: 'Phiếu chuyển lên', value: data.submittedTransfers, detail: 'Phiếu gửi đến KCS trong kỳ', tone: 'mint' },
    { label: 'Số mẫu chuyển lên', value: data.submittedSamples, detail: 'Mẫu trong các phiếu đã gửi', tone: 'blue' },
    { label: 'Đang chờ nhận', value: data.pendingNow, detail: 'Tại thời điểm hiện tại', tone: 'amber' },
    { label: 'Đã nhận trong kỳ', value: data.acceptedTransfers, detail: 'Phiếu đã đối chiếu và nhận', tone: 'mint' },
    { label: 'Từ chối trong kỳ', value: data.rejectedTransfers, detail: 'Phiếu đã trả lại tiếp nhận', tone: 'violet' },
    { label: 'Thời gian xử lý TB', value: data.averageProcessingSeconds == null ? '—' : `${Math.round(data.averageProcessingSeconds / 60)} phút`, detail: 'Từ khi gửi đến khi quyết định', tone: 'blue' },
  ];
  const firstName = user?.displayName?.split(' ').slice(-1)[0] ?? 'bạn';
  return <section className="admin-page receiving-page receiving-dashboard">
    <header className="admin-heading"><div><p className="section-kicker">{new Date().toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric', timeZone: 'Asia/Ho_Chi_Minh' })}</p><h1>Chào {firstName}.</h1><p className="heading-copy">Theo dõi bàn giao và đối chiếu mẫu tại phòng KCS.</p></div><button className="primary-button" onClick={() => navigate('/admin/receiving/transfers')}>Xem danh sách nhận mẫu</button></header>
    <section className="panel-card intake-filter-card receiving-period" aria-label="Khoảng thời gian">
      <div className="receiving-period-presets" role="group" aria-label="Chọn khoảng thời gian">{[['today','Hôm nay'],['week','Tuần này'],['month','Tháng này'],['custom','Tùy chọn']].map(([id,label]) => <button type="button" key={id} className={preset === id ? 'primary-button small-button' : 'outline-button small-button'} aria-pressed={preset === id} onClick={() => setPreset(id)}>{label}</button>)}</div>
      {preset === 'custom' && <><label><small>Từ ngày</small><input type="date" max={custom.to || undefined} value={custom.from} onChange={e => setCustom(c => ({ ...c, from: e.target.value }))} /></label><label><small>Đến ngày</small><input type="date" min={custom.from || undefined} value={custom.to} onChange={e => setCustom(c => ({ ...c, to: e.target.value }))} /></label></>}
      <button type="button" className="outline-button small-button" onClick={() => setRefreshKey(key => key + 1)}>↻ Làm mới</button>
    </section>
    {error && <p className="form-error" role="alert">{error}</p>}
    {loading && <PageLoader label="Đang tải số liệu…" compact />}
    {!loading && !error && !data && <p>Chọn khoảng ngày để xem số liệu.</p>}
    {data && <>
      <section className="stats-grid" aria-label="Thống kê nhận mẫu">{stats.map(stat => <article className={`stat-card ${stat.tone}`} key={stat.label}><div className="stat-label">{stat.label}<span className="stat-dot" /></div><strong>{stat.value}</strong><span className="stat-detail">{stat.detail}</span></article>)}</section>
      <div className="dashboard-grid"><section className="panel-card"><div className="receiving-section-heading"><div><p className="section-kicker">Hàng đợi bàn giao</p><h2>Chờ nhận lâu nhất</h2><p>Các phiếu đang chờ đối chiếu, ưu tiên phiếu đến trước.</p></div><span className="receiving-count">{data.pendingNow} đang chờ</span></div>{data.pendingQueue.length ? data.pendingQueue.map(item => <button className="receiving-queue-item" key={item.id} onClick={() => navigate(`/admin/receiving/transfers/${item.id}`)}><span className="receiving-queue-icon" aria-hidden="true">↗</span><span className="receiving-queue-main"><strong>{item.transferCode}</strong><small>{item.items.length} mẫu · Gửi lúc {formatTime(item.submittedAt)}</small></span><span className="receiving-wait">Đã chờ {waitingTime(item.submittedAt)}</span><span aria-hidden="true">›</span></button>) : <div className="receiving-queue-empty"><span aria-hidden="true">✓</span><div><strong>Không có phiếu chờ nhận</strong><p>Các phiếu mới sẽ xuất hiện tại đây khi được bàn giao.</p></div></div>}</section>
      <section className="panel-card"><div className="receiving-section-heading"><div><p className="section-kicker">Trong khoảng đã chọn</p><h2>Nhịp nhận mẫu theo ngày</h2><p>Theo dõi phiếu chuyển lên và quyết định nhận.</p></div></div>{data.trend.length ? <div className="table-wrap"><table><thead><tr><th>Ngày</th><th>Chuyển lên</th><th>Đã nhận</th><th>Từ chối</th></tr></thead><tbody>{data.trend.map(day => <tr key={day.date}><td>{day.date}</td><td>{day.submitted}</td><td>{day.accepted}</td><td>{day.rejected}</td></tr>)}</tbody></table></div> : <p>Chưa có dữ liệu trong khoảng này.</p>}</section></div>
      <section className="panel-card receiving-history"><h2>Quyết định gần đây</h2>{data.recentDecisions.length ? data.recentDecisions.map(item => <button className="receiving-queue-item" key={item.id} onClick={() => navigate(`/admin/receiving/transfers/${item.id}`)}><span className="receiving-queue-main"><strong>{item.transferCode}</strong><small>{formatTime(item.receivedAt)}</small></span><span className="receiving-count">{item.status === 'ACCEPTED' ? 'Đã nhận' : 'Từ chối'}</span><span aria-hidden="true">›</span></button>) : <p className="receiving-empty">Chưa có quyết định gần đây.</p>}</section>
    </>}
  </section>;
}
function formatTime(value) { return value ? new Date(value).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' }) : '—'; }
function waitingTime(value) { return value ? `${Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 60000))} phút` : '—'; }
