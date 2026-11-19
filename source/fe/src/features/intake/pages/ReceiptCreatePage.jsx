import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { PhotoPicker } from '../../../shared/components/PhotoPicker';
import { createReceipt, downloadReceiptForm3, uploadReceiptFile } from '../api/intakeApi';
import { Icon } from '../../../shared/components/Icon';
import { DateInput } from '../../../shared/components/DateInput';
import { getTestRequest } from '../../sampling/api/testRequestApi';

const initialForm = {
  customerName: '', customerPhone: '', requestNumber: '', customerEmail: '', customerAddress: '', deliveredBy: '',
  sampledBy: '', sampledAt: '', growingArea: '', packingFacility: '', vehicleNumber: '',
  trailerNumber: '', containerNumber: '', sealNumber: '', containerSealNumber: '', sealStatus: 'INTACT', conditionDetail: '',
  transportInfo: '', receivedTemperature: '', storageCondition: 'Thường', temperatureOther: '', expectedDate: '',
  specialRequest: '', acceptanceDecision: 'ACCEPTED', acceptanceReason: '',
};

const newSample = () => ({
  sampleCode: '', sourceCode: 'CT001', productType: 'Sầu riêng', name: '', quantity: '', packaging: 'Túi PE',
  packagingOther: '', parameterOther: '', parameterOtherEnabled: false, condition: 'Nguyên vẹn', requestedParameters: '',
});

const parameterOptions = ['Cadimi (Cd)', 'Auramine O (Vàng ô)', 'Dư lượng thuốc BVTV'];
const packagingOptions = ['Túi PE', 'Hộp/thùng', 'Chai/lọ'];
const temperatureOptions = ['Thường', 'Lạnh', 'Khác'];

function selectedParameters(value) {
  return (value || '').split(',').map((item) => item.trim()).filter(Boolean);
}

export function ReceiptCreatePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [form, setForm] = useState(initialForm);
  const [samples, setSamples] = useState([newSample()]);
  const [files, setFiles] = useState({ form3: [], samples: [], lot: [] });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [uploadWarning, setUploadWarning] = useState('');
  const [created, setCreated] = useState(null);
  const [sourceRequest, setSourceRequest] = useState(null);

  useEffect(() => {
    const requestId = searchParams.get('requestId');
    if (!requestId) return;
    getTestRequest(requestId).then((data) => {
      setSourceRequest(data);
      const field = data.fieldSampling;
      setForm((current) => ({ ...current,
        testRequestId: data.id, fieldSamplingId: field?.id ?? null, requestNumber: data.requestNumber,
        customerName: data.customerName, customerPhone: data.customerPhone, customerEmail: data.customerEmail ?? '',
        customerAddress: data.customerAddress ?? '', sampledBy: field?.sampleCollector ?? '', sampledAt: localDateTime(field?.samplingEnd),
        growingArea: field?.growingArea ?? data.origin ?? '', packingFacility: field?.packingFacility ?? '',
        vehicleNumber: field?.vehicleNumber ?? '', trailerNumber: field?.trailerNumber ?? '', containerNumber: field?.containerNumber ?? '',
        sealNumber: field?.sampleSealNumber ?? '', sealStatus: field?.sealPresent === false ? 'NOT_PRESENT' : 'INTACT',
        conditionDetail: field?.sampleCondition ?? '', transportInfo: field?.fieldNotes ?? '', storageCondition: field?.storageCondition ?? 'Thường',
        expectedDate: data.expectedDate ?? '',
      }));
      setSamples((data.items?.length ? data.items : [{ sampleNameOrId: data.sampleName, parameters: '' }]).map((item) => ({
        ...newSample(), name: item.sampleNameOrId || data.sampleName, requestedParameters: item.parameters ?? '',
      })));
    }).catch((exception) => setError(exception.message));
  }, [searchParams]);

  useEffect(() => {
    const phoneInput = document.querySelector('.receipt-form-page input[type="tel"]');
    if (!phoneInput) return;
    phoneInput.placeholder = '090 123 4567';
    phoneInput.inputMode = 'numeric';
    phoneInput.maxLength = 11;
    phoneInput.pattern = '0[0-9]{9,10}';
    phoneInput.setAttribute('aria-describedby', 'customer-phone-help');
  });

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
    const validationMessage = validateForm(form, samples);
    if (validationMessage) {
      setError(validationMessage);
      return;
    }
    setSaving(true); setError(''); setUploadWarning('');
    try {
      const primary = samples[0];
      const { temperatureOther, ...formPayload } = form;
      const receipt = await createReceipt({
        ...formPayload,
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
      <div className="code-display"><small>MÃ PHIẾU</small><strong>{created.receiptCode}</strong>
        <small>MÃ MẪU HIỆN TẠI</small>
        {created.samples?.map((sample) => <strong key={sample.id}>{sample.sampleCode}</strong>)}
      </div>
      <p className="muted">Đã lưu {created.attachments?.length ?? 0} file/ảnh. Mở chi tiết phiếu, dán nhãn và xác nhận trước khi chuyển; KCS sẽ đối chiếu độc lập sau khi nhận.</p>
      <div className="receipt-success-actions">
        <button className="outline-button" onClick={() => downloadReceiptForm3(created.id).catch((exception) => setUploadWarning(exception.message))}>Xuất phiếu tiếp nhận</button>
        <button className="outline-button" onClick={() => navigate('/admin/intake/receipts')}>Về danh sách</button>
        <button className="primary-button" onClick={() => { setForm(initialForm); setSamples([newSample()]); setFiles({ form3: [], samples: [], lot: [] }); setCreated(null); }}>Tạo phiếu khác</button>
      </div>
    </div>
  </section>;

  return <section className="staff-page receipt-form-page">
    <header className="admin-heading staff-heading"><div>
      <button className="back-link" onClick={() => navigate('/admin/intake/receipts')}>← Quay lại danh sách</button>
      <p className="section-kicker">Vận hành / Tiếp nhận mẫu</p><h1>Tạo hồ sơ tiếp nhận</h1>
    </div></header>
    {sourceRequest && <div className="source-banner"><strong>Đang tạo từ yêu cầu: {sourceRequest.requestNumber}</strong><span>Dữ liệu đã được điền sẵn từ hồ sơ nguồn. Hãy đối chiếu thực tế trước khi lưu.</span></div>}
    {error && <div className="toast toast-error" role="alert">{error}</div>}
    {!form.customerPhone && <p id="customer-phone-help" className="form-example-guide" role="note">Ghi nhận đầy đủ người nhận, nguồn mẫu, từng mẫu và bằng chứng ảnh.</p>}
    <form className="receipt-form-layout" noValidate onSubmit={handleSubmit}>
      <div className="receipt-form-main">
        <section className="panel-card intake-form-card"><FormHeading number="01" title="Người giao và người lấy mẫu" copy="Tài khoản đang đăng nhập được lưu tự động là người tiếp nhận." />
          <div className="form-two-columns">
            <label>Họ tên người giao<input value={form.deliveredBy} onChange={update('deliveredBy')} required /></label>
            <label>Số điện thoại<input type="tel" value={form.customerPhone} onChange={update('customerPhone')} required /></label>
            <label>Số phiếu yêu cầu<input value={form.requestNumber} onChange={update('requestNumber')} required readOnly={Boolean(sourceRequest)} /></label>
            <label>Tên khách hàng / đơn vị<input value={form.customerName} onChange={update('customerName')} required /></label>
            <label>Email<input type="email" value={form.customerEmail} onChange={update('customerEmail')} /></label>
            <label className="full-span">Địa chỉ<input value={form.customerAddress} onChange={update('customerAddress')} /></label>
            <label>Người lấy mẫu<input value={form.sampledBy} onChange={update('sampledBy')} placeholder="Nếu khác người giao" /></label>
            <label>Thời điểm lấy mẫu<input type="datetime-local" value={form.sampledAt} onChange={update('sampledAt')} /></label>
          </div>
        </section>

        <section className="panel-card intake-form-card"><FormHeading number="02" title="Hồ sơ nguồn và lô hàng" copy="Dữ liệu có cấu trúc để truy xuất và đưa vào hồ sơ kết quả sau này." />
          <div className="form-two-columns">
            <label>Vùng trồng / nguồn mẫu<input value={form.growingArea} onChange={update('growingArea')} /></label>
            <label>Cơ sở đóng gói<input value={form.packingFacility} onChange={update('packingFacility')} /></label>
            <label>Biển số xe<input value={form.vehicleNumber} onChange={update('vehicleNumber')} /></label>
            <label>Số moóc<input value={form.trailerNumber} onChange={update('trailerNumber')} /></label>
            <label>Số container<input value={form.containerNumber} onChange={update('containerNumber')} /></label>
            <label>Số niêm phong mẫu<input value={form.sealNumber} onChange={update('sealNumber')} /></label>
            <label>Số seal container<input value={form.containerSealNumber} onChange={update('containerSealNumber')} /></label>
            <label>Trạng thái niêm phong<select value={form.sealStatus} onChange={update('sealStatus')} required><option value="INTACT">Còn nguyên</option><option value="NOT_PRESENT">Không có</option><option value="OPENED">Đã mở</option></select></label>
            <label className="full-span">Phương tiện / điều kiện vận chuyển<input value={form.transportInfo} onChange={update('transportInfo')} /></label>
          </div>
        </section>

        <section className="panel-card intake-form-card"><FormHeading number="03" title="Danh sách mẫu" copy="Một phiếu tiếp nhận có thể chứa nhiều mẫu độc lập." />
          <div className="intake-sample-editor">
            {samples.map((sample, index) => <article className="sample-editor-card" key={index}>
              <div className="sample-editor-heading"><strong>Mẫu {String(index + 1).padStart(2, '0')}</strong>
                {samples.length > 1 && <button type="button" className="table-action" onClick={() => setSamples((current) => current.filter((_, itemIndex) => itemIndex !== index))}>Bỏ mẫu</button>}
              </div>
              <div className="form-two-columns">
                <label>Mã mẫu do người tiếp nhận nhập<input value={sample.sampleCode} onChange={updateSample(index, 'sampleCode')} placeholder="Ví dụ: CT001-SR-19112026-001" required /></label>
                <label>Tên mẫu<input value={sample.name} onChange={updateSample(index, 'name')} required /></label>
                <label>Mã nguồn<input value={sample.sourceCode} onChange={updateSample(index, 'sourceCode')} required /></label>
                <label>Số lượng / khối lượng<input value={sample.quantity} onChange={updateSample(index, 'quantity')} /></label>
                <label>Loại bao bì<select value={packagingOptions.includes(sample.packaging) ? sample.packaging : sample.packaging ? 'Khác' : ''} onChange={updateSample(index, 'packaging')}><option value="">Chọn loại bao bì</option>{packagingOptions.map((option) => <option value={option} key={option}>{option}</option>)}<option value="Khác">Khác</option></select></label>
                {sample.packaging === 'Khác' && <label>Loại bao bì khác<input value={sample.packagingOther} onChange={updateSample(index, 'packagingOther')} placeholder="Nhập loại bao bì" required /></label>}
                <label>Tình trạng<input value={sample.condition} onChange={updateSample(index, 'condition')} /></label>
                <fieldset className="full-span choice-fieldset">
                  <legend>Chỉ tiêu yêu cầu</legend>
                  <div className="choice-grid">
                    {parameterOptions.map((option) => <label className="choice-option" key={option}>
                      <input type="checkbox" checked={selectedParameters(sample.requestedParameters).includes(option)} onChange={() => setSamples((current) => current.map((item, itemIndex) => {
                        if (itemIndex !== index) return item;
                        const currentValues = selectedParameters(item.requestedParameters);
                        const next = currentValues.includes(option) ? currentValues.filter((value) => value !== option) : [...currentValues, option];
                        return { ...item, requestedParameters: next.join(', ') };
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
            </article>)}
          </div>
          <button type="button" className="outline-button add-sample-button" onClick={() => setSamples((current) => [...current, newSample()])}>＋ Thêm mẫu vào phiếu</button>
        </section>

        <section className="panel-card intake-form-card"><FormHeading number="04" title="Điều kiện và kết luận tiếp nhận" copy="Ghi nhận tình trạng chung của lô tại thời điểm nhận." />
          <div className="form-two-columns">
            <label>Nhiệt độ / điều kiện khi nhận<select value={temperatureOptions.includes(form.storageCondition) ? form.storageCondition : 'Khác'} onChange={update('storageCondition')}><option value="">Chọn điều kiện</option>{temperatureOptions.map((option) => <option value={option} key={option}>{option}</option>)}</select></label>
            {form.storageCondition === 'Khác' && <label>Điều kiện khác<input value={form.temperatureOther} onChange={update('temperatureOther')} placeholder="Nhập điều kiện" required /></label>}
            <label>Nhiệt độ cụ thể (°C)<input type="number" step="0.1" value={form.receivedTemperature} onChange={update('receivedTemperature')} placeholder="Chỉ nhập khi cần" /></label>
            <label>Thời hạn dự kiến<DateInput value={form.expectedDate} onChange={update('expectedDate')} /></label>
            <label className="full-span">Mô tả tình trạng<textarea value={form.conditionDetail} onChange={update('conditionDetail')} rows="3" /></label>
            <label className="full-span">Yêu cầu đặc biệt<textarea value={form.specialRequest} onChange={update('specialRequest')} rows="3" /></label>
          </div>
          <div className="decision-grid">{[['ACCEPTED', 'Chấp nhận'], ['ACCEPTED_WITH_CONDITION', 'Chấp nhận có điều kiện'], ['REJECTED', 'Từ chối']].map(([value, label]) => <label className={`decision-option ${form.acceptanceDecision === value ? 'selected' : ''}`} key={value}><input type="radio" name="acceptanceDecision" value={value} checked={form.acceptanceDecision === value} onChange={update('acceptanceDecision')} /><span><strong>{label}</strong><small>{value === 'ACCEPTED' ? 'Đủ điều kiện tiếp nhận' : 'Bắt buộc ghi lý do/điều kiện'}</small></span></label>)}</div>
          {form.acceptanceDecision !== 'ACCEPTED' && <label className="reason-field">Lý do / điều kiện<textarea value={form.acceptanceReason} onChange={update('acceptanceReason')} rows="3" required /></label>}
        </section>
      </div>

      <aside className="receipt-form-aside">
        <section className="panel-card upload-card"><FormHeading number="05" title="Ảnh và hồ sơ" copy="Ảnh được phân loại và ghi nhận người tải lên." />
          <PhotoPicker label="Biên bản tiếp nhận" hint="PDF, DOCX hoặc ảnh chụp phiếu" camera accept="application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,image/*" files={files.form3} onChange={updateFiles('form3')} />
          <PhotoPicker label="Ảnh mẫu" hint="Chụp trực tiếp hoặc chọn nhiều ảnh" files={files.samples} onChange={updateFiles('samples')} />
          <PhotoPicker label="Ảnh lô hàng" hint="Toàn cảnh lô/xe/container khi nhận" files={files.lot} onChange={updateFiles('lot')} />
          <small className="field-help">Tối đa 10 MB mỗi file. Hỗ trợ PDF, DOCX, JPG, PNG, WEBP, HEIC/HEIF.</small>
        </section>
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
  if (samples.some((sample) => !sample.sampleCode.trim() || !sample.name.trim()
    || !sample.sourceCode.trim()
    || (!sample.requestedParameters.trim() && !(sample.parameterOtherEnabled && sample.parameterOther.trim()))
    || (sample.parameterOtherEnabled && !sample.parameterOther.trim())
    || (sample.packaging === 'Khác' && !sample.packagingOther.trim()))) {
    return 'Mỗi mẫu cần có mã mẫu, tên mẫu, mã nguồn, chỉ tiêu yêu cầu và loại bao bì.';
  }
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

function FormHeading({ number, title, copy }) {
  return <div className="form-card-heading"><span className="form-number">{number}</span><div><h2>{title}</h2><p>{copy}</p></div></div>;
}
