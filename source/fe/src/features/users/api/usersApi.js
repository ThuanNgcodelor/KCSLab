import { request } from '../../../shared/api/httpClient';

export function listUsers() {
  return request('/users');
}

export function createUser(data) {
  return request('/users', { method: 'POST', body: JSON.stringify(data) });
}

export function updateUser(id, data) {
  return request(`/users/${id}`, { method: 'PATCH', body: JSON.stringify(data) });
}
