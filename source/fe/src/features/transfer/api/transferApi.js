import { request } from '../../../shared/api/httpClient';
import { cleanParams } from '../../../shared/hooks/useListState';

export function listTransfers(params = {}) {
  return request(`/transfers?${new URLSearchParams(cleanParams({ size: 20, ...params }))}`);
}
export function getTransfer(id) { return request(`/transfers/${id}`); }
export function createTransfer(data) { return request('/transfers', { method: 'POST', body: JSON.stringify(data) }); }
export function updateTransfer(id, data) { return request(`/transfers/${id}`, { method: 'PUT', body: JSON.stringify(data) }); }
export function submitTransfer(id) { return request(`/transfers/${id}/submit`, { method: 'POST' }); }
export function decideTransfer(id, data) { return request(`/transfers/${id}/decision`, { method: 'POST', body: JSON.stringify(data) }); }
export function uploadTransferEvidence(receiptId, file) {
  const formData = new FormData();
  formData.append('file', file);
  const search = new URLSearchParams({
    category: 'TRANSFER_EVIDENCE',
    description: 'Bằng chứng bàn giao mẫu',
  });
  if (file.lastModified) search.set('capturedAt', new Date(file.lastModified).toISOString());
  return request(`/receipts/${receiptId}/attachments?${search}`, { method: 'POST', body: formData });
}
