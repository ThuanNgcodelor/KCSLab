import { request } from '../../../shared/api/httpClient';

export function listTransfers() { return request('/transfers'); }
export function getTransfer(id) { return request(`/transfers/${id}`); }
export function createTransfer(data) { return request('/transfers', { method: 'POST', body: JSON.stringify(data) }); }
export function updateTransfer(id, data) { return request(`/transfers/${id}`, { method: 'PUT', body: JSON.stringify(data) }); }
export function submitTransfer(id) { return request(`/transfers/${id}/submit`, { method: 'POST' }); }
export function decideTransfer(id, data) { return request(`/transfers/${id}/decision`, { method: 'POST', body: JSON.stringify(data) }); }
