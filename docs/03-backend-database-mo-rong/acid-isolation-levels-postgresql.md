# ACID và isolation levels trong PostgreSQL

## Mục tiêu học

Tài liệu này giúp mình hiểu transaction ở mức backend engineer cần dùng trong Phase 1: vì sao nhiều câu SQL cần được gom thành một đơn vị, các transaction đồng thời có thể nhìn thấy gì, và vì sao điều đó quan trọng trong shared-table multi-tenant SaaS.

Không xem đây là tài liệu tuning lock hoặc thiết kế hệ thống tài chính hoàn chỉnh. Mục tiêu hiện tại là đọc được behavior cơ bản và biết lúc nào cần cẩn trọng hơn.

## ACID là gì?

| Chữ | Ý nghĩa thực tế |
|---|---|
| **A - Atomicity** | Một transaction hoặc thành công toàn bộ, hoặc không để lại thay đổi nào. |
| **C - Consistency** | Transaction đưa database từ trạng thái hợp lệ này sang trạng thái hợp lệ khác, vẫn tôn trọng constraint/rule đã đặt ra. |
| **I - Isolation** | Transaction đang chạy không nên thấy trạng thái dở dang của transaction khác; mức cô lập quyết định nó thấy snapshot nào. |
| **D - Durability** | Khi database đã báo commit thành công, thay đổi phải được ghi bền vững ngay cả khi sau đó có crash. |

Ví dụ kế toán đơn giản: chuyển tiền giữa hai tài khoản không được trừ tiền bên A rồi crash trước khi cộng tiền bên B. PostgreSQL tutorial nhấn mạnh transaction là một thao tác “all-or-nothing”, trạng thái trung gian không lộ ra cho transaction khác và thay đổi đã commit phải được lưu bền vững.

## Transaction basics

```sql
BEGIN;
-- nhiều câu SQL thuộc cùng một nghiệp vụ
COMMIT;
```

- `BEGIN` mở một transaction block.
- `COMMIT` xác nhận toàn bộ thay đổi trong transaction.
- `ROLLBACK` hủy toàn bộ thay đổi chưa commit.

```sql
BEGIN;
UPDATE account_balance
SET amount = amount - 100
WHERE tenant_id = 1 AND account_code = 'CASH';

-- Nếu phát hiện sai điều kiện nghiệp vụ:
ROLLBACK;
```

Nếu không viết `BEGIN`, PostgreSQL vẫn chạy mỗi statement trong một transaction ngầm riêng. Với backend service, nhiều thay đổi cùng một use case thường cần nằm trong cùng transaction để tránh trạng thái nửa chừng.

## Các hiện tượng cần biết

| Hiện tượng | Mô tả ngắn |
|---|---|
| **Dirty read** | Transaction A đọc dữ liệu transaction B đã viết nhưng chưa commit. |
| **Non-repeatable read** | Cùng một transaction đọc lại một row và thấy giá trị đã đổi vì transaction khác vừa commit. |
| **Phantom read** | Chạy lại cùng một query điều kiện và thấy tập row phù hợp đã thay đổi vì transaction khác vừa commit insert/delete. |
| **Serialization anomaly** | Kết quả nhiều transaction concurrent không tương đương với bất kỳ thứ tự chạy tuần tự nào. |

Đây là các tên gọi để mô tả “mức độ lạ” có thể xảy ra khi nhiều transaction chạy cùng lúc.

## PostgreSQL isolation levels

Theo tài liệu PostgreSQL hiện tại:

| Isolation level yêu cầu | Dirty read | Non-repeatable read | Phantom read | Serialization anomaly |
|---|---:|---:|---:|---:|
| `READ UNCOMMITTED` | PostgreSQL không cho dirty read; thực tế cư xử như `READ COMMITTED` | Có thể | Có thể | Có thể |
| `READ COMMITTED` | Không | Có thể | Có thể | Có thể |
| `REPEATABLE READ` | Không | Không | PostgreSQL cũng ngăn được | Có thể |
| `SERIALIZABLE` | Không | Không | Không | Không |

Điểm dễ nhầm:

- PostgreSQL cho phép request cả bốn tên chuẩn SQL, nhưng bên trong chỉ có ba behavior khác nhau; `READ UNCOMMITTED` hoạt động như `READ COMMITTED`.
- PostgreSQL `REPEATABLE READ` mạnh hơn yêu cầu tối thiểu của standard vì nó cũng không cho phantom read.
- Default của PostgreSQL là `READ COMMITTED`.

## Hiểu từng mức ở mức thực dụng

### `READ COMMITTED`

Đây là default. Mỗi statement thấy snapshot mới tại lúc statement bắt đầu. Vì vậy:

- không thấy dữ liệu chưa commit của transaction khác;
- hai câu `SELECT` liên tiếp trong cùng transaction có thể thấy kết quả khác nhau nếu có transaction khác commit ở giữa;
- phù hợp nhiều use case đơn giản vì nhanh và dễ dùng.

### `REPEATABLE READ`

Transaction thấy snapshot ổn định từ đầu transaction:

- `SELECT` lặp lại trong cùng transaction thấy cùng một dữ liệu;
- không thấy commit mới của transaction khác trong lúc transaction còn mở;
- nếu cố update row đã bị transaction khác thay đổi sau khi snapshot bắt đầu, có thể gặp serialization failure và cần retry.

### `SERIALIZABLE`

Mức mạnh nhất: kết quả phải tương đương một thứ tự chạy tuần tự nào đó. Đổi lại:

- transaction có thể bị abort với serialization failure;
- application phải sẵn sàng retry;
- không nên bật chỉ vì “nghe an toàn hơn” mà chưa hiểu workload.

## Vì sao isolation quan trọng trong shared-table multi-tenant?

Shared-table nghĩa là nhiều tenant cùng dùng bảng vật lý như:

```text
account_balance
├── tenant_id = 1
├── tenant_id = 2
└── ...
```

Một query thiếu `tenant_id` là lỗi isolation theo nghĩa tenant. Nhưng ngay cả khi query đã có `tenant_id`, nhiều request đồng thời của cùng hoặc khác tenant vẫn có thể:

- cập nhật cùng một row;
- đọc tổng hợp trong lúc dữ liệu đang đổi;
- chờ lock vì cùng đụng một tài nguyên;
- tạo kết quả business khó hiểu nếu transaction boundary đặt sai.

Ví dụ ERP/kế toán:

- hai request cùng cập nhật số dư tồn kho của tenant 1;
- một báo cáo tổng hợp đọc dữ liệu trong lúc batch posting đang commit;
- cùng bảng shared-table bị nhiều tenant ghi đồng thời, làm concurrency/lock trở thành chuyện production thật chứ không chỉ lý thuyết.

Tenant isolation và transaction isolation là hai lớp khác nhau:

- tenant isolation trả lời “dữ liệu của ai?”;
- transaction isolation trả lời “dữ liệu ở thời điểm nào và dưới cạnh tranh đồng thời nào?”.

## Trade-off cần nhớ

| Muốn tăng | Thường phải trả giá bằng |
|---|---|
| Tính nhất quán snapshot mạnh hơn | Ít concurrency hơn hoặc nhiều retry hơn |
| Transaction dài hơn | Giữ tài nguyên lâu hơn, dễ va chạm hơn |
| Lock chặt hơn | Request khác có thể chờ lâu hơn |
| Throughput cao hơn | Có thể phải chấp nhận behavior yếu hơn ở một số use case |

Không có isolation level “tốt nhất cho mọi thứ”. Backend engineer cần chọn theo nghiệp vụ, query pattern và hậu quả nếu đọc/ghi concurrent bị lệch.

## Rule thực dụng cho backend shared-table SME SaaS

1. Query nghiệp vụ luôn tenant-aware bằng `tenant_id`; transaction không thay thế tenant filtering.
2. Giữ transaction ngắn, gom đúng những statement cùng một use case.
3. Không gọi external service lâu bên trong transaction nếu tránh được.
4. Với update quan trọng, biết row nào có thể bị concurrent update và chuẩn bị retry khi dùng mức cô lập cao hơn.
5. Dùng `READ COMMITTED` mặc định nếu nghiệp vụ không cần snapshot mạnh hơn; tăng isolation chỉ khi có lý do rõ.
6. Với báo cáo/tổng hợp cần snapshot ổn định, cân nhắc `REPEATABLE READ` hoặc chiến lược khác sau khi hiểu trade-off.
7. Luôn test concurrent behavior trên local/staging cho luồng dễ va chạm như balance, inventory, posting.
8. Ghi rõ transaction boundary ở service layer; đừng để nhiều statement quan trọng rơi vào autocommit rời rạc ngoài ý muốn.

## Liên hệ với bài lab 09

Sau khi đọc tài liệu này, dùng `lab-code/sql-playground/09-acid-isolation-observation.sql` để quan sát:

- default isolation hiện tại;
- transaction chưa commit không lộ sang session khác;
- `READ COMMITTED` có thể thấy giá trị mới ở lần `SELECT` sau;
- `REPEATABLE READ` giữ snapshot ổn định;
- commit/rollback thay đổi điều gì;
- vì sao hai session cùng đụng dữ liệu tenant-aware vẫn cần transaction mindset.

## Nguồn tham khảo chuẩn

- [PostgreSQL - Transactions tutorial](https://www.postgresql.org/docs/current/tutorial-transactions.html)
- [PostgreSQL - Transaction Isolation](https://www.postgresql.org/docs/current/transaction-iso.html)
- [PostgreSQL - BEGIN](https://www.postgresql.org/docs/current/sql-begin.html)
- [PostgreSQL - COMMIT](https://www.postgresql.org/docs/current/sql-commit.html)
- [PostgreSQL - ROLLBACK](https://www.postgresql.org/docs/current/sql-rollback.html)
