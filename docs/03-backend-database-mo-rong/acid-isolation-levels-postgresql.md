# ACID, isolation levels và lock trong PostgreSQL

## Mục tiêu học

Tài liệu này giúp mình nhớ transaction bằng ví dụ backend thực tế, không chỉ bằng định nghĩa. Trọng tâm là PostgreSQL trong bối cảnh shared-table multi-tenant SaaS: nhiều tenant dùng chung bảng, nhiều request chạy đồng thời, và backend phải vừa đúng dữ liệu vừa tránh giữ transaction quá lâu.

Không cần học sâu toàn bộ lock mode ở Milestone #8. Chỉ cần nắm: transaction là unit of work, isolation quyết định transaction nhìn thấy gì, còn lock/MVCC là cơ chế PostgreSQL dùng để giữ dữ liệu nhất quán.

## ACID bằng ví dụ backend

| Thuộc tính | Nghĩa thực tế | Ví dụ backend |
|---|---|---|
| Atomicity | Thành công toàn bộ hoặc hủy toàn bộ | Tạo chứng từ kế toán: nếu insert header thành công nhưng insert lines lỗi thì rollback cả chứng từ. |
| Consistency | Sau transaction, dữ liệu vẫn hợp lệ theo constraint/rule | `UNIQUE (tenant_id, code)` vẫn được giữ; số tiền không bị ghi thành trạng thái vô nghĩa. |
| Isolation | Transaction không đọc trạng thái dở dang của transaction khác | Request B không thấy số dư request A vừa update nhưng chưa commit. |
| Durability | Commit xong thì thay đổi được lưu bền vững | API trả success sau commit thì dữ liệu không biến mất vì request kết thúc. |

Ví dụ service flow:

```text
POST /api/accounting/posting
-> validate request
-> BEGIN transaction
-> insert journal header
-> insert journal lines
-> update balance
-> COMMIT nếu tất cả thành công
-> ROLLBACK nếu bất kỳ bước nào lỗi
```

Ở Spring Boot, flow này thường được biểu diễn bằng transaction boundary ở service layer, ví dụ conceptually là `@Transactional`. Phase này chưa cần đi sâu annotation, nhưng cần hiểu ý nghĩa database phía dưới.

## BEGIN / COMMIT / ROLLBACK

```sql
BEGIN;

UPDATE acid_isolation_lab
SET balance = balance - 100
WHERE tenant_id = 1 AND account_code = 'CASH';

UPDATE acid_isolation_lab
SET balance = balance + 100
WHERE tenant_id = 1 AND account_code = 'BANK';

COMMIT;
```

- `BEGIN`: bắt đầu transaction.
- `COMMIT`: giữ lại toàn bộ thay đổi.
- `ROLLBACK`: hủy toàn bộ thay đổi chưa commit.
- Nếu không viết `BEGIN`, mỗi statement thường chạy trong transaction ngầm riêng.

Rule thực dụng: một use case nghiệp vụ có nhiều statement phụ thuộc nhau thì nên nghĩ “chúng có cần chung transaction không?”.

## Các hiện tượng isolation

| Hiện tượng | Cách nhớ | Ví dụ |
|---|---|---|
| Dirty read | Đọc dữ liệu bẩn chưa commit | Request B thấy balance A vừa update nhưng A rollback sau đó. PostgreSQL không cho hiện tượng này. |
| Non-repeatable read | Đọc lại cùng row, thấy giá trị khác | Trong `READ COMMITTED`, SELECT balance lần 1 thấy `1000`, request khác commit update, SELECT lần 2 thấy `1050`. |
| Phantom read | Query lại cùng điều kiện, tập row thay đổi | SELECT danh sách invoice pending lần 1 có 10 dòng, request khác insert thêm pending invoice, SELECT lần 2 có 11 dòng. |
| Serialization anomaly | Kết quả concurrent không tương đương thứ tự tuần tự nào | Hai transaction cùng kiểm tra quota còn chỗ rồi cùng insert, cuối cùng vượt quota. |

## PostgreSQL isolation levels

Theo tài liệu PostgreSQL:

| Isolation level | Dirty read | Non-repeatable read | Phantom read | Serialization anomaly |
|---|---:|---:|---:|---:|
| `READ UNCOMMITTED` | Không xảy ra trong PostgreSQL; cư xử như `READ COMMITTED` | Có thể | Có thể | Có thể |
| `READ COMMITTED` | Không | Có thể | Có thể | Có thể |
| `REPEATABLE READ` | Không | Không | PostgreSQL cũng ngăn được | Có thể |
| `SERIALIZABLE` | Không | Không | Không | Không |

Điểm cần nhớ:

- PostgreSQL default là `READ COMMITTED`.
- PostgreSQL cho phép viết `READ UNCOMMITTED`, nhưng behavior thực tế như `READ COMMITTED`.
- PostgreSQL `REPEATABLE READ` mạnh hơn mức tối thiểu của SQL standard vì không cho phantom read.
- `SERIALIZABLE` có thể abort transaction bằng lỗi serialization; app phải retry cả transaction.

## READ COMMITTED dùng khi nào?

`READ COMMITTED` là mức mặc định và phù hợp với nhiều CRUD/API thông thường.

Behavior:

- Một `SELECT` thường chỉ thấy dữ liệu đã commit trước lúc statement bắt đầu.
- Không thấy dữ liệu chưa commit của transaction khác.
- Hai câu `SELECT` trong cùng transaction có thể thấy dữ liệu khác nhau nếu transaction khác commit ở giữa.
- `UPDATE`/`DELETE` cùng row có thể phải chờ transaction khác commit/rollback.

Ví dụ phù hợp:

- list master data;
- update một record theo id;
- create/update đơn giản có constraint bảo vệ;
- API không cần snapshot báo cáo ổn định nhiều query.

## REPEATABLE READ dùng khi nào?

`REPEATABLE READ` giữ snapshot ổn định trong suốt transaction.

Behavior:

- Các `SELECT` lặp lại thấy cùng snapshot.
- Không thấy commit mới từ transaction khác sau khi transaction đã bắt đầu.
- Nếu cố update/lock row đã bị transaction khác thay đổi sau snapshot, có thể gặp serialization failure và cần retry.

Ví dụ có thể cân nhắc:

- báo cáo cần nhiều query nhưng muốn cùng một snapshot dữ liệu;
- export dữ liệu cần nhất quán trong một khoảng đọc;
- xử lý batch đọc nhiều bước, nơi `READ COMMITTED` dễ làm kết quả khó giải thích.

## SERIALIZABLE dùng khi nào?

`SERIALIZABLE` là mức mạnh nhất. PostgreSQL cố đảm bảo kết quả tương đương một thứ tự chạy tuần tự nào đó.

Trade-off:

- Có overhead theo dõi dependency.
- Transaction có thể bị abort với SQLSTATE `40001` (`serialization_failure`).
- Backend phải retry toàn bộ transaction, bao gồm cả logic quyết định SQL/value, không chỉ retry riêng câu SQL cuối.

Ví dụ có thể cần:

- invariant nghiêm ngặt kiểu “mỗi tenant chỉ được có N bản ghi active”;
- luồng tài chính/quota mà constraint đơn giản chưa đủ;
- business rule phụ thuộc vào việc đọc trước rồi ghi sau.

Không nên bật `SERIALIZABLE` toàn hệ thống chỉ vì “nghe an toàn”. Cần hiểu contention và có retry strategy.

## Transaction liên hệ với lock như thế nào?

Ba khái niệm dễ nhầm:

| Khái niệm | Nghĩa |
|---|---|
| Transaction | Đơn vị công việc: bắt đầu, commit hoặc rollback. |
| Isolation | Quy tắc transaction này thấy dữ liệu của transaction khác như thế nào. |
| Lock/MVCC | Cơ chế PostgreSQL dùng để bảo vệ consistency và concurrency. |

PostgreSQL dùng MVCC nên đọc và ghi thường không chặn nhau như database lock-based đơn giản:

- `SELECT` thường không block `UPDATE`.
- `UPDATE` cùng một row sẽ block `UPDATE` khác trên row đó.
- `SELECT FOR UPDATE` chủ động lock row để transaction khác không update/delete/lock row đó cho tới khi transaction hiện tại kết thúc.
- DDL như `ALTER TABLE`, `DROP TABLE`, `TRUNCATE` có thể lấy lock mạnh; một số dạng có thể block cả đọc/ghi.
- Transaction càng dài thì lock/snapshot giữ càng lâu, làm request khác dễ chờ hơn.

Ví dụ dễ nhớ:

```text
SELECT bình thường      -> thường đọc snapshot, không giữ row lock để chặn writer
UPDATE row A            -> giữ row lock trên row A tới COMMIT/ROLLBACK
SELECT row A FOR UPDATE -> giữ row lock dù chưa update
ALTER TABLE             -> có thể cần table lock mạnh
```

Tóm lại: transaction không tự động block mọi thứ. Chỉ các operation đụng cùng row/object với lock conflict mới chờ nhau.

## Shared-table multi-tenant trade-off

Trong shared-table:

```text
acid_isolation_lab
├── tenant_id = 1, account_code = CASH
├── tenant_id = 2, account_code = CASH
└── ...
```

`tenant_id` filtering giải quyết câu hỏi “dữ liệu thuộc tenant nào?”. Nó không tự giải quyết câu hỏi “hai transaction đồng thời nhìn thấy dữ liệu ở thời điểm nào?”.

Trade-off thực tế:

- Một bảng lớn dùng chung nhiều tenant có thể có nhiều write đồng thời.
- Query/update càng rộng, càng thiếu index, càng dễ chạm nhiều row và giữ lock lâu.
- Query có `tenant_id` + index phù hợp giúp thao tác hẹp hơn, giảm contention.
- Long transaction của một tenant có thể ảnh hưởng tenant khác nếu nó giữ lock ở mức bảng, chạy DDL, hoặc scan/update quá rộng.
- Noisy write cũng là một dạng noisy neighbor, không chỉ noisy read.

Rule cho SME SaaS:

1. Luôn filter theo `tenant_id` ở repository/service.
2. Index theo query pattern tenant-aware để update/select hẹp.
3. Giữ transaction ngắn.
4. Không gọi external API khi đang giữ transaction nếu tránh được.
5. Với nghiệp vụ quan trọng, biết rõ có cần lock row bằng `SELECT FOR UPDATE` không.
6. Với `SERIALIZABLE` hoặc `REPEATABLE READ`, chuẩn bị retry.
7. DDL/migration phải được thiết kế riêng, không xem như request thường.

## Liên hệ với bài lab 09

Dùng `lab-code/sql-playground/09-acid-isolation-observation.sql` để quan sát:

- default isolation là `read committed`;
- update chưa commit không lộ sang session khác;
- `READ COMMITTED` có non-repeatable read;
- `REPEATABLE READ` giữ snapshot ổn định;
- update cùng row gây chờ;
- `SELECT FOR UPDATE` lock row dù chưa update;
- `SERIALIZABLE` có thể abort và cần retry;
- cùng bảng shared-table nhưng khác tenant thường là khác row, còn query rộng/DDL/transaction dài vẫn có thể ảnh hưởng lớn.

## Nguồn tham khảo chuẩn

- [PostgreSQL - Transactions tutorial](https://www.postgresql.org/docs/current/tutorial-transactions.html)
- [PostgreSQL - Transaction Isolation](https://www.postgresql.org/docs/current/transaction-iso.html)
- [PostgreSQL - Explicit Locking](https://www.postgresql.org/docs/current/explicit-locking.html)
- [PostgreSQL - Serialization Failure Handling](https://www.postgresql.org/docs/current/mvcc-serialization-failure-handling.html)
- [PostgreSQL - BEGIN](https://www.postgresql.org/docs/current/sql-begin.html)
- [PostgreSQL - COMMIT](https://www.postgresql.org/docs/current/sql-commit.html)
- [PostgreSQL - ROLLBACK](https://www.postgresql.org/docs/current/sql-rollback.html)
