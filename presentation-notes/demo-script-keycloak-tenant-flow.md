# Demo script: Keycloak + tenant-aware API

> Mục tiêu: có một đường demo backend chắc chắn để trình bày với mentor trước khi quyết định có làm React UI nhỏ hay không.

## 1. Thông điệp chính

Demo này chứng minh flow:

```text
Keycloak phát access token
-> tenant-demo validate token bằng issuer/JWKS
-> JwtTenantContextFilter lấy tenant_id từ token đã validate
-> Service/Repository query theo tenantId
-> API không lộ dữ liệu cross-tenant
```

Điểm cần nói rõ: Keycloak xác thực token và cung cấp claim đáng tin hơn JWT tạm, nhưng tenant isolation vẫn phải được enforce ở service/repository.

## 2. Chuẩn bị trước khi demo

Mở 3 terminal:

- Terminal A: PostgreSQL.
- Terminal B: Keycloak.
- Terminal C: tenant-demo Spring Boot.

Kiểm tra không commit token thật, admin token hoặc secret thật vào repo. File HTTP Client chỉ dùng placeholder.

## 3. Chạy PostgreSQL

```bash
cd lab-code
make db-up
make db-status
```

Kỳ vọng: PostgreSQL container healthy hoặc running.

## 4. Chạy Keycloak local

```bash
cd lab-code/keycloak-lab
docker compose up -d
```

Mở Admin Console nếu cần kiểm tra:

```text
http://localhost:18080
```

Dev-only admin trong lab:

```text
admin / admin
```

Kiểm tra metadata:

```bash
curl -s http://localhost:18080/realms/viettel-lab/.well-known/openid-configuration
```

Cần nhận ra các field:

- `issuer`
- `token_endpoint`
- `jwks_uri`

## 5. Kiểm tra setup Keycloak

Trong Admin Console:

- realm: `viettel-lab`
- client: `tenant-demo-api-client`
- users: `tenant1-user`, `tenant2-user`
- user attribute: `tenant_id = 1` hoặc `tenant_id = 2`
- mapper: đưa `tenant_id` vào access token

Với Keycloak 26.x, nếu attribute không lưu hoặc token thiếu `tenant_id`, kiểm tra `Realm settings -> User profile` và mapper `Add to access token`.

## 6. Chạy tenant-demo ở Keycloak mode

Cách an toàn nhất là để `.env` local chứa config thật, nhưng không commit `.env`.

Ví dụ chạy trực tiếp trong terminal:

```bash
cd lab-code/tenant-demo
APP_AUTH_MODE=keycloak \
KEYCLOAK_ISSUER_URI=http://localhost:18080/realms/viettel-lab \
JWT_DEV_TOKEN_ENABLED=false \
./mvnw spring-boot:run
```

Kỳ vọng log:

- app start thành công ở port `8080`;
- Flyway không lỗi;
- Resource Server dùng issuer-uri Keycloak.

## 7. Lấy token Keycloak

Dùng IntelliJ HTTP Client:

```text
lab-code/keycloak-lab/http/keycloak-token-flow.http
```

Chạy:

- `Token tenant 1 bằng password grant`
- `Token tenant 2 bằng password grant`

Không paste access token thật vào repo/report. Nếu cần dùng lặp lại trong IntelliJ, paste tạm trong working tree local hoặc private environment file bị ignore.

## 8. Gọi API bằng Keycloak token

Dùng:

```text
lab-code/tenant-demo/http/keycloak-api.http
```

Các case cần chạy:

| Case | Kỳ vọng |
|---|---|
| Tenant 1 list `/api/master-data` | `200`, chỉ thấy `tenantId = 1` |
| Tenant 2 list `/api/master-data` | `200`, chỉ thấy `tenantId = 2` |
| Missing token | `401` |
| Invalid token | `401` |
| Tenant 1 query `code = LAPTOP-01` | trả record tenant 1 |
| Tenant 2 query `code = LAPTOP-01` | trả record tenant 2 |
| Tenant 1 gọi id thuộc tenant 2 | `404`, không lộ dữ liệu |

Với cross-tenant id:

1. Chạy tenant 2 list.
2. Copy một `id` thuộc tenant 2.
3. Paste vào biến `@tenant2RecordId`.
4. Gọi request cross-tenant bằng token tenant 1.
5. Kỳ vọng `404`.

## 9. Regression fallback

Sau demo thủ công, chạy test tự động:

```bash
cd lab-code
make app-test
```

Kỳ vọng: test tenant isolation vẫn pass ở local JWT mode. Test này không phụ thuộc container Keycloak để tránh flaky trong Phase 1.

## 10. Cách nói với mentor

Ngắn gọn:

- Ban đầu em dùng `X-Tenant-Id` để học tenant context.
- Sau đó em chuyển sang JWT tạm để hiểu Bearer token và Spring Security.
- Tiếp theo em chạy Keycloak mini-lab: Keycloak phát token, Spring Boot validate bằng issuer/JWKS.
- `tenant_id` chỉ được tin sau khi token đã validate.
- Dù auth đã tốt hơn, backend vẫn phải query theo `tenantId`; Keycloak không tự chống data leakage trong repository.

## 11. Giới hạn hiện tại

- Chưa phải production Keycloak setup.
- Password grant/direct access grant chỉ dùng để học local nhanh.
- Chưa có Authorization Code + PKCE cho React frontend.
- Chưa có RBAC role matrix đầy đủ.
- Chưa có realm import/export chuẩn để setup Keycloak hoàn toàn repeatable.
- React UI để sau nếu còn thời gian; backend demo script này là đường demo chắc chắn hơn.

