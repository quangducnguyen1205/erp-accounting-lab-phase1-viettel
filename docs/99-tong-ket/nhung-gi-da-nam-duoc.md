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
