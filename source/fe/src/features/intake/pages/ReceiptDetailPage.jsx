import { ParameterList } from '../../../shared/components/ParameterRows';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getReceipt, uploadReceiptFile } from '../api/intakeApi';
import { PhotoPicker } from '../../../shared/components/PhotoPicker';
import { PageLoader } from '../../../shared/components/Loading';
import { SampleCodingPanel } from '../components/SampleCodingPanel';
import { CustomerNotificationHistory } from '../components/CustomerNotificationHistory';

const categoryLabels = {
  FORM_3: 'Phiếu tiếp nhận số 3', LOT_PHOTO: 'Ảnh lô hàng', SAMPLE_PHOTO: 'Ảnh mẫu',
  PACKAGING_PHOTO: 'Ảnh bao bì', SEAL_PHOTO: 'Ảnh niêm phong', DAMAGE_PHOTO: 'Ảnh hư hỏng',
  LABEL_PHOTO: 'Ảnh nhãn mẫu', TRANSFER_EVIDENCE: 'Bằng chứng bàn giao', OTHER: 'Tài liệu khác',
};

export function ReceiptDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [receipt, setReceipt] = useState(null);
  const [category, setCategory] = useState('SAMPLE_PHOTO');
  const [description, setDescription] = useState('');
  const [selected, setSelected] = useState([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => { getReceipt(id).then(setReceipt).catch((exception) => setError(exception.message)); }, [id]);

  async function handleUpload(event) {
    event.preventDefault();
    if (selected.length === 0) return;
    setSaving(true); setError('');
    const failed = [];
    for (const file of selected) {
      try {
        const attachment = await uploadReceiptFile(id, file, {
          category, description,
          capturedAt: file.lastModified ? new Date(file.lastModified).toISOString() : null,
        });
        setReceipt((current) => ({ ...current, attachments: [...current.attachments, attachment] }));
      } catch (exception) { failed.push(`${file.name}: ${exception.message}`); }
    }
    setSelected(failed.length ? selected.filter((file) => failed.some((entry) => entry.startsWith(`${file.name}:`))) : []);
    if (!failed.length) setDescription('');
    if (failed.length) setError(`Chưa tải lên được: ${failed.join('; ')}`);
    setSaving(false);
  }

  if (!receipt) return <section className="staff-page">{error ? <p className="form-error" role="alert">{error}</p> : <PageLoader label="Đang tải hồ sơ tiếp nhận…" rows={4} />}</section>;
  const photoCategory = category !== 'FORM_3' && category !== 'OTHER';

  return <section className="staff-page receipt-detail-page">
    <header className="admin-heading staff-heading"><div><button className="back-link" onClick={() => navigate('/admin/intake/receipts')}>← Danh sách tiếp nhận</button><p className="section-kicker">Hồ sơ tiếp nhận / {receipt.requestNumber || receipt.receiptCode}</p><h1>{receipt.sampleName}</h1><p className="heading-copy">Tiếp nhận bởi {receipt.receivedByName} · {formatDateTime(receipt.receivedAt)}</p></div></header>
    {error && <p className="form-error intake-toast" role="alert">{error}</p>}
    <div className="receipt-detail-grid">
      <main className="receipt-form-main">
        <CustomerNotificationHistory receiptId={id} />
        <section className="panel-card intake-form-card"><DetailHeading number="I" en="Source & chain of custody" title="Nguồn và chuỗi tiếp nhận" />
          <div className="receipt-source-grid">
            <Detail label="Khách hàng" value={`${receipt.customerName} · ${receipt.customerPhone}`} />
            <Detail label="Người giao" value={receipt.deliveredBy} />
            <Detail label="Người nhận mẫu" value={receipt.sampledBy} />
            <Detail label="Thời điểm lấy" value={formatDateTime(receipt.sampledAt)} />
            <Detail label="Mã vùng trồng" value={receipt.growingArea} />
            <Detail label="Cơ sở đóng gói" value={receipt.packingFacility} />
            <Detail label="Xe / moóc" value={[receipt.vehicleNumber, receipt.trailerNumber].filter(Boolean).join(' / ')} />
            <Detail label="Container / seal container" value={[receipt.containerNumber, receipt.containerSealNumber].filter(Boolean).join(' / ')} />
            <Detail label="Niêm phong mẫu" value={receipt.sealNumber} />
          </div>
        </section>
        <section className="panel-card intake-form-card"><DetailHeading number="II" en="Samples" title={`Danh sách mẫu (${receipt.samples.length})`} />
          <div className="receipt-sample-list">{receipt.samples.map((sample) => <article className="receipt-sample-item" key={sample.id}><div><strong>{sample.sampleCode}</strong><small>{sample.sourceCode} · {sample.productType}</small></div><div><strong>{sample.name}</strong><small>{sample.quantity || 'Chưa ghi lượng'} · {sample.packaging || 'Chưa ghi bao bì'}</small></div><div><strong>{sample.sampleCondition || 'Chưa ghi tình trạng'}</strong><ParameterList value={sample.requestedParameters} /></div><SampleCodingPanel sample={sample} onChanged={() => getReceipt(id).then(setReceipt).catch(() => {})} /></article>)}</div>
        </section>
        <section className="panel-card intake-form-card"><DetailHeading number="III" en="Photos & documents" title={`Ảnh và hồ sơ (${receipt.attachments.length})`} />
          {receipt.attachments.length === 0 ? <p className="muted">Chưa có ảnh hoặc file đính kèm.</p> : <div className="attachment-list">{receipt.attachments.map((attachment) => <a className="attachment-item" href={attachment.contentUrl} target="_blank" rel="noreferrer" key={attachment.id}>{attachment.contentType.startsWith('image/') ? <img className="attachment-thumb" src={attachment.contentUrl} alt="" loading="lazy" /> : <span>▤</span>}<div><strong>{categoryLabels[attachment.category] ?? attachment.category}</strong><small>{attachment.originalName} · {formatSize(attachment.size)}</small><small>{attachment.uploadedByName || 'Không rõ người tải'} · {formatDateTime(attachment.capturedAt || attachment.uploadedAt)}</small></div></a>)}</div>}
        </section>
      </main>
      <aside className="receipt-form-aside"><form className="panel-card intake-form-card" onSubmit={handleUpload}><DetailHeading number="IV" en="Add evidence" title="Bổ sung bằng chứng" />
        <label>Loại hồ sơ<select value={category} onChange={(event) => setCategory(event.target.value)}>{Object.entries(categoryLabels).map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select></label>
        <label>Mô tả<input value={description} onChange={(event) => setDescription(event.target.value)} placeholder="Góc chụp, tình trạng…" /></label>
        <PhotoPicker label="Ảnh / file" hint={photoCategory ? 'Chụp trực tiếp hoặc chọn từ máy' : 'PDF, DOCX hoặc ảnh'} camera={photoCategory} accept={photoCategory ? 'image/*' : 'application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,image/*'} files={selected} onChange={setSelected} />
        <button className="primary-button" type="submit" disabled={selected.length === 0 || saving}>{saving ? 'Đang tải lên…' : `Lưu ${selected.length || ''} file vào hồ sơ`}</button>
      </form></aside>
    </div>
  </section>;
}

function DetailHeading({ number, title, en }) { return <div className="form-card-heading"><span className="form-number">{number}</span><div><h2>{title}{en && <i className="heading-en">{en}</i>}</h2></div></div>; }
function Detail({ label, value }) { return <span><small>{label}</small><strong>{value || '—'}</strong></span>; }
function formatDateTime(value) { return value ? new Date(value).toLocaleString('vi-VN') : '—'; }
function formatSize(value) { return value < 1024 * 1024 ? `${Math.ceil(value / 1024)} KB` : `${(value / 1024 / 1024).toFixed(1)} MB`; }
