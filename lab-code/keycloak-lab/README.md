# Keycloak mini-lab

## Mục tiêu

Folder này chạy Keycloak local tách biệt với `tenant-demo`. Mục tiêu là lấy token OIDC thật từ Keycloak và hiểu issuer/JWKS/claims trước khi thay đổi code Spring Boot.

Không dùng lab này như production IAM setup.

Nếu Admin Console nhìn quá nhiều mục, đọc trước:

```text
docs/05-security/keycloak-admin-console-guide.md
```

Nếu chưa chắc realm/client/user/mapper/JWKS liên hệ với Spring Boot như thế nào, đọc trước:

```text
docs/05-security/keycloak-oidc-mental-model.md
```

## Chạy Keycloak

```bash
cd lab-code/keycloak-lab
docker compose up -d
```

Admin console:

```text
http://localhost:18080
```

Dev-only admin:

```text
admin / admin
```

Dừng lab:

```bash
docker compose down
```

## Thiết lập thủ công trong Admin Console

### 1. Tạo realm

```text
Realm name: viettel-lab
```

Metadata endpoint:

```text
http://localhost:18080/realms/viettel-lab/.well-known/openid-configuration
```

Issuer:

```text
http://localhost:18080/realms/viettel-lab
```

### 2. Tạo client

Gợi ý cho lab:

```text
Client ID: tenant-demo-api-client
Client type: OpenID Connect
Client authentication: Off
Direct access grants: On
```

Password grant/direct access grants chỉ để học local nhanh. Production/frontend thường dùng Authorization Code + PKCE.

### 3. Tạo user

```text
tenant1-user / password
tenant2-user / password
```

Gợi ý điền field để tránh quên khi dùng Keycloak 26.x:

| User | Email | First name | Last name | Email verified |
|---|---|---|---|---|
| `tenant1-user` | `tenant1-user@example.local` | `Tenant 1` | `User` | `On` nếu UI/policy yêu cầu |
| `tenant2-user` | `tenant2-user@example.local` | `Tenant 2` | `User` | `On` nếu UI/policy yêu cầu |

Trong một số cấu hình Keycloak 26.x, User Profile có thể yêu cầu thêm field hoặc quản lý custom attributes chặt hơn. Nếu không lưu được user/attribute, kiểm tra `Realm settings -> User profile` trước khi nghi ngờ Spring Boot.

User attributes:

```text
tenant1-user: tenant_id = 1
tenant2-user: tenant_id = 2
```

### 4. Đưa `tenant_id` vào access token

Tạo protocol mapper cho client hoặc client scope:

```text
Mapper type: User Attribute
User Attribute: tenant_id
Token Claim Name: tenant_id
Claim JSON Type: long hoặc string
Add to access token: On
```

Backend hiện đọc được cả `tenant_id` dạng number hoặc string.

Ghi chú với Keycloak 26.x: nếu set user attribute bằng Admin Console thì thường dễ thấy ngay trong UI. Nếu setup bằng CLI/API, cần nhớ User Profile có thể chặn unmanaged attributes. Cách kiểm tra nhanh là lấy token và decode access token: phải thấy claim `tenant_id`.

Khi `kcadm.sh update ... -s attributes...` không lưu attribute như mong đợi, dùng Admin REST `PUT /admin/realms/{realm}/users/{id}` với body `UserRepresentation` đầy đủ là cách verify rõ hơn trong lab. Không cần đưa access token admin hoặc token user vào repo.

## Lấy token

Dùng:

```text
lab-code/keycloak-lab/http/keycloak-token-flow.http
```

Hoặc curl:

```bash
curl -s \
  -X POST "http://localhost:18080/realms/viettel-lab/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=tenant-demo-api-client" \
  -d "username=tenant1-user" \
  -d "password=password"
```

Không commit access token thật.

## Spring Boot sẽ dùng gì sau này?

`tenant-demo` hiện đã có hai mode học tập:

- `local-jwt`: dùng JWT tạm HS256 để test tự động không phụ thuộc Keycloak.
- `keycloak`: dùng `issuer-uri`/JWKS để validate token do Keycloak phát hành.

Keycloak mode dùng cấu hình kiểu:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:18080/realms/viettel-lab
```

Khi dùng issuer-uri, Spring Security Resource Server có thể discovery metadata và JWKS để validate token từ Keycloak.

Local JWT mode vẫn được giữ làm fallback cho `make app-test`. Không xóa fallback này cho tới khi có test Keycloak tách riêng hoặc môi trường CI có Keycloak ổn định.

Sau khi lấy token từ Keycloak, gọi tenant-demo bằng file:

```text
lab-code/tenant-demo/http/keycloak-api.http
```

Không paste token thật vào repo.

## RBAC/Authorization mini-lab tiếp theo

Sau khi token flow đã chạy được, phần kế tiếp là tạo role và kiểm tra authorization:

```text
docs/05-security/keycloak-authorization-rbac-tenant-scope.md
docs/05-security/keycloak-authorization-admin-console-guide.md
docs/05-security/keycloak-authorization-mini-lab-plan.md
lab-code/tenant-demo/http/keycloak-authorization-api.http
```

Gợi ý roles nhỏ:

```text
ADMIN
ACCOUNTANT
VIEWER
```

Ưu tiên tạo client roles dưới `tenant-demo-api-client` để role claim nằm trong `resource_access.tenant-demo-api-client.roles`. Nếu dùng realm roles để học nhanh, token thường có `realm_access.roles`.

Ghi chú Keycloak 26.x: nếu custom user attribute như `tenant_id` không lưu hoặc không xuất hiện trong token, kiểm tra `Realm settings -> User profile`, mapper `tenant_id`, và lấy token mới sau khi đổi attribute.

## Done criteria

- [x] Keycloak chạy ở `localhost:18080`.
- [x] Realm `viettel-lab` tồn tại.
- [x] Client `tenant-demo-api-client` lấy được token.
- [x] Token tenant 1 có `tenant_id = 1`.
- [x] Token tenant 2 có `tenant_id = 2`.
- [x] Mở được `.well-known/openid-configuration` và chỉ ra `issuer`, `jwks_uri`.
- [x] `tenant-demo` gọi được bằng Keycloak token khi chạy `APP_AUTH_MODE=keycloak`.
- [x] Giải thích được khác biệt giữa JWT tạm và Keycloak/OIDC.

## Giới hạn của lab hiện tại

- Realm/client/user được tạo thủ công, chưa có realm import/export chuẩn.
- Password grant/direct access grants chỉ dùng để học local nhanh.
- Chưa làm HTTPS, key rotation, RBAC role matrix, production Keycloak database/cluster.
- Nếu chạy `docker compose down` và môi trường không persist volume, có thể phải tạo lại realm/client/user.
