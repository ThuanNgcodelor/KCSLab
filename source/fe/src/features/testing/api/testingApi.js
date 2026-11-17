import { request } from '../../../shared/api/httpClient';
import { cleanParams } from '../../../shared/hooks/useListState';

export function listTestingRecords(params = {}) {
  return request(`/testing-records?${new URLSearchParams(cleanParams({ size: 20, ...params }))}`);
}
export function listTestingCandidates() { return request('/testing-records/candidates'); }
export function getTestingRecord(id) { return request(`/testing-records/${id}`); }
export function createTestingRecord(data) { return request('/testing-records', { method: 'POST', body: JSON.stringify(data) }); }
export function updateTestingRecord(id, data) { return request(`/testing-records/${id}`, { method: 'PUT', body: JSON.stringify(data) }); }
export function submitTestingRecord(id) { return request(`/testing-records/${id}/submit`, { method: 'POST' }); }
export function reviewTestingRecord(id, data) { return request(`/testing-records/${id}/review`, { method: 'POST', body: JSON.stringify(data) }); }
