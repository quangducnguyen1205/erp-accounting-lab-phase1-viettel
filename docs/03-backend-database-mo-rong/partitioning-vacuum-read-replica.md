# Partitioning, vacuum và read replica

## Khi nào cần nghĩ đến partitioning?

Partitioning chia một bảng logic lớn thành nhiều phần vật lý nhỏ hơn. Trong multi-tenant shared table, partitioning có thể hữu ích khi một số bảng lớn dần theo thời gian hoặc theo tenant.

Ví dụ:

```sql
CREATE TABLE invoice (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    invoice_no varchar(50) NOT NULL,
    amount numeric(18,2) NOT NULL,
    created_at timestamp NOT NULL
) PARTITION BY HASH (tenant_id);
```

Partitioning không phải việc cần làm ngay trong Phase 1. Nó là hướng mở rộng khi dữ liệu đủ lớn và query pattern đủ rõ.

## Partition pruning

Partition pruning là việc PostgreSQL bỏ qua các partition không liên quan khi query có điều kiện phù hợp.

Nếu partition theo `tenant_id` và query có:

```sql
WHERE tenant_id = :tenant_id
```

PostgreSQL có thể chỉ đọc partition liên quan thay vì quét toàn bộ bảng logic.

Lợi ích:

- Giảm lượng dữ liệu phải đọc.
- Index trên từng partition nhỏ hơn.
- VACUUM trên partition nhỏ có thể nhẹ hơn bảng khổng lồ.
- Có thể giảm noisy neighbor trong một số trường hợp.

## Trade-off của partitioning

| Lợi ích | Cái giá phải trả |
|---|---|
| Query có thể nhanh hơn nếu pruning tốt | Thiết kế schema phức tạp hơn |
| Quản lý dữ liệu lớn tốt hơn | Primary key/unique constraint bị ràng buộc bởi partition key |
| Có thể giảm bloat theo partition | Query cross-partition có thể chậm |
| Dễ archive/drop một phần dữ liệu | Số lượng partition quá nhiều làm planning nặng |

Partitioning không thay thế index tốt. Nếu query thiếu `tenant_id`, partitioning theo tenant cũng không giúp đúng hướng.

## VACUUM và bloat

PostgreSQL dùng MVCC. Khi update hoặc delete, row cũ không biến mất ngay mà trở thành dead tuple. VACUUM dọn các dead tuple này để tái sử dụng không gian.

Trong shared table:

```text
Tenant A update nhiều
    -> tạo nhiều dead tuple
    -> bảng chung phình ra
    -> autovacuum phải làm việc nhiều
    -> tenant khác cũng bị ảnh hưởng bởi bảng/index lớn hơn
```

Điều cần học tiếp:

- Autovacuum chạy khi nào.
- Bloat ảnh hưởng query ra sao.
- Index bloat là gì.
- Vì sao bảng nhiều update cần được quan sát riêng.

## Read replica

Read replica là bản sao chỉ đọc của database chính. Có thể dùng để giảm tải workload đọc nặng như report, dashboard hoặc export.

Ví dụ hướng xử lý:

```text
Request ghi chứng từ -> primary database
Report đọc dữ liệu   -> read replica
```

Lợi ích:

- Giảm tải đọc trên primary.
- Tách workload report khỏi giao dịch chính.
- Hữu ích khi nhiều tenant chạy báo cáo.

Giới hạn:

- Replica có thể có replication lag.
- Không phù hợp cho dữ liệu cần đọc ngay sau khi ghi nếu yêu cầu real-time tuyệt đối.
- Vẫn cần query/index tốt trên replica.
- Không giải quyết data leakage nếu query thiếu tenant filter.

## Hướng học tiếp

Các chủ đề cần đào sâu sau Phase 1:

- Đọc `EXPLAIN ANALYZE`.
- Hiểu index scan, bitmap scan, sequential scan.
- Theo dõi locks và long-running transactions.
- Cấu hình autovacuum ở mức cơ bản.
- Thiết kế migration không block lâu.
- Phân biệt workload giao dịch và workload báo cáo.

## Kết luận

Partitioning, vacuum và read replica là các chủ đề mở rộng giúp hiểu backend production. Chưa cần áp dụng sớm, nhưng cần biết chúng tồn tại để không thiết kế shared table như thể dữ liệu sẽ luôn nhỏ.
