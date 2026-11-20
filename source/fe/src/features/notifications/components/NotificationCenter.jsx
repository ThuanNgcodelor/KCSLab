import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useNotifications } from '../hooks/useNotifications';
import { Icon } from '../../../shared/components/Icon';
import { NotificationAvatar } from './NotificationAvatar';

export function NotificationCenter() {
  const navigate = useNavigate();
  const { notifications, unreadCount, connectionState, previewError, refresh, markRead, markAllRead } = useNotifications();
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [marking, setMarking] = useState(false);
  const rootRef = useRef(null);
  const triggerRef = useRef(null);

  useEffect(() => {
    if (!open) return;
    const close = (event) => {
      if (!rootRef.current?.contains(event.target)) setOpen(false);
    };
    document.addEventListener('pointerdown', close);
    return () => document.removeEventListener('pointerdown', close);
  }, [open]);

  async function openNotification(notification) {
    setError('');
    try {
      await markRead(notification);
      setOpen(false);
      if (notification.targetUrl) navigate(notification.targetUrl);
    } catch (exception) {
      setError(exception.message);
    }
  }

  async function markAll() {
    setError('');
    setMarking(true);
    try { await markAllRead(); }
    catch (exception) { setError(exception.message); }
    finally { setMarking(false); }
  }

  return <div className="notification-center" ref={rootRef} onKeyDown={(event) => {
    if (event.key === 'Escape' && open) {
      setOpen(false);
      triggerRef.current?.focus();
    }
  }}>
    <button ref={triggerRef} className="notification-trigger" type="button" aria-label={`Thông báo, ${unreadCount} chưa đọc`} aria-expanded={open} onClick={() => { setError(''); setOpen((value) => !value); }}>
      <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" /></svg>
      {unreadCount > 0 && <span className="notification-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>}
    </button>
    {open && <section className="notification-popover" aria-label="Thông báo mới nhất">
      <header><div><strong>Thông báo</strong><small role="status"><i className={`realtime-dot ${connectionState}`} />{connectionState === 'connected' ? 'Đang cập nhật trực tiếp' : connectionState === 'offline' ? 'Tạm mất kết nối trực tiếp' : 'Đang kết nối…'}</small></div>{unreadCount > 0 && <button type="button" disabled={marking} onClick={markAll}>{marking ? 'Đang đánh dấu…' : 'Đọc tất cả'}</button>}</header>
      {(error || previewError) && <p className="notification-read-error" role="alert">{error || previewError}</p>}
      {previewError && <button className="notification-more" type="button" onClick={() => refresh().catch(() => undefined)}>Tải lại thông báo</button>}
      <div className="notification-preview-list">
        {notifications.length === 0
          ? !previewError && <div className="notification-empty"><span><Icon name="checkCircle" size={22} /></span><p>Chưa có thông báo mới.</p></div>
          : notifications.map((notification) => <button className={`notification-preview ${notification.readAt ? '' : 'unread'}`} type="button" key={notification.id} onClick={() => openNotification(notification)}>
            <NotificationAvatar notification={notification} />
            <span><strong>{notification.title}</strong><small>{notification.message}</small><time>{formatTime(notification.createdAt)}</time></span>
          </button>)}
      </div>
      <button className="notification-more" type="button" onClick={() => { setOpen(false); navigate('/admin/notifications'); }}>Xem tất cả thông báo <span>→</span></button>
    </section>}
  </div>;
}

export function formatTime(value) {
  if (!value) return '';
  const date = new Date(value);
  const today = new Date();
  if (date.toDateString() === today.toDateString()) return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}
