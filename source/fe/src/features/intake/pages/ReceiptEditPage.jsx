import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { PageLoader } from '../../../shared/components/Loading';
import { DateInput } from '../../../shared/components/DateInput';
import { getReceipt, updateReceipt } from '../api/intakeApi';
import { buildCondition, conditionOptions, parameterOptions, parseCondition } from '../intakeModel';

const packagingOptions = ['Túi PE', 'Hộp/thùng', 'Chai/lọ'];
const sealOptions = [['INTACT', 'Còn nguyên'], ['NOT_PRESENT', 'Không có'], ['OPENED', 'Đã mở']];

/** Chỉ tiêu lưu thành chuỗi "Cadimi (Cd), Khác: Ruột + vỏ": tách thành lựa chọn có sẵn và phần nhập thêm. */
function splitParameterText(value) {
  const parts = (value || '').split(',').map((item) => item.trim()).filter(Boolean);
  return {
    picked: parts.filter((item) => parameterOptions.includes(item)),
    other: parts.filter((item) => !parameterOptions.includes(item)).map((item) => item.replace(/^Khác:\s*/i, '')).join(', '),
  };
}
function joinParameterText({ picked, other }) {
  return [...picked, ...(other.trim() ? [`Khác: ${other.trim()}`] : [])].join(', ');
}

// Lỗi thường gặp: hai mẫu cùng tên. Cảnh báo ngay khi gõ để người nhập thấy trước khi lưu.
function duplicateNames(samples) {
  const seen = new Map();
  samples.forEach((sample, index) => {
    const key = sample.name.trim().toLowerCase().replace(/\s+/g, ' ');
    if (key) seen.set(key, [...(seen.get(key) ?? []), index]);
  });
  return new Set([...seen.values()].filter((list) => list.length > 1).flat());
}

export function ReceiptEditPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [receipt, setReceipt] = useState(null);
  const [form, setForm] = useState(null);
  const [samples, setSamples] = useState([]);
  const [reason, setReason] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    getReceipt(id).then((data) => {
      if (!active) return;
      setReceipt(data);
      setForm({
        deliveredBy: data.deliveredBy ?? '', sampledBy: data.sampledBy ?? '', growingArea: data.growingArea ?? '',
        packingFacility: data.packingFacility ?? '', vehicleNumber: data.vehicleNumber ?? '', trailerNumber: data.trailerNumber ?? '',
        containerNumber: data.containerNumber ?? '', sealNumber: data.sealNumber ?? '', containerSealNumber: data.containerSealNumber ?? '',
        sealStatus: data.sealStatus ?? 'INTACT', conditionDetail: data.conditionDetail ?? '', transportInfo: data.transportInfo ?? '',
        receivedTemperature: data.receivedTemperature ?? '', storageCondition: data.storageCondition ?? '',
        storageAfterReceipt: data.storageAfterReceipt ?? '', expectedDate: data.expectedDate ?? '', specialRequest: data.specialRequest ?? '',
      });
      setSamples(data.samples.map((sample) => {
        const packaged = packagingOptions.includes(sample.packaging);
        return {
          id: sample.id, sampleCode: sample.sampleCode, name: sample.name ?? '', quantity: sample.quantity ?? '', weight: sample.weight ?? '',
          packaging: packaged ? sample.packaging : sample.packaging ? 'Khác' : '',
          packagingOther: packaged ? '' : (sample.packaging ?? '').replace(/^Khác:\s*/i, ''),
          condition: sample.sampleCondition ?? '', parameters: splitParameterText(sample.requestedParameters), status: sample.status,
        };
      }));
    }).catch((exception) => { if (active) setError(exception.message); });
    return () => { active = false; };
  }, [id]);

  if (!receipt || !form) return <section className="staff-page">{error ? <p className="form-error" role="alert">{error}</p> : <PageLoader label="Đang tải hồ sơ tiếp nhận…" rows={4} />}</section>;

  const isAdmin = user?.role === 'ADMIN';
  const locked = samples.some((sample) => sample.status !== 'RECEIVED');
  const allowed = isAdmin || (user?.role === 'RECEPTIONIST' && !locked);
  const duplicates = duplicateNames(samples);
  const change = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.value }));
  const changeSample = (index, patch) => setSamples((current) => current.map((sample, position) => (position === index ? { ...sample, ...patch } : sample)));

  async function handleSubmit(event) {
    event.preventDefault();
    if (duplicates.size) { setError('Có hai mẫu trùng tên. Mỗi mẫu cần một tên riêng (ví dụ Mẫu 1, Mẫu 2).'); return; }
    if (locked && !reason.trim()) { setError('Hồ sơ đã gửi cho KCS: vui lòng ghi lý do chỉnh sửa.'); return; }
    for (const sample of samples) {
      if (!sample.condition.trim()) { setError(`Mẫu ${sample.sampleCode}: chọn tình trạng khi nhận.`); return; }
      if (!joinParameterText(sample.parameters)) { setError(`Mẫu ${sample.sampleCode}: chọn ít nhất một chỉ tiêu.`); return; }
      if (sample.packaging === 'Khác' && !sample.packagingOther.trim()) { setError(`Mẫu ${sample.sampleCode}: nhập loại bao bì.`); return; }
    }
    setSaving(true); setError('');
    try {
      await updateReceipt(id, {
        reason: reason.trim() || null,
        ...form,
        receivedTemperature: form.receivedTemperature === '' ? null : Number(form.receivedTemperature),
        expectedDate: form.expectedDate || null,
        sampledAt: receipt.sampledAt ?? null,
        samples: samples.map((sample) => ({
          id: sample.id, name: sample.name.trim(), quantity: sample.quantity, weight: sample.weight,
          packaging: sample.packaging === 'Khác' ? `Khác: ${sample.packagingOther.trim()}` : sample.packaging,
          condition: sample.condition, requestedParameters: joinParameterText(sample.parameters),
        })),
      });
      navigate(`/admin/intake/${id}`, { state: { message: 'Đã lưu chỉnh sửa hồ sơ tiếp nhận.' } });
    } catch (exception) { setError(exception.message); } finally { setSaving(false); }
  }

  return <section className="staff-page receipt-form-page receipt-edit-page">
    <header className="admin-heading staff-heading"><div>
      <button className="back-link" type="button" onClick={() => navigate(`/admin/intake/${id}`)}>← Hồ sơ {receipt.requestNumber || receipt.receiptCode}</button>
      <p className="section-kicker">Sửa hồ sơ tiếp nhận / {receipt.requestNumber || receipt.receiptCode}</p>
      <h1>Sửa hồ sơ tiếp nhận</h1>
      <p className="heading-copy">Mã mẫu, Số/No., khách hàng và ảnh/file không đổi ở đây. Mỗi lần sửa đều được ghi lại.</p>
    </div></header>
    {error && <p className="form-error intake-toast" role="alert">{error}</p>}
    {!allowed ? <section className="panel-card intake-form-card"><h2>Không thể sửa hồ sơ này</h2>
      <p>{locked && !isAdmin ? 'Hồ sơ đã gửi cho KCS nên chỉ Admin được sửa (kèm lý do).' : 'Chỉ nhân viên tiếp nhận hoặc Admin được sửa.'}</p>
      <button className="outline-button" type="button" onClick={() => navigate(`/admin/intake/${id}`)}>Quay lại hồ sơ</button></section>
    : <form className="receipt-edit-form" onSubmit={handleSubmit}>
      {locked && <section className="panel-card intake-form-card receipt-edit-notice" role="status">
        <strong>Hồ sơ đã gửi cho KCS.</strong> Thay đổi sẽ cập nhật cả Phiếu chuyển mẫu (tên mẫu luôn đổi theo; số lượng, khối lượng, tình trạng, chỉ tiêu chỉ đổi nếu Phiếu chuyển chưa sửa riêng). Phiếu kết quả đã tạo cần tạo lại PDF.
        <label>Lý do chỉnh sửa (bắt buộc)<textarea value={reason} onChange={(event) => setReason(event.target.value)} maxLength={500} rows={2} required placeholder="Ví dụ: nhập nhầm tên mẫu khi tiếp nhận" /></label>
      </section>}

      {samples.map((sample, index) => {
        const condition = parseCondition(sample.condition);
        const setCondition = (next) => changeSample(index, { condition: buildCondition(next) });
        return <section className="panel-card intake-form-card" key={sample.id}>
          <div className="form-card-heading"><span className="form-number">{index + 1}</span><div><h2>Mẫu {sample.sampleCode}</h2><p>Mã mẫu không đổi.</p></div></div>
          <div className="form-two-columns">
            <label>Tên mẫu<input value={sample.name} onChange={(event) => changeSample(index, { name: event.target.value })} maxLength={160} required aria-invalid={duplicates.has(index)} />
              {duplicates.has(index) && <small className="form-error" role="alert">Trùng tên với mẫu khác.</small>}</label>
            <label>Số lượng / lượng mẫu<input value={sample.quantity} onChange={(event) => changeSample(index, { quantity: event.target.value })} maxLength={80} /></label>
            <label>Khối lượng<input value={sample.weight} onChange={(event) => changeSample(index, { weight: event.target.value })} maxLength={80} placeholder="Ví dụ: 2,4kg" /></label>
            <label>Loại bao bì<select value={sample.packaging} onChange={(event) => changeSample(index, { packaging: event.target.value })}>
              <option value="">Chọn loại bao bì</option>{packagingOptions.map((option) => <option key={option} value={option}>{option}</option>)}<option value="Khác">Khác</option></select></label>
            {sample.packaging === 'Khác' && <label>Bao bì khác<input value={sample.packagingOther} onChange={(event) => changeSample(index, { packagingOther: event.target.value })} maxLength={70} required /></label>}
          </div>
          <fieldset className="full-span choice-fieldset"><legend>Tình trạng mẫu khi nhận</legend>
            <div className="choice-grid">
              {conditionOptions.map((option) => <label className="choice-option" key={option}>
                <input type="checkbox" checked={condition.picked.includes(option)} onChange={(event) => setCondition({ ...condition, picked: event.target.checked ? [...condition.picked, option] : condition.picked.filter((item) => item !== option) })} /><span>{option}</span></label>)}
              <label className="choice-option"><input type="checkbox" checked={condition.abnormal} onChange={(event) => setCondition({ ...condition, abnormal: event.target.checked })} /><span>Dấu hiệu bất thường</span></label>
            </div>
            {condition.abnormal && <label className="choice-other-field">Mô tả dấu hiệu bất thường<input value={condition.abnormalText} onChange={(event) => setCondition({ ...condition, abnormalText: event.target.value })} placeholder="Ví dụ: có mùi lạ, mốc" required /></label>}
          </fieldset>
          <fieldset className="full-span choice-fieldset"><legend>Chỉ tiêu yêu cầu (chọn nhiều nếu một mẫu cần nhiều chỉ tiêu)</legend>
            <div className="choice-grid">
              {parameterOptions.map((option) => <label className="choice-option" key={option}>
                <input type="checkbox" checked={sample.parameters.picked.includes(option)} onChange={(event) => changeSample(index, { parameters: { ...sample.parameters, picked: event.target.checked ? [...sample.parameters.picked, option] : sample.parameters.picked.filter((item) => item !== option) } })} /><span>{option}</span></label>)}
            </div>
            <label className="choice-other-field">Chỉ tiêu khác<input value={sample.parameters.other} onChange={(event) => changeSample(index, { parameters: { ...sample.parameters, other: event.target.value } })} placeholder="Nhập tên chỉ tiêu khác (nếu có)" /></label>
          </fieldset>
        </section>;
      })}

      <section className="panel-card intake-form-card">
        <div className="form-card-heading"><span className="form-number">≡</span><div><h2>Thông tin bàn giao và nguồn gốc</h2><p>Người giao, lấy mẫu, vận chuyển, niêm phong, bảo quản.</p></div></div>
        <div className="form-two-columns">
          <label>Người giao<input value={form.deliveredBy} onChange={change('deliveredBy')} maxLength={160} required /></label>
          <label>Người lấy mẫu<input value={form.sampledBy} onChange={change('sampledBy')} maxLength={160} /></label>
          <label>Mã vùng trồng<input value={form.growingArea} onChange={change('growingArea')} maxLength={255} /></label>
          <label>Cơ sở đóng gói<input value={form.packingFacility} onChange={change('packingFacility')} maxLength={255} /></label>
          <label>Biển số xe<input value={form.vehicleNumber} onChange={change('vehicleNumber')} maxLength={80} /></label>
          <label>Số moóc<input value={form.trailerNumber} onChange={change('trailerNumber')} maxLength={80} /></label>
          <label>Số container<input value={form.containerNumber} onChange={change('containerNumber')} maxLength={80} /></label>
          <label>Số niêm phong<input value={form.sealNumber} onChange={change('sealNumber')} maxLength={80} /></label>
          <label>Số seal container<input value={form.containerSealNumber} onChange={change('containerSealNumber')} maxLength={80} /></label>
          <label>Trạng thái niêm phong<select value={form.sealStatus} onChange={change('sealStatus')}>{sealOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
          <label>Nhiệt độ khi nhận (°C)<input type="number" step="0.1" value={form.receivedTemperature} onChange={change('receivedTemperature')} /></label>
          <label>Điều kiện bảo quản<input value={form.storageCondition} onChange={change('storageCondition')} maxLength={80} /></label>
          <label>Bảo quản sau khi nhận<input value={form.storageAfterReceipt} onChange={change('storageAfterReceipt')} maxLength={255} /></label>
          <label>Thời hạn dự kiến<DateInput value={form.expectedDate} onChange={change('expectedDate')} /></label>
        </div>
        <label>Mô tả tình trạng<textarea value={form.conditionDetail} onChange={change('conditionDetail')} rows={2} /></label>
        <label>Vận chuyển<textarea value={form.transportInfo} onChange={change('transportInfo')} rows={2} /></label>
        <label>Yêu cầu đặc biệt<textarea value={form.specialRequest} onChange={change('specialRequest')} rows={2} /></label>
      </section>

      <div className="receipt-edit-actions">
        <button className="outline-button" type="button" onClick={() => navigate(`/admin/intake/${id}`)} disabled={saving}>Hủy</button>
        <button className="primary-button" type="submit" disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu chỉnh sửa'}</button>
      </div>
    </form>}
  </section>;
}
