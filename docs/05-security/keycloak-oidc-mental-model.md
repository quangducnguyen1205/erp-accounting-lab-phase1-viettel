# Keycloak/OIDC mental model trước khi tích hợp backend

## Mục tiêu

Tài liệu này gom lại mô hình tinh thần cần nắm trước khi chuyển `tenant-demo` từ JWT tạm local sang Keycloak/OIDC. Mục tiêu là hiểu đúng vai trò của từng phần, không học toàn bộ IAM/RBAC production.

Đọc cùng:

- `docs/05-security/keycloak-admin-console-guide.md` - xem và bấm Admin Console.
- `docs/05-security/jwt-implementation-walkthrough.md` - flow JWT tạm đang chạy trong Spring Boot.
- `docs/05-security/oauth2-jwt-resource-server-concepts.md` - thuật ngữ OAuth2/JWT nền.
- `docs/05-security/spring-boot-keycloak-integration-plan.md` - kế hoạch code skeleton trước khi tích hợp backend.

## 1. JWT tạm hiện tại vs Keycloak/OIDC

### Flow hiện tại: JWT tạm local

```text
HTTP Client / curl
-> gọi dev endpoint /api/dev/tokens/tenant-1
-> tenant-demo tự tạo JWT bằng local HS256 secret
-> client gọi /api/master-data bằng Authorization: Bearer <token>
-> Spring Security validate bằng JwtDecoder local
-> SecurityContext có Jwt đã validate
-> JwtTenantContextFilter đọc tenant_id
-> TenantContext
-> Service/Repository query tenant-aware
```

Ý nghĩa: flow này giúp học cách backend đọc tenant từ token đã validate. Nó chưa phải Keycloak/OIDC production vì chính Spring Boot demo đang tự phát token.

### Flow mong muốn: Keycloak/OIDC

```text
HTTP Client / Frontend
-> xin token từ Keycloak
-> Keycloak phát access token
-> client gọi /api/master-data bằng Authorization: Bearer <access-token>
-> Spring Boot Resource Server validate token bằng issuer/JWKS
-> SecurityContext có Jwt đã validate
-> JwtTenantContextFilter đọc tenant_id
-> TenantContext
-> Service/Repository query tenant-aware
```

Điểm giống nhau: sau khi token hợp lệ, `TenantContext` và service/repository vẫn hoạt động theo cùng tư duy tenant-aware.

Điểm khác nhau: token không còn do `tenant-demo` tự phát hành bằng secret local; token đến từ Keycloak và được backend validate bằng issuer/JWKS.

## 2. Keycloak là ai trong flow?

| Thành phần | Vai trò |
|---|---|
| Keycloak | Authorization Server / OpenID Provider: quản lý user/client/role và phát token. |
| Spring Boot `tenant-demo` | Resource Server: nhận Bearer token, validate token, phục vụ API. |
| HTTP Client / curl | Client học tập: xin token và gọi API. |
| User | Người dùng đăng nhập trong realm. |
| Access token | Token client gửi cho Resource Server trong header `Authorization`. |

Trong lab này, Keycloak là nơi phát token. Spring Boot không cần biết password user; Spring Boot chỉ cần biết token có hợp lệ không và claim nào có thể tin sau khi validate.

## 3. Realm, client, user có quan hệ thế nào?

Mô hình đơn giản:

```text
Realm: viettel-lab
|
|-- Users
|   |-- tenant1-user
|   |-- tenant2-user
|
|-- Clients
|   |-- tenant-demo-api-client
|
|-- Roles
|   |-- USER, ADMIN... nếu cần sau này
|
|-- Client scopes
|   |-- dedicated scope / default scopes
|   |-- mappers
|
|-- Groups
    |-- chưa cần dùng trong mini-lab
```

Điểm dễ nhầm:

- User **không nằm trong client**.
- User và client cùng sống trong realm.
- Client đại diện cho ứng dụng/tool/API/frontend, không đại diện cho một user.
- User đăng nhập hoặc xin token thông qua một client.
- Client scopes/mappers quyết định token có claim nào.

## 4. Thuật ngữ cần nhớ

| Thuật ngữ | Nghĩa thực dụng trong lab |
|---|---|
| Realm | Không gian quản lý độc lập cho user, client, role, group, client scope. |
| Client | Ứng dụng/tool xin token hoặc tham gia OIDC flow. Lab dùng `tenant-demo-api-client`. |
| User | Tài khoản đăng nhập trong realm. Lab dùng `tenant1-user`, `tenant2-user`. |
| Role | Quyền được gán cho user/client. Phase này chỉ biết qua, chưa làm RBAC phức tạp. |
| Client scope | Nhóm claims/mappers có thể gắn vào token cho client. |
| Mapper | Cấu hình biến user attribute/role/property thành token claim. |
| Issuer | URL định danh bên phát hành token, ví dụ `http://localhost:18080/realms/viettel-lab`. |
| JWKS | Endpoint chứa public keys để backend verify chữ ký JWT từ Keycloak. |
| Access token | Token client gửi tới backend Resource Server. |
| Claim | Trường dữ liệu trong token, ví dụ `iss`, `sub`, `exp`, `tenant_id`. |

## 5. User attribute thành token claim như thế nào?

User attribute trong Keycloak chưa tự động xuất hiện trong access token.

Flow cần có:

```text
User tenant1-user
-> attribute tenant_id = 1
-> protocol mapper đọc user attribute tenant_id
-> mapper ghi token claim tenant_id
-> access token có "tenant_id": "1" hoặc 1
-> Spring Security validate token
-> JwtTenantContextFilter đọc tenant_id đã validate
```

Nếu thiếu mapper, backend sẽ không thấy claim `tenant_id` dù user đã có attribute.

## 6. `.well-known/openid-configuration` cần đọc gì?

Metadata OIDC là tài liệu JSON để client/backend biết Keycloak cung cấp endpoint nào và key ở đâu. Với realm lab:

```text
http://localhost:18080/realms/viettel-lab/.well-known/openid-configuration
```

| Field | Nghĩa | Cần ngay không? | Liên hệ với lab |
|---|---|---|---|
| `issuer` | Định danh của Authorization Server/OpenID Provider. | Có | Spring Boot `issuer-uri` phải khớp `iss` trong token. |
| `authorization_endpoint` | Nơi browser/frontend bắt đầu login OIDC. | Chưa sâu | Cần khi làm React login/Authorization Code + PKCE sau này. |
| `token_endpoint` | Nơi client đổi credential/code lấy token. | Có | HTTP Client đang POST vào endpoint này để lấy access token. |
| `jwks_uri` | Nơi công bố public keys/JWK. | Có | Resource Server dùng để verify chữ ký token Keycloak. |
| `userinfo_endpoint` | Nơi lấy thông tin user bằng access token. | Chưa cần | Backend API lab không cần gọi UserInfo lúc này. |
| `end_session_endpoint` | Nơi hỗ trợ logout/session end. | Chưa cần | Cần khi làm login/logout UI sau này. |
| `grant_types_supported` | Các grant type Keycloak hỗ trợ. | Biết qua | Lab dùng password grant/direct access grant để học nhanh; production frontend thường dùng authorization code + PKCE. |
| `response_types_supported` | Các response type cho authorization endpoint. | Chưa cần | Liên quan browser login flow, chưa dùng trong backend integration đầu tiên. |
| `scopes_supported` | Scope OIDC/OAuth2 có thể request. | Biết qua | `openid`, `profile`, `email` thường gặp; claim custom `tenant_id` đến từ mapper. |
| `token_endpoint_auth_methods_supported` | Cách client authenticate khi gọi token endpoint. | Biết qua | Public client lab không dùng client secret; confidential client production có thể dùng secret/private key. |
| `id_token_signing_alg_values_supported` hoặc signing algorithms tương tự | Thuật toán ký token được hỗ trợ. | Biết qua | Giúp hiểu backend phải trust đúng thuật toán/key, thường thấy `RS256`. |

Điểm cần nhớ: với Spring Security Resource Server, cấu hình `issuer-uri` cho phép framework discovery metadata và tìm JWKS. Nếu cấu hình thêm `jwk-set-uri`, backend vẫn nên giữ `issuer-uri` để validate claim `iss`.

## 7. Current lab object map

| Loại | Giá trị hiện tại |
|---|---|
| Keycloak URL | `http://localhost:18080` |
| Realm | `viettel-lab` |
| Issuer | `http://localhost:18080/realms/viettel-lab` |
| Client | `tenant-demo-api-client` |
| Users | `tenant1-user`, `tenant2-user` |
| User attribute | `tenant_id = 1` hoặc `tenant_id = 2` |
| Mapper | `tenant_id` user attribute -> `tenant_id` access token claim |
| Backend config sau này | `spring.security.oauth2.resourceserver.jwt.issuer-uri` |
| Protected API | `/api/master-data/**` |
| Tenant bridge | `JwtTenantContextFilter` -> `TenantContext` |

## 8. Điều gì thay đổi khi tích hợp Spring Boot?

### Hiện tại

```yaml
app:
  jwt:
    enabled: true
    secret: ${JWT_SECRET:...}
    issuer: ${JWT_ISSUER:tenant-demo-local}
```

`SecurityConfig` tự khai báo `JwtDecoder` bằng local HS256 secret.

### Hướng kế tiếp

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:18080/realms/viettel-lab
```

Ý tưởng integration:

- giữ local JWT mode làm fallback/test profile;
- thêm Keycloak mode bằng config/profile riêng;
- không bật đồng thời hai cách validate token mâu thuẫn nhau;
- `JwtTenantContextFilter` vẫn đọc `tenant_id` từ `Jwt` đã validate;
- test cần tách rõ local JWT test và Keycloak integration/manual verification.

## 9. Những phần quan trọng bây giờ

- Hiểu realm chứa user/client/role/client scope.
- Hiểu user không thuộc client.
- Lấy được token từ Keycloak.
- Decode token và thấy `iss`, `sub`, `exp`, `tenant_id`.
- Mở metadata và nhận ra `issuer`, `token_endpoint`, `jwks_uri`.
- Hiểu backend là Resource Server, không phải Authorization Server.
- Hiểu claim `tenant_id` chỉ đáng tin sau validation.
- Giữ service/repository query theo tenantId.

## 10. Có thể để sau

- Full React login flow.
- Authorization Code + PKCE chi tiết.
- Refresh token/session/logout.
- RBAC/permission matrix cho ERP.
- Audience validation sâu.
- Key rotation, HTTPS, Keycloak cluster/database production.
- Identity Provider federation.

## 11. Checklist trước khi code integration

- [ ] Tôi lấy được token từ Keycloak bằng `keycloak-token-flow.http`.
- [ ] Token tenant 1 có `tenant_id = 1`.
- [ ] Token tenant 2 có `tenant_id = 2`.
- [ ] Token có `iss`, `sub`, `exp`.
- [ ] Metadata có `issuer` và `jwks_uri`.
- [ ] Tôi hiểu JWT tạm hiện tại validate bằng local HS256 secret.
- [ ] Tôi hiểu Keycloak mode sẽ validate bằng `issuer-uri`/JWKS.
- [ ] Tôi hiểu `TenantContext` flow sau validation vẫn giữ nguyên.
- [ ] Tôi không định xóa local JWT mode ngay khi chưa có test/rollback.
- [ ] Tôi không đưa access token thật vào Git.

## 12. Nguồn tham khảo chuẩn

- [Keycloak - Server Administration Guide](https://www.keycloak.org/docs/latest/server_admin/)
- [Keycloak - Securing applications and services](https://www.keycloak.org/securing-apps/overview)
- [Spring Security - OAuth2 Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Spring Boot - OAuth2 Resource Server](https://docs.spring.io/spring-boot/reference/web/spring-security.html#web.security.oauth2.resource-server)
- [OpenID Connect Discovery 1.0](https://openid.net/specs/openid-connect-discovery-1_0.html)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0-18.html)
- [RFC 6750 - Bearer Token Usage](https://www.rfc-editor.org/rfc/rfc6750)
- [RFC 7519 - JSON Web Token](https://www.rfc-editor.org/rfc/rfc7519)
