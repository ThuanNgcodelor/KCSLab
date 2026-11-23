import { request } from '../../../shared/api/httpClient';

const base = '/admin/customer-notifications';
export function getCustomerNotificationOverview() { return request(base); }
export function listCustomerDeliveries({ status, q, page = 0 }) {
  const search = new URLSearchParams({ page: String(page), size: '20' });
  if (status) search.set('status', status);
  if (q.trim()) search.set('q', q.trim());
  return request(`${base}/deliveries?${search}`);
}
export function pauseCustomerNotifications(data) {
  return request(`${base}/pause`, { method: 'POST', body: JSON.stringify(data) });
}
