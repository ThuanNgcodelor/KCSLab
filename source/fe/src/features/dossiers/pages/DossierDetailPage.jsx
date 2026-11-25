import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { Icon } from '../../../shared/components/Icon';
import { Pagination } from '../../../shared/components/ListToolbar';
import { PageLoader } from '../../../shared/components/Loading';
import * as api from '../api/dossierApi';
import { ExportDialog } from '../components/ExportDialog';
import { bytes, categories, count, date } from '../dossierFormat';
import './dossiers.css';

const groups = [
  ['Tiếp nhận', (f) => !f.historical && f.source === 'RECEIPT' && f.category !== 'TRANSFER_EVIDENCE'],
  ['Bàn giao', (f) => !f.historical && f.category === 'TRANSFER_EVIDENCE'],
  ['Kết quả đang phát hành', (f) => !f.historical && f.source === 'RESULT'],
  ['Bản nháp và lịch sử', (f) => f.historical],
];

export function DossierDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { state } = useLocation();
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);

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

  const back = <button type="button" className="back-link" onClick={() => navigate(state?.back ?? '/admin/dossiers')}>← Danh sách hồ sơ</button>;
  if (!data) return <section className="staff-page dossiers-page">{back}{error ? <p className="form-error intake-toast" role="alert">{error}</p> : <PageLoader label="Đang tải hồ sơ…" rows={4} />}</section>;

  const name = data.requestNumber || data.receiptCode;
  const shown = groups.map(([title, match]) => [title, data.files.content.filter(match)]).filter(([, files]) => files.length > 0);
  return <section className="staff-page dossiers-page">
    <header className="admin-heading staff-heading"><div>{back}<p className="section-kicker">Hồ sơ lưu trữ / {data.folder}</p><h1>{name}</h1><p className="heading-copy">{data.customerName} · {data.sampleName} · tiếp nhận {date(data.receivedAt)}</p></div>
      <div className="dossier-actions"><Link className="outline-button dossier-link" to={`/admin/intake/${id}`}>Mở phiếu tiếp nhận</Link><button type="button" className="primary-button" onClick={() => setExporting(true)}>Xuất hồ sơ này</button></div></header>
    {error && <p className="form-error intake-toast" role="alert">{error}</p>}
    {loading ? <PageLoader label="Đang tải tài liệu…" rows={4} /> : shown.length === 0
      ? <section className="panel-card"><div className="empty-intake"><span><Icon name="file" size={22} /></span><h3>Hồ sơ chưa có file đính kèm</h3><p>Dữ liệu phiếu vẫn được đưa vào bản xuất.</p></div></section>
      : shown.map(([title, files], index) => <section className="panel-card intake-form-card dossier-group" key={title}>
        <div className="form-card-heading"><span className="form-number">{['I', 'II', 'III', 'IV'][index]}</span><div><h2>{title} ({count(files.length)})</h2></div></div>
        <div className="attachment-list">{files.map((f) => <article className="attachment-item dossier-file" key={`${f.source}-${f.id}`}>
          {f.contentType?.startsWith('image/')
            ? <a href={api.previewUrl(id, f)} target="_blank" rel="noreferrer" aria-label={`Xem ảnh ${f.originalName}`}><img className="attachment-thumb" src={api.previewUrl(id, f)} alt="" loading="lazy" /></a>
            : <span><Icon name="file" size={18} /></span>}
          <div><strong>{categories[f.category] || f.category}{f.version ? ` · phiên bản ${f.version}` : ''}{f.sampleId ? ` · mẫu ${f.sampleId}` : ''}</strong>
            <small>{f.originalName} · {bytes(f.size)}</small><small>{f.uploadedBy || 'Không rõ người tải'} · {date(f.uploadedAt)}</small>
            <small className="dossier-checksum" title="SHA-256 ghi lúc tải lên">SHA-256: {f.checksum}</small></div>
          <button type="button" className="table-action" aria-label={`Tải ${f.originalName}`} onClick={() => download(f)}>Tải file</button>
        </article>)}</div>
      </section>)}
    {data.files.totalPages > 1 && <Pagination data={data.files} loading={loading} noun="file" onPage={setPage} />}
    {exporting && <ExportDialog scope={{ receiptId: Number(id), label: name }} onClose={() => setExporting(false)} onCreated={() => navigate('/admin/dossiers?tab=exports')} />}
  </section>;
}
