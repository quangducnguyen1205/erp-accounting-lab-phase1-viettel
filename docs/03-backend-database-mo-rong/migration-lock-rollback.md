# Migration, lock và rollback

## Vì sao migration nguy hiểm trong SaaS?

Trong SaaS, nhiều tenant có thể đang dùng chung bảng. Một migration giữ lock lâu có thể ảnh hưởng tất cả tenant.

Ví dụ:

```sql
ALTER TABLE invoice ADD COLUMN tax_code varchar(20);
```

Lệnh này có thể rất nhanh trong nhiều trường hợp. Nhưng không phải mọi `ALTER TABLE` đều an toàn như nhau.

## Lock trong PostgreSQL

PostgreSQL dùng lock để bảo vệ tính nhất quán khi đọc và ghi. Một số thay đổi schema cần lock mạnh.

Điểm cần nhớ:

- `SELECT` thông thường cần lock nhẹ.
- `INSERT`, `UPDATE`, `DELETE` cũng có lock riêng.
- Một số `ALTER TABLE` cần `ACCESS EXCLUSIVE`, có thể block cả đọc và ghi.
- `CREATE INDEX` thường có thể block write, còn `CREATE INDEX CONCURRENTLY` ít gây gián đoạn hơn nhưng chạy lâu hơn.

## Migration backward-compatible

Migration an toàn trong zero-downtime deployment thường đi theo hướng tương thích ngược.

Ví dụ quy trình thêm field mới:

1. Thêm cột nullable.
2. Deploy code mới có thể đọc/ghi cột mới.
3. Backfill dữ liệu cũ theo batch nếu cần.
4. Thêm constraint sau khi dữ liệu đã sạch.
5. Xóa code cũ ở lần deploy sau.

Ví dụ:

```sql
ALTER TABLE invoice ADD COLUMN tax_code varchar(20);
```

Sau đó nếu cần constraint:

```sql
ALTER TABLE invoice
ADD CONSTRAINT chk_invoice_tax_code
CHECK (tax_code IS NOT NULL) NOT VALID;

ALTER TABLE invoice
VALIDATE CONSTRAINT chk_invoice_tax_code;
```

Ý tưởng là tránh một bước lớn vừa đổi schema, vừa ép dữ liệu, vừa bắt code mới phải chạy ngay.

## Migration rủi ro hơn

Các thay đổi cần cẩn trọng:

- Đổi type của cột lớn.
- Thêm `NOT NULL` khi dữ liệu cũ chưa sạch.
- Tạo index thường trên bảng lớn.
- Rename hoặc drop cột khi code cũ vẫn còn dùng.
- Migration vừa thay schema vừa backfill lượng lớn dữ liệu trong một transaction.

Ví dụ nên tránh làm vội:

```sql
ALTER TABLE invoice ALTER COLUMN amount TYPE numeric(20,2);
```

Nếu bảng lớn, lệnh này có thể rewrite dữ liệu và giữ lock lâu.

## Rollback không chỉ là rollback code

Rollback code thường nhanh hơn rollback database. Nếu schema đã đổi và dữ liệu mới đã ghi vào cột mới, việc quay lại phiên bản cũ có thể khó.

Vì vậy cần hỏi trước khi migration:

- Code cũ có chạy được với schema mới không?
- Code mới có chạy được nếu dữ liệu cũ chưa backfill xong không?
- Nếu deploy lỗi, có thể tắt feature flag thay vì rollback DB không?
- Migration có thể chạy lại an toàn không?
- Có cần chia migration thành nhiều bước nhỏ không?

## Schema per tenant và partial migration

Với schema per tenant, migration phải chạy qua nhiều schema:

```text
tenant_001 -> thành công
tenant_002 -> thành công
tenant_003 -> lỗi
tenant_004 -> chưa chạy
...
```

Rủi ro:

- Một số tenant ở schema mới, một số tenant ở schema cũ.
- Code mới kỳ vọng cột mới nhưng tenant chưa migrate.
- Rollback phải xử lý nhiều schema, có thể lại fail ở giữa.
- Thời gian tổng tăng theo số tenant.

Đây là lý do Phase 1 chưa nên vội dùng schema per tenant nếu mục tiêu chính là học nền tảng.

## Kết luận

Migration trong SaaS là bài toán production nghiêm túc. Cần thiết kế migration nhỏ, tương thích ngược, quan sát được và có đường thoát khi lỗi. Với hệ thống kế toán, migration sai có thể không chỉ gây downtime mà còn ảnh hưởng tính đúng đắn dữ liệu.
