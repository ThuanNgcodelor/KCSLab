import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { createTestRequest, getNextRequestNumber, getTestRequest, updateTestRequest } from '../api/testRequestApi';
import { PageLoader } from '../../../shared/components/Loading';
import { normalizeRequestNumber } from '../../../shared/requestNumber';
import { Check, Field, FormSection, Radio } from '../components/SamplingParts';
import { AURAMINE, CADMIUM, DELAY_CHANNELS, FLESH, PURPOSES, SKIN, buildChecklist, emptyRequest, emptySample, parseChecklist, requestFrom, requestPayload } from '../samplingModel';

export function TestRequestFormPage() {
  const { id } = useParams(); const editing = Boolean(id); const navigate = useNavigate();
  const [form, setForm] = useState(emptyRequest); const [progress, setProgress] = useState({ plan: false, field: false });
  const [loading, setLoading] = useState(editing); const [saving, setSaving] = useState(''); const [error, setError] = useState(''); const [message, setMessage] = useState('');

  useEffect(() => {
    if (editing) return undefined;
    let active = true;
    getNextRequestNumber().then((data) => { if (active) setForm((current) => current.requestNumber ? current : { ...current, requestNumber: data.requestNumber }); }).catch(() => {});
    return () => { active = false; };
  }, [editing]);

  useEffect(() => {
    if (!editing) return;
    getTestRequest(id).then((data) => { setForm(requestFrom(data)); setProgress({ plan: Boolean(data.samplingPlan), field: Boolean(data.fieldSampling) }); }).catch((e) => setError(e.message)).finally(() => setLoading(false));
  }, [editing, id]);

  const change = (setter, key) => (e) => setter((current) => ({ ...current, [key]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }));
  const sampleChange = (index, patch) => setForm((current) => ({ ...current, samples: current.samples.map((sample, position) => position === index ? patch(sample) : sample) }));

  async function refreshNumber() { setError(''); try { const data = await getNextRequestNumber(); setForm((current) => ({ ...current, requestNumber: data.requestNumber })); } catch (e) { setError(e.message); } }

  async function saveRequest(event) {
    event.preventDefault(); setSaving('request'); setError(''); setMessage('');
    try {
      const payload = requestPayload(form);
      if (!payload.items.length) throw new Error('Chọn ít nhất một chỉ tiêu và nền mẫu ở mục Nền mẫu, chỉ tiêu và phương pháp.');
      const result = editing ? await updateTestRequest(id, payload) : await createTestRequest(payload);
      setMessage('Đã lưu Phiếu yêu cầu kiểm nghiệm.');
      if (!editing) navigate(`/admin/requests/${result.id}`, { replace: true });
      else { setForm(requestFrom(result)); setProgress({ plan: Boolean(result.samplingPlan), field: Boolean(result.fieldSampling) }); }
    } catch (e) { setError(e.message); } finally { setSaving(''); }
  }

  if (loading) return <section className="staff-page"><PageLoader label="Đang tải Phiếu yêu cầu kiểm nghiệm…" /></section>;
  return <section className="staff-page sampling-page form-page">
    <header className="admin-heading staff-heading"><div><button className="back-link" onClick={() => navigate('/admin/requests')}>← Danh sách yêu cầu kiểm nghiệm</button><p className="section-kicker">Yêu cầu kiểm nghiệm</p><h1>{editing ? form.requestNumber || 'Yêu cầu kiểm nghiệm' : 'Tạo yêu cầu kiểm nghiệm'}</h1><p className="heading-copy">Thông tin lấy mẫu và tiếp nhận sẽ được liên kết tự động với yêu cầu này.</p></div></header>
    {error && <p className="form-error" role="alert">{error}</p>}{message && <p className="form-success" role="status">{message}</p>}
    <form onSubmit={saveRequest} className="sampling-workflow">
      <FormSection number="I" title="Thông tin khách hàng" en="Customer information" copy="Số/No. và ngày lập phiếu, thông tin khách hàng.">
        <div className="form-two-columns">
          <label>Số/ No.<input value={form.requestNumber} onChange={change(setForm, 'requestNumber')} onBlur={(event) => { const canonical = normalizeRequestNumber(event.target.value); if (canonical !== event.target.value) setForm((current) => ({ ...current, requestNumber: canonical })); }} disabled={editing} placeholder="CFC26.0001" autoComplete="off" spellCheck="false" />
            {!editing && <small className="field-hint">Tự điền số kế tiếp, có thể sửa (CFC + 2 số năm + số thứ tự). Số trùng sẽ báo lỗi. <button type="button" className="inline-link" onClick={refreshNumber}>Lấy số kế tiếp</button></small>}
          </label>
          <Field type="date" label="Ngày/ Date" value={form.requestDate} onChange={change(setForm, 'requestDate')} required />
          <Field label="Tên tổ chức/ cá nhân/ Customer" value={form.customerName} onChange={change(setForm, 'customerName')} required /><Field label="Mã số thuế/ Tax code" value={form.taxCode} onChange={change(setForm, 'taxCode')} />
          <Field label="Người liên hệ/ Contact person" value={form.contactPerson} onChange={change(setForm, 'contactPerson')} /><Field label="Điện thoại/ Phone" value={form.customerPhone} onChange={change(setForm, 'customerPhone')} required />
          <Field label="Email" type="email" value={form.customerEmail} onChange={change(setForm, 'customerEmail')} /><Field label="Địa chỉ/ Address" value={form.customerAddress} onChange={change(setForm, 'customerAddress')} wide />
        </div>
      </FormSection>
      <FormSection number="II" title="Thông tin mẫu" en="Sample information">
        <div className="form-two-columns">
          <Field label="Tên mẫu/ Name of sample" value={form.sampleName} onChange={change(setForm, 'sampleName')} required /><Field label="Mã lô/ Lot code" value={form.lotCode} onChange={change(setForm, 'lotCode')} />
          <Field label="Giống sầu riêng/ Variety" value={form.variety} onChange={change(setForm, 'variety')} /><Field label="Mã vùng trồng / Growing area code" value={form.growingArea} onChange={change(setForm, 'growingArea')} maxLength={255} />
          <Field type="date" label="Ngày thu hoạch/ Harvest date" value={form.harvestDate} onChange={change(setForm, 'harvestDate')} /><Field type="date" label="Ngày đóng gói/ Packing date" value={form.packingDate} onChange={change(setForm, 'packingDate')} />
          <div className="full-span"><span className="group-label">Nguồn lấy mẫu/ Sampled by</span><div className="choice-row">
            <Radio name="samplingMode" label="Công ty lấy mẫu (kèm Phiếu lấy mẫu hiện trường)" checked={form.samplingMode === 'COMPANY'} onChange={() => setForm((c) => ({ ...c, samplingMode: 'COMPANY' }))} />
            <Radio name="samplingMode" label="Khách hàng tự lấy mẫu" checked={form.samplingMode === 'CUSTOMER'} onChange={() => setForm((c) => ({ ...c, samplingMode: 'CUSTOMER' }))} /></div></div>
        </div>
      </FormSection>
      <FormSection number="III" title="Nền mẫu, chỉ tiêu và phương pháp" en="Matrix, parameters and method" copy="Tick nền mẫu cho từng chỉ tiêu theo mẫu phiếu. Mã nhận diện chính thức do nhân viên đặt ở Phiếu tiếp nhận.">
        <div className="request-items">{form.samples.map((sample, index) => <SampleBlock key={index} index={index} sample={sample} removable={form.samples.length > 1} onChange={(patch) => sampleChange(index, patch)} onRemove={() => setForm((c) => ({ ...c, samples: c.samples.filter((_, p) => p !== index) }))} />)}</div>
        <button type="button" className="outline-button" onClick={() => setForm((c) => ({ ...c, samples: [...c.samples, emptySample()] }))}>＋ Thêm mẫu</button>
      </FormSection>
      <FormSection number="IV" title="Mục đích thử nghiệm" en="Purpose of testing">
        <ChecklistField options={PURPOSES} value={form.purpose} onChange={(value) => setForm((c) => ({ ...c, purpose: value }))} otherLabel="Khác: ghi rõ mục đích" />
      </FormSection>
      <FormSection number="V" title="Thời gian trả kết quả" en="Turnaround time">
        <div className="form-two-columns">
          <Field type="date" label="Thời hạn dự kiến/ Expected date" value={form.expectedDate} onChange={change(setForm, 'expectedDate')} /><Field label="Hình thức nhận kết quả" value={form.resultDeliveryMethod} onChange={change(setForm, 'resultDeliveryMethod')} />
          <Field label="Điều kiện áp dụng/ Conditions" value={form.serviceConditions} onChange={change(setForm, 'serviceConditions')} wide />
          <div className="full-span"><span className="group-label">Thông báo khi chậm/ Delay notice</span>
            <ChecklistField options={DELAY_CHANNELS} value={form.delayNotice} onChange={(value) => setForm((c) => ({ ...c, delayNotice: value }))} otherLabel="Khác: ghi rõ cách thông báo" /></div>
        </div>
      </FormSection>
      <FormSection number="VI" title="Xác nhận của khách hàng" en="Customer confirmation" copy="Phần ký tên và đóng dấu thực hiện trên bản in.">
        <p className="field-hint legal-note">Khách hàng xác nhận các thông tin trên là chính xác; đồng ý với phạm vi yêu cầu, điều kiện dịch vụ của Công ty và mục đích sử dụng kết quả đã ghi. Trường hợp khách hàng tự lấy mẫu, Công ty chỉ chịu trách nhiệm thử nghiệm mẫu đã nhận; tính đại diện của mẫu đối với cả lô hàng phụ thuộc quy trình lấy mẫu và thỏa thuận dịch vụ.</p>
        <label className="checkbox-line"><input type="checkbox" checked={form.completed} onChange={change(setForm, 'completed')} /><span>Đánh dấu Phiếu yêu cầu kiểm nghiệm đã hoàn tất</span></label>
        <button className="primary-button" disabled={saving === 'request'}>{saving === 'request' ? 'Đang lưu…' : 'Lưu Phiếu yêu cầu kiểm nghiệm'}</button>
      </FormSection>
    </form>
    {editing && <section className="panel-card sampling-steps" aria-label="Các bước tiếp theo"><h2>Các bước tiếp theo</h2>
      <div className="sampling-steps-grid">
        <article className="sampling-step"><span className="form-number">07</span><div><h3>Kế hoạch lấy mẫu</h3><p>Tùy chọn — lập trước khi đi lấy mẫu.</p><span className={`status-pill ${progress.plan ? 'status-success' : 'status-neutral'}`}>{progress.plan ? 'Đã lập' : 'Chưa lập'}</span></div><button type="button" className="outline-button" onClick={() => navigate(`/admin/requests/${id}/sampling-plan`)}>{progress.plan ? 'Xem / sửa' : 'Lập phiếu'}</button></article>
        <article className="sampling-step"><span className="form-number">02</span><div><h3>Lấy mẫu hiện trường</h3><p>Ghi thông tin thực tế, niêm phong và chỉ tiêu.</p><span className={`status-pill ${progress.field ? 'status-success' : 'status-neutral'}`}>{progress.field ? 'Đã lập' : 'Chưa lập'}</span></div><button type="button" className="outline-button" onClick={() => navigate(`/admin/requests/${id}/field-sampling`)}>{progress.field ? 'Xem / sửa' : 'Lập phiếu'}</button></article>
        <article className="sampling-step sampling-step-main"><span className="form-number">03</span><div><h3>Tiếp nhận và bàn giao mẫu</h3><p>Dữ liệu từ các phiếu trên được điền sẵn để kiểm tra trước khi lưu.</p></div><button type="button" className="primary-button" onClick={() => navigate(`/admin/intake/new?requestId=${id}`)}>Tạo hồ sơ tiếp nhận</button></article>
      </div></section>}
  </section>;
}

function SampleBlock({ index, sample, removable, onChange, onRemove }) {
  const set = (key, patch) => onChange((current) => ({ ...current, [key]: { ...current[key], ...patch } }));
  return <article className="request-item">
    <div className="sample-editor-heading"><strong>TT {index + 1}</strong>{removable && <button type="button" className="table-action" onClick={onRemove}>Xóa mẫu</button>}</div>
    <label className="request-sample-label">Tên mẫu / Mã nhận diện <i>Sample name / ID</i><input value={sample.label} onChange={(e) => onChange((current) => ({ ...current, label: e.target.value }))} placeholder={`Để trống sẽ lưu là “Mẫu ${index + 1}”`} /></label>
    <div className="request-matrix" role="group" aria-label={`Chỉ tiêu mẫu ${index + 1}`}>
      <span className="request-matrix-head">Chỉ tiêu yêu cầu <i>Parameters</i></span><span className="request-matrix-head">Nền mẫu <i>Matrix</i></span><span className="request-matrix-head">Phương pháp thử <i>Test method</i></span>
      <strong>{CADMIUM}</strong>
      <div className="request-matrix-options"><Check label={FLESH} checked={sample.cadmium.flesh} onChange={(v) => set('cadmium', { flesh: v })} /></div>
      <input aria-label={`Phương pháp thử ${CADMIUM}`} value={sample.cadmium.method} onChange={(e) => set('cadmium', { method: e.target.value })} placeholder="Mã, phiên bản" />
      <strong>{AURAMINE}</strong>
      <div className="request-matrix-options"><Check label={SKIN} checked={sample.auramine.skin} onChange={(v) => set('auramine', { skin: v })} /><Check label={FLESH} checked={sample.auramine.flesh} onChange={(v) => set('auramine', { flesh: v })} /></div>
      <input aria-label={`Phương pháp thử ${AURAMINE}`} value={sample.auramine.method} onChange={(e) => set('auramine', { method: e.target.value })} placeholder="Mã, phiên bản" />
    </div>
    {sample.legacy.length > 0 && <p className="field-hint legacy-note" role="note">Dữ liệu cũ ngoài danh mục, được giữ nguyên khi lưu: {sample.legacy.map((row) => `${row.parameters}${row.matrix ? ` (${row.matrix})` : ''}`).join('; ')}</p>}
  </article>;
}

// Các ô tick + ô "Khác" của mục IV và V; giá trị lưu thành một chuỗi nên không đổi schema.
function ChecklistField({ options, value, onChange, otherLabel }) {
  const state = parseChecklist(value, options);
  const toggle = (option) => onChange(buildChecklist(state.selected.includes(option) ? state.selected.filter((item) => item !== option) : [...state.selected, option], state.otherEnabled, state.other));
  return <div className="choice-row">{options.map((option) => <Check key={option} label={option} checked={state.selected.includes(option)} onChange={() => toggle(option)} />)}
    <Check label="Khác" checked={state.otherEnabled} onChange={(checked) => onChange(buildChecklist(state.selected, checked, checked ? state.other : ''))} />
    {state.otherEnabled && <input className="checklist-other" aria-label={otherLabel} value={state.other} onChange={(e) => onChange(buildChecklist(state.selected, true, e.target.value))} placeholder={otherLabel} />}</div>;
}
