# Keycloak, OAuth2 và OIDC trong tenant-demo

## Mục tiêu

Tài liệu này nối từ JWT tạm hiện tại sang Keycloak/OIDC thật ở mức Phase 1. Mục tiêu không phải xây IAM production, mà là hiểu vai trò của Keycloak trong kiến trúc và chuẩn bị mini-lab lấy token thật từ Keycloak.

Flow mong muốn:

```text
Client / HTTP Client
-> lấy access token từ Keycloak
-> gọi Spring Boot API bằng Authorization: Bearer <token>
-> Spring Security Resource Server validate token bằng issuer/JWKS
-> JwtTenantContextFilter đọc tenant_id claim đã validate
-> Service/Repository vẫn query tenant-aware
```

## Keycloak là gì?

Trong mini-lab này, Keycloak đóng vai trò:

- **Authorization Server** trong OAuth2: phát hành access token cho client.
- **OpenID Provider** trong OIDC: xác thực user và cung cấp identity claims.
- Nơi quản lý realm, client, user, role và key ký token.

Spring Boot `tenant-demo` không nên tự phát hành token production. Backend nên là **Resource Server**: nhận Bearer token, validate token, rồi phục vụ protected API.

## OAuth2 và OIDC khác nhau thế nào?

| Khái niệm | Trọng tâm | Ví dụ trong lab |
|---|---|---|
| OAuth2 | Authorization: client được gọi resource nào | HTTP Client lấy access token để gọi `/api/master-data` |
| OIDC | Authentication/identity layer trên OAuth2 | Token có `sub`, issuer, user claims |
| JWT | Format token có claim và chữ ký | Access token từ Keycloak thường là JWT |
| Bearer token | Cách gửi token qua HTTP | `Authorization: Bearer <access-token>` |

Điểm cần nhớ: Bearer token không bắt buộc phải là JWT, nhưng trong mini-lab Keycloak này mình kỳ vọng access token là JWT để Spring Security Resource Server validate được bằng issuer/JWKS.

## Các thuật ngữ Keycloak cần biết

| Thuật ngữ | Nghĩa thực dụng |
|---|---|
| Realm | Không gian quản lý độc lập cho user/client/roles. Lab dùng realm `viettel-lab`. |
| Client | Ứng dụng xin token hoặc dùng OIDC. Lab có thể tạo public client để HTTP Client lấy token. |
| User | Người dùng đăng nhập. Lab tạo `tenant1-user` và `tenant2-user`. |
| Role | Quyền ở mức realm/client. Phase này chỉ awareness, chưa làm role matrix. |
| Issuer | URL định danh bên phát hành token, ví dụ `http://localhost:18080/realms/viettel-lab`. |
| JWKS | Endpoint public keys để Resource Server verify chữ ký token. |
| Access token | Token client gửi tới backend API. |
| Claims | Dữ liệu trong token, ví dụ `sub`, `iss`, `exp`, `tenant_id`, `roles`. |

## JWT tạm hiện tại khác Keycloak thế nào?

| Chủ đề | JWT tạm hiện tại | Keycloak/OIDC mini-lab |
|---|---|---|
| Ai phát hành token? | `tenant-demo` dev endpoint | Keycloak |
| Ký token | HS256 shared secret local | Keycloak quản lý key, Resource Server đọc JWKS |
| Config backend | `app.jwt.secret`, `app.jwt.issuer` | `issuer-uri` hoặc `jwk-set-uri` |
| User management | Không có thật | Có user trong Keycloak realm |
| Dev token endpoint | Có, local-only | Không cần khi lấy token từ Keycloak |
| Mức production | Bridge học tập | Gần kiến trúc thật hơn, vẫn chỉ mini-lab |

JWT tạm đã giúp mình học backend-side flow. Keycloak mini-lab giúp hiểu phần Authorization Server/OpenID Provider thật.

## Spring Boot Resource Server sẽ evolve thế nào?

Hiện tại `tenant-demo` có custom `JwtDecoder` dùng HS256 secret local:

```text
JWT_SECRET
-> NimbusJwtDecoder.withSecretKey(...)
-> validate issuer tenant-demo-local
```

Khi chuyển sang Keycloak, hướng Spring Security chuẩn là để Resource Server validate JWT bằng issuer/JWKS:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:18080/realms/viettel-lab
```

Spring Security có thể dùng issuer metadata để tìm JWKS, verify chữ ký, kiểm tra issuer và timestamp. Nếu muốn không phụ thuộc discovery lúc startup/request, có thể cấu hình thêm `jwk-set-uri`, nhưng vẫn nên giữ `issuer-uri` để validate claim `iss`.

Trong repo này, bước code tiếp theo nên được profile-gated hoặc config-gated để không phá JWT tạm đang pass test.

## Tenant context rule vẫn giữ nguyên

Keycloak không thay thế tenant-aware query.

Rule vẫn là:

1. Token phải được validate trước.
2. Backend chỉ đọc `tenant_id` từ claim đã validate.
3. `JwtTenantContextFilter` set `TenantContext`.
4. Service/repository query bằng `tenantId`.
5. Không tin `tenant_id` từ request body.

Để mini-lab hoạt động với tenant context, Keycloak cần phát claim `tenant_id`. Có thể làm bằng user attribute + protocol mapper ở Keycloak:

```text
tenant1-user attribute tenant_id = 1
tenant2-user attribute tenant_id = 2
Protocol mapper đưa user attribute tenant_id vào access token
```

## Vai trò của roles/RBAC

Role trả lời “user được làm gì?”. Tenant claim trả lời “request thuộc tenant nào?”. Hai thứ liên quan nhưng không thay thế nhau.

Phase này không làm role matrix phức tạp. Chỉ cần hiểu:

- Keycloak có realm roles/client roles.
- Spring Security có thể map claim thành authorities.
- Tenant isolation vẫn phải enforce ở service/repository.

## Không làm trong mini-lab này

- Không thay toàn bộ security flow ngay.
- Không làm login UI/React.
- Không làm refresh token/session/logout đầy đủ.
- Không làm production RBAC.
- Không cấu hình HTTPS/key rotation/cluster Keycloak production.
- Không hardcode secret thật vào repo.

## Đọc tiếp

- `docs/05-security/keycloak-admin-console-guide.md`
- `docs/05-security/keycloak-mini-lab-plan.md`
- `lab-code/keycloak-lab/README.md`
- `docs/05-security/oauth2-jwt-resource-server-concepts.md`
- `docs/05-security/jwt-implementation-walkthrough.md`

## Nguồn tham khảo chuẩn

- [Keycloak - Getting started with Docker](https://www.keycloak.org/getting-started/getting-started-docker)
- [Keycloak - Server containers](https://www.keycloak.org/server/containers)
- [Spring Security - OAuth2 Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Spring Boot - OAuth2 Resource Server](https://docs.spring.io/spring-boot/reference/web/spring-security.html#web.security.oauth2.resource-server)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0-18.html)
- [RFC 6750 - Bearer Token Usage](https://www.rfc-editor.org/rfc/rfc6750)
- [RFC 7519 - JSON Web Token](https://www.rfc-editor.org/rfc/rfc7519)
