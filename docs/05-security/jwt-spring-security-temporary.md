# JWT tạm thời trong Spring Boot tenant-demo

## Mục tiêu của task JWT

Task kế tiếp không nhằm xây hệ thống bảo mật production. Mục tiêu là thay cơ chế học tập `X-Tenant-Id` bằng một bước gần thực tế hơn:

```text
Client
-> Authorization: Bearer <token>
-> Backend validate token
-> Đọc tenant_id claim
-> Set TenantContext
-> Service/Repository query theo tenantId
```

Trong production, phần phát hành và quản lý token thường thuộc về Identity Provider như Keycloak. Trong lab này, JWT tạm chỉ giúp mình hiểu request flow và tenant context trước khi học Keycloak/OIDC đầy đủ.

## AuthN và AuthZ

| Khái niệm | Câu hỏi chính | Ví dụ trong demo |
|---|---|---|
| Authentication (AuthN) | Bạn là ai? | Token hợp lệ, có `sub` là user |
| Authorization (AuthZ) | Bạn được làm gì? | User thuộc tenant nào, có role gì |

Điểm cần nhớ: xác thực được user chưa đủ. Backend vẫn phải kiểm tra tenant scope và quyền truy cập dữ liệu.

## JWT là gì?

JWT là token dạng compact dùng để truyền các claim giữa các bên. Claim là cặp tên/giá trị nói về subject hoặc token, ví dụ:

```json
{
  "sub": "user-001",
  "tenant_id": 1,
  "roles": ["USER"],
  "exp": 1710000000
}
```

JWT có thể được ký để backend kiểm tra token có bị sửa hay không. Với lab này, điều quan trọng chưa phải thuộc thuật toán ký, mà là hiểu rằng backend không được chỉ decode token rồi tin ngay nếu chưa validate chữ ký/thời hạn/issuer theo cách phù hợp.

## Bearer token là gì?

Bearer token thường được gửi qua HTTP header:

```http
Authorization: Bearer <access-token>
```

“Bearer” nghĩa là ai cầm token thì có thể dùng token đó. Vì vậy token phải được bảo vệ, không paste vào report công khai, không commit vào repo, và không log bừa bãi.

## Claim cần quan tâm trong lab

| Claim | Ý nghĩa thực tế |
|---|---|
| `sub` | định danh user |
| `tenant_id` | tenant hiện tại của request |
| `roles` | quyền/role đơn giản cho bước học sau |
| `iat` | thời điểm phát hành token |
| `exp` | thời điểm hết hạn token |
| `iss` | issuer, hệ thống phát hành token |
| `aud` | audience, hệ thống mà token được cấp cho |

Trong demo tạm, `tenant_id` claim sẽ thay vai trò của header `X-Tenant-Id`. Service/repository vẫn phải dùng `TenantContext`, không được lấy `tenant_id` từ request body.

## Khác gì với Keycloak/OIDC production?

JWT tạm trong lab:

- có thể dùng secret local;
- có thể tạo token bằng helper/dev endpoint hoặc script học tập;
- chỉ phục vụ demo tenant flow;
- chưa có login page, refresh token, user management, federation, MFA, realm/client config đầy đủ.

Keycloak/OIDC production:

- Keycloak đóng vai trò Authorization Server / OpenID Provider;
- app backend thường là Resource Server validate access token;
- token có issuer, audience, signature, expiration, roles/scopes theo cấu hình;
- frontend thường login qua OIDC flow thay vì tự tạo token;
- public keys/JWK, rotation key, session/logout, realm/client/user/role đều được quản lý tập trung.

Vì vậy không được ghi trong report rằng demo JWT tạm là “đã triển khai Keycloak”. Cách nói đúng hơn: “đã mô phỏng bước backend đọc tenant từ token; Keycloak/OIDC sẽ là hướng production awareness”.

## Spring Security sẽ nằm ở đâu?

Trong Spring Boot production-style hơn, Spring Security thường đứng trước controller trong filter chain:

```text
HTTP request
-> Spring Security filter chain
-> Validate Bearer token
-> Authentication trong SecurityContext
-> Tenant context filter / converter đọc tenant_id
-> Controller
-> Service
-> Repository
```

Ở task kế tiếp, mình chỉ cần làm mức tối thiểu để hiểu flow. Không thêm role matrix phức tạp, không tích hợp Keycloak thật, không làm refresh token.

## Skeleton đề xuất cho task sau

Không implement ngay trong note này. Khi bắt đầu code, có thể tạo các file theo hướng TODO:

```text
lab-code/tenant-demo/src/main/java/com/viettel/demo/security/
├── JwtTokenService.java        # TODO: tạo/validate token tạm cho lab
├── JwtTenantFilter.java        # TODO: đọc Authorization: Bearer, lấy tenant_id, set TenantContext
└── SecurityConfig.java         # TODO: cấu hình filter/security tối thiểu nếu dùng Spring Security
```

Checklist trước khi tự code:

- [ ] Đọc lại `TenantFilter` hiện tại để hiểu flow header-based.
- [ ] Xác định dependency nào cần thêm, không thêm thừa.
- [ ] Không hardcode secret thật; nếu có secret local thì dùng env var.
- [ ] Token thiếu/sai/hết hạn phải bị chặn.
- [ ] Token tenant 1 chỉ thấy data tenant 1.
- [ ] Token tenant 2 chỉ thấy data tenant 2.
- [ ] Request body không được quyết định `tenant_id`.

## Common mistakes

- Decode JWT nhưng không validate chữ ký hoặc thời hạn.
- Gọi claim `tenant_id` là authorization đầy đủ. Nó chỉ là một phần của context.
- Tin `tenant_id` từ request body vì “đã có JWT rồi”.
- Log nguyên token ra console/report.
- Nhầm JWT tạm với Keycloak/OIDC production.
- Thêm Spring Security rồi vô tình bypass `TenantContext.clear()`.
- Làm role/RBAC phức tạp trước khi flow tenant cơ bản pass.

## Nguồn tham khảo chuẩn

- Spring Security Authentication: https://docs.spring.io/spring-security/reference/features/authentication/
- Spring Security Authorization: https://docs.spring.io/spring-security/reference/features/authorization/index.html
- Spring Security OAuth2 Resource Server JWT: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- RFC 7519 - JSON Web Token (JWT): https://www.rfc-editor.org/rfc/rfc7519.html
- RFC 6750 - OAuth 2.0 Bearer Token Usage: https://www.rfc-editor.org/rfc/rfc6750.html
- OpenID Connect Core 1.0: https://openid.net/specs/openid-connect-core-1_0-18.html
- Keycloak OIDC guide: https://www.keycloak.org/securing-apps/oidc-layers
