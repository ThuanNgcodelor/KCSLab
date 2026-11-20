import { request } from '../../../shared/api/httpClient';
import { cleanParams } from '../../../shared/hooks/useListState';

export function listTestMethods(params = {}) {
  return request(`/test-methods?${new URLSearchParams(cleanParams({ size: 20, ...params }))}`);
}

export function createTestMethod(data) {
  return request('/test-methods', { method: 'POST', body: JSON.stringify(data) });
}

export function updateTestMethod(id, data) {
  return request(`/test-methods/${id}`, { method: 'PUT', body: JSON.stringify(data) });
}

export function deleteTestMethod(id) {
  return request(`/test-methods/${id}`, { method: 'DELETE' });
}
