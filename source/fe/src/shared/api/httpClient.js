import { trackRequest } from '../loading/loadingStore';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';
let refreshPromise = null;

function readCookie(name) {
  return document.cookie
    .split('; ')
    .find((cookie) => cookie.startsWith(`${name}=`))
    ?.split('=')[1];
}

export async function initializeCsrf() {
  await fetchFromApi('/auth/csrf', { credentials: 'include' });
}

async function fetchFromApi(path, options) {
  try {
    return await fetch(`${API_BASE_URL}${path}`, options);
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new Error(navigator.onLine
      ? 'Không kết nối được máy chủ. Vui lòng thử lại.'
      : 'Thiết bị đang mất kết nối mạng. Vui lòng thử lại khi có mạng.', { cause: error });
  }
}

function requestOptions(options = {}) {
  const headers = new Headers(options.headers);
  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  const csrf = readCookie('XSRF-TOKEN');
  if (csrf && !['GET', 'HEAD', 'OPTIONS'].includes((options.method ?? 'GET').toUpperCase())) {
    headers.set('X-XSRF-TOKEN', decodeURIComponent(csrf));
  }

  return { ...options, headers, credentials: 'include' };
}

async function parseResponse(response) {
  if (response.ok) {
    if (response.status === 204) return null;
    return response.json();
  }

  const error = await response.json().catch(() => ({}));
  const exception = new Error(error.detail ?? error.message ?? 'Có lỗi xảy ra');
  exception.status = response.status;
  exception.fieldErrors = error.fieldErrors;
  throw exception;
}

async function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = fetchFromApi('/auth/refresh', requestOptions({ method: 'POST' }))
      .then(parseResponse)
      .then((user) => {
        window.dispatchEvent(new CustomEvent('auth:refreshed', { detail: user }));
        return user;
      })
      .catch((error) => {
        if (error.status === 401 || error.status === 403) window.dispatchEvent(new Event('auth:expired'));
        throw error;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

export async function request(path, options = {}) {
  const done = options.silent ? () => {} : trackRequest();
  try { return await send(path, options); } finally { done(); }
}

async function send(path, options) {
  const { silent, ...fetchOptions } = options; // eslint-disable-line no-unused-vars
  options = fetchOptions;
  let response = await fetchFromApi(path, requestOptions(options));
  const canRefresh = response.status === 401 && !['/auth/login', '/auth/refresh'].includes(path);

  if (canRefresh) {
    await refreshAccessToken();
    response = await fetchFromApi(path, requestOptions(options));
  }

  return parseResponse(response);
}
