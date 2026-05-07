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

## BEGIN, COMMIT và ROLLBACK ở mức lab

Ở mức học hiện tại, có thể hiểu transaction control đơn giản như sau:

- `BEGIN` mở một transaction mới.
- `COMMIT` xác nhận và giữ lại các thay đổi trong transaction.
- `ROLLBACK` hủy các thay đổi chưa được commit trong transaction.

Ví dụ thử một schema change rồi hủy:

```sql
BEGIN;
ALTER TABLE master_data ADD COLUMN lab_note varchar(255);
ROLLBACK;
```

Sau `ROLLBACK`, cột `lab_note` không được giữ lại. Cách này hữu ích khi học local vì mình có thể quan sát lệnh `ALTER TABLE` mà không làm bẩn schema lâu dài.

Ngược lại, nếu dùng `COMMIT`:

```sql
BEGIN;
ALTER TABLE master_data ADD COLUMN lab_note varchar(255);
COMMIT;
```

Thay đổi schema được giữ lại, và cần một migration/cleanup khác nếu muốn quay về trạng thái cũ.

Điểm cần nhớ:

- `BEGIN` + `ROLLBACK` là cách tốt để thử nghiệm local có kiểm soát.
- `COMMIT` làm thay đổi trở thành trạng thái thật của database.
- Trước khi chạy migration, nên nghĩ trước cách kiểm tra, cleanup và rollback.
- Trong hệ thống thật dùng Flyway, rollback không chỉ là gõ `ROLLBACK` sau khi migration đã chạy xong. Flyway quản lý lịch sử migration; nếu migration đã áp dụng và hệ thống đã ghi dữ liệu mới, thường cần một migration tiếp theo hoặc kế hoạch rollback được thiết kế trước.

Phần này chỉ là giải thích ở mức học lab, chưa đi sâu vào transaction isolation level hay toàn bộ cơ chế lock của PostgreSQL.

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

## Liên hệ với bài lab 06

Sau khi đọc phần lý thuyết này, dùng `lab-code/sql-playground/06-migration-lock-observation.sql` như bài thực hành đi kèm. Mục tiêu của lab là quan sát một schema change nhỏ, nghĩ trước cleanup/rollback, và thử thấy việc giữ lock có thể ảnh hưởng session khác trong PostgreSQL local.

Không cần biến bài lab này thành hướng dẫn migration production đầy đủ. Ở giai đoạn này, chỉ cần nắm mindset: migration phải nhỏ, có đường thoát, và được kiểm thử trước khi áp dụng vào bảng shared-table nhiều tenant.

## Kết luận

Migration trong SaaS là bài toán production nghiêm túc. Cần thiết kế migration nhỏ, tương thích ngược, quan sát được và có đường thoát khi lỗi. Với hệ thống kế toán, migration sai có thể không chỉ gây downtime mà còn ảnh hưởng tính đúng đắn dữ liệu.
