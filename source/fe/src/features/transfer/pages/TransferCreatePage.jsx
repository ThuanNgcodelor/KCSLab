import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listReceipts } from '../../intake/api/intakeApi';
import { createTransfer } from '../api/transferApi';

const today = new Date().toISOString().slice(0, 10);
const dueDefault = new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 10);

export function TransferCreatePage() {
  const navigate = useNavigate();
  const [receipts, setReceipts] = useState([]);
  const [receiptId, setReceiptId] = useState('');
  const [items, setItems] = useState({});
  const [form, setForm] = useState({ transferDate: today, resultsDueDate: dueDefault, returnToCustomer: false, customerReturnDate: '', changesNote: '' });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    listReceipts().then((data) => {
      const eligible = data.filter((receipt) => receipt.status !== 'REJECTED' && receipt.samples?.some((sample) => sample.status === 'RECEIVED'));
      setReceipts(eligible);
      if (eligible[0]) setReceiptId(String(eligible[0].id));
    }).catch((exception) => setError(exception.message)).finally(() => setLoading(false));
  }, []);

  const receipt = useMemo(() => receipts.find((entry) => String(entry.id) === receiptId), [receipts, receiptId]);
  const availableSamples = useMemo(() => receipt?.samples?.filter((sample) => sample.status === 'RECEIVED') ?? [], [receipt]);

  useEffect(() => {
    setItems(Object.fromEntries(availableSamples.map((sample) => [sample.id, {
      selected: true,
      weight: sample.quantity ?? '',
      quantity: sample.quantity ?? '',
      sampleCondition: sample.sampleCondition ?? 'Nguyên vẹn',
      requestedParameters: sample.requestedParameters ?? '',
      testMethod: '',
    }])));
  }, [receiptId, availableSamples.length]);

  const updateForm = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.type === 'checkbox' ? event.target.checked : event.target.value }));
  const updateItem = (id, field) => (event) => setItems((current) => ({ ...current, [id]: { ...current[id], [field]: event.target.type === 'checkbox' ? event.target.checked : event.target.value } }));

  async function handleSubmit(event) {
    event.preventDefault();
    const selected = availableSamples.filter((sample) => items[sample.id]?.selected);
    if (selected.length === 0) { setError('Chọn ít nhất một mẫu để chuyển.'); return; }
    setSaving(true); setError('');
    try {
      const transfer = await createTransfer({
        receiptId: Number(receiptId), transferDate: form.transferDate, resultsDueDate: form.resultsDueDate,
        returnToCustomer: form.returnToCustomer, customerReturnDate: form.returnToCustomer ? form.customerReturnDate : null,
        changesNote: form.changesNote,
        items: selected.map((sample) => ({ sampleId: sample.id, ...items[sample.id], selected: undefined })),
      });
      navigate(`/admin/transfers/${transfer.id}`, { replace: true, state: { message: `Đã lập phiếu ${transfer.transferCode}` } });
    } catch (exception) { setError(exception.message); }
    finally { setSaving(false); }
  }

  if (loading) return <section className="staff-page"><p className="muted">Đang tải hồ sơ tiếp nhận…</p></section>;

  return <section className="staff-page transfer-form-page">
    <header className="admin-heading staff-heading"><div><button className="back-link" onClick={() => navigate('/admin/transfers')}>← Quay lại danh sách</button><p className="section-kicker">Phiếu số 4</p><h1>Lập phiếu chuyển mẫu</h1><p className="heading-copy">Bàn giao mẫu và yêu cầu thử nghiệm cho KCS/phòng phân tích.</p></div></header>
    {error && <p className="form-error form-page-error" role="alert">{error}</p>}
    {receipts.length === 0 ? <section className="panel-card empty-intake"><span>!</span><h3>Không có mẫu sẵn sàng chuyển</h3><p>Cần có phiếu tiếp nhận hợp lệ và mẫu chưa được đưa vào phiếu chuyển.</p><button className="outline-button" onClick={() => navigate('/admin/intake')}>Xem phiếu tiếp nhận</button></section> : <form className="transfer-form-layout" onSubmit={handleSubmit}>
      <div className="receipt-form-main">
        <section className="panel-card intake-form-card"><FormHeading number="01" title="Phiếu yêu cầu" copy="Chọn hồ sơ tiếp nhận cần bàn giao." /><label>Phiếu tiếp nhận<select value={receiptId} onChange={(event) => setReceiptId(event.target.value)}>{receipts.map((entry) => <option key={entry.id} value={entry.id}>{entry.receiptCode} — {entry.customerName}</option>)}</select></label>{receipt && <div className="transfer-receipt-summary"><span><small>Người giao mẫu</small><strong>{receipt.deliveredBy}</strong></span><span><small>Ngày nhận</small><strong>{formatDate(receipt.receivedAt)}</strong></span><span><small>Chỉ tiêu</small><strong>{receipt.requestedParameters || 'Chưa ghi'}</strong></span></div>}</section>
        <section className="panel-card intake-form-card"><FormHeading number="02" title="Danh sách mẫu" copy="Thông tin được chụp lại tại thời điểm lập phiếu." /><div className="transfer-sample-list">{availableSamples.map((sample) => <article className={`transfer-sample-card ${items[sample.id]?.selected ? 'selected' : ''}`} key={sample.id}><label className="sample-select"><input type="checkbox" checked={items[sample.id]?.selected ?? false} onChange={updateItem(sample.id, 'selected')} /><span><strong>{sample.sampleCode}</strong><small>{sample.name}</small></span></label>{items[sample.id]?.selected && <div className="form-two-columns sample-transfer-fields"><label>Khối lượng<input value={items[sample.id]?.weight ?? ''} onChange={updateItem(sample.id, 'weight')} placeholder="10 kg" /></label><label>Số lượng<input value={items[sample.id]?.quantity ?? ''} onChange={updateItem(sample.id, 'quantity')} placeholder="02 mẫu" /></label><label>Tình trạng<input value={items[sample.id]?.sampleCondition ?? ''} onChange={updateItem(sample.id, 'sampleCondition')} /></label><label>Phương pháp thử<input value={items[sample.id]?.testMethod ?? ''} onChange={updateItem(sample.id, 'testMethod')} placeholder="CFC-TN-..." required /></label><label className="full-span">Chỉ tiêu yêu cầu<input value={items[sample.id]?.requestedParameters ?? ''} onChange={updateItem(sample.id, 'requestedParameters')} required /></label></div>}</article>)}</div></section>
      </div>
      <aside className="receipt-form-aside transfer-form-aside">
        <section className="panel-card intake-form-card"><FormHeading number="03" title="Thời hạn và lưu mẫu" copy="Theo mục III của phiếu chuyển mẫu." /><label>Ngày chuyển mẫu<input type="date" value={form.transferDate} onChange={updateForm('transferDate')} required /></label><label>Hạn trả kết quả<input type="date" min={form.transferDate} value={form.resultsDueDate} onChange={updateForm('resultsDueDate')} required /></label><label className="switch-label"><input type="checkbox" checked={form.returnToCustomer} onChange={updateForm('returnToCustomer')} /><span className="fake-switch" />Lưu và trả mẫu cho khách</label>{form.returnToCustomer && <label>Ngày trả/lưu đến<input type="date" min={form.transferDate} value={form.customerReturnDate} onChange={updateForm('customerReturnDate')} required /></label>}<label>Thay đổi / ghi chú<textarea value={form.changesNote} onChange={updateForm('changesNote')} rows="4" placeholder="Ghi các thay đổi so với phiếu yêu cầu" /></label></section>
        <div className="form-submit-panel"><p><strong>Chuẩn bị bàn giao</strong><span>Phiếu sẽ ở trạng thái chờ phòng phân tích xác nhận.</span></p><button className="primary-button" type="submit" disabled={saving}>{saving ? 'Đang lập phiếu…' : 'Lập phiếu chuyển mẫu'}</button></div>
      </aside>
    </form>}
  </section>;
}

function FormHeading({ number, title, copy }) { return <div className="form-card-heading"><span className="form-number">{number}</span><div><h2>{title}</h2><p>{copy}</p></div></div>; }
function formatDate(value) { return value ? new Date(value).toLocaleDateString('vi-VN') : '—'; }
