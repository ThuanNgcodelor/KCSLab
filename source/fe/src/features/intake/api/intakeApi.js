import { request } from '../../../shared/api/httpClient';

export function listReceipts({ page = 0, size = 20, sort = 'receivedAt,desc', ...filters } = {}) {
  const search = new URLSearchParams({ page: String(page), size: String(size), sort });
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') search.set(key, String(value).trim());
  });
  return request(`/receipts?${search}`);
}
export function getReceptionistDashboard(filters = {}) {
  return request(`/intake/dashboard/receptionist?${dashboardSearch(filters)}`);
}
export function getAdminDashboard(filters = {}) {
  return request(`/intake/dashboard/admin?${dashboardSearch(filters)}`);
}
export function getReceipt(id) { return request(`/receipts/${id}`); }
export function createReceipt(data) { return request('/receipts', { method: 'POST', body: JSON.stringify(data) }); }
export function uploadReceiptFile(id, file, metadata = {}) {
  const formData = new FormData(); formData.append('file', file);
  const search = new URLSearchParams({ category: metadata.category ?? 'OTHER' });
  if (metadata.description) search.set('description', metadata.description);
  if (metadata.capturedAt) search.set('capturedAt', metadata.capturedAt);
  return request(`/receipts/${id}/attachments?${search}`, { method: 'POST', body: formData });
}

function dashboardSearch(filters) {
  const search = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') search.set(key, String(value).trim());
  });
  return search;
}
