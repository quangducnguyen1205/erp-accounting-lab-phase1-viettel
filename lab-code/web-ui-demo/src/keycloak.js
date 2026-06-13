import Keycloak from 'keycloak-js';
import { config } from './config';

export const keycloak = new Keycloak({
  url: config.keycloakUrl,
  realm: config.keycloakRealm,
  clientId: config.keycloakClientId
});

let initPromise;

export function initKeycloak() {
  if (!initPromise) {
    initPromise = keycloak.init({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false
    });
  }

  return initPromise;
}

export function refreshToken(minValiditySeconds = 30) {
  return keycloak.updateToken(minValiditySeconds);
}

export function getSafeUserInfo() {
  const token = keycloak.tokenParsed ?? {};
  const realmRoles = token.realm_access?.roles ?? [];
  const resourceRoles = Object.fromEntries(
    Object.entries(token.resource_access ?? {})
      .map(([clientId, client]) => [clientId, client.roles ?? []])
  );
  const clientRoles = Object.values(resourceRoles).flatMap((roles) => roles);

  return {
    username: token.preferred_username ?? token.name ?? token.sub ?? '(unknown)',
    tenantId: token.tenant_id ?? '(missing)',
    realmRoles,
    clientRoles,
    resourceRoles
  };
}

export function getAuthSnapshot() {
  const authenticated = Boolean(keycloak.authenticated);
  const hasToken = Boolean(keycloak.token);
  const tokenParsed = keycloak.tokenParsed ?? {};
  const userInfo = authenticated ? getSafeUserInfo() : null;
  const tokenExpiresAt = tokenParsed.exp ? new Date(tokenParsed.exp * 1000).toLocaleString() : '(missing)';
  let warning = '';

  if (authenticated && !hasToken) {
    warning = 'Đăng nhập thành công nhưng access token đang thiếu.';
  } else if (authenticated && userInfo?.tenantId === '(missing)') {
    warning = 'Token hợp lệ nhưng thiếu claim tenant_id.';
  }

  return {
    authenticated,
    hasToken,
    userInfo,
    tokenExpiresAt,
    warning
  };
}
