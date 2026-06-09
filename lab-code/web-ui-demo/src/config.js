export const config = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081',
  keycloakUrl: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:18080',
  keycloakRealm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'viettel-lab',
  keycloakClientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'tenant-demo-web',
  requestIdPrefix: import.meta.env.VITE_REQUEST_ID_PREFIX ?? 'web-demo'
};
