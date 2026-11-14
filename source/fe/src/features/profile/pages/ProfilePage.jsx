import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { changePassword, deleteAvatar, getProfile, updateProfile, uploadAvatar } from '../api/profileApi';
import { UserAvatar } from '../components/UserAvatar';
import { formatBytes, prepareAvatar } from '../utils/prepareAvatar';

const roleLabels = { ADMIN: 'Quản trị viên', RECEPTIONIST: 'Nhân viên tiếp nhận', TECHNICIAN: 'Kỹ thuật viên KCS' };

export function ProfilePage() {
  const { user, updateCurrentUser, signOut } = useAuth();
  const navigate = useNavigate();
  const fileInput = useRef(null);
  const [profile, setProfile] = useState(user);
  const [displayName, setDisplayName] = useState(user?.displayName ?? '');
  const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [loading, setLoading] = useState(true);
  const [profileSaving, setProfileSaving] = useState(false);
  const [avatarSaving, setAvatarSaving] = useState(false);
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    getProfile()
      .then((nextProfile) => {
        if (!active) return;
        setProfile(nextProfile);
        setDisplayName(nextProfile.displayName);
        updateCurrentUser(nextProfile);
      })
      .catch((exception) => active && setError(exception.message))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [updateCurrentUser]);

  function applyProfile(nextProfile) {
    setProfile(nextProfile);
    setDisplayName(nextProfile.displayName);
    updateCurrentUser(nextProfile);
  }

  async function handleProfileSubmit(event) {
    event.preventDefault();
    setProfileSaving(true); setError(''); setMessage('');
    try {
      applyProfile(await updateProfile({ displayName }));
      setMessage('Đã cập nhật thông tin cá nhân.');
    } catch (exception) { setError(exception.message); }
    finally { setProfileSaving(false); }
  }

  async function handleAvatarChange(event) {
    const source = event.target.files?.[0];
    event.target.value = '';
    if (!source) return;
    setAvatarSaving(true); setError(''); setMessage('');
    try {
      const optimized = await prepareAvatar(source);
      const result = await uploadAvatar(optimized);
      applyProfile(result.user);
      setMessage(`Đã đổi avatar. File lưu ${formatBytes(result.storedBytes)}, kích thước ${result.width}×${result.height} px.`);
    } catch (exception) { setError(exception.message); }
    finally { setAvatarSaving(false); }
  }

  async function handleAvatarDelete() {
    setAvatarSaving(true); setError(''); setMessage('');
    try {
      applyProfile(await deleteAvatar());
      setMessage('Đã xóa avatar, hệ thống chuyển về ký tự đại diện.');
    } catch (exception) { setError(exception.message); }
    finally { setAvatarSaving(false); }
  }

  async function handlePasswordSubmit(event) {
    event.preventDefault();
    setError(''); setMessage('');
    if (passwords.newPassword !== passwords.confirmPassword) {
      setError('Mật khẩu xác nhận chưa khớp.'); return;
    }
    setPasswordSaving(true);
    try {
      await changePassword({ currentPassword: passwords.currentPassword, newPassword: passwords.newPassword });
      await signOut();
      navigate('/login', { replace: true, state: { message: 'Đã đổi mật khẩu. Hãy đăng nhập lại trên thiết bị này.' } });
    } catch (exception) { setError(exception.message); }
    finally { setPasswordSaving(false); }
  }

  if (loading) return <section className="admin-page"><p className="muted">Đang tải thông tin cá nhân…</p></section>;

  return <section className="admin-page profile-page">
    <header className="admin-heading"><div><p className="section-kicker">Tài khoản cá nhân</p><h1>Thông tin của tôi</h1><p className="heading-copy">Quản lý nhận diện nhân sự và bảo mật đăng nhập của chính bạn.</p></div></header>
    {message && <p className="success-note profile-feedback" role="status">✓ {message}</p>}
    {error && <p className="form-error profile-feedback" role="alert">{error}</p>}
    <div className="profile-layout">
      <aside className="profile-identity panel-card">
        <div className="profile-avatar-frame"><UserAvatar user={profile} className="profile-avatar" alt={`Avatar của ${profile?.displayName}`} /><span className="profile-presence" title="Tài khoản đang hoạt động" /></div>
        <div><h2>{profile?.displayName}</h2><p>{roleLabels[profile?.role] ?? profile?.role}</p></div>
        <dl><div><dt>Email đăng nhập</dt><dd>{profile?.email}</dd></div><div><dt>Trạng thái</dt><dd>{profile?.enabled ? 'Đang hoạt động' : 'Đã khóa'}</dd></div><div><dt>Cập nhật gần nhất</dt><dd>{formatDateTime(profile?.updatedAt)}</dd></div></dl>
        <input ref={fileInput} className="visually-hidden" type="file" accept="image/jpeg,image/png" onChange={handleAvatarChange} />
        <button type="button" className="primary-button" disabled={avatarSaving} onClick={() => fileInput.current?.click()}>{avatarSaving ? 'Đang tối ưu ảnh…' : profile?.avatarUrl ? 'Đổi avatar' : 'Tải avatar lên'}</button>
        {profile?.avatarUrl && <button type="button" className="profile-remove-avatar" disabled={avatarSaving} onClick={handleAvatarDelete}>Xóa avatar</button>}
        <small>JPG/PNG tối đa 10 MB ở thiết bị. Trước khi gửi, ảnh được thu nhỏ; máy chủ chỉ lưu JPEG 320×320 và không giữ ảnh gốc.</small>
      </aside>

      <div className="profile-forms">
        <form className="panel-card profile-form" onSubmit={handleProfileSubmit}>
          <div className="profile-section-heading"><span>Hồ sơ</span><div><h2>Thông tin hiển thị</h2><p>Tên này xuất hiện trên phiếu, lịch sử thao tác và thông báo nội bộ.</p></div></div>
          <label>Tên hiển thị<input value={displayName} minLength="2" maxLength="120" onChange={(event) => setDisplayName(event.target.value)} required /></label>
          <label>Email<input value={profile?.email ?? ''} readOnly aria-readonly="true" /><small>Email là định danh đăng nhập, chỉ Admin mới thay đổi được.</small></label>
          <div className="profile-form-actions"><button className="primary-button" disabled={profileSaving || displayName.trim() === profile?.displayName} type="submit">{profileSaving ? 'Đang lưu…' : 'Lưu thông tin'}</button></div>
        </form>

        <form className="panel-card profile-form" onSubmit={handlePasswordSubmit}>
          <div className="profile-section-heading"><span>Bảo mật</span><div><h2>Đổi mật khẩu</h2><p>Sau khi đổi, mọi phiên đăng nhập sẽ bị thu hồi và bạn cần đăng nhập lại.</p></div></div>
          <div className="profile-password-grid"><label>Mật khẩu hiện tại<input type="password" autoComplete="current-password" value={passwords.currentPassword} onChange={(event) => setPasswords((current) => ({ ...current, currentPassword: event.target.value }))} required /></label><label>Mật khẩu mới<input type="password" autoComplete="new-password" minLength="10" maxLength="128" value={passwords.newPassword} onChange={(event) => setPasswords((current) => ({ ...current, newPassword: event.target.value }))} required /><small>Tối thiểu 10 ký tự.</small></label><label>Xác nhận mật khẩu mới<input type="password" autoComplete="new-password" minLength="10" maxLength="128" value={passwords.confirmPassword} onChange={(event) => setPasswords((current) => ({ ...current, confirmPassword: event.target.value }))} required /></label></div>
          <div className="profile-form-actions"><button className="outline-button" disabled={passwordSaving} type="submit">{passwordSaving ? 'Đang đổi mật khẩu…' : 'Đổi mật khẩu'}</button></div>
        </form>
      </div>
    </div>
  </section>;
}

function formatDateTime(value) {
  return value ? new Date(value).toLocaleString('vi-VN') : '—';
}
