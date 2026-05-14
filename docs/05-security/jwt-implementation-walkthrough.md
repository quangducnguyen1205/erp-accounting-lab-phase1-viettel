# Walkthrough implementation JWT tạm

## Mục tiêu

Tài liệu này giải thích ngắn flow JWT tạm đã implement trong `tenant-demo`. Đây là bước học request authentication + tenant context, không phải hệ thống Keycloak/OIDC production.

## Flow chính

```text
Client
-> Authorization: Bearer <token>
-> Spring Security Resource Server validate JWT
-> SecurityContext có Jwt đã validate
-> JwtTenantContextFilter đọc tenant_id
-> TenantContext.setCurrentTenant(tenantId)
-> Controller
-> Service
-> Repository tenant-aware
-> PostgreSQL
```

## Vai trò từng class

| Class | Vai trò |
|---|---|
| `SecurityConfig` | Khai báo `SecurityFilterChain`: stateless API, endpoint public, endpoint protected, JWT resource server, vị trí custom filter |
| `JwtProperties` | Bind `app.jwt.*` từ `application.yml`/env vars vào object có type rõ ràng |
| `JwtTokenService` | Tạo dev token local và đọc claim từ `Jwt` đã validate |
| `JwtTenantContextFilter` | Bridge từ `SecurityContext` sang `TenantContext`, luôn `clear()` trong `finally` |
| `DevTokenController` | Endpoint local-only để tạo token demo khi `app.jwt.dev-token-enabled=true` |
| `TenantFilter` | Legacy filter dùng `X-Tenant-Id`, chỉ active khi `app.jwt.enabled=false` |

## SecurityContext vs TenantContext

`SecurityContext` thuộc Spring Security. Nó lưu thông tin request đã authenticated chưa và principal hiện tại là gì.

`TenantContext` là context tự viết cho lab. Nó lưu tenant hiện tại để service/repository query theo tenant.

Điểm quan trọng: `TenantContext` chỉ được set sau khi JWT đã được validate. Không đọc `tenant_id` từ request body và không tin payload JWT chưa validate.

## Cấu hình quan trọng

```yaml
app:
  jwt:
    enabled: true
    secret: ${JWT_SECRET:local-learning-secret-change-me-32-characters-minimum}
    issuer: tenant-demo-local
    expiration-seconds: 3600
    dev-token-enabled: false
```

`JWT_SECRET` là secret local để ký/verify token tạm. Nó không phải password user và không phải secret production. Nếu chạy demo thủ công, nên copy `.env.example` thành `.env` và đổi secret local.

## Cách verify thủ công

1. Bật DB và app:

```bash
cd lab-code
make db-up
make app-run
```

2. Nếu dùng dev token endpoint, đặt `JWT_DEV_TOKEN_ENABLED=true` trong `.env`.

3. Lấy token:

```bash
curl -s http://localhost:8080/api/dev/tokens/tenant-1
curl -s http://localhost:8080/api/dev/tokens/tenant-2
```

4. Gọi API:

```bash
curl -i -H "Authorization: Bearer <TOKEN_TENANT_1>" http://localhost:8080/api/master-data
curl -i -H "Authorization: Bearer <TOKEN_TENANT_2>" http://localhost:8080/api/master-data
```

Expected pattern:

- token tenant 1 chỉ thấy `tenantId = 1`;
- token tenant 2 chỉ thấy `tenantId = 2`;
- thiếu token trả `401`;
- token sai trả `401`;
- tenant 1 gọi id của tenant 2 trả `404`.

## Giới hạn hiện tại

- Chưa có Keycloak/OIDC thật.
- Chưa có login/refresh token/user management.
- Chưa có RBAC đầy đủ.
- Dev token endpoint chỉ để học local, không dùng production.
- JWT dùng symmetric secret local; production thường dùng issuer/JWK/public key từ Identity Provider.

## Ôn nhanh trước khi demo

- Request demo dùng `Authorization: Bearer <token>`, không dùng `X-Tenant-Id` trực tiếp.
- Spring Security chạy trước controller.
- `SecurityConfig` khai báo API stateless, endpoint public/dev token và endpoint cần authenticated.
- Spring Security Resource Server validate JWT trước khi code đọc claim.
- `SecurityContext` chứa `Authentication` và principal là `Jwt` đã validate.
- `JwtTenantContextFilter` đọc claim `tenant_id` từ `Jwt`, rồi set `TenantContext`.
- `TenantContext` vẫn là thứ service/repository dùng để lấy tenant hiện tại.
- `TenantContext.clear()` phải chạy trong `finally` để tránh rò tenant giữa request.
- `JwtTokenService` tạo dev token local và đọc claim từ `Jwt`; nó không phải identity provider production.
- `JWT_SECRET` là secret local để ký/verify token tạm, không phải password user.
- Tenant isolation vẫn phụ thuộc repository/service query có `tenantId`; JWT không thay thế query tenant-aware.
- Demo hiện tại chưa phải Keycloak, chưa có RBAC production, chưa có login/refresh token.

## Đọc thêm

- `spring-security-core-concepts.md`
- `oauth2-jwt-resource-server-concepts.md`
- `spring-security-config-and-properties.md`
