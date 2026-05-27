# Keycloak Authorization code guide cho Spring Boot tenant-demo

## Vai trò tài liệu

Đây là code guide cho mini-lab Keycloak Authorization/RBAC. Tài liệu này giải thích cách code, nhưng không tự thay bạn implement toàn bộ. Đọc theory trước:

- `keycloak-authorization-rbac-tenant-scope.md`
- `spring-security-core-concepts.md`
- `spring-boot-keycloak-integration-plan.md`

Mục tiêu code của mini-lab:

```text
Keycloak role claim
-> Spring Security GrantedAuthority
-> endpoint/service authorization
-> TenantContext vẫn lấy tenant_id từ token đã validate
-> repository vẫn query tenant-aware
```

---

## 1. Flow thực thi chi tiết

```text
HTTP request
-> Authorization: Bearer <access_token>
-> BearerTokenAuthenticationFilter lấy Bearer token
-> JwtDecoder validate chữ ký, issuer, expiration bằng issuer/JWKS
-> JwtAuthenticationConverter map Jwt sang Authentication + GrantedAuthority
-> SecurityContext lưu Authentication cho request hiện tại
-> JwtTenantContextFilter đọc tenant_id từ Jwt đã validate
-> URL authorization hoặc @PreAuthorize kiểm tra role/authority
-> Controller gọi Service
-> Service có thể check business authorization nếu cần
-> Repository query bằng tenantId từ TenantContext
```

Điểm quan trọng: `JwtTenantContextFilter` không nên tự parse role. Nó chỉ bridge `tenant_id` sang `TenantContext`.

---

## 2. Spring Security class/interface map

| Class/interface | Vai trò trong flow |
|---|---|
| `SecurityFilterChain` | Khai báo filter/security rules cho HTTP request: public/protected endpoints, stateless, resource server, custom filter. |
| `BearerTokenAuthenticationFilter` | Filter của Spring Security lấy Bearer token từ header `Authorization`. |
| `JwtDecoder` | Validate JWT: chữ ký, expiration, issuer. Với Keycloak thường dùng issuer-uri/JWKS. |
| `Jwt` | Object đại diện JWT đã decode/validate; chứa claims như `sub`, `tenant_id`, `realm_access`. |
| `JwtAuthenticationConverter` | Chuyển `Jwt` thành `Authentication`. Có thể cấu hình converter đọc roles. |
| `JwtAuthenticationToken` | Một dạng `Authentication` cho JWT; principal thường là `Jwt`, authorities nằm trong object này. |
| `GrantedAuthority` | Quyền/authority mà Spring dùng để check `hasRole`, `hasAuthority`, `@PreAuthorize`. |
| `SecurityContext` | Nơi lưu `Authentication` của request hiện tại. |
| `@PreAuthorize` | Annotation method-level authorization, kiểm tra trước khi method được gọi. |

---

## 3. Vì sao Keycloak roles không tự thành `ROLE_*`

Spring Security Resource Server mặc định thường đọc scopes theo dạng authority như:

```text
SCOPE_openid
SCOPE_profile
```

Trong khi Keycloak roles thường nằm ở:

```text
realm_access.roles
resource_access.<client-id>.roles
```

Vì vậy backend cần converter riêng nếu muốn dùng:

```java
@PreAuthorize("hasRole('VIEWER')")
```

`hasRole('VIEWER')` thực tế tìm authority `ROLE_VIEWER`. Nếu converter chỉ tạo authority `VIEWER`, expression này sẽ fail. Khi đó hoặc đổi sang `hasAuthority('VIEWER')`, hoặc chuẩn hóa converter thành `ROLE_VIEWER`.

Khuyến nghị cho repo này: map role thành `ROLE_<ROLE_NAME>` để dùng `hasRole(...)` dễ đọc.

---

## 4. Đọc realm roles và client roles

### Realm roles

Claim shape:

```json
{
  "realm_access": {
    "roles": ["ACCOUNTANT", "VIEWER"]
  }
}
```

Pseudo-code:

```text
realmAccess = jwt.getClaim("realm_access")
roles = realmAccess["roles"]
for each role -> SimpleGrantedAuthority("ROLE_" + role)
```

### Client roles

Claim shape:

```json
{
  "resource_access": {
    "tenant-demo-api-client": {
      "roles": ["VIEWER"]
    }
  }
}
```

Pseudo-code:

```text
resourceAccess = jwt.getClaim("resource_access")
clientAccess = resourceAccess["tenant-demo-api-client"]
roles = clientAccess["roles"]
for each role -> SimpleGrantedAuthority("ROLE_" + role)
```

Khuyến nghị mini-lab:

- ưu tiên client roles của `tenant-demo-api-client`;
- optional fallback realm roles để dễ debug;
- tránh duplicate authority bằng `Set`.

---

## 5. `JwtAuthenticationConverter` nên đặt ở đâu?

Giữ `SecurityConfig` sạch:

- `SecurityConfig` khai báo `SecurityFilterChain`, `JwtDecoder`, và bean converter.
- Logic đọc role claim nên nằm trong class nhỏ, ví dụ `KeycloakRoleConverter`.
- `JwtTenantContextFilter` không đọc role.

Hình dạng hợp lý:

```text
SecurityConfig
├── SecurityFilterChain
├── JwtDecoder
└── JwtAuthenticationConverter bean

KeycloakRoleConverter
└── extract realm/client roles -> GrantedAuthority
```

Trong `oauth2ResourceServer`:

```text
oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(...))
```

Không cần làm generic framework. Chỉ cần converter rõ, test được, dễ giải thích.

---

## 6. `hasRole` vs `hasAuthority`

| Expression | Spring kiểm tra authority nào | Khi dùng |
|---|---|---|
| `hasRole('VIEWER')` | `ROLE_VIEWER` | Khi converter prefix role bằng `ROLE_`. |
| `hasAuthority('ROLE_VIEWER')` | `ROLE_VIEWER` | Khi muốn explicit authority đầy đủ. |
| `hasAuthority('SCOPE_read')` | `SCOPE_read` | Khi check OAuth2 scope. |

Khuyến nghị:

- Dùng `hasRole('VIEWER')` cho business roles.
- Dùng `hasAuthority('SCOPE_x')` nếu sau này check scope.
- Đừng trộn `VIEWER`, `ROLE_VIEWER`, `SCOPE_VIEWER` tùy tiện.

---

## 7. Bật method security với `@EnableMethodSecurity`

Nếu dùng `@PreAuthorize`, cần bật method security:

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    ...
}
```

`@PreAuthorize` được đánh giá trước khi method chạy. Có thể đặt ở:

- controller method: dễ thấy endpoint nào bị chặn;
- service method: gần business use case hơn.

Khuyến nghị mini-lab:

- bắt đầu với controller/service rất nhỏ;
- nếu bảo vệ reindex/search admin, đặt rule rõ ở controller hoặc service;
- business rule phức tạp hơn thì để service.

---

## 8. URL-level vs method-level vs service/business check

| Cách check | Nơi đặt | Phù hợp với |
|---|---|---|
| URL-level | `SecurityFilterChain` | Rule đơn giản theo path/method. |
| Method-level | `@PreAuthorize` | Rule rõ trên từng use case. |
| Service/business check | Service code | Rule phụ thuộc data/business state. |

Ví dụ áp dụng repo:

- `GET /api/master-data/**`: cần user authenticated và có role đọc.
- `POST /api/search/master-data/reindex`: nếu dùng, nên cần `ADMIN`.
- Cross-tenant id: không giải bằng role, mà giải bằng repository query theo tenantId.

### Implementation hiện tại trong repo

Code hiện tại chọn cách nhỏ nhất cho mini-lab:

- `KeycloakRoleConverter` đọc `roles`, `realm_access.roles` và `resource_access.<client-id>.roles`.
- `SecurityConfig` dùng `JwtAuthenticationConverter` để giữ scope mặc định dạng `SCOPE_*` và thêm Keycloak/local roles dạng `ROLE_*`.
- RBAC rule hiện đặt ở URL-level trong `SecurityFilterChain` khi `APP_AUTH_MODE=keycloak`.
- Local JWT mode vẫn chỉ yêu cầu authenticated để `DataLeakageTest` không phụ thuộc Keycloak live.
- `JwtTenantContextFilter` không đọc role; filter này chỉ đọc `tenant_id` từ `Jwt` đã validate.

Kết quả cần nhớ:

```text
Missing/invalid token -> 401
Valid token nhưng thiếu role -> 403
Valid role nhưng sai tenant -> vẫn 404/không lộ data
```

---

## 9. Gợi ý thứ tự tự code

1. Trong Keycloak, tạo roles:
   - `VIEWER`
   - `ACCOUNTANT`
   - `ADMIN`
2. Assign role cho user:
   - `tenant1-user`: `ACCOUNTANT` hoặc `VIEWER`
   - `tenant2-user`: role khác để test
   - một user thiếu role nếu muốn test `403`
3. Lấy token và decode local để xem role nằm ở đâu:
   - `realm_access.roles`
   - hay `resource_access.tenant-demo-api-client.roles`
4. Implement converter:
   - đọc role claim;
   - map sang `ROLE_*`;
   - return `Collection<GrantedAuthority>`.
5. Wire converter vào `SecurityConfig`.
6. Bật `@EnableMethodSecurity` nếu dùng `@PreAuthorize`.
7. Thêm authorization rule nhỏ.
8. Verify HTTP cases.
9. Sau khi chạy được, nhờ Codex review.

---

## 10. Skeleton code mental model

Không copy/paste mù quáng. Đây là hình dạng cần tự implement:

```text
class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    convert(jwt):
        authorities = empty set
        add roles from resource_access[clientId].roles
        add roles from realm_access.roles if chosen
        return authorities mapped to ROLE_*
}
```

Config idea:

```text
app.auth.keycloak.client-id = tenant-demo-api-client
```

Nếu chưa muốn thêm config mới, có thể hardcode `tenant-demo-api-client` tạm trong converter cho mini-lab, nhưng tốt hơn là đưa vào `AuthProperties` để giải thích được.

---

## 11. Test strategy

Không đổi ý nghĩa của `DataLeakageTest`.

`DataLeakageTest` hiện kiểm tra tenant isolation:

- tenant 1 không thấy tenant 2;
- missing/invalid token `401`;
- query by code vẫn scoped.

Authorization test nên tách riêng, ví dụ:

```text
AuthorizationTest
├── user_with_required_role_should_get_200
├── user_without_required_role_should_get_403
├── missing_token_should_get_401
└── valid_role_still_cannot_access_other_tenant_id
```

Nếu dùng local JWT mode cho automated test, dev token cần có role claim tương đương. Không ép test tự động phụ thuộc live Keycloak container ở giai đoạn đầu.

---

## 12. HTTP verification checklist

Manual cases:

| Case | Expected |
|---|---|
| Token `tenant1-user` có role được phép gọi `GET /api/master-data` | `200` |
| Token `tenant2-user` có role được phép | `200`, chỉ tenant 2 |
| Token hợp lệ nhưng thiếu role | `403` |
| Missing token | `401` |
| Invalid token | `401` |
| Token tenant 1 có role đúng gọi id tenant 2 | `404` hoặc không lộ dữ liệu |
| Optional `VIEWER` gọi reindex admin | `403` |
| Optional `ADMIN` gọi reindex admin | `200` nếu endpoint/search enabled |

---

## 13. Common mistakes

- Nhầm `401` với `403`.
- Tạo role trong Keycloak nhưng token không chứa role claim.
- Token có role nhưng Spring Security chưa map thành `GrantedAuthority`.
- Dùng `hasRole('VIEWER')` nhưng authority thực tế là `VIEWER`.
- Map role trong `JwtTenantContextFilter`, làm filter này quá nhiều trách nhiệm.
- Check role rồi quên tenant-aware query.
- Dùng `tenant_id` từ request body.
- Bật role requirement làm vỡ test local JWT vì token fixture chưa có role.
- Làm role matrix phức tạp khi mini-lab chỉ cần 2-3 roles.

---

## 14. Done criteria

- Keycloak token có role claim rõ ràng.
- Spring Boot map role claim thành `GrantedAuthority`.
- User có role phù hợp gọi endpoint được.
- User thiếu role nhận `403`.
- Missing/invalid token vẫn `401`.
- Tenant isolation vẫn pass: user tenant 1 không đọc được tenant 2.
- `make app-test` vẫn pass với local JWT fallback hoặc test config rõ.

---

## Nguồn tham khảo chuẩn

- [Spring Security - OAuth2 Resource Server JWT](https://docs.spring.io/spring-security/reference/6.5/servlet/oauth2/resource-server/jwt.html)
- [Spring Security - Authorization](https://docs.spring.io/spring-security/reference/servlet/authorization/index.html)
- [Spring Security - Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [Keycloak Server Administration Guide](https://www.keycloak.org/docs/latest/server_admin/)
