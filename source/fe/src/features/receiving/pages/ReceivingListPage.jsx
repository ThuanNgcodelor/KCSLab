import { WorkflowGuide } from '../../../shared/components/WorkflowGuide';
import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { listReceivingTransfers } from '../api/receivingApi';
import { useNotifications } from '../../notifications/hooks/useNotifications';
import { StatusBadge } from '../../../shared/components/StatusBadge';
import { labelsOf } from '../../../shared/status';
import { PageLoader } from '../../../shared/components/Loading';
import { ListToolbar, Pagination } from '../../../shared/components/ListToolbar';

const labels = labelsOf('transfer');
const receivingStatuses = ['PENDING', 'ACCEPTED', 'REJECTED'];
export function ReceivingListPage() {
  const [params, setParams] = useSearchParams();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const { dataVersion } = useNotifications();
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
  }, [status, page, size, q, from, to, sort, dataVersion]);
  return <section className="admin-page receiving-page receiving-list"><header className="admin-heading"><div><p className="section-kicker">Nhận mẫu KCS / Danh sách</p><h1>Nhận mẫu KCS</h1><p className="heading-copy">Theo dõi phiếu bàn giao từ bộ phận tiếp nhận và xác nhận nhận mẫu.</p></div></header>
    <WorkflowGuide stage="handover" />
    <nav className="receiving-filter receiving-status-tabs" aria-label="Trạng thái phiếu">{receivingStatuses.map((key) => <button key={key} className={status === key ? 'primary-button' : 'outline-button'} aria-pressed={status === key} onClick={() => update('status',key)}>{labels[key]}</button>)}</nav>
    <ListToolbar q={q} onQ={(value) => update('q', value)} searchLabel="Tìm Số/No. hoặc mã hóa mẫu"
      placeholder="Ví dụ: CFC26.0001" from={from} to={to}
      onFrom={(value) => update('from', value)} onTo={(value) => update('to', value)}
      sort={sort} onSort={(value) => update('sort', value)} sortOptions={[
        ['', 'Mặc định'], ['submittedAt,desc', 'Gửi: mới nhất'], ['submittedAt,asc', 'Gửi: cũ nhất'],
        ['receivedAt,desc', 'Nhận: mới nhất'], ['resultsDueDate,asc', 'Hạn trả: gần nhất'], ['transferCode,asc', 'Mã phiếu: A → Z'],
      ]}
      filters={[{ label: 'Số phiếu mỗi trang', value: size, onChange: (value) => update('size', value), options: [['20', '20'], ['50', '50']] }]}
      onReset={() => setParams({ status })} />
    {error && <p role="alert" className="form-error">{error}</p>}{loading && <PageLoader label="Đang tải phiếu…" rows={4} />}
    {!loading && data && <section className="panel-card"><div className="receiving-section-heading"><div><h2>{labels[status]}</h2><p>Danh sách phiếu chuyển mẫu đến phòng KCS</p></div><span className="receiving-count">{data.totalElements ?? data.content.length} phiếu</span></div>{data.content.length ? <div className="receiving-cards">{data.content.map(item => <article className="receiving-row" key={item.id}><div className="receiving-row-main"><strong>Số/No {item.requestNumber || '—'} · {item.items.length} mẫu</strong><span className="receiving-sample-codes">Mã hóa mẫu: {item.items.map(sample => sample.sampleCode).join(', ')}</span></div><div className="receiving-row-meta"><StatusBadge domain="transfer" value={item.status} /><span><small>Người giao</small>{item.deliveredBy}</span><span><small>Thời gian gửi</small>{new Date(item.submittedAt).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' })}</span>{item.status === 'PENDING' && <span><small>Thời gian chờ</small>{Math.max(0, Math.floor((Date.now() - new Date(item.submittedAt).getTime()) / 60000))} phút</span>}</div><button className="outline-button" onClick={() => navigate(`/admin/receiving/transfers/${item.id}`)}>Xem phiếu <span aria-hidden="true">→</span></button></article>)}</div> : <p className="receiving-empty">Không có phiếu phù hợp.</p>}<Pagination data={data} onPage={(nextPage) => update('page', String(nextPage))} noun="phiếu" /></section>}
  </section>;
}
