# Báo cáo tiến độ Phase 1 - cập nhật sau Keycloak/Search

**Người báo cáo:** NQD  
**Thời điểm:** 25/05/2026  
**Repo:** `erp-accounting-lab-phase1-viettel`

Báo cáo này cập nhật các phần em đã học/thực hành thêm từ sau phần SaaS/multi-tenant.

## 1. Tóm tắt ngắn

- Em đã hoàn thành phần PostgreSQL multi-tenant nâng cao: tenant-aware schema, data leakage proof, EXPLAIN/index, query-pattern, Flyway failure/rollback và ACID/isolation.
- Em đã có Spring Boot tenant-aware API cho lát cắt `master_data`, dùng PostgreSQL + Flyway và repository/service query theo `tenantId`.
- Em đã có `DataLeakageTest` để regression test các case tenant 1/tenant 2, missing/invalid token và cross-tenant access.
- Em đã chuyển từ JWT tạm sang Keycloak/OIDC local flow: Keycloak phát token, Spring Boot validate bằng issuer/JWKS, backend đọc `tenant_id` claim đã validate.
- Em đã bổ sung Elasticsearch/search mini-lab: PostgreSQL là source of truth, Elasticsearch là search projection và search query vẫn filter theo tenant.
- Hướng tiếp theo là học Keycloak Authorization/RBAC/tenant-scope trước, sau đó mới tiếp tục MinIO, Redis, Kafka và Observability.

## 2. Các phần đã học/thực hành thêm

### A. PostgreSQL/database

- Thiết kế shared-table multi-tenant với `tenant_id`.
- Dùng `UNIQUE (tenant_id, code)` để business code chỉ cần unique trong phạm vi từng tenant.
- Chứng minh query thiếu `tenant_id` có thể leak dữ liệu tenant khác.
- Thực hành `EXPLAIN ANALYZE`, đọc các plan cơ bản như `Seq Scan`, `Index Scan`, `Bitmap Index Scan`, `Bitmap Heap Scan`.
- Đào sâu index query-pattern: prefix `LIKE`, contains search, expression index, composite index và leftmost prefix.
- Học Flyway migration/failure/rollback: `flyway_schema_history`, `validate`, `repair`, transaction behavior trên PostgreSQL.
- Học ACID/isolation levels: `READ COMMITTED`, `REPEATABLE READ`, `SERIALIZABLE`, transaction visibility và lock cơ bản.

### B. Spring Boot tenant-aware backend

- Tạo Spring Boot app với Maven, PostgreSQL, Flyway và cấu hình môi trường local.
- Tự triển khai flow `TenantContext -> Service -> Repository -> PostgreSQL`.
- Tạo entity/repository/service/controller cho `master_data`.
- Repository methods đều scoped theo `tenantId`, tránh các method thiếu tenant như `findByCode(...)`.
- Viết `DataLeakageTest` bằng Spring Boot Test/MockMvc để giữ lại các case chống leakage.

Các phần này đều có source/lab trong repo; đường verify chính hiện tại là `cd lab-code && make app-test`.

### C. Security: JWT tạm + Keycloak/OIDC

- JWT tạm giúp em hiểu flow Bearer token, Spring Security Resource Server, `SecurityContext` và claim `tenant_id`.
- Backend chỉ set `TenantContext` sau khi token đã được validate.
- Keycloak/OIDC mini-lab đã chạy local: realm/client/user, token endpoint, issuer/JWKS và custom claim `tenant_id`.
- `tenant-demo` chạy được với `APP_AUTH_MODE=keycloak`.
- Token tenant 1/tenant 2 gọi API chỉ thấy dữ liệu đúng tenant; missing/invalid token trả `401`.
- Local JWT mode vẫn được giữ làm fallback cho test tự động, để test không phụ thuộc container Keycloak.

Điểm còn thiếu ở security là Authorization/RBAC: hiện em đã học xác thực token và tenant context, nhưng chưa hoàn thiện phần user có quyền gì trên endpoint/service.

### D. Search/architecture: Elasticsearch + target map

- Em đã tạo target architecture adoption map để nối lab hiện tại với các phần trong kiến trúc rộng hơn: Keycloak, Gateway, PostgreSQL, Redis, Kafka, MinIO, Elasticsearch, Observability.
- Elasticsearch mini-lab chỉ search trên `master_data`, giữ scope nhỏ.
- PostgreSQL vẫn là source of truth; Elasticsearch chỉ là search projection.
- Search document có `tenantId`; search query bắt buộc filter theo tenant hiện tại.
- App dùng official Elasticsearch Java API Client và giữ `APP_SEARCH_ENABLED=false` làm default an toàn.

## 3. Bài học kỹ thuật chính

- Tenant isolation phải nằm ở backend query/service/repository, không dựa vào frontend, Swagger hoặc request body.
- Token/authentication chỉ trả lời “ai đang gọi”; authorization và tenant-aware query vẫn là trách nhiệm của backend.
- Index phải đi cùng query pattern; có index không đồng nghĩa planner sẽ dùng index đúng như mình tưởng.
- Migration cần có failure/rollback mindset, đặc biệt với shared-table nhiều tenant.
- Elasticsearch phù hợp cho search projection, nhưng không thay thế PostgreSQL source of truth và không thay thế tenant filter.
- Automated test chống leakage giúp bảo vệ flow khi thay đổi filter/security/API.

## 4. Kế hoạch tuần tới

Theo feedback mới của anh Đạt, em sẽ ưu tiên Keycloak Authorization/RBAC/tenant-scope trước khi đi tiếp các công nghệ khác.

Trọng tâm:

- Phân biệt rõ `401 Unauthorized` và `403 Forbidden`.
- Học realm roles vs client roles, scopes/claims và Spring Security authorities.
- Mapping role claim trong Keycloak token sang `GrantedAuthority`.
- Thử endpoint-level authorization bằng `@PreAuthorize` hoặc cấu hình tương đương.
- Thử service-level/business authorization ở mức nhỏ.
- Giữ rule quan trọng: user có role hợp lệ vẫn chỉ được đọc dữ liệu đúng tenant; role không thay thế `tenantId` filter.

Sau phần này, em sẽ tiếp tục các mini-lab còn lại theo thứ tự: MinIO/file storage, Redis/cache, Kafka/async messaging và Observability.
