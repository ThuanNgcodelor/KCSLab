import { request } from '../../../shared/api/httpClient';
import { cleanParams } from '../../../shared/hooks/useListState';

export function listResultReleases(params = {}) {
  return request(`/result-releases?${new URLSearchParams(cleanParams({ size: 20, ...params }))}`);
}
export function listReadyRecords() { return request('/result-releases/ready'); }
export function getResultRelease(id) { return request(`/result-releases/${id}`); }
export function getResultReleaseEvents(id) { return request(`/result-releases/${id}/events`); }
export function createResultRelease(testingRecordId) { return request('/result-releases', { method: 'POST', body: JSON.stringify({ testingRecordId }) }); }
export function uploadResultFiles(id, files) {
  const form = new FormData();
  files.forEach((file) => form.append('files', file));
  return request(`/result-releases/${id}/files`, { method: 'POST', body: form });
}
export function uploadReportFile(id, file) {
  const form = new FormData();
  form.append('file', file);
  return request(`/result-releases/${id}/report-file`, { method: 'POST', body: form });
}
export function removeReportFile(id) { return request(`/result-releases/${id}/report-file`, { method: 'DELETE' }); }
export function removeResultFile(id, fileId) { return request(`/result-releases/${id}/files/${fileId}`, { method: 'DELETE' }); }
export function submitResultRelease(id) { return request(`/result-releases/${id}/submit`, { method: 'POST' }); }
export function returnResultRelease(id, reason) { return request(`/result-releases/${id}/return`, { method: 'POST', body: JSON.stringify({ reason }) }); }
export function publishResultRelease(id, publicFileIds) { return request(`/result-releases/${id}/publish`, { method: 'POST', body: JSON.stringify({ publicFileIds }) }); }
export function revokeResultRelease(id, reason) { return request(`/result-releases/${id}/revoke`, { method: 'POST', body: JSON.stringify({ reason }) }); }
export function reviseResultRelease(id) { return request(`/result-releases/${id}/revisions`, { method: 'POST' }); }
export async function downloadResultFile(releaseId, file) {
  const response = await fetch(`/api/v1/result-releases/${releaseId}/files/${file.id}/content`, { credentials: 'include' });
  if (!response.ok) throw new Error('Không thể tải file kết quả.');
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url; anchor.download = file.originalName;
  anchor.click();
  setTimeout(() => URL.revokeObjectURL(url), 10000);
}
export function saveResultReport(id, data) { return request(`/result-releases/${id}/report`, { method: 'PUT', body: JSON.stringify(data) }); }
export async function openReportPreview(id) {
  const response = await fetch(`/api/v1/result-releases/${id}/report-preview`, { credentials: 'include' });
  if (!response.ok) {
    const detail = await response.json().catch(() => ({}));
    throw new Error(detail.detail ?? 'Không thể tạo bản xem trước.');
  }
  const url = URL.createObjectURL(await response.blob());
  window.open(url, '_blank', 'noopener');
  setTimeout(() => URL.revokeObjectURL(url), 60000);
}
