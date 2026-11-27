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
export function getAdminOperationsDashboard(filters = {}) {
  return request(`/admin/dashboard?${dashboardSearch(filters)}`);
}
export function getReceipt(id) { return request(`/receipts/${id}`); }
export async function downloadReceiptForm3(id, sampleId) {
  const response = await fetch(`/api/v1/receipts/${id}/form-3${sampleId ? `?sampleId=${sampleId}` : ''}`, { credentials: 'include' });
  if (!response.ok) throw new Error('Không thể tải Phiếu tiếp nhận và bàn giao mẫu.');
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = `phieu-tiep-nhan-${id}${sampleId ? `-mau-${sampleId}` : ''}.docx`;
  anchor.click();
  URL.revokeObjectURL(url);
}
export function updateReceipt(id, data) { return request(`/receipts/${id}`, { method: 'PUT', body: JSON.stringify(data) }); }
export function getReceiptEdits(id) { return request(`/receipts/${id}/edits`); }
export function createReceipt(data) { return request('/receipts', { method: 'POST', body: JSON.stringify(data) }); }
export function getCustomerNotificationSettings() { return request('/receipts/notification-settings'); }
export function getCustomerNotificationDeliveries(id) { return request(`/receipts/${id}/notification-deliveries?page=0&size=20`); }
export function retryCustomerNotification(id, deliveryId, data) {
  return request(`/receipts/${id}/notification-deliveries/${deliveryId}/retry`, { method: 'POST', body: JSON.stringify(data) });
}
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

export function getSampleCoding(sampleId) { return request(`/samples/${sampleId}/coding`); }
export function getSampleCodingEvents(sampleId) { return request(`/samples/${sampleId}/coding/events`); }
export function submitSampleCoding(sampleId) { return request(`/samples/${sampleId}/coding/submit`, { method: 'POST' }); }
export function verifySampleCoding(sampleId) { return request(`/samples/${sampleId}/coding/verify`, { method: 'POST' }); }
export function rejectSampleCoding(sampleId, reason) { return request(`/samples/${sampleId}/coding/reject`, { method: 'POST', body: JSON.stringify({ reason }) }); }
export function correctSampleCoding(sampleId, data) { return request(`/samples/${sampleId}/coding/correct`, { method: 'POST', body: JSON.stringify(data) }); }
export function replaceManualSampleCode(sampleId, sampleCode, reason) {
  return request(`/samples/${sampleId}/coding/manual-code`, { method: 'POST', body: JSON.stringify({ sampleCode, reason }) });
}
