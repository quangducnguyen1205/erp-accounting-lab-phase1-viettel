# Security notes index

## Thư mục này chứa gì?

Ghi chú về Spring Security, JWT tạm và Keycloak/OIDC cho tenant-aware backend demo.

## Reading order

1. `oauth2-jwt-resource-server-concepts.md` - thuật ngữ OAuth2/JWT nền.
2. `spring-security-core-concepts.md` - filter chain, SecurityContext, stateless API.
3. `jwt-spring-security-temporary.md` - vì sao có JWT tạm.
4. `jwt-implementation-walkthrough.md` - flow code đã implement trong `tenant-demo`.
5. `spring-security-config-and-properties.md` - config/properties binding.
6. `keycloak-oauth2-oidc-awareness.md` - Keycloak ở mức awareness.
7. `keycloak-oidc-mental-model.md` - mental model Keycloak/OIDC.
8. `keycloak-admin-console-guide.md` - thao tác Admin Console có screenshot.
9. `keycloak-mini-lab-plan.md` - checklist mini-lab.
10. `spring-boot-keycloak-integration-plan.md` - tích hợp Keycloak mode.
11. `../07-architecture/keycloak-in-target-architecture.md` - Keycloak trong target architecture.

## Core

- JWT tạm và Spring Security Resource Server.
- `JwtTenantContextFilter` đọc `tenant_id` từ token đã validate.
- Keycloak mini-lab: issuer/JWKS/access token/claim.
- Không tin `tenant_id` từ request body.

## Optional / later

- Full RBAC role matrix.
- React Authorization Code + PKCE.
- Refresh token/logout/session management.
- Keycloak production deployment, HTTPS, key rotation, realm import/export chuẩn.

## Lab liên quan

- `lab-code/tenant-demo/src/main/java/com/viettel/demo/security/`
- `lab-code/tenant-demo/http/keycloak-api.http`
- `lab-code/keycloak-lab/`
- `presentation-notes/demo-script-keycloak-tenant-flow.md`

