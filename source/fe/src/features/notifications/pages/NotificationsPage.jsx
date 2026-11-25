import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listNotifications } from '../api/notificationApi';
import { formatTime } from '../components/NotificationCenter';
import { NotificationAvatar } from '../components/NotificationAvatar';
import { useNotifications } from '../hooks/useNotifications';
import { PageLoader } from '../../../shared/components/Loading';
import { ListToolbar } from '../../../shared/components/ListToolbar';
import { useListState } from '../../../shared/hooks/useListState';
import { Icon } from '../../../shared/components/Icon';
import { useAuth } from '../../auth/hooks/useAuth';
import { notificationTypesFor } from '../notificationPolicy';

const allTypeOptions = [['', 'Tất cả loại'], ['TEST_REQUEST_CREATED', 'Yêu cầu kiểm nghiệm mới'], ['INTAKE_RECORDED', 'Tiếp nhận mới'], ['TRANSFER_SUBMITTED', 'Gửi chuyển mẫu'], ['TRANSFER_ACCEPTED', 'KCS đã nhận'], ['TRANSFER_REJECTED', 'KCS từ chối']];
const sortOptions = [['createdAt,desc', 'Mới nhất'], ['createdAt,asc', 'Cũ nhất'], ['type,asc', 'Loại: A → Z'], ['title,asc', 'Tiêu đề: A → Z'], ['readAt,desc', 'Đã đọc gần nhất']];
const defaults = { q: '', from: '', to: '', type: '', sort: 'createdAt,desc' };
const emptyPage = { content: [], page: 0, totalElements: 0, totalPages: 0, first: true, last: true };

export function NotificationsPage() {
  const { user } = useAuth();
  const typeOptions = allTypeOptions.filter(([type]) => !type || notificationTypesFor(user?.role).includes(type));
  const navigate = useNavigate();
  const live = useNotifications();
  const [notificationPage, setNotificationPage] = useState(emptyPage);
  const [list, setList] = useListState(defaults);
  const page = list.page;
  const setPage = (value) => setList({ page: typeof value === 'function' ? value(page) : value });
  const [unread, setUnread] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reload, setReload] = useState(0);
  const [marking, setMarking] = useState(false);

  useEffect(() => {
    if (list.type && !notificationTypesFor(user?.role).includes(list.type)) setList({ type: '', page: 0 });
  }, [user?.role, list.type, setList]);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    listNotifications({ page, size: 20, unread, q: list.q, from: list.from, to: list.to, type: list.type, sort: list.sort, silent: true })
      .then((result) => {
        if (!active) return;
        if (page > 0 && result.content.length === 0 && result.totalPages <= page) {
          setList({ page: Math.max(0, result.totalPages - 1) });
          return;
        }
        setNotificationPage(result);
      })
      .catch((exception) => { if (active) setError(exception.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [page, unread, list.q, list.from, list.to, list.type, list.sort, live.dataVersion, reload, setList]);

  async function openNotification(notification) {
    setError('');
    try {
      await live.markRead(notification);
      if (notification.targetUrl) navigate(notification.targetUrl);
    } catch (exception) {
      setError(exception.message);
    }
  }

  async function markAll() {
    setError('');
    setMarking(true);
    try { await live.markAllRead(); }
    catch (exception) { setError(exception.message); }
    finally { setMarking(false); }
  }

  return <section className="admin-page notifications-page">
    <header className="admin-heading"><div><p className="section-kicker">Hệ thống / Nhật ký công việc</p><h1>Thông báo</h1><p className="heading-copy">Theo dõi tiếp nhận, bàn giao mẫu và phản hồi từ KCS theo thời gian thực.</p></div>{live.unreadCount > 0 && <button className="outline-button" type="button" disabled={marking} onClick={markAll}>{marking ? 'Đang đánh dấu…' : 'Đánh dấu tất cả đã đọc'}</button>}</header>
    <div className="notification-tabs" role="group" aria-label="Lọc thông báo">
      <button className={!unread ? 'active' : ''} aria-pressed={!unread} type="button" onClick={() => { setUnread(false); setPage(0); }}>Tất cả</button>
      <button className={unread ? 'active' : ''} aria-pressed={unread} type="button" onClick={() => { setUnread(true); setPage(0); }}>Chưa đọc <span>{notificationPage.unreadCount ?? live.unreadCount}</span></button>
    </div>
    <ListToolbar q={list.q} onQ={(q) => setList({ q })} searchLabel="Tìm thông báo" placeholder="Tiêu đề hoặc nội dung…" from={list.from} to={list.to} onFrom={(from) => setList({ from })} onTo={(to) => setList({ to })}
      filters={[{ label: 'Loại', value: list.type, onChange: (type) => setList({ type }), options: typeOptions }]}
      sort={list.sort} sortOptions={sortOptions} onSort={(sort) => setList({ sort })} onReset={() => setList({ ...defaults })} />
    {error && <div className="notification-feedback"><p className="form-error" role="alert">{error}</p><button type="button" className="outline-button" onClick={() => setReload((current) => current + 1)}>Tải lại thông báo</button></div>}
    <section className="panel-card notification-page-card">
      {loading ? <PageLoader label="Đang tải thông báo…" rows={3} /> : notificationPage.content.length === 0
        ? !error && <div className="notification-page-empty"><span><Icon name="checkCircle" size={24} /></span><h2>{unread ? 'Không còn thông báo chưa đọc' : 'Chưa có thông báo'}</h2><p>Các hoạt động tiếp nhận và bàn giao sẽ xuất hiện tại đây.</p></div>
        : <div className="notification-page-list">{notificationPage.content.map((notification) => <button className={`notification-page-row ${notification.readAt ? '' : 'unread'}`} type="button" key={notification.id} onClick={() => openNotification(notification)}>
          <NotificationAvatar notification={notification} />
          <span className="notification-page-copy"><strong>{notification.title}</strong><small>{notification.message}</small></span>
          <time>{formatTime(notification.createdAt)}</time>
          {!notification.readAt && <i aria-label="Chưa đọc" />}
        </button>)}</div>}
      {notificationPage.totalPages > 1 && <nav className="notification-pagination" aria-label="Phân trang thông báo"><button className="pagination-button" disabled={notificationPage.first || loading} onClick={() => setPage((value) => value - 1)}>← Trước</button><span>Trang {notificationPage.page + 1}/{notificationPage.totalPages}</span><button className="pagination-button" disabled={notificationPage.last || loading} onClick={() => setPage((value) => value + 1)}>Sau →</button></nav>}
    </section>
  </section>;
}
