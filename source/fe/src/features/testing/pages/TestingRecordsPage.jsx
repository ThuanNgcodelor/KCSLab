import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listTestingRecords } from '../api/testingApi';
import { StatusBadge } from '../../../shared/components/StatusBadge';
import { labelsOf } from '../../../shared/status';
import { PageLoader } from '../../../shared/components/Loading';
import { ListToolbar, Pagination, emptyPage } from '../../../shared/components/ListToolbar';
import { useListState } from '../../../shared/hooks/useListState';
import { Icon } from '../../../shared/components/Icon';

const statusLabels = labelsOf('testing');

const sortOptions = [
  ['createdAt,desc', 'Mới lập nhất'], ['createdAt,asc', 'Cũ nhất'],
  ['resultsDueDate,asc', 'Thời hạn: gần nhất'], ['resultsDueDate,desc', 'Thời hạn: xa nhất'],
  ['recordCode,asc', 'Mã biên bản: A → Z'], ['recordCode,desc', 'Mã biên bản: Z → A'],
  ['sampleCode,asc', 'Mã mẫu: A → Z'], ['status,asc', 'Trạng thái: A → Z'],
];
const defaults = { q: '', from: '', to: '', status: '', sort: 'createdAt,desc' };

export function TestingRecordsPage() {
  const navigate = useNavigate();
  const [list, setList] = useListState(defaults);
  const [data, setData] = useState(emptyPage);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadRecords = useCallback(async () => {
    setLoading(true); setError('');
    try { setData(await listTestingRecords(list)); }
    catch (exception) { setError(exception.message); }
    finally { setLoading(false); }
  }, [list]);

  useEffect(() => { loadRecords(); }, [loadRecords]);
  const records = data.content;

  return <section className="staff-page testing-page">
    <header className="admin-heading staff-heading"><div><p className="section-kicker">Phòng phân tích / Phiếu số 5</p><h1>Biên bản thử nghiệm — Phiếu 5</h1><p className="heading-copy">Chọn mẫu đã được KCS nhận và tải file kết quả DOCX/PDF để lập Biên bản thử nghiệm số 5.</p></div><button className="primary-button add-staff-button" onClick={() => navigate('/admin/testing/new')}>＋ Tạo Biên bản thử nghiệm — Phiếu 5</button></header>
    <div className="intake-guide testing-guide" aria-label="Tiến độ xử lý mẫu"><div className="guide-step done"><span>✓</span><div><strong>Tiếp nhận</strong><small>Phiếu số 3</small></div></div><i className="guide-line done" /><div className="guide-step done"><span>✓</span><div><strong>Chuyển mẫu</strong><small>Phiếu số 4</small></div></div><i className="guide-line done" /><div className="guide-step current"><span>03</span><div><strong>Thử nghiệm</strong><small>Biên bản số 5</small></div></div></div>
    {error && <p className="form-error intake-toast" role="alert">{error}</p>}
    <section className="panel-card intake-list-card"><div className="panel-heading"><div><p className="section-kicker">Hồ sơ chuyên môn</p><h2>Biên bản gần đây</h2></div><button className="outline-button small-button" onClick={loadRecords}>↻ Làm mới</button></div>
      <ListToolbar q={list.q} onQ={(q) => setList({ q })} searchLabel="Tìm biên bản" placeholder="Mã biên bản, mã mẫu, tên mẫu, phương pháp…" dateLabel="Ngày lập" from={list.from} to={list.to} onFrom={(from) => setList({ from })} onTo={(to) => setList({ to })}
        filters={[{ label: 'Trạng thái', value: list.status, onChange: (status) => setList({ status }), options: [['', 'Tất cả'], ...Object.entries(statusLabels)] }]}
        sort={list.sort} sortOptions={sortOptions} onSort={(sort) => setList({ sort })} onReset={() => setList({ ...defaults })} />
      {loading ? <PageLoader label="Đang tải Biên bản 5…" rows={3} /> : records.length === 0 ? <div className="empty-intake"><span><Icon name="flask" size={22} /></span><h3>Chưa có Biên bản thử nghiệm — Phiếu 5</h3><p>Chọn một mẫu đã bàn giao và tải file kết quả hoàn chỉnh để tạo Phiếu 5.</p><button className="primary-button" onClick={() => navigate('/admin/testing/new')}>Tạo Biên bản thử nghiệm — Phiếu 5 đầu tiên</button></div> : <div className="table-wrap"><table className="staff-table testing-table"><thead><tr><th>Biên bản / mẫu</th><th>Phương pháp</th><th>Người phân tích</th><th>Thời hạn</th><th>Trạng thái</th><th /></tr></thead><tbody>{records.map((record) => <tr key={record.id}><td><strong className="receipt-code">{record.recordCode}</strong><small>{record.sampleCode} · {record.sampleName}</small></td><td>{record.testMethod}<small>{record.requestedParameters}</small></td><td>{record.analyst}</td><td>{formatDate(record.dueDate)}</td><td><StatusBadge domain="testing" value={record.status} /></td><td><button className="table-action" onClick={() => navigate(`/admin/testing/${record.id}`)}>Xem Biên bản 5</button></td></tr>)}</tbody></table></div>}
      <Pagination data={data} loading={loading} noun="biên bản" onPage={(page) => setList({ page })} />
    </section>
  </section>;
}

function formatDate(value) { return value ? new Date(`${value}T00:00:00`).toLocaleDateString('vi-VN') : '—'; }
