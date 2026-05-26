# Keycloak Authorization / RBAC mini-lab plan

## Mục tiêu

Mini-lab này bổ sung phần authorization sau khi Keycloak/OIDC token validation đã chạy được.

Kết quả cần đạt:

```text
Token hợp lệ + role phù hợp -> gọi API được
Token hợp lệ + thiếu role -> 403
Thiếu/sai token -> 401
Role đúng vẫn không đọc được dữ liệu tenant khác
```

Không làm:

- full ERP permission matrix;
- Keycloak Authorization Services/UMA;
- React login flow;
- production IAM setup.

---

## 1. Tài liệu cần đọc trước

1. `keycloak-authorization-rbac-tenant-scope.md`
2. `keycloak-authorization-code-guide-spring-boot.md`
3. `keycloak-authorization-admin-console-guide.md`
4. `spring-boot-keycloak-integration-plan.md` nếu cần nhớ lại Keycloak mode.

---

## 2. Role model nhỏ cho lab

| Role | Mục đích |
|---|---|
| `VIEWER` | Đọc API cơ bản. |
| `ACCOUNTANT` | Role nghiệp vụ giả lập, có thể đọc/ghi sau này. |
| `ADMIN` | Dùng cho endpoint admin/local lab như reindex nếu cần. |

Khuyến nghị: tạo roles dưới client `tenant-demo-api-client`. Nếu thao tác chậm, dùng realm roles trước rồi ghi lại trade-off.

---

## 3. Setup Keycloak

1. Start Keycloak:

```bash
cd lab-code/keycloak-lab
docker compose up -d
```

2. Mở Admin Console:

```text
http://localhost:18080
```

3. Kiểm tra realm/client/user cũ:

```text
realm: viettel-lab
client: tenant-demo-api-client
users: tenant1-user, tenant2-user
tenant_id attribute: 1 / 2
```

4. Tạo/assign roles:

```text
tenant1-user -> ACCOUNTANT hoặc VIEWER
tenant2-user -> VIEWER
optional no-role user -> không có role cần thiết
```

5. Lấy token mới và decode để verify:

```text
tenant_id
realm_access.roles hoặc resource_access.tenant-demo-api-client.roles
```

---

## 4. Spring Boot self-code checklist

Tự code theo thứ tự:

- [ ] Thêm config nếu cần: `app.auth.keycloak.client-id`.
- [ ] Implement converter đọc Keycloak roles từ JWT.
- [ ] Map role thành `ROLE_ADMIN`, `ROLE_ACCOUNTANT`, `ROLE_VIEWER`.
- [ ] Wire converter vào `oauth2ResourceServer().jwt(...)`.
- [ ] Nếu dùng `@PreAuthorize`, bật `@EnableMethodSecurity`.
- [ ] Thêm authorization rule nhỏ:
  - read endpoint cần `VIEWER` hoặc `ACCOUNTANT`;
  - optional reindex endpoint cần `ADMIN`.
- [ ] Không đổi `JwtTenantContextFilter` sang đọc role.
- [ ] Không đổi repository method tenant-aware.
- [ ] Tạo test RBAC riêng nếu có thời gian; không làm bẩn `DataLeakageTest`.

---

## 5. Manual HTTP verification cases

Tạo token bằng:

```text
lab-code/keycloak-lab/http/keycloak-token-flow.http
```

Gọi API bằng:

```text
lab-code/tenant-demo/http/keycloak-authorization-api.http
```

Cases:

| Case | Expected |
|---|---|
| Missing token gọi `GET /api/master-data` | `401` |
| Invalid token gọi `GET /api/master-data` | `401` |
| Token có role đọc gọi `GET /api/master-data` | `200` |
| Token thiếu role gọi endpoint cần role | `403` |
| Tenant 1 role đúng gọi id tenant 2 | `404` hoặc không lộ dữ liệu |
| Tenant 1/tenant 2 search cùng keyword nếu search enabled | mỗi tenant chỉ thấy data của mình |
| Optional `VIEWER` gọi reindex | `403` |
| Optional `ADMIN` gọi reindex | `200` nếu search enabled |

---

## 6. Automated test strategy

Không bắt buộc test tự động phụ thuộc live Keycloak.

Gợi ý:

- `DataLeakageTest` tiếp tục dùng local JWT mode để bảo vệ tenant isolation.
- Tạo `AuthorizationTest` riêng dùng local JWT role claim nếu muốn regression nhanh.
- Keycloak role mapping có thể verify manual bằng HTTP trước.

Nếu sau này CI có Keycloak container ổn định, có thể tách integration profile riêng.

---

## 7. Done criteria

- [ ] Token Keycloak có role claim rõ.
- [ ] Backend map role thành `GrantedAuthority`.
- [ ] User có role phù hợp nhận `200`.
- [ ] User thiếu role nhận `403`.
- [ ] Missing/invalid token vẫn `401`.
- [ ] Cross-tenant id vẫn không leak.
- [ ] `cd lab-code && make app-test` pass.
- [ ] Summary ngắn được ghi vào `docs/99-tong-ket/nhung-gi-da-nam-duoc.md`.

---

## 8. Câu hỏi tự trả lời sau lab

- Role đang nằm ở `realm_access` hay `resource_access`?
- Spring Security đang có authority tên gì?
- `hasRole` hay `hasAuthority` phù hợp hơn với converter của mình?
- Case nào là `401`, case nào là `403`?
- Vì sao role đúng vẫn không được bỏ `tenantId` khỏi query?

