import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import cfcLogo from '../../../assets/brand/cfc-logo.png';
import { InstallGuide } from '../../../shared/components/InstallGuide';

// "/admin" đưa mỗi role về trang tổng quan của mình (Admin, Tiếp nhận, KCS).
const HOME = '/admin';
const FLOW = [
  ['01', 'Tiếp nhận', 'Phiếu yêu cầu, hồ sơ mẫu'],
  ['02', 'Chuyển mẫu', 'Bàn giao sang KCS'],
  ['03', 'Thử nghiệm', 'Biên bản thử nghiệm'],
  ['04', 'Kết quả', 'Phiếu kết quả KQKN'],
];
// Hạt sáng trang trí: vị trí/tốc độ cố định để không đổi mỗi lần render.
const PARTICLES = [
  [6, 14, 9], [14, 22, 7], [22, 9, 11], [31, 18, 8], [38, 26, 10], [47, 12, 7], [55, 20, 9], [63, 8, 12],
  [71, 24, 8], [79, 15, 10], [86, 21, 7], [93, 11, 9], [10, 28, 12], [44, 30, 10], [68, 29, 11], [90, 27, 8],
];
function greeting() {
  const hour = Number(new Intl.DateTimeFormat('en-GB', { hour: '2-digit', hour12: false, timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date()));
  if (hour < 11) return 'Chào buổi sáng';
  if (hour < 14) return 'Chào buổi trưa';
  if (hour < 18) return 'Chào buổi chiều';
  return 'Chào buổi tối';
}
const TODAY = new Intl.DateTimeFormat('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date());

export function LoginPage() {
  const { user, signIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '', rememberMe: false });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [capsLock, setCapsLock] = useState(false);

  if (user) return <Navigate to={location.state?.from ?? HOME} replace />;

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await signIn(form);
      navigate(location.state?.from ?? HOME, { replace: true });
    } catch (exception) {
      setError(exception.message);
    } finally {
      setSubmitting(false);
    }
  }
  const trackCapsLock = (event) => setCapsLock(Boolean(event.getModifierState?.('CapsLock')));
  // Vệt sáng đi theo con trỏ trên thẻ (chỉ đổi biến CSS, không render lại).
  const spotlight = (event) => {
    const box = event.currentTarget.getBoundingClientRect();
    event.currentTarget.style.setProperty('--mx', `${event.clientX - box.left}px`);
    event.currentTarget.style.setProperty('--my', `${event.clientY - box.top}px`);
  };

  return (
    <main className="auth-page">
      <div className="auth-bg" aria-hidden="true">
        <i className="auth-aurora auth-aurora--green" />
        <i className="auth-aurora auth-aurora--mint" />
        <i className="auth-aurora auth-aurora--red" />
        <i className="auth-grid" />
        <span className="auth-particles">{PARTICLES.map(([x, delay, duration], index) => <i key={index} style={{ '--x': `${x}%`, '--delay': `-${delay}s`, '--duration': `${duration + 8}s` }} />)}</span>
      </div>

      <section className="auth-hero" aria-label="Giới thiệu CFC Lab">
        <header className="auth-brand-top">
          <span className="auth-logo-tile"><img src={cfcLogo} alt="Logo CFC Cò Bay" width="64" height="44" /></span>
          <span className="auth-brand-name"><strong>CFC Lab</strong><small>Phòng kiểm nghiệm · CFC Cò Bay</small></span>
        </header>

        <div className="auth-hero-body">
          <div className="auth-copy">
            <p className="auth-kicker"><i aria-hidden="true" />Hệ thống quản lý phòng kiểm nghiệm</p>
            <h2><span className="auth-gradient-text">Từ tiếp nhận đến kết quả,</span> <em>mọi bước đều có dấu vết.</em></h2>
            <p className="auth-lead">Tiếp nhận mẫu, bàn giao KCS, thử nghiệm và phát hành phiếu kết quả trên cùng một hệ thống — rõ người, rõ thời điểm, rõ mã mẫu.</p>
          </div>
          <div className="auth-emblem" aria-hidden="true">
            <span className="auth-emblem-glow" />
            <span className="auth-emblem-ring" />
            <span className="auth-emblem-ring auth-emblem-ring--dashed" />
            <span className="auth-emblem-orbit"><i /><i /><i /></span>
            <span className="auth-emblem-core"><img src={cfcLogo} alt="" width="174" height="120" /></span>
            <span className="auth-chip auth-chip--a"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 3.5 5 6v5.5c0 4.2 2.8 7.4 7 9 4.2-1.6 7-4.8 7-9V6Z" /><path d="m9 12 2.2 2.2L15.5 10" /></svg><b>Truy xuất nguồn gốc mẫu</b></span>
            <span className="auth-chip auth-chip--b"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 3.5 5 6v5.5c0 4.2 2.8 7.4 7 9 4.2-1.6 7-4.8 7-9V6Z" /><path d="m9 12 2.2 2.2L15.5 10" /></svg><b>Mã hóa mẫu</b></span>
            <span className="auth-chip auth-chip--c"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 3.5 5 6v5.5c0 4.2 2.8 7.4 7 9 4.2-1.6 7-4.8 7-9V6Z" /><path d="m9 12 2.2 2.2L15.5 10" /></svg><b>Nhận mẫu</b></span>

          </div>
        </div>

        <ol className="auth-flow" aria-label="Quy trình xử lý mẫu">
          {FLOW.map(([number, title, detail], index) => <li key={number} style={{ '--step': index }}><span>{number}</span><strong>{title}</strong><small>{detail}</small></li>)}
        </ol>

        <footer className="auth-hero-foot"><span>© 2026 CFC Cò Bay · Cần Thơ</span><span className="auth-today">{TODAY}</span></footer>
      </section>

      <section className="auth-panel" aria-labelledby="login-title">
        <div className="auth-card-frame" onPointerMove={spotlight}>
          <span className="auth-card-emblem" aria-hidden="true"><i /><img src={cfcLogo} alt="" width="64" height="44" /></span>
          <div className="auth-card">
            <p className="auth-greeting">{greeting()}</p>
            <h1 id="login-title">Đăng nhập <span>CFC Lab</span></h1>
            <p className="auth-subtitle"><i aria-hidden="true" />Dùng tài khoản công việc do quản trị viên cấp</p>
            {location.state?.message && <p className="auth-alert auth-alert--success" role="status">{location.state.message}</p>}
            <form onSubmit={handleSubmit} className="auth-form">
              <label className="auth-float">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><rect x="3.5" y="5.5" width="17" height="13" rx="2.5" /><path d="m4.5 7 7.5 6 7.5-6" /></svg>
                <input
                  type="email"
                  autoComplete="email"
                  value={form.email}
                  onChange={(event) => setForm({ ...form, email: event.target.value })}
                  placeholder=" "
                  required
                />
                <span>Email công việc</span>
              </label>
              <div className="auth-field">
                <label className="auth-float" htmlFor="login-password">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><rect x="5" y="10.5" width="14" height="9.5" rx="2.2" /><path d="M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5" /></svg>
                  <input
                    id="login-password"
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="current-password"
                    value={form.password}
                    onChange={(event) => setForm({ ...form, password: event.target.value })}
                    onKeyUp={trackCapsLock}
                    onKeyDown={trackCapsLock}
                    onBlur={() => setCapsLock(false)}
                    placeholder=" "
                    required
                  />
                  <span>Mật khẩu</span>
                  <button type="button" className="auth-password-toggle" aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} aria-pressed={showPassword} onClick={() => setShowPassword((visible) => !visible)}>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z" /><circle cx="12" cy="12" r="2.7" />{showPassword && <path d="M4 4 20 20" />}</svg>
                  </button>
                </label>
                {capsLock && <small className="auth-caps" role="status">⇪ Caps Lock đang bật</small>}
              </div>
              <label className="auth-switch">
                <input
                  type="checkbox"
                  role="switch"
                  checked={form.rememberMe}
                  onChange={(event) => setForm({ ...form, rememberMe: event.target.checked })}
                />
                <i aria-hidden="true" />
                <span><strong>Ghi nhớ đăng nhập</strong><small>Giữ phiên trên thiết bị này trong 30 ngày</small></span>
              </label>
              {error && <p className="auth-alert auth-alert--error" role="alert">{error}</p>}
              <button className={`auth-submit${submitting ? ' is-loading' : ''}`} type="submit" disabled={submitting}>
                {submitting && <i className="auth-spinner" aria-hidden="true" />}
                <span>{submitting ? 'Đang kiểm tra…' : 'Đăng nhập'}</span>
                {!submitting && <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M5 12h14M13 6l6 6-6 6" /></svg>}
              </button>
            </form>
            <p className="auth-note">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M12 3.5 5 6v5.5c0 4.2 2.8 7.4 7 9 4.2-1.6 7-4.8 7-9V6Z" /><path d="m9 12 2.2 2.2L15.5 10" /></svg>
              Chỉ tài khoản do quản trị viên tạo mới truy cập được. Quên mật khẩu, hãy liên hệ quản trị viên.
            </p>
            <InstallGuide />
          </div>
        </div>
        <a className="auth-public-link" href="/tracuu/">Khách hàng tra cứu kết quả kiểm nghiệm <span aria-hidden="true">→</span></a>
      </section>
    </main>
  );
}
