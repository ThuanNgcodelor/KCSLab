import { useCallback, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { listTransfers } from '../api/transferApi';
import { StatusBadge } from '../../../shared/components/StatusBadge';
import { labelsOf } from '../../../shared/status';
import { PageLoader } from '../../../shared/components/Loading';
import { ListToolbar, Pagination, emptyPage } from '../../../shared/components/ListToolbar';
import { useListState } from '../../../shared/hooks/useListState';
import { Icon } from '../../../shared/components/Icon';

const statusLabels = labelsOf('transfer');

const sortOptions = [
  ['createdAt,desc', 'Mới tạo nhất'], ['createdAt,asc', 'Cũ nhất'],
  ['transferDate,desc', 'Ngày chuyển: mới → cũ'], ['transferDate,asc', 'Ngày chuyển: cũ → mới'],
  ['resultsDueDate,asc', 'Hạn trả: gần nhất'], ['resultsDueDate,desc', 'Hạn trả: xa nhất'],
  ['transferCode,asc', 'Mã phiếu: A → Z'], ['transferCode,desc', 'Mã phiếu: Z → A'],
  ['customerName,asc', 'Khách hàng: A → Z'],
  ['customerName,desc', 'Khách hàng: Z → A'], ['status,asc', 'Trạng thái: A → Z'],
];
const defaults = { q: '', from: '', to: '', status: '', sort: 'createdAt,desc' };

export function TransfersPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [list, setList] = useListState(defaults);
  const [data, setData] = useState(emptyPage);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const canCreate = ['ADMIN', 'RECEPTIONIST'].includes(user?.role);

  const loadTransfers = useCallback(async () => {
    setLoading(true); setError('');
    try { setData(await listTransfers(list)); }
    catch (exception) { setError(exception.message); }
    finally { setLoading(false); }
  }, [list]);

  useEffect(() => { loadTransfers(); }, [loadTransfers]);
  const transfers = data.content;

  return <section className="intake-page transfer-page">
    <header className="admin-heading"><div><p className="section-kicker">Vận hành / Chuyển mẫu</p><h1>Chuyển mẫu KCS/PTN</h1><p className="heading-copy">Bản nháp do tiếp nhận chuẩn bị; chỉ phiếu đã gửi mới khóa mẫu và chờ phòng nhận.</p></div>{canCreate && <button className="primary-button" onClick={() => navigate('/admin/transfers/new')}>＋ Tạo hồ sơ chuyển mẫu</button>}</header>
    {location.state?.message && <p className="success-note intake-toast" role="status">✓ {location.state.message}</p>}
    {error && <p className="form-error intake-toast" role="alert">{error}</p>}
    <div className="intake-guide"><div className="guide-step done"><span>✓</span><div><strong>Tiếp nhận</strong><small>Hồ sơ tiếp nhận</small></div></div><div className="guide-line done" /><div className="guide-step current"><span>02</span><div><strong>Chuyển mẫu</strong><small>Bàn giao sang KCS</small></div></div><div className="guide-line" /><div className="guide-step"><span>03</span><div><strong>Thử nghiệm</strong><small>Hồ sơ phân tích</small></div></div></div>
    <section className="panel-card intake-list-card">
      <div className="panel-heading"><div><p className="section-kicker">Chuỗi bàn giao</p><h2>Phiếu chuyển mẫu</h2></div><button className="outline-button small-button" onClick={loadTransfers}>↻ Làm mới</button></div>
      <ListToolbar q={list.q} onQ={(q) => setList({ q })} searchLabel="Tìm phiếu" placeholder="Mã phiếu chuyển, mã tiếp nhận, khách hàng…" dateLabel="Ngày chuyển" from={list.from} to={list.to} onFrom={(from) => setList({ from })} onTo={(to) => setList({ to })}
        filters={[{ label: 'Trạng thái', value: list.status, onChange: (status) => setList({ status }), options: [['', 'Tất cả'], ...Object.entries(statusLabels)] }]}
        sort={list.sort} sortOptions={sortOptions} onSort={(sort) => setList({ sort })} onReset={() => setList({ ...defaults })} />
      {loading ? <PageLoader label="Đang tải hồ sơ chuyển mẫu…" rows={3} /> : transfers.length === 0 ? <div className="empty-intake"><span><Icon name="transfer" size={22} /></span><h3>Chưa có hồ sơ chuyển mẫu</h3><p>Tạo hồ sơ chuyển mẫu từ hồ sơ tiếp nhận để bàn giao mẫu.</p>{canCreate && <button className="outline-button" onClick={() => navigate('/admin/transfers/new')}>Tạo hồ sơ chuyển mẫu đầu tiên</button>}</div> : <div className="table-wrap"><table className="staff-table transfer-table"><thead><tr><th>Mã phiếu</th><th>Phiếu tiếp nhận</th><th>Nơi nhận</th><th>Mẫu</th><th>Hạn trả</th><th>Trạng thái</th><th /></tr></thead><tbody>{transfers.map((transfer) => <tr key={transfer.id}>
        <td data-label="Mã phiếu"><span className="transfer-cell-copy"><strong className="receipt-code">{transfer.transferCode}</strong><small>{transfer.customerName}</small></span></td>
        <td data-label="Tiếp nhận"><span className="transfer-cell-copy"><strong>{transfer.requestNumber || '—'}</strong><small>{transfer.deliveredBy}</small></span></td>
        <td data-label="Nơi nhận">{transfer.receivingDepartment}</td>
        <td data-label="Số mẫu">{transfer.items.length} mẫu</td>
        <td data-label="Hạn trả">{formatDate(transfer.resultsDueDate)}</td>
        <td data-label="Trạng thái"><StatusBadge domain="transfer" value={transfer.status} /></td>
        <td data-label="Thao tác"><button className="table-action" onClick={() => navigate(`/admin/transfers/${transfer.id}`)}>Xem phiếu</button></td>
      </tr>)}</tbody></table></div>}
      <Pagination data={data} loading={loading} noun="phiếu" onPage={(page) => setList({ page })} />
    </section>
  </section>;
}

function formatDate(value) { return value ? new Date(`${value}T00:00:00`).toLocaleDateString('vi-VN') : '—'; }
