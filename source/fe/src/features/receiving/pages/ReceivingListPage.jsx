import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { listReceivingTransfers } from '../api/receivingApi';
import { useNotifications } from '../../notifications/hooks/useNotifications';
import { StatusBadge } from '../../../shared/components/StatusBadge';
import { labelsOf } from '../../../shared/status';
import { PageLoader } from '../../../shared/components/Loading';
import { DateInput } from '../../../shared/components/DateInput';

const labels = labelsOf('transfer');
const receivingStatuses = ['PENDING', 'ACCEPTED', 'REJECTED'];
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
  const sort = params.get('sort') || '';
  function update(key, value) { const next = new URLSearchParams(params); value ? next.set(key, value) : next.delete(key); if (key !== 'page') next.delete('page'); setParams(next); }
  useEffect(() => {
    let active = true; setLoading(true); setError('');
    listReceivingTransfers({ status, page, size, q, from, to, sort }).then(value => { if (active) setData(value); })
      .catch(e => { if (active) { setData(null); setError(e.message); } }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [status, page, size, q, from, to, sort, notifications, connectionState]);
  return <section className="admin-page receiving-page receiving-list"><header className="admin-heading"><div><p className="section-kicker">Nhận mẫu KCS / Danh sách</p><h1>Nhận mẫu KCS</h1><p className="heading-copy">Theo dõi phiếu bàn giao từ bộ phận tiếp nhận và xác nhận nhận mẫu.</p></div></header>
    <nav className="receiving-filter receiving-status-tabs" aria-label="Trạng thái phiếu">{receivingStatuses.map((key) => <button key={key} className={status === key ? 'primary-button' : 'outline-button'} aria-pressed={status === key} onClick={() => update('status',key)}>{labels[key]}</button>)}</nav>
    <section className="panel-card receiving-filter"><label>Tìm Số/No. hoặc mã hóa mẫu<input type="search" value={q} onChange={e => update('q',e.target.value)} placeholder="Ví dụ: CFC26.0001" /></label><label>Từ ngày<DateInput value={from} onChange={e => update('from',e.target.value)} /></label><label>Đến ngày<DateInput value={to} onChange={e => update('to',e.target.value)} /></label><label>Sắp xếp<select value={sort} onChange={e => update('sort', e.target.value)}><option value="">Mặc định</option><option value="submittedAt,desc">Gửi: mới nhất</option><option value="submittedAt,asc">Gửi: cũ nhất</option><option value="receivedAt,desc">Nhận: mới nhất</option><option value="resultsDueDate,asc">Hạn trả: gần nhất</option><option value="transferCode,asc">Mã phiếu: A → Z</option></select></label><label>Số phiếu mỗi trang<select value={size} onChange={e => update('size',e.target.value)}><option value="20">20</option><option value="50">50</option></select></label></section>
    {error && <p role="alert" className="form-error">{error}</p>}{loading && <PageLoader label="Đang tải phiếu…" rows={4} />}
    {!loading && data && <section className="panel-card"><div className="receiving-section-heading"><div><h2>{labels[status]}</h2><p>Danh sách phiếu chuyển mẫu đến phòng KCS</p></div><span className="receiving-count">{data.totalElements ?? data.content.length} phiếu</span></div>{data.content.length ? <div className="receiving-cards">{data.content.map(item => <article className="receiving-row" key={item.id}><div className="receiving-row-main"><strong>Số/No {item.requestNumber || '—'} · {item.items.length} mẫu</strong><span className="receiving-sample-codes">Mã hóa mẫu: {item.items.map(sample => sample.sampleCode).join(', ')}</span></div><div className="receiving-row-meta"><StatusBadge domain="transfer" value={item.status} /><span><small>Người giao</small>{item.deliveredBy}</span><span><small>Thời gian gửi</small>{new Date(item.submittedAt).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' })}</span>{item.status === 'PENDING' && <span><small>Thời gian chờ</small>{Math.max(0, Math.floor((Date.now() - new Date(item.submittedAt).getTime()) / 60000))} phút</span>}</div><button className="outline-button" onClick={() => navigate(`/admin/receiving/transfers/${item.id}`)}>Xem phiếu <span aria-hidden="true">→</span></button></article>)}</div> : <p className="receiving-empty">Không có phiếu phù hợp.</p>}<nav className="receiving-pagination" aria-label="Phân trang"><button className="outline-button" disabled={data.first} onClick={() => update('page',String(page-1))}>Trước</button> <span>Trang {page+1} / {Math.max(data.totalPages,1)}</span> <button className="outline-button" disabled={data.last} onClick={() => update('page',String(page+1))}>Sau</button></nav></section>}
  </section>;
}
