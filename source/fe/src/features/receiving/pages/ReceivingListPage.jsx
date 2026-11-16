import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { listReceivingTransfers } from '../api/receivingApi';
import { useNotifications } from '../../notifications/hooks/useNotifications';

const labels = { PENDING: 'Chờ nhận', ACCEPTED: 'Đã nhận', REJECTED: 'Đã từ chối' };
export function ReceivingListPage() {
  const [params, setParams] = useSearchParams();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const { notifications, connectionState } = useNotifications();
  const status = params.get('status') || 'PENDING';
  const page = Math.max(0, Number(params.get('page')) || 0);
  const size = params.get('size') === '50' ? 50 : 20;
  const q = params.get('q') || '';
  const from = params.get('from') || '';
  const to = params.get('to') || '';
  function update(key, value) { const next = new URLSearchParams(params); value ? next.set(key, value) : next.delete(key); if (key !== 'page') next.delete('page'); setParams(next); }
  useEffect(() => {
    let active = true; setLoading(true); setError('');
    listReceivingTransfers({ status, page, size, q, from, to }).then(value => { if (active) setData(value); })
      .catch(e => { if (active) { setData(null); setError(e.message); } }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [status, page, size, q, from, to, notifications, connectionState]);
  return <section className="admin-page"><header className="admin-heading"><h1>Nhận mẫu KCS</h1></header>
    <nav className="receiving-filter" aria-label="Trạng thái phiếu">{Object.entries(labels).map(([key,label]) => <button key={key} className={status === key ? 'primary-button' : 'outline-button'} aria-pressed={status === key} onClick={() => update('status',key)}>{label}</button>)}</nav>
    <section className="panel-card receiving-filter"><label>Tìm mã phiếu hoặc mã mẫu<input value={q} onChange={e => update('q',e.target.value)} /></label><label>Từ ngày<input type="date" value={from} onChange={e => update('from',e.target.value)} /></label><label>Đến ngày<input type="date" value={to} onChange={e => update('to',e.target.value)} /></label><label>Số phiếu mỗi trang<select value={size} onChange={e => update('size',e.target.value)}><option value="20">20</option><option value="50">50</option></select></label></section>
    {error && <p role="alert" className="form-error">{error}</p>}{loading && <p role="status">Đang tải phiếu…</p>}
    {!loading && data && <section className="panel-card"><h2>{labels[status]}</h2>{data.content.length ? <div className="receiving-cards">{data.content.map(item => <article className="receiving-row" key={item.id}><strong>{item.transferCode}</strong><span>{item.receiptCode} · {item.items.length} mẫu</span><span>{item.items.map(sample => sample.sampleCode).join(', ')}</span><span>Người giao: {item.deliveredBy}</span><span>Đã gửi: {new Date(item.submittedAt).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' })}</span>{item.status === 'PENDING' && <span>Đã chờ: {Math.max(0, Math.floor((Date.now() - new Date(item.submittedAt).getTime()) / 60000))} phút</span>}<button className="outline-button" onClick={() => navigate(`/admin/receiving/transfers/${item.id}`)}>Xem phiếu</button></article>)}</div> : <p>Không có phiếu phù hợp.</p>}<nav aria-label="Phân trang"><button className="outline-button" disabled={data.first} onClick={() => update('page',String(page-1))}>Trước</button> Trang {page+1} / {Math.max(data.totalPages,1)} <button className="outline-button" disabled={data.last} onClick={() => update('page',String(page+1))}>Sau</button></nav></section>}
  </section>;
}
