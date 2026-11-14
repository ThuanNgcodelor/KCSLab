import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listNotifications, markAllNotificationsRead, markNotificationRead } from '../api/notificationApi';
import { formatTime, typeIcon } from '../components/NotificationCenter';
import { useNotifications } from '../hooks/useNotifications';

const emptyPage = { content: [], page: 0, totalElements: 0, totalPages: 0, first: true, last: true };

export function NotificationsPage() {
  const navigate = useNavigate();
  const live = useNotifications();
  const [notificationPage, setNotificationPage] = useState(emptyPage);
  const [page, setPage] = useState(0);
  const [unread, setUnread] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setLoading(true); setError('');
    return listNotifications({ page, size: 20, unread })
      .then(setNotificationPage)
      .catch((exception) => setError(exception.message))
      .finally(() => setLoading(false));
  }, [page, unread]);

  useEffect(() => { load(); }, [load, live.unreadCount]);

  async function openNotification(notification) {
    if (!notification.readAt) await markNotificationRead(notification.id);
    await Promise.all([load(), live.refresh()]);
    if (notification.targetUrl) navigate(notification.targetUrl);
  }

  async function markAll() {
    await markAllNotificationsRead();
    await Promise.all([load(), live.refresh()]);
  }

  return <section className="admin-page notifications-page">
    <header className="admin-heading"><div><p className="section-kicker">Hệ thống / Nhật ký công việc</p><h1>Thông báo</h1><p className="heading-copy">Theo dõi tiếp nhận, bàn giao mẫu và phản hồi từ KCS theo thời gian thực.</p></div>{notificationPage.unreadCount > 0 && <button className="outline-button" type="button" onClick={markAll}>Đánh dấu đã đọc</button>}</header>
    <div className="notification-tabs" role="tablist" aria-label="Lọc thông báo">
      <button className={!unread ? 'active' : ''} type="button" onClick={() => { setUnread(false); setPage(0); }}>Tất cả</button>
      <button className={unread ? 'active' : ''} type="button" onClick={() => { setUnread(true); setPage(0); }}>Chưa đọc <span>{notificationPage.unreadCount ?? live.unreadCount}</span></button>
    </div>
    {error && <p className="form-error" role="alert">{error}</p>}
    <section className="panel-card notification-page-card">
      {loading ? <p className="muted">Đang tải thông báo…</p> : notificationPage.content.length === 0
        ? <div className="notification-page-empty"><span>✓</span><h2>{unread ? 'Không còn thông báo chưa đọc' : 'Chưa có thông báo'}</h2><p>Các hoạt động tiếp nhận và bàn giao sẽ xuất hiện tại đây.</p></div>
        : <div className="notification-page-list">{notificationPage.content.map((notification) => <button className={`notification-page-row ${notification.readAt ? '' : 'unread'}`} type="button" key={notification.id} onClick={() => openNotification(notification)}>
          <span className="notification-type-icon">{typeIcon(notification.type)}</span>
          <span className="notification-page-copy"><strong>{notification.title}</strong><small>{notification.message}</small></span>
          <time>{formatTime(notification.createdAt)}</time>
          {!notification.readAt && <i aria-label="Chưa đọc" />}
        </button>)}</div>}
      {notificationPage.totalPages > 1 && <nav className="notification-pagination" aria-label="Phân trang thông báo"><button className="pagination-button" disabled={notificationPage.first || loading} onClick={() => setPage((value) => value - 1)}>← Trước</button><span>Trang {notificationPage.page + 1}/{notificationPage.totalPages}</span><button className="pagination-button" disabled={notificationPage.last || loading} onClick={() => setPage((value) => value + 1)}>Sau →</button></nav>}
    </section>
  </section>;
}
