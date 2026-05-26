# Keycloak Admin Console - hướng dẫn trực quan cho mini-lab

## Mục tiêu

Tài liệu này giúp mình bớt bỡ ngỡ khi lần đầu mở Keycloak Admin Console. Mục tiêu hiện tại chỉ là dùng được Keycloak ở mức mini-lab:

- tạo realm riêng cho lab;
- tạo client để lấy access token;
- tạo user thuộc tenant 1 và tenant 2;
- đưa claim `tenant_id` vào access token;
- hiểu Spring Boot Resource Server sẽ validate token bằng `issuer`/`JWKS`;
- giữ nguyên nguyên tắc tenant-aware ở service/repository.

File này tập trung vào thao tác UI. Nếu cần mental model OAuth2/OIDC, đọc:

- `keycloak-oidc-mental-model.md`

Nếu cần role/RBAC, đọc guide riêng:

- `keycloak-authorization-admin-console-guide.md`

Ảnh trong tài liệu được chụp từ Keycloak local dev tại `http://localhost:18080`. Không có access token thật hoặc secret production trong ảnh.

---

## 1. Mở Admin Console

Chạy Keycloak:

```bash
cd lab-code/keycloak-lab
docker compose up -d
```

Mở:

```text
http://localhost:18080
```

Đăng nhập dev-only:

```text
admin / admin
```

![Màn hình đăng nhập Keycloak Admin Console](../assets/keycloak/01-admin-login.jpg)

Kết quả mong đợi: vào được Admin Console.

![Trang landing sau khi đăng nhập](../assets/keycloak/02-admin-landing.jpg)

Lưu ý: tài khoản `admin/admin` chỉ là local dev default từ Docker Compose. Không dùng kiểu này cho môi trường thật.

---

## 2. Tạo hoặc chọn realm `viettel-lab`

Realm là một “không gian quản lý” riêng. User, client, role trong realm này tách khỏi realm khác.

Vào:

```text
Realm selector
-> Manage realms
-> Create realm
```

Nhập:

```text
viettel-lab
```

![Manage realms và nút tạo realm](../assets/keycloak/03-manage-realms-create.jpg)

Kết quả mong đợi:

- realm selector đang ở `viettel-lab`;
- metadata endpoint mở được:

```text
http://localhost:18080/realms/viettel-lab/.well-known/openid-configuration
```

Vì sao quan trọng: không cấu hình lab trực tiếp trên realm `master`.

---

## 3. Tạo client `tenant-demo-api-client`

Client là ứng dụng xin token hoặc tham gia OIDC flow. Trong mini-lab, HTTP Client/curl đóng vai client học tập để xin access token.

Vào:

```text
Clients
-> Create client
```

Gợi ý cấu hình local:

| Field | Giá trị lab |
|---|---|
| Client type | `OpenID Connect` |
| Client ID | `tenant-demo-api-client` |
| Client authentication | `Off` nếu dùng public client cho lab |
| Direct access grants | `On` để xin token nhanh bằng username/password trong HTTP Client |

![Trang cấu hình client tenant-demo-api-client](../assets/keycloak/04-client-settings.jpg)

Kết quả mong đợi: client xuất hiện trong danh sách `Clients` và có thể dùng để xin token.

Lưu ý: Direct Access Grant/password grant chỉ dùng để học local nhanh. Với frontend production, hướng phổ biến hơn là Authorization Code + PKCE.

---

## 4. Tạo user và password

User là tài khoản đăng nhập trong realm.

Vào:

```text
Users
-> Add user / Create new user
```

Tạo tối thiểu:

```text
tenant1-user
tenant2-user
```

Với Keycloak 26.x, các field bắt buộc có thể phụ thuộc cấu hình `User Profile` của realm. Trong lab hiện tại nên điền rõ các field sau để dễ đọc lại và ít bị UI/API từ chối hơn:

| Field | Gợi ý lab |
|---|---|
| Username | `tenant1-user` / `tenant2-user` |
| Email | `tenant1-user@example.local` / `tenant2-user@example.local` |
| First name | `Tenant 1` / `Tenant 2` |
| Last name | `User` |
| Email verified | `On` nếu UI hoặc policy yêu cầu |
| Enabled | `On` |

Sau đó vào:

```text
Users
-> chọn user
-> Credentials
-> Set password
Temporary: Off
```

Password này chỉ là password user lab. Nó không phải secret dùng để ký JWT.

---

## 5. Gắn `tenant_id` cho user

Để backend biết user thuộc tenant nào, lab dùng user attribute:

```text
tenant1-user: tenant_id = 1
tenant2-user: tenant_id = 2
```

Vào:

```text
Users
-> chọn user
-> Details / Attributes
```

![User tenant1-user với attribute tenant_id](../assets/keycloak/05-user-tenant-attribute.jpg)

Kết quả mong đợi:

- attribute `tenant_id` được lưu trên user;
- sau khi tạo mapper, access token có claim `tenant_id`.

Ghi chú Keycloak 26.x:

- Nếu `tenant_id` không lưu được, kiểm tra `Realm settings -> User profile`.
- Nếu UI cho lưu attribute nhưng token vẫn thiếu claim, kiểm tra mapper ở bước tiếp theo.
- Dấu hiệu verify cuối cùng không phải “UI nhìn có vẻ đúng”, mà là access token thật có claim `tenant_id`.

---

## 6. Tạo mapper đưa `tenant_id` vào access token

User attribute tự nó chưa chắc xuất hiện trong access token. Cần protocol mapper để biến `tenant_id` thành claim.

Vào:

```text
Clients
-> tenant-demo-api-client
-> Client scopes
-> dedicated scope của client
-> Mappers
-> Configure a new mapper
-> User Attribute
```

Cấu hình mapper gợi ý:

| Field | Giá trị lab |
|---|---|
| Name | `tenant-id` |
| User Attribute | `tenant_id` |
| Token Claim Name | `tenant_id` |
| Claim JSON Type | `long` hoặc `String` |
| Add to access token | `On` |

![Protocol mapper tenant-id](../assets/keycloak/06-tenant-id-mapper.jpg)

Kết quả mong đợi: access token sau khi decode có claim `tenant_id`.

Common mistake:

- User có attribute nhưng chưa có mapper.
- Mapper có nhưng chưa bật `Add to access token`.
- Đổi mapper xong nhưng vẫn dùng token cũ.

---

## 7. Xem issuer, metadata và JWKS

Spring Boot Resource Server không cần gọi database Keycloak để validate từng request. Nó dùng metadata và public key.

Vào:

```text
Realm settings
-> Endpoints
-> OpenID Endpoint Configuration
```

![Realm settings và endpoint metadata](../assets/keycloak/07-realm-settings.jpg)

Các URL quan trọng:

```text
Issuer:
http://localhost:18080/realms/viettel-lab

OIDC metadata:
http://localhost:18080/realms/viettel-lab/.well-known/openid-configuration

JWKS:
đọc từ field jwks_uri trong metadata
```

Backend Keycloak mode dùng hướng cấu hình:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:18080/realms/viettel-lab
```

---

## 8. Lấy token và kiểm tra claims

Dùng:

```text
lab-code/keycloak-lab/http/keycloak-token-flow.http
```

Hoặc curl local:

```bash
curl -s \
  -X POST "http://localhost:18080/realms/viettel-lab/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=tenant-demo-api-client" \
  -d "username=tenant1-user" \
  -d "password=password"
```

Kết quả mong đợi: response JSON có `access_token`. Không commit token thật vào repo.

Decode access token bằng công cụ local/IntelliJ/jwt.io chỉ để học claim. Cần thấy:

- `iss = http://localhost:18080/realms/viettel-lab`;
- `sub` tồn tại;
- `exp` tồn tại;
- `tenant_id = 1` hoặc `tenant_id = 2`.

---

## 9. Liên hệ với Spring Boot

Flow đúng khi sang mode Keycloak:

```text
Keycloak token
-> Spring Security Resource Server validate bằng issuer/JWKS
-> JwtTenantContextFilter đọc tenant_id claim
-> TenantContext
-> MasterDataService
-> MasterDataRepository query tenant-aware
```

Điểm quan trọng: Keycloak giúp backend tin được danh tính/token, nhưng không thay thế việc query theo `tenantId`.

---

## 10. Checklist tự kiểm tra

- [x] Tôi biết realm `viettel-lab` là gì.
- [x] Tôi biết client `tenant-demo-api-client` dùng để xin token.
- [x] Tôi tạo được user tenant 1 và tenant 2.
- [x] Tôi biết password user khác với JWT signing key/Keycloak key.
- [x] Tôi đưa được `tenant_id` vào access token.
- [x] Tôi mở được OIDC metadata và chỉ ra `issuer`, `jwks_uri`.
- [x] Tôi hiểu backend chỉ đọc `tenant_id` sau khi token validate.
- [x] Tôi hiểu Keycloak không thay thế tenant-aware repository query.

## Khi bị kẹt thì kiểm tra gì?

| Triệu chứng | Nên kiểm tra |
|---|---|
| Không vào được Admin Console | Container có chạy không, port `18080` có đúng không. |
| Token endpoint báo invalid client | Client ID đúng chưa, client có Direct Access Grants chưa. |
| Token endpoint báo invalid user/password | User enabled chưa, password đã set và Temporary Off chưa. |
| Token không có `tenant_id` | User attribute đã lưu chưa, mapper có `Add to access token` chưa. |
| Spring Boot reject token | `issuer-uri` có khớp `iss` trong token không, Keycloak có chạy không. |

## Đọc liên quan

- `docs/05-security/keycloak-oidc-mental-model.md`
- `docs/05-security/keycloak-oauth2-oidc-awareness.md`
- `docs/05-security/keycloak-mini-lab-plan.md`
- `lab-code/keycloak-lab/README.md`
- `docs/05-security/oauth2-jwt-resource-server-concepts.md`
- `docs/05-security/jwt-implementation-walkthrough.md`
- `docs/05-security/keycloak-authorization-admin-console-guide.md`

## Nguồn tham khảo chuẩn

- [Keycloak - Getting started with Docker](https://www.keycloak.org/getting-started/getting-started-docker)
- [Keycloak - Server Administration Guide](https://www.keycloak.org/docs/latest/server_admin/)
- [Spring Security - OAuth2 Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Spring Boot - OAuth2 Resource Server](https://docs.spring.io/spring-boot/reference/web/spring-security.html#web.security.oauth2.resource-server)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0-18.html)
- [RFC 6750 - Bearer Token Usage](https://www.rfc-editor.org/rfc/rfc6750)
