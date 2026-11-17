import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { createResultRelease, listReadyRecords, listResultReleases } from '../api/resultsApi';
import { StatusBadge } from '../../../shared/components/StatusBadge';
import { PageLoader } from '../../../shared/components/Loading';
import { ListToolbar, Pagination, emptyPage } from '../../../shared/components/ListToolbar';
import { useListState } from '../../../shared/hooks/useListState';
import { resultStatusLabels } from './ResultReleaseDetailPage';
import { Icon } from '../../../shared/components/Icon';

const sortOptions = [
  ['updatedAt,desc', 'Cập nhật mới nhất'], ['updatedAt,asc', 'Cập nhật cũ nhất'],
  ['createdAt,desc', 'Mới lập nhất'], ['publishedAt,desc', 'Phát hành gần nhất'],
  ['recordCode,asc', 'Mã biên bản: A → Z'], ['resultNumber,asc', 'Số phiếu: A → Z'],
  ['status,asc', 'Trạng thái: A → Z'], ['version,desc', 'Phiên bản: cao → thấp'],
];
const defaults = { q: '', from: '', to: '', status: '', sort: 'updatedAt,desc' };

export function ResultReleasesPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [list, setList] = useListState(defaults);
  const [data, setData] = useState(emptyPage);
  const [ready, setReady] = useState([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const [page, waiting] = await Promise.all([listResultReleases(list), listReadyRecords()]);
      setData(page); setReady(waiting);
    } catch (exception) { setError(exception.message); }
    finally { setLoading(false); }
  }, [list]);
  useEffect(() => { load(); }, [load]);

  async function start(record) {
    setCreating(record.testingRecordId); setError('');
    try { const release = await createResultRelease(record.testingRecordId); navigate(`/admin/results/${release.id}`); }
    catch (exception) { setError(exception.message); setCreating(null); }
  }

  const canPrepare = ['TECHNICIAN', 'ADMIN'].includes(user?.role);
  const filtered = Boolean(list.q || list.from || list.to || list.status);

  return <section className="staff-page results-page">
    <header className="admin-heading staff-heading"><div><p className="section-kicker">Hồ sơ kết quả / KQKN-DV</p><h1>Hồ sơ kết quả</h1><p className="heading-copy">Soạn phiếu kết quả KQKN từ biên bản thử nghiệm, gửi duyệt, phát hành PDF và thu hồi. Chỉ phiếu đã phát hành mới tra cứu và tải được công khai.</p></div></header>
    <div className="intake-guide testing-guide" aria-label="Tiến độ xử lý mẫu"><div className="guide-step done"><span>✓</span><div><strong>Tiếp nhận</strong><small>Phiếu số 3</small></div></div><i className="guide-line done" /><div className="guide-step done"><span>✓</span><div><strong>Chuyển mẫu</strong><small>Phiếu số 4</small></div></div><i className="guide-line done" /><div className="guide-step done"><span>✓</span><div><strong>Thử nghiệm</strong><small>Biên bản số 5</small></div></div><i className="guide-line done" /><div className="guide-step current"><span>04</span><div><strong>Kết quả</strong><small>Phiếu KQKN</small></div></div></div>
    {error && <p className="form-error intake-toast" role="alert">{error}</p>}
    <section className="panel-card intake-list-card ready-card">
      <div className="panel-heading"><div><p className="section-kicker">Bước tiếp theo</p><h2>Biên bản đã kiểm tra, chờ lập hồ sơ</h2></div><span className="receiving-count">{ready.length} biên bản</span></div>
      {loading ? <PageLoader label="Đang tải…" rows={2} /> : ready.length === 0
        ? <div className="empty-intake compact-empty"><span><Icon name="checkCircle" size={22} /></span><h3>Không có biên bản nào đang chờ</h3><p>Biên bản xuất hiện ở đây sau khi được kiểm tra (Thử nghiệm → Đã kiểm tra).</p></div>
        : <div className="table-wrap"><table className="staff-table"><thead><tr><th>Biên bản / mẫu</th><th>Người phân tích</th><th>Kiểm tra lúc</th><th /></tr></thead><tbody>{ready.map((record) => <tr key={record.testingRecordId}>
          <td><Link className="receipt-code" to={`/admin/testing/${record.testingRecordId}`}>{record.recordCode}</Link><small>{record.sampleCode} · {record.sampleName}</small></td>
          <td>{record.analystName}</td>
          <td>{formatDateTime(record.reviewedAt)}</td>
          <td>{canPrepare ? <button type="button" className="table-action" disabled={creating === record.testingRecordId} onClick={() => start(record)}>{creating === record.testingRecordId ? 'Đang tạo…' : 'Lập hồ sơ'}</button> : <small>Không có quyền lập</small>}</td>
        </tr>)}</tbody></table></div>}
    </section>
    <section className="panel-card intake-list-card">
      <div className="panel-heading"><div><p className="section-kicker">Phiếu kết quả</p><h2>Hồ sơ gần đây</h2></div><button className="outline-button small-button" onClick={load}>↻ Làm mới</button></div>
      <ListToolbar q={list.q} onQ={(q) => setList({ q })} searchLabel="Tìm hồ sơ" placeholder="Mã biên bản, mã mẫu, số phiếu…" dateLabel="Cập nhật" from={list.from} to={list.to} onFrom={(from) => setList({ from })} onTo={(to) => setList({ to })}
        filters={[{ label: 'Trạng thái', value: list.status, onChange: (status) => setList({ status }), options: [['', 'Tất cả'], ...Object.entries(resultStatusLabels)] }]}
        sort={list.sort} sortOptions={sortOptions} onSort={(sort) => setList({ sort })} onReset={() => setList({ ...defaults })} />
      {loading ? <PageLoader label="Đang tải hồ sơ kết quả…" rows={3} /> : data.content.length === 0
        ? <div className="empty-intake"><span><Icon name="file" size={22} /></span>{filtered ? <><h3>Không có hồ sơ phù hợp</h3><p>Thử đổi hoặc xóa bộ lọc.</p></> : <><h3>Chưa có hồ sơ kết quả nào</h3><p>Lập hồ sơ từ một biên bản đã kiểm tra ở phần “Bước tiếp theo” phía trên.</p></>}</div>
        : <div className="table-wrap"><table className="staff-table"><thead><tr><th>Biên bản / mẫu</th><th>Số phiếu</th><th>Phiên bản</th><th>Cập nhật</th><th>Trạng thái</th><th /></tr></thead><tbody>{data.content.map((item) => <tr key={item.id}>
          <td><strong className="receipt-code">{item.testingRecordCode}</strong><small>Mẫu {item.sampleCode}</small></td>
          <td>{item.resultNumber || '—'}</td>
          <td>v{item.version}</td>
          <td>{formatDateTime(item.updatedAt)}</td>
          <td><StatusBadge domain="release" value={item.status} /></td>
          <td><button type="button" className="table-action" onClick={() => navigate(`/admin/results/${item.id}`)}>Mở hồ sơ</button></td>
        </tr>)}</tbody></table></div>}
      <Pagination data={data} loading={loading} noun="hồ sơ" onPage={(page) => setList({ page })} />
    </section>
  </section>;
}

function formatDateTime(value) { return value ? new Date(value).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' }) : '—'; }
