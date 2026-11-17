import { request } from '../../../shared/api/httpClient';

export function listResultReleases({ page = 0, size = 20, status } = {}) {
  const search = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) search.set('status', status);
  return request(`/result-releases?${search}`);
}
export function getResultRelease(id) { return request(`/result-releases/${id}`); }
export function getResultReleaseEvents(id) { return request(`/result-releases/${id}/events`); }
export function createResultRelease(testingRecordId) { return request('/result-releases', { method: 'POST', body: JSON.stringify({ testingRecordId }) }); }
export function uploadResultFiles(id, files) {
  const form = new FormData();
  files.forEach((file) => form.append('files', file));
  return request(`/result-releases/${id}/files`, { method: 'POST', body: form });
}
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
