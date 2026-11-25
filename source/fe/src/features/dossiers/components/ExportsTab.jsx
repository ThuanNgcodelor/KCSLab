import { Fragment, useEffect, useState } from 'react';
import { Icon } from '../../../shared/components/Icon';
import { Pagination, emptyPage } from '../../../shared/components/ListToolbar';
import { PageLoader } from '../../../shared/components/Loading';
import { StatusBadge } from '../../../shared/components/StatusBadge';
import * as api from '../api/dossierApi';
import { bytes, count, date, exportErrors, readyStatuses, runningStatuses, scopeLabel } from '../dossierFormat';

const cancellable = ['QUEUED', 'PREPARING', 'PROCESSING'];
function parseScope(text) { try { return JSON.parse(text); } catch { return {}; } }

function note(job) {
  if (job.status === 'FAILED') return exportErrors[job.error_code] || 'Chưa xuất đủ hồ sơ. Kiểm tra ổ lưu, dung lượng rồi tạo lượt mới.';
  if (job.status === 'COMPLETED_WITH_WARNINGS') return `${count(job.warning_count)} file thiếu hoặc sai checksum, xem cột trang_thai trong danh-muc-goi của từng ZIP.`;
  return '';
}

export function ExportsTab() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState(emptyPage);
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
        const next = await api.listExports(page, controller.signal);
        if (controller.signal.aborted) return;
        setData(next); setError('');
        if (!next.content.some((r) => runningStatuses.includes(r.status))) again = 0;
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
    {error && <p className="form-error intake-toast" role="alert">{error}</p>}
    <section className="panel-card intake-list-card">
      <div className="panel-heading"><div><p className="section-kicker">Gói tải giữ 7 ngày</p><h2>Lượt xuất hồ sơ</h2><p className="dossier-note">Tải từng phần rồi giải nén vào cùng một thư mục. Để chọn nơi lưu trên máy, bật “Hỏi nơi lưu mỗi lần tải” trong trình duyệt.</p></div>
        <button type="button" className="outline-button small-button" disabled={loading} onClick={() => setReload((n) => n + 1)}>↻ Làm mới</button></div>
      {loading ? <PageLoader label="Đang tải các lượt xuất…" rows={3} /> : data.content.length === 0
        ? <div className="empty-intake"><span><Icon name="inbox" size={22} /></span><h3>Chưa có lượt xuất</h3><p>Chọn hồ sơ, tháng hoặc năm ở mục Hồ sơ rồi bấm Xuất.</p></div>
        : <div className="table-wrap"><table className="staff-table intake-table" aria-label="Lượt xuất hồ sơ"><thead><tr><th scope="col">Phạm vi</th><th scope="col">Trạng thái</th><th scope="col">Tiến độ</th><th scope="col">Tạo lúc</th><th scope="col">Tải đến</th><th scope="col"><span className="dossier-hidden">Thao tác</span></th></tr></thead><tbody>{data.content.map((job) => {
          const open = detail?.id === job.id; const ready = readyStatuses.includes(job.status); const message = note(job);
          return <Fragment key={job.id}><tr>
            <td data-label="Phạm vi"><span className="intake-cell-content"><strong>{scopeLabel({ ...parseScope(job.scope_json), label: job.receipt_number })}</strong></span></td>
            <td data-label="Trạng thái"><StatusBadge domain="dossierExport" value={job.status} /></td>
            <td data-label="Tiến độ"><span className="intake-cell-content">{job.status === 'PROCESSING' && <progress value={job.processed_files} max={job.total_files || 1} aria-label="Tiến độ đóng gói" />}<strong>{count(job.processed_files)} / {count(job.total_files)} mục</strong><small>{bytes(job.processed_bytes)} / {bytes(job.total_bytes)}</small></span></td>
            <td data-label="Tạo lúc" className="muted-cell">{date(job.created_at)}</td>
            <td data-label="Tải đến" className="muted-cell">{ready ? date(job.expires_at) : '—'}</td>
            <td data-label="Thao tác">{ready ? <button type="button" className="table-action" aria-expanded={open} disabled={busy} onClick={() => (open ? setDetail(null) : run(async () => setDetail(await api.getExport(job.id))))}>{open ? 'Ẩn các phần' : 'Tải xuống'}</button>
              : cancellable.includes(job.status) ? <button type="button" className="table-action" disabled={busy} onClick={() => run(async () => { await api.cancelExport(job.id); setReload((n) => n + 1); })}>Hủy</button> : <span className="dossier-none" aria-hidden="true">—</span>}</td>
          </tr>
          {(open || message) && <tr className="dossier-subrow"><td colSpan={6}>
            {message && <p className={job.status === 'FAILED' ? 'dossier-dialog-error' : 'dossier-note'}>{message}</p>}
            {open && <ul className="dossier-parts">{detail.parts.map((part) => <li key={part.part_number}><button type="button" className="outline-button small-button" onClick={() => run(() => api.downloadPart(api.partUrl(detail.id, part.part_number)))}>Phần {part.part_number}/{detail.parts.length} · {bytes(part.size)}</button></li>)}</ul>}
          </td></tr>}</Fragment>;
        })}</tbody></table></div>}
      <Pagination data={data} loading={loading} noun="lượt xuất" onPage={setPage} />
    </section>
  </>;
}
