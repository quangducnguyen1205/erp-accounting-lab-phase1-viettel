# Keycloak Authorization / RBAC / tenant-scope

## Vai trò tài liệu

Tài liệu này chuẩn bị cho mini-lab tiếp theo sau Keycloak/OIDC. Mục tiêu là hiểu authorization ở mức vừa đủ để tự code thử trong `tenant-demo`, không làm full permission platform.

Đọc trước:

- `keycloak-oidc-mental-model.md`
- `oauth2-jwt-resource-server-concepts.md`
- `spring-security-core-concepts.md`
- `spring-boot-keycloak-integration-plan.md`

## 1. AuthN, AuthZ và tenant isolation

| Khái niệm | Câu hỏi trả lời | Ví dụ trong repo |
|---|---|---|
| Authentication/AuthN | Bạn là ai? | Token Keycloak hợp lệ, có `sub`, `iss`, `exp`. |
| Authorization/AuthZ | Bạn được làm gì? | User có role `ACCOUNTANT` được đọc API kế toán. |
| Tenant isolation | Bạn được thấy dữ liệu tenant nào? | Query repository luôn có `tenantId` từ `TenantContext`. |

Điểm quan trọng: role hợp lệ không thay thế tenant-aware query. User có role `ADMIN` hoặc `ACCOUNTANT` vẫn không được đọc dữ liệu tenant khác nếu tenant context là tenant 1.

## 2. 401 vs 403

| Status | Ý nghĩa | Ví dụ |
|---|---|---|
| `401 Unauthorized` | Request chưa được xác thực hoặc token không hợp lệ. | Thiếu Bearer token, token sai, token hết hạn. |
| `403 Forbidden` | Đã xác thực nhưng không đủ quyền. | Token hợp lệ nhưng thiếu role cần thiết. |

Mini-lab cần verify cả hai case để không nhầm authentication với authorization.

## 3. Realm roles vs client roles

### Realm roles

Role thuộc toàn realm, có thể áp dụng rộng cho nhiều client/app.

Ví dụ học tập:

- `ADMIN`
- `ACCOUNTANT`
- `VIEWER`

### Client roles

Role gắn với một client cụ thể, ví dụ role chỉ có ý nghĩa với `tenant-demo-api`.

Ví dụ:

- `tenant-demo-api:master-data-read`
- `tenant-demo-api:master-data-write`

Trong Phase 1, có thể bắt đầu bằng role đơn giản để học flow. Khi cần sát kiến trúc hơn, xem client roles vì backend API thường cần permission theo từng service/client.

## 4. Role/scope claims trong token

Keycloak token có thể chứa role ở các claim như:

- `realm_access.roles`
- `resource_access.<client-id>.roles`
- `scope`

Spring Security không tự hiểu mọi claim custom thành authority đúng ý mình. Backend thường cần converter để map claim từ JWT sang `GrantedAuthority`.

Ví dụ mental model:

```text
Keycloak access token
-> realm_access.roles = ["ACCOUNTANT"]
-> Spring Security JwtAuthenticationConverter
-> GrantedAuthority "ROLE_ACCOUNTANT"
-> @PreAuthorize("hasRole('ACCOUNTANT')")
```

## 5. Endpoint-level authorization

Endpoint-level authorization kiểm tra quyền trước khi vào controller/service.

Ví dụ mục tiêu mini-lab:

- User có role `VIEWER` được gọi `GET /api/master-data`.
- User thiếu role bị `403`.
- Missing/invalid token vẫn bị `401`.

Có thể dùng:

- cấu hình trong `SecurityFilterChain`;
- hoặc annotation như `@PreAuthorize`.

## 6. Service-level/business authorization

Không phải mọi rule nghiệp vụ nên đặt ở endpoint.

Ví dụ:

- role `ACCOUNTANT` được đọc dữ liệu kế toán;
- role `ADMIN` được gọi endpoint reindex/search local nếu đang bật lab;
- một số rule cần kiểm tra tenant, trạng thái record, ownership hoặc business state.

Service-level check phù hợp khi rule phụ thuộc dữ liệu nghiệp vụ, không chỉ phụ thuộc URL.

## 7. Tenant-scope authorization

Tenant-scope trả lời: user này đang thao tác trong tenant nào?

Trong repo hiện tại:

```text
Keycloak token đã validate
-> claim tenant_id
-> JwtTenantContextFilter
-> TenantContext
-> service/repository query theo tenantId
```

Mini-lab RBAC không được phá flow này. Role chỉ bổ sung “được phép gọi hành động nào”, còn `tenantId` vẫn quyết định phạm vi dữ liệu.

## 8. Planned mini-lab cases

Case tối thiểu:

- Valid token + role phù hợp -> `200`.
- Valid token nhưng thiếu role -> `403`.
- Missing token -> `401`.
- Invalid token -> `401`.
- Tenant 1 có role phù hợp vẫn không đọc được record tenant 2.
- Query by code vẫn scoped theo tenant.

Role gợi ý:

- `VIEWER`: đọc danh sách/tìm kiếm.
- `ACCOUNTANT`: đọc dữ liệu nghiệp vụ.
- `ADMIN`: dùng cho endpoint admin/local lab nếu thật sự cần.

## 9. Không overdo ở Phase 1

- Chưa cần full ERP permission matrix.
- Chưa cần Keycloak Authorization Services/UMA.
- Chưa cần ABAC phức tạp.
- Chưa cần xây authorization service riêng.
- Chưa cần production RBAC governance.

## Nguồn tham khảo chuẩn

- [Keycloak Server Administration Guide - Roles](https://www.keycloak.org/docs/latest/server_admin/)
- [Spring Security - Authorization](https://docs.spring.io/spring-security/reference/servlet/authorization/index.html)
- [Spring Security - OAuth2 Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
