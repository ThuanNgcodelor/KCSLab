import { request } from '../../../shared/api/httpClient';

export function listTestingRecords() { return request('/testing-records'); }
export function getTestingRecord(id) { return request(`/testing-records/${id}`); }
export function createTestingRecord(data) { return request('/testing-records', { method: 'POST', body: JSON.stringify(data) }); }
export function updateTestingRecord(id, data) { return request(`/testing-records/${id}`, { method: 'PUT', body: JSON.stringify(data) }); }
export function submitTestingRecord(id) { return request(`/testing-records/${id}/submit`, { method: 'POST' }); }
export function reviewTestingRecord(id, data) { return request(`/testing-records/${id}/review`, { method: 'POST', body: JSON.stringify(data) }); }
