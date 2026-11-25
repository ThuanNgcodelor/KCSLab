import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Icon } from '../../../shared/components/Icon';
import { ListToolbar, Pagination, emptyPage } from '../../../shared/components/ListToolbar';
import { PageLoader } from '../../../shared/components/Loading';
import * as api from '../api/dossierApi';
import { bytes, count, day, scopeLabel } from '../dossierFormat';
import { ExportDialog } from './ExportDialog';

const sortOptions = [
  ['receivedAt,desc', 'Ngày nhận: mới nhất'], ['receivedAt,asc', 'Ngày nhận: cũ nhất'],
  ['requestNumber,desc', 'Số/No.: mới nhất'], ['requestNumber,asc', 'Số/No.: cũ nhất'],
];
const ALL = 'all';

/** The filter lives in the URL, so going back from a dossier returns to the same year, month and page. */
export function CatalogTab({ periods, storage, onExported }) {
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const [data, setData] = useState(emptyPage);
  const [summary, setSummary] = useState(null);
  const [exporting, setExporting] = useState(false);
  const [reload, setReload] = useState(0);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const year = params.get('year') === ALL ? null : Number(params.get('year')) || periods?.[0]?.year || null;
  const month = year ? Number(params.get('month')) || null : null;
  const q = params.get('q') ?? '';
  const sort = params.get('sort') ?? 'receivedAt,desc';
  const page = Number(params.get('page')) || 0;
  const ready = periods != null;

  function update(patch) {
    setParams((current) => {
      const next = new URLSearchParams(current);
      Object.entries({ page: null, ...patch }).forEach(([key, value]) => { if (value) next.set(key, value); else next.delete(key); });
      return next;
    }, { replace: true });
  }

  useEffect(() => {
    if (!ready) return undefined;
    const controller = new AbortController(); setLoading(true); setError('');
    api.listDossiers({ year, month, q, sort, page, size: 20 }, controller.signal).then(setData)
      .catch((e) => { if (e.name !== 'AbortError') setError(e.message); }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [ready, year, month, q, sort, page, reload]);

  useEffect(() => {
    if (!ready) return undefined;
    const controller = new AbortController(); setSummary(null);
    api.getSummary({ year, month, q }, controller.signal).then(setSummary).catch(() => {});
    return () => controller.abort();
  }, [ready, year, month, q, reload]);

  const years = [...(periods ?? []).reduce((totals, p) => totals.set(p.year, (totals.get(p.year) ?? 0) + p.dossiers), new Map())];
  const months = (periods ?? []).filter((p) => p.year === year);
  const scope = { year, month, q };
  const filtered = Boolean(q || month);
  const receiving = storage?.locations.find((h) => (storage.settings.locationId == null ? h.location.legacyKey === 'RECEIPTS' : h.location.id === storage.settings.locationId));

  return <>
    <section className="intake-command-strip" aria-label="Tổng quan hồ sơ lưu trữ">
      <article><span>Hồ sơ</span><strong>{summary ? count(summary.dossiers) : '…'}</strong><small>{scopeLabel(scope)}</small></article>
      <article><span>File đã lưu</span><strong>{summary ? count(summary.files) : '…'}</strong><small>Gồm cả bản cũ còn lưu</small></article>
      <article><span>Dung lượng</span><strong>{summary ? bytes(summary.bytes) : '…'}</strong><small>Trong phạm vi đang xem</small></article>
      <article><span>Ổ nhận upload còn trống</span><strong>{receiving?.totalBytes ? bytes(receiving.freeBytes) : '—'}</strong><small>{receiving?.location.label ?? 'Đang kiểm tra'}</small></article>
    </section>

    {months.length > 0 && <nav className="status-chips" aria-label={`Lọc theo tháng của năm ${year}`}>
      <button type="button" className={`status-chip ${month ? '' : 'active'}`} aria-pressed={!month} onClick={() => update({ month: null })}>Cả năm {year}</button>
      {months.map((p) => <button type="button" key={p.month} className={`status-chip ${month === p.month ? 'active' : ''}`} aria-pressed={month === p.month} onClick={() => update({ month: p.month })}>Tháng {String(p.month).padStart(2, '0')} ({count(p.dossiers)})</button>)}
    </nav>}

    {error && <p className="form-error intake-toast" role="alert">{error}</p>}
    <section className="panel-card intake-list-card">
      <div className="panel-heading intake-list-heading"><div><p className="section-kicker">Theo ngày tiếp nhận</p><h2>Danh sách hồ sơ</h2></div>
        <div className="intake-list-tools">
          <button type="button" className="outline-button small-button" onClick={() => setReload((n) => n + 1)}>↻ Làm mới</button>
          <button type="button" className="outline-button small-button" disabled={loading || data.totalElements === 0} onClick={() => setExporting(true)}>Xuất {data.totalElements > 0 ? `${count(data.totalElements)} hồ sơ` : 'hồ sơ'}</button>
        </div></div>
      <ListToolbar q={q} onQ={(value) => update({ q: value })} searchLabel="Tìm hồ sơ" placeholder="Số/No., ví dụ CFC26.0003"
        filters={[{ label: 'Năm tiếp nhận', value: year ? String(year) : ALL, onChange: (value) => update({ year: value, month: null }), options: [...years.map(([value, total]) => [String(value), `${value} (${count(total)} hồ sơ)`]), [ALL, 'Tất cả các năm']] }]}
        sort={sort} sortOptions={sortOptions} onSort={(value) => update({ sort: value })} onReset={() => setParams({}, { replace: true })} />
      {loading ? <PageLoader label="Đang tải hồ sơ…" rows={4} /> : data.content.length === 0
        ? <div className="empty-intake"><span><Icon name="file" size={22} /></span>{filtered ? <><h3>Không có hồ sơ phù hợp</h3><p>Thử đổi hoặc xóa bộ lọc.</p></> : <><h3>Chưa có hồ sơ nào</h3><p>Hồ sơ xuất hiện ở đây sau khi tiếp nhận mẫu.</p></>}</div>
        : <div className="table-wrap"><table className="staff-table intake-table" aria-label="Danh sách hồ sơ lưu trữ"><thead><tr><th scope="col">Số/No · khách hàng</th><th scope="col">Mẫu</th><th scope="col">File</th><th scope="col">Ngày nhận</th><th scope="col"><span className="dossier-hidden">Thao tác</span></th></tr></thead><tbody>{data.content.map((r) => <tr key={r.id}>
          <td data-label="Hồ sơ"><span className="intake-cell-content"><strong className="receipt-code">{r.requestNumber || r.receiptCode}</strong><small>{r.customerName}</small></span></td>
          <td data-label="Mẫu"><span className="intake-cell-content"><strong>{r.sampleCount} mẫu · {r.sampleName}</strong><small>Phiếu {r.receiptCode}</small></span></td>
          <td data-label="File"><span className="intake-cell-content"><strong>{count(r.fileCount)} file</strong><small>{bytes(r.totalBytes)}</small></span></td>
          <td data-label="Ngày nhận" className="muted-cell">{day(r.receivedAt)}</td>
          <td data-label="Thao tác"><button type="button" className="table-action" aria-label={`Xem hồ sơ ${r.requestNumber || r.receiptCode}`} onClick={() => navigate(`/admin/dossiers/${r.id}`, { state: { back: `/admin/dossiers?${params}` } })}>Xem hồ sơ</button></td>
        </tr>)}</tbody></table></div>}
      <Pagination data={data} loading={loading} noun="hồ sơ" onPage={(value) => update({ page: value })} />
    </section>
    {exporting && <ExportDialog scope={scope} onClose={() => setExporting(false)} onCreated={onExported} />}
  </>;
}
