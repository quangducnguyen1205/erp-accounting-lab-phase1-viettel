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
11. `keycloak-authorization-rbac-tenant-scope.md` - AuthN vs AuthZ, role/scope/tenant-scope.
12. `keycloak-authorization-code-guide-spring-boot.md` - code guide cho RBAC mini-lab.
13. `keycloak-authorization-admin-console-guide.md` - thao tác Admin Console để tạo/gán role.
14. `keycloak-authorization-mini-lab-plan.md` - checklist thực hành RBAC.
15. `../07-architecture/keycloak-in-target-architecture.md` - Keycloak trong target architecture.

## Source of truth để tránh trùng lặp

| File | Vai trò chính |
|---|---|
| `keycloak-oidc-mental-model.md` | Theory/mental model: realm, client, user, issuer, JWKS, token claim. |
| `keycloak-admin-console-guide.md` | UI walkthrough có screenshot, không cố giải thích lại toàn bộ OIDC. |
| `keycloak-mini-lab-plan.md` | Checklist chạy lab và done criteria. |
| `spring-boot-keycloak-integration-plan.md` | Code/config integration path cho Spring Boot. |
| `keycloak-authorization-rbac-tenant-scope.md` | Theory cho authorization/RBAC/tenant-scope sau AuthN. |
| `keycloak-authorization-code-guide-spring-boot.md` | Code guide/skeleton cho Spring Security authorities và authorization checks. |
| `keycloak-authorization-admin-console-guide.md` | UI checklist cho roles, role mapping và token claim verification. |
| `keycloak-authorization-mini-lab-plan.md` | Mini-lab cases/done criteria, tránh biến RBAC thành full IAM project. |
| `../07-architecture/keycloak-in-target-architecture.md` | Keycloak trong target architecture rộng hơn. |

Nếu cần chỉnh docs, ưu tiên cập nhật đúng file theo vai trò trên rồi link sang file khác, tránh copy lại cùng một flow dài ở nhiều nơi.

## Core

- JWT tạm và Spring Security Resource Server.
- `JwtTenantContextFilter` đọc `tenant_id` từ token đã validate.
- Keycloak mini-lab: issuer/JWKS/access token/claim.
- Keycloak Authorization/RBAC mini-lab tiếp theo: role/scope/authority, 401 vs 403, tenant-scope.
- Không tin `tenant_id` từ request body.

## Optional / later

- Full RBAC role matrix / Keycloak Authorization Services / UMA.
- React Authorization Code + PKCE.
- Refresh token/logout/session management.
- Keycloak production deployment, HTTPS, key rotation, realm import/export chuẩn.

## Lab liên quan

- `lab-code/tenant-demo/src/main/java/com/viettel/demo/security/`
- `lab-code/tenant-demo/http/keycloak-api.http`
- `lab-code/keycloak-lab/`
- `presentation-notes/demo-script-keycloak-tenant-flow.md`
