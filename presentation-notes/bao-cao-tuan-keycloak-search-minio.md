# Route báo cáo tuần: Keycloak RBAC, Elasticsearch, MinIO

## 1. Mở đầu ngắn

Sau lần báo cáo trước, em tập trung đưa thêm các công nghệ trong target architecture vào demo backend tenant-aware hiện tại, theo hướng nhỏ nhưng chạy được thật. Base flow vẫn giữ nguyên: **Keycloak token -> Spring Security -> TenantContext -> Service/Repository tenant-aware query**. Em không mở rộng thành full microservices, mà học theo từng lát: authorization, search projection, file/object storage. Các phần mới đều có theory note, code guide, HTTP request mẫu và summary trong repo.

## 2. Thứ tự báo cáo đề xuất

### A. Keycloak Authorization / RBAC / tenant-scope

**Vấn đề giải quyết**

- Sau khi đã có login/token OIDC, cần biết user được phép làm gì.
- Phân biệt authentication, authorization và tenant isolation.

**Em đã học**

- `401` là thiếu/sai token; `403` là token hợp lệ nhưng thiếu quyền.
- Keycloak role có thể nằm ở `realm_access.roles` hoặc `resource_access.<client>.roles`.
- Spring Security cần map role claim thành `GrantedAuthority`.
- Role không thay thế tenant-aware query.

**Em đã implement**

- Custom role mapping từ Keycloak JWT sang `ROLE_*`.
- `SecurityConfig` phân quyền endpoint theo role.
- `JwtTenantContextFilter` vẫn chỉ đọc `tenant_id`, không trộn authorization.

**Files nên mở**

- `lab-code/tenant-demo/src/main/java/com/viettel/demo/security/SecurityConfig.java`
- `lab-code/tenant-demo/src/main/java/com/viettel/demo/security/KeycloakRoleConverter.java`
- `lab-code/tenant-demo/src/main/java/com/viettel/demo/security/JwtTenantContextFilter.java`
- `lab-code/tenant-demo/http/keycloak-authorization-api.http`
- `docs/05-security/keycloak-authorization-rbac-tenant-scope.md`

**Demo nhanh nếu cần**

- `role allowed -> 200`
- `no role -> 403`
- `missing/invalid token -> 401`
- `valid role but wrong tenant -> 404/no leakage`

**Caveat cần nói**

- Đây là RBAC nhỏ cho lab, chưa phải full permission matrix.
- Keycloak cấp identity/role; backend vẫn phải enforce business rule và tenant query.

**Mentor có thể hỏi**

- Vì sao không chỉ dựa vào role?
  - Vì role trả lời “được làm gì”, còn tenant query trả lời “được thấy dữ liệu tenant nào”.
- `hasRole("ADMIN")` khác `hasAuthority("ROLE_ADMIN")` thế nào?
  - `hasRole("ADMIN")` tự check authority `ROLE_ADMIN`.
- Tại sao `JwtTenantContextFilter` không map role?
  - Vì role mapping thuộc authentication/authorization converter; filter này chỉ bridge tenant context sau khi token đã validate.

### B. Elasticsearch / Search mini-lab

**Vấn đề giải quyết**

- PostgreSQL index tốt cho exact/prefix query, nhưng full-text/contains search và ranking cần search engine khi bài toán lớn hơn.

**Em đã học**

- Elasticsearch là search projection, không phải source of truth.
- Document, index, mapping, analyzer, inverted index, Query DSL.
- Query phải filter `tenantId` ngay trong Elasticsearch query.
- DB và search index có eventual consistency.

**Em đã implement**

- Search `master_data` qua Elasticsearch Java API Client.
- Reindex từ PostgreSQL sang Elasticsearch.
- Search keyword theo tenant, không leak tenant khác.
- Tách `Document` khỏi JPA Entity.

**Files nên mở**

- `lab-code/tenant-demo/src/main/java/com/viettel/demo/search/MasterDataSearchController.java`
- `lab-code/tenant-demo/src/main/java/com/viettel/demo/search/MasterDataSearchService.java`
- `lab-code/tenant-demo/src/main/java/com/viettel/demo/search/MasterDataSearchGateway.java`
- `lab-code/tenant-demo/src/main/java/com/viettel/demo/search/MasterDataSearchDocument.java`
- `lab-code/tenant-demo/http/search-api.http`
- `docs/07-architecture/search-elasticsearch/elasticsearch-design-patterns-spring-boot.md`
- `docs/07-architecture/search-elasticsearch/elasticsearch-request-response-shapes.md`

**Demo nhanh nếu cần**

- Elasticsearch health.
- Reindex `master_data`.
- Tenant 1 search keyword.
- Tenant 2 search same keyword.
- Tenant 1 search keyword chỉ có ở tenant 2 -> empty/no leakage.

**Caveat cần nói**

- Elasticsearch không thay PostgreSQL.
- Không filter tenant sau khi fetch kết quả; phải filter trong query.
- Reindex endpoint là lab/admin-only, không phải public production API.

**Mentor có thể hỏi**

- Vì sao không dùng PostgreSQL LIKE?
  - Với exact/prefix thì PostgreSQL có thể đủ; search engine hữu ích khi cần full-text, analyzer, ranking, fuzzy/contains search.
- Gateway khác Repository thế nào?
  - Repository truy cập source-of-truth DB; Gateway là adapter gọi external system Elasticsearch.
- Vì sao tách document khỏi entity?
  - Entity map schema DB; document là projection tối ưu cho search.

### C. MinIO / File storage mini-lab

**Vấn đề giải quyết**

- File/chứng từ/attachment không nên lưu binary trực tiếp trong PostgreSQL cho demo này.
- Cần object storage kiểu S3-compatible, còn metadata nghiệp vụ vẫn ở DB.

**Em đã học**

- Bucket, object, object key, content type, metadata, private bucket.
- Binary object nằm ở MinIO; PostgreSQL giữ metadata và tenant ownership.
- Backend sinh object key, client chỉ dùng `fileId`.

**Em đã implement**

- Upload/download/delete file qua backend.
- Metadata tenant-aware trong PostgreSQL.
- Binary object lưu trong MinIO private bucket.
- Cross-tenant download bị chặn bằng lookup `tenantId + fileId`.

**Files nên mở**

- `lab-code/tenant-demo/src/main/java/com/viettel/demo/storage/FileController.java`
- `lab-code/tenant-demo/src/main/java/com/viettel/demo/storage/FileStorageService.java`
- `lab-code/tenant-demo/src/main/java/com/viettel/demo/storage/FileStorageGateway.java`
- `lab-code/tenant-demo/src/main/java/com/viettel/demo/storage/MinioFileStorageGateway.java`
- `lab-code/tenant-demo/src/main/java/com/viettel/demo/entity/FileMetadata.java`
- `lab-code/tenant-demo/src/main/resources/db/migration/V5__create_file_metadata.sql`
- `lab-code/tenant-demo/http/file-storage-api.http`
- `docs/07-architecture/object-storage-minio/minio-code-guide-spring-boot.md`

**Demo nhanh nếu cần**

- MinIO health.
- Upload file tenant 1 -> `201`.
- Download own file tenant 1 -> `200`.
- Tenant 2 download tenant 1 file -> `404`.
- Missing/invalid token -> `401`.

**Caveat cần nói**

- Chưa làm presigned URL production flow.
- Consistency DB/MinIO hiện là best-effort cleanup, chưa phải distributed transaction.
- Không expose raw object key, access key/secret hoặc public bucket.

**Mentor có thể hỏi**

- Vì sao DB vẫn cần metadata?
  - Vì metadata có tenant ownership, audit, business query; MinIO chỉ giữ binary object.
- Vì sao cross-tenant trả 404?
  - Để không tiết lộ fileId đó có tồn tại ở tenant khác hay không.
- Vì sao không cho client tự gửi object key?
  - Vì dễ path/key injection và cross-tenant access; backend phải sinh key.

### D. Kế hoạch ngắn tiếp theo

- MinIO đã đủ để demo route upload/download tenant-aware; chỉ polish thêm nếu mentor yêu cầu.
- Tiếp theo: Redis/cache mini-lab.
- Sau Redis: Kafka/async messaging, rồi Observability/logging/metrics.

## 3. Demo checklist

### Chuẩn bị hạ tầng

```bash
cd lab-code
make infra-up
make infra-status
make app-test
```

### Chạy app

Kiểm tra `.env` local trước khi chạy, không commit token/secret:

```text
APP_AUTH_MODE=keycloak
APP_SEARCH_ENABLED=true
APP_FILE_STORAGE_ENABLED=true
```

```bash
cd lab-code
make app-run
```

Fallback nếu chỉ cần verify nhanh không phụ thuộc Keycloak role setup: dùng local JWT dev token trong HTTP files, nhưng nói rõ Keycloak mode mới là auth demo chính.

### HTTP files nên dùng

- Keycloak RBAC: `lab-code/tenant-demo/http/keycloak-authorization-api.http`
- Search: `lab-code/tenant-demo/http/search-api.http`
- File storage: `lab-code/tenant-demo/http/file-storage-api.http`

### Case nên chuẩn bị sẵn

- Keycloak RBAC:
  - allowed role -> `200`;
  - no role -> `403`;
  - missing/invalid token -> `401`;
  - wrong tenant -> không leak.
- Elasticsearch:
  - reindex;
  - tenant 1 search;
  - tenant 2 search;
  - same keyword nhưng result scoped theo tenant.
- MinIO:
  - upload;
  - download own file;
  - cross-tenant download blocked;
  - missing/invalid token blocked.

## 4. Files mở theo nhóm

### Security / Keycloak

- `SecurityConfig.java`
- `KeycloakRoleConverter.java`
- `JwtTenantContextFilter.java`
- `keycloak-authorization-api.http`
- `docs/05-security/keycloak-authorization-code-guide-spring-boot.md`

### Search

- `MasterDataSearchService.java`
- `MasterDataSearchGateway.java`
- `MasterDataSearchDocument.java`
- `search-api.http`
- `docs/07-architecture/search-elasticsearch/elasticsearch-design-patterns-spring-boot.md`

### File storage

- `FileStorageService.java`
- `MinioFileStorageGateway.java`
- `FileMetadata.java`
- `file-storage-api.http`
- `docs/07-architecture/object-storage-minio/minio-object-storage.md`

### Summary

- `ROADMAP.md`
- `docs/99-tong-ket/nhung-gi-da-nam-duoc.md`

## 5. Main technical takeaways

- RBAC trả lời “user được làm gì”, còn tenant-aware query bảo vệ “user được thấy dữ liệu tenant nào”.
- `401` khác `403`: thiếu/sai token khác với thiếu quyền.
- Elasticsearch là search projection; PostgreSQL vẫn là source of truth.
- Search phải filter theo `tenantId` trong query, không lọc sau khi fetch.
- MinIO lưu binary object; PostgreSQL lưu tenant-aware metadata.
- Backend phải authorize upload/download; client không được tự quyết định object key.
- Các mini-lab đều giữ default disabled/fallback để `make app-test` không phụ thuộc toàn bộ hạ tầng ngoài.

## 6. Nếu thiếu thời gian

Ưu tiên nói theo thứ tự:

1. Keycloak RBAC: 401/403 + role không thay tenant query.
2. Elasticsearch: search projection + tenant filter trong query.
3. MinIO: binary object ở MinIO + metadata tenant-aware ở PostgreSQL.

Mỗi phần chỉ cần mở 1 code file chính + 1 HTTP file. Không cần đọc hết theory doc trong buổi báo cáo.
