# Keycloak/OIDC awareness trong kiến trúc Phase 1

## Vai trò của file này

File này giữ vai trò **awareness kiến trúc**: Keycloak nằm ở đâu trong hệ thống và vì sao Phase 1 cần học nó. Phần mental model chi tiết trước khi code backend đã được tách sang:

- `docs/05-security/keycloak-oidc-mental-model.md`

Phần thao tác Admin Console có screenshot:

- `docs/05-security/keycloak-admin-console-guide.md`

Kế hoạch chạy mini-lab:

- `docs/05-security/keycloak-mini-lab-plan.md`

## Keycloak nằm ở đâu trong kiến trúc?

Trong kiến trúc target, Keycloak thuộc lớp authentication/authorization. Ở mức Phase 1, mình chỉ cần hiểu:

- Keycloak là Authorization Server / OpenID Provider.
- Frontend/tool lấy access token từ Keycloak.
- Backend Spring Boot đóng vai Resource Server.
- Backend validate token rồi mới đọc claims như `tenant_id`.
- Tenant isolation vẫn phải enforce ở service/repository, không chỉ dựa vào token.

## Vì sao không dừng ở JWT tạm?

JWT tạm trong `tenant-demo` là bridge học tập:

- giúp bỏ dần `X-Tenant-Id` trực tiếp;
- giúp hiểu Bearer token, Spring Security Resource Server và `TenantContext`;
- dễ test bằng MockMvc/curl.

Nhưng JWT tạm không có user management thật, không có issuer/JWKS production, không có login/OIDC flow. Vì mentor khuyến khích học công nghệ thật khi chạm feature auth, mini-lab Keycloak/OIDC được thêm để hiểu flow gần thực tế hơn.

## Giới hạn awareness hiện tại

Chưa làm:

- full RBAC/role matrix;
- React login flow;
- refresh token/logout/session management;
- Keycloak production deployment;
- HTTPS/key rotation/cluster/external database;
- policy-based authorization phức tạp.

## Đọc theo thứ tự

1. `docs/05-security/oauth2-jwt-resource-server-concepts.md`
2. `docs/05-security/jwt-implementation-walkthrough.md`
3. `docs/05-security/keycloak-oidc-mental-model.md`
4. `docs/05-security/keycloak-admin-console-guide.md`
5. `docs/05-security/keycloak-mini-lab-plan.md`

## Nguồn tham khảo chuẩn

- [Keycloak - Server Administration Guide](https://www.keycloak.org/docs/latest/server_admin/)
- [Keycloak - Securing applications and services](https://www.keycloak.org/securing-apps/overview)
- [Spring Security - OAuth2 Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0-18.html)
