import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { presetRange } from '../../intake/pages/ReceptionistDashboardPage';
import { getReceivingDashboard } from '../api/receivingApi';
import { useNotifications } from '../../notifications/hooks/useNotifications';

export function ReceivingDashboardPage() {
  const [preset, setPreset] = useState('today');
  const [custom, setCustom] = useState({ from: '', to: '' });
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();
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
  }, [range.from, range.to, notifications, connectionState]);
  return <section className="admin-page">
    <header className="admin-heading"><div><h1>Tổng quan nhận mẫu</h1><p>Phiếu chuyển đến KCS và tình trạng bàn giao.</p></div><button className="primary-button" onClick={() => navigate('/admin/receiving/transfers')}>Xem danh sách nhận mẫu</button></header>
    <section className="panel-card receiving-filter" aria-label="Khoảng thời gian">
      {[['today','Hôm nay'],['week','Tuần này'],['month','Tháng này'],['custom','Tùy chọn']].map(([id,label]) => <button key={id} className={preset === id ? 'primary-button' : 'outline-button'} aria-pressed={preset === id} onClick={() => setPreset(id)}>{label}</button>)}
      {preset === 'custom' && <><label>Từ ngày<input type="date" value={custom.from} onChange={e => setCustom(c => ({ ...c, from: e.target.value }))} /></label><label>Đến ngày<input type="date" value={custom.to} onChange={e => setCustom(c => ({ ...c, to: e.target.value }))} /></label></>}
    </section>
    {error && <p className="form-error" role="alert">{error}</p>}
    {loading && <p role="status">Đang tải số liệu…</p>}
    {!loading && !error && !data && <p>Chọn khoảng ngày để xem số liệu.</p>}
    {data && <>
      <section className="stats-grid" aria-label="Thống kê nhận mẫu">{[['Phiếu chuyển lên', data.submittedTransfers], ['Số mẫu chuyển lên', data.submittedSamples], ['Đang chờ nhận', data.pendingNow], ['Đã nhận trong kỳ', data.acceptedTransfers], ['Từ chối trong kỳ', data.rejectedTransfers], ['Thời gian xử lý TB', data.averageProcessingSeconds == null ? '—' : `${Math.round(data.averageProcessingSeconds / 60)} phút`]].map(([label,value]) => <article className="stat-card" key={label}><div className="stat-label">{label}</div><strong>{value}</strong></article>)}</section>
      <div className="dashboard-grid"><section className="panel-card"><h2>Chờ nhận lâu nhất</h2>{data.pendingQueue.length ? data.pendingQueue.map(item => <button className="activity-row activity-row-button" key={item.id} onClick={() => navigate(`/admin/receiving/transfers/${item.id}`)}><strong>{item.transferCode}</strong><span>{item.items.length} mẫu · {formatTime(item.submittedAt)}</span></button>) : <p>Không có phiếu chờ nhận.</p>}</section>
      <section className="panel-card"><h2>Nhịp nhận mẫu theo ngày</h2>{data.trend.length ? <div className="table-wrap"><table><thead><tr><th>Ngày</th><th>Chuyển lên</th><th>Đã nhận</th><th>Từ chối</th></tr></thead><tbody>{data.trend.map(day => <tr key={day.date}><td>{day.date}</td><td>{day.submitted}</td><td>{day.accepted}</td><td>{day.rejected}</td></tr>)}</tbody></table></div> : <p>Chưa có dữ liệu trong khoảng này.</p>}</section></div>
      <section className="panel-card"><h2>Quyết định gần đây</h2>{data.recentDecisions.length ? data.recentDecisions.map(item => <button className="activity-row activity-row-button" key={item.id} onClick={() => navigate(`/admin/receiving/transfers/${item.id}`)}>{item.transferCode} · {item.status === 'ACCEPTED' ? 'Đã nhận' : 'Từ chối'} · {formatTime(item.receivedAt)}</button>) : <p>Chưa có quyết định gần đây.</p>}</section>
    </>}
  </section>;
}
function formatTime(value) { return value ? new Date(value).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' }) : '—'; }
