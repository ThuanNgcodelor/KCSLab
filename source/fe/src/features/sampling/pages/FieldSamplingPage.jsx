import { WorkflowGuide } from '../../../shared/components/WorkflowGuide';
import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getTestRequest, saveFieldSampling } from '../api/testRequestApi';
import { PageLoader } from '../../../shared/components/Loading';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { FieldForm } from '../components/SamplingParts';
import { fieldFrom, fieldPayload, fieldSeed, formFrom, planSeed, requestFrom } from '../samplingModel';

export function FieldSamplingPage() {
  const { id } = useParams(); const navigate = useNavigate();
  const [request, setRequest] = useState(null); const [plan, setPlan] = useState(null); const [field, setField] = useState(null);
  const [loading, setLoading] = useState(true); const [saving, setSaving] = useState(false); const [saved, setSaved] = useState(false);
  const [error, setError] = useState(''); const [message, setMessage] = useState(''); const [confirmReseed, setConfirmReseed] = useState(false);
  const hasPlan = useRef(false);

  useEffect(() => {
    getTestRequest(id).then((data) => {
      const form = requestFrom(data); setRequest(form);
      const source = data.samplingPlan ? formFrom(data.samplingPlan) : planSeed(form);
      hasPlan.current = Boolean(data.samplingPlan); setPlan(source);
      setSaved(Boolean(data.fieldSampling)); setField(data.fieldSampling ? fieldFrom(data.fieldSampling) : fieldSeed(source, form));
    }).catch((e) => setError(e.message)).finally(() => setLoading(false));
  }, [id]);

  const update = (patch) => setField((current) => ({ ...current, ...patch }));
  async function save() {
    setSaving(true); setError(''); setMessage('');
    try { await saveFieldSampling(id, fieldPayload(field, request.requestNumber)); setSaved(true); setMessage('Đã lưu thông tin lấy mẫu hiện trường.'); } catch (e) { setError(e.message); } finally { setSaving(false); }
  }
  function reseed() {
    setConfirmReseed(false);
    setField((current) => ({ ...fieldSeed(plan, request), formDate: current.formDate }));
  }

  if (loading) return <section className="staff-page"><PageLoader label="Đang tải phiếu lấy mẫu hiện trường…" /></section>;
  return <section className="staff-page sampling-page form-page">
    <header className="admin-heading staff-heading"><div><button className="back-link" onClick={() => navigate(`/admin/requests/${id}`)}>← Phiếu yêu cầu {request?.requestNumber}</button><p className="section-kicker">Phiếu 02 · Field sampling form</p><h1>Lấy mẫu hiện trường</h1><p className="heading-copy">{hasPlan.current ? 'Đã điền sẵn từ Kế hoạch lấy mẫu và Phiếu yêu cầu; ghi lại thông tin thực tế.' : 'Chưa có Kế hoạch lấy mẫu — điền sẵn từ Phiếu yêu cầu; có thể lập phiếu này trực tiếp.'}</p></div></header>
    <WorkflowGuide stage="sampling" />
    {error && <p className="form-error" role="alert">{error}</p>}{message && <p className="form-success" role="status">{message}</p>}
    {field && request && <FieldForm form={request} data={field} update={update} onSave={save} onReseed={() => setConfirmReseed(true)} saving={saving} reseedLabel={hasPlan.current ? 'Lấy lại từ Kế hoạch lấy mẫu' : 'Lấy lại từ Phiếu yêu cầu'} />}
    <div className="page-next-step"><button type="button" className="outline-button" onClick={() => navigate(`/admin/requests/${id}`)}>← Về Phiếu yêu cầu</button>{saved && <button type="button" className="primary-button" onClick={() => navigate(`/admin/intake/new?requestId=${id}`)}>Tiếp theo: Tiếp nhận và bàn giao →</button>}</div>
    {confirmReseed && <ConfirmDialog title={`Lấy lại dữ liệu từ ${hasPlan.current ? 'Kế hoạch lấy mẫu' : 'Phiếu yêu cầu'}?`} description="Các ô đang nhập trong Phiếu lấy mẫu hiện trường sẽ được thay bằng dữ liệu nguồn. Dữ liệu đã lưu không đổi cho đến khi bạn bấm Lưu." confirmLabel="Lấy lại dữ liệu" cancelLabel="Giữ dữ liệu đang nhập" onCancel={() => setConfirmReseed(false)} onConfirm={reseed} />}
  </section>;
}
