# Keycloak Authorization / RBAC / tenant-scope

## Vai trò tài liệu

Tài liệu này là phần nền tảng về authorization cho backend. Mục tiêu không chỉ là làm một mini-lab trong `tenant-demo`, mà là nắm được cách một backend dùng role/permission từ Keycloak một cách đúng phạm vi:

- Keycloak giúp phát token đã xác thực user.
- Spring Security validate token và chuyển role/scope thành authority.
- Backend quyết định endpoint/service nào được gọi.
- Tenant isolation vẫn phải được enforce ở service/repository bằng `tenantId`.

Đọc trước nếu còn mơ hồ về OIDC:

- `keycloak-oidc-mental-model.md`
- `oauth2-jwt-resource-server-concepts.md`
- `spring-security-core-concepts.md`
- `spring-boot-keycloak-integration-plan.md`

---

## 1. Authentication, authorization và tenant isolation

| Lớp | Câu hỏi trả lời | Ví dụ |
|---|---|---|
| Authentication/AuthN | Bạn là ai? | Token Keycloak hợp lệ, có `iss`, `sub`, `exp`. |
| Authorization/AuthZ | Bạn được phép làm gì? | User có role `ACCOUNTANT` được đọc API nghiệp vụ. |
| Tenant isolation | Bạn được phép thấy dữ liệu tenant nào? | Query repository luôn có `tenantId` từ `TenantContext`. |

Ba lớp này liên quan nhưng không thay thế nhau.

Ví dụ quan trọng:

- User có token hợp lệ nhưng thiếu role phù hợp -> `403`.
- User có role phù hợp nhưng token thuộc tenant 1 -> vẫn không được đọc record tenant 2.
- Role `ADMIN` không phải giấy phép bỏ qua tenant-aware query.

---

## 2. 401 vs 403

| Status | Ý nghĩa | Case thường gặp |
|---|---|---|
| `401 Unauthorized` | Request chưa authenticated hoặc token không hợp lệ. | Thiếu Bearer token, token sai chữ ký, token hết hạn, issuer sai. |
| `403 Forbidden` | Request đã authenticated nhưng không đủ quyền. | Token hợp lệ nhưng thiếu role/authority cần thiết. |

Trong mini-lab RBAC cần verify cả hai. Nếu mọi lỗi đều thành `401`, backend chưa phân biệt authentication với authorization. Nếu user thiếu role vẫn `200`, authorization chưa hoạt động.

---

## 3. RBAC là gì?

RBAC là Role-Based Access Control: backend gán quyền theo vai trò.

Ví dụ:

| Role | Ý nghĩa học tập | Ví dụ quyền |
|---|---|---|
| `VIEWER` | Người chỉ đọc | Được gọi `GET /api/master-data`. |
| `ACCOUNTANT` | Người thao tác nghiệp vụ kế toán | Được gọi các endpoint nghiệp vụ đọc/ghi sau này. |
| `ADMIN` | Người quản trị lab | Có thể gọi endpoint reindex/search admin nếu bật search mini-lab. |

Trong sản phẩm thật, role/permission có thể chi tiết hơn rất nhiều. Phase 1 chỉ cần 2-3 role để hiểu flow.

---

## 4. Realm roles và client roles trong Keycloak

### Realm roles

Realm role thuộc toàn realm. Role này có thể dùng chung cho nhiều client/app trong cùng realm.

Ví dụ:

```text
viettel-lab realm
├── role ADMIN
├── role ACCOUNTANT
└── role VIEWER
```

Ưu điểm: dễ tạo, dễ hiểu khi mới học.

Nhược điểm: nếu hệ thống có nhiều service, role realm quá chung có thể khó quản lý.

### Client roles

Client role thuộc một client cụ thể. Với kiến trúc nhiều backend services, client role thường hợp lý hơn vì role gắn với phạm vi của một app/API.

Ví dụ:

```text
client tenant-demo-api-client
├── role ADMIN
├── role ACCOUNTANT
└── role VIEWER
```

Ưu điểm: sát hơn với mô hình service/API riêng.

Nhược điểm: token claim nằm sâu hơn trong `resource_access.<client-id>.roles`, backend phải map cẩn thận.

### Khuyến nghị cho repo này

Mini-lab nên ưu tiên **client roles** trên `tenant-demo-api-client`, vì `tenant-demo` đang đóng vai trò một backend API/resource server. Nếu lúc thao tác Admin Console bị chậm, có thể dùng realm roles làm bước học đầu tiên, nhưng code guide nên biết cách đọc cả hai để dễ so sánh.

---

## 5. Scopes, roles và authorities

Ba khái niệm này hay bị lẫn:

| Khái niệm | Thuộc đâu | Dùng để làm gì |
|---|---|---|
| Scope | OAuth2/OIDC token/client consent | Diễn tả phạm vi truy cập dạng `read`, `profile`, `email`. |
| Role | Keycloak/user/app model | Diễn tả vai trò như `ADMIN`, `ACCOUNTANT`. |
| Authority | Spring Security runtime | Chuẩn nội bộ để Spring kiểm tra quyền request hiện tại. |

Spring Security không tự đoán mọi Keycloak role thành authority đúng ý mình. Backend thường cần converter:

```text
Keycloak token claim
-> JwtAuthenticationConverter
-> GrantedAuthority
-> hasRole(...) / hasAuthority(...) / @PreAuthorize(...)
```

---

## 6. Role claims trong Keycloak access token

Keycloak có thể đưa roles vào token ở các claim phổ biến:

```json
{
  "realm_access": {
    "roles": ["ACCOUNTANT"]
  },
  "resource_access": {
    "tenant-demo-api-client": {
      "roles": ["VIEWER"]
    }
  },
  "scope": "openid profile email"
}
```

Ý nghĩa:

- `realm_access.roles`: realm roles của user.
- `resource_access.<client-id>.roles`: client roles của user cho client đó.
- `scope`: OAuth2 scopes, không tự đồng nghĩa với business role.

Trong mini-lab, token cần có ít nhất một role dễ kiểm tra, ví dụ `VIEWER` hoặc `ACCOUNTANT`.

---

## 7. Spring Security concepts cần biết

| Concept/class | Vai trò |
|---|---|
| `Authentication` | Đại diện cho principal đã authenticated và authorities của request hiện tại. |
| `GrantedAuthority` | Một quyền/authority Spring dùng để kiểm tra access, ví dụ `ROLE_VIEWER`. |
| `JwtAuthenticationToken` | `Authentication` dành cho JWT resource server; principal thường là `Jwt`. |
| `JwtAuthenticationConverter` | Chuyển `Jwt` đã validate thành `Authentication`. |
| `JwtGrantedAuthoritiesConverter` | Converter mặc định/tiện ích để đọc scopes/claim thành authorities. |
| `SecurityContext` | Nơi Spring Security lưu `Authentication` của request hiện tại. |

Flow học tập:

```text
Bearer token
-> JwtDecoder validate token
-> JwtAuthenticationConverter tạo Authentication
-> SecurityContext lưu Authentication
-> Authorization rule đọc GrantedAuthority
```

---

## 8. Endpoint-level authorization

Endpoint-level authorization kiểm tra quyền theo URL/HTTP request.

Ví dụ mental model:

```text
GET  /api/master-data/**             requires ROLE_VIEWER hoặc ROLE_ACCOUNTANT
POST /api/search/master-data/reindex requires ROLE_ADMIN
```

Có thể cấu hình trong `SecurityFilterChain`.

Ưu điểm:

- Dễ nhìn ở một nơi.
- Phù hợp với rule đơn giản theo URL.

Giới hạn:

- Khó diễn tả rule phụ thuộc dữ liệu nghiệp vụ.
- Nếu controller có nhiều hành động khác nhau dưới cùng URL pattern, config có thể rối.

---

## 9. Method-level authorization với `@PreAuthorize`

`@PreAuthorize` kiểm tra quyền trước khi method được gọi. Để dùng annotation này, app cần bật method security bằng `@EnableMethodSecurity`.

Ví dụ mental model:

```java
@PreAuthorize("hasRole('VIEWER')")
public List<MasterData> getAll() {
    ...
}
```

Ưu điểm:

- Rule nằm gần use case cần bảo vệ.
- Dễ áp dụng cho service method hoặc controller method cụ thể.

Giới hạn:

- Nếu lạm dụng, rule phân tán nhiều nơi.
- Vẫn không thay thế tenant-aware repository query.

---

## 10. Service/business authorization

Không phải mọi authorization rule đều nên đặt ở URL.

Service/business authorization phù hợp khi rule phụ thuộc:

- trạng thái bản ghi;
- tenant ownership;
- vai trò trong nghiệp vụ;
- hành động cụ thể như approve, close, reindex, export.

Ví dụ:

```text
User có ROLE_ACCOUNTANT
-> được đọc dữ liệu kế toán trong tenant hiện tại
-> nhưng vẫn chỉ query records có tenant_id = TenantContext.currentTenant
```

Rule service nên rõ ràng, không dựa vào frontend tự ẩn nút.

---

## 11. Tenant-scope authorization

Tenant-scope trả lời câu hỏi: request hiện tại đang thao tác trong phạm vi tenant nào?

Trong repo hiện tại:

```text
Keycloak token đã validate
-> claim tenant_id
-> JwtTenantContextFilter
-> TenantContext
-> service/repository query theo tenantId
```

RBAC chỉ bổ sung câu hỏi “được phép làm gì”. Tenant context trả lời “trong tenant nào”. Repository query trả lời “chỉ lấy data tenant đó”.

Rule cần nhớ:

- Không nhận `tenant_id` từ request body.
- Không query business data bằng method thiếu `tenantId`.
- Không nghĩ role `ADMIN` tự động được đọc mọi tenant.
- Nếu sau này có super-admin cross-tenant, phải thiết kế endpoint/audit riêng, không lẫn với flow tenant thường.

---

## 12. Ba hướng thiết kế authorization

### Option 1: Keycloak roles + Spring Security authority mapping

Backend đọc role claim từ Keycloak token và map thành `GrantedAuthority`.

Phù hợp khi:

- cần mini-lab rõ ràng;
- rule còn đơn giản;
- muốn học Spring Security chuẩn;
- chưa cần policy engine.

Đây là hướng chọn cho Phase 1.

### Option 2: Backend custom authorization service

Backend tự quản lý permission/role/business rules trong database hoặc service riêng.

Phù hợp khi:

- permission phụ thuộc sâu vào domain;
- cần audit, approval, data ownership phức tạp;
- Keycloak chỉ làm identity provider, còn authorization thuộc domain.

Để sau Phase 1 hoặc khi có domain thật hơn.

### Option 3: Keycloak Authorization Services / UMA / policies

Keycloak quản lý resource, scope, policy và permission chi tiết.

Phù hợp khi:

- cần centralized policy engine;
- có nhu cầu fine-grained authorization chuẩn hóa;
- team đã đủ quen Keycloak.

Chưa chọn cho mini-lab vì scope lớn và dễ lệch khỏi mục tiêu backend tenant-aware.

---

## 13. Recommended approach cho mini-lab này

1. Dùng Keycloak client roles trên `tenant-demo-api-client`:
   - `VIEWER`
   - `ACCOUNTANT`
   - `ADMIN`
2. Đảm bảo access token có role claim.
3. Trong Spring Boot, tạo converter đọc role từ:
   - `resource_access.tenant-demo-api-client.roles`
   - optional fallback `realm_access.roles`
4. Prefix thành authority:
   - `ROLE_VIEWER`
   - `ROLE_ACCOUNTANT`
   - `ROLE_ADMIN`
5. Dùng authorization check nhỏ:
   - read endpoint cần `VIEWER` hoặc `ACCOUNTANT`;
   - reindex/admin endpoint nếu dùng thì cần `ADMIN`.
6. Không đổi `TenantContext` và tenant-aware repository flow.

---

## 14. Mini-lab cases tối thiểu

| Case | Expected |
|---|---|
| Missing token | `401` |
| Invalid token | `401` |
| Token hợp lệ + role được phép | `200` |
| Token hợp lệ + thiếu role | `403` |
| Token tenant 1 + role đúng + gọi id tenant 2 | `404` hoặc không lộ dữ liệu |
| Query by code/search | vẫn scoped theo tenant hiện tại |

Optional nếu search mini-lab đang bật:

- `ADMIN` được gọi reindex.
- `VIEWER` đọc search được nhưng không reindex.

---

## 15. Không overdo ở Phase 1

- Chưa cần full ERP permission matrix.
- Chưa cần Keycloak Authorization Services/UMA.
- Chưa cần ABAC phức tạp.
- Chưa cần authorization service riêng.
- Chưa cần React login/PKCE trong task này.
- Chưa cần production governance cho roles.

---

## Nguồn tham khảo chuẩn

- [Keycloak Server Administration Guide - Roles](https://www.keycloak.org/docs/latest/server_admin/)
- [Spring Security - OAuth2 Resource Server JWT](https://docs.spring.io/spring-security/reference/6.5/servlet/oauth2/resource-server/jwt.html)
- [Spring Security - Authorization](https://docs.spring.io/spring-security/reference/servlet/authorization/index.html)
- [Spring Security - Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
