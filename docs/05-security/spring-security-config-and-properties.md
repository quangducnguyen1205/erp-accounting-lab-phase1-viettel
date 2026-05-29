# Spring Security config và configuration properties

## Mục tiêu

Note này giúp hiểu phần cấu hình trước khi tự code JWT tạm. Mục tiêu không phải học full Spring Security, mà là biết chỗ nào validate token, chỗ nào set `TenantContext`, và vì sao config nên gom vào `JwtProperties`.

Các khái niệm nền như `SecurityFilterChain`, `SecurityContext`, `Authentication` và stateless API được tách riêng trong `spring-security-core-concepts.md` để tránh lặp lại quá nhiều ở file này.

## SecurityConfig là gì?

`SecurityConfig` là class cấu hình security cho Spring Boot app. Khi active, nó thường khai báo một `SecurityFilterChain` bean để nói với Spring Security:

- request nào được public;
- request nào phải có token;
- app dùng cơ chế authentication nào;
- custom filter nằm ở đâu trong security chain.

Trong repo này, `SecurityConfig.java` hiện đã active cho JWT tạm. Nó khai báo REST API stateless, cho phép dev token endpoint, bảo vệ `/api/master-data/**`, bật OAuth2 Resource Server JWT và gắn `JwtTenantContextFilter` sau bước Bearer token authentication.

## SecurityFilterChain là gì?

Ở Spring MVC, request đi qua nhiều filter trước khi tới controller. Spring Security cũng là một hệ thống filter. `SecurityFilterChain` là danh sách filter security áp dụng cho request.

Flow mục tiêu:

```text
Client
-> Spring Security filters
-> Validate Bearer JWT
-> SecurityContext có Authentication/Jwt
-> JwtTenantContextFilter set TenantContext
-> Controller
-> Service
-> Repository
```

So với `TenantFilter` cũ:

| Hiện tại | JWT tạm sau này |
|---|---|
| `TenantFilter` đọc `X-Tenant-Id` trực tiếp | Spring Security validate JWT trước |
| Tenant đến từ header học tập | Tenant đến từ claim `tenant_id` đã validate |
| Missing/invalid tenant trả `400` | Missing/invalid token thường trả `401` |
| Dễ hiểu để học tenant flow | Gần production hơn nhưng vẫn chưa phải Keycloak |

`TenantFilter` cũ đã bị loại bỏ vì JWT là mặc định. Chế độ legacy mode không còn được hỗ trợ.

## Vì sao phải validate JWT trước khi đọc tenant_id?

JWT payload có thể decode được dễ dàng. Nhưng decode không có nghĩa là token đáng tin.

Backend chỉ nên tin `tenant_id` sau khi token đã được validate:

- chữ ký đúng;
- token chưa hết hạn;
- issuer đúng;
- token đúng format;
- các claim bắt buộc tồn tại.

Vì vậy `JwtTenantContextFilter` không nên tự parse string token. Nó nên đọc `Jwt` đã được Spring Security đặt trong `SecurityContext`.

## SecurityContext là gì?

Ở mức beginner, `SecurityContext` là nơi Spring Security lưu thông tin authentication của request hiện tại.

Sau khi JWT hợp lệ, Spring Security có thể đặt một `Authentication` vào `SecurityContext`. Với JWT resource server, principal thường là object `Jwt` chứa claims đã validate.

`JwtTenantContextFilter` sau này sẽ đọc từ đó:

```java
// Pseudo-code, chưa phải implementation hoàn chỉnh
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
Jwt jwt = (Jwt) authentication.getPrincipal();
Long tenantId = jwt.getClaim("tenant_id");
```

## @ConfigurationProperties là gì?

`@ConfigurationProperties` bind một nhóm config từ `application.yml` hoặc environment variables vào một Java object.

Thay vì rải rác:

```java
@Value("${app.jwt.secret}")
private String secret;
```

ta gom lại:

```text
app.jwt -> JwtProperties
```

Lợi ích:

- config có type rõ ràng;
- dễ inject vào service/config;
- giảm lỗi copy-paste key;
- dễ giải thích với mentor hơn.

Trong repo này, `JwtProperties` bind các key:

- `app.jwt.secret`;
- `app.jwt.issuer`;
- `app.jwt.expiration-seconds`;
- `app.jwt.dev-token-enabled`.

Các key này lấy default từ `application.yml`, và có thể override bằng env vars như `JWT_SECRET`, `JWT_ISSUER`.

Trong implementation hiện tại, `JwtProperties` dùng `@Component` để được Spring scan như một bean. Cách này đơn giản cho lab. Khi dự án lớn hơn, có thể cân nhắc cách chuẩn hơn là `@EnableConfigurationProperties(JwtProperties.class)` trong một configuration class.

## Secret local khác password user

`JWT_SECRET` là secret local để ký hoặc verify token trong lab. Nó không phải password user.

Trong production với Keycloak/OIDC, backend thường không dùng shared secret kiểu demo. Backend validate token dựa trên issuer, audience và public keys/JWK do Identity Provider công bố.

## Dependency đang dùng

Implementation hiện tại dùng `spring-boot-starter-oauth2-resource-server`. Dependency này kéo Spring Security và JWT support cần thiết để backend validate Bearer token.

Repo không thêm Keycloak adapter, không thêm full OAuth2 login, không thêm thư viện JWT custom riêng. Đây là lựa chọn cố ý để giữ task nhỏ và giải thích được.

## DataLeakageTest sẽ đổi thế nào?

Hiện test đã gửi:

```text
Authorization: Bearer <token tenant 1>
```

Các case vẫn giữ nguyên về ý nghĩa:

- valid token tenant 1 chỉ thấy data tenant 1;
- valid token tenant 2 chỉ thấy data tenant 2;
- missing token trả `401`;
- invalid token trả `401`;
- tenant 1 không truy cập được id của tenant 2;
- query by code vẫn scoped theo tenant.

## Nguồn tham khảo chuẩn

- Spring Security Servlet Architecture: https://docs.spring.io/spring-security/reference/servlet/architecture.html
- Spring Security OAuth2 Resource Server JWT: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- Spring Boot Externalized Configuration: https://docs.spring.io/spring-boot/reference/features/external-config.html
- RFC 7519 - JSON Web Token: https://www.rfc-editor.org/rfc/rfc7519.html
- RFC 6750 - Bearer Token Usage: https://www.rfc-editor.org/rfc/rfc6750.html
