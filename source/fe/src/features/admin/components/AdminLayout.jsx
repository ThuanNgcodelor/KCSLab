import { useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { NotificationCenter } from '../../notifications/components/NotificationCenter';
import { NotificationProvider } from '../../notifications/hooks/useNotifications';
import { NotificationToasts } from '../../notifications/components/NotificationToasts';
import cfcLogo from '../../../assets/brand/cfc-logo.png';
import { UserAvatar } from '../../profile/components/UserAvatar';
import './workspace.css';

const workspaceNames = {
  ADMIN: 'Điều hành phòng lab',
  RECEPTIONIST: 'Tiếp nhận mẫu',
  TECHNICIAN: 'Kiểm tra chất lượng',
};

const sidebarStorageKey = 'cfc:sidebar-collapsed:v1';

function readSidebarPreference() {
  try {
    return window.localStorage.getItem(sidebarStorageKey) === 'true';
  } catch {
    return false;
  }
}

function Icon({ name }) {
  const paths = {
    grid: 'M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z',
    users: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75',
    file: 'M14 3H7.5A1.5 1.5 0 0 0 6 4.5v15A1.5 1.5 0 0 0 7.5 21h9a1.5 1.5 0 0 0 1.5-1.5V7ZM14 3v4h4M9 12h6M9 16h6',
    flask: 'M9 3h6M10 3v6.4L4.5 19a2 2 0 0 0 1.7 3h11.6a2 2 0 0 0 1.7-3L14 9.4V3M7 16h10',
    transfer: 'M4 8h13M14 5l3 3-3 3M20 16H7M10 13l-3 3 3 3',
    bell: 'M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4',
    settings: 'M12 15.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7ZM19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-1.9 1.9-.06-.06a1.7 1.7 0 0 0-1.88-.34 1.7 1.7 0 0 0-1.03 1.56V20h-2.68v-.09a1.7 1.7 0 0 0-1.03-1.56 1.7 1.7 0 0 0-1.88.34l-.06.06-1.9-1.9.06-.06A1.7 1.7 0 0 0 7.8 15a1.7 1.7 0 0 0-1.56-1.03H6v-2.68h.24A1.7 1.7 0 0 0 7.8 10.3a1.7 1.7 0 0 0-.34-1.88L7.4 8.36l1.9-1.9.06.06a1.7 1.7 0 0 0 1.88.34 1.7 1.7 0 0 0 1.03-1.56V5h2.68v.3a1.7 1.7 0 0 0 1.03 1.56 1.7 1.7 0 0 0 1.88-.34l.06-.06 1.9 1.9-.06.06a1.7 1.7 0 0 0-.34 1.88 1.7 1.7 0 0 0 1.56 1.03H21v2.68h-.04A1.7 1.7 0 0 0 19.4 15Z',
    profile: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM4 21a8 8 0 0 1 16 0',
    inbox: 'M4 13.5 6.5 5.5A1.5 1.5 0 0 1 8 4.5h8a1.5 1.5 0 0 1 1.5 1l2.5 8M4 13.5V18a1.5 1.5 0 0 0 1.5 1.5h13A1.5 1.5 0 0 0 20 18v-4.5h-4.5a3.5 3.5 0 0 1-7 0Z',
    method: 'M5 4.5h14M5 9.5h14M5 14.5h9M5 19.5h6M17.5 15.5l1.5 1.5 3-3',
    result: 'M8 3.5h8M9 3.5v2h6v-2M6.5 5.5h11A1.5 1.5 0 0 1 19 7v12.5A1.5 1.5 0 0 1 17.5 21h-11A1.5 1.5 0 0 1 5 19.5V7a1.5 1.5 0 0 1 1.5-1.5ZM8.5 13l2.2 2.2L15.5 10.5',
    collapse: 'M4 5h16v14H4zM9 5v14m7-10-3 3 3 3',
    expand: 'M4 5h16v14H4zM9 5v14m4-10 3 3-3 3',
    logout: 'M10 4H5v16h5m4-12 4 4-4 4m4-4H9',
  };
  return <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[name]} /></svg>;
}

export function AdminLayout() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(readSidebarPreference);
  const isAdmin = user?.role === 'ADMIN';
  const canReceive = isAdmin || user?.role === 'RECEPTIONIST';
  const canTest = isAdmin || user?.role === 'TECHNICIAN';
  const roleLabel = { ADMIN: 'Quản trị viên', RECEPTIONIST: 'Nhân viên tiếp nhận', TECHNICIAN: 'Người nhận mẫu KCS' }[user?.role] ?? 'Nhân viên';
  const pageTitle = getPageTitle(location.pathname);
  const sidebarToggleLabel = sidebarCollapsed ? 'Mở rộng thanh điều hướng' : 'Thu gọn thanh điều hướng';

  function toggleSidebar() {
    const next = !sidebarCollapsed;
    setSidebarCollapsed(next);
    try {
      window.localStorage.setItem(sidebarStorageKey, String(next));
    } catch {
      // The current session still supports the toggle when storage is unavailable.
    }
  }

  return (
    <NotificationProvider><NotificationToasts /><div className="admin-shell" data-role={user?.role}
      onKeyDown={(event) => { if (event.key === 'Escape' && mobileOpen) setMobileOpen(false); }}>
      <a className="workspace-skip" href="#workspace-content">Chuyển đến nội dung</a>
      <aside id="workspace-sidebar" className={`admin-sidebar ${mobileOpen ? 'mobile-open' : ''}`} data-collapsed={sidebarCollapsed}>
        <div className="sidebar-brand">
          <img className="sidebar-logo" src={cfcLogo} alt="Logo CFC Cò Bay" width="58" height="40" />
          <div className="sidebar-brand-copy"><strong>CFC Lab</strong></div>
          <button type="button" className="sidebar-toggle" onClick={toggleSidebar} aria-controls="workspace-sidebar"
            aria-expanded={!sidebarCollapsed} aria-label={sidebarToggleLabel} title={sidebarToggleLabel}>
            <Icon name={sidebarCollapsed ? 'expand' : 'collapse'} />
          </button>
        </div>
        <div className="sidebar-section-label">{workspaceNames[user?.role]}</div>
        <nav className="admin-nav" aria-label="Điều hướng công việc">
          {isAdmin && <NavLink title="Tổng quan" to="/admin" end onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="grid" /><span>Tổng quan</span></NavLink>}
          {isAdmin && <NavLink title="Nhân viên" to="/admin/users" onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="users" /><span>Nhân viên</span></NavLink>}
          {isAdmin && <NavLink title="Phương pháp thử" to="/admin/test-methods" onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="method" /><span>Phương pháp thử</span></NavLink>}
          {user?.role === 'RECEPTIONIST' && <NavLink title="Tổng quan" to="/admin/intake" end onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="grid" /><span>Tổng quan</span></NavLink>}
          {canReceive && <NavLink title="Yêu cầu kiểm nghiệm" to="/admin/requests" onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="file" /><span>Yêu cầu kiểm nghiệm</span></NavLink>}
          {canReceive && <NavLink title="Tiếp nhận mẫu" to="/admin/intake/receipts" onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="inbox" /><span>Tiếp nhận mẫu</span></NavLink>}
          {user?.role === 'TECHNICIAN' && <NavLink title="Tổng quan" to="/admin/receiving" end onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="grid" /><span>Tổng quan</span></NavLink>}
          <NavLink title={user?.role === 'TECHNICIAN' ? 'Nhận mẫu' : 'Chuyển mẫu'} to={user?.role === 'TECHNICIAN' ? '/admin/receiving/transfers' : '/admin/transfers'} onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="transfer" /><span>{user?.role === 'TECHNICIAN' ? 'Nhận mẫu' : 'Chuyển mẫu'}</span></NavLink>
          {canTest && <NavLink title="Thử nghiệm" to="/admin/testing" onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="flask" /><span>Thử nghiệm</span></NavLink>}
          {canTest && <NavLink title="Hồ sơ kết quả" to="/admin/results" onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="result" /><span>Hồ sơ kết quả</span></NavLink>}
          <NavLink title="Thông báo" to="/admin/notifications" onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="bell" /><span>Thông báo</span></NavLink>
        </nav>
        <div className="sidebar-section-label">Hệ thống</div>
        <nav className="admin-nav">
          {isAdmin && <NavLink title="Thông báo Zalo" to="/admin/customer-notifications" onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="bell" /><span>Thông báo Zalo</span></NavLink>}
          <NavLink title="Thông tin cá nhân" to="/admin/profile" onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="profile" /><span>Thông tin cá nhân</span></NavLink>
        </nav>
        <div className="sidebar-bottom">
          <div className="sidebar-status" title="Hệ thống hoạt động"><span className="pulse-dot" /><span className="sidebar-status-text">Hệ thống hoạt động</span></div>
          <button className="sidebar-profile" title="Thông tin cá nhân" aria-label="Thông tin cá nhân" onClick={() => { setMobileOpen(false); navigate('/admin/profile'); }}>
            <UserAvatar user={user} className="sidebar-user-avatar" />
            <span className="sidebar-profile-copy"><strong>{user?.displayName ?? roleLabel}</strong><small>{roleLabel}</small></span>
            <span className="profile-more">•••</span>
          </button>
          <button className="sidebar-logout" title="Đăng xuất" aria-label="Đăng xuất" onClick={signOut}><Icon name="logout" /><span>Đăng xuất</span></button>
        </div>
      </aside>
      {mobileOpen && <button className="mobile-nav-backdrop" onClick={() => setMobileOpen(false)} aria-label="Đóng menu" />}
      <main className="admin-main">
        <header className="admin-topbar">
          <div className="workspace-location">
            <button className="mobile-menu workspace-menu" onClick={() => setMobileOpen((open) => !open)} aria-expanded={mobileOpen} aria-controls="workspace-sidebar" aria-label={mobileOpen ? 'Đóng menu' : 'Mở menu'}>{mobileOpen ? '×' : '☰'}</button>
            <button className="workspace-home" onClick={() => navigate('/admin')} aria-label="Tổng quan CFC Lab"><img src={cfcLogo} alt="" width="32" height="24" /></button>
            <div><small>{workspaceNames[user?.role]}</small><strong>{pageTitle}</strong></div>
          </div>
          <NotificationCenter />
        </header>
        <div className="admin-content" id="workspace-content" tabIndex={-1}><Outlet /></div>
        <AdminFooter />
      </main>
    </div></NotificationProvider>
  );
}

function getPageTitle(pathname) {
  if (pathname.includes('/customer-notifications')) return 'Thông báo Zalo';
  if (pathname.includes('/profile')) return 'Thông tin cá nhân';
  if (pathname.includes('/notifications')) return 'Thông báo';
  if (pathname.endsWith('/sampling-plan')) return 'Kế hoạch lấy mẫu';
  if (pathname.endsWith('/field-sampling')) return 'Lấy mẫu hiện trường';
  if (pathname.includes('/requests')) return 'Phiếu yêu cầu kiểm nghiệm';
  if (pathname.includes('/intake/new')) return 'Tạo hồ sơ tiếp nhận';
  if (pathname.replace(/\/$/, '') === '/admin/intake') return 'Tổng quan';
  if (pathname.includes('/intake')) return 'Tiếp nhận mẫu';
  if (pathname.includes('/receiving/transfers')) return 'Nhận mẫu';
  if (pathname.includes('/receiving')) return 'Tổng quan nhận mẫu';
  if (pathname.includes('/transfers')) return 'Chuyển mẫu';
  if (pathname.includes('/results')) return 'Hồ sơ kết quả';
  if (pathname.includes('/testing')) return 'Thử nghiệm';
  if (pathname.includes('/users')) return 'Nhân viên';
  if (pathname.includes('/test-methods')) return 'Phương pháp thử';
  return 'Tổng quan';
}

function AdminFooter() {
  return (
    <footer className="admin-footer">
      <div className="footer-frame">
        <section className="footer-identity">
          <div className="footer-spectrum" aria-hidden="true">
            <i /><i /><i /><i /><i /><i /><i />
          </div>
          <a className="footer-brand" href="https://www.cfccobay.com/" target="_blank" rel="noreferrer">
            <img className="footer-logo" src={cfcLogo} alt="" width="58" height="40" loading="lazy" />
            <span><strong>CFC CÒ BAY</strong><small>Agriculture &amp; Technology</small></span>
          </a>
          <p>Từ một mẫu thử đến một quyết định đáng tin cậy.</p>
          <a className="footer-website" href="https://www.cfccobay.com/" target="_blank" rel="noreferrer">
            Khám phá CFC Cò Bay <span aria-hidden="true">↗</span>
          </a>
        </section>

        <div className="footer-directory">
          <section className="footer-contact">
            <h2>Liên hệ phòng LAB</h2>
            <address>Công ty CP Phân bón &amp; Hóa chất Cần Thơ<br />Trục Chính Khu Công Nghiệp Trà Nóc 1,<br />phường Thới An Đông, thành phố Cần Thơ</address>
            <div><a href="tel:19005307">1900 5307</a><a href="mailto:info@cfccobay.com">info@cfccobay.com</a></div>
          </section>
          <FooterLinks title="Về CFC" links={[['Giới thiệu', '/gioi-thieu'], ['Tin doanh nghiệp', '/tin-tuc'], ['Khảo nghiệm đồng ruộng', '/khao-nghiem-dong-ruong']]} />
          <FooterLinks title="Hỗ trợ" links={[['Tìm kiếm', '/search'], ['Liên hệ', '/lien-he'], ['Chính sách', '/chinh-sach']]} />
        </div>
      </div>

      <div className="footer-bottom">
        <span>© 2026 CFC Cò Bay</span>
        <span>CFC Lab Management — hệ thống nội bộ</span>
      </div>
    </footer>
  );
}

function FooterLinks({ title, links }) {
  return <section className="footer-links"><h2>{title}</h2>{links.map(([label, path]) => <a key={path} href={`https://www.cfccobay.com${path}`} target="_blank" rel="noreferrer">{label}</a>)}</section>;
}
