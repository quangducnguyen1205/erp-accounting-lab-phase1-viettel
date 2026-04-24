# Index và query tenant-aware

## Vì sao index quan trọng trong shared table?

Trong mô hình shared table, nhiều tenant cùng nằm trong một bảng. Nếu bảng `invoice` có hàng triệu dòng, query thiếu index phù hợp có thể quét qua dữ liệu của nhiều tenant khác nhau.

Ví dụ:

```sql
SELECT *
FROM invoice
WHERE tenant_id = 2
  AND status = 'PAID';
```

Index phù hợp:

```sql
CREATE INDEX idx_invoice_tenant_status
ON invoice (tenant_id, status);
```

Index này giúp database đi thẳng vào nhóm dữ liệu của tenant 2 rồi lọc tiếp theo `status`.

## Composite index

Composite index là index trên nhiều cột. Thứ tự cột rất quan trọng.

```sql
CREATE INDEX idx_invoice_tenant_created_at
ON invoice (tenant_id, created_at);
```

Index trên `(tenant_id, created_at)` hữu ích cho:

```sql
WHERE tenant_id = :tenant_id
WHERE tenant_id = :tenant_id AND created_at >= :from_date
```

Nhưng không tối ưu cho:

```sql
WHERE created_at >= :from_date
```

Lý do là B-tree index mạnh nhất khi query dùng cột đầu tiên hoặc nhóm cột đầu theo thứ tự index. Đây thường được gọi là leftmost prefix.

## Nguyên tắc trong multi-tenant

Với shared table, phần lớn index nghiệp vụ nên bắt đầu bằng `tenant_id` nếu query luôn chạy trong phạm vi một tenant.

Ví dụ:

```sql
CREATE INDEX idx_invoice_tenant_status
ON invoice (tenant_id, status);

CREATE INDEX idx_invoice_tenant_created_at
ON invoice (tenant_id, created_at);

CREATE INDEX idx_customer_tenant_code
ON customer (tenant_id, code);
```

Không phải mọi index đều bắt đầu bằng `tenant_id`, nhưng với query nghiệp vụ của tenant, đây là default cần nghĩ đến đầu tiên.

## Full table scan

Full table scan hoặc sequential scan xảy ra khi PostgreSQL đọc toàn bộ bảng để lọc dữ liệu.

Ví dụ nguy hiểm:

```sql
SELECT *
FROM invoice
WHERE status = 'PAID';
```

Vấn đề:

- Thiếu tenant filter nên có nguy cơ trả dữ liệu cross-tenant.
- Nếu bảng lớn, DB phải đọc nhiều page.
- CPU, I/O và cache bị tiêu thụ.
- Tenant khác có thể bị chậm theo.

## Dùng EXPLAIN

Khi học PostgreSQL, cần tập thói quen đọc query plan.

```sql
EXPLAIN ANALYZE
SELECT *
FROM invoice
WHERE tenant_id = 2
  AND status = 'PAID';
```

Nếu thấy `Index Scan` trên index tenant-aware, query có khả năng ổn hơn. Nếu thấy `Seq Scan` trên bảng lớn, cần kiểm tra:

- Có thiếu index không?
- Điều kiện query có dùng đúng cột index không?
- Query trả quá nhiều dòng không?
- Statistics có cũ không?

## Unique constraint tenant-aware

Trong ERP/kế toán, nhiều mã chỉ cần unique trong phạm vi doanh nghiệp, không phải toàn hệ thống.

Sai trong multi-tenant:

```sql
UNIQUE (code)
```

Đúng hơn:

```sql
UNIQUE (tenant_id, code)
```

Ví dụ mã khách hàng `KH001` có thể tồn tại ở nhiều doanh nghiệp khác nhau. Không nên bắt mọi tenant dùng mã duy nhất toàn hệ thống nếu nghiệp vụ không yêu cầu.

## Kết luận

Tenant-aware query không chỉ là thêm `WHERE tenant_id`. Nó còn kéo theo cách thiết kế index, unique constraint, test, monitoring và thói quen đọc query plan. Với shared table, composite index bắt đầu bằng `tenant_id` là một nền tảng rất quan trọng.
