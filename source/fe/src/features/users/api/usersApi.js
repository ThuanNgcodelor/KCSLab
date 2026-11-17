import { request } from '../../../shared/api/httpClient';
import { cleanParams } from '../../../shared/hooks/useListState';

export function listUsers(params = {}) {
  return request(`/users?${new URLSearchParams(cleanParams({ size: 20, ...params }))}`);
}

export function getUser(id) {
  return request(`/users/${id}`);
}

export function createUser(data) {
  return request('/users', { method: 'POST', body: JSON.stringify(data) });
}

export function updateUser(id, data) {
  return request(`/users/${id}`, { method: 'PATCH', body: JSON.stringify(data) });
}
