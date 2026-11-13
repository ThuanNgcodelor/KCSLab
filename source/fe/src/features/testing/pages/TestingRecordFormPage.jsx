import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { listTransfers } from '../../transfer/api/transferApi';
import { createTestingRecord, getTestingRecord, listTestingRecords, updateTestingRecord } from '../api/testingApi';

const emptyEquipment = () => ({ name: '', serialNumber: '', manufacturer: '', identificationCode: '', calibrationDate: '' });
const emptyReagent = () => ({ name: '', code: '', origin: '', preparationDate: '', expiryDate: '', storageCondition: '', concentration: '' });
const emptyMeasurement = (type, replicate = '1') => ({ type, replicate, sampleAmount: '', finalVolume: '', dilutionFactor: '', measuredConcentration: '', resultPerReplicate: '', result: '', deviation: '' });
const initialForm = () => ({ transferItemId: '', analysisStart: '', analysisEnd: '', calibrationCurve: '', calculationFormula: '', recoveryPercent: '', qcChartNote: '', analystNote: '', equipment: [emptyEquipment()], reagents: [emptyReagent()], measurements: [emptyMeasurement('QC'), emptyMeasurement('RAW')] });

export function TestingRecordFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [form, setForm] = useState(initialForm);
  const [options, setOptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const editing = Boolean(id);

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const [transfers, records, current] = await Promise.all([
          listTransfers(),
          listTestingRecords(),
          editing ? getTestingRecord(id) : Promise.resolve(null),
        ]);
        if (!active) return;
        const usedIds = new Set(records.filter((record) => !current || record.id !== current.id).map((record) => record.transferItemId));
        const available = transfers.filter((transfer) => transfer.status === 'ACCEPTED').flatMap((transfer) => transfer.items.map((item) => ({ ...item, transferCode: transfer.transferCode, receiptCode: transfer.receiptCode, customerName: transfer.customerName, receivedDate: transfer.transferDate, dueDate: transfer.resultsDueDate }))).filter((item) => !usedIds.has(item.id));
        setOptions(available);
        if (current) setForm(fromRecord(current));
        else if (available.length) setForm((value) => ({ ...value, transferItemId: String(available[0].id) }));
      } catch (exception) { if (active) setError(exception.message); }
      finally { if (active) setLoading(false); }
    }
    load();
    return () => { active = false; };
  }, [editing, id]);

  const selected = useMemo(() => options.find((item) => String(item.id) === String(form.transferItemId)), [options, form.transferItemId]);
  const update = (field) => (event) => setForm((value) => ({ ...value, [field]: event.target.value }));
  const updateRow = (group, index, field, value) => setForm((current) => ({ ...current, [group]: current[group].map((row, rowIndex) => rowIndex === index ? { ...row, [field]: value } : row) }));
  const addRow = (group, row) => setForm((current) => ({ ...current, [group]: [...current[group], row] }));
  const removeRow = (group, index) => setForm((current) => ({ ...current, [group]: current[group].filter((_, rowIndex) => rowIndex !== index) }));

  async function handleSubmit(event) {
    event.preventDefault(); setSaving(true); setError('');
    const payload = {
      ...form,
      transferItemId: Number(form.transferItemId),
      analysisStart: form.analysisStart ? new Date(form.analysisStart).toISOString() : null,
      analysisEnd: form.analysisEnd ? new Date(form.analysisEnd).toISOString() : null,
      recoveryPercent: form.recoveryPercent === '' ? null : Number(form.recoveryPercent),
      equipment: form.equipment.filter((item) => item.name.trim()),
      reagents: form.reagents.filter((item) => item.name.trim()),
      measurements: form.measurements.filter((item) => item.replicate.trim()),
    };
    try {
      const saved = editing ? await updateTestingRecord(id, payload) : await createTestingRecord(payload);
      navigate(`/admin/testing/${saved.id}`, { state: { message: editing ? 'Đã cập nhật biên bản.' : `Đã lập biên bản ${saved.recordCode}.` } });
    } catch (exception) { setError(exception.message); }
    finally { setSaving(false); }
  }

  if (loading) return <section className="staff-page"><p className="muted">Đang chuẩn bị biên bản…</p></section>;

  return <section className="staff-page testing-form-page">
    <header className="admin-heading staff-heading"><div><button className="back-link" onClick={() => navigate(editing ? `/admin/testing/${id}` : '/admin/testing')}>← {editing ? 'Quay lại biên bản' : 'Danh sách biên bản'}</button><p className="section-kicker">Biên bản số 5</p><h1>{editing ? 'Cập nhật biên bản' : 'Lập biên bản thử nghiệm'}</h1><p className="heading-copy">Ghi lại dữ liệu chuyên môn có thể truy xuất và kiểm tra.</p></div></header>
    {error && <p className="form-error form-page-error">{error}</p>}
    {!editing && options.length === 0 ? <section className="panel-card empty-intake"><span>⌁</span><h3>Chưa có mẫu sẵn sàng thử nghiệm</h3><p>Mẫu phải được phòng phân tích xác nhận nhận ở phiếu chuyển số 4.</p><button className="outline-button" onClick={() => navigate('/admin/transfers')}>Xem phiếu chuyển mẫu</button></section> : <form className="testing-form-layout" onSubmit={handleSubmit}>
      <main className="receipt-form-main">
        <section className="panel-card intake-form-card"><FormHeading number="I" title="Thông tin chung" copy="Mẫu, thời gian và người thực hiện." /><label>Mẫu đã bàn giao<select value={form.transferItemId} onChange={update('transferItemId')} disabled={editing} required>{options.map((item) => <option key={item.id} value={item.id}>{item.sampleCode} — {item.sampleName} ({item.transferCode})</option>)}</select></label>{selected && <div className="testing-sample-summary"><span><small>Phiếu yêu cầu</small><strong>{selected.receiptCode}</strong></span><span><small>Chỉ tiêu</small><strong>{selected.requestedParameters || '—'}</strong></span><span><small>Phương pháp</small><strong>{selected.testMethod}</strong></span><span><small>Hạn trả</small><strong>{formatDate(selected.dueDate)}</strong></span></div>}<div className="form-two-columns"><label>Bắt đầu thử nghiệm<input type="datetime-local" value={form.analysisStart} onChange={update('analysisStart')} /></label><label>Kết thúc thử nghiệm<input type="datetime-local" value={form.analysisEnd} onChange={update('analysisEnd')} /></label><label className="full-span">Ghi chú người phân tích<textarea value={form.analystNote} onChange={update('analystNote')} placeholder="Tình trạng mẫu, thao tác đặc biệt hoặc sai lệch" /></label></div></section>
        <section className="panel-card intake-form-card"><FormHeading number="II" title="Thiết bị phân tích" copy="Mã hiệu và tình trạng hiệu chuẩn dùng cho phép thử." />{form.equipment.map((item, index) => <EquipmentEditor key={index} item={item} index={index} updateRow={updateRow} removeRow={removeRow} />)}<button type="button" className="outline-button add-row-button" onClick={() => addRow('equipment', emptyEquipment())}>＋ Thêm thiết bị</button></section>
        <section className="panel-card intake-form-card"><FormHeading number="III" title="Hóa chất và thuốc thử" copy="Nguồn gốc, ngày pha, hạn dùng và điều kiện bảo quản." />{form.reagents.map((item, index) => <ReagentEditor key={index} item={item} index={index} updateRow={updateRow} removeRow={removeRow} />)}<button type="button" className="outline-button add-row-button" onClick={() => addRow('reagents', emptyReagent())}>＋ Thêm hóa chất</button></section>
        <section className="panel-card intake-form-card"><FormHeading number="IV–V" title="Đường chuẩn và công thức" copy="Ghi nội dung áp dụng cho lần thử nghiệm này." /><label>Đường chuẩn<textarea value={form.calibrationCurve} onChange={update('calibrationCurve')} placeholder="Phương trình, hệ số tương quan, khoảng tuyến tính…" /></label><label>Công thức tính<textarea value={form.calculationFormula} onChange={update('calculationFormula')} placeholder="Công thức và diễn giải các biến" /></label></section>
        <MeasurementSection type="QC" title="Đảm bảo chất lượng" number="VI" rows={form.measurements} updateRow={updateRow} removeRow={removeRow} addRow={addRow} />
        <MeasurementSection type="RAW" title="Số liệu thực nghiệm" number="VII" rows={form.measurements} updateRow={updateRow} removeRow={removeRow} addRow={addRow} />
      </main>
      <aside className="receipt-form-aside testing-form-aside"><section className="panel-card intake-form-card"><FormHeading number="QC" title="Đánh giá chất lượng" copy="Tóm tắt hiệu suất thu hồi và biểu đồ kiểm soát." /><label>Hiệu suất thu hồi (%)<input type="number" min="0" max="999.9999" step="0.0001" value={form.recoveryPercent} onChange={update('recoveryPercent')} /></label><label>Biểu đồ / nhận xét QC<textarea value={form.qcChartNote} onChange={update('qcChartNote')} placeholder="Mô tả vị trí biểu đồ hoặc kết luận kiểm soát" /></label></section><section className="form-submit-panel"><p><strong>{editing ? 'Lưu thay đổi' : 'Tạo bản nháp'}</strong><span>Sau khi lưu, kỹ thuật viên gửi biên bản sang bước kiểm tra.</span></p><button className="primary-button" disabled={saving || !form.transferItemId}>{saving ? 'Đang lưu…' : editing ? 'Lưu biên bản' : 'Tạo biên bản'}</button></section></aside>
    </form>}
  </section>;
}

function FormHeading({ number, title, copy }) { return <div className="form-card-heading"><span className="form-number">{number}</span><div><h2>{title}</h2><p>{copy}</p></div></div>; }

function EquipmentEditor({ item, index, updateRow, removeRow }) { return <div className="lab-entry-card"><div className="entry-card-heading"><strong>Thiết bị {index + 1}</strong><button type="button" onClick={() => removeRow('equipment', index)}>Xóa</button></div><div className="form-two-columns"><label>Tên thiết bị<input value={item.name} onChange={(event) => updateRow('equipment', index, 'name', event.target.value)} placeholder="ICP-MS" /></label><label>Mã hiệu / Serial<input value={item.serialNumber} onChange={(event) => updateRow('equipment', index, 'serialNumber', event.target.value)} /></label><label>Nhà sản xuất<input value={item.manufacturer} onChange={(event) => updateRow('equipment', index, 'manufacturer', event.target.value)} /></label><label>Mã nhận dạng<input value={item.identificationCode} onChange={(event) => updateRow('equipment', index, 'identificationCode', event.target.value)} /></label><label>Ngày hiệu chuẩn<input type="date" value={item.calibrationDate} onChange={(event) => updateRow('equipment', index, 'calibrationDate', event.target.value)} /></label></div></div>; }

function ReagentEditor({ item, index, updateRow, removeRow }) { return <div className="lab-entry-card"><div className="entry-card-heading"><strong>Hóa chất {index + 1}</strong><button type="button" onClick={() => removeRow('reagents', index)}>Xóa</button></div><div className="form-two-columns"><label>Tên hóa chất<input value={item.name} onChange={(event) => updateRow('reagents', index, 'name', event.target.value)} /></label><label>Mã hóa chất<input value={item.code} onChange={(event) => updateRow('reagents', index, 'code', event.target.value)} /></label><label>Nước sản xuất<input value={item.origin} onChange={(event) => updateRow('reagents', index, 'origin', event.target.value)} /></label><label>Nồng độ<input value={item.concentration} onChange={(event) => updateRow('reagents', index, 'concentration', event.target.value)} /></label><label>Ngày pha<input type="date" value={item.preparationDate} onChange={(event) => updateRow('reagents', index, 'preparationDate', event.target.value)} /></label><label>Hạn sử dụng<input type="date" value={item.expiryDate} onChange={(event) => updateRow('reagents', index, 'expiryDate', event.target.value)} /></label><label className="full-span">Bảo quản<input value={item.storageCondition} onChange={(event) => updateRow('reagents', index, 'storageCondition', event.target.value)} /></label></div></div>; }

function MeasurementSection({ type, title, number, rows, updateRow, removeRow, addRow }) { const typedRows = rows.map((row, index) => ({ row, index })).filter(({ row }) => row.type === type); return <section className="panel-card intake-form-card"><FormHeading number={number} title={title} copy={type === 'QC' ? 'Số liệu mẫu QC thực hiện cùng quy trình.' : 'Dữ liệu đo và kết quả theo từng lần lặp.'} />{typedRows.map(({ row, index }, visibleIndex) => <div className="lab-entry-card measurement-card" key={index}><div className="entry-card-heading"><strong>Lần lặp {visibleIndex + 1}</strong><button type="button" onClick={() => removeRow('measurements', index)}>Xóa</button></div><div className="measurement-grid">{measurementFields.map((field) => <label key={field.key}>{field.label}<input value={row[field.key]} onChange={(event) => updateRow('measurements', index, field.key, event.target.value)} /></label>)}</div></div>)}<button type="button" className="outline-button add-row-button" onClick={() => addRow('measurements', emptyMeasurement(type, String(typedRows.length + 1)))}>＋ Thêm lần lặp</button></section>; }

const measurementFields = [{ key: 'replicate', label: 'Lần lặp' }, { key: 'sampleAmount', label: 'Lượng mẫu' }, { key: 'finalVolume', label: 'Thể tích định mức' }, { key: 'dilutionFactor', label: 'Hệ số pha loãng' }, { key: 'measuredConcentration', label: 'Nồng độ phân tích' }, { key: 'resultPerReplicate', label: 'KQ / lần' }, { key: 'result', label: 'Kết quả' }, { key: 'deviation', label: 'Độ lệch' }];

function fromRecord(record) { return { transferItemId: String(record.transferItemId), analysisStart: toLocalInput(record.analysisStart), analysisEnd: toLocalInput(record.analysisEnd), calibrationCurve: record.calibrationCurve || '', calculationFormula: record.calculationFormula || '', recoveryPercent: record.recoveryPercent ?? '', qcChartNote: record.qcChartNote || '', analystNote: record.analystNote || '', equipment: record.equipment.length ? record.equipment.map(stripId) : [emptyEquipment()], reagents: record.reagents.length ? record.reagents.map(stripId) : [emptyReagent()], measurements: record.measurements.length ? record.measurements.map(stripId) : [emptyMeasurement('QC'), emptyMeasurement('RAW')] }; }
function stripId({ id, rowOrder, ...value }) { return value; }
function toLocalInput(value) { if (!value) return ''; const date = new Date(value); return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16); }
function formatDate(value) { return value ? new Date(`${value}T00:00:00`).toLocaleDateString('vi-VN') : '—'; }
