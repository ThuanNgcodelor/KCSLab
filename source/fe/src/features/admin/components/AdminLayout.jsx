import { useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { NotificationCenter } from '../../notifications/components/NotificationCenter';
import { NotificationProvider } from '../../notifications/hooks/useNotifications';
import cfcLogo from '../../../../../../guideline-assets/cfc-008.png';

function Icon({ name }) {
  const paths = {
    grid: 'M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z',
    users: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75',
    flask: 'M9 3h6M10 3v6.4L4.5 19a2 2 0 0 0 1.7 3h11.6a2 2 0 0 0 1.7-3L14 9.4V3M7 16h10',
    transfer: 'M4 8h13M14 5l3 3-3 3M20 16H7M10 13l-3 3 3 3',
    bell: 'M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4',
    settings: 'M12 15.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7ZM19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-1.9 1.9-.06-.06a1.7 1.7 0 0 0-1.88-.34 1.7 1.7 0 0 0-1.03 1.56V20h-2.68v-.09a1.7 1.7 0 0 0-1.03-1.56 1.7 1.7 0 0 0-1.88.34l-.06.06-1.9-1.9.06-.06A1.7 1.7 0 0 0 7.8 15a1.7 1.7 0 0 0-1.56-1.03H6v-2.68h.24A1.7 1.7 0 0 0 7.8 10.3a1.7 1.7 0 0 0-.34-1.88L7.4 8.36l1.9-1.9.06.06a1.7 1.7 0 0 0 1.88.34 1.7 1.7 0 0 0 1.03-1.56V5h2.68v.3a1.7 1.7 0 0 0 1.03 1.56 1.7 1.7 0 0 0 1.88-.34l.06-.06 1.9 1.9-.06.06a1.7 1.7 0 0 0-.34 1.88 1.7 1.7 0 0 0 1.56 1.03H21v2.68h-.04A1.7 1.7 0 0 0 19.4 15Z',
  };
  return <svg className="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[name]} /></svg>;
}

export function AdminLayout() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);
  const isAdmin = user?.role === 'ADMIN';
  const canReceive = isAdmin || user?.role === 'RECEPTIONIST';
  const canTest = isAdmin || user?.role === 'TECHNICIAN';
  const roleLabel = { ADMIN: 'Quản trị viên', RECEPTIONIST: 'Nhân viên tiếp nhận', TECHNICIAN: 'Kỹ thuật viên' }[user?.role] ?? 'Nhân viên';
  const pageTitle = getPageTitle(location.pathname);

  return (
    <NotificationProvider><div className="admin-shell">
      <aside className={`admin-sidebar ${mobileOpen ? 'mobile-open' : ''}`}>
        <div className="sidebar-brand"><img className="sidebar-logo" src={cfcLogo} alt="" width="38" height="38" /><div><strong>CFC Lab</strong><small>Management system</small></div></div>
        <div className="sidebar-section-label">Không gian quản trị</div>
        <nav className="admin-nav" aria-label="Điều hướng quản trị">
          {isAdmin && <NavLink to="/admin" end onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="grid" /><span>Tổng quan</span></NavLink>}
          {isAdmin && <NavLink to="/admin/users" onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="users" /><span>Nhân viên</span></NavLink>}
          {canReceive && <NavLink to="/admin/intake" onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="flask" /><span>Tiếp nhận mẫu</span></NavLink>}
          <NavLink to="/admin/transfers" onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="transfer" /><span>Chuyển mẫu</span></NavLink>
          {canTest && <NavLink to="/admin/testing" onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="flask" /><span>Thử nghiệm</span></NavLink>}
          <NavLink to="/admin/notifications" onClick={() => setMobileOpen(false)} className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}><Icon name="bell" /><span>Thông báo</span></NavLink>
        </nav>
        <div className="sidebar-section-label">Hệ thống</div>
        <nav className="admin-nav">
          <a className="nav-item disabled" href="#settings" onClick={(event) => event.preventDefault()}><Icon name="settings" /><span>Cài đặt chung</span></a>
        </nav>
        <div className="sidebar-bottom"><div className="sidebar-status"><span className="pulse-dot" />Hệ thống hoạt động</div><button className="sidebar-profile" onClick={() => navigate(isAdmin ? '/admin/users' : '/admin/transfers')}><span className="avatar">{(user?.displayName ?? 'A').slice(0, 1)}</span><span><strong>{user?.displayName ?? roleLabel}</strong><small>{roleLabel}</small></span><span className="profile-more">•••</span></button><button className="sidebar-logout" onClick={signOut}>Đăng xuất</button></div>
      </aside>
      <main className="admin-main"><div className="mobile-nav-bar"><button className="mobile-brand" onClick={() => navigate('/admin')}><img className="mobile-logo" src={cfcLogo} alt="" width="30" height="30" />CFC Lab</button><button className="mobile-menu" onClick={() => setMobileOpen((open) => !open)} aria-expanded={mobileOpen} aria-label={mobileOpen ? 'Đóng menu' : 'Mở menu'}>{mobileOpen ? '×' : '☰'}</button></div>{mobileOpen && <button className="mobile-nav-backdrop" onClick={() => setMobileOpen(false)} aria-label="Đóng menu" />}<header className="admin-topbar"><div><small>Không gian làm việc</small><strong>{pageTitle}</strong></div><NotificationCenter /></header><div className="admin-content"><Outlet /></div><AdminFooter /></main>
    </div></NotificationProvider>
  );
}

function getPageTitle(pathname) {
  if (pathname.includes('/notifications')) return 'Thông báo';
  if (pathname.includes('/intake/new')) return 'Tạo phiếu tiếp nhận';
  if (pathname.includes('/intake')) return 'Tiếp nhận mẫu';
  if (pathname.includes('/transfers')) return 'Chuyển mẫu';
  if (pathname.includes('/testing')) return 'Thử nghiệm';
  if (pathname.includes('/users')) return 'Nhân viên';
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
            <img className="footer-logo" src={cfcLogo} alt="" width="42" height="42" loading="lazy" />
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
