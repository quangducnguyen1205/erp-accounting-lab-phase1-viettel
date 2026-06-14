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

async function createRequestContext(path, apiBaseUrl) {
  if (!keycloak.authenticated) {
    throw new Error('Bạn cần login Keycloak trước khi gọi API.');
  }

  if (!keycloak.token) {
    throw new Error('Keycloak đã authenticated nhưng access token chưa sẵn sàng. Hãy refresh hoặc login lại.');
  }

  await refreshToken();

  const requestId = newRequestId();
  const endpoint = `${apiBaseUrl ?? config.apiBaseUrl}${path}`;
  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
    'X-Request-Id': requestId
  };

  return { requestId, endpoint, headers };
}

function filenameFromDisposition(disposition) {
  if (!disposition) {
    return '';
  }

  const match = disposition.match(/filename="?([^"]+)"?/i);
  return match?.[1] ?? '';
}

export async function apiRequest(path, options = {}) {
  const { requestId, endpoint, headers } = await createRequestContext(path, options.apiBaseUrl);
  const hasJsonBody = options.body !== undefined;
  const formData = options.formData;
  const requestHeaders = {
    ...headers,
    ...(hasJsonBody ? { 'Content-Type': 'application/json' } : {})
  };

  const response = await fetch(endpoint, {
    method: options.method ?? 'GET',
    headers: requestHeaders,
    body: formData ?? (hasJsonBody ? JSON.stringify(options.body) : undefined)
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

export function loadMasterData(apiBaseUrl) {
  return apiRequest('/api/master-data', { apiBaseUrl });
}

export function loadMasterDataByCode(code, apiBaseUrl) {
  return apiRequest(`/api/master-data/code/${encodeURIComponent(code)}`, { apiBaseUrl });
}

export function createMasterData(payload, apiBaseUrl) {
  return apiRequest('/api/master-data', {
    apiBaseUrl,
    method: 'POST',
    body: payload
  });
}

export function updateMasterData(id, payload, apiBaseUrl) {
  return apiRequest(`/api/master-data/${encodeURIComponent(id)}`, {
    apiBaseUrl,
    method: 'PUT',
    body: payload
  });
}

export function deleteMasterData(id, apiBaseUrl) {
  return apiRequest(`/api/master-data/${encodeURIComponent(id)}`, {
    apiBaseUrl,
    method: 'DELETE'
  });
}

export function loadAuditEvents(apiBaseUrl, limit = 20) {
  return apiRequest(`/api/audit-events?limit=${encodeURIComponent(limit)}`, { apiBaseUrl });
}

export function searchMasterData(keyword, apiBaseUrl) {
  return apiRequest(`/api/search/master-data?keyword=${encodeURIComponent(keyword)}`, { apiBaseUrl });
}

export function listFiles(apiBaseUrl) {
  return apiRequest('/api/files', { apiBaseUrl });
}

export function uploadFile(file, apiBaseUrl) {
  const formData = new FormData();
  formData.append('file', file);
  return apiRequest('/api/files', {
    apiBaseUrl,
    method: 'POST',
    formData
  });
}

export function deleteFile(fileId, apiBaseUrl) {
  return apiRequest(`/api/files/${encodeURIComponent(fileId)}`, {
    apiBaseUrl,
    method: 'DELETE'
  });
}

export async function downloadFile(fileId, apiBaseUrl) {
  const path = `/api/files/${encodeURIComponent(fileId)}`;
  const { requestId, endpoint, headers } = await createRequestContext(path, apiBaseUrl);
  const response = await fetch(endpoint, { headers });
  const data = response.ok
    ? {
        blob: await response.blob(),
        filename: filenameFromDisposition(response.headers.get('content-disposition')) || fileId
      }
    : await parseResponse(response);

  console.info('[web-ui-demo] API request complete', {
    method: 'GET',
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
