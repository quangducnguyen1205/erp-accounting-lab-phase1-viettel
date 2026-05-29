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

Nếu cần ôn thuật ngữ trước demo, đọc thêm:

- `spring-security-core-concepts.md`
- `oauth2-jwt-resource-server-concepts.md`
- `jwt-implementation-walkthrough.md`

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

## JWT secret không phải password user

Secret/private key dùng để ký hoặc verify JWT là bí mật của hệ thống phát hành token. Nó khác với password của user.

Ví dụ trong lab:

- user password: nếu có, là thứ user dùng để đăng nhập;
- `JWT_SECRET`: là secret local để backend/dev tool ký hoặc kiểm tra token tạm;
- `tenant_id` claim: là dữ liệu trong token sau khi token đã được validate.

Không được dùng password user để ký JWT. Không được commit secret thật vào Git. Trong production với Keycloak/OIDC, backend thường validate token bằng public key/JWK hoặc cấu hình issuer/audience, không tự phát minh cơ chế key management.

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

## Thiết kế khuyến nghị cho repo này

Không nên tự viết parser/validator JWT thủ công trong `JwtTenantContextFilter`.

Hướng sạch hơn:

```text
HTTP request
-> Spring Security Resource Server validate Bearer JWT
-> SecurityContext chứa Authentication/Jwt đã validate
-> JwtTenantContextFilter đọc tenant_id claim
-> TenantContext.setCurrentTenant(tenantId)
-> Controller
-> Service
-> Repository tenant-aware
```

Lý do:

- JWT phải được validate chữ ký, expiration, issuer trước khi tin claim.
- Filter tenant context chỉ nên đọc token đã validate, không tự làm security engine.
- Service/repository hiện đã dùng `TenantContext`, nên không cần đổi business logic quá nhiều.
- Đây vẫn là JWT local tạm, không phải Keycloak/OIDC production.

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

## Implementation hiện tại trong repo

Task JWT tạm đã được implement ở mức lab:

```text
lab-code/tenant-demo/src/main/java/com/viettel/demo/security/
├── JwtClaims.java             # object gom tenant_id, sub, roles sau validate
├── JwtProperties.java         # bind app.jwt từ application.yml/env vars
├── JwtTokenService.java        # tạo dev token local, đọc claim từ Jwt đã validate
├── JwtTenantContextFilter.java # đọc Jwt từ SecurityContext, set/clear TenantContext
├── DevTokenController.java     # endpoint local-only để tạo token dev
└── SecurityConfig.java         # stateless SecurityFilterChain + Resource Server JWT
```

Flow implementation:

```text
Authorization: Bearer <token>
-> Spring Security validate chữ ký/expiration/issuer
-> JwtTenantContextFilter đọc tenant_id từ Jwt đã validate
-> TenantContext.setCurrentTenant(tenantId)
-> Service/Repository query tenant-aware như trước
-> finally TenantContext.clear()
```

`TenantFilter` header-based cũ đã bị loại bỏ vì JWT là mặc định. Backend không đọc `X-Tenant-Id` làm tenant context chính nữa.

Checklist khi verify:

- [ ] `JWT_SECRET` (copy từ `.env.example`).
- [ ] `JWT_SECRET` đủ dài và không phải secret production.
- [ ] `JWT_DEV_TOKEN_ENABLED=true` nếu muốn dùng endpoint `/api/dev/tokens/...`.
- [ ] Missing/invalid token trả `401`.
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
