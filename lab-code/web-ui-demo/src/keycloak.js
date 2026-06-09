import Keycloak from 'keycloak-js';
import { config } from './config';

export const keycloak = new Keycloak({
  url: config.keycloakUrl,
  realm: config.keycloakRealm,
  clientId: config.keycloakClientId
});

export function initKeycloak() {
  return keycloak.init({
    onLoad: 'check-sso',
    pkceMethod: 'S256',
    checkLoginIframe: false
  });
}

export function refreshToken(minValiditySeconds = 30) {
  return keycloak.updateToken(minValiditySeconds);
}

export function getSafeUserInfo() {
  const token = keycloak.tokenParsed ?? {};
  const realmRoles = token.realm_access?.roles ?? [];
  const clientRoles = Object.values(token.resource_access ?? {})
    .flatMap((client) => client.roles ?? []);

  return {
    username: token.preferred_username ?? token.name ?? token.sub ?? '(unknown)',
    tenantId: token.tenant_id ?? '(missing)',
    realmRoles,
    clientRoles
  };
}
