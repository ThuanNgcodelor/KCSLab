import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { decideTransfer, getTransfer } from '../api/transferApi';

const statusLabels = { PENDING: 'Chờ phòng PT nhận', ACCEPTED: 'Đã nhận mẫu', REJECTED: 'Từ chối nhận' };

export function TransferDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [transfer, setTransfer] = useState(null);
  const [decision, setDecision] = useState({ receivingNote: '', labOpinion: '', labRetain: false, labRetentionUntil: '' });
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const canDecide = ['ADMIN', 'TECHNICIAN'].includes(user?.role);

  useEffect(() => { getTransfer(id).then(setTransfer).catch((exception) => setError(exception.message)); }, [id]);
  const update = (field) => (event) => setDecision((current) => ({ ...current, [field]: event.target.type === 'checkbox' ? event.target.checked : event.target.value }));

  async function submitDecision(nextDecision) {
    setSaving(true); setError('');
    try {
      setTransfer(await decideTransfer(id, { ...decision, decision: nextDecision, labRetentionUntil: decision.labRetain ? decision.labRetentionUntil : null }));
    } catch (exception) { setError(exception.message); }
    finally { setSaving(false); }
  }

  if (!transfer) return <section className="staff-page">{error ? <p className="form-error">{error}</p> : <p className="muted">Đang tải phiếu chuyển…</p>}</section>;

  return <section className="staff-page transfer-detail-page">
    <header className="admin-heading staff-heading"><div><button className="back-link" onClick={() => navigate('/admin/transfers')}>← Danh sách phiếu chuyển</button><p className="section-kicker">Phiếu chuyển mẫu / {transfer.transferCode}</p><h1>{statusLabels[transfer.status]}</h1><p className="heading-copy">Liên kết phiếu tiếp nhận {transfer.receiptCode} · {transfer.customerName}</p></div><span className={`transfer-status large ${transfer.status.toLowerCase()}`}><i />{statusLabels[transfer.status]}</span></header>
    {location.state?.message && <p className="success-note intake-toast">✓ {location.state.message}</p>}{error && <p className="form-error intake-toast">{error}</p>}
    <div className="transfer-detail-grid">
      <main className="receipt-form-main">
        <section className="panel-card transfer-sheet"><div className="transfer-sheet-heading"><div><p className="section-kicker">Sample transfer form</p><h2>Phiếu chuyển mẫu</h2></div><strong>{transfer.transferCode}</strong></div><div className="transfer-meta"><span><small>Phiếu yêu cầu</small><strong>{transfer.receiptCode}</strong></span><span><small>Ngày chuyển</small><strong>{formatDate(transfer.transferDate)}</strong></span><span><small>Hạn trả kết quả</small><strong>{formatDate(transfer.resultsDueDate)}</strong></span><span><small>Người giao</small><strong>{transfer.deliveredBy}</strong></span></div><div className="table-wrap"><table className="staff-table transfer-items-table"><thead><tr><th>Mẫu</th><th>Khối lượng / SL</th><th>Tình trạng</th><th>Chỉ tiêu</th><th>Phương pháp</th></tr></thead><tbody>{transfer.items.map((item) => <tr key={item.id}><td data-label="Mẫu"><strong>{item.sampleCode}</strong><small>{item.sampleName}</small></td><td data-label="Khối lượng / SL">{item.weight || '—'}<small>{item.quantity || '—'}</small></td><td data-label="Tình trạng">{item.sampleCondition || '—'}</td><td data-label="Chỉ tiêu">{item.requestedParameters || '—'}</td><td data-label="Phương pháp">{item.testMethod}</td></tr>)}</tbody></table></div></section>
        <section className="panel-card transfer-notes"><div><p className="section-kicker">Thay đổi / ghi chú</p><p>{transfer.changesNote || 'Không có thay đổi so với phiếu yêu cầu.'}</p></div><div><p className="section-kicker">Ý kiến phòng phân tích</p><p>{transfer.labOpinion || 'Chưa có ý kiến.'}</p></div></section>
      </main>
      <aside className="receipt-form-aside">
        <section className="panel-card custody-card"><p className="section-kicker">Chuỗi bàn giao</p><div className="custody-event complete"><i /><span><strong>Đã lập phiếu</strong><small>{transfer.deliveredBy} · {formatDateTime(transfer.createdAt)}</small></span></div><div className={`custody-event ${transfer.status !== 'PENDING' ? 'complete' : ''}`}><i /><span><strong>{transfer.status === 'REJECTED' ? 'Đã từ chối' : 'Phòng PT tiếp nhận'}</strong><small>{transfer.receivedBy ? `${transfer.receivedBy} · ${formatDateTime(transfer.receivedAt)}` : 'Đang chờ xác nhận'}</small></span></div></section>
        {transfer.status === 'PENDING' && canDecide && <section className="panel-card intake-form-card decision-panel"><div className="form-card-heading"><span className="form-number">✓</span><div><h2>Xác nhận bàn giao</h2><p>Ghi ý kiến trước khi nhận hoặc từ chối mẫu.</p></div></div><label>Ghi chú tiếp nhận<textarea value={decision.receivingNote} onChange={update('receivingNote')} rows="3" placeholder="Tình trạng khi nhận hoặc lý do từ chối" /></label><label>Ý kiến phòng phân tích<textarea value={decision.labOpinion} onChange={update('labOpinion')} rows="3" /></label><label className="switch-label"><input type="checkbox" checked={decision.labRetain} onChange={update('labRetain')} /><span className="fake-switch" />Phòng phân tích lưu mẫu</label>{decision.labRetain && <label>Lưu đến ngày<input type="date" value={decision.labRetentionUntil} onChange={update('labRetentionUntil')} required /></label>}<div className="transfer-decision-actions"><button type="button" className="outline-button reject-button" disabled={saving} onClick={() => submitDecision('REJECTED')}>Từ chối nhận</button><button type="button" className="primary-button" disabled={saving} onClick={() => submitDecision('ACCEPTED')}>{saving ? 'Đang lưu…' : 'Xác nhận đã nhận'}</button></div></section>}
        {transfer.status !== 'PENDING' && <section className={`handoff-result ${transfer.status.toLowerCase()}`}><strong>{statusLabels[transfer.status]}</strong><span>{transfer.receivingNote || 'Không có ghi chú tiếp nhận.'}</span>{transfer.labRetain && <small>Lưu mẫu đến {formatDate(transfer.labRetentionUntil)}</small>}</section>}
      </aside>
    </div>
  </section>;
}

function formatDate(value) { return value ? new Date(`${value}T00:00:00`).toLocaleDateString('vi-VN') : '—'; }
function formatDateTime(value) { return value ? new Date(value).toLocaleString('vi-VN') : '—'; }
