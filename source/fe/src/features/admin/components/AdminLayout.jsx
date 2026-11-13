import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';

function Icon({ name }) {
  const paths = {
    grid: 'M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z',
    users: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75',
    flask: 'M9 3h6M10 3v6.4L4.5 19a2 2 0 0 0 1.7 3h11.6a2 2 0 0 0 1.7-3L14 9.4V3M7 16h10',
    settings: 'M12 15.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7ZM19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-1.9 1.9-.06-.06a1.7 1.7 0 0 0-1.88-.34 1.7 1.7 0 0 0-1.03 1.56V20h-2.68v-.09a1.7 1.7 0 0 0-1.03-1.56 1.7 1.7 0 0 0-1.88.34l-.06.06-1.9-1.9.06-.06A1.7 1.7 0 0 0 7.8 15a1.7 1.7 0 0 0-1.56-1.03H6v-2.68h.24A1.7 1.7 0 0 0 7.8 10.3a1.7 1.7 0 0 0-.34-1.88L7.4 8.36l1.9-1.9.06.06a1.7 1.7 0 0 0 1.88.34 1.7 1.7 0 0 0 1.03-1.56V5h2.68v.3a1.7 1.7 0 0 0 1.03 1.56 1.7 1.7 0 0 0 1.88-.34l.06-.06 1.9 1.9-.06.06a1.7 1.7 0 0 0-.34 1.88 1.7 1.7 0 0 0 1.56 1.03H21v2.68h-.04A1.7 1.7 0 0 0 19.4 15Z',
  };
  return <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[name]} /></svg>;
}

export function AdminLayout() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <div className="admin-shell">
      <aside className={`admin-sidebar ${mobileOpen ? 'mobile-open' : ''}`}>
        <div className="sidebar-brand"><span className="brand-orbit">CFC</span><div><strong>CFC Lab</strong><small>Management system</small></div></div>
        <div className="sidebar-section-label">Không gian quản trị</div>
        <nav className="admin-nav" aria-label="Điều hướng quản trị">
          <NavLink to="/admin" end onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="grid" /><span>Tổng quan</span></NavLink>
          <NavLink to="/admin/users" onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="users" /><span>Nhân viên</span><em>3</em></NavLink>
          <NavLink to="/admin/intake" onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="flask" /><span>Tiếp nhận mẫu</span></NavLink>
          <a className="nav-item disabled" href="#modules" onClick={(event) => event.preventDefault()}><Icon name="flask" /><span>Quản lý phòng lab</span><small>Sắp tới</small></a>
        </nav>
        <div className="sidebar-section-label">Hệ thống</div>
        <nav className="admin-nav">
          <a className="nav-item disabled" href="#settings" onClick={(event) => event.preventDefault()}><Icon name="settings" /><span>Cài đặt chung</span></a>
        </nav>
        <div className="sidebar-bottom"><div className="sidebar-status"><span className="pulse-dot" />Hệ thống hoạt động</div><button className="sidebar-profile" onClick={() => navigate('/admin/users')}><span className="avatar">{(user?.displayName ?? 'A').slice(0, 1)}</span><span><strong>{user?.displayName ?? 'Quản trị viên'}</strong><small>Quản trị viên</small></span><span className="profile-more">•••</span></button><button className="sidebar-logout" onClick={signOut}>Đăng xuất</button></div>
      </aside>
      <main className="admin-main"><div className="mobile-nav-bar"><button className="mobile-brand" onClick={() => navigate('/admin')}><span className="brand-orbit">CFC</span>CFC Lab</button><button className="mobile-menu" onClick={() => setMobileOpen((open) => !open)} aria-expanded={mobileOpen} aria-label={mobileOpen ? 'Đóng menu' : 'Mở menu'}>{mobileOpen ? '×' : '☰'}</button></div>{mobileOpen && <button className="mobile-nav-backdrop" onClick={() => setMobileOpen(false)} aria-label="Đóng menu" />}<Outlet /></main>
    </div>
  );
}
