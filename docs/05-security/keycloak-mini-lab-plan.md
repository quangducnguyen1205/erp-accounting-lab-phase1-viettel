# Keycloak/OIDC mini-lab plan

## Mục tiêu mini-lab

Mini-lab này chứng minh mình hiểu đường đi thật của OAuth2/OIDC và đã nối được token Keycloak vào backend demo ở mức local:

```text
Keycloak realm/client/user
-> lấy access token
-> token có issuer/JWKS/claims
-> Spring Boot validate bằng issuer-uri/JWKS khi APP_AUTH_MODE=keycloak
-> tenant_id claim sau validate mới được đưa vào TenantContext
```

Không mục tiêu production:

- chưa làm full RBAC;
- không xóa JWT tạm vì vẫn dùng làm fallback/test path;
- chưa làm React login;
- chưa triển khai Keycloak cluster/HTTPS/database production.

Nếu lần đầu mở Keycloak Admin Console, đọc thêm bản hướng dẫn trực quan trước khi bấm UI:

- `docs/05-security/keycloak-admin-console-guide.md`

Nếu còn lẫn lộn realm/client/user/JWKS/issuer hoặc JWT tạm vs Keycloak, đọc file mental model trước:

- `docs/05-security/keycloak-oidc-mental-model.md`

## Done criteria

- [x] Chạy được Keycloak local ở `http://localhost:18080`.
- [x] Tạo realm `viettel-lab`.
- [x] Tạo client local để lấy token.
- [x] Tạo ít nhất 2 user: `tenant1-user`, `tenant2-user`.
- [x] User/token có claim `tenant_id` tương ứng `1` và `2`.
- [x] Gọi token endpoint lấy access token.
- [x] Mở `.well-known/openid-configuration` và nhận ra `issuer`, `jwks_uri`.
- [x] So sánh được JWT tạm hiện tại với Keycloak token.
- [x] Spring Boot dùng `issuer-uri` ở `APP_AUTH_MODE=keycloak` nhưng vẫn giữ local JWT fallback cho test.

## Thứ tự làm khuyến nghị

### 1. Chạy Keycloak local

Dùng folder cô lập:

```bash
cd lab-code/keycloak-lab
docker compose up -d
```

Admin console:

```text
http://localhost:18080
```

Tài khoản dev-only trong lab:

```text
admin / admin
```

### 2. Tạo realm

Tạo realm:

```text
viettel-lab
```

Issuer mong đợi:

```text
http://localhost:18080/realms/viettel-lab
```

Metadata endpoint:

```text
http://localhost:18080/realms/viettel-lab/.well-known/openid-configuration
```

### 3. Tạo client để lấy token

Gợi ý lab:

```text
Client ID: tenant-demo-api-client
Client type: OpenID Connect
Client authentication: Off nếu dùng public client cho lab
Direct access grants: On nếu muốn lấy token bằng password grant để học nhanh
```

Lưu ý: password grant/direct access grant chỉ dùng để học local. Production/frontend thường dùng Authorization Code + PKCE.

### 4. Tạo user và tenant claim

Tạo user:

```text
tenant1-user / password
tenant2-user / password
```

User attributes:

```text
tenant1-user: tenant_id = 1
tenant2-user: tenant_id = 2
```

Tạo protocol mapper để đưa user attribute `tenant_id` vào access token:

```text
Mapper type: User Attribute
User Attribute: tenant_id
Token Claim Name: tenant_id
Claim JSON Type: long hoặc string đều được, nhưng backend hiện đọc được cả Number và String
Add to access token: On
```

Nếu chưa tìm được UI mapper vì version Keycloak thay đổi, ghi lại screenshot/đường dẫn menu và hỏi Codex review. Không cần ép làm production role mapping ngay.

### 5. Lấy token

Dùng file:

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

### 6. Kiểm tra token ở mức học

Không cần tự viết JWT parser. Chỉ cần decode bằng tool local/IntelliJ hoặc jwt.io để nhìn claim ở môi trường local.

Cần thấy:

- `iss = http://localhost:18080/realms/viettel-lab`;
- `sub` có user id;
- `exp` tồn tại;
- `tenant_id = 1` hoặc `2`;
- roles/scopes nếu đã cấu hình.

### 7. So sánh với JWT tạm

Không lặp lại bảng dài ở đây. Sau khi lấy được token, tự đối chiếu với:

- `docs/05-security/keycloak-oidc-mental-model.md`
- `docs/05-security/jwt-implementation-walkthrough.md`

Cần trả lời được 3 câu:

- token hiện tại do ai phát hành?
- backend verify bằng local secret hay issuer/JWKS?
- `tenant_id` đến từ dev token service hay từ user attribute + mapper?

### 8. Bước code đã nối vào backend

Backend đã có hướng:

- thêm config mode `keycloak`;
- để Spring Security Resource Server dùng `issuer-uri`/JWKS;
- không dùng local HMAC decoder khi ở Keycloak mode;
- giữ `JwtTenantContextFilter` đọc claim `tenant_id` từ `Jwt` đã validate;
- giữ tests ở local JWT mode để không phụ thuộc Keycloak container.

## Câu hỏi tự trả lời sau mini-lab

- Authorization Server là gì?
- Resource Server là gì?
- Vì sao backend không cần biết password user?
- `issuer-uri` dùng để làm gì?
- JWKS dùng để làm gì?
- Vì sao claim `tenant_id` chỉ đáng tin sau khi token đã validate?
- Vì sao Keycloak vẫn không thay thế tenant-aware repository query?
