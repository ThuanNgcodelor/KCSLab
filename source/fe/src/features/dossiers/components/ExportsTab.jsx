import { useEffect, useState } from 'react';
import * as api from '../api/dossierApi';
import { bytes, count, date, exportErrors, exportStatuses, readyStatuses, runningStatuses, scopeLabel } from '../dossierFormat';

const cancellable = ['QUEUED', 'PREPARING', 'PROCESSING'];
function parseScope(text) { try { return JSON.parse(text); } catch { return {}; } }

export function ExportsTab() {
  const [page, setPage] = useState(0);
  const [rows, setRows] = useState(null);
  const [detail, setDetail] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [reload, setReload] = useState(0);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const controller = new AbortController(); let timer;
    async function refresh() {
      let again = 4000;
      try {
        const data = await api.listExports(page, controller.signal);
        if (controller.signal.aborted) return;
        setRows(data); setError('');
        if (!data.some((r) => runningStatuses.includes(r.status))) again = 0;
      } catch (e) {
        if (controller.signal.aborted) return;
        setError(e.message); again = 15000; // keep watching a running job through a short network loss
      }
      setLoading(false);
      if (again) timer = setTimeout(refresh, again);
    }
    setLoading(true); refresh();
    return () => { controller.abort(); clearTimeout(timer); };
  }, [page, reload]);

  async function run(work) {
    setBusy(true); setError('');
    try { await work(); } catch (e) { setError(e.message); } finally { setBusy(false); }
  }

  return <>
    <div className="dossier-toolbar"><p>ZIP giữ cây năm/tháng/Số CFC · gói tải tạm được giữ 7 ngày.</p>
      <button type="button" className="outline-button" disabled={loading} onClick={() => setReload((n) => n + 1)}>Làm mới</button></div>
    <p className="dossier-muted">Tải từng phần rồi giải nén vào cùng một thư mục. Muốn chọn nơi lưu trên máy, bật “Hỏi nơi lưu mỗi lần tải” trong trình duyệt.</p>
    {loading && <p role="status">Đang tải các lượt xuất…</p>}
    {error && <p className="form-error" role="alert">{error}</p>}
    {rows?.length === 0 && <div className="panel-card dossier-empty">{page ? 'Không còn lượt xuất cũ hơn.' : 'Chưa có lượt xuất. Chọn hồ sơ hoặc tháng/năm ở mục Hồ sơ để bắt đầu.'}</div>}
    <div className="dossier-export-list">{rows?.map((job) => {
      const open = detail?.id === job.id;
      return <article className="panel-card dossier-export" key={job.id}>
        <div>
          <span className={`dossier-badge ${['FAILED', 'COMPLETED_WITH_WARNINGS'].includes(job.status) ? 'warning' : ''}`}>{exportStatuses[job.status] || job.status}</span>
          <h2>{scopeLabel({ ...parseScope(job.scope_json), label: job.receipt_number })}</h2>
          <p>{date(job.created_at)} · {count(job.processed_files)} / {count(job.total_files)} mục · {bytes(job.processed_bytes)} / {bytes(job.total_bytes)}</p>
          {job.status === 'PROCESSING' && <progress value={job.processed_files} max={job.total_files || 1} aria-label="Tiến độ đóng gói" />}
          {job.status === 'FAILED' && <p className="form-error">{exportErrors[job.error_code] || 'Chưa xuất đủ hồ sơ. Kiểm tra ổ lưu, dung lượng rồi tạo lượt mới.'}</p>}
          {job.status === 'COMPLETED_WITH_WARNINGS' && <p>{count(job.warning_count)} file thiếu hoặc sai checksum, xem cột trang_thai trong danh-muc-goi-….csv của từng ZIP.</p>}
          {readyStatuses.includes(job.status) && <p>Tải được đến {date(job.expires_at)}.</p>}
        </div>
        <div className="dossier-actions">
          {readyStatuses.includes(job.status) && <button type="button" className="primary-button" aria-expanded={open} disabled={busy}
            onClick={() => (open ? setDetail(null) : run(async () => setDetail(await api.getExport(job.id))))}>{open ? 'Ẩn các phần' : 'Các phần tải xuống'}</button>}
          {cancellable.includes(job.status) && <button type="button" className="outline-button" disabled={busy}
            onClick={() => run(async () => { await api.cancelExport(job.id); setReload((n) => n + 1); })}>Hủy lượt xuất</button>}
        </div>
        {open && <ul className="dossier-parts">{detail.parts.map((part) => <li key={part.part_number}>
          <button type="button" className="outline-button" onClick={() => run(() => api.downloadPart(api.partUrl(detail.id, part.part_number)))}>Tải phần {part.part_number} / {detail.parts.length} · {bytes(part.size)}</button>
        </li>)}</ul>}
      </article>;
    })}</div>
    {rows && (page > 0 || rows.length === 20) && <div className="dossier-pager">
      <button type="button" className="outline-button" disabled={page === 0 || loading} onClick={() => setPage(page - 1)}>Trước</button><span>Trang {page + 1}</span>
      <button type="button" className="outline-button" disabled={rows.length < 20 || loading} onClick={() => setPage(page + 1)}>Sau</button>
    </div>}
  </>;
}
