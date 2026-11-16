import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import cfcLogo from '../../../../../../guideline-assets/cfc-008.png';

export function LoginPage() {
  const { user, signIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '', rememberMe: false });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  if (user) return <Navigate to={location.state?.from ?? '/users'} replace />;

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await signIn(form);
      navigate(location.state?.from ?? '/users', { replace: true });
    } catch (exception) {
      setError(exception.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="login-shell">
      <section className="login-panel" aria-labelledby="login-title">
        <div className="login-brand"><img src={cfcLogo} alt="Logo CFC Cò Bay" width="48" height="48" /><span><strong>CFC Lab</strong><small>Phòng kiểm nghiệm</small></span></div>
        <p className="eyebrow">LAB MANAGEMENT</p>
        <h1 id="login-title">Chào mừng trở lại</h1>
        <p className="muted">Đăng nhập để làm việc trong hệ thống nội bộ.</p>
        {location.state?.message && <p className="success-note" role="status">✓ {location.state.message}</p>}
        <form onSubmit={handleSubmit} className="login-form">
          <label>
            Email công việc
            <input
              type="email"
              autoComplete="email"
              value={form.email}
              onChange={(event) => setForm({ ...form, email: event.target.value })}
              placeholder="ten@cfc.vn"
              required
            />
          </label>
          <div className="login-password-group"><label htmlFor="login-password">Mật khẩu</label>
          <div className="login-password-field">
            <input
              id="login-password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })}
              required
            />
            <button type="button" className="login-password-toggle" aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} aria-pressed={showPassword} onClick={() => setShowPassword((visible) => !visible)}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z"/><circle cx="12" cy="12" r="2.7"/>{showPassword && <path d="M4 4 20 20"/>}</svg>
            </button>
          </div></div>
          <label className="remember-login">
            <input
              type="checkbox"
              checked={form.rememberMe}
              onChange={(event) => setForm({ ...form, rememberMe: event.target.checked })}
            />
            <span>
              <strong>Ghi nhớ đăng nhập</strong>
              <small>Duy trì phiên trên thiết bị này trong 30 ngày.</small>
            </span>
          </label>
          {error && <p className="form-error" role="alert">{error}</p>}
          <button className="primary-button" type="submit" disabled={submitting}>
            {submitting ? 'Đang kiểm tra…' : 'Đăng nhập'}
          </button>
        </form>
        <p className="security-note">Chỉ tài khoản do quản trị viên tạo mới có thể truy cập.</p>
      </section>
      <aside className="login-aside">
        <p className="eyebrow">QUY TRÌNH RÕ RÀNG</p>
        <h2>Từ tiếp nhận đến kết quả, mọi bước đều có dấu vết.</h2>
      </aside>
    </main>
  );
}
