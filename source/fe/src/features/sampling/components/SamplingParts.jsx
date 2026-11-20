import { DateInput, DateTimeInput } from '../../../shared/components/DateInput';
import { useId, useState } from 'react';
import { CONDITIONS, FIELD_PARAMETERS, LOT_BANDS, SAMPLE_FORMS, TRANSPORT_MODES, csvList, emptyFieldItem, parseCondition, toggleCsv } from '../samplingModel';

export function Heading({ number, title, copy }) { return <div className="form-card-heading"><span className="form-number">{number}</span><div><h2>{title}</h2><p>{copy}</p></div></div>; }
export function Chevron({ open }) { return <svg className={`chevron${open ? ' is-open' : ''}`} viewBox="0 0 20 20" width="18" height="18" aria-hidden="true" focusable="false"><path d="M5 7.5l5 5 5-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" /></svg>; }
export function Group({ title, en, children, defaultOpen = true }) {
  const [open, setOpen] = useState(defaultOpen); const bodyId = useId();
  return <section className="form-group"><button type="button" className="group-toggle" aria-expanded={open} aria-controls={bodyId} onClick={() => setOpen(!open)}><span>{title}{en && <i>{en}</i>}</span><Chevron open={open} /></button><div id={bodyId} className="section-body" hidden={!open}>{children}</div></section>;
}
export function Linked({ label, value, wide }) { return <label className={wide ? 'full-span' : ''}>{label}<input value={value || ''} readOnly aria-readonly="true" className="readonly-field" placeholder="Theo Phiếu yêu cầu" /></label>; }
export function Radio({ name, label, checked, onChange }) { return <label className="checkbox-line"><input type="radio" name={name} checked={checked} onChange={onChange} /><span>{label}</span></label>; }
export function Actions({ onReseed, reseedLabel, onSave, saving, saveLabel }) { return <div className="optional-form-actions"><button type="button" className="outline-button" onClick={onReseed}>{reseedLabel}</button><button type="button" className="primary-button" disabled={saving} onClick={onSave}>{saving ? 'Đang lưu…' : saveLabel}</button></div>; }

// Phần "Thông tin chung" dùng chung cho 07 và 02: dữ liệu khách lấy từ Phiếu 1 (chỉ đọc), phần còn lại nhập.
export function GeneralInfo({ form, data, update, withEmail, collectorLabel = 'Người lấy mẫu / Sample collector' }) {
  return <Group title="I. Thông tin chung" en="General information"><div className="form-two-columns">
    <Linked label="Số/No. (theo Phiếu yêu cầu)" value={data.formNumber || form.requestNumber} />
    <Field type="date" label="Ngày lập / Date" value={data.formDate} onChange={(e) => update({ formDate: e.target.value })} required />
    <Linked label="Đơn vị / Khách hàng" value={form.customerName} wide />
    <Linked label="Địa chỉ / Address" value={form.customerAddress} wide />
    <Field label="Người đại diện / Representative" value={data.representative} onChange={(e) => update({ representative: e.target.value })} />
    <Field label="Điện thoại người đại diện" value={data.representativePhone} onChange={(e) => update({ representativePhone: e.target.value })} />
    <Linked label="Số giấy đề nghị / Request No." value={form.requestNumber} />
    {withEmail ? <Linked label="Email" value={form.customerEmail} /> : <Linked label="Hạn trả kết quả dự kiến" value={form.expectedDate ? new Date(`${form.expectedDate}T00:00:00`).toLocaleDateString('vi-VN') : ''} />}
    <Field label={collectorLabel} value={data.sampleCollector} onChange={(e) => update({ sampleCollector: e.target.value })} required />
    <Field label="Điện thoại người lấy mẫu" value={data.collectorPhone} onChange={(e) => update({ collectorPhone: e.target.value })} />
    <Field type="datetime-local" label="Bắt đầu lấy mẫu / Sampling from" value={data.samplingStart} onChange={(e) => update({ samplingStart: e.target.value })} />
    <Field type="datetime-local" label="Kết thúc / to" value={data.samplingEnd} onChange={(e) => update({ samplingEnd: e.target.value })} />
    <Field label="Địa điểm lấy mẫu / Sampling location" value={data.samplingLocation} onChange={(e) => update({ samplingLocation: e.target.value })} wide />
  </div></Group>;
}
export function ConsignmentInfo({ form, data, update }) {
  return <Group title="II. Thông tin lô hàng" en="Consignment information"><div className="form-two-columns">
    <Linked label="Tên mẫu / Name of sample" value={form.sampleName} />
    <Linked label="Giống / Variety" value={form.variety} />
    <Field label="Mã vùng trồng / Growing area code" value={data.growingArea} onChange={(e) => update({ growingArea: e.target.value })} />
    <Field label="Mã cơ sở đóng gói / Packing code" value={data.packingFacility} onChange={(e) => update({ packingFacility: e.target.value })} />
    <Field label="Số xe đầu kéo" value={data.vehicleNumber} onChange={(e) => update({ vehicleNumber: e.target.value })} />
    <Field label="Số rơ moóc" value={data.trailerNumber} onChange={(e) => update({ trailerNumber: e.target.value })} />
    <Field label="Số container" value={data.containerNumber} onChange={(e) => update({ containerNumber: e.target.value })} />
  </div></Group>;
}

export function PlanForm({ form, data, update, onSave, onReseed, saving }) {
  const forms = csvList(data.sampleForms); const modes = csvList(data.transportModes);
  return <section className="panel-card intake-form-card sampling-form-section optional-form">
    <Heading number="07" title="Kế hoạch lấy mẫu" copy="Sampling plan — có thể lập trước thông tin lấy mẫu hiện trường hoặc bỏ qua để chuyển thẳng tới tiếp nhận." />
    <GeneralInfo form={form} data={data} update={update} collectorLabel="Người lấy mẫu (QLCLSP) / Sample collector" />
    <ConsignmentInfo form={form} data={data} update={update} />
    <Group title="III. Phương án lấy mẫu" en="Sampling scheme">
      <p className="field-hint">Dạng mẫu lấy (tick một hoặc nhiều)</p>
      <div className="scheme-options">{SAMPLE_FORMS.map((item) => <label className="checkbox-line scheme-option" key={item.code}><input type="checkbox" checked={forms.includes(item.code)} onChange={() => update({ sampleForms: toggleCsv(data.sampleForms, item.code) })} /><span><strong>{item.label}</strong><small>Chỉ tiêu: {item.parameters} · {item.mass}</small><small>{item.note}</small></span></label>)}</div>
      <p className="field-hint">Cỡ lô (khối lượng) → số trái lấy</p>
      <div className="scheme-options">{LOT_BANDS.map((item) => <label className="checkbox-line scheme-option" key={item.code}><input type="radio" name="lotSizeBand" checked={data.lotSizeBand === item.code} onChange={() => update({ lotSizeBand: item.code })} /><span><strong>{item.label}: {item.count}</strong><small>{item.note}</small></span></label>)}</div>
      <label className="full-span">Ghi chú phương án (nếu có)<input value={data.schemeNotes} onChange={(e) => update({ schemeNotes: e.target.value })} /></label>
    </Group>
    <Group title="IV–V. Điều kiện vận chuyển và thời hạn giao về phòng" en="Transport conditions">
      <div className="scheme-options">{TRANSPORT_MODES.map((item) => <label className="checkbox-line scheme-option" key={item.code}><input type="checkbox" checked={modes.includes(item.code)} onChange={() => update({ transportModes: toggleCsv(data.transportModes, item.code) })} /><span><strong>{item.label}</strong><small>{item.condition} · {item.deadline}</small></span></label>)}</div>
      <label className="full-span">Ghi chú vận chuyển (nếu có)<input value={data.transportConditions} onChange={(e) => update({ transportConditions: e.target.value })} /></label>
      <details className="reference-note"><summary>Dụng cụ, chống nhiễm bẩn, bao gói và niêm phong (theo mẫu 07)</summary>
        <ul><li>Dụng cụ: dao inox/gốm sạch, thớt nhựa PP, găng nitrile không bột, túi PE/zip PE hoặc hộp PP sạch, thùng giữ lạnh, đá/gel lạnh, nhiệt kế, cân.</li><li>Ghi chép: bút mực không phai (đen/xanh), nhãn, dây/tem niêm phong có số seri, Phiếu lấy mẫu hiện trường, máy ảnh.</li><li>Chống nhiễm bẩn: không dùng bút dạ quang, bút/nhãn/băng keo/bao bì màu vàng tiếp xúc với mẫu; thay găng giữa các mẫu; vỏ và ruột đóng gói riêng; không để mẫu gần hóa chất, phân bón, chất tẩy.</li><li>Niêm phong bằng tem có số seri, ghi vào Phiếu lấy mẫu hiện trường; tem còn nguyên khi tiếp nhận là điều kiện coi là mẫu do Công ty lấy.</li></ul></details>
    </Group>
    <Actions onReseed={onReseed} reseedLabel="Lấy lại từ Phiếu yêu cầu" onSave={onSave} saving={saving} saveLabel="Lưu kế hoạch lấy mẫu" />
  </section>;
}

export function FieldForm({ form, data, update, onSave, onReseed, saving, reseedLabel = 'Lấy lại từ Kế hoạch lấy mẫu' }) {
  const condition = parseCondition(data.sampleCondition);
  const setItem = (index, patch) => update({ items: data.items.map((item, position) => position === index ? { ...item, ...patch } : item) });
  return <section className="panel-card intake-form-card sampling-form-section optional-form">
    <Heading number="02" title="Lấy mẫu hiện trường" copy="Field sampling form — ghi thông tin thực tế và niêm phong mẫu; có thể chuyển thẳng tới tiếp nhận nếu phù hợp." />
    <GeneralInfo form={form} data={data} update={update} withEmail />
    <ConsignmentInfo form={form} data={data} update={update} />
    <Group title="Tình trạng, bảo quản và niêm phong" en="Condition, storage and seal"><div className="field-status-grid">
      <div><span className="group-label">Tình trạng mẫu / Sample condition</span><div className="choice-row">
        {CONDITIONS.map((item) => <Radio key={item} name="sampleCondition" label={item} checked={condition.choice === item} onChange={() => update({ sampleCondition: item })} />)}
        <Radio name="sampleCondition" label="Khác" checked={condition.choice === 'Khác'} onChange={() => update({ sampleCondition: 'Khác: ' })} /></div>
        {condition.choice === 'Khác' && <input aria-label="Tình trạng mẫu khác" value={condition.other} onChange={(e) => update({ sampleCondition: `Khác: ${e.target.value}` })} placeholder="Ghi rõ tình trạng" />}</div>
      <div><span className="group-label">Bảo quản / Storage</span><div className="choice-row">
        <Radio name="storageCondition" label="Thường" checked={data.storageCondition === 'Thường'} onChange={() => update({ storageCondition: 'Thường', storageTemperature: '' })} />
        <Radio name="storageCondition" label="Lạnh" checked={data.storageCondition === 'Lạnh'} onChange={() => update({ storageCondition: 'Lạnh' })} /></div>
        {data.storageCondition === 'Lạnh' && <input aria-label="Nhiệt độ bảo quản (°C)" inputMode="decimal" value={data.storageTemperature} onChange={(e) => update({ storageTemperature: e.target.value })} placeholder="Nhiệt độ °C" />}</div>
      <div><span className="group-label">Niêm phong / Seal</span><div className="choice-row">
        <Radio name="sealPresent" label="Có, số" checked={data.sealPresent} onChange={() => update({ sealPresent: true })} />
        <Radio name="sealPresent" label="Không" checked={!data.sealPresent} onChange={() => update({ sealPresent: false, sampleSealNumber: '' })} /></div>
        {data.sealPresent && <input aria-label="Số niêm phong mẫu" value={data.sampleSealNumber} onChange={(e) => update({ sampleSealNumber: e.target.value })} placeholder="Số niêm phong" />}</div>
    </div></Group>
    <Group title="III. Thông tin mẫu thử nghiệm" en="Sample details">
      <div className="request-items">{data.items.map((item, index) => <article className="request-item" key={index}>
        <div className="sample-editor-heading"><strong>TT {index + 1}</strong>{data.items.length > 1 && <button type="button" className="table-action" onClick={() => update({ items: data.items.filter((_, position) => position !== index) })}>Xóa</button>}</div>
        <div className="form-two-columns">
          <Field label="Tên mẫu / Name of sample" value={item.sampleName} onChange={(e) => setItem(index, { sampleName: e.target.value })} />
          <Field label="Khối lượng mẫu (quả/kg)" value={item.quantity} onChange={(e) => setItem(index, { quantity: e.target.value })} />
          <Field label="Phương pháp lấy mẫu / Sampling method" value={item.samplingMethod} onChange={(e) => setItem(index, { samplingMethod: e.target.value })} wide />
        </div>
        <div className="parameter-checks" role="group" aria-label={`Chỉ tiêu thử nghiệm mẫu ${index + 1}`}><span className="group-label">Chỉ tiêu thử nghiệm / Parameters</span>
          {FIELD_PARAMETERS.map((param) => <Check key={param.code} label={param.label} checked={csvList(item.parameters).includes(param.code)} onChange={() => setItem(index, { parameters: toggleCsv(item.parameters, param.code) })} />)}
          <input aria-label="Chỉ tiêu khác" value={item.parametersOther} onChange={(e) => setItem(index, { parametersOther: e.target.value })} placeholder="Khác: ghi rõ" /></div>
      </article>)}</div>
      <button type="button" className="outline-button" onClick={() => update({ items: [...data.items, emptyFieldItem()] })}>＋ Thêm mẫu</button>
    </Group>
    <Group title="IV. Ghi chú hiện trường" en="Field notes"><label className="full-span">Ghi chú<textarea rows={3} value={data.fieldNotes} onChange={(e) => update({ fieldNotes: e.target.value })} /></label></Group>
    <Actions onReseed={onReseed} reseedLabel={reseedLabel} onSave={onSave} saving={saving} saveLabel="Lưu Phiếu lấy mẫu hiện trường" />
  </section>;
}

export function FormSection({ number, title, en, copy, children, defaultOpen = true }) {
  const [open, setOpen] = useState(defaultOpen); const bodyId = useId();
  return <section className="panel-card intake-form-card sampling-form-section"><button type="button" className="section-toggle" aria-expanded={open} aria-controls={bodyId} onClick={() => setOpen(!open)}><span className="form-number">{number}</span><span className="section-toggle-text"><strong>{title}{en && <i>{en}</i>}</strong>{copy && <small>{copy}</small>}</span><Chevron open={open} /></button><div id={bodyId} className="section-body" hidden={!open}>{children}</div></section>;
}
export function Field({ label, wide, type, ...props }) {
  // Ngày luôn dd/mm/yyyy, giờ 24h, không phụ thuộc ngôn ngữ trình duyệt.
  const control = type === 'date' ? <DateInput {...props} /> : type === 'datetime-local' ? <DateTimeInput {...props} /> : <input type={type} {...props} />;
  return <label className={wide ? 'full-span' : ''}>{label}{control}</label>;
}
export function Check({ label, checked, onChange }) { return <label className="checkbox-line"><input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} /><span>{label}</span></label>; }

