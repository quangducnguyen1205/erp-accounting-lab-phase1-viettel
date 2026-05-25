# Keycloak Authorization code guide cho Spring Boot tenant-demo

## Vai trò tài liệu

Đây là code guide/skeleton cho task Keycloak Authorization/RBAC. Chưa implement code trong tài liệu này. Mục tiêu là chuẩn bị hình dạng code để mình tự code và sau đó nhờ review.

Đọc theory trước:

- `keycloak-authorization-rbac-tenant-scope.md`

## 1. Target flow

```text
Request có Bearer token Keycloak
-> Spring Security validate issuer/JWKS
-> JwtAuthenticationConverter map role claim sang GrantedAuthority
-> SecurityFilterChain / @PreAuthorize kiểm tra quyền
-> JwtTenantContextFilter set TenantContext từ tenant_id
-> Service/Repository query tenant-aware
```

Điểm giữ nguyên:

- `TenantContext` vẫn lấy tenant từ claim đã validate.
- Repository vẫn query theo `tenantId`.
- Role không thay thế tenant filter.

## 2. File/khu vực có thể chạm

Dự kiến:

```text
lab-code/tenant-demo/src/main/java/com/viettel/demo/security/
├── SecurityConfig.java
├── AuthProperties.java
├── JwtTenantContextFilter.java
└── KeycloakRoleConverter.java          # TODO nếu cần tách converter

lab-code/tenant-demo/src/main/java/com/viettel/demo/service/
└── MasterDataService.java              # TODO nếu cần service-level check

lab-code/tenant-demo/src/main/java/com/viettel/demo/controller/
└── MasterDataController.java           # TODO nếu dùng @PreAuthorize
```

Không tạo nhiều class nếu chưa cần. Nếu converter ngắn, có thể để private bean trong `SecurityConfig` trước.

## 3. Configuration idea

Có thể thêm config nếu cần:

```yaml
app:
  auth:
    mode: ${APP_AUTH_MODE:local-jwt}
    required-read-role: ${APP_REQUIRED_READ_ROLE:VIEWER}
```

Không hardcode secret/token thật.

## 4. Role converter TODO

Mục tiêu converter:

- đọc role từ `realm_access.roles` hoặc `resource_access.<client-id>.roles`;
- map role sang `GrantedAuthority`;
- thống nhất prefix, ví dụ `ROLE_VIEWER`, `ROLE_ACCOUNTANT`, `ROLE_ADMIN`;
- không làm full permission matrix.

Pseudo-shape:

```text
Jwt
-> extract realm roles / client roles
-> map each role to SimpleGrantedAuthority
-> return collection authorities
```

Điểm cần tự quyết:

- dùng realm role hay client role cho mini-lab đầu tiên;
- role claim trong token Keycloak hiện đang nằm ở đâu;
- Spring Security expression sẽ dùng `hasRole(...)` hay `hasAuthority(...)`.

## 5. Endpoint-level authorization TODO

Option A: cấu hình theo URL trong `SecurityFilterChain`.

Ví dụ mental model:

```text
/api/master-data/** requires ROLE_VIEWER
/api/search/** requires ROLE_VIEWER hoặc ROLE_ACCOUNTANT
/api/dev/** chỉ local/dev, không production
```

Option B: dùng annotation:

```java
@PreAuthorize("hasRole('VIEWER')")
```

Nếu dùng `@PreAuthorize`, cần enable method security.

## 6. Service/business authorization TODO

Service-level check phù hợp khi rule không chỉ phụ thuộc URL.

Ví dụ:

- chỉ `ADMIN` được gọi reindex endpoint;
- chỉ `ACCOUNTANT` được gọi một endpoint nghiệp vụ giả lập;
- vẫn phải query theo tenantId sau khi pass role.

Không nên đưa toàn bộ rule vào controller.

## 7. Test/verification plan

Manual HTTP cases:

- Token tenant 1 có role phù hợp -> `GET /api/master-data` trả `200`.
- Token tenant 2 có role phù hợp -> `200`, chỉ thấy tenant 2.
- Token hợp lệ nhưng thiếu role -> `403`.
- Missing token -> `401`.
- Invalid token -> `401`.
- Tenant 1 có role phù hợp gọi id tenant 2 -> `404` hoặc không lộ dữ liệu.

Automated test strategy:

- Giữ `DataLeakageTest` không phụ thuộc Keycloak live container nếu có thể.
- Nếu test RBAC bằng local JWT mode, đảm bảo token test có role claim tương đương.
- Keycloak live verification có thể để manual HTTP trước.

## 8. Common mistakes

- Nhầm `401` với `403`.
- Token có role trong Keycloak nhưng backend không map thành `GrantedAuthority`.
- Dùng `hasRole('VIEWER')` nhưng authority thực tế là `VIEWER` thay vì `ROLE_VIEWER`.
- Chỉ check role mà quên tenant-aware query.
- Nhận role/tenant từ request body.
- Làm role matrix quá lớn khi mini-lab chỉ cần 2-3 role.
- Bật requirement role làm vỡ `DataLeakageTest` mà chưa update token fixture.

## 9. Done criteria

- Có theory note và code guide rõ.
- Có token/user Keycloak chứa role claim.
- Backend map role claim thành authority.
- User có role phù hợp gọi endpoint được.
- User thiếu role nhận `403`.
- Missing/invalid token vẫn `401`.
- Tenant isolation vẫn pass: tenant 1 không đọc tenant 2.

## Nguồn tham khảo chuẩn

- [Spring Security - Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [Spring Security - JWT Authentication Converter](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Keycloak Server Administration Guide](https://www.keycloak.org/docs/latest/server_admin/)
