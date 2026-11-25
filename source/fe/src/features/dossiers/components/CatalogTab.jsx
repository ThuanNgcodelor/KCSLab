import { useEffect, useMemo, useState } from 'react';
import * as api from '../api/dossierApi';
import { bytes, count, day, scopeLabel } from '../dossierFormat';
import { DossierDetail } from './DossierDetail';
import { ExportPanel } from './ExportPanel';
import { Pager } from './Pager';

const SIZE = 20;

export function CatalogTab({ onExported }) {
  const [periods, setPeriods] = useState(null);
  const [scope, setScope] = useState({ year: null, month: null, q: '' });
  const [text, setText] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [summary, setSummary] = useState(null);
  const [selected, setSelected] = useState(null);
  const [exporting, setExporting] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const controller = new AbortController();
    api.getPeriods(controller.signal).then((list) => {
      setPeriods(list);
      if (list.length) setScope((s) => ({ ...s, year: list[0].year }));
    }).catch((e) => { if (e.name !== 'AbortError') { setError(e.message); setPeriods([]); } });
    return () => controller.abort();
  }, []);

  useEffect(() => {
    if (!periods) return undefined;
    const controller = new AbortController(); setLoading(true); setError('');
    api.listDossiers({ ...scope, page, size: SIZE }, controller.signal).then(setData)
      .catch((e) => { if (e.name !== 'AbortError') setError(e.message); }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [periods, scope, page]);

  useEffect(() => {
    if (!periods) return undefined;
    const controller = new AbortController(); setSummary(null);
    api.getSummary(scope, controller.signal).then(setSummary).catch(() => {});
    return () => controller.abort();
  }, [periods, scope]);

  const years = useMemo(() => {
    const totals = new Map();
    (periods ?? []).forEach((p) => totals.set(p.year, (totals.get(p.year) ?? 0) + p.dossiers));
    return [...totals];
  }, [periods]);
  const months = (periods ?? []).filter((p) => p.year === scope.year);

  function choose(next) { setScope((s) => ({ ...s, ...next })); setPage(0); setSelected(null); setExporting(null); }
  const exportScope = { year: scope.year, month: scope.month, q: scope.q || null };

  return <>
    <section className="panel-card dossier-tree" aria-label="Chọn năm và tháng">
      <div className="dossier-chips" role="group" aria-label="Năm">
        <button type="button" aria-pressed={!scope.year} onClick={() => choose({ year: null, month: null })}>Tất cả năm</button>
        {years.map(([year, total]) => <button type="button" key={year} aria-pressed={scope.year === year} onClick={() => choose({ year, month: null })}>{year}<span>{count(total)}</span></button>)}
      </div>
      {scope.year && <div className="dossier-chips" role="group" aria-label={`Tháng của năm ${scope.year}`}>
        <button type="button" aria-pressed={!scope.month} onClick={() => choose({ month: null })}>Cả năm</button>
        {months.map((p) => <button type="button" key={p.month} aria-pressed={scope.month === p.month} onClick={() => choose({ month: p.month })}>Tháng {String(p.month).padStart(2, '0')}<span>{count(p.dossiers)}</span></button>)}
      </div>}
      <form className="dossier-filters" onSubmit={(e) => { e.preventDefault(); choose({ q: text.trim() }); }}>
        <label className="dossier-search">Số/No.<input value={text} maxLength={120} placeholder="CFC26.0003" onChange={(e) => setText(e.target.value)} /></label>
        <button className="primary-button" disabled={loading}>Tìm hồ sơ</button>
        {scope.q && <button type="button" className="outline-button" onClick={() => { setText(''); choose({ q: '' }); }}>Bỏ tìm kiếm</button>}
      </form>
    </section>
    {error && <p className="form-error" role="alert">{error}</p>}
    {exporting && <ExportPanel scope={exporting} onClose={() => setExporting(null)} onCreated={onExported} />}
    {selected ? <DossierDetail key={selected} id={selected} onBack={() => setSelected(null)} onExport={(label) => setExporting({ receiptId: selected, label })} />
      : <>
        <div className="dossier-toolbar">
          <p><strong>{scopeLabel(exportScope)}</strong>{summary && ` · ${count(summary.dossiers)} hồ sơ · ${count(summary.files)} file · ${bytes(summary.bytes)}`}</p>
          <button type="button" className="outline-button" disabled={loading || !data?.totalElements} onClick={() => setExporting(exportScope)}>Xuất phạm vi đang xem</button>
        </div>
        {loading && <p role="status">Đang tải hồ sơ…</p>}
        {!loading && !error && data?.content.length === 0 && <div className="panel-card dossier-empty">{scope.q ? `Không có hồ sơ khớp “${scope.q}” trong khoảng đã chọn.` : 'Chưa có hồ sơ trong khoảng đã chọn.'}</div>}
        {!loading && data?.content.length > 0 && <div className="dossier-grid">{data.content.map((r) => <article className="panel-card dossier-card" key={r.id}>
          <h2>{r.requestNumber || r.receiptCode}</h2>
          <p>{r.sampleCount} mẫu · {r.fileCount} file · {bytes(r.totalBytes)}</p>
          <small>Tiếp nhận: {day(r.receivedAt)}</small>
          <button type="button" className="outline-button" aria-label={`Xem hồ sơ ${r.requestNumber || r.receiptCode}`} onClick={() => { setSelected(r.id); setExporting(null); }}>Xem hồ sơ</button>
        </article>)}</div>}
        {data && <Pager page={page} size={SIZE} total={data.totalElements} onChange={setPage} disabled={loading} />}
      </>}
  </>;
}
