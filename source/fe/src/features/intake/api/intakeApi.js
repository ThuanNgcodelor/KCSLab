import { request } from '../../../shared/api/httpClient';

export function listReceipts() { return request('/receipts'); }
export function createReceipt(data) { return request('/receipts', { method: 'POST', body: JSON.stringify(data) }); }
export function uploadReceiptFile(id, file) {
  const formData = new FormData(); formData.append('file', file);
  return request(`/receipts/${id}/attachments`, { method: 'POST', body: formData });
}
