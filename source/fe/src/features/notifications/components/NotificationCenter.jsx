import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useNotifications } from '../hooks/useNotifications';

export function NotificationCenter() {
  const navigate = useNavigate();
  const { notifications, unreadCount, connectionState, markRead, markAllRead } = useNotifications();
  const [open, setOpen] = useState(false);
  const rootRef = useRef(null);

  useEffect(() => {
    const close = (event) => {
      if (!rootRef.current?.contains(event.target)) setOpen(false);
    };
    document.addEventListener('pointerdown', close);
    return () => document.removeEventListener('pointerdown', close);
  }, []);

  async function openNotification(notification) {
    await markRead(notification).catch(() => undefined);
    setOpen(false);
    if (notification.targetUrl) navigate(notification.targetUrl);
  }

  return <div className="notification-center" ref={rootRef}>
    <button className="notification-trigger" type="button" aria-label={`Thông báo, ${unreadCount} chưa đọc`} aria-expanded={open} onClick={() => setOpen((value) => !value)}>
      <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" /></svg>
      {unreadCount > 0 && <span className="notification-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>}
    </button>
    {open && <section className="notification-popover" aria-label="Thông báo mới nhất">
      <header><div><strong>Thông báo</strong><small><i className={`realtime-dot ${connectionState}`} />{connectionState === 'connected' ? 'Đang cập nhật trực tiếp' : 'Đang kết nối lại'}</small></div>{unreadCount > 0 && <button type="button" onClick={() => markAllRead().catch(() => undefined)}>Đọc tất cả</button>}</header>
      <div className="notification-preview-list">
        {notifications.length === 0
          ? <div className="notification-empty"><span>✓</span><p>Chưa có thông báo mới.</p></div>
          : notifications.map((notification) => <button className={`notification-preview ${notification.readAt ? '' : 'unread'}`} type="button" key={notification.id} onClick={() => openNotification(notification)}>
            <span className="notification-type-icon">{typeIcon(notification.type)}</span>
            <span><strong>{notification.title}</strong><small>{notification.message}</small><time>{formatTime(notification.createdAt)}</time></span>
          </button>)}
      </div>
      <button className="notification-more" type="button" onClick={() => { setOpen(false); navigate('/admin/notifications'); }}>Xem tất cả thông báo <span>→</span></button>
    </section>}
  </div>;
}

export function typeIcon(type) {
  if (type === 'INTAKE_RECORDED') return 'TN';
  if (type === 'TRANSFER_SUBMITTED') return 'KCS';
  if (type === 'TRANSFER_ACCEPTED') return '✓';
  return '!';
}

export function formatTime(value) {
  if (!value) return '';
  const date = new Date(value);
  const today = new Date();
  if (date.toDateString() === today.toDateString()) return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}
