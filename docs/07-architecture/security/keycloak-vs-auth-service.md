# Keycloak vs Auth Service

## 1. General Anatomy

Trong sơ đồ kiến trúc backend, ô "Auth Service" thường có thể mang nhiều nghĩa:

- Identity Provider/IAM: quản lý realm, client, user, role, login flow và phát token.
- Authorization service: quyết định policy phức tạp ở một nơi riêng.
- Shared auth library/starter: code dùng lại để backend validate token, map role, đọc claims.
- API Gateway auth plugin: kiểm tra token ở gateway trước khi forward request.

Các khái niệm này dễ bị trộn lẫn, nhưng chúng không phải cùng một thứ.

## 2. Repo-Specific Mapping

Trong repo này, **Keycloak đã là Auth Service runtime**:

- Keycloak quản lý realm `viettel-lab`.
- Keycloak quản lý clients như `tenant-demo-api-client` và `tenant-demo-web`.
- Keycloak quản lý users `tenant1-user`, `tenant2-user`.
- Keycloak quản lý role `ADMIN`, `ACCOUNTANT`, `VIEWER`.
- Keycloak phát access token có `tenant_id` và role claims.

Hai backend service hiện tại là **Resource Server**:

- `tenant-demo` validate Bearer JWT bằng Spring Security Resource Server.
- `audit-log-service` validate Bearer JWT bằng Spring Security Resource Server.
- Cả hai đọc issuer/JWKS từ Keycloak.
- Cả hai tự enforce endpoint rules và tenant-aware data query.

Shared module mới:

- `lab-code/common-security`
- Đây là code library, không phải service chạy runtime.
- Nó gom phần plumbing bị duplicate: `TenantContext`, tenant claim filter, role converter.

## 3. Runtime Flow

```text
Browser / API client
  -> Keycloak login/token endpoint
  -> receives access token
  -> calls Kong with Authorization: Bearer <token>
  -> Kong forwards request
  -> target backend service validates JWT locally
  -> shared JwtTenantContextFilter extracts tenant_id from validated Jwt
  -> service/controller/repository applies tenant-aware rules
```

Backend service không gọi một custom `auth-service` trên từng request. Nó dùng Keycloak metadata/JWKS để validate token locally.

## 4. Why Not Create A Custom Runtime auth-service?

Không tạo thêm Spring Boot `auth-service` trong Phase 1.5 vì:

- Keycloak đã xử lý login/user/client/role/token issuance tốt hơn tự viết.
- Gọi network sang auth-service trên mỗi request tăng latency và điểm lỗi.
- Nếu auth-service down thì toàn bộ API dễ fail, dù token đã có thể validate bằng JWKS locally.
- Tự viết auth-service dễ dẫn đến tự implement OAuth2/OIDC sai.
- Bài học đúng hơn là hiểu Identity Provider + Resource Server + shared library khác nhau thế nào.

## 5. What common-security Should Contain

Nên chứa:

- Đọc `tenant_id` từ Jwt đã validate.
- Lưu/clear tenant context theo request.
- Convert Keycloak role claims sang Spring `ROLE_*`.
- Constants claim name/request attribute nhỏ.

Không nên chứa:

- Business authorization rules cụ thể của từng endpoint.
- JPA entity/repository/service business.
- Gateway route rules.
- Token issuance/login logic.
- Keycloak admin/bootstrap logic.

## 6. Common Mistakes

- Tạo custom auth-service rồi để mọi API call hỏi service đó token có hợp lệ không.
- Tin `tenantId` từ request body/query thay vì claim đã validate.
- Nghĩ gateway auth có thể thay backend authorization.
- Đưa endpoint rules của mọi service vào shared module.
- Để `ThreadLocal` tenant context không clear sau request.
- Tự viết lại Keycloak/OIDC thay vì dùng chuẩn.

## 7. Verification

Sau refactor shared module:

```bash
cd lab-code
make common-security-install
make app-test
make audit-log-validate
```

Kỳ vọng hành vi không đổi:

- Missing/invalid token -> `401`.
- Role không đủ -> `403`.
- `tenant_id` vẫn lấy từ JWT đã validate.
- Query vẫn filter theo tenant.
- `tenant-demo` và `audit-log-service` không share JPA entity/repository.
