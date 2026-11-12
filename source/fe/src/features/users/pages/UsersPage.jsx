import { useEffect, useState } from 'react';
import { createUser, listUsers, updateUser } from '../api/usersApi';
import { useAuth } from '../../auth/hooks/useAuth';

const initialForm = { email: '', displayName: '', password: '', role: 'RECEPTIONIST' };
const roleLabels = { ADMIN: 'Quản trị viên', RECEPTIONIST: 'Tiếp nhận', TECHNICIAN: 'Kỹ thuật viên' };

export function UsersPage() {
  const { user } = useAuth();
  const [users, setUsers] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [formOpen, setFormOpen] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);

  async function loadUsers() { setLoading(true); try { setUsers(await listUsers()); } catch (exception) { setError(exception.message); } finally { setLoading(false); } }
  useEffect(() => { loadUsers(); }, []);
  async function handleCreate(event) { event.preventDefault(); setError(''); setMessage(''); try { await createUser(form); setForm(initialForm); setFormOpen(false); setMessage('Đã tạo tài khoản nhân viên.'); await loadUsers(); } catch (exception) { setError(exception.message); } }
  async function toggleUser(target) { setError(''); try { await updateUser(target.id, { enabled: !target.enabled }); await loadUsers(); } catch (exception) { setError(exception.message); } }

  return <section className="staff-page">
    <header className="admin-heading staff-heading"><div><p className="section-kicker">Không gian quản trị / Nhân viên</p><h1>Nhân viên</h1><p className="heading-copy">Quản lý tài khoản, vai trò và quyền truy cập của đội ngũ CFC Lab.</p></div><button className="primary-button add-staff-button" onClick={() => setFormOpen(true)}>＋ Thêm nhân viên</button></header>
    {message && <p className="success-note toast-note" role="status">✓ {message}</p>}{error && <p className="form-error toast-note" role="alert">{error}</p>}
    <div className="staff-overview"><div><span>Tổng nhân viên</span><strong>{users.length || '—'}</strong></div><div><span>Đang hoạt động</span><strong className="green-number">{users.filter((target) => target.enabled).length || '—'}</strong></div><div><span>Quản trị viên</span><strong>{users.filter((target) => target.role === 'ADMIN').length || '—'}</strong></div><div><span>Cập nhật gần nhất</span><strong className="overview-date">Vừa xong</strong></div></div>
    <section className="panel-card staff-table-card"><div className="panel-heading"><div><p className="section-kicker">Danh sách tài khoản</p><h2>Đội ngũ CFC Lab</h2></div><button className="outline-button small-button" onClick={loadUsers}>↻ Làm mới</button></div>{loading ? <p className="muted">Đang tải danh sách…</p> : <div className="table-wrap"><table className="staff-table"><thead><tr><th>Nhân viên</th><th>Vai trò</th><th>Trạng thái</th><th>Lần cập nhật</th><th /></tr></thead><tbody>{users.map((target) => <tr key={target.id}><td><div className="staff-person"><span className="staff-avatar">{target.displayName?.slice(0, 1) ?? '?'}</span><span><strong>{target.displayName}</strong><small>{target.email}</small></span></div></td><td><span className="role-label">{roleLabels[target.role] ?? target.role}</span></td><td><span className={`status-pill ${target.enabled ? 'active' : 'disabled'}`}><i />{target.enabled ? 'Đang hoạt động' : 'Đã khóa'}</span></td><td className="muted-cell">Vừa cập nhật</td><td><button className="table-action" disabled={target.id === user?.id} onClick={() => toggleUser(target)}>{target.enabled ? 'Khóa tài khoản' : 'Mở khóa'}</button></td></tr>)}</tbody></table></div>}</section>
    {formOpen && <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && setFormOpen(false)}><section className="staff-modal" role="dialog" aria-modal="true" aria-labelledby="add-staff-title"><button className="modal-close" onClick={() => setFormOpen(false)} aria-label="Đóng">×</button><p className="section-kicker">Tài khoản nội bộ</p><h2 id="add-staff-title">Thêm nhân viên</h2><p className="muted">Tạo thông tin đăng nhập và phân quyền cho thành viên mới.</p><form className="user-form" onSubmit={handleCreate}><label>Email công việc<input type="email" autoComplete="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} placeholder="ten@cfc.vn" required /></label><label>Tên hiển thị<input value={form.displayName} onChange={(event) => setForm({ ...form, displayName: event.target.value })} placeholder="Nguyễn Văn A" required /></label><label>Mật khẩu tạm thời<input type="password" minLength="12" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} placeholder="Tối thiểu 12 ký tự" required /></label><label>Vai trò<select value={form.role} onChange={(event) => setForm({ ...form, role: event.target.value })}><option value="RECEPTIONIST">Tiếp nhận</option><option value="TECHNICIAN">Kỹ thuật viên</option><option value="ADMIN">Quản trị viên</option></select></label><div className="modal-actions"><button type="button" className="outline-button" onClick={() => setFormOpen(false)}>Hủy</button><button className="primary-button" type="submit">Tạo tài khoản</button></div></form></section></div>}
  </section>;
}
