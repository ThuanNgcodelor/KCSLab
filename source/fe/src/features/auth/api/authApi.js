import { initializeCsrf, request } from '../../../shared/api/httpClient';

export async function login(credentials) {
  await initializeCsrf();
  return request('/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  });
}

export function getCurrentUser() {
  return request('/auth/me');
}

export function logout() {
  return request('/auth/logout', { method: 'POST' });
}

export function refreshSession() {
  return request('/auth/refresh', { method: 'POST' });
}
