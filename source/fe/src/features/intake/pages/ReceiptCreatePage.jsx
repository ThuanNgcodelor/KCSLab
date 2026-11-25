import { useEffect, useId, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { PhotoPicker } from '../../../shared/components/PhotoPicker';
import { createReceipt, downloadReceiptForm3, listReceipts, uploadReceiptFile } from '../api/intakeApi';
import { Icon } from '../../../shared/components/Icon';
import { DateInput, DateTimeInput } from '../../../shared/components/DateInput';
import { getTestRequest, getTestRequestByNumber, listTestRequests } from '../../sampling/api/testRequestApi';
import { Chevron } from '../../sampling/components/SamplingParts';
import { splitParameters } from '../../../shared/parameters';
import { normalizeRequestNumber } from '../../../shared/requestNumber';
import { CustomerNotificationFields } from '../components/CustomerNotificationFields';
import { CustomerNotificationHistory } from '../components/CustomerNotificationHistory';
import { fieldQuantityForSample } from '../intakeModel';

const initialForm = {
  customerName: '', customerPhone: '', requestNumber: '', customerEmail: '', customerAddress: '', deliveredBy: '',
  sampledBy: '', sampledAt: '', growingArea: '', packingFacility: '', vehicleNumber: '',
  trailerNumber: '', containerNumber: '', sealNumber: '', containerSealNumber: '', sealStatus: 'INTACT', conditionDetail: '',
  transportInfo: '', receivedTemperature: '', storageCondition: 'Thường', temperatureOther: '', storageAfterReceipt: '', expectedDate: '',
  specialRequest: '', acceptanceDecision: 'ACCEPTED', acceptanceReason: '',
};

const newSample = () => ({
  sampleCode: '', sourceCode: 'CT001', productType: 'Sầu riêng', name: '', quantity: '', weight: '', packaging: 'Túi PE',
  packagingOther: '', parameterOther: '', parameterOtherEnabled: false, condition: 'Nguyên vẹn', requestedParameters: '',
});

const parameterOptions = ['Cadimi (Cd)', 'Auramine O (Vỏ)', 'Auramine O (Ruột)', 'Dư lượng thuốc BVTV'];
const conditionOptions = ['Nguyên vẹn', 'Rò rỉ', 'Dập nát', 'Thiếu lượng'];
const ABNORMAL = 'Dấu hiệu bất thường';
// Tình trạng mẫu là checklist theo mẫu Word, lưu thành chuỗi "Rò rỉ; Dập nát; Dấu hiệu bất thường: mốc".
function parseCondition(value) {
  const tokens = (value || '').split(';').map((item) => item.trim()).filter(Boolean);
  const abnormal = tokens.find((item) => item.startsWith(ABNORMAL));
  return { picked: tokens.filter((item) => conditionOptions.includes(item)), abnormal: abnormal !== undefined, abnormalText: abnormal ? abnormal.slice(ABNORMAL.length).replace(/^:\s*/, '') : '' };
}
function buildCondition({ picked, abnormal, abnormalText }) {
  return [...picked, ...(abnormal ? [`${ABNORMAL}:${abnormalText ? ` ${abnormalText}` : ''}`] : [])].join('; ');
}
const packagingOptions = ['Túi PE', 'Hộp/thùng', 'Chai/lọ'];
const temperatureOptions = ['Thường', 'Lạnh', 'Khác'];

function selectedParameters(value) {
  return (value || '').split(',').map((item) => item.trim()).filter(Boolean);
}

// Mẫu mới sao chép mẫu liền trước (trừ chỉ tiêu); mã mẫu tăng số đuôi nếu có (…-001 → …-002).
function nextSampleCode(code) {
  const match = /^(.*?)(\d+)$/.exec((code || '').trim());
  return match ? `${match[1]}${String(Number(match[2]) + 1).padStart(match[2].length, '0')}` : '';
}
function cloneSample(previous) {
  if (!previous) return newSample();
  return { ...previous, sampleCode: nextSampleCode(previous.sampleCode), requestedParameters: '', parameterOther: '', parameterOtherEnabled: false };
}
// Phiếu yêu cầu lưu mỗi (mẫu, chỉ tiêu, nền mẫu) một dòng; Phiếu 3 gộp lại: một mẫu thật = một mã, nhiều chỉ tiêu.
function samplesFromRequest(data) {
  const byName = new Map();
  (data.items ?? []).forEach((item) => {
    const name = item.sampleNameOrId || data.sampleName;
    if (!byName.has(name)) byName.set(name, []);
    splitParameters(item.parameters, item.matrix).forEach((parameter) => { if (!byName.get(name).includes(parameter)) byName.get(name).push(parameter); });
  });
  const rows = [...byName].map(([name, parameters]) => {
    const known = parameters.filter((parameter) => parameterOptions.includes(parameter));
    const other = parameters.filter((parameter) => !parameterOptions.includes(parameter));
    return { ...newSample(), name, quantity: fieldQuantityForSample(data.fieldSampling, name, byName.size), requestedParameters: known.join(', '), parameterOtherEnabled: other.length > 0, parameterOther: other.join(', ') };
  });
  return rows.length ? rows : [{ ...newSample(), name: data.sampleName, quantity: fieldQuantityForSample(data.fieldSampling, data.sampleName, 1) }];
}

export function ReceiptCreatePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [form, setForm] = useState(initialForm);
  const [customerNotification, setCustomerNotification] = useState({ enabled: false, recipientPhone: null, consentAccepted: false });
  const [samples, setSamples] = useState([newSample()]);
  const [files, setFiles] = useState({ form3: [], samples: [], lot: [] });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [uploadWarning, setUploadWarning] = useState('');
  const [created, setCreated] = useState(null);
  const [sourceRequest, setSourceRequest] = useState(null);
  // Số/No. là định danh duy nhất: mỗi Số/No. chỉ có một hồ sơ tiếp nhận.
  const [usedBy, setUsedBy] = useState(null);
  useEffect(() => {
    const number = form.requestNumber.trim();
    if (!number) { setUsedBy(null); return undefined; }
    let active = true;
    const timer = window.setTimeout(() => {
      listReceipts({ q: number, size: 5 })
        .then((page) => { if (active) setUsedBy(page.content.find((item) => (item.requestNumber || '').toLowerCase() === number.toLowerCase()) ?? null); })
        .catch(() => { if (active) setUsedBy(null); });
    }, 300);
    return () => { active = false; window.clearTimeout(timer); };
  }, [form.requestNumber]);

  function applyRequest(data) {
    setSourceRequest(data);
    const field = data.fieldSampling;
    setForm((current) => ({ ...current,
      testRequestId: data.id, fieldSamplingId: field?.id ?? null, requestNumber: data.requestNumber,
      customerName: data.customerName ?? '', customerPhone: data.customerPhone ?? '', customerEmail: data.customerEmail ?? '',
      customerAddress: data.customerAddress ?? '', sampledBy: field?.sampleCollector ?? '', sampledAt: localDateTime(field?.samplingEnd),
      // origin là khóa API cũ của Mã vùng trồng trên Phiếu yêu cầu.
      growingArea: field?.growingArea ?? data.samplingPlan?.growingArea ?? data.origin ?? '', packingFacility: field?.packingFacility ?? '',
      vehicleNumber: field?.vehicleNumber ?? '', trailerNumber: field?.trailerNumber ?? '', containerNumber: field?.containerNumber ?? '',
      sealNumber: field?.sampleSealNumber ?? '', sealStatus: field?.sealPresent === false ? 'NOT_PRESENT' : 'INTACT',
      conditionDetail: field?.sampleCondition ?? '', transportInfo: field?.fieldNotes ?? '', storageCondition: field?.storageCondition ?? 'Thường',
      expectedDate: data.expectedDate ?? '',
    }));
    setSamples(samplesFromRequest(data));
  }

  useEffect(() => {
    const requestId = searchParams.get('requestId');
    if (!requestId) return;
    getTestRequest(requestId).then(applyRequest).catch((exception) => setError(exception.message));
  }, [searchParams]);

  async function pickRequest(number) {
    setError('');
    try { applyRequest(await getTestRequestByNumber(number)); } catch (exception) { setError(exception.message); }
  }
  function unlinkRequest() {
    setSourceRequest(null);
    setForm((current) => ({ ...current, testRequestId: undefined, fieldSamplingId: undefined }));
  }

  const update = (field) => (event) => setForm((current) => ({
    ...current,
    [field]: event.target.type === 'checkbox'
      ? event.target.checked
      : field === 'customerPhone' ? event.target.value.replace(/\D/g, '').slice(0, 11) : event.target.value,
  }));
  const updateSample = (index, field) => (event) => setSamples((current) => current.map((sample, itemIndex) => (
    itemIndex === index ? { ...sample, [field]: event.target.value } : sample
  )));
  const updateFiles = (group) => (next) => setFiles((current) => ({ ...current, [group]: next }));

  async function handleSubmit(event) {
    event.preventDefault();
    const validationMessage = usedBy ? `Số/No. ${usedBy.requestNumber} đã có hồ sơ tiếp nhận. Mỗi Số/No. chỉ được dùng cho một hồ sơ.` : validateForm(form, samples);
    if (validationMessage) {
      setError(validationMessage);
      return;
    }
    if (customerNotification.enabled && (!customerNotification.consentAccepted
      || !/^(?:0|\+?84)[1-9]\d{8,9}$/.test((customerNotification.recipientPhone ?? form.customerPhone).replace(/[\s().-]/g, '')))) {
      setError('Kiểm tra số điện thoại nhận tin và xác nhận đồng ý nhận thông báo.');
      return;
    }
    setSaving(true); setError(''); setUploadWarning('');
    try {
      const primary = samples[0];
      const { temperatureOther, ...formPayload } = form;
      const receipt = await createReceipt({
        ...formPayload,
        customerNotification: customerNotification.enabled ? { ...customerNotification, recipientPhone: customerNotification.recipientPhone ?? form.customerPhone } : undefined,
        storageCondition: form.storageCondition === 'Khác' && temperatureOther.trim() ? `Khác: ${temperatureOther.trim()}` : form.storageCondition,
        sampledAt: form.sampledAt ? new Date(form.sampledAt).toISOString() : null,
        receivedTemperature: form.receivedTemperature ? Number(form.receivedTemperature) : null,
        expectedDate: form.expectedDate || null,
        sampleName: primary.name,
        quantity: primary.quantity,
        packaging: normalizedPackaging(primary),
        sampleCondition: primary.condition,
        requestedParameters: normalizedParameters(primary),
        samples: samples.map(({ productType, packagingOther, parameterOther, parameterOtherEnabled, ...sample }) => ({
          ...sample,
          productType: sample.name.trim(),
          packaging: normalizedPackaging({ ...sample, packagingOther }),
          requestedParameters: normalizedParameters({ ...sample, parameterOther, parameterOtherEnabled }),
        })),
      });
      setCreated(receipt);

      const uploads = [
        ...files.form3.map((file) => ({ file, category: 'FORM_3', description: 'Phiếu tiếp nhận số 3' })),
        ...files.samples.map((file) => ({ file, category: 'SAMPLE_PHOTO', description: 'Ảnh mẫu khi tiếp nhận' })),
        ...files.lot.map((file) => ({ file, category: 'LOT_PHOTO', description: 'Ảnh lô hàng khi tiếp nhận' })),
      ];
      try {
        const attachments = await Promise.all(uploads.map(({ file, ...metadata }) => uploadReceiptFile(
          receipt.id,
          file,
          { ...metadata, capturedAt: file.lastModified ? new Date(file.lastModified).toISOString() : null },
        )));
        setCreated((current) => ({ ...current, attachments }));
      } catch (exception) {
        setUploadWarning(`Phiếu đã được tạo nhưng có file chưa tải lên được: ${exception.message}`);
      }
    } catch (exception) {
      setError(exception.message);
    } finally {
      setSaving(false);
    }
  }

  if (created) return <section className="staff-page receipt-success-page">
    <div className="receipt-success">
      <span className="success-check"><Icon name="check" size={26} /></span><p className="section-kicker">Đã lưu hồ sơ tiếp nhận</p>
      <h1>Đã tạo hồ sơ tiếp nhận</h1>
      <p>Người tiếp nhận, hồ sơ nguồn, danh sách mẫu và ảnh hiện trường đã được gắn vào cùng một hồ sơ.</p>
      {uploadWarning && <div className="toast toast-warning" role="alert">{uploadWarning}</div>}
      <div className="code-display"><small>SỐ/NO</small><strong>{created.requestNumber || created.receiptCode}</strong>
        <small>MÃ MẪU HIỆN TẠI</small>
        {created.samples?.map((sample) => <strong key={sample.id}>{sample.sampleCode}</strong>)}
      </div>
      <p className="muted">Đã lưu {created.attachments?.length ?? 0} file/ảnh. Mở chi tiết phiếu, dán nhãn và xác nhận trước khi chuyển; KCS sẽ đối chiếu độc lập sau khi nhận.</p>
      <CustomerNotificationHistory receiptId={created.id} />
      <div className="receipt-success-actions">
        {(created.samples?.length ? created.samples : [null]).map((item, index) => <button key={item?.id ?? 'all'} className="outline-button" onClick={() => downloadReceiptForm3(created.id, item?.id).catch((exception) => setUploadWarning(exception.message))}>{item ? `Xuất Phiếu 3 · Mẫu ${String(index + 1).padStart(2, '0')}` : 'Xuất phiếu tiếp nhận'}</button>)}
        <button className="outline-button" onClick={() => navigate('/admin/intake/receipts')}>Về danh sách</button>
        <button className="primary-button" onClick={() => { setForm(initialForm); setCustomerNotification({ enabled: false, recipientPhone: null, consentAccepted: false }); setSamples([newSample()]); setFiles({ form3: [], samples: [], lot: [] }); setCreated(null); }}>Tạo phiếu khác</button>
      </div>
    </div>
  </section>;

  return <section className="staff-page receipt-form-page">
    <header className="admin-heading staff-heading"><div>
      <button className="back-link" onClick={() => navigate('/admin/intake/receipts')}>← Quay lại danh sách</button>
      <p className="section-kicker">Vận hành / Tiếp nhận mẫu</p><h1>Tạo hồ sơ tiếp nhận</h1>
    </div></header>
    {usedBy && <div className="toast toast-error" role="alert">Số/No. <strong>{usedBy.requestNumber}</strong> đã có hồ sơ tiếp nhận (nhận ngày {new Date(usedBy.receivedAt).toLocaleDateString('vi-VN')}). Mỗi Số/No. chỉ được dùng cho một hồ sơ. <button type="button" className="link-button" onClick={() => navigate(`/admin/intake/${usedBy.id}`)}>Xem hồ sơ đó</button></div>}
    {sourceRequest && <div className="source-banner"><strong>Đang tạo từ yêu cầu: {sourceRequest.requestNumber}</strong><span>Dữ liệu đã được điền sẵn từ hồ sơ nguồn. Hãy đối chiếu thực tế trước khi lưu.</span></div>}
    {error && <div className="toast toast-error" role="alert">{error}</div>}
    <form className="receipt-form-layout" noValidate onSubmit={handleSubmit}>
      <div className="receipt-form-main">
        <FormCard number="I" en="Delivery & receipt" title="Người giao và người nhận mẫu" copy="Tài khoản đang đăng nhập được lưu tự động là người tiếp nhận.">
          <div className="form-two-columns">
            <label>Họ tên người giao<input value={form.deliveredBy} onChange={update('deliveredBy')} required /></label>
            <label>Số điện thoại<input type="tel" inputMode="numeric" autoComplete="tel" placeholder="0901234567" maxLength={11} pattern="0[0-9]{9,10}"
              aria-describedby="customer-phone-help" value={form.customerPhone} onChange={update('customerPhone')} required />
              <small id="customer-phone-help">Nhập 10 hoặc 11 chữ số, bắt đầu bằng 0.</small></label>
            <RequestPicker value={form.requestNumber} linked={Boolean(sourceRequest)} onChange={update('requestNumber')} onPick={pickRequest} onUnlink={unlinkRequest} />
            <label>Tên khách hàng / đơn vị<input value={form.customerName} onChange={update('customerName')} required /></label>
            <label>Email<input type="email" value={form.customerEmail} onChange={update('customerEmail')} /></label>
            <label className="full-span">Địa chỉ<input value={form.customerAddress} onChange={update('customerAddress')} /></label>
            <label>Người nhận mẫu<input value={form.sampledBy} onChange={update('sampledBy')} placeholder="Nếu khác người giao" /></label>
            <label>Thời điểm nhận mẫu<DateTimeInput value={form.sampledAt} onChange={update('sampledAt')} /></label>
          </div>
        </FormCard>

        <FormCard number="II" en="Source & consignment" title="Hồ sơ nguồn và lô hàng" copy="Dữ liệu có cấu trúc để truy xuất và đưa vào hồ sơ kết quả sau này.">
          <div className="form-two-columns">
            <label>Mã vùng trồng<input value={form.growingArea} onChange={update('growingArea')} maxLength={255} /></label>
            <label>Cơ sở đóng gói<input value={form.packingFacility} onChange={update('packingFacility')} /></label>
            <label>Biển số xe<input value={form.vehicleNumber} onChange={update('vehicleNumber')} /></label>
            <label>Số moóc<input value={form.trailerNumber} onChange={update('trailerNumber')} /></label>
            <label>Số container<input value={form.containerNumber} onChange={update('containerNumber')} /></label>
            <label>Số niêm phong mẫu<input value={form.sealNumber} onChange={update('sealNumber')} /></label>
            <label>Số seal container<input value={form.containerSealNumber} onChange={update('containerSealNumber')} /></label>
            <label>Trạng thái niêm phong<select value={form.sealStatus} onChange={update('sealStatus')} required><option value="INTACT">Còn nguyên</option><option value="NOT_PRESENT">Không có</option><option value="OPENED">Đã mở</option></select></label>
            <label className="full-span">Phương tiện / điều kiện vận chuyển<input value={form.transportInfo} onChange={update('transportInfo')} /></label>
          </div>
        </FormCard>

        <FormCard number="III" en="Samples" title="Danh sách mẫu" copy="Một phiếu tiếp nhận có thể chứa nhiều mẫu độc lập.">
          <div className="intake-sample-editor">
            {samples.map((sample, index) => <SampleCard key={index} index={index} sample={sample} canRemove={samples.length > 1} onRemove={() => setSamples((current) => current.filter((_, itemIndex) => itemIndex !== index))}>
              <div className="form-two-columns">
                <label>Mã hóa mẫu<input value={sample.sampleCode} onChange={updateSample(index, 'sampleCode')} placeholder="Ví dụ: CT001-SR-19112026-001" required /></label>
                <label>Tên mẫu<input value={sample.name} onChange={updateSample(index, 'name')} required /></label>
                <label>Mã nguồn<input value={sample.sourceCode} onChange={updateSample(index, 'sourceCode')} required /></label>
                <label>Số lượng / lượng mẫu<input value={sample.quantity} onChange={updateSample(index, 'quantity')} maxLength={80} placeholder="Ví dụ: 2 quả" /></label>
                <label>Khối lượng<input value={sample.weight} onChange={updateSample(index, 'weight')} maxLength={80} placeholder="Ví dụ: 500 g hoặc 2 kg" /></label>
                <label>Loại bao bì<select value={packagingOptions.includes(sample.packaging) ? sample.packaging : sample.packaging ? 'Khác' : ''} onChange={updateSample(index, 'packaging')}><option value="">Chọn loại bao bì</option>{packagingOptions.map((option) => <option value={option} key={option}>{option}</option>)}<option value="Khác">Khác</option></select></label>
                {sample.packaging === 'Khác' && <label>Loại bao bì khác<input value={sample.packagingOther} onChange={updateSample(index, 'packagingOther')} placeholder="Nhập loại bao bì" required /></label>}
                <ConditionChecklist value={sample.condition} onChange={(condition) => setSamples((current) => current.map((item, itemIndex) => (itemIndex === index ? { ...item, condition } : item)))} name={`condition-${index}`} />
                <fieldset className="full-span choice-fieldset">
                  <legend>Chỉ tiêu yêu cầu (chọn nhiều nếu một mẫu cần nhiều chỉ tiêu)</legend>
                  <div className="choice-grid">
                    {parameterOptions.map((option) => <label className="choice-option" key={option}>
                      <input type="checkbox" checked={selectedParameters(sample.requestedParameters).includes(option)} onChange={(event) => setSamples((current) => current.map((item, itemIndex) => {
                        if (itemIndex !== index) return item;
                        const picked = selectedParameters(item.requestedParameters).filter((name) => name !== option);
                        return { ...item, requestedParameters: (event.target.checked ? [...picked, option] : picked).join(', ') };
                      }))} />
                      <span>{option}</span>
                    </label>)}
                    <label className="choice-option">
                      <input type="checkbox" checked={sample.parameterOtherEnabled} onChange={(event) => setSamples((current) => current.map((item, itemIndex) => (
                        itemIndex === index ? { ...item, parameterOtherEnabled: event.target.checked, parameterOther: event.target.checked ? item.parameterOther : '' } : item
                      )))} />
                      <span>Khác</span>
                    </label>
                  </div>
                  {sample.parameterOtherEnabled && <label className="choice-other-field">Chỉ tiêu khác<input value={sample.parameterOther} onChange={updateSample(index, 'parameterOther')} placeholder="Nhập tên chỉ tiêu" required /></label>}
                </fieldset>
              </div>
            </SampleCard>)}
          </div>
          <button type="button" className="outline-button add-sample-button" onClick={() => setSamples((current) => [...current, cloneSample(current[current.length - 1])])}>＋ Thêm mẫu vào phiếu (sao chép mẫu trước)</button>
        </FormCard>

        <FormCard number="IV" en="Conditions & decision" title="Điều kiện và kết luận tiếp nhận" copy="Ghi nhận tình trạng chung của lô tại thời điểm nhận.">
          <div className="form-two-columns">
            <label>Nhiệt độ / điều kiện khi nhận<select value={temperatureOptions.includes(form.storageCondition) ? form.storageCondition : 'Khác'} onChange={update('storageCondition')}><option value="">Chọn điều kiện</option>{temperatureOptions.map((option) => <option value={option} key={option}>{option}</option>)}</select></label>
            {form.storageCondition === 'Khác' && <label>Điều kiện khác<input value={form.temperatureOther} onChange={update('temperatureOther')} placeholder="Nhập điều kiện" required /></label>}
            <label className="full-span">Bảo quản sau khi nhận<input value={form.storageAfterReceipt} onChange={update('storageAfterReceipt')} placeholder="Ví dụ: Tủ mát 4 °C" /></label>
            <label>Nhiệt độ cụ thể (°C)<input type="number" step="0.1" value={form.receivedTemperature} onChange={update('receivedTemperature')} placeholder="Chỉ nhập khi cần" /></label>
            <label>Thời hạn dự kiến<DateInput value={form.expectedDate} onChange={update('expectedDate')} /></label>
            <label className="full-span">Mô tả tình trạng<textarea value={form.conditionDetail} onChange={update('conditionDetail')} rows="3" /></label>
            <label className="full-span">Yêu cầu đặc biệt<textarea value={form.specialRequest} onChange={update('specialRequest')} rows="3" /></label>
          </div>
          <div className="decision-grid">{[['ACCEPTED', 'Chấp nhận'], ['ACCEPTED_WITH_CONDITION', 'Chấp nhận có điều kiện'], ['REJECTED', 'Từ chối']].map(([value, label]) => <label className={`decision-option ${form.acceptanceDecision === value ? 'selected' : ''}`} key={value}><input type="radio" name="acceptanceDecision" value={value} checked={form.acceptanceDecision === value} onChange={update('acceptanceDecision')} /><span><strong>{label}</strong><small>{value === 'ACCEPTED' ? 'Đủ điều kiện tiếp nhận' : 'Bắt buộc ghi lý do/điều kiện'}</small></span></label>)}</div>
          {form.acceptanceDecision !== 'ACCEPTED' && <label className="reason-field">Lý do / điều kiện<textarea value={form.acceptanceReason} onChange={update('acceptanceReason')} rows="3" required /></label>}
        </FormCard>
      </div>

      <aside className="receipt-form-aside">
        <CustomerNotificationFields value={customerNotification} defaultPhone={form.customerPhone} onChange={setCustomerNotification} />
        <FormCard number="V" en="Photos & documents" title="Ảnh và hồ sơ" copy="Ảnh được phân loại và ghi nhận người tải lên." className="upload-card">
          <PhotoPicker label="Biên bản tiếp nhận" hint="PDF, DOCX hoặc ảnh chụp phiếu" camera accept="application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,image/*" files={files.form3} onChange={updateFiles('form3')} />
          <PhotoPicker label="Ảnh mẫu" hint="Chụp trực tiếp hoặc chọn nhiều ảnh" files={files.samples} onChange={updateFiles('samples')} />
          <PhotoPicker label="Ảnh lô hàng" hint="Toàn cảnh lô/xe/container khi nhận" files={files.lot} onChange={updateFiles('lot')} />
          <small className="field-help">Tối đa 10 MB mỗi file. Hỗ trợ PDF, DOCX, JPG, PNG, WEBP, HEIC/HEIF.</small>
        </FormCard>
        <div className="form-submit-panel"><p><strong>Kiểm tra trước khi lưu</strong><span>Sau khi lưu, dán nhãn và xác nhận tại chi tiết phiếu; KCS đối chiếu độc lập sau bàn giao.</span></p><button className="primary-button" type="submit" disabled={saving}>{saving ? 'Đang lưu hồ sơ…' : 'Lưu phiếu tiếp nhận'}</button></div>
      </aside>
    </form>
  </section>;
}

function validateForm(form, samples) {
  if (!form.deliveredBy.trim()) return 'Vui lòng nhập họ tên người giao mẫu.';
  if (!/^0\d{9,10}$/.test(form.customerPhone)) return 'Số điện thoại phải có 10 hoặc 11 chữ số và bắt đầu bằng số 0.';
  if (!form.requestNumber.trim()) return 'Vui lòng nhập số Phiếu yêu cầu.';
  if (!form.customerName.trim()) return 'Vui lòng nhập tên khách hàng hoặc đơn vị.';
  if (form.customerEmail.trim() && !/^\S+@\S+\.\S+$/.test(form.customerEmail.trim())) return 'Email khách hàng không đúng định dạng.';
  if (!form.sealStatus) return 'Vui lòng chọn trạng thái niêm phong.';
  if (!samples.length) return 'Phiếu phải có ít nhất một mẫu.';
  if (samples.some((sample) => sample.quantity.length > 80 || sample.weight.length > 80)) return 'Số lượng và khối lượng mỗi mẫu tối đa 80 ký tự. Vui lòng kiểm tra lại thông tin lấy từ phiếu hiện trường.';
  if (samples.some((sample) => !sample.sampleCode.trim() || !sample.name.trim()
    || !sample.sourceCode.trim()
    || (!sample.requestedParameters.trim() && !(sample.parameterOtherEnabled && sample.parameterOther.trim()))
    || (sample.parameterOtherEnabled && !sample.parameterOther.trim())
    || (sample.packaging === 'Khác' && !sample.packagingOther.trim()))) {
    return 'Mỗi mẫu cần có mã mẫu, tên mẫu, mã nguồn, chỉ tiêu yêu cầu và loại bao bì.';
  }
  if (samples.some((sample) => {
    const condition = parseCondition(sample.condition);
    return !condition.picked.length && !condition.abnormal || (condition.abnormal && !condition.abnormalText.trim());
  })) return 'Mỗi mẫu cần chọn tình trạng khi nhận (nếu có dấu hiệu bất thường hãy mô tả).';
  if (form.acceptanceDecision !== 'ACCEPTED' && !form.acceptanceReason.trim()) {
    return 'Vui lòng nhập lý do hoặc điều kiện tiếp nhận.';
  }
  return '';
}

function normalizedPackaging(sample) {
  return sample.packaging === 'Khác' && sample.packagingOther.trim()
    ? `Khác: ${sample.packagingOther.trim()}`
    : sample.packaging;
}

function normalizedParameters(sample) {
  const parameters = selectedParameters(sample.requestedParameters);
  if (sample.parameterOtherEnabled && sample.parameterOther.trim()) parameters.push(`Khác: ${sample.parameterOther.trim()}`);
  return parameters.join(', ');
}

function localDateTime(value) {
  if (!value) return '';
  const date = new Date(value); const pad = (number) => String(number).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function ConditionChecklist({ value, onChange, name }) {
  const state = parseCondition(value);
  function toggle(option) {
    const picked = state.picked.includes(option) ? state.picked.filter((item) => item !== option) : [...state.picked, option];
    // "Nguyên vẹn" loại trừ các tình trạng còn lại
    onChange(buildCondition(option === 'Nguyên vẹn' && picked.includes(option) ? { picked: [option], abnormal: false, abnormalText: '' }
      : { ...state, picked: picked.filter((item) => option === 'Nguyên vẹn' || item !== 'Nguyên vẹn') }));
  }
  function toggleAbnormal() {
    const abnormal = !state.abnormal;
    onChange(buildCondition({ picked: abnormal ? state.picked.filter((item) => item !== 'Nguyên vẹn') : state.picked, abnormal, abnormalText: abnormal ? state.abnormalText : '' }));
  }
  return <fieldset className="full-span choice-fieldset">
    <legend>Tình trạng mẫu khi nhận</legend>
    <div className="choice-grid">
      {conditionOptions.map((option) => <label className="choice-option" key={option}>
        <input type="checkbox" name={name} checked={state.picked.includes(option)} onChange={() => toggle(option)} /><span>{option}</span>
      </label>)}
      <label className="choice-option"><input type="checkbox" name={name} checked={state.abnormal} onChange={toggleAbnormal} /><span>Dấu hiệu bất thường</span></label>
    </div>
    {state.abnormal && <label className="choice-other-field">Mô tả dấu hiệu bất thường<input value={state.abnormalText} onChange={(event) => onChange(buildCondition({ ...state, abnormalText: event.target.value }))} placeholder="Ví dụ: có mùi lạ, mốc" required /></label>}
  </fieldset>;
}

function FormCard({ number, title, en, copy, className = 'intake-form-card', defaultOpen = true, children }) {
  const [open, setOpen] = useState(defaultOpen); const bodyId = useId();
  return <section className={`panel-card ${className} collapsible-card`}>
    <button type="button" className="section-toggle" aria-expanded={open} aria-controls={bodyId} onClick={() => setOpen(!open)}>
      <span className="form-number">{number}</span><span className="section-toggle-text"><strong>{title}{en && <i>{en}</i>}</strong>{copy && <small>{copy}</small>}</span><Chevron open={open} />
    </button>
    <div id={bodyId} className="section-body" hidden={!open}>{children}</div>
  </section>;
}

function SampleCard({ index, sample, canRemove, onRemove, children }) {
  const [open, setOpen] = useState(true); const bodyId = useId();
  const summary = [sample.name, [sample.requestedParameters, sample.parameterOtherEnabled ? sample.parameterOther : ''].filter(Boolean).join(', ')].filter(Boolean).join(' · ');
  return <article className="sample-editor-card">
    <div className="sample-editor-heading">
      <button type="button" className="group-toggle" aria-expanded={open} aria-controls={bodyId} onClick={() => setOpen(!open)}>
        <span>Mẫu {String(index + 1).padStart(2, '0')}{!open && summary && <i>{summary}</i>}</span><Chevron open={open} />
      </button>
      {canRemove && <button type="button" className="table-action" onClick={onRemove}>Bỏ mẫu</button>}
    </div>
    <div id={bodyId} className="section-body" hidden={!open}>{children}</div>
  </article>;
}

// Chọn Số/No. của Phiếu yêu cầu đã lập để tự điền hồ sơ tiếp nhận (hoặc gõ tay nếu chưa có phiếu).
function RequestPicker({ value, linked, onChange, onPick, onUnlink }) {
  const listId = useId();
  const [options, setOptions] = useState([]);
  const timer = useRef(null);
  useEffect(() => {
    if (linked) return undefined;
    window.clearTimeout(timer.current);
    timer.current = window.setTimeout(() => {
      listTestRequests({ q: value, size: 20, sort: 'createdAt,desc' }).then((page) => setOptions(page.content ?? [])).catch(() => setOptions([]));
    }, 250);
    return () => window.clearTimeout(timer.current);
  }, [value, linked]);
  function handle(event) {
    onChange(event);
    const typed = normalizeRequestNumber(event.target.value).toUpperCase();
    if (options.some((item) => item.requestNumber === typed)) onPick(typed);
  }
  // Rời ô nhập: đưa về dạng chuẩn CFC26.0001 rồi tự liên kết nếu đúng một Phiếu yêu cầu có sẵn.
  function handleBlur(event) {
    const canonical = normalizeRequestNumber(event.target.value);
    if (canonical === event.target.value) return;
    onChange({ target: { value: canonical } });
    if (options.some((item) => item.requestNumber === canonical.toUpperCase())) onPick(canonical.toUpperCase());
  }
  return <label className="request-picker full-span">Số/No. Phiếu yêu cầu
    <span className="request-picker-row">
      <input value={value} onChange={handle} onBlur={handleBlur} list={listId} required readOnly={linked} placeholder="Chọn hoặc gõ số, ví dụ CFC26.0001" autoComplete="off" spellCheck="false" />
      {linked && <button type="button" className="outline-button" onClick={onUnlink}>Đổi phiếu</button>}
    </span>
    <datalist id={listId}>{options.map((item) => <option key={item.id} value={item.requestNumber}>{item.customerName} · {item.sampleName}</option>)}</datalist>
    <small className="field-help">{linked ? 'Đã liên kết — dữ liệu khách hàng, lô hàng, mẫu và chỉ tiêu được điền từ phiếu này; vẫn sửa được ở các mục bên dưới.' : 'Chọn số trong danh sách để tự điền; hoặc gõ tay nếu chưa có Phiếu yêu cầu.'}</small>
  </label>;
}
