import { request } from '../../../shared/api/httpClient';

export function getReceivingDashboard(filters) {
  return request(`/receiving/dashboard?${new URLSearchParams(filters)}`);
}
export function listReceivingTransfers(filters) {
  return request(`/receiving/transfers?${new URLSearchParams(Object.entries(filters).filter(([, value]) => value !== '' && value != null))}`);
}
export function getReceivingTransfer(id) { return request(`/receiving/transfers/${id}`); }
export function getReceivingEvidence(id) { return request(`/receiving/transfers/${id}/evidence`); }
export function decideReceivingTransfer(id, decision) {
  return request(`/receiving/transfers/${id}/decision`, { method: 'POST', body: JSON.stringify(decision) });
}
