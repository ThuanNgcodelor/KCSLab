import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getReceipt, listReceipts } from '../../intake/api/intakeApi';
import { createTransfer, getTransfer, updateTransfer } from '../api/transferApi';

const today = businessDate();
const dueDefault = businessDate(7);
const emptyPage = { content: [], number: 0, totalElements: 0, totalPages: 0 };

export function TransferCreatePage() {
  const { id } = useParams();
  const editing = Boolean(id);
  const navigate = useNavigate();
  const [receipt, setReceipt] = useState(null);
  const [receiptPage, setReceiptPage] = useState(emptyPage);
  const [receiptQuery, setReceiptQuery] = useState('');
  const [receiptSearchPage, setReceiptSearchPage] = useState(0);
  const [pickerOpen, setPickerOpen] = useState(!editing);
  const [searchLoading, setSearchLoading] = useState(!editing);
  const [searchError, setSearchError] = useState('');
  const [items, setItems] = useState({});
  const [form, setForm] = useState({ receivingDepartment: 'KCS', transferDate: today, resultsDueDate: dueDefault, returnToCustomer: false, customerReturnDate: '', changesNote: '' });
  const [loading, setLoading] = useState(editing);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!editing) return undefined;
    let active = true;
    getTransfer(id)
      .then(async (draft) => {
        if (draft.status !== 'DRAFT') throw new Error('Chỉ được sửa phiếu chuyển mẫu đang ở bản nháp.');
        const selectedReceipt = await getReceipt(draft.receiptId);
        if (!active) return;
        setReceipt(selectedReceipt);
        setItems(buildItems(selectedReceipt.samples ?? [], draft.items ?? []));
        setForm({
          receivingDepartment: draft.receivingDepartment,
          transferDate: draft.transferDate,
          resultsDueDate: draft.resultsDueDate,
          returnToCustomer: draft.returnToCustomer,
          customerReturnDate: draft.customerReturnDate ?? '',
          changesNote: draft.changesNote ?? '',
        });
      })
      .catch((exception) => active && setError(exception.message))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [editing, id]);

  useEffect(() => {
    if (editing || !pickerOpen) return undefined;
    let active = true;
    const timer = window.setTimeout(() => {
      setSearchLoading(true);
      setSearchError('');
      listReceipts({
        page: receiptSearchPage,
        size: 20,
        sort: 'receivedAt,desc',
        q: receiptQuery,
        transferable: true,
      })
        .then((page) => active && setReceiptPage(page))
        .catch((exception) => active && setSearchError(exception.message))
        .finally(() => active && setSearchLoading(false));
    }, receiptQuery ? 300 : 0);
    return () => { active = false; window.clearTimeout(timer); };
  }, [editing, pickerOpen, receiptQuery, receiptSearchPage]);

  const availableSamples = useMemo(() => receipt?.samples?.filter((sample) => sample.status === 'RECEIVED') ?? [], [receipt]);
  const updateForm = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.type === 'checkbox' ? event.target.checked : event.target.value }));
  const updateItem = (sampleId, field) => (event) => setItems((current) => ({ ...current, [sampleId]: { ...current[sampleId], [field]: event.target.type === 'checkbox' ? event.target.checked : event.target.value } }));

  function selectReceipt(nextReceipt) {
    setReceipt(nextReceipt);
    setItems(buildItems(nextReceipt.samples ?? []));
    setPickerOpen(false);
    setError('');
  }

  function changeReceipt() {
    setReceipt(null);
    setItems({});
    setReceiptQuery('');
    setReceiptSearchPage(0);
    setPickerOpen(true);
  }

  function updateReceiptQuery(value) {
    setReceiptQuery(value);
    setReceiptSearchPage(0);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (!receipt) { setError('Hãy chọn phiếu tiếp nhận cần chuyển mẫu.'); return; }
    const selected = availableSamples.filter((sample) => items[sample.id]?.selected);
    if (selected.length === 0) { setError('Chọn ít nhất một mẫu để chuyển.'); return; }
    setSaving(true); setError('');
    try {
      const payload = {
        receiptId: receipt.id, ...form,
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
    <form className="transfer-form-layout" onSubmit={handleSubmit}>
      <div className="receipt-form-main">
        <section className="panel-card intake-form-card">
          <FormHeading number="01" title="Phiếu yêu cầu" copy={editing ? 'Phiếu tiếp nhận được khóa khi tạo bản nháp.' : 'Tìm trên toàn bộ hồ sơ và chọn phiếu còn mẫu sẵn sàng bàn giao.'} />
          <ReceiptPicker editing={editing} open={pickerOpen} query={receiptQuery} page={receiptPage} loading={searchLoading} error={searchError} selected={receipt} onQueryChange={updateReceiptQuery} onSelect={selectReceipt} onChange={changeReceipt} onPageChange={setReceiptSearchPage} />
          {receipt && <div className="transfer-receipt-summary"><span><small>Người tiếp nhận</small><strong>{receipt.receivedByName}</strong></span><span><small>Ngày nhận</small><strong>{formatDate(receipt.receivedAt)}</strong></span><span><small>Số mẫu sẵn sàng</small><strong>{availableSamples.length}</strong></span></div>}
        </section>
        <section className="panel-card intake-form-card">
          <FormHeading number="02" title="Danh sách mẫu" copy="Thông tin được chụp lại tại thời điểm lập Phiếu số 4." />
          {!receipt ? <div className="transfer-sample-placeholder"><strong>Chưa chọn phiếu tiếp nhận</strong><span>Tìm theo mã phiếu, mã mẫu, khách hàng hoặc số điện thoại ở bước 01.</span></div> : <div className="transfer-sample-list">{availableSamples.map((sample) => <article className={`transfer-sample-card ${items[sample.id]?.selected ? 'selected' : ''}`} key={sample.id}><label className="sample-select"><input type="checkbox" checked={items[sample.id]?.selected ?? false} onChange={updateItem(sample.id, 'selected')} /><span><strong>{sample.sampleCode}</strong><small>{sample.name}</small></span></label>{items[sample.id]?.selected && <div className="form-two-columns sample-transfer-fields"><label>Khối lượng<input value={items[sample.id]?.weight ?? ''} onChange={updateItem(sample.id, 'weight')} /></label><label>Số lượng<input value={items[sample.id]?.quantity ?? ''} onChange={updateItem(sample.id, 'quantity')} /></label><label>Tình trạng<input value={items[sample.id]?.sampleCondition ?? ''} onChange={updateItem(sample.id, 'sampleCondition')} /></label><label>Phương pháp thử<input value={items[sample.id]?.testMethod ?? ''} onChange={updateItem(sample.id, 'testMethod')} placeholder="CFC-TN-…" required /></label><label className="full-span">Chỉ tiêu yêu cầu<input value={items[sample.id]?.requestedParameters ?? ''} onChange={updateItem(sample.id, 'requestedParameters')} required /></label></div>}</article>)}</div>}
        </section>
      </div>
      <aside className="receipt-form-aside transfer-form-aside">
        <section className="panel-card intake-form-card"><FormHeading number="03" title="Nơi nhận, thời hạn và lưu mẫu" copy="Theo mục III của Phiếu chuyển mẫu số 4." /><label>Phòng / bộ phận nhận<input value="KCS" readOnly aria-readonly="true" /><small className="field-help">Hiện hệ thống chỉ vận hành một phòng KCS.</small></label><label>Ngày chuyển mẫu<input type="date" value={form.transferDate} onChange={updateForm('transferDate')} required /></label><label>Hạn trả kết quả<input type="date" min={form.transferDate} value={form.resultsDueDate} onChange={updateForm('resultsDueDate')} required /></label><label className="switch-label"><input type="checkbox" checked={form.returnToCustomer} onChange={updateForm('returnToCustomer')} /><span className="fake-switch" />Lưu và trả mẫu cho khách</label>{form.returnToCustomer && <label>Ngày trả/lưu đến<input type="date" min={form.transferDate} value={form.customerReturnDate} onChange={updateForm('customerReturnDate')} required /></label>}<label>Thay đổi / ghi chú<textarea value={form.changesNote} onChange={updateForm('changesNote')} rows="4" /></label></section>
        <div className="form-submit-panel"><p><strong>{editing ? 'Cập nhật bản nháp' : 'Lưu bản nháp'}</strong><span>Kiểm tra lại Phiếu số 4 rồi bấm “Gửi bàn giao” ở màn chi tiết để KCS nhận yêu cầu.</span></p><button className="primary-button" type="submit" disabled={saving || !receipt}>{saving ? 'Đang lưu bản nháp…' : editing ? 'Lưu thay đổi' : 'Lưu bản nháp phiếu chuyển'}</button></div>
      </aside>
    </form>
  </section>;
}

function ReceiptPicker({ editing, open, query, page, loading, error, selected, onQueryChange, onSelect, onChange, onPageChange }) {
  if (selected) return <div className="receipt-picker-selected"><div><span className="receipt-picker-code">{selected.receiptCode}</span><strong>{selected.customerName}</strong><small>{selected.customerPhone || 'Chưa có số điện thoại'} · {selected.samples?.filter((sample) => sample.status === 'RECEIVED').length ?? 0} mẫu sẵn sàng</small></div>{!editing && <button type="button" className="outline-button" onClick={onChange}>Đổi phiếu</button>}</div>;
  if (!open) return null;
  const currentPage = page.number ?? 0;
  const totalPages = page.totalPages ?? 0;
  return <div className="receipt-picker">
    <label htmlFor="receipt-search">Tìm phiếu tiếp nhận</label>
    <div className="receipt-search-box"><span aria-hidden="true">⌕</span><input id="receipt-search" type="search" value={query} onChange={(event) => onQueryChange(event.target.value)} placeholder="Mã phiếu, mã mẫu, tên khách hàng, số điện thoại…" autoComplete="off" role="combobox" aria-expanded="true" aria-controls="receipt-search-results" /></div>
    <div className="receipt-picker-meta"><span>Tìm trực tiếp trên toàn bộ dữ liệu</span><span>{page.totalElements ?? 0} phiếu phù hợp</span></div>
    <div id="receipt-search-results" className="receipt-picker-results" role="listbox" aria-label="Kết quả phiếu tiếp nhận">
      {loading && <p className="receipt-picker-state">Đang tìm phiếu…</p>}
      {!loading && error && <p className="receipt-picker-state error" role="alert">{error}</p>}
      {!loading && !error && page.content.length === 0 && <p className="receipt-picker-state">Không có phiếu còn mẫu sẵn sàng chuyển.</p>}
      {!loading && !error && page.content.map((entry) => <button type="button" role="option" aria-selected="false" className="receipt-picker-result" key={entry.id} onClick={() => onSelect(entry)}><span><strong>{entry.receiptCode}</strong><small>{entry.customerName} · {entry.customerPhone || 'Chưa có SĐT'}</small></span><span><strong>{entry.samples?.filter((sample) => sample.status === 'RECEIVED').length ?? 0} mẫu</strong><small>{formatDate(entry.receivedAt)}</small></span></button>)}
    </div>
    {totalPages > 1 && <div className="receipt-picker-pagination"><button type="button" disabled={currentPage === 0 || loading} onClick={() => onPageChange(currentPage - 1)}>← Trước</button><span>Trang {currentPage + 1}/{totalPages} · 20 phiếu/trang</span><button type="button" disabled={currentPage + 1 >= totalPages || loading} onClick={() => onPageChange(currentPage + 1)}>Sau →</button></div>}
  </div>;
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
