import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createReceipt, uploadReceiptFile } from '../api/intakeApi';

const initialForm = {
  customerName: '', customerPhone: '', customerEmail: '', customerAddress: '', deliveredBy: '',
  sampledBy: '', sampledAt: '', growingArea: '', packingFacility: '', vehicleNumber: '',
  trailerNumber: '', containerNumber: '', sealNumber: '', sealIntact: true, conditionDetail: '',
  transportInfo: '', receivedTemperature: '', storageCondition: 'Thường', expectedDate: '',
  specialRequest: '', acceptanceDecision: 'ACCEPTED', acceptanceReason: '',
};

const newSample = () => ({
  sourceCode: 'CT001', productType: 'Sầu riêng', name: '', quantity: '', packaging: 'Túi PE',
  condition: 'Nguyên vẹn', requestedParameters: '',
});

export function ReceiptCreatePage() {
  const navigate = useNavigate();
  const [form, setForm] = useState(initialForm);
  const [samples, setSamples] = useState([newSample()]);
  const [files, setFiles] = useState({ form3: [], samples: [], lot: [] });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [uploadWarning, setUploadWarning] = useState('');
  const [created, setCreated] = useState(null);

  const update = (field) => (event) => setForm((current) => ({
    ...current,
    [field]: event.target.type === 'checkbox' ? event.target.checked : event.target.value,
  }));
  const updateSample = (index, field) => (event) => setSamples((current) => current.map((sample, itemIndex) => (
    itemIndex === index ? { ...sample, [field]: event.target.value } : sample
  )));
  const updateFiles = (group) => (event) => setFiles((current) => ({
    ...current,
    [group]: Array.from(event.target.files ?? []),
  }));

  async function handleSubmit(event) {
    event.preventDefault();
    setSaving(true); setError(''); setUploadWarning('');
    try {
      const primary = samples[0];
      const receipt = await createReceipt({
        ...form,
        sampledAt: form.sampledAt ? new Date(form.sampledAt).toISOString() : null,
        receivedTemperature: form.receivedTemperature ? Number(form.receivedTemperature) : null,
        expectedDate: form.expectedDate || null,
        sampleName: primary.name,
        quantity: primary.quantity,
        packaging: primary.packaging,
        sampleCondition: primary.condition,
        requestedParameters: primary.requestedParameters,
        samples,
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
      <span className="success-check">✓</span><p className="section-kicker">Đã lưu hồ sơ tiếp nhận</p>
      <h1>Phiếu đã được tạo</h1>
      <p>Người tiếp nhận, hồ sơ nguồn, danh sách mẫu và ảnh hiện trường đã được gắn vào cùng một hồ sơ.</p>
      {uploadWarning && <p className="form-error" role="alert">{uploadWarning}</p>}
      <div className="code-display"><small>MÃ PHIẾU</small><strong>{created.receiptCode}</strong>
        <small>MÃ MẪU HIỆN TẠI</small>
        {created.samples?.map((sample) => <strong key={sample.id}>{sample.sampleCode}</strong>)}
      </div>
      <p className="muted">Đã lưu {created.attachments?.length ?? 0} file/ảnh. Mã mẫu hiện tại chưa mang ý nghĩa “đã xác minh hai người”; Phase 2 sẽ bổ sung cổng xác minh này.</p>
      <div className="receipt-success-actions">
        <button className="outline-button" onClick={() => navigate('/admin/intake')}>Về danh sách</button>
        <button className="primary-button" onClick={() => { setForm(initialForm); setSamples([newSample()]); setFiles({ form3: [], samples: [], lot: [] }); setCreated(null); }}>Tạo phiếu khác</button>
      </div>
    </div>
  </section>;

  return <section className="staff-page receipt-form-page">
    <header className="admin-heading staff-heading"><div>
      <button className="back-link" onClick={() => navigate('/admin/intake')}>← Quay lại danh sách</button>
      <p className="section-kicker">Vận hành / Tiếp nhận mẫu</p><h1>Tạo phiếu tiếp nhận</h1>
      <p className="heading-copy">Ghi nhận đầy đủ người nhận, nguồn mẫu, từng mẫu và bằng chứng ảnh theo Phiếu số 3.</p>
    </div></header>
    {error && <p className="form-error form-page-error" role="alert">{error}</p>}
    <form className="receipt-form-layout" onSubmit={handleSubmit}>
      <div className="receipt-form-main">
        <section className="panel-card intake-form-card"><FormHeading number="01" title="Người giao và người lấy mẫu" copy="Tài khoản đang đăng nhập được lưu tự động là người tiếp nhận." />
          <div className="form-two-columns">
            <label>Họ tên người giao<input value={form.deliveredBy} onChange={update('deliveredBy')} required /></label>
            <label>Số điện thoại<input type="tel" value={form.customerPhone} onChange={update('customerPhone')} required /></label>
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
            <label>Số niêm phong<input value={form.sealNumber} onChange={update('sealNumber')} /></label>
            <label className="check-field"><input type="checkbox" checked={form.sealIntact} onChange={update('sealIntact')} /> Niêm phong còn nguyên</label>
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
                <label>Tên mẫu<input value={sample.name} onChange={updateSample(index, 'name')} required /></label>
                <label>Loại sản phẩm<input list="product-type-suggestions" maxLength="80" value={sample.productType} onChange={updateSample(index, 'productType')} placeholder="Nhập loại sản phẩm" required /></label>
                <label>Mã nguồn<input value={sample.sourceCode} onChange={updateSample(index, 'sourceCode')} required /></label>
                <label>Số lượng / khối lượng<input value={sample.quantity} onChange={updateSample(index, 'quantity')} /></label>
                <label>Bao bì<input value={sample.packaging} onChange={updateSample(index, 'packaging')} /></label>
                <label>Tình trạng<input value={sample.condition} onChange={updateSample(index, 'condition')} /></label>
                <label className="full-span">Chỉ tiêu yêu cầu<input value={sample.requestedParameters} onChange={updateSample(index, 'requestedParameters')} required /></label>
              </div>
            </article>)}
          </div>
          <button type="button" className="outline-button add-sample-button" onClick={() => setSamples((current) => [...current, newSample()])}>＋ Thêm mẫu vào phiếu</button>
          <datalist id="product-type-suggestions"><option value="Sầu riêng" /><option value="Mít" /><option value="Thanh long" /><option value="Xoài" /><option value="Nông sản khác" /></datalist>
        </section>

        <section className="panel-card intake-form-card"><FormHeading number="04" title="Điều kiện và kết luận tiếp nhận" copy="Ghi nhận tình trạng chung của lô tại thời điểm nhận." />
          <div className="form-two-columns">
            <label>Nhiệt độ khi nhận (°C)<input type="number" step="0.1" value={form.receivedTemperature} onChange={update('receivedTemperature')} /></label>
            <label>Điều kiện bảo quản<input value={form.storageCondition} onChange={update('storageCondition')} /></label>
            <label>Thời hạn dự kiến<input type="date" value={form.expectedDate} onChange={update('expectedDate')} /></label>
            <label className="full-span">Mô tả tình trạng<textarea value={form.conditionDetail} onChange={update('conditionDetail')} rows="3" /></label>
            <label className="full-span">Yêu cầu đặc biệt<textarea value={form.specialRequest} onChange={update('specialRequest')} rows="3" /></label>
          </div>
          <div className="decision-grid">{[['ACCEPTED', 'Chấp nhận'], ['ACCEPTED_WITH_CONDITION', 'Chấp nhận có điều kiện'], ['REJECTED', 'Từ chối']].map(([value, label]) => <label className={`decision-option ${form.acceptanceDecision === value ? 'selected' : ''}`} key={value}><input type="radio" name="acceptanceDecision" value={value} checked={form.acceptanceDecision === value} onChange={update('acceptanceDecision')} /><span><strong>{label}</strong><small>{value === 'ACCEPTED' ? 'Đủ điều kiện tiếp nhận' : 'Bắt buộc ghi lý do/điều kiện'}</small></span></label>)}</div>
          {form.acceptanceDecision !== 'ACCEPTED' && <label className="reason-field">Lý do / điều kiện<textarea value={form.acceptanceReason} onChange={update('acceptanceReason')} rows="3" required /></label>}
        </section>
      </div>

      <aside className="receipt-form-aside">
        <section className="panel-card upload-card"><FormHeading number="05" title="Ảnh và hồ sơ" copy="Ảnh được phân loại và ghi nhận người tải lên." />
          <FilePicker label="Phiếu số 3" copy="PDF, DOCX hoặc ảnh" accept="application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,image/*" files={files.form3} onChange={updateFiles('form3')} />
          <FilePicker label="Ảnh mẫu" copy="Có thể chụp trực tiếp bằng điện thoại" accept="image/*" capture="environment" multiple files={files.samples} onChange={updateFiles('samples')} />
          <FilePicker label="Ảnh lô hàng" copy="Toàn cảnh lô/xe/container khi nhận" accept="image/*" capture="environment" multiple files={files.lot} onChange={updateFiles('lot')} />
          <small className="field-help">Tối đa 10 MB mỗi file. Hỗ trợ PDF, DOCX, JPG, PNG, WEBP, HEIC/HEIF.</small>
        </section>
        <div className="form-submit-panel"><p><strong>Kiểm tra trước khi lưu</strong><span>Mã hiện được sinh theo CFC-TN-14; xác minh độc lập sẽ được bổ sung ở Phase 2.</span></p><button className="primary-button" type="submit" disabled={saving}>{saving ? 'Đang lưu hồ sơ…' : 'Lưu phiếu tiếp nhận'}</button></div>
      </aside>
    </form>
  </section>;
}

function FormHeading({ number, title, copy }) {
  return <div className="form-card-heading"><span className="form-number">{number}</span><div><h2>{title}</h2><p>{copy}</p></div></div>;
}

function FilePicker({ label, copy, files, multiple = false, ...inputProps }) {
  return <label className="file-drop compact-file-drop"><input type="file" multiple={multiple} {...inputProps} /><span className="upload-symbol">↑</span><strong>{label}</strong><small>{files.length ? `${files.length} file đã chọn` : copy}</small></label>;
}
