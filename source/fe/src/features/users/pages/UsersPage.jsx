import { useEffect, useState } from 'react';
import { createUser, listUsers, updateUser } from '../api/usersApi';
import { useAuth } from '../../auth/hooks/useAuth';

const initialForm = { email: '', displayName: '', password: '', role: 'RECEPTIONIST' };

export function UsersPage() {
  const { user, signOut } = useAuth();
  const [users, setUsers] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);

  async function loadUsers() {
    setLoading(true);
    try {
      setUsers(await listUsers());
    } catch (exception) {
      setError(exception.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { loadUsers(); }, []);

  async function handleCreate(event) {
    event.preventDefault();
    setError('');
    setMessage('');
    try {
      await createUser(form);
      setForm(initialForm);
      setMessage('Đã tạo tài khoản nhân viên.');
      await loadUsers();
    } catch (exception) {
      setError(exception.message);
    }
  }

  async function toggleUser(target) {
    setError('');
    try {
      await updateUser(target.id, { enabled: !target.enabled });
      await loadUsers();
    } catch (exception) {
      setError(exception.message);
    }
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div><span className="topbar-logo">CFC</span><span className="topbar-title">Lab Management</span></div>
        <div className="topbar-actions"><span>{user?.displayName ?? user?.email}</span><button className="ghost-button" onClick={signOut}>Đăng xuất</button></div>
      </header>
      <section className="page-content">
        <div className="page-heading"><div><p className="eyebrow">QUẢN TRỊ HỆ THỐNG</p><h1>Tài khoản nhân viên</h1><p className="muted">Cấp quyền truy cập nội bộ. Hệ thống không có đăng ký công khai.</p></div></div>
        <div className="content-grid">
          <section className="surface-card">
            <h2>Tạo tài khoản</h2>
            <form className="user-form" onSubmit={handleCreate}>
              <label>Email công việc<input type="email" autoComplete="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} placeholder="ten@cfc.vn" required /></label>
              <label>Tên hiển thị<input value={form.displayName} onChange={(event) => setForm({ ...form, displayName: event.target.value })} required /></label>
              <label>Mật khẩu tạm thời<input type="password" minLength="12" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} required /></label>
              <label>Vai trò<select value={form.role} onChange={(event) => setForm({ ...form, role: event.target.value })}><option value="RECEPTIONIST">Tiếp nhận</option><option value="TECHNICIAN">Kỹ thuật viên</option><option value="ADMIN">Quản trị viên</option></select></label>
              <button className="primary-button" type="submit">Tạo tài khoản</button>
            </form>
          </section>
          <section className="surface-card users-card">
            <div className="card-heading"><h2>Danh sách tài khoản</h2><button className="ghost-button" onClick={loadUsers}>Làm mới</button></div>
            {message && <p className="success-note" role="status">{message}</p>}
            {error && <p className="form-error" role="alert">{error}</p>}
            {loading ? <p className="muted">Đang tải danh sách…</p> : <div className="table-wrap"><table><thead><tr><th>Nhân viên</th><th>Vai trò</th><th>Trạng thái</th><th></th></tr></thead><tbody>{users.map((target) => <tr key={target.id}><td><strong>{target.displayName}</strong><small>{target.email}</small></td><td>{target.role}</td><td><span className={`status-pill ${target.enabled ? 'active' : 'disabled'}`}>{target.enabled ? 'Đang hoạt động' : 'Đã khóa'}</span></td><td><button className="table-action" disabled={target.id === user?.id} onClick={() => toggleUser(target)}>{target.enabled ? 'Khóa' : 'Mở khóa'}</button></td></tr>)}</tbody></table></div>}
          </section>
        </div>
      </section>
    </main>
  );
}
