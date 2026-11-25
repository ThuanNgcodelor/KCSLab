import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import * as api from '../api/dossierApi';
import { bytes, categories, date } from '../dossierFormat';
import { Pager } from './Pager';

export function DossierDetail({ id, onBack, onExport }) {
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const controller = new AbortController(); setLoading(true); setError('');
    api.getDossier(id, page, controller.signal).then(setData).catch((e) => { if (e.name !== 'AbortError') setError(e.message); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [id, page]);

  async function download(file) {
    setError('');
    try { await api.downloadFile(api.fileUrl(id, file), file.originalName); } catch (e) { setError(e.message); }
  }

  const name = data?.requestNumber || data?.receiptCode || '';
  return <section className="panel-card dossier-detail" aria-label="Chi tiết hồ sơ">
    <button type="button" className="outline-button" onClick={onBack}>← Danh sách hồ sơ</button>
    <div className="dossier-toolbar">
      <div><h2>{name || 'Đang tải…'}</h2>{data && <p className="dossier-path">{data.folder}</p>}</div>
      <div className="dossier-actions">
        <Link className="dossier-link" to={`/admin/intake/${id}`}>Mở phiếu tiếp nhận</Link>
        <button type="button" className="primary-button" disabled={!data} onClick={() => onExport(name)}>Xuất hồ sơ này</button>
      </div>
    </div>
    {error && <p className="form-error" role="alert">{error}</p>}
    {loading && <p role="status">Đang tải tài liệu…</p>}
    {data && !loading && <>
      {!data.files.content.length && <p className="dossier-empty">Hồ sơ chưa có file đính kèm. Dữ liệu phiếu vẫn được đưa vào bản xuất.</p>}
      <div className="dossier-file-grid">{data.files.content.map((f) => <article className="dossier-file" key={`${f.source}-${f.id}`}>
        {f.contentType?.startsWith('image/') && <img src={api.previewUrl(id, f)} loading="lazy" alt={`${categories[f.category] || 'Ảnh'}: ${f.originalName}`}
          onError={(e) => { e.currentTarget.hidden = true; }} />}
        <strong>{f.originalName}</strong>
        <span>{categories[f.category] || f.category}{f.sampleId ? ` · mẫu ${f.sampleId}` : ''}{f.version ? ` · phiên bản ${f.version}` : ''}</span>
        {f.historical && <span className="dossier-badge">Lịch sử / chưa phát hành</span>}
        <small>{bytes(f.size)} · {date(f.uploadedAt)}{f.uploadedBy ? ` · ${f.uploadedBy}` : ''}</small>
        <details><summary>Checksum SHA-256</summary><code>{f.checksum}</code></details>
        <button type="button" className="outline-button" onClick={() => download(f)}>Tải file</button>
      </article>)}</div>
      <Pager page={page} size={50} total={data.files.totalElements} onChange={setPage} />
    </>}
  </section>;
}
