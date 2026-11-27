import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { decideReceivingTransfer, getReceivingTransfer } from '../api/receivingApi';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { StatusBadge } from '../../../shared/components/StatusBadge';
import { PageLoader } from '../../../shared/components/Loading';
import { DateInput } from '../../../shared/components/DateInput';
import { AssigneePicker } from '../components/AssigneePicker';
import { ParameterRows } from '../../../shared/components/ParameterRows';

export function ReceivingDetailPage() {
  const { id } = useParams(); const navigate = useNavigate();
  const [transfer, setTransfer] = useState(null);
  const [error, setError] = useState(''); const [loading, setLoading] = useState(true); const [saving, setSaving] = useState(false);
  const [note, setNote] = useState(''); const [labOpinion, setLabOpinion] = useState('');
  const [labRetain, setLabRetain] = useState(false); const [labRetentionUntil, setLabRetentionUntil] = useState('');
  const [pendingDecision, setPendingDecision] = useState(null);
  async function load() { setLoading(true); setError(''); try { setTransfer(await getReceivingTransfer(id)); return true; } catch (e) { setTransfer(null); setError(e.message); return false; } finally { setLoading(false); } }
  useEffect(() => { load(); }, [id]);
  function requestDecision(decision) { if (decision === 'REJECTED' && !note.trim()) { setError('Vui lòng ghi lý do từ chối.'); return; } if (labRetain && !labRetentionUntil) { setError('Vui lòng chọn ngày lưu mẫu đến.'); return; } setError(''); setPendingDecision(decision); }
  async function decide() { const decision = pendingDecision; if (!decision || saving) return; setPendingDecision(null); setSaving(true); setError(''); try { setTransfer(await decideReceivingTransfer(id, { decision, receivingNote: note, labOpinion, labRetain, labRetentionUntil: labRetain ? labRetentionUntil : null })); } catch (e) { if (e.status === 409) { if (await load()) setError('Phiếu đã thay đổi. Đã tải lại trạng thái mới.'); } else setError(e.message); } finally { setSaving(false); } }
  return <section className="admin-page receiving-page receiving-detail">
    <header className="admin-heading"><div><button className="back-link" onClick={() => navigate('/admin/receiving/transfers')}>← Danh sách nhận mẫu</button><p className="section-kicker">Nhận mẫu KCS / Chi tiết bàn giao</p><h1>{transfer ? `Số/No ${transfer.requestNumber || '—'}` : 'Chi tiết phiếu chuyển'}</h1></div></header>
    {error && <p className="form-error" role="alert">{error}</p>}{loading && <PageLoader label="Đang tải phiếu…" rows={4} />}
    {!loading && transfer && <>
      <section className="panel-card handover-card"><div className="receiving-section-heading"><div><h2>Thông tin bàn giao</h2><p>Thông tin phiếu và các mẫu được chuyển đến KCS</p></div><StatusBadge domain="transfer" value={transfer.status} /></div>
        <div className="handover-summary">
          <div className="handover-primary"><span>Số/No</span><strong>{transfer.requestNumber || '—'}</strong><small>{transfer.items.length} mẫu · {transfer.receivingDepartment || 'KCS'}</small></div>
          <dl className="handover-grid">{[['Người giao nội bộ', transfer.deliveredBy], ['Ngày chuyển', formatDate(transfer.transferDate)], ['Hạn trả kết quả', formatDate(transfer.resultsDueDate), 'due'], ['Người nhận', transfer.receivedBy || 'Chưa xác nhận'], ['Trả mẫu cho khách', transfer.returnToCustomer ? `Có${transfer.customerReturnDate ? ` · ${formatDate(transfer.customerReturnDate)}` : ''}` : 'Không']].map(([label, value, tone]) => <div key={label} className={tone ? `handover-item ${tone}` : 'handover-item'}><dt>{label}</dt><dd>{value || '—'}</dd></div>)}</dl>
        </div>
        <h3 className="receiving-subheading handover-subheading">Danh sách mẫu <span>{transfer.items.length}</span></h3><div className="table-wrap receiving-sample-table"><table><thead><tr><th>Mã hóa mẫu</th><th>Số/No</th><th>Số lượng</th><th>Khối lượng</th><th>Tình trạng</th><th>Chỉ tiêu</th><th>Phương pháp</th></tr></thead><tbody>{transfer.items.map((item) => <ParameterRows key={item.id} item={item} columns={(rowSpan) => <><td data-label="Mã hóa mẫu" rowSpan={rowSpan}><strong>{item.sampleCode}</strong></td><td data-label="Số/No" rowSpan={rowSpan}>{transfer.requestNumber || '—'}</td><td data-label="Số lượng" rowSpan={rowSpan}>{item.quantity || '—'}</td><td data-label="Khối lượng" rowSpan={rowSpan}>{item.weight || '—'}</td><td data-label="Tình trạng" rowSpan={rowSpan}>{item.sampleCondition || '—'}</td></>} />)}</tbody></table></div><div className="handover-note"><strong>Ghi chú bàn giao</strong><p>{transfer.changesNote || 'Không có ghi chú.'}</p></div>
      </section>
      {['PENDING', 'ACCEPTED'].includes(transfer.status) && <AssigneePicker transferId={transfer.id} assignees={transfer.assignees ?? []} onSaved={(assignees) => setTransfer((current) => ({ ...current, assignees }))} />}
      {transfer.status === 'PENDING' && <section className="panel-card"><h2>Xác nhận bàn giao</h2><label>Ghi chú hoặc lý do từ chối<textarea rows="3" value={note} onChange={(e) => setNote(e.target.value)} /></label><label>Ý kiến phòng phân tích<textarea rows="3" value={labOpinion} onChange={(e) => setLabOpinion(e.target.value)} /></label><label className="switch-label"><input type="checkbox" checked={labRetain} onChange={(e) => setLabRetain(e.target.checked)} /><span className="fake-switch" />Phòng phân tích lưu mẫu</label>{labRetain && <label>Lưu đến ngày<DateInput value={labRetentionUntil} onChange={(e) => setLabRetentionUntil(e.target.value)} /></label>}<div className="receiving-filter"><button className="outline-button" disabled={saving} onClick={() => requestDecision('REJECTED')}>Từ chối</button><button className="primary-button" disabled={saving} onClick={() => requestDecision('ACCEPTED')}>{saving ? 'Đang lưu…' : 'Xác nhận đã nhận'}</button></div></section>}
      {pendingDecision && <ConfirmDialog title={pendingDecision === 'ACCEPTED' ? 'Xác nhận nhận mẫu?' : 'Từ chối nhận mẫu?'} description={pendingDecision === 'ACCEPTED' ? `Bạn xác nhận đã nhận mẫu của Số/No ${transfer.requestNumber || '—'}.` : `Phiếu của Số/No ${transfer.requestNumber || '—'} sẽ được trả lại cùng lý do đã nhập.`} confirmLabel={pendingDecision === 'ACCEPTED' ? 'Xác nhận đã nhận' : 'Xác nhận từ chối'} tone={pendingDecision === 'REJECTED' ? 'danger' : 'default'} onCancel={() => setPendingDecision(null)} onConfirm={decide} />}
    </>}
  </section>;
}

function formatDate(value) {
  if (!value) return '';
  const [year, month, day] = String(value).slice(0, 10).split('-');
  return day && month && year ? `${day}/${month}/${year}` : value;
}
