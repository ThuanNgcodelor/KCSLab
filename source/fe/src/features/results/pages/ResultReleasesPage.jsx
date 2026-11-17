import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listResultReleases } from '../api/resultsApi';
import { PageLoader } from '../../../shared/components/Loading';
import { resultStatusLabels } from './ResultReleaseDetailPage';

export function ResultReleasesPage() {
  const navigate = useNavigate();
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try { setData(await listResultReleases({ page, status })); }
    catch (exception) { setError(exception.message); }
    finally { setLoading(false); }
  }, [page, status]);
  useEffect(() => { load(); }, [load]);

  return <section className="admin-page staff-page results-page">
    <header className="admin-heading staff-heading"><div><p className="section-kicker">Hồ sơ kết quả / KQKN-DV</p><h1>Hồ sơ kết quả</h1><p className="heading-copy">Tải file, gửi duyệt, phát hành và thu hồi. Chỉ hồ sơ đã phát hành mới tra cứu được công khai.</p></div></header>
    {error && <p className="form-error intake-toast" role="alert">{error}</p>}
    <section className="panel-card intake-list-card">
      <div className="panel-heading"><div><p className="section-kicker">Danh sách</p><h2>Hồ sơ gần đây</h2></div>
        <label className="inline-filter">Trạng thái<select value={status} onChange={(event) => { setPage(0); setStatus(event.target.value); }}><option value="">Tất cả</option>{Object.entries(resultStatusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
        <button className="outline-button small-button" onClick={load}>↻ Làm mới</button></div>
      {loading ? <PageLoader label="Đang tải hồ sơ kết quả…" rows={3} /> : !data || data.content.length === 0
        ? <div className="empty-intake"><span>⌁</span><h3>Chưa có hồ sơ kết quả</h3><p>Hồ sơ được lập từ một biên bản thử nghiệm đã kiểm tra (mở biên bản → Lập hồ sơ kết quả).</p></div>
        : <div className="results-table" role="table" aria-label="Hồ sơ kết quả">
          {data.content.map((item) => <button type="button" className="results-row" role="row" key={item.id} onClick={() => navigate(`/admin/results/${item.id}`)}>
            <span><strong>{item.testingRecordCode}</strong><small>Mẫu {item.sampleCode} · v{item.version}</small></span>
            <span className={`result-status result-${item.status.toLowerCase()}`}>{resultStatusLabels[item.status]}</span>
            <span><small>Số phiếu</small><strong>{item.resultNumber || '—'}</strong></span>
            <span><small>Cập nhật</small><strong>{new Date(item.updatedAt).toLocaleString('vi-VN')}</strong></span>
          </button>)}
          <div className="pager"><button className="outline-button small-button" disabled={data.first} onClick={() => setPage(page - 1)}>← Trước</button><span>Trang {data.page + 1} / {Math.max(1, data.totalPages)}</span><button className="outline-button small-button" disabled={data.last} onClick={() => setPage(page + 1)}>Sau →</button></div>
        </div>}
    </section>
  </section>;
}
