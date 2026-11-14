import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export function LoginPage() {
  const { user, signIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '', rememberMe: false });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

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
        <div className="brand-mark" aria-hidden="true"><img src="/cfc-mark.png" alt="" /></div>
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
          <label>
            Mật khẩu
            <input
              type="password"
              autoComplete="current-password"
              value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })}
              required
            />
          </label>
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
