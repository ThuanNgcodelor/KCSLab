import { WorkflowGuide } from '../../../shared/components/WorkflowGuide';
import { useCallback, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { listTestRequests } from '../api/testRequestApi';
import { StatusBadge } from '../../../shared/components/StatusBadge';
import { PageLoader } from '../../../shared/components/Loading';
import { ListToolbar, Pagination, emptyPage } from '../../../shared/components/ListToolbar';
import { useListState } from '../../../shared/hooks/useListState';
import { Icon } from '../../../shared/components/Icon';

const sortOptions = [
  ['createdAt,desc', 'Mới tạo nhất'], ['createdAt,asc', 'Cũ nhất'],
  ['requestDate,desc', 'Ngày yêu cầu: mới → cũ'], ['requestDate,asc', 'Ngày yêu cầu: cũ → mới'],
  ['expectedDate,asc', 'Hạn trả: gần nhất'], ['expectedDate,desc', 'Hạn trả: xa nhất'],
  ['requestNumber,asc', 'Số phiếu: A → Z'], ['requestNumber,desc', 'Số phiếu: Z → A'],
  ['customerName,asc', 'Khách hàng: A → Z'], ['customerName,desc', 'Khách hàng: Z → A'],
  ['sampleName,asc', 'Tên mẫu: A → Z'], ['status,asc', 'Trạng thái: A → Z'],
];
const defaults = { q: '', from: '', to: '', status: '', samplingMode: '', sort: 'createdAt,desc' };

export function TestRequestsPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [list, setList] = useListState(defaults);
  const [data, setData] = useState(emptyPage);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try { setData(await listTestRequests(list)); }
    catch (exception) { setError(exception.message); }
    finally { setLoading(false); }
  }, [list]);
  useEffect(() => { load(); }, [load]);

  const rows = data.content;
  const filtered = Boolean(list.q || list.from || list.to || list.status || list.samplingMode);

  return <section className="intake-page request-page">
    <header className="admin-heading"><div><p className="section-kicker">Quy trình hồ sơ / Yêu cầu kiểm nghiệm</p><h1>Yêu cầu kiểm nghiệm</h1><p className="heading-copy">Phiếu 1 là điểm bắt đầu của hồ sơ; từ đây lập Kế hoạch lấy mẫu (07), Lấy mẫu hiện trường (02) và tiếp nhận mẫu.</p></div>
      <button className="primary-button" onClick={() => navigate('/admin/requests/new')}>＋ Tạo yêu cầu kiểm nghiệm</button></header>
    <WorkflowGuide stage="request" />
    {location.state?.message && <p className="success-note intake-toast" role="status">✓ {location.state.message}</p>}
    {error && <p className="form-error intake-toast" role="alert">{error}</p>}
    <section className="panel-card intake-list-card">
      <div className="panel-heading"><div><p className="section-kicker">Danh sách</p><h2>Phiếu yêu cầu kiểm nghiệm</h2></div><button className="outline-button small-button" onClick={load}>↻ Làm mới</button></div>
      <ListToolbar q={list.q} onQ={(q) => setList({ q })} searchLabel="Tìm phiếu" placeholder="Số phiếu, khách hàng, tên mẫu, mã lô, số điện thoại…"
        dateLabel="Ngày yêu cầu" from={list.from} to={list.to} onFrom={(from) => setList({ from })} onTo={(to) => setList({ to })}
        filters={[
          { label: 'Trạng thái', value: list.status, onChange: (status) => setList({ status }), options: [['', 'Tất cả'], ['DRAFT', 'Bản nháp'], ['COMPLETED', 'Đã hoàn tất']] },
          { label: 'Nguồn lấy mẫu', value: list.samplingMode, onChange: (samplingMode) => setList({ samplingMode }), options: [['', 'Tất cả'], ['COMPANY', 'Công ty lấy mẫu'], ['CUSTOMER', 'Khách tự lấy mẫu']] },
        ]}
        sort={list.sort} sortOptions={sortOptions} onSort={(sort) => setList({ sort })} onReset={() => setList({ ...defaults })} />
      {loading ? <PageLoader label="Đang tải Phiếu yêu cầu kiểm nghiệm…" rows={3} /> : rows.length === 0
        ? <div className="empty-intake"><span><Icon name="file" size={22} /></span><h3>{filtered ? 'Không tìm thấy Phiếu yêu cầu phù hợp' : 'Chưa có Phiếu yêu cầu kiểm nghiệm'}</h3>
          <p>{filtered ? 'Thử đổi từ khóa, khoảng ngày hoặc xóa bộ lọc.' : 'Tạo Phiếu yêu cầu đầu tiên để bắt đầu hồ sơ lấy mẫu và tiếp nhận.'}</p>
          {filtered ? <button className="outline-button" onClick={() => setList({ ...defaults })}>Xóa bộ lọc</button> : <button className="outline-button" onClick={() => navigate('/admin/requests/new')}>Tạo yêu cầu đầu tiên</button>}</div>
        : <div className="table-wrap"><table className="staff-table request-table"><thead><tr><th>Số phiếu</th><th>Mẫu / lô</th><th>Ngày yêu cầu</th><th>Hạn trả</th><th>Hồ sơ lấy mẫu</th><th>Trạng thái</th><th /></tr></thead>
          <tbody>{rows.map((row) => <tr key={row.id}>
            <td data-label="Số phiếu"><span className="transfer-cell-copy"><strong className="receipt-code">{row.requestNumber}</strong><small>{row.customerName}</small></span></td>
            <td data-label="Mẫu / lô"><span className="transfer-cell-copy"><strong>{row.sampleName}</strong><small>{row.lotCode || '—'}</small></span></td>
            <td data-label="Ngày yêu cầu">{formatDate(row.requestDate)}</td>
            <td data-label="Hạn trả">{formatDate(row.expectedDate)}</td>
            <td data-label="Hồ sơ lấy mẫu"><span className="request-progress">
              {row.samplingMode === 'CUSTOMER' && <span className="request-chip request-chip--info">Khách tự lấy mẫu</span>}
              <span className={`request-chip ${row.samplingPlan ? 'request-chip--done' : ''}`} title="Kế hoạch lấy mẫu">07 {row.samplingPlan ? '✓' : '—'}</span>
              <span className={`request-chip ${row.fieldSampling ? 'request-chip--done' : ''}`} title="Lấy mẫu hiện trường">02 {row.fieldSampling ? '✓' : '—'}</span></span></td>
            <td data-label="Trạng thái"><StatusBadge domain="testRequest" value={row.status} /></td>
            <td data-label="Thao tác"><button className="table-action" onClick={() => navigate(`/admin/requests/${row.id}`)}>Mở hồ sơ</button></td>
          </tr>)}</tbody></table></div>}
      <Pagination data={data} loading={loading} noun="phiếu" onPage={(page) => setList({ page })} />
    </section>
  </section>;
}

function formatDate(value) { return value ? new Date(`${value}T00:00:00`).toLocaleDateString('vi-VN') : '—'; }
