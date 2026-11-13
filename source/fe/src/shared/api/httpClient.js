const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';

function readCookie(name) {
  return document.cookie
    .split('; ')
    .find((cookie) => cookie.startsWith(`${name}=`))
    ?.split('=')[1];
}

export async function initializeCsrf() {
  await fetch(`${API_BASE_URL}/auth/csrf`, { credentials: 'include' });
}

export async function request(path, options = {}) {
  const headers = new Headers(options.headers);
  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  const csrf = readCookie('XSRF-TOKEN');
  if (csrf && !['GET', 'HEAD', 'OPTIONS'].includes((options.method ?? 'GET').toUpperCase())) {
    headers.set('X-XSRF-TOKEN', decodeURIComponent(csrf));
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    credentials: 'include',
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    const exception = new Error(error.detail ?? error.message ?? 'Có lỗi xảy ra');
    exception.status = response.status;
    exception.fieldErrors = error.fieldErrors;
    throw exception;
  }
  if (response.status === 204) return null;
  return response.json();
}
