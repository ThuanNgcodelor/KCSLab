import { WorkflowGuide } from '../../../shared/components/WorkflowGuide';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getTestRequest, saveSamplingPlan } from '../api/testRequestApi';
import { PageLoader } from '../../../shared/components/Loading';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { PlanForm } from '../components/SamplingParts';
import { formFrom, planSeed, requestFrom, timePayload } from '../samplingModel';

export function SamplingPlanPage() {
  const { id } = useParams(); const navigate = useNavigate();
  const [request, setRequest] = useState(null); const [plan, setPlan] = useState(null);
  const [loading, setLoading] = useState(true); const [saving, setSaving] = useState(false); const [saved, setSaved] = useState(false);
  const [error, setError] = useState(''); const [message, setMessage] = useState(''); const [confirmReseed, setConfirmReseed] = useState(false);

  useEffect(() => {
    getTestRequest(id).then((data) => {
      const form = requestFrom(data); setRequest(form);
      setSaved(Boolean(data.samplingPlan)); setPlan(data.samplingPlan ? formFrom(data.samplingPlan) : planSeed(form));
    }).catch((e) => setError(e.message)).finally(() => setLoading(false));
  }, [id]);

  const update = (patch) => setPlan((current) => ({ ...current, ...patch }));
  async function save() {
    setSaving(true); setError(''); setMessage('');
    try { await saveSamplingPlan(id, timePayload(plan, request.requestNumber)); setSaved(true); setMessage('Đã lưu kế hoạch lấy mẫu.'); } catch (e) { setError(e.message); } finally { setSaving(false); }
  }
  function reseed() {
    setConfirmReseed(false);
    setPlan((current) => ({ ...planSeed(request), formDate: current.formDate }));
  }

  if (loading) return <section className="staff-page"><PageLoader label="Đang tải kế hoạch lấy mẫu…" /></section>;
  return <section className="staff-page sampling-page form-page">
    <header className="admin-heading staff-heading"><div><button className="back-link" onClick={() => navigate(`/admin/requests/${id}`)}>← Phiếu yêu cầu {request?.requestNumber}</button><p className="section-kicker">Phiếu 07 · Sampling plan</p><h1>Kế hoạch lấy mẫu</h1><p className="heading-copy">Thông tin khách hàng và lô hàng lấy từ Phiếu yêu cầu {request?.requestNumber}; chỉ cần bổ sung phần lấy mẫu.</p></div></header>
    <WorkflowGuide stage="sampling" />
    {error && <p className="form-error" role="alert">{error}</p>}{message && <p className="form-success" role="status">{message}</p>}
    {plan && request && <PlanForm form={request} data={plan} update={update} onSave={save} onReseed={() => setConfirmReseed(true)} saving={saving} />}
    <div className="page-next-step"><button type="button" className="outline-button" onClick={() => navigate(`/admin/requests/${id}`)}>← Về Phiếu yêu cầu</button>{saved && <button type="button" className="primary-button" onClick={() => navigate(`/admin/requests/${id}/field-sampling`)}>Tiếp theo: Lấy mẫu hiện trường →</button>}</div>
    {confirmReseed && <ConfirmDialog title="Lấy lại dữ liệu từ Phiếu yêu cầu?" description="Các ô đang nhập trong Kế hoạch lấy mẫu sẽ được thay bằng dữ liệu từ Phiếu yêu cầu. Dữ liệu đã lưu không đổi cho đến khi bạn bấm Lưu." confirmLabel="Lấy lại dữ liệu" cancelLabel="Giữ dữ liệu đang nhập" onCancel={() => setConfirmReseed(false)} onConfirm={reseed} />}
  </section>;
}
