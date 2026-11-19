import { request } from '../../../shared/api/httpClient';

export function listTestRequests() { return request('/test-requests'); }
export function getTestRequest(id) { return request(`/test-requests/${id}`); }
export function getTestRequestByNumber(number) {
  return request(`/test-requests/by-number/${encodeURIComponent(number.trim())}`);
}
export function createTestRequest(data) {
  return request('/test-requests', { method: 'POST', body: JSON.stringify(data) });
}
export function updateTestRequest(id, data) {
  return request(`/test-requests/${id}`, { method: 'PUT', body: JSON.stringify(data) });
}
export function saveSamplingPlan(id, data) {
  return request(`/test-requests/${id}/sampling-plan`, { method: 'PUT', body: JSON.stringify(data) });
}
export function saveFieldSampling(id, data) {
  return request(`/test-requests/${id}/field-sampling`, { method: 'PUT', body: JSON.stringify(data) });
}
