import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listTestRequests } from '../api/testRequestApi';
import { PageLoader } from '../../../shared/components/Loading';

export function TestRequestsPage() {
  const navigate = useNavigate();
  const [rows, setRows] = useState(null);
  const [query, setQuery] = useState('');
  const [error, setError] = useState('');
  useEffect(() => { listTestRequests().then(setRows).catch((e) => setError(e.message)); }, []);
  const visible = useMemo(() => {
    const q = query.trim().toLocaleLowerCase('vi');
    return q ? (rows ?? []).filter((row) => `${row.requestNumber} ${row.customerName} ${row.sampleName}`.toLocaleLowerCase('vi').includes(q)) : (rows ?? []);
  }, [rows, query]);
  if (!rows && !error) return <section className="staff-page"><PageLoader label="Đang tải Phiếu yêu cầu kiểm nghiệm…" /></section>;
  return <section className="staff-page sampling-page">
    <header className="admin-heading staff-heading"><div><p className="section-kicker">Quy trình hồ sơ</p><h1>Yêu cầu kiểm nghiệm</h1><p className="heading-copy">Tạo và quản lý yêu cầu kiểm nghiệm cùng các hồ sơ lấy mẫu liên kết.</p></div><button className="primary-button" onClick={() => navigate('/admin/requests/new')}>＋ Tạo yêu cầu kiểm nghiệm</button></header>
    {error && <p className="form-error" role="alert">{error}</p>}
    <section className="panel-card request-list-panel">
      <label className="request-search">Tìm theo số phiếu, khách hàng hoặc mẫu<input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Ví dụ: YC-2026-001" /></label>
      {!visible.length ? <div className="empty-state sampling-empty"><strong>{query ? 'Không tìm thấy Phiếu yêu cầu kiểm nghiệm' : 'Chưa có Phiếu yêu cầu kiểm nghiệm'}</strong><span>{query ? 'Thử lại với số phiếu, khách hàng hoặc tên mẫu khác.' : 'Dùng nút “Tạo Phiếu yêu cầu kiểm nghiệm” ở phía trên để bắt đầu hồ sơ.'}</span></div> : <div className="request-list">
        {visible.map((row) => <article className="request-row" key={row.id}>
          <div><span className={`status-pill ${row.status === 'COMPLETED' ? 'status-success' : 'status-neutral'}`}>{row.status === 'COMPLETED' ? 'Đã hoàn tất' : 'Bản nháp'}</span><h2>{row.requestNumber}</h2><p>{row.customerName} · {row.sampleName}</p></div>
          <div className="request-row-meta"><small>Ngày yêu cầu</small><strong>{formatDate(row.requestDate)}</strong><small>Hồ sơ lấy mẫu</small><strong>{row.fieldSampling ? 'Đã có thông tin lấy mẫu' : row.samplingPlan ? 'Đã có kế hoạch lấy mẫu' : 'Có thể bỏ qua'}</strong></div>
          <button className="outline-button" onClick={() => navigate(`/admin/requests/${row.id}`)}>Mở hồ sơ →</button>
        </article>)}
      </div>}
    </section>
  </section>;
}

function formatDate(value) { return value ? new Intl.DateTimeFormat('vi-VN').format(new Date(`${value}T00:00:00`)) : '—'; }
