import { request } from '../../../shared/api/httpClient';

export function listNotifications({ page = 0, size = 20, sort = 'createdAt,desc', unread = false } = {}) {
  const search = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort,
    unread: String(unread),
  });
  return request(`/notifications?${search}`);
}

export function markNotificationRead(id) {
  return request(`/notifications/${id}`, { method: 'PATCH' });
}

export function markAllNotificationsRead() {
  return request('/notifications/read-all', { method: 'POST' });
}
