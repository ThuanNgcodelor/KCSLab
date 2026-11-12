import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listUsers, updateUser } from '../api/usersApi';
import { useAuth } from '../../auth/hooks/useAuth';

const roleConfig = {
  ADMIN: { label: 'Quản trị viên', description: 'Toàn quyền vận hành hệ thống', permissions: ['Dashboard & báo cáo', 'Quản lý nhân viên', 'Quản lý phòng lab', 'Cấu hình hệ thống'] },
  RECEPTIONIST: { label: 'Tiếp nhận', description: 'Tiếp nhận mẫu và theo dõi tiến độ', permissions: ['Tiếp nhận mẫu', 'Quản lý khách hàng', 'Theo dõi tiến độ', 'Xem báo cáo'] },
  TECHNICIAN: { label: 'Kỹ thuật viên', description: 'Xử lý phân tích và cập nhật kết quả', permissions: ['Xem mẫu được giao', 'Cập nhật kết quả', 'Quản lý thiết bị', 'Xem hướng dẫn kỹ thuật'] },
};

export { roleConfig };

export function UsersPage() {
  const { user } = useAuth(); const navigate = useNavigate();
  const [users, setUsers] = useState([]); const [error, setError] = useState(''); const [message, setMessage] = useState(''); const [loading, setLoading] = useState(true);
  async function loadUsers() { setLoading(true); try { setUsers(await listUsers()); } catch (exception) { setError(exception.message); } finally { setLoading(false); } }
  useEffect(() => { loadUsers(); }, []);
  async function toggleUser(target) { setError(''); try { await updateUser(target.id, { enabled: !target.enabled }); await loadUsers(); } catch (exception) { setError(exception.message); } }

  return <section className="staff-page">
    <header className="admin-heading staff-heading"><div><p className="section-kicker">Không gian quản trị / Nhân viên</p><h1>Nhân viên</h1><p className="heading-copy">Quản lý tài khoản, vai trò và quyền truy cập của đội ngũ CFC Lab.</p></div><button className="primary-button add-staff-button" onClick={() => navigate('/admin/users/new')}>＋ Thêm nhân viên</button></header>
    {message && <p className="success-note toast-note" role="status">✓ {message}</p>}{error && <p className="form-error toast-note" role="alert">{error}</p>}
    <div className="staff-overview"><div><span>Tổng nhân viên</span><strong>{users.length || '—'}</strong></div><div><span>Đang hoạt động</span><strong className="green-number">{users.filter((target) => target.enabled).length || '—'}</strong></div><div><span>Quản trị viên</span><strong>{users.filter((target) => target.role === 'ADMIN').length || '—'}</strong></div><div><span>Cập nhật gần nhất</span><strong className="overview-date">Vừa xong</strong></div></div>
    <section className="panel-card permissions-card"><div className="panel-heading"><div><p className="section-kicker">Ma trận quyền</p><h2>Vai trò trong hệ thống</h2></div><span className="permission-note">Quyền được áp dụng theo vai trò</span></div><div className="role-cards">{Object.entries(roleConfig).map(([key, role]) => <article className="role-card" key={key}><div className={`role-symbol role-${key.toLowerCase()}`}>{key === 'ADMIN' ? '⌘' : key === 'TECHNICIAN' ? '⚗' : '↗'}</div><h3>{role.label}</h3><p>{role.description}</p><div className="permission-list">{role.permissions.map((permission) => <span key={permission}>✓ {permission}</span>)}</div></article>)}</div></section>
    <section className="panel-card staff-table-card"><div className="panel-heading"><div><p className="section-kicker">Danh sách tài khoản</p><h2>Đội ngũ CFC Lab</h2></div><button className="outline-button small-button" onClick={loadUsers}>↻ Làm mới</button></div>{loading ? <p className="muted">Đang tải danh sách…</p> : <div className="table-wrap"><table className="staff-table"><thead><tr><th>Nhân viên</th><th>Vai trò</th><th>Trạng thái</th><th>Lần cập nhật</th><th /></tr></thead><tbody>{users.map((target) => <tr key={target.id}><td><div className="staff-person"><span className="staff-avatar">{target.displayName?.slice(0, 1) ?? '?'}</span><span><strong>{target.displayName}</strong><small>{target.email}</small></span></div></td><td><span className="role-label">{roleConfig[target.role]?.label ?? target.role}</span></td><td><span className={`status-pill ${target.enabled ? 'active' : 'disabled'}`}><i />{target.enabled ? 'Đang hoạt động' : 'Đã khóa'}</span></td><td className="muted-cell">Vừa cập nhật</td><td><div className="table-actions"><button className="table-action" onClick={() => navigate(`/admin/users/${target.id}/edit`)}>Sửa quyền</button><button className="table-action" disabled={target.id === user?.id} onClick={() => toggleUser(target)}>{target.enabled ? 'Khóa' : 'Mở khóa'}</button></div></td></tr>)}</tbody></table></div>}</section>
  </section>;
}
