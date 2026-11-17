import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listUsers, updateUser } from '../api/usersApi';
import { useAuth } from '../../auth/hooks/useAuth';
import { UserAvatar } from '../../profile/components/UserAvatar';
import { StatusBadge } from '../../../shared/components/StatusBadge';
import { PageLoader } from '../../../shared/components/Loading';
import { ListToolbar, Pagination, emptyPage } from '../../../shared/components/ListToolbar';
import { useListState } from '../../../shared/hooks/useListState';
import { Icon } from '../../../shared/components/Icon';

const roleConfig = {
  ADMIN: { label: 'Quản trị viên', description: 'Toàn quyền vận hành hệ thống', permissions: ['Dashboard & báo cáo', 'Quản lý nhân viên', 'Quản lý phòng lab', 'Cấu hình hệ thống'] },
  RECEPTIONIST: { label: 'Tiếp nhận', description: 'Tiếp nhận mẫu và theo dõi tiến độ', permissions: ['Tiếp nhận mẫu', 'Quản lý khách hàng', 'Theo dõi tiến độ', 'Xem báo cáo'] },
  TECHNICIAN: { label: 'Kỹ thuật viên', description: 'Xử lý phân tích và cập nhật kết quả', permissions: ['Xem mẫu được giao', 'Cập nhật kết quả', 'Quản lý thiết bị', 'Xem hướng dẫn kỹ thuật'] },
};

export { roleConfig };

const sortOptions = [
  ['createdAt,desc', 'Mới thêm nhất'], ['createdAt,asc', 'Cũ nhất'],
  ['displayName,asc', 'Tên: A → Z'], ['displayName,desc', 'Tên: Z → A'],
  ['email,asc', 'Email: A → Z'], ['role,asc', 'Vai trò: A → Z'], ['enabled,desc', 'Đang hoạt động trước'],
];
const defaults = { q: '', role: '', enabled: '', sort: 'createdAt,desc' };

export function UsersPage() {
  const { user } = useAuth(); const navigate = useNavigate();
  const [list, setList] = useListState(defaults);
  const [data, setData] = useState(emptyPage);
  const [stats, setStats] = useState({ total: null, active: null, admins: null});
  const users = data.content; const [error, setError] = useState(''); const [message, setMessage] = useState(''); const [loading, setLoading] = useState(true);
  const loadUsers = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const [page, total, active, admins] = await Promise.all([
        listUsers(list), listUsers({ size: 1 }), listUsers({ size: 1, enabled: true }), listUsers({ size: 1, role: 'ADMIN' }),
      ]);
      setData(page); setStats({ total: total.totalElements, active: active.totalElements, admins: admins.totalElements });
    } catch (exception) { setError(exception.message); }
    finally { setLoading(false); }
  }, [list]);
  useEffect(() => { loadUsers(); }, [loadUsers]);
  async function toggleUser(target) { setError(''); try { await updateUser(target.id, { enabled: !target.enabled }); await loadUsers(); } catch (exception) { setError(exception.message); } }

  return <section className="staff-page">
    <header className="admin-heading staff-heading"><div><p className="section-kicker">Không gian quản trị / Nhân viên</p><h1>Nhân viên</h1><p className="heading-copy">Quản lý tài khoản, vai trò và quyền truy cập của đội ngũ CFC Lab.</p></div><button className="primary-button add-staff-button" onClick={() => navigate('/admin/users/new')}>＋ Thêm nhân viên</button></header>
    {message && <p className="success-note toast-note" role="status"><Icon name="checkCircle" size={14} /> {message}</p>}{error && <p className="form-error toast-note" role="alert">{error}</p>}
    <div className="staff-overview"><div><span>Tổng nhân viên</span><strong>{stats.total ?? '—'}</strong></div><div><span>Đang hoạt động</span><strong className="green-number">{stats.active ?? '—'}</strong></div><div><span>Quản trị viên</span><strong>{stats.admins ?? '—'}</strong></div><div><span>Cập nhật gần nhất</span><strong className="overview-date">Vừa xong</strong></div></div>
    <section className="panel-card permissions-card"><div className="panel-heading"><div><p className="section-kicker">Ma trận quyền</p><h2>Vai trò trong hệ thống</h2></div><span className="permission-note">Quyền được áp dụng theo vai trò</span></div><div className="role-cards">{Object.entries(roleConfig).map(([key, role]) => <article className="role-card" key={key}><div className={`role-symbol role-${key.toLowerCase()}`}><Icon name={key === 'ADMIN' ? 'shield' : key === 'TECHNICIAN' ? 'flask' : 'inbox'} size={16} /></div><h3>{role.label}</h3><p>{role.description}</p><div className="permission-list">{role.permissions.map((permission) => <span key={permission}><Icon name="check" size={11} /> {permission}</span>)}</div></article>)}</div></section>
    <section className="panel-card staff-table-card"><div className="panel-heading"><div><p className="section-kicker">Danh sách tài khoản</p><h2>Đội ngũ CFC Lab</h2></div><button className="outline-button small-button" onClick={loadUsers}>↻ Làm mới</button></div><ListToolbar q={list.q} onQ={(q) => setList({ q })} searchLabel="Tìm nhân viên" placeholder="Tên hoặc email…" filters={[{ label: 'Vai trò', value: list.role, onChange: (role) => setList({ role }), options: [['', 'Tất cả'], ...Object.entries(roleConfig).map(([value, config]) => [value, config.label])] }, { label: 'Trạng thái', value: list.enabled, onChange: (enabled) => setList({ enabled }), options: [['', 'Tất cả'], ['true', 'Đang hoạt động'], ['false', 'Đã khóa']] }]} sort={list.sort} sortOptions={sortOptions} onSort={(sort) => setList({ sort })} onReset={() => setList({ ...defaults })} />{loading ? <PageLoader label="Đang tải danh sách nhân viên…" rows={3} /> : <div className="table-wrap"><table className="staff-table"><thead><tr><th>Nhân viên</th><th>Vai trò</th><th>Trạng thái</th><th>Lần cập nhật</th><th /></tr></thead><tbody>{users.map((target) => <tr key={target.id}><td data-label="Nhân viên"><div className="staff-person"><UserAvatar user={target} className="staff-avatar" /><span><strong>{target.displayName}</strong><small>{target.email}</small></span></div></td><td data-label="Vai trò"><span className="role-label">{roleConfig[target.role]?.label ?? target.role}</span></td><td data-label="Trạng thái"><StatusBadge domain="account" value={target.enabled ? 'ACTIVE' : 'DISABLED'} /></td><td data-label="Cập nhật" className="muted-cell">{target.updatedAt ? new Date(target.updatedAt).toLocaleString('vi-VN') : '—'}</td><td data-label="Thao tác"><div className="table-actions"><button className="table-action" onClick={() => navigate(`/admin/users/${target.id}/edit`)}>Sửa quyền</button><button className="table-action" disabled={target.id === user?.id} onClick={() => toggleUser(target)}>{target.enabled ? 'Khóa' : 'Mở khóa'}</button></div></td></tr>)}</tbody></table></div>}<Pagination data={data} loading={loading} noun="nhân viên" onPage={(page) => setList({ page })} /></section>
  </section>;
}
