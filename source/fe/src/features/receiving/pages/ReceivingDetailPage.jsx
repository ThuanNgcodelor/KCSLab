import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { decideReceivingTransfer, getReceivingEvidence, getReceivingTransfer, verifyReceivingSample } from '../api/receivingApi';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { StatusBadge } from '../../../shared/components/StatusBadge';
import { PageLoader } from '../../../shared/components/Loading';
import { Icon } from '../../../shared/components/Icon';
import { DateInput } from '../../../shared/components/DateInput';

const groupNames = { FORM_3: 'Phiếu 3', SAMPLE_PHOTO: 'Ảnh mẫu', LOT_PHOTO: 'Ảnh lô hàng' };
export function ReceivingDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [transfer, setTransfer] = useState(null);
  const [evidence, setEvidence] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [note, setNote] = useState('');
  const [labOpinion, setLabOpinion] = useState('');
  const [labRetain, setLabRetain] = useState(false);
  const [labRetentionUntil, setLabRetentionUntil] = useState('');
  const [selected, setSelected] = useState(null);
  const [pendingDecision, setPendingDecision] = useState(null);
  const [verifySample, setVerifySample] = useState(null);
  const dialog = useRef(null);
  async function load() {
    setLoading(true); setError('');
    try { const [record, files] = await Promise.all([getReceivingTransfer(id), getReceivingEvidence(id)]); setTransfer(record); setEvidence(files); }
    catch (e) { setTransfer(null); setEvidence([]); setError(e.message); return false; }
    finally { setLoading(false); }
    return true;
  }
  useEffect(() => { load(); }, [id]);
  useEffect(() => { if (selected) dialog.current?.showModal(); }, [selected]);
  function requestDecision(decision) {
    if (decision === 'REJECTED' && !note.trim()) { setError('Vui lòng ghi lý do từ chối.'); return; }
    if (labRetain && !labRetentionUntil) { setError('Vui lòng chọn ngày lưu mẫu đến.'); return; }
    setError('');
    setPendingDecision(decision);
  }
  async function decide() {
    const decision = pendingDecision;
    if (!decision || saving) return;
    setPendingDecision(null);
    setSaving(true); setError('');
    try { setTransfer(await decideReceivingTransfer(id, { decision, receivingNote: note, labOpinion, labRetain, labRetentionUntil: labRetain ? labRetentionUntil : null })); }
    catch (e) { if (e.status === 409) { if (await load()) setError('Phiếu đã thay đổi. Đã tải lại trạng thái mới.'); } else setError(e.message); }
    finally { setSaving(false); }
  }
  const groups = ['Phiếu 3','Ảnh mẫu','Ảnh lô hàng','Khác'];
  return <section className="admin-page receiving-page receiving-detail"><header className="admin-heading"><div><button className="back-link" onClick={() => navigate('/admin/receiving/transfers')}>← Danh sách nhận mẫu</button><p>Chi tiết phiếu bàn giao</p><h1>{transfer?.transferCode || 'Chi tiết phiếu chuyển'}</h1></div></header>
    {error && <p className="form-error" role="alert">{error}</p>}{loading && <PageLoader label="Đang tải phiếu…" rows={4} />}
    {!loading && transfer && <><section className="panel-card"><div className="receiving-section-heading"><div><h2>Thông tin bàn giao <StatusBadge domain="transfer" value={transfer.status} /></h2><p>Thông tin phiếu và các mẫu cần đối chiếu</p></div><span className="receiving-count">{{ PENDING: 'Chờ nhận', ACCEPTED: 'Đã nhận', REJECTED: 'Đã từ chối' }[transfer.status] || transfer.status}</span></div><dl className="receiving-details-grid">{[['Phiếu tiếp nhận', transfer.receiptCode], ['Người giao nội bộ', transfer.deliveredBy], ['Ngày chuyển', transfer.transferDate], ['Hạn trả kết quả', transfer.resultsDueDate], ['Phòng nhận', transfer.receivingDepartment], ['Người nhận', transfer.receivedBy || 'Chưa xác nhận'], ['Trả mẫu cho khách', transfer.returnToCustomer ? `Có${transfer.customerReturnDate ? ` (${transfer.customerReturnDate})` : ''}` : 'Không']].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value || '—'}</dd></div>)}</dl><h3 className="receiving-subheading">Danh sách mẫu <span>{transfer.items.length}</span></h3><div className="table-wrap receiving-sample-table"><table><thead><tr><th>Mã mẫu</th><th>Tên mẫu</th><th>Số lượng</th><th>Khối lượng</th><th>Tình trạng</th><th>Chỉ tiêu</th><th>Phương pháp</th></tr></thead><tbody>{transfer.items.map(item => <tr key={item.id}><td data-label="Mã mẫu">{item.sampleCode}</td><td data-label="Tên mẫu">{item.sampleName}</td><td data-label="Số lượng">{item.quantity || '—'}</td><td data-label="Khối lượng">{item.weight || '—'}</td><td data-label="Tình trạng">{item.sampleCondition || '—'}</td><td data-label="Chỉ tiêu">{item.requestedParameters || '—'}</td><td data-label="Phương pháp">{item.testMethod || '—'}</td></tr>)}</tbody></table></div><p className="receiving-note"><strong>Ghi chú:</strong> {transfer.changesNote || 'Không có'}</p></section>
      <section className="panel-card receiving-evidence"><div className="receiving-section-heading"><div><h2>Ảnh và hồ sơ đối chiếu</h2><p>Bằng chứng được đính kèm trong hồ sơ tiếp nhận</p></div><span className="receiving-count">{evidence.length} file</span></div>{!evidence.some(file => file.contentType.startsWith('image/')) && <p className="receiving-warning" role="note">Chưa có ảnh đối chiếu. Bạn vẫn có thể nhận mẫu sau khi kiểm tra thực tế.</p>}{groups.map(group => { const files = evidence.filter(file => (groupNames[file.category] || 'Khác') === group); return files.length ? <div key={group}><h3>{group} <span>({files.length})</span></h3><div className="receiving-gallery">{files.map(file => <article key={file.id}>{file.contentType.startsWith('image/') ? <button className="receiving-preview" onClick={() => setSelected(file)} aria-label={`Xem ảnh ${file.originalName}`}><img src={file.contentUrl} alt={file.originalName} /></button> : <span className="receiving-file-icon"><Icon name="file" size={26} /></span>}<strong>{file.originalName}</strong><small>{Math.ceil(file.size / 1024)} KB · {file.uploadedByName || 'Không rõ người tải'}</small><small>{file.capturedAt ? 'Chụp' : 'Tải lên'}: {new Date(file.capturedAt || file.uploadedAt).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' })}</small><a href={file.contentUrl} target="_blank" rel="noreferrer">Mở / tải file</a></article>)}</div></div> : null; })}{!evidence.length && <p className="receiving-empty">Chưa có file đính kèm cho phiếu này.</p>}</section>
      {transfer.status === 'PENDING' && <section className="panel-card"><h2>Xác nhận bàn giao</h2><label>Ghi chú hoặc lý do từ chối<textarea rows="3" value={note} onChange={e => setNote(e.target.value)} /></label><label>Ý kiến phòng phân tích<textarea rows="3" value={labOpinion} onChange={e => setLabOpinion(e.target.value)} /></label><label className="switch-label"><input type="checkbox" checked={labRetain} onChange={e => setLabRetain(e.target.checked)} /><span className="fake-switch" />Phòng phân tích lưu mẫu</label>{labRetain && <label>Lưu đến ngày<DateInput value={labRetentionUntil} onChange={e => setLabRetentionUntil(e.target.value)} /></label>}<div className="receiving-filter"><button className="outline-button" disabled={saving} onClick={() => requestDecision('REJECTED')}>Từ chối</button><button className="primary-button" disabled={saving} onClick={() => requestDecision('ACCEPTED')}>{saving ? 'Đang lưu…' : 'Xác nhận đã nhận'}</button></div></section>}
      {transfer.status !== 'PENDING' && <section className="panel-card"><h2>Kết quả bàn giao</h2><p>{transfer.receivingNote || 'Không có ghi chú.'}</p><p>Ý kiến phòng phân tích: {transfer.labOpinion || 'Không có'}</p>{transfer.labRetain && <p>Lưu mẫu đến: {transfer.labRetentionUntil}</p>}</section>}
      {transfer.status === 'ACCEPTED' && <section className="panel-card"><h2>Đối chiếu mã mẫu tại KCS</h2><p>Người đối chiếu phải khác người mã hóa. Kiểm tra nhãn trên mẫu thực tế, hồ sơ và phiếu yêu cầu trước khi xác nhận. Mẫu chưa xác minh không được đưa vào thử nghiệm.</p>{transfer.items.map(item => <div key={item.id} className="receiving-note"><strong>{item.sampleCode}</strong> · {item.codingStatus === 'VERIFIED' ? `Đã xác minh${item.verifiedBy ? ` bởi ${item.verifiedBy}` : ''}` : 'Chờ đối chiếu'}{item.codedBy && <span> · Người dán nhãn: {item.codedBy}</span>}{item.codingStatus === 'PENDING_VERIFICATION' && <button type="button" className="outline-button" disabled={saving} onClick={() => setVerifySample(item)}>Xác minh mã mẫu</button>}</div>)}<p>Nếu thông tin không khớp, dừng thử nghiệm và liên hệ người tiếp nhận để xử lý; không xác nhận khi chưa kiểm tra.</p></section>}
      <dialog ref={dialog} onClose={() => setSelected(null)} className="receiving-dialog"><button className="outline-button" onClick={() => dialog.current?.close()}>Đóng</button>{selected && <img src={selected.contentUrl} alt={selected.originalName} />}</dialog>
      {pendingDecision && <ConfirmDialog
        title={pendingDecision === 'ACCEPTED' ? 'Xác nhận nhận mẫu?' : 'Từ chối nhận mẫu?'}
        description={pendingDecision === 'ACCEPTED'
          ? `Bạn xác nhận đã đối chiếu và nhận mẫu trong phiếu ${transfer.transferCode}.`
          : `Phiếu ${transfer.transferCode} sẽ được trả lại cho người bàn giao cùng lý do bạn đã nhập.`}
        confirmLabel={pendingDecision === 'ACCEPTED' ? 'Xác nhận đã nhận' : 'Xác nhận từ chối'}
        tone={pendingDecision === 'REJECTED' ? 'danger' : 'default'}
        onCancel={() => setPendingDecision(null)} onConfirm={decide} />}
      {verifySample && <ConfirmDialog title="Xác nhận đối chiếu mã mẫu?" description={`Bạn đã đối chiếu nhãn thực tế, hồ sơ và phiếu yêu cầu cho mẫu ${verifySample.sampleCode}; bạn không phải người đã mã hóa mẫu này.`} confirmLabel="Xác minh" onCancel={() => setVerifySample(null)} onConfirm={async () => { const sample = verifySample; setVerifySample(null); setSaving(true); setError(''); try { setTransfer(await verifyReceivingSample(id, sample.sampleId)); } catch (e) { setError(e.message); } finally { setSaving(false); } }} />}
    </>}
  </section>;
}
