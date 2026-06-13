# Common Security Code Walkthrough

## 1. General Anatomy

Một backend Resource Server dùng JWT/OIDC thường có các phần:

- JWT resource server config: validate signature, issuer, expiration bằng issuer/JWKS.
- Authorities converter: map claims thành Spring Security authorities.
- Tenant claim resolver: đọc tenant context từ claim đã validate.
- Request filter: đặt tenant vào request-scoped context rồi clear sau request.
- Endpoint rules: service nào, route nào, role nào được gọi.
- Business/service query: dùng tenant context để filter data.

Phần có thể dùng chung là plumbing. Phần endpoint rule và business rule nên ở lại từng service.

## 2. Repo-Specific Mapping

Shared module:

```text
lab-code/common-security/
  pom.xml
  src/main/java/com/viettel/common/security/
    SecurityConstants.java
    TenantContext.java
    TenantClaimResolver.java
    JwtTenantContextFilter.java
    KeycloakRoleConverter.java
    JwtAuthenticationConverters.java
```

`tenant-demo` dùng shared module qua:

- `lab-code/tenant-demo/pom.xml`
- `com.viettel.demo.security.SecurityConfig`
- business classes import `com.viettel.common.security.TenantContext`

`audit-log-service` dùng shared module qua:

- `lab-code/audit-log-service/pom.xml`
- `com.viettel.audit.security.SecurityConfig`
- audit service imports `com.viettel.common.security.TenantContext`

## 3. Class Walkthrough

### `SecurityConstants`

Giữ các tên dùng chung:

- `TENANT_ID_CLAIM = "tenant_id"`
- `TENANT_ID_REQUEST_ATTRIBUTE = "tenantId"`
- `ROLE_PREFIX = "ROLE_"`

Không đưa route path hoặc business constants vào đây.

### `TenantContext`

`TenantContext` dùng `ThreadLocal<Long>` để giữ tenant hiện tại trong một request.

Nó trả lời câu hỏi:

> Request hiện tại thuộc tenant nào?

Nó không trả lời:

> User có được gọi endpoint này không?

Điểm quan trọng: context phải được clear trong `finally` để tránh rò tenant khi server tái sử dụng thread.

### `TenantClaimResolver`

Đọc `tenant_id` từ `Jwt` đã được Spring Security validate.

Hỗ trợ:

- `tenant_id` dạng number.
- `tenant_id` dạng string parse được sang long.

Nếu claim thiếu/sai, filter trả `401`.

### `JwtTenantContextFilter`

Filter chạy sau `BearerTokenAuthenticationFilter`.

Flow:

```text
Spring Security validates Bearer JWT
  -> SecurityContext contains Jwt principal
  -> JwtTenantContextFilter reads tenant_id
  -> TenantContext.setCurrentTenant(...)
  -> request enters controller/service
  -> finally TenantContext.clear()
```

Filter không tự validate token và không tự map role.

### `KeycloakRoleConverter`

Đọc role từ các claim phổ biến trong lab:

- `roles` cho local JWT dev token.
- `realm_access.roles` cho Keycloak realm roles.
- `resource_access.<client-id>.roles` cho Keycloak client roles.

Sau đó map sang `ROLE_*` để dùng với `hasRole(...)`.

### `JwtAuthenticationConverters`

Factory nhỏ để tạo `JwtAuthenticationConverter` gồm:

- converter scope mặc định của Spring (`SCOPE_*`);
- converter role Keycloak/local (`ROLE_*`).

## 4. Runtime Flow

### tenant-demo

```text
Kong -> tenant-demo /api/master-data
  -> SecurityConfig validates JWT
  -> JwtAuthenticationConverters maps roles
  -> JwtTenantContextFilter sets TenantContext
  -> MasterDataService reads TenantContext
  -> repository query includes tenantId
```

`tenant-demo` vẫn giữ route rules riêng:

- `GET /api/master-data/**`: `ADMIN`, `ACCOUNTANT`, `VIEWER`.
- write master data: `ADMIN`, `ACCOUNTANT`.
- file/search rules vẫn ở service này.

### audit-log-service

```text
Kong -> audit-log-service /api/audit-events
  -> SecurityConfig validates JWT
  -> JwtAuthenticationConverters maps roles
  -> JwtTenantContextFilter sets TenantContext
  -> AuditEventService filters audit events by tenantId
```

`audit-log-service` vẫn giữ route rules riêng:

- `GET /api/audit-events/**`: `ADMIN`, `ACCOUNTANT`, `VIEWER`.

## 5. Maven Wiring

`common-security` là Maven jar local:

```text
com.viettel:common-security:0.0.1-SNAPSHOT
```

Vì repo chưa có parent multi-module Maven chung, local workflow cần install module trước khi build service riêng:

```bash
cd lab-code
make common-security-install
```

Các Makefile target chính như `make app-test`, `make app-run`, `make app-run-logs`, `make audit-log-validate`, `make audit-log-run` và `make audit-log-run-logs` đã phụ thuộc target này để workflow ít bị quên bước.

Java backend services hiện chạy Maven/IntelliJ trên host. Docker vẫn dùng cho infra/tooling, không còn là runtime chính của `audit-log-service`.

## 6. Common Mistakes

- Đưa endpoint-specific route rules vào `common-security`.
- Đưa `MasterDataService`, `AuditEventService`, entity hoặc repository vào shared module.
- Tin tenantId từ request body thay vì JWT claim.
- Không clear `TenantContext`.
- Quên install shared jar trước khi chạy service bằng Maven trực tiếp.
- Nghĩ shared module là một runtime service.
- Dùng shared module để né việc mỗi service tự validate JWT.

## 7. Verification

```bash
git diff --check
mvn -f lab-code/common-security/pom.xml validate
cd lab-code && make common-security-install
mvn -f lab-code/tenant-demo/pom.xml validate
mvn -f lab-code/audit-log-service/pom.xml validate
cd lab-code/tenant-demo && ./mvnw test
cd lab-code && make app-test
```

Behavior cần giữ nguyên:

- Missing token -> `401`.
- Invalid token -> `401`.
- Role không đủ -> `403`.
- Query không leak cross-tenant.
- Duplicate `master_data.code` cùng tenant vẫn trả `409`.
