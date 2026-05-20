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

Hiện tại `tenant-demo` vẫn dùng JWT tạm HS256. Sau mini-lab, hướng code tiếp theo là thêm mode Keycloak an toàn:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:18080/realms/viettel-lab
```

Khi dùng issuer-uri, Spring Security Resource Server có thể discovery metadata và JWKS để validate token từ Keycloak.

Không đổi code trong bước lab này để tránh phá test JWT hiện tại.

## Done criteria

- [ ] Keycloak chạy ở `localhost:18080`.
- [ ] Realm `viettel-lab` tồn tại.
- [ ] Client `tenant-demo-api-client` lấy được token.
- [ ] Token tenant 1 có `tenant_id = 1`.
- [ ] Token tenant 2 có `tenant_id = 2`.
- [ ] Mở được `.well-known/openid-configuration` và chỉ ra `issuer`, `jwks_uri`.
- [ ] Giải thích được khác biệt giữa JWT tạm và Keycloak/OIDC.
