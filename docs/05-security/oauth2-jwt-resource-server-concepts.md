# OAuth2, Bearer token và JWT Resource Server

## Mục tiêu

Note này giúp phân biệt các thuật ngữ dễ lẫn: OAuth2, Resource Server, Authorization Server, Client, Bearer token và JWT. Nội dung nối trực tiếp với `tenant-demo`, không đi sâu OAuth2 flows.

## OAuth2 ở mức rất cao

OAuth2 là framework ủy quyền truy cập. Nó định nghĩa cách một client dùng access token để gọi protected resource.

Trong kiến trúc production:

```text
Client / Frontend
-> lấy access token từ Authorization Server
-> gọi Backend Resource Server bằng Bearer token
-> Backend validate token rồi xử lý request
```

## Các vai trò chính

| Vai trò | Ý nghĩa | Trong production | Trong lab hiện tại |
|---|---|---|---|
| Client | Ứng dụng gọi API | React app, mobile app | curl/HTTP Client, sau này React UI nhỏ |
| Authorization Server | Nơi phát hành token | Keycloak/OIDC provider | Dev token endpoint local |
| Resource Server | API được bảo vệ | Spring Boot backend | `tenant-demo` |
| Resource Owner | Người dùng/chủ dữ liệu | User thật | Chưa mô phỏng đầy đủ |

## Resource Server là gì?

Resource Server là backend nhận request có access token và quyết định có cho truy cập API hay không.

Trong `tenant-demo`, Spring Boot app là Resource Server vì:

- `/api/master-data/**` là protected resource;
- request phải có `Authorization: Bearer ...`;
- Spring Security validate JWT;
- nếu token hợp lệ thì request mới đi tiếp vào controller/service.

## Bearer token là gì?

Bearer token là token mà client gửi để chứng minh quyền truy cập. Header thường dùng:

```http
Authorization: Bearer <access-token>
```

Bearer token không tự nói nó là JWT. Nó chỉ nói cách token được gửi và sử dụng: ai cầm token thì có thể dùng token đó. Vì vậy không log token, không đưa token vào URL, không commit token vào repo.

Trong lab này, Bearer token đang là JWT.

## JWT access token là gì?

JWT là format token chứa các claim. Một số claim thường gặp:

| Claim | Ý nghĩa |
|---|---|
| `iss` | issuer, ai phát hành token |
| `sub` | subject/user |
| `exp` | thời điểm hết hạn |
| `iat` | thời điểm phát hành |
| `tenant_id` | claim custom của lab để xác định tenant |
| `roles` | role đơn giản để chuẩn bị RBAC awareness |

JWT có thể được ký. Backend phải validate chữ ký, expiration và issuer trước khi tin claim.

## Keycloak nằm ở đâu?

Trong production, Keycloak thường đóng vai trò Authorization Server/OpenID Provider:

- quản lý user/client/realm/role;
- phát hành access token;
- công bố public keys/JWK để backend validate token;
- hỗ trợ OAuth2/OIDC flows.

Trong Phase 1, `tenant-demo` hiện vẫn dùng JWT tạm ở backend, còn `lab-code/keycloak-lab/` dùng để chạy Keycloak mini-lab riêng. Demo JWT tạm giúp hiểu backend side: nhận token, validate, đọc `tenant_id`, set `TenantContext`. Keycloak mini-lab giúp hiểu Authorization Server/OpenID Provider thật trước khi tích hợp vào backend.

## Vì sao lab dùng JWT local tạm?

Vì mục tiêu hiện tại là học tenant-aware backend flow, không phải dựng identity platform.

JWT local tạm giúp:

- bỏ `X-Tenant-Id` header trực tiếp;
- tập đọc tenant từ token đã validate;
- giữ demo nhỏ, test được bằng MockMvc/curl;
- chuẩn bị tư duy trước khi học Keycloak/OIDC.

Giới hạn:

- secret local không phải key management production;
- dev token endpoint không được dùng production;
- chưa có login/refresh token;
- chưa có audience validation đầy đủ;
- chưa có RBAC production.

## Liên hệ với tenant isolation

JWT chỉ đưa tenant context vào backend. Nó không tự bảo vệ database.

Tenant isolation vẫn cần:

- `JwtTenantContextFilter` set đúng `TenantContext`;
- service lấy tenant từ `TenantContext`;
- repository query bằng method có `tenantId`;
- database có constraint/index tenant-aware.

Nếu repository có method nguy hiểm như `findByCode(...)` thiếu tenant, JWT vẫn không cứu được data leakage.

## Đọc tiếp khi chuẩn bị tích hợp Keycloak

- `docs/05-security/keycloak-oidc-mental-model.md`

## Nguồn tham khảo chuẩn

- Spring Security OAuth2 Resource Server JWT: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- RFC 6750 - OAuth 2.0 Bearer Token Usage: https://www.rfc-editor.org/rfc/rfc6750.html
- RFC 7519 - JSON Web Token: https://www.rfc-editor.org/rfc/rfc7519.html
- OpenID Connect Core 1.0: https://openid.net/specs/openid-connect-core-1_0-18.html
