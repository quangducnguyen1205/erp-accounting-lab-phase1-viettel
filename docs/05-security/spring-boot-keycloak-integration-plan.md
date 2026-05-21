# Spring Boot Keycloak integration plan

## Mục tiêu

Tài liệu này chuẩn bị bước tích hợp Keycloak-issued JWT vào `tenant-demo` mà không phá local JWT mode đang chạy. Đây là kế hoạch/skeleton trước khi tự code phần switch thật.

Đọc trước:

- `docs/05-security/keycloak-oidc-mental-model.md`
- `docs/05-security/jwt-implementation-walkthrough.md`
- `docs/05-security/spring-security-config-and-properties.md`

## 1. Mode hiện tại: local JWT

Flow hiện tại:

```text
/api/dev/tokens/tenant-1
-> tenant-demo tạo JWT bằng HS256 local secret
-> client gọi /api/master-data bằng Bearer token
-> SecurityConfig tạo JwtDecoder bằng app.jwt.secret
-> JwtTenantContextFilter đọc tenant_id
-> TenantContext
-> Service/Repository tenant-aware
```

Config chính:

```yaml
app:
  auth:
    mode: local-jwt
  jwt:
    secret: ${JWT_SECRET:...}
    issuer: ${JWT_ISSUER:tenant-demo-local}
```

Mode này vẫn là default để `DataLeakageTest` và demo hiện tại pass.

## 2. Mode kế tiếp: Keycloak

Flow mong muốn:

```text
Keycloak
-> phát access token
-> client gọi tenant-demo bằng Authorization: Bearer <keycloak-token>
-> Spring Security Resource Server validate token bằng issuer-uri/JWKS
-> JwtTenantContextFilter đọc tenant_id claim
-> TenantContext
-> Service/Repository tenant-aware
```

Config skeleton đã chuẩn bị:

```yaml
app:
  auth:
    mode: ${APP_AUTH_MODE:local-jwt}
    keycloak:
      issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:18080/realms/viettel-lab}
      jwk-set-uri: ${KEYCLOAK_JWK_SET_URI:}
```

`issuer-uri` sẽ thay thế local HS256 secret validation trong Keycloak mode. Spring Security Resource Server có thể dùng issuer metadata để tìm JWKS và verify chữ ký token.

## 3. Cái gì giữ nguyên?

Các phần sau không cần biết token đến từ local JWT hay Keycloak:

- `JwtTenantContextFilter`;
- `TenantContext`;
- `MasterDataService`;
- `MasterDataRepository`;
- tenant-aware query pattern.

Rule vẫn giữ:

1. Token phải được validate trước.
2. Chỉ đọc `tenant_id` từ `Jwt` đã validate.
3. Thiếu hoặc sai `tenant_id` phải fail rõ ràng.
4. Repository/service vẫn query theo `tenantId`.

## 4. Code skeleton đã chuẩn bị

| File | Vai trò |
|---|---|
| `AuthProperties.java` | Bind `app.auth.mode` và `app.auth.keycloak.*`. |
| `SecurityConfig.java` | Có TODO marker cho `app.auth.mode=keycloak`. |
| `application.yml` | Có placeholder `APP_AUTH_MODE`, `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_JWK_SET_URI`. |
| `.env.example` | Có biến mẫu cho Keycloak local lab. |
| `http/keycloak-api.http` | Có request lấy token Keycloak và gọi tenant-demo API sau khi integration xong. |

Hiện tại nếu bật `APP_AUTH_MODE=keycloak`, app sẽ fail rõ với thông báo skeleton vì `JwtDecoder` issuer-uri/JWKS switch chưa được tự code. Đây là cố ý để không làm bạn tưởng Keycloak mode đã hoàn tất.

## 5. Việc cần tự code ở task tiếp theo

Trong `SecurityConfig.jwtDecoder(...)`:

- nếu `app.auth.mode=local-jwt`: giữ decoder HS256 hiện tại;
- nếu `app.auth.mode=keycloak`: tạo decoder bằng `issuer-uri` hoặc `jwk-set-uri`;
- không dùng local `JWT_SECRET` để validate token Keycloak;
- kiểm tra issuer khớp `http://localhost:18080/realms/viettel-lab`;
- giữ `JwtTenantContextFilter` chạy sau `BearerTokenAuthenticationFilter`.

Gợi ý hướng đọc:

```java
// issuer-uri direction
JwtDecoders.fromIssuerLocation(authProperties.getKeycloak().getIssuerUri())

// jwk-set-uri direction nếu muốn trỏ trực tiếp
NimbusJwtDecoder.withJwkSetUri(authProperties.getKeycloak().getJwkSetUri()).build()
```

Chỉ chọn một hướng trước. Với beginner lab, `issuer-uri` dễ giải thích hơn.

## 6. Test strategy

Không rewrite toàn bộ test ngay.

Giai đoạn này:

- `DataLeakageTest` tiếp tục dùng local JWT mode để regression test không bị phụ thuộc Keycloak container.
- Keycloak mode verify thủ công bằng HTTP Client/curl.
- Khi flow ổn, có thể thêm test profile riêng hoặc integration test sau.

Lý do: test tự động không nên phụ thuộc một Keycloak container thủ công nếu chưa có Testcontainers/profile rõ ràng.

## 7. Manual verification sau khi tự code decoder switch

1. Chạy Keycloak:

```bash
cd lab-code/keycloak-lab
docker compose up -d
```

2. Lấy token bằng:

```text
lab-code/tenant-demo/http/keycloak-api.http
```

3. Chạy app ở Keycloak mode:

```bash
cd lab-code
APP_AUTH_MODE=keycloak make app-run
```

4. Gọi API:

```http
GET http://localhost:8080/api/master-data
Authorization: Bearer <KEYCLOAK_ACCESS_TOKEN>
```

Expected:

- tenant 1 token chỉ thấy tenant 1;
- tenant 2 token chỉ thấy tenant 2;
- thiếu token trả `401`;
- token sai trả `401`;
- token thiếu `tenant_id` trả `401` rõ ràng.

## 8. Không làm trong bước này

- Không xóa local JWT mode.
- Không thêm React login.
- Không làm full RBAC.
- Không hardcode token/secret vào repo.
- Không biến Keycloak mini-lab thành production IAM platform.

## Nguồn tham khảo chuẩn

- [Spring Security - OAuth2 Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Spring Boot - OAuth2 Resource Server](https://docs.spring.io/spring-boot/reference/web/spring-security.html#web.security.oauth2.resource-server)
- [Keycloak - Securing applications and services](https://www.keycloak.org/securing-apps/overview)
- [OpenID Connect Discovery 1.0](https://openid.net/specs/openid-connect-discovery-1_0.html)
