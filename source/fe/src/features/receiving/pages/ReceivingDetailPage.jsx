import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { decideReceivingTransfer, getReceivingEvidence, getReceivingTransfer } from '../api/receivingApi';

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
  const dialog = useRef(null);
  async function load() {
    setLoading(true); setError('');
    try { const [record, files] = await Promise.all([getReceivingTransfer(id), getReceivingEvidence(id)]); setTransfer(record); setEvidence(files); }
    catch (e) { setError(e.message); }
    finally { setLoading(false); }
  }
  useEffect(() => { load(); }, [id]);
  useEffect(() => { if (selected) dialog.current?.showModal(); }, [selected]);
  async function decide(decision) {
    if (decision === 'REJECTED' && !note.trim()) { setError('Vui lòng ghi lý do từ chối.'); return; }
    if (labRetain && !labRetentionUntil) { setError('Vui lòng chọn ngày lưu mẫu đến.'); return; }
    if (!window.confirm(decision === 'ACCEPTED' ? 'Xác nhận đã đối chiếu và nhận mẫu?' : 'Xác nhận từ chối nhận mẫu?')) return;
    setSaving(true); setError('');
    try { setTransfer(await decideReceivingTransfer(id, { decision, receivingNote: note, labOpinion, labRetain, labRetentionUntil: labRetain ? labRetentionUntil : null })); }
    catch (e) { if (e.status === 409) { await load(); setError('Phiếu đã thay đổi. Đã tải lại trạng thái mới.'); } else setError(e.message); }
    finally { setSaving(false); }
  }
  const groups = ['Phiếu 3','Ảnh mẫu','Ảnh lô hàng','Khác'];
  return <section className="admin-page"><header className="admin-heading"><div><button className="back-link" onClick={() => navigate('/admin/receiving/transfers')}>← Danh sách nhận mẫu</button><h1>{transfer?.transferCode || 'Chi tiết phiếu chuyển'}</h1></div></header>
    {error && <p className="form-error" role="alert">{error}</p>}{loading && <p role="status">Đang tải phiếu…</p>}
    {!loading && transfer && <><section className="panel-card"><h2>Thông tin bàn giao</h2><p>Phiếu tiếp nhận: {transfer.receiptCode} · Người giao nội bộ: {transfer.deliveredBy} · Trạng thái: {transfer.status}</p><div className="table-wrap"><table><thead><tr><th>Mã mẫu</th><th>Tên mẫu</th><th>Số lượng</th><th>Tình trạng</th><th>Chỉ tiêu</th><th>Phương pháp</th></tr></thead><tbody>{transfer.items.map(item => <tr key={item.id}><td>{item.sampleCode}</td><td>{item.sampleName}</td><td>{item.quantity || item.weight || '—'}</td><td>{item.sampleCondition || '—'}</td><td>{item.requestedParameters || '—'}</td><td>{item.testMethod}</td></tr>)}</tbody></table></div><p>Ghi chú: {transfer.changesNote || 'Không có'}</p></section>
      <section className="panel-card"><h2>Ảnh và hồ sơ đối chiếu</h2>{!evidence.some(file => file.contentType.startsWith('image/')) && <p role="note">Chưa có ảnh đối chiếu. Bạn vẫn có thể nhận mẫu sau khi kiểm tra thực tế.</p>}{groups.map(group => { const files = evidence.filter(file => (groupNames[file.category] || 'Khác') === group); return <div key={group}><h3>{group} ({files.length})</h3>{files.length ? <div className="receiving-gallery">{files.map(file => <article key={file.id}>{file.contentType.startsWith('image/') ? <button className="receiving-preview" onClick={() => setSelected(file)} aria-label={`Xem ảnh ${file.originalName}`}><img src={file.contentUrl} alt={file.originalName} /></button> : <span aria-hidden="true">📄</span>}<strong>{file.originalName}</strong><small>{Math.ceil(file.size / 1024)} KB · {file.uploadedByName || 'Không rõ người tải'}</small><small>{file.capturedAt ? 'Chụp' : 'Tải lên'}: {new Date(file.capturedAt || file.uploadedAt).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' })}</small><a href={file.contentUrl} target="_blank" rel="noreferrer">Mở / tải file</a></article>)}</div> : <p>Không có file trong nhóm này.</p>}</div>; })}</section>
      {transfer.status === 'PENDING' && <section className="panel-card"><h2>Xác nhận bàn giao</h2><label>Ghi chú hoặc lý do từ chối<textarea rows="3" value={note} onChange={e => setNote(e.target.value)} /></label><label>Ý kiến phòng phân tích<textarea rows="3" value={labOpinion} onChange={e => setLabOpinion(e.target.value)} /></label><label className="switch-label"><input type="checkbox" checked={labRetain} onChange={e => setLabRetain(e.target.checked)} /><span className="fake-switch" />Phòng phân tích lưu mẫu</label>{labRetain && <label>Lưu đến ngày<input type="date" value={labRetentionUntil} onChange={e => setLabRetentionUntil(e.target.value)} /></label>}<div className="receiving-filter"><button className="outline-button" disabled={saving} onClick={() => decide('REJECTED')}>Từ chối</button><button className="primary-button" disabled={saving} onClick={() => decide('ACCEPTED')}>{saving ? 'Đang lưu…' : 'Xác nhận đã nhận'}</button></div></section>}
      {transfer.status !== 'PENDING' && <section className="panel-card"><h2>Kết quả bàn giao</h2><p>{transfer.receivingNote || 'Không có ghi chú.'}</p><p>Ý kiến phòng phân tích: {transfer.labOpinion || 'Không có'}</p>{transfer.labRetain && <p>Lưu mẫu đến: {transfer.labRetentionUntil}</p>}</section>}
      <dialog ref={dialog} onClose={() => setSelected(null)} className="receiving-dialog"><button className="outline-button" onClick={() => dialog.current?.close()}>Đóng</button>{selected && <img src={selected.contentUrl} alt={selected.originalName} />}</dialog>
    </>}
  </section>;
}
