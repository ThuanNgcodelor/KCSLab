import { useNavigate } from 'react-router-dom';
import { useNotifications } from '../hooks/useNotifications';
import { typeIcon } from './NotificationCenter';

export function NotificationToasts() {
  const navigate = useNavigate();
  const { toasts, dismissToast, markRead } = useNotifications();

  async function open(notification) {
    dismissToast(notification.id);
    try { await markRead(notification); } catch { /* The notification remains unread if the request fails. */ }
    if (notification.targetUrl) navigate(notification.targetUrl);
  }

  return <div className="notification-toast-stack" aria-live="polite" aria-relevant="additions">
    {toasts.map((notification) => <article className="notification-toast" key={notification.id}>
      <span className="notification-type-icon" aria-hidden="true">{typeIcon(notification.type)}</span>
      <div className="notification-toast-copy"><strong>{notification.title}</strong><p>{notification.message}</p>
        {notification.targetUrl && <button type="button" className="notification-toast-open" onClick={() => open(notification)}>Xem chi tiết</button>}
      </div>
      <button type="button" className="notification-toast-close" aria-label="Đóng thông báo" onClick={() => dismissToast(notification.id)}>×</button>
    </article>)}
  </div>;
}
