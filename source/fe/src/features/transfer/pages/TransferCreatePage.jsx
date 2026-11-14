import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { listReceipts } from '../../intake/api/intakeApi';
import { createTransfer, getTransfer, updateTransfer } from '../api/transferApi';

const today = businessDate();
const dueDefault = businessDate(7);

export function TransferCreatePage() {
  const { id } = useParams();
  const editing = Boolean(id);
  const navigate = useNavigate();
  const [receipts, setReceipts] = useState([]);
  const [receiptId, setReceiptId] = useState('');
  const [items, setItems] = useState({});
  const [form, setForm] = useState({ receivingDepartment: 'KCS', transferDate: today, resultsDueDate: dueDefault, returnToCustomer: false, customerReturnDate: '', changesNote: '' });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([
      listReceipts({ page: 0, size: 100, sort: 'receivedAt,desc' }),
      editing ? getTransfer(id) : Promise.resolve(null),
    ]).then(([receiptPage, draft]) => {
      if (draft && draft.status !== 'DRAFT') throw new Error('Chỉ được sửa phiếu chuyển mẫu đang ở bản nháp.');
      const data = receiptPage.content;
      const eligible = data.filter((entry) => entry.status !== 'REJECTED'
        && (entry.samples?.some((sample) => sample.status === 'RECEIVED') || entry.id === draft?.receiptId));
      setReceipts(eligible);
      const nextReceiptId = draft ? String(draft.receiptId) : String(eligible[0]?.id ?? '');
      const nextReceipt = eligible.find((entry) => String(entry.id) === nextReceiptId);
      setReceiptId(nextReceiptId);
      setItems(buildItems(nextReceipt?.samples ?? [], draft?.items ?? []));
      if (draft) setForm({
        receivingDepartment: draft.receivingDepartment,
        transferDate: draft.transferDate,
        resultsDueDate: draft.resultsDueDate,
        returnToCustomer: draft.returnToCustomer,
        customerReturnDate: draft.customerReturnDate ?? '',
        changesNote: draft.changesNote ?? '',
      });
    }).catch((exception) => setError(exception.message)).finally(() => setLoading(false));
  }, [editing, id]);

  const receipt = useMemo(() => receipts.find((entry) => String(entry.id) === receiptId), [receipts, receiptId]);
  const availableSamples = useMemo(() => receipt?.samples?.filter((sample) => sample.status === 'RECEIVED') ?? [], [receipt]);

  const updateForm = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.type === 'checkbox' ? event.target.checked : event.target.value }));
  const updateItem = (id, field) => (event) => setItems((current) => ({ ...current, [id]: { ...current[id], [field]: event.target.type === 'checkbox' ? event.target.checked : event.target.value } }));
  const changeReceipt = (event) => {
    const nextId = event.target.value;
    const nextReceipt = receipts.find((entry) => String(entry.id) === nextId);
    setReceiptId(nextId);
    setItems(buildItems(nextReceipt?.samples ?? []));
  };

  async function handleSubmit(event) {
    event.preventDefault();
    const selected = availableSamples.filter((sample) => items[sample.id]?.selected);
    if (selected.length === 0) { setError('Chọn ít nhất một mẫu để chuyển.'); return; }
    setSaving(true); setError('');
    try {
      const payload = {
        receiptId: Number(receiptId), ...form,
        customerReturnDate: form.returnToCustomer ? form.customerReturnDate : null,
        items: selected.map((sample) => {
          const { selected: _selected, ...item } = items[sample.id];
          return { sampleId: sample.id, ...item };
        }),
      };
      const transfer = editing ? await updateTransfer(id, payload) : await createTransfer(payload);
      navigate(`/admin/transfers/${transfer.id}`, { replace: true, state: { message: `${editing ? 'Đã cập nhật' : 'Đã lưu'} bản nháp ${transfer.transferCode}` } });
    } catch (exception) { setError(exception.message); }
    finally { setSaving(false); }
  }

  if (loading) return <section className="staff-page"><p className="muted">Đang tải hồ sơ tiếp nhận…</p></section>;

  return <section className="staff-page transfer-form-page">
    <header className="admin-heading staff-heading"><div><button className="back-link" onClick={() => navigate(editing ? `/admin/transfers/${id}` : '/admin/transfers')}>← {editing ? 'Quay lại phiếu chuyển' : 'Quay lại danh sách'}</button><p className="section-kicker">Phiếu số 4</p><h1>{editing ? 'Sửa bản nháp chuyển mẫu' : 'Lập phiếu chuyển mẫu'}</h1><p className="heading-copy">Chuẩn bị hồ sơ bàn giao mẫu cho KCS/phòng phân tích; bản nháp chưa khóa mẫu.</p></div></header>
    {error && <p className="form-error form-page-error" role="alert">{error}</p>}
    {receipts.length === 0 ? <section className="panel-card empty-intake"><span>!</span><h3>Không có mẫu sẵn sàng chuyển</h3><p>Cần có phiếu tiếp nhận hợp lệ và mẫu chưa nằm trong phiếu chuyển hiệu lực.</p><button className="outline-button" onClick={() => navigate('/admin/intake')}>Xem phiếu tiếp nhận</button></section> : <form className="transfer-form-layout" onSubmit={handleSubmit}>
      <div className="receipt-form-main">
        <section className="panel-card intake-form-card"><FormHeading number="01" title="Phiếu yêu cầu" copy={editing ? 'Phiếu tiếp nhận được khóa khi tạo bản nháp.' : 'Chọn hồ sơ tiếp nhận cần bàn giao.'} /><label>Phiếu tiếp nhận<select value={receiptId} onChange={changeReceipt} disabled={editing}>{receipts.map((entry) => <option key={entry.id} value={entry.id}>{entry.receiptCode} — {entry.customerName}</option>)}</select></label>{receipt && <div className="transfer-receipt-summary"><span><small>Người tiếp nhận</small><strong>{receipt.receivedByName}</strong></span><span><small>Ngày nhận</small><strong>{formatDate(receipt.receivedAt)}</strong></span><span><small>Số mẫu sẵn sàng</small><strong>{availableSamples.length}</strong></span></div>}</section>
        <section className="panel-card intake-form-card"><FormHeading number="02" title="Danh sách mẫu" copy="Thông tin được chụp lại tại thời điểm lập Phiếu số 4." /><div className="transfer-sample-list">{availableSamples.map((sample) => <article className={`transfer-sample-card ${items[sample.id]?.selected ? 'selected' : ''}`} key={sample.id}><label className="sample-select"><input type="checkbox" checked={items[sample.id]?.selected ?? false} onChange={updateItem(sample.id, 'selected')} /><span><strong>{sample.sampleCode}</strong><small>{sample.name}</small></span></label>{items[sample.id]?.selected && <div className="form-two-columns sample-transfer-fields"><label>Khối lượng<input value={items[sample.id]?.weight ?? ''} onChange={updateItem(sample.id, 'weight')} /></label><label>Số lượng<input value={items[sample.id]?.quantity ?? ''} onChange={updateItem(sample.id, 'quantity')} /></label><label>Tình trạng<input value={items[sample.id]?.sampleCondition ?? ''} onChange={updateItem(sample.id, 'sampleCondition')} /></label><label>Phương pháp thử<input value={items[sample.id]?.testMethod ?? ''} onChange={updateItem(sample.id, 'testMethod')} placeholder="CFC-TN-…" required /></label><label className="full-span">Chỉ tiêu yêu cầu<input value={items[sample.id]?.requestedParameters ?? ''} onChange={updateItem(sample.id, 'requestedParameters')} required /></label></div>}</article>)}</div></section>
      </div>
      <aside className="receipt-form-aside transfer-form-aside">
        <section className="panel-card intake-form-card"><FormHeading number="03" title="Nơi nhận, thời hạn và lưu mẫu" copy="Theo mục III của Phiếu chuyển mẫu số 4." /><label>Phòng / bộ phận nhận<input value="KCS" readOnly aria-readonly="true" /><small className="field-help">Hiện hệ thống chỉ vận hành một phòng KCS.</small></label><label>Ngày chuyển mẫu<input type="date" value={form.transferDate} onChange={updateForm('transferDate')} required /></label><label>Hạn trả kết quả<input type="date" min={form.transferDate} value={form.resultsDueDate} onChange={updateForm('resultsDueDate')} required /></label><label className="switch-label"><input type="checkbox" checked={form.returnToCustomer} onChange={updateForm('returnToCustomer')} /><span className="fake-switch" />Lưu và trả mẫu cho khách</label>{form.returnToCustomer && <label>Ngày trả/lưu đến<input type="date" min={form.transferDate} value={form.customerReturnDate} onChange={updateForm('customerReturnDate')} required /></label>}<label>Thay đổi / ghi chú<textarea value={form.changesNote} onChange={updateForm('changesNote')} rows="4" /></label></section>
        <div className="form-submit-panel"><p><strong>{editing ? 'Cập nhật bản nháp' : 'Lưu bản nháp'}</strong><span>Kiểm tra lại Phiếu số 4 rồi bấm “Gửi bàn giao” ở màn chi tiết để KCS nhận yêu cầu.</span></p><button className="primary-button" type="submit" disabled={saving}>{saving ? 'Đang lưu bản nháp…' : editing ? 'Lưu thay đổi' : 'Lưu bản nháp phiếu chuyển'}</button></div>
      </aside>
    </form>}
  </section>;
}

function FormHeading({ number, title, copy }) { return <div className="form-card-heading"><span className="form-number">{number}</span><div><h2>{title}</h2><p>{copy}</p></div></div>; }
function formatDate(value) { return value ? new Date(value).toLocaleDateString('vi-VN') : '—'; }

function businessDate(offsetDays = 0) {
  const parts = new Intl.DateTimeFormat('en', {
    timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(new Date(Date.now() + offsetDays * 86400000));
  const values = Object.fromEntries(parts.map((part) => [part.type, part.value]));
  return `${values.year}-${values.month}-${values.day}`;
}

function buildItems(samples, draftItems = []) {
  const draftBySample = new Map(draftItems.map((item) => [item.sampleId, item]));
  return Object.fromEntries(samples.filter((sample) => sample.status === 'RECEIVED').map((sample) => {
    const draft = draftBySample.get(sample.id);
    return [sample.id, {
      selected: draft ? true : draftItems.length === 0,
      weight: draft?.weight ?? sample.quantity ?? '',
      quantity: draft?.quantity ?? sample.quantity ?? '',
      sampleCondition: draft?.sampleCondition ?? sample.sampleCondition ?? 'Nguyên vẹn',
      requestedParameters: draft?.requestedParameters ?? sample.requestedParameters ?? '',
      testMethod: draft?.testMethod ?? '',
    }];
  }));
}
