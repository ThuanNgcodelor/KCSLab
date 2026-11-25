import { request } from '../../../shared/api/httpClient';

const base = '/admin';
const apiBase = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';
const query = (values) => new URLSearchParams(Object.entries(values).filter(([, v]) => v !== '' && v != null)).toString();

export const listDossiers = (filters, signal) => request(`${base}/dossiers?${query(filters)}`, { signal, silent: true });
export const getSummary = (scope, signal) => request(`${base}/dossiers/summary?${query(scope)}`, { signal, silent: true });
export const getPeriods = (signal) => request(`${base}/dossiers/periods`, { signal, silent: true });
export const getDossier = (id, page, signal) => request(`${base}/dossiers/${id}?page=${page}`, { signal, silent: true });

export const getStorage = (signal) => request(`${base}/storage-locations`, { signal, silent: true });
export const registerStorage = (data) => request(`${base}/storage-locations`, { method: 'POST', body: JSON.stringify(data) });
export const validateStorage = (id) => request(`${base}/storage-locations/${id}/validate`, { method: 'POST' });
export const activateStorage = (locationId, version) => request(`${base}/storage-write-target`, { method: 'PUT', body: JSON.stringify({ locationId, version }) });

export const listExports = (page, signal) => request(`${base}/dossier-exports?page=${page}`, { signal, silent: true });
export const getExport = (id, signal) => request(`${base}/dossier-exports/${id}`, { signal, silent: true });
export const createExport = (data) => request(`${base}/dossier-exports`, { method: 'POST', body: JSON.stringify(data) });
export const cancelExport = (id) => request(`${base}/dossier-exports/${id}/cancel`, { method: 'POST' });

export const fileUrl = (receiptId, file) => `${apiBase}${base}/dossiers/${receiptId}/files/${file.source}/${file.id}`;
export const previewUrl = (receiptId, file) => `${fileUrl(receiptId, file)}?preview=true`;
export const partUrl = (id, part) => `${apiBase}${base}/dossier-exports/${id}/parts/${part}`;

function save(href, name) {
  const anchor = document.createElement('a');
  anchor.href = href; anchor.download = name; document.body.append(anchor); anchor.click(); anchor.remove();
}

/** Export parts are gigabytes: refresh the login first, then let the browser stream straight to disk. */
export async function downloadPart(url) {
  await request('/auth/me', { silent: true });
  save(url, '');
}

/** Single documents are small (upload limit), so read them here and show a clear error when the disk is offline. */
export async function downloadFile(url, name) {
  await request('/auth/me', { silent: true });
  const response = await fetch(url, { credentials: 'include' });
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}));
    throw new Error(problem.detail ?? 'Không tải được file');
  }
  const href = URL.createObjectURL(await response.blob());
  save(href, name);
  setTimeout(() => URL.revokeObjectURL(href), 10000);
}

// crypto.randomUUID needs HTTPS; the LAN address is plain HTTP.
export function operationId() {
  return Array.from(crypto.getRandomValues(new Uint8Array(16)), (n) => n.toString(16).padStart(2, '0')).join('');
}
