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

## Ngày 14/05: JWT tạm cho tenant context

Sau khi API tenant-aware đã chạy bằng `X-Tenant-Id`, demo được nâng lên một bước gần thực tế hơn: dùng `Authorization: Bearer <token>` để xác thực request ở mức lab, sau đó lấy `tenant_id` từ JWT claim đã validate.

Flow hiện tại:

```text
Request có Bearer token
-> Spring Security validate JWT
-> SecurityContext chứa Jwt đã validate
-> JwtTenantContextFilter đọc tenant_id
-> TenantContext.setCurrentTenant(...)
-> Service/Repository query theo tenantId
```

### Điểm đã verify

`DataLeakageTest.java` đã được chuyển từ header `X-Tenant-Id` sang Bearer JWT và pass 6 test:

- Token tenant 1 chỉ thấy dữ liệu tenant 1.
- Token tenant 2 chỉ thấy dữ liệu tenant 2.
- Tenant 1 không truy cập được id thuộc tenant 2.
- Query theo cùng `code = LAPTOP-01` vẫn scoped đúng theo tenant hiện tại.
- Request thiếu token trả `401`.
- Request dùng token sai trả `401`.

Runtime HTTP cũng đã verify pattern tương tự: dev token tenant 1/2 gọi API thành công, missing/invalid token bị chặn, cross-tenant id trả `404`.

### Điều cần nhớ

- JWT tạm này không phải Keycloak/OIDC production.
- `JWT_SECRET` là secret local để ký/verify token, không phải password user.
- Backend chỉ tin `tenant_id` sau khi JWT đã được validate.
- `TenantContext.clear()` vẫn cần chạy sau request để tránh rò tenant giữa các request/thread.
- Index và JWT không thay thế tenant-aware repository query; service/repository vẫn phải query theo `tenantId`.

### Khác gì với Keycloak/OIDC production?

Trong lab hiện tại, backend tự tạo dev token và tự validate bằng shared secret local để học flow bảo mật tối thiểu. Trong hệ thống production, phần phát hành token thường thuộc về Authorization Server như Keycloak. Backend lúc đó đóng vai trò Resource Server: kiểm tra issuer, chữ ký/JWK, expiration, scope/role/claim rồi mới tin các thông tin như `tenant_id`.

Vì vậy JWT tạm chỉ là bridge học tập:

- giúp bỏ dần cơ chế `X-Tenant-Id` trực tiếp;
- giúp hiểu `Authorization: Bearer <token>` và Spring Security filter chain;
- giúp chứng minh tenant context có thể lấy từ claim đã validate;
- chưa thay thế Keycloak/OIDC, RBAC production, key rotation hoặc user/session management thật.

### Liên hệ với feedback mentor

Mentor nhắc rằng khi đã chạm tới một công nghệ trong feature thật thì nên học công nghệ đó theo ngữ cảnh thật nếu feasible. Vì vậy roadmap mới giữ JWT tạm như bước cầu nối, nhưng đưa Keycloak/OIDC lên thành mini-lab hoặc awareness có evidence rõ. Điều quan trọng là không overclaim: demo hiện tại đã tenant-aware và có regression test, nhưng auth vẫn là lab-level.

## Milestone #6: PostgreSQL index query-pattern mini-lab

Milestone #6 bổ sung phần mentor feedback: index không chỉ là `CREATE INDEX`, mà phụ thuộc rất nhiều vào query pattern và cách PostgreSQL planner ước lượng chi phí.

### Những pattern đã quan sát trong lab 07

Lab dùng bảng tạm `master_data_pattern_lab`, sinh khoảng 200.000 dòng local và không đụng vào business constraint thật của `master_data`.

Kết quả verify định tính:

- Exact match `tenant_id + code`: trước index dùng `Seq Scan`; sau index `(tenant_id, code)` dùng `Index Scan`, `Index Cond` có cả `tenant_id` và `code`.
- Prefix search `name LIKE 'Laptop%'`: nếu chỉ có index bắt đầu bằng `tenant_id`, PostgreSQL có thể dùng index để giới hạn tenant nhưng `name LIKE` vẫn nằm ở `Filter`. Sau index `(tenant_id, name text_pattern_ops)`, điều kiện prefix trên `name` xuất hiện trong `Index Cond`.
- Contains search `name LIKE '%Dell%'`: plan vẫn có thể dùng index nhờ `tenant_id`, nhưng `%Dell%` nằm ở `Filter`. Đây là dấu hiệu B-tree chưa thật sự hỗ trợ tốt contains search.
- `lower(name)`: trước expression index, `lower(name)` nằm ở `Filter`; sau index `(tenant_id, lower(name))`, expression này đi vào `Index Cond`.
- Composite index `(tenant_id, category, code)`: query có `tenant_id + category` dùng index tốt hơn; query chỉ có `category` thiếu leftmost column nên quay về `Seq Scan` trong lab.

### Cách đọc EXPLAIN rút ra

- `Index Cond` cho biết điều kiện nào được dùng để truy cập index.
- `Filter` cho biết điều kiện nào chỉ lọc sau khi đã lấy candidate rows.
- `Rows Removed by Filter` lớn nghĩa là PostgreSQL đã lấy nhiều row rồi loại bỏ sau.
- Estimated rows và actual rows nên được so sánh để xem planner ước lượng có gần thực tế không.
- `Execution Time` hữu ích nhưng dễ nhiễu do cache/máy local; không nên kết luận chỉ dựa trên thời gian một lần chạy.

### Kết luận học được

- B-tree rất phù hợp với exact match và có thể hỗ trợ prefix search nếu index/operator class phù hợp.
- Leading wildcard hoặc contains search như `%abc%` thường không phù hợp với B-tree thông thường; khi có requirement thật nên cân nhắc `pg_trgm` + GIN/GiST.
- Query dùng function như `lower(name)` có thể cần expression index.
- Composite index phải theo query pattern thật; với shared-table multi-tenant, `tenant_id` thường nên đứng đầu.
- Việc plan có dùng index không có nghĩa toàn bộ điều kiện search đã dùng index. Phải phân biệt `Index Cond` và `Filter`.
- Query thiếu `tenant_id` vẫn là lỗi isolation trước khi là lỗi performance.

## Milestone #7: Flyway rollback/failure handling

Milestone #7 bổ sung phần mentor feedback: migration không chỉ là viết SQL thay đổi schema, mà cần hiểu cách migration được execute, fail, rollback/repair và quản lý lịch sử.

### Những điểm đã nắm

- Flyway chạy versioned migration theo thứ tự và ghi lịch sử vào `flyway_schema_history`.
- `migrate` chạy migration pending; `validate` kiểm tra migration files với history; `repair` sửa metadata trong history nhưng không sửa schema/data thật.
- Không nên sửa migration đã apply vì checksum/history sẽ lệch giữa các môi trường.
- Undo migration của Flyway tồn tại nhưng có giới hạn edition và không phải cơ chế rollback tự động cho mọi lỗi.

### Quan sát từ mini-lab

Mini-lab dùng schema riêng `flyway_failure_lab`, không đụng chain `tenant-demo` `V1/V2/V3`.

Kết quả verify:

- `V1` và `V2` chạy thành công, được ghi vào `flyway_schema_history`.
- `V3__intentional_failure.sql` fail vì thêm trùng cột `broken_note`.
- Với PostgreSQL transactional DDL, Flyway rollback sạch migration `V3`; cột `broken_note` không còn lại.
- `flyway_schema_history` không có row `V3` failed trong scenario này; `flyway info` xem `V3` là `Pending`.
- `validate` báo còn migration `V3` resolved nhưng chưa applied; đây không phải dấu hiệu phải chạy `repair`.

### Kết luận về `repair`

`repair` không phải lệnh cần chạy sau mọi migration failure. Trong lab PostgreSQL này, failure được rollback sạch nên không cần `repair`.

`repair` phù hợp hơn khi schema history thật sự cần sửa metadata, ví dụ có failed migration entry, checksum/description/type lệch, hoặc migration missing đã được team quyết định xử lý. Nếu database object thật bị tạo dở dang, phải cleanup object/data bằng SQL phù hợp trước; `repair` không tự làm việc đó.

### Liên hệ shared-table SaaS

Trong shared-table multi-tenant, một migration lỗi trên bảng chung có thể ảnh hưởng tất cả tenant. Vì vậy trước khi chạy migration cần có rollback/forward-fix plan, kiểm tra lock/transaction behavior, ưu tiên backward-compatible migration và test trên local/staging trước.

## Milestone #8: ACID và isolation levels

Milestone #8 bổ sung nền tảng transaction/concurrency cho PostgreSQL. Mục tiêu không phải học hết lock internals, mà là hiểu đủ để không nhầm transaction, isolation và lock khi làm backend shared-table SaaS.

### Những điểm đã nắm

- ACID gồm Atomicity, Consistency, Isolation và Durability.
- `BEGIN` mở transaction, `COMMIT` giữ thay đổi, `ROLLBACK` hủy thay đổi chưa commit.
- PostgreSQL default isolation là `READ COMMITTED`.
- PostgreSQL không cho dirty read; `READ UNCOMMITTED` cư xử như `READ COMMITTED`.
- `READ COMMITTED` có thể có non-repeatable read và phantom read.
- `REPEATABLE READ` giữ snapshot ổn định; trong PostgreSQL mức này cũng ngăn phantom read.
- `SERIALIZABLE` mạnh nhất nhưng có thể abort transaction; application phải retry toàn bộ transaction.

### Transaction và lock

Transaction là unit of work. Isolation là quy tắc transaction nhìn thấy dữ liệu của nhau. Lock/MVCC là cơ chế PostgreSQL dùng để thực thi consistency/concurrency.

Điểm thực dụng:

- `SELECT` thường đọc snapshot và không block writer.
- `UPDATE`/`DELETE` cùng một row có thể block nhau.
- `SELECT FOR UPDATE` lock row dù chưa update, dùng khi backend cần đọc rồi quyết định update an toàn.
- DDL như `ALTER TABLE` có thể giữ lock mạnh hơn DML thường.
- Long transaction nguy hiểm vì giữ lock/snapshot lâu và làm request khác dễ chờ.

### Liên hệ shared-table SaaS

`tenant_id` filtering giải quyết ownership isolation: dữ liệu thuộc tenant nào. Transaction isolation giải quyết concurrency: transaction nhìn thấy dữ liệu ở thời điểm nào và có va chạm update không.

Trong shared-table, một bảng lớn có thể nhận nhiều write từ nhiều tenant. Query/update càng rộng, thiếu index hoặc giữ transaction càng lâu thì càng dễ tạo noisy neighbor. Vì vậy rule thực dụng là: query tenant-aware, index đúng pattern, transaction ngắn, tránh external call lâu trong transaction, và chuẩn bị retry khi dùng isolation mạnh.

### Lab 09

`lab-code/sql-playground/09-acid-isolation-observation.sql` hiện setup bảng lab riêng và hướng dẫn quan sát hai session:

- rollback/commit visibility;
- `READ COMMITTED` non-repeatable read;
- `REPEATABLE READ` stable snapshot;
- update cùng row gây waiting;
- `SELECT FOR UPDATE`;
- optional `SERIALIZABLE` anomaly prevention;
- shared-table tenant-aware row-level behavior.

## Milestone #9: Keycloak/OIDC mini-lab — placeholder

Milestone #9 bắt đầu chuyển từ JWT tạm trong `tenant-demo` sang hiểu luồng Keycloak/OIDC thật. Mục tiêu là lấy token từ Keycloak, hiểu issuer/JWKS/claims và chuẩn bị hướng tích hợp Resource Server bằng `issuer-uri`, chưa thay thế ngay flow JWT local đang pass test.

### TODO sau mini-lab

- Keycloak đóng vai trò Authorization Server/OpenID Provider như thế nào?
- Spring Boot backend đóng vai trò Resource Server như thế nào?
- Realm, client, user, role, issuer, JWKS khác nhau ra sao?
- Access token từ Keycloak có claim `tenant_id` như thế nào?
- JWT tạm HS256 hiện tại khác Keycloak/OIDC ở đâu?
- Vì sao token claim chỉ đáng tin sau khi đã validate?
- Vì sao Keycloak không thay thế tenant-aware service/repository query?
