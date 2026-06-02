# Báo cáo tiến độ tuần - Keycloak Authorization, Elasticsearch, MinIO

**Người báo cáo:** NQD  
**Thời điểm:** 30/05/2026  
**Repo:** `erp-accounting-lab-phase1-viettel`

## 1. Tóm tắt ngắn

- Em đã bổ sung phần Keycloak Authorization/RBAC để tách rõ authentication, authorization và tenant isolation.
- Em đã hoàn thành Elasticsearch/search mini-lab trên lát cắt `master_data`, giữ PostgreSQL là source of truth và Elasticsearch là search projection.
- Em đã hoàn thành MinIO/file storage cơ bản: upload, download, delete file thông qua backend, metadata vẫn tenant-aware.
- Các phần mới đều bám theo flow nền: Keycloak token -> Spring Security -> `TenantContext` -> Service/Repository tenant-aware.
- Hướng tiếp theo là Redis/cache mini-lab; hiện đã có docs/setup/skeleton, phần cache-aside logic sẽ tự code tiếp.

## 2. Các nội dung đã học và triển khai

### A. Keycloak Authorization/RBAC/tenant-scope

Phần trước em đã làm Keycloak/OIDC để xác thực token. Tuần này em bổ sung lớp phân quyền:

- Phân biệt rõ:
  - Authentication/AuthN: user là ai, token có hợp lệ không.
  - Authorization/AuthZ: user được phép gọi chức năng nào.
  - Tenant isolation: user chỉ được thấy dữ liệu thuộc tenant nào.
- Hiểu và verify khác biệt `401` và `403`:
  - thiếu/sai token -> `401`;
  - token hợp lệ nhưng thiếu role -> `403`.
- Học realm roles, client roles và cách role xuất hiện trong Keycloak access token.
- Mapping Keycloak role claim sang Spring Security `GrantedAuthority` qua `KeycloakRoleConverter`.
- Giữ `JwtTenantContextFilter` chỉ làm việc tenant context, không trộn role/authorization logic vào filter này.
- Role check không thay thế tenant-aware query: user có role đúng vẫn không được đọc dữ liệu tenant khác.

Các file chính có thể xem nhanh: `SecurityConfig`, `KeycloakRoleConverter`, `JwtTenantContextFilter`, và HTTP cases trong `keycloak-authorization-api.http`.

### B. Elasticsearch/search mini-lab

Sau phần PostgreSQL index/query-pattern, em làm tiếp Elasticsearch để hiểu khi nào cần search engine thay vì chỉ dùng SQL `LIKE`.

- Học các khái niệm nền: index, document, mapping, field type, analyzer, inverted index, Query DSL, search response và error shape.
- Dùng official Elasticsearch Java API Client thay vì tự ghép JSON raw HTTP trong code chính.
- Thiết kế theo Gateway/Adapter:
  - Controller mỏng;
  - Service giữ intent tenant-aware;
  - Gateway giữ chi tiết Elasticsearch request/response;
  - Document DTO tách khỏi JPA Entity.
- PostgreSQL vẫn là source of truth; Elasticsearch chỉ là projection phục vụ search và có thể stale.
- Search document có `tenantId`; search query bắt buộc filter theo `tenantId`, không lọc tenant sau khi đã lấy kết quả.
- Đã verify reindex và search theo tenant 1/tenant 2, không thấy dữ liệu cross-tenant.

Điểm học quan trọng: Elasticsearch giúp search tốt hơn cho full-text/keyword search, nhưng không thay thế database nghiệp vụ và không thay thế authorization/tenant filter.

### C. MinIO/file storage mini-lab

Phần này giúp em hiểu object storage/S3-compatible API trong bối cảnh file chứng từ/attachment.

- Học object storage, bucket, object key, metadata, content type, private bucket và S3-compatible API.
- MinIO lưu binary object; PostgreSQL lưu metadata nghiệp vụ có `tenantId`.
- Backend sinh object key, không tin object key/tenant từ client.
- Flow cơ bản đã có:
  - upload file;
  - download file của tenant hiện tại;
  - delete file;
  - tenant khác không download được file không thuộc tenant mình.
- Thiết kế vẫn theo Gateway/Adapter:
  - `FileStorageService` xử lý tenant/business flow;
  - `MinioFileStorageGateway` giữ chi tiết MinIO SDK;
  - controller trả response/stream an toàn, không trả raw MinIO object.
- Advanced MinIO như presigned URL expiry, lifecycle/expiration, versioning, retention/object lock/legal hold đã ghi nhận là backlog optional, chưa đưa vào luồng chính.

Điểm học quan trọng: file binary và metadata nghiệp vụ nên tách nhau. Metadata/query/authorization nằm ở backend + PostgreSQL; MinIO chỉ là nơi lưu object.

## 3. Bài học kỹ thuật chính

- Authorization trả lời “user được làm gì”; tenant-aware query trả lời “user được xem dữ liệu tenant nào”.
- `401` là chưa authenticated hoặc token sai; `403` là đã authenticated nhưng không đủ quyền.
- External systems nên được bọc bằng Gateway/Adapter để controller/service không bị trộn SDK/request format.
- Elasticsearch và MinIO là hệ thống phụ trợ/projection/storage, không thay thế PostgreSQL source of truth.
- Mỗi integration bên ngoài vẫn phải giữ security boundary và tenant boundary.
- Feature flag/default disabled mode giúp app-test ổn định khi Redis/Elasticsearch/MinIO/Keycloak không chạy.
- Khi thêm công nghệ mới, em đang đi theo vòng: concept doc -> code guide -> skeleton/TODO -> tự code -> verify -> review -> summary.

## 4. Trạng thái hiện tại

- Keycloak Authorization/RBAC: đã hoàn thành mini-lab cơ bản.
- Elasticsearch/search: đã hoàn thành mini-lab cơ bản.
- MinIO/file storage: đã hoàn thành upload/download/delete cơ bản.
- Redis/cache: đã có theory docs, Redis local setup, config placeholder và code skeleton/TODO.
- Redis chưa được tính là hoàn thành vì em sẽ tự code cache-aside logic tiếp theo: tenant-safe key, get/set với TTL, hit/miss verification và caveat stale/invalidation.

## 5. Kế hoạch tiếp theo

- Hoàn thành Redis/cache mini-lab:
  - thiết kế cache key có `tenantId`;
  - implement cache-aside cho một read path nhỏ;
  - set TTL;
  - verify cache miss/hit;
  - kiểm tra tenant 1/tenant 2 không dùng chung cache key;
  - ghi rõ stale data và invalidation caveat.
- Sau Redis, tiếp tục Kafka/async messaging ở scope nhỏ.
- Sau Kafka, học Observability/logging/metrics.
- MinIO advanced object management để optional/later, không chặn roadmap chính.
