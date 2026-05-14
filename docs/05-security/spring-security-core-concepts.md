# Spring Security core concepts trong tenant-demo

## Mục tiêu

Note này dùng để ôn nhanh các khái niệm Spring Security xuất hiện trong `tenant-demo`. Không học full framework, chỉ tập trung vào flow JWT tạm đã implement.

Đọc cùng:

- `docs/05-security/jwt-spring-security-temporary.md`
- `docs/05-security/spring-security-config-and-properties.md`
- `docs/05-security/jwt-implementation-walkthrough.md`

## Spring Security filter chain

Trong Spring MVC, request đi qua nhiều filter trước khi tới controller. Spring Security cũng hoạt động bằng filter.

Flow hiện tại:

```text
HTTP request
-> Spring Security filters
-> BearerTokenAuthenticationFilter validate token
-> JwtTenantContextFilter set TenantContext
-> Controller
```

Điểm cần nhớ: thứ tự filter quan trọng. Filter validate JWT phải chạy trước filter đọc `tenant_id`.

## SecurityFilterChain

`SecurityFilterChain` là cấu hình nói với Spring Security:

- request nào public;
- request nào cần authenticated;
- app dùng JWT resource server hay cơ chế khác;
- custom filter được đặt ở đâu.

Trong repo:

- `SecurityConfig` khai báo `SecurityFilterChain`;
- `/api/dev/tokens/**` public để lấy token local;
- `/api/master-data/**` cần Bearer JWT;
- app chạy stateless, không dùng session login.

## SecurityContext

`SecurityContext` là nơi Spring Security lưu kết quả authentication của request hiện tại.

Với JWT hợp lệ, principal trong `Authentication` là object `Jwt` đã validate. `JwtTenantContextFilter` đọc object đó, không tự decode chuỗi token.

## Authentication

`Authentication` trả lời câu hỏi: request hiện tại đã được xác thực chưa và principal là ai?

Trong lab:

- token hợp lệ -> có `Authentication`;
- principal là `Jwt`;
- `Jwt` có claim như `sub`, `tenant_id`, `roles`;
- service/repository không đọc `Authentication` trực tiếp, mà đọc `TenantContext`.

## SecurityContext khác TenantContext thế nào?

| Context | Thuộc về | Trả lời câu hỏi |
|---|---|---|
| `SecurityContext` | Spring Security | Request đã authenticated chưa, principal là ai? |
| `TenantContext` | Code lab tự viết | Request hiện tại thuộc tenant nào? |

Không nên trộn hai khái niệm này. `SecurityContext` là nguồn đã validate; `TenantContext` là cầu nối để business layer query theo tenant.

## Stateless API

Stateless nghĩa là backend không lưu login session server-side cho mỗi user. Mỗi request phải tự mang credential, ở đây là Bearer JWT.

Lợi ích trong lab:

- dễ test bằng curl/HTTP Client;
- phù hợp REST API;
- không cần học session/login form lúc này.

Giới hạn:

- token bị lộ thì người cầm token có thể gọi API cho đến khi token hết hạn;
- production cần HTTPS, thời hạn token hợp lý, key rotation, issuer/audience, monitoring.

## Vì sao disable CSRF trong lab?

CSRF quan trọng với app dùng cookie/session mà browser tự gửi credential. Trong lab này API dùng Bearer token gửi qua `Authorization` header và không dùng session login, nên `SecurityConfig` disable CSRF để giữ REST flow đơn giản.

Không hiểu nhầm: disable CSRF không có nghĩa là CSRF luôn vô dụng. Nếu sau này dùng cookie-based auth hoặc browser tự gửi credential, phải đánh giá lại CSRF.

## Demo vs production

Phần đang có là demo:

- HS256 secret local;
- dev token endpoint;
- chưa có Keycloak;
- chưa có refresh token;
- chưa có RBAC production.

Production thường cần:

- Authorization Server/Identity Provider như Keycloak;
- public key/JWK hoặc issuer discovery;
- audience validation;
- role/scope mapping rõ ràng;
- không để dev token endpoint.

## Nguồn tham khảo chuẩn

- Spring Security Servlet Architecture: https://docs.spring.io/spring-security/reference/servlet/architecture.html
- Spring Security OAuth2 Resource Server JWT: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- Spring Security CSRF: https://docs.spring.io/spring-security/reference/features/exploits/csrf.html

