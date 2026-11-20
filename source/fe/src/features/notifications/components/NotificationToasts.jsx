import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useNotifications } from '../hooks/useNotifications';
import { NotificationAvatar } from './NotificationAvatar';

export function NotificationToasts() {
  const navigate = useNavigate();
  const { toasts, dismissToast, markRead } = useNotifications();
  const [error, setError] = useState(null);

  async function open(notification) {
    setError(null);
    try {
      await markRead(notification);
      dismissToast(notification.id);
      if (notification.targetUrl) navigate(notification.targetUrl);
    } catch (exception) {
      setError({ id: notification.id, message: exception.message });
    }
  }

  return <div className="notification-toast-stack" aria-live="polite" aria-relevant="additions">
    {toasts.map((notification) => <article className="notification-toast" key={notification.id}>
      <NotificationAvatar notification={notification} />
      <div className="notification-toast-copy"><strong>{notification.title}</strong><p>{notification.message}</p>
        {notification.targetUrl && <button type="button" className="notification-toast-open" onClick={() => open(notification)}>Xem chi tiết</button>}
        {error?.id === notification.id && <p className="notification-read-error" role="alert">{error.message}</p>}
      </div>
      <button type="button" className="notification-toast-close" aria-label="Đóng thông báo" onClick={() => dismissToast(notification.id)}>×</button>
    </article>)}
  </div>;
}
