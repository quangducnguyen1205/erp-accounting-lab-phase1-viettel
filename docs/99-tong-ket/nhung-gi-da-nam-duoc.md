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

## Giới hạn hiện tại

Những phần vẫn cần học tiếp:

- Thực hành đọc `EXPLAIN ANALYZE` với dữ liệu thật.
- Thực hành tạo index và so sánh query plan.
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
