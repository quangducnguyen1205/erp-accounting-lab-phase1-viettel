# Những gì đã nắm được

## Bức tranh tổng quan

Phase 1 đã giúp xây nền tảng về cách nhìn một hệ thống ERP/kế toán SaaS không chỉ ở mức tính năng, mà ở mức backend architecture và vận hành.

Các điểm đã nắm chắc hơn:

- SaaS là mô hình phân phối phần mềm.
- Multi-tenant là pattern kiến trúc, không phải định nghĩa của SaaS.
- Subscription là mô hình tính tiền, không đồng nghĩa với SaaS.
- Tenant isolation có nhiều mức, mỗi mức có trade-off riêng.
- Backend cho SaaS phải tenant-aware từ database đến auth, cache, log và monitoring.

## Kiến thức SaaS

Đã phân biệt được:

| Khái niệm | Bản chất |
|---|---|
| SaaS | Nhà cung cấp host, vận hành và cập nhật phần mềm |
| Cloud | Hạ tầng hoặc nền tảng để chạy hệ thống |
| Subscription | Cách thu phí định kỳ |
| On-premise | Phần mềm chạy trong môi trường khách hàng |
| Multi-tenant | Nhiều tenant dùng chung nền tảng với dữ liệu cách ly |

Điểm quan trọng nhất: các khái niệm này có liên quan nhưng không thay thế nhau.

## Kiến thức multi-tenant

Đã hiểu ba mô hình tenant isolation:

- Shared table với `tenant_id`.
- Schema per tenant.
- Database per tenant.

Đã hiểu các trade-off:

- Chi phí càng thấp thì isolation thường càng yếu.
- Isolation càng mạnh thì vận hành và migration càng phức tạp.
- Shared table phù hợp học và demo, nhưng cần chống data leakage và noisy neighbor.
- Schema per tenant tăng isolation nhưng migration N schema là vấn đề lớn.
- Database per tenant phù hợp enterprise nhưng chi phí cao.

## Kiến thức deployment và rollout

Đã nắm các khái niệm:

- Blast radius.
- Feature flags.
- Rolling deployment.
- Blue-green deployment.
- Canary rollout ở mức khái niệm.
- Backward-compatible migration.

Điểm rút ra: trong SaaS, deploy không chỉ là đưa code mới lên. Deploy còn liên quan đến schema, dữ liệu cũ, code cũ đang chạy song song và khả năng rollback.

## Kiến thức backend/database

Đã bắt đầu hiểu:

- Vì sao PostgreSQL phù hợp với dữ liệu kế toán.
- Vì sao ACID quan trọng trong nghiệp vụ tài chính.
- Composite index và thứ tự cột trong index.
- Query tenant-aware cần index bắt đầu bằng `tenant_id`.
- Full table scan có thể gây noisy neighbor.
- Migration có thể giữ lock và ảnh hưởng nhiều tenant.
- Partitioning, vacuum và read replica là hướng học sâu tiếp theo.

## Milestone #1: SQL playground tenant-aware

Milestone #1 đã đóng phần thực hành SQL nền tảng cho mô hình shared-table multi-tenant. Các lệnh verify đã chạy lại trên database local sạch: `make db-up`, `make db-status`, `make sql-reset`, `make sql-all`, `make sql-3`, `make sql-4`, `make sql-5`.

### Schema baseline

Đã tạo và kiểm chứng hai bảng chính:

- `tenants`: lưu danh sách tenant/doanh nghiệp.
- `master_data`: lưu dữ liệu nghiệp vụ dùng chung bảng, có cột `tenant_id`.

Điểm quan trọng nhất là constraint `UNIQUE (tenant_id, code)`. Trong multi-tenant, `UNIQUE(code)` là sai nếu mã nghiệp vụ chỉ cần duy nhất trong phạm vi từng tenant. Nếu dùng `UNIQUE(code)`, hai doanh nghiệp khác nhau sẽ không thể có cùng một mã hợp lệ như `LAPTOP-01`, và hệ thống có thể vô tình để lộ thông tin tồn tại của dữ liệu tenant khác.

### Dữ liệu mẫu và tenant isolation

Dữ liệu mẫu có hai tenant: VIETTEL và FPT. Bảng `master_data` có 6 dòng, mỗi tenant có 3 dòng. Mã `LAPTOP-01` xuất hiện ở cả hai tenant, chứng minh rằng cùng một business code có thể tồn tại hợp lệ ở nhiều tenant khác nhau.

Kết luận: tenant isolation không tự xảy ra chỉ vì schema có `tenant_id`. Query ở tầng backend phải luôn giới hạn theo tenant hiện tại.

### EXPLAIN và EXPLAIN ANALYZE

Đã thực hành dùng `EXPLAIN ANALYZE` để đọc query plan cơ bản:

- `Seq Scan`: PostgreSQL đọc tuần tự qua bảng rồi lọc dữ liệu.
- `Index Scan`: PostgreSQL dùng index để tìm dòng phù hợp.
- `Bitmap Index Scan` + `Bitmap Heap Scan`: PostgreSQL dùng index để tìm candidate rows, sau đó quay lại bảng để lấy row đầy đủ.

Đã hiểu sự khác nhau giữa estimated values và actual values: `cost`, `rows`, `width` là ước lượng của planner; `actual time`, `actual rows`, `loops`, `Planning Time`, `Execution Time`, `Buffers` là thông tin thực tế khi chạy query. Với bảng nhỏ, PostgreSQL vẫn có thể chọn `Seq Scan` vì đọc cả bảng vài dòng đôi khi rẻ hơn dùng index.

### So sánh index trong lab

Bảng thật `master_data` vẫn giữ business constraint `UNIQUE (tenant_id, code)`. Không drop constraint thật chỉ để benchmark, vì constraint này bảo vệ tính đúng đắn dữ liệu.

Để thí nghiệm index an toàn, bài lab dùng bảng tạm `master_data_index_lab` được copy từ `master_data` và sinh thêm dữ liệu local. Kết quả verify cho thấy:

- Khi chưa có index thử nghiệm, query theo `tenant_id` dùng `Seq Scan`.
- Khi có index trên `tenant_id`, plan chuyển sang dạng index-assisted (`Bitmap Index Scan` + `Bitmap Heap Scan`).
- Khi có composite index `(tenant_id, code)`, query `WHERE tenant_id = ... AND code = ...` dùng `Index Scan`.
- Query thiếu `tenant_id` vẫn là lỗi thiết kế multi-tenant, dù có thể nhanh hoặc chậm tùy dữ liệu.

Điểm rút ra: PostgreSQL chọn plan có chi phí ước lượng rẻ nhất, không phải lúc nào cũng dùng index chỉ vì index tồn tại.

### Data leakage

Bài `05-data-leakage-test.sql` chứng minh rõ rủi ro:

- Query an toàn: `WHERE tenant_id = 1 AND code = 'LAPTOP-01'` chỉ trả dữ liệu của VIETTEL.
- Query thiếu `tenant_id`: `WHERE code = 'LAPTOP-01'` trả cả dữ liệu VIETTEL và FPT.

Đây là lỗi correctness/tenant isolation trước khi là lỗi performance. Index giúp truy vấn nhanh hơn, nhưng không thay thế authorization và không tự bảo vệ dữ liệu giữa các tenant. Ở backend, repository/service query phải enforce `tenant_id` nhất quán từ trusted context như JWT/header đã validate hoặc `TenantContext`, không tin trực tiếp `tenant_id` từ request body.

## Milestone #2: Migration & Locking — ghi chú sau thực hành

Milestone #2 đóng vai trò cầu nối giữa SQL playground và phần Spring Boot + Flyway. Mục tiêu không phải học sâu toàn bộ PostgreSQL internals, mà là nắm mindset tối thiểu trước khi đưa schema migration vào backend demo.

### Migration là gì?

Migration là thay đổi có kiểm soát trên schema hoặc dữ liệu database, ví dụ thêm cột, thêm constraint, tạo index hoặc chuẩn bị dữ liệu cho phiên bản code mới. Backend team cần quản lý migration cẩn thận vì database thường là trạng thái chung của hệ thống, khó rollback hơn code và có thể ảnh hưởng dữ liệu đang được nhiều tenant sử dụng.

### Lock là gì?

Lock là cơ chế database dùng để bảo vệ tính nhất quán khi nhiều session cùng đọc/ghi hoặc thay đổi schema. Ở mức học hiện tại, điểm cần nhớ là một số lệnh DDL như `ALTER TABLE` có thể giữ lock mạnh và làm statement khác phải chờ. Không cần thuộc toàn bộ lock mode, nhưng cần biết migration có thể gây blocking.

### BEGIN / COMMIT / ROLLBACK

`BEGIN` mở một transaction, `COMMIT` giữ lại thay đổi, còn `ROLLBACK` hủy thay đổi chưa commit. Trong lab local, `BEGIN` + `ROLLBACK` hữu ích để thử một schema change nhỏ mà không giữ lại schema rác. Trong migration thật với Flyway, rollback cần được thiết kế trước; nếu migration đã áp dụng và dữ liệu mới đã phát sinh, thường không thể chỉ coi rollback là một lệnh SQL đơn giản.

### Quan sát local từ `06-migration-lock-observation.sql`

Lab 06 kiểm tra baseline của `master_data`, thử thêm cột nullable `lab_observation`, kiểm tra dữ liệu cũ vẫn còn, sau đó cleanup bằng `DROP COLUMN IF EXISTS`. Lab cũng có ví dụ `BEGIN` + `ROLLBACK` với cột `lab_rollback_test` để thấy thay đổi schema trong transaction có thể được hủy ở local.

Phần quan sát blocking thật cần hai terminal/session nên không chạy tự động bằng `make sql-6`. Bài lab chỉ giữ hướng dẫn thủ công để tránh giả lập sai: một session giữ transaction sau `ALTER TABLE`, session còn lại thử query và quan sát việc chờ/block nếu xảy ra.

### Vì sao shared-table multi-tenant cần cẩn thận khi `ALTER TABLE`?

Trong shared-table design, tất cả tenant cùng dùng một bảng vật lý như `master_data`. Vì vậy một `ALTER TABLE master_data` không chỉ tác động một tenant, mà có thể ảnh hưởng toàn bộ request của nhiều doanh nghiệp đang dùng bảng đó. Nếu lock kéo dài, blast radius là toàn hệ thống dùng chung bảng.

### Rule of thumb rút ra

- Ưu tiên migration nhỏ, dễ kiểm tra và dễ rollback.
- Nghĩ cleanup/rollback trước khi chạy migration, không đợi lỗi mới nghĩ.
- Với SaaS shared-table, luôn cân nhắc lock duration và ảnh hưởng lên tất cả tenant.
- Ưu tiên backward-compatible migration để code cũ và code mới có thể cùng tồn tại khi deploy.
- Test trên local/staging trước production; nếu cần backfill lớn, nên tách bước và đo đạc kỹ hơn.

### Giới hạn hiện tại / chưa học sâu

Phần này mới dừng ở mức quan sát local và mindset an toàn. Chưa học sâu lock mode chi tiết, partitioning, vacuum tuning, zero-downtime migration production, migration trên bảng rất lớn, hoặc strategy rollback phức tạp khi dữ liệu mới đã được ghi trong thời gian dài.

## Giới hạn hiện tại

Những phần vẫn cần học tiếp:

- Hiểu sâu hơn về PostgreSQL locks.
- Thực hành migration an toàn bằng Flyway hoặc Liquibase.
- Tự code một demo backend nhỏ để kiểm chứng lý thuyết.
- Thiết kế test chống data leakage.

## Hướng tiếp theo

Hướng học tiếp nên đi theo thứ tự:

1. Củng cố PostgreSQL: index, query plan, lock, transaction.
2. Thực hành một backend demo nhỏ với shared table + `tenant_id`.
3. Tự implement task code trước.
4. Nhờ Agent review code, chỉ ra lỗi và đề xuất cải thiện.
5. Khi demo lớn, tách code sang repository riêng nếu cần.

## Kết luận

Nền tảng SaaS và multi-tenant đã đủ rõ để chuyển sang giai đoạn học backend/database sâu hơn. Trọng tâm tiếp theo không phải học thêm thật nhiều khái niệm rời rạc, mà là thực hành có kiểm soát để thấy các trade-off xuất hiện trong code, query, migration và vận hành.

## Milestone #4: Spring Boot tenant-aware API

Milestone #4 chuyển bài học data leakage từ SQL playground sang backend API thật. API `master_data` hiện đi theo flow:

```text
Request
-> TenantFilter đọc X-Tenant-Id
-> TenantContext lưu tenant hiện tại
-> Controller gọi Service
-> Service gọi Repository bằng method có tenantId
-> PostgreSQL chỉ trả dữ liệu trong phạm vi tenant đó
```

### 3 rule backend tenant-aware

1. **Tenant phải lấy từ trusted context**, ví dụ `TenantContext` hiện tại hoặc JWT/OIDC đã validate sau này; không để request body tự quyết định tenant.
2. **Repository query nghiệp vụ phải scoped theo `tenantId`**, ví dụ dùng `findByTenantIdAndCode(...)` thay vì `findByCode(...)`.
3. **Backend phải enforce tenant isolation ở service/repository**, không dựa vào frontend, Swagger UI, Postman hay người dùng tự gửi đúng dữ liệu.

Ví dụ thực tế trong demo:

- Tìm master data theo code phải dùng `tenantId + code`, vì `LAPTOP-01` có thể tồn tại hợp lệ ở nhiều tenant.
- Tenant 1 gọi `/api/master-data/{id}` với id thuộc tenant 2 nên trả `404`, không trả dữ liệu tenant 2.

### Curl/HTTP Client verification pattern

Không cần paste log dài vào report. Chỉ cần ghi pattern kiểm chứng:

| Case | Request pattern | Expected behavior |
|---|---|---|
| Tenant 1 list | `GET /api/master-data` + `X-Tenant-Id: 1` | `200`, chỉ thấy data tenant 1 |
| Tenant 2 list | `GET /api/master-data` + `X-Tenant-Id: 2` | `200`, chỉ thấy data tenant 2 |
| Missing tenant | `GET /api/master-data` không có header | `400` |
| Invalid tenant | `X-Tenant-Id: abc` | `400` |
| Find by code | `GET /api/master-data/code/{code}` + tenant header | Kết quả scoped theo tenant hiện tại |
| Cross-tenant id | Tenant 1 gọi id thuộc tenant 2 | `404` hoặc bị chặn, không lộ dữ liệu |

File hỗ trợ test thủ công:

- `docs/04-spring-boot/curl-va-http-client-api-testing.md`
- `lab-code/tenant-demo/http/master-data-api.http`

### Kết quả verify Milestone #4 ngày 13/05

Đã verify API trên app đang chạy ở `localhost:8080` với dữ liệu local hiện tại:

| Case | Kết quả |
|---|---|
| Tenant 1 list | `200`, response chỉ có `tenantId = 1` |
| Tenant 2 list | `200`, response chỉ có `tenantId = 2` |
| Missing `X-Tenant-Id` | `400 Bad Request` |
| Invalid `X-Tenant-Id: abc` | `400 Bad Request` |
| Tenant 1 tìm `code = LAPTOP-01` | trả record tenant 1 |
| Tenant 2 tìm `code = LAPTOP-01` | trả record tenant 2 |
| Tenant 1 gọi id `6` của tenant 2 | `404 Not Found`, không lộ dữ liệu |
| Tenant 2 gọi id `6` | `200`, chứng minh record tồn tại nhưng scoped theo tenant |

Kết luận: API hiện tại đã chứng minh được tenant-aware flow ở mức demo. Đây vẫn là cơ chế header-based bằng `X-Tenant-Id` để học request flow; bước sau mới chuyển sang JWT tạm và sau đó hiểu Keycloak/OIDC ở mức architecture awareness.

## Ngày 14/05: DataLeakageTest chống leakage

Sau khi verify thủ công bằng curl/HTTP Client, phần tenant isolation đã được khóa lại bằng automated regression test trong `DataLeakageTest.java`.

Test hiện dùng `@SpringBootTest` + `@AutoConfigureMockMvc` để request đi qua flow gần giống runtime:

```text
MockMvc
-> TenantFilter
-> Controller
-> Service
-> Repository
-> PostgreSQL
```

### Kết quả chính

`cd lab-code && make app-test` đã pass với 6 test:

- Tenant 1 list chỉ trả dữ liệu `tenantId = 1`.
- Tenant 2 list chỉ trả dữ liệu `tenantId = 2`.
- Tenant 1 không truy cập được id thuộc tenant 2.
- Query theo cùng `code = LAPTOP-01` vẫn scoped đúng theo tenant hiện tại.
- Request thiếu `X-Tenant-Id` trả `400`.
- Request có `X-Tenant-Id` không hợp lệ trả `400`.

### Điểm học được

- Curl chứng minh API đúng ở thời điểm hiện tại; test tự động giúp chống regression khi code thay đổi.
- Test cần tự chuẩn bị fixture, không phụ thuộc dữ liệu local đang bẩn.
- Với multi-tenant, happy path chưa đủ; phải test cả case cross-tenant access và missing/invalid tenant context.
- `404` trong cross-tenant id case là hành vi tốt cho demo này: record có tồn tại ở tenant 2, nhưng tenant 1 không được thấy.

### Giới hạn hiện tại

Test hiện dùng PostgreSQL local của lab, chưa dùng Testcontainers hoặc test database tách riêng. Cách này đủ cho Phase 1 learning, nhưng khi dự án lớn hơn nên cân nhắc test profile/database riêng để không ảnh hưởng dữ liệu local dùng demo thủ công.
