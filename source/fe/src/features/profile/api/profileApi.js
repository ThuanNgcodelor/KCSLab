import { request } from '../../../shared/api/httpClient';

export function getProfile() {
  return request('/users/me');
}

export function updateProfile(data) {
  return request('/users/me', { method: 'PATCH', body: JSON.stringify(data) });
}

export function uploadAvatar(file) {
  const body = new FormData();
  body.append('file', file, 'avatar.jpg');
  return request('/users/me/avatar', { method: 'PUT', body });
}

export function deleteAvatar() {
  return request('/users/me/avatar', { method: 'DELETE' });
}

export function changePassword(data) {
  return request('/users/me/password', { method: 'PUT', body: JSON.stringify(data) });
}
