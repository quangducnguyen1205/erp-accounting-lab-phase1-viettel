import { config } from './config';
import { keycloak, refreshToken } from './keycloak';

function newRequestId() {
  const suffix = Math.random().toString(16).slice(2, 8);
  return `${config.requestIdPrefix}-${Date.now()}-${suffix}`;
}

async function parseResponse(response) {
  const contentType = response.headers.get('content-type') ?? '';
  if (contentType.includes('application/json')) {
    return response.json();
  }
  return response.text();
}

export async function apiRequest(path, options = {}) {
  if (!keycloak.authenticated) {
    throw new Error('Bạn cần login Keycloak trước khi gọi API.');
  }

  if (!keycloak.token) {
    throw new Error('Keycloak đã authenticated nhưng access token chưa sẵn sàng. Hãy refresh hoặc login lại.');
  }

  await refreshToken();

  const requestId = newRequestId();
  const endpoint = `${config.apiBaseUrl}${path}`;
  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
    'X-Request-Id': requestId,
    ...(options.body ? { 'Content-Type': 'application/json' } : {})
  };

  const response = await fetch(endpoint, {
    method: options.method ?? 'GET',
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined
  });
  const data = await parseResponse(response);

  console.info('[web-ui-demo] API request complete', {
    method: options.method ?? 'GET',
    path,
    status: response.status,
    requestId
  });

  return {
    ok: response.ok,
    status: response.status,
    requestId,
    endpoint,
    data
  };
}

export function loadMasterData() {
  return apiRequest('/api/master-data');
}

export function createMasterData(payload) {
  return apiRequest('/api/master-data', {
    method: 'POST',
    body: payload
  });
}
