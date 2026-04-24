# Noisy neighbor trong shared table

## Noisy neighbor là gì?

Noisy neighbor xảy ra khi một tenant sử dụng quá nhiều tài nguyên chung, làm tenant khác bị chậm hoặc lỗi theo.

Trong shared table, nhiều tenant dùng chung:

- Database.
- Connection pool.
- CPU.
- Disk I/O.
- Shared buffers.
- Bảng và index.

Nếu tenant A chạy report nặng, tenant B có thể bị chậm dù dữ liệu của B rất ít.

## Ví dụ đơn giản

```text
invoice table
├── Tenant A: 1.000.000 dòng
├── Tenant B: 100 dòng
└── Tenant C: 50.000 dòng
```

Nếu query của tenant B có index tốt:

```sql
SELECT *
FROM invoice
WHERE tenant_id = 2
  AND status = 'PAID';
```

với index:

```sql
CREATE INDEX idx_invoice_tenant_status
ON invoice (tenant_id, status);
```

PostgreSQL có thể tìm nhanh phần dữ liệu của tenant B.

Nhưng nếu tenant A chạy query nặng không tối ưu:

```sql
SELECT *
FROM invoice
WHERE amount > 1000000;
```

query này có thể quét nhiều dữ liệu và tiêu thụ tài nguyên chung.

## Các tầng gây ảnh hưởng

| Tầng | Cơ chế ảnh hưởng |
|---|---|
| CPU | Query nặng chiếm thời gian xử lý |
| Disk I/O | Full scan đọc nhiều page từ disk |
| Shared buffers | Dữ liệu nóng của tenant khác có thể bị đẩy khỏi cache |
| Connection pool | Tenant lớn chiếm nhiều connection |
| VACUUM | Bảng lớn nhiều update/delete tạo bloat, autovacuum tốn tài nguyên |

## Shared buffers

PostgreSQL dùng vùng nhớ chung để cache data pages. Khi query lớn đọc nhiều page, cache có thể bị thay đổi mạnh. Tenant khác sau đó query lại dữ liệu của mình có thể gặp cache miss và phải đọc disk.

```text
Trước:
[A1][B1][B2][C1][A2][B3]

Tenant A chạy query lớn

Sau:
[A8][A9][A10][A11][A12][A13]
```

Đây là một cách noisy neighbor xuất hiện ở tầng database engine.

## Connection pool contention

Nếu connection pool có 20 connection và tenant A chiếm 15 connection để chạy report, các tenant khác có thể phải chờ.

Giải pháp có thể gồm:

- Giới hạn request hoặc job nặng theo tenant.
- Tách queue cho report nặng.
- Tách connection pool cho tenant lớn.
- Dùng PgBouncer hoặc pooler phù hợp.
- Đưa report nặng sang read replica nếu dữ liệu cho phép.

## Cách giảm noisy neighbor

Các hướng giảm rủi ro:

- Query luôn có tenant filter.
- Composite index bắt đầu bằng `tenant_id`.
- Giới hạn report/export nặng.
- Rate limit theo tenant.
- Resource quota hoặc connection pool isolation.
- Partitioning khi bảng lớn.
- Read replica cho workload đọc nặng.
- Chuyển tenant lớn sang schema/database riêng nếu cần.

## Điều Phase 1 cần nắm

Phase 1 chưa cần giải quyết đầy đủ noisy neighbor như production. Nhưng cần hiểu:

- Shared table rẻ và đơn giản nhưng không miễn phí.
- Index tốt giúp nhiều query tenant nhỏ vẫn nhanh.
- Một query xấu của tenant lớn vẫn có thể ảnh hưởng toàn hệ thống.
- Khi scale, database behavior quan trọng không kém code business.
