# Security Architecture Notes

Folder này giải thích phần auth/security dùng chung sau khi repo có nhiều backend service.

## Reading Order

1. [keycloak-vs-auth-service.md](keycloak-vs-auth-service.md) - vì sao không tạo custom runtime `auth-service` trong repo này.
2. [common-security-code-walkthrough.md](common-security-code-walkthrough.md) - walkthrough module shared `lab-code/common-security`.
3. [keycloak-custom-login-theme-walkthrough.md](keycloak-custom-login-theme-walkthrough.md) - theme `master-data-portal` cho trang login Keycloak local.

## Trạng Thái Lab

- Keycloak là Identity Provider/Auth Service runtime cho local demo.
- Keycloak login page đã có theme local `master-data-portal`.
- `tenant-demo`, `audit-log-service`, `file-service` và `search-service` là Resource Server: mỗi service tự validate JWT bằng issuer/JWKS.
- `lab-code/common-security` là shared code module để giảm duplicated security plumbing.
- Endpoint authorization rules vẫn nằm trong từng service.

## Caveats

- Shared module tạo coupling ở code level; chấp nhận được trong learning monorepo.
- Không đưa business rule, JPA entity, repository hoặc route rule vào `common-security`.
- Không gọi network sang một custom auth service cho mỗi API request.
