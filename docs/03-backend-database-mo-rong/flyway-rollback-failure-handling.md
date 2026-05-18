# Flyway rollback/failure handling

## Mục tiêu học

Tài liệu này giúp mình hiểu migration theo mindset execute/rollback, không chỉ là viết thêm một file SQL rồi để app chạy. Trọng tâm là Spring Boot + PostgreSQL + Flyway trong repo này, ở mức đủ để giải thích với mentor và tránh thói quen nguy hiểm khi thay đổi schema.

Không xem tài liệu này như production runbook đầy đủ. Phase 1 chỉ cần nắm đúng vai trò của Flyway, cách đọc trạng thái migration và cách nghĩ trước khi migration fail.

## Flyway versioned migration là gì?

Flyway quản lý thay đổi database bằng các file migration có version, ví dụ trong repo:

```text
lab-code/tenant-demo/src/main/resources/db/migration/
├── V1__create_tenants.sql
├── V2__create_master_data.sql
└── V3__create_indexes.sql
```

Khi chạy `migrate`, Flyway so sánh các migration có trong code với những migration đã được ghi nhận trong database. Migration nào chưa chạy thì Flyway chạy theo thứ tự version.

Điểm cần nhớ:

- Migration phải được lưu trong version control.
- Một migration đã apply vào database thì không nên sửa lại tùy tiện.
- Nếu cần thay đổi tiếp, tạo migration mới, ví dụ `V4__add_xxx.sql`.
- Hibernate `ddl-auto` không nên tự tạo/sửa schema trong lab này; Flyway là công cụ sở hữu schema.

## `flyway_schema_history` là gì?

Flyway tạo bảng lịch sử schema để biết migration nào đã chạy. Mặc định bảng này thường tên là `flyway_schema_history`.

Bảng này lưu các thông tin như:

- version migration;
- tên/description;
- script đã chạy;
- checksum;
- thời điểm chạy;
- trạng thái thành công/thất bại.

Với backend engineer, bảng này giống “sổ nhật ký schema”. Khi app không start vì lỗi Flyway, việc đầu tiên nên làm là đọc lỗi migration và kiểm tra schema history, không đoán mò.

Ví dụ quan sát:

```sql
SELECT installed_rank, version, description, script, checksum, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

## Ba lệnh cần hiểu trước

### `migrate`

`migrate` đưa database lên version mới nhất bằng cách chạy các migration còn pending. Trong Spring Boot, nếu bật Flyway, quá trình này thường chạy lúc app startup sau khi datasource được tạo.

Nếu migration lỗi, app có thể fail startup. Đây là hành vi tốt trong nhiều trường hợp vì backend không nên chạy trên schema nửa đúng nửa sai.

### `validate`

`validate` kiểm tra migration files hiện tại có khớp với lịch sử đã apply trong database không.

Ví dụ lỗi thường gặp:

- đã apply `V2`, sau đó sửa nội dung `V2`;
- xóa một migration đã từng apply;
- checksum trong file khác checksum trong schema history.

Khi `validate` fail, không nên reflex chạy `repair`. Cần hiểu vì sao schema history và source code lệch nhau.

### `repair`

`repair` sửa metadata trong schema history trong một số tình huống, ví dụ loại bỏ entry failed migration hoặc căn lại checksum khi team chủ động chấp nhận thay đổi.

Điểm cực kỳ quan trọng: `repair` không tự sửa schema thật. Nếu database đã tạo bảng/cột/index dở dang, mình vẫn phải cleanup dữ liệu/schema bằng SQL phù hợp trước. Vì vậy `repair` là công cụ metadata, không phải “nút undo database”.

Trong mini-lab PostgreSQL hiện tại, failed migration được rollback sạch và không tạo failed row trong schema history, nên `repair` không giải quyết gì đáng kể. `repair` phù hợp hơn khi schema history thật sự có failed entry, checksum/description/type cần realign, hoặc migration đã bị missing và team đã quyết định xử lý metadata.

## Khi migration fail thì chuyện gì xảy ra?

Theo tài liệu Flyway, migration được chạy theo thứ tự. Nếu một migration fail, Flyway dừng lại và không chạy các migration sau.

Kết quả cụ thể phụ thuộc database và transaction behavior:

- Với database hỗ trợ DDL trong transaction sạch, migration lỗi sẽ được rollback nếu migration đang chạy trong transaction.
- Với database có DDL không transactional hoặc statement không được chạy trong transaction, có thể còn lại trạng thái schema dở dang và cần cleanup thủ công.
- Một số statement đặc biệt trong PostgreSQL như `CREATE INDEX CONCURRENTLY` không chạy được bên trong transaction block, nên cần hiểu rõ trước khi đưa vào migration.

Với PostgreSQL trong mini-lab hiện tại, migration `V3__intentional_failure.sql` fail vì thêm trùng cột. Flyway log ghi rõ:

```text
Migration ... failed! Changes successfully rolled back.
```

Sau đó `flyway_schema_history` chỉ có `V1` và `V2` thành công; không có dòng `V3` failed. `flyway info` hiển thị `V3` là `Pending`, vì file vẫn tồn tại nhưng chưa apply thành công.

Điều này khớp với behavior của PostgreSQL transactional DDL. Trong scenario này, **không cần chạy `repair` chỉ vì migration fail**. Việc cần làm là đọc lỗi, sửa migration chưa apply thành công hoặc reset schema lab nếu đây chỉ là bài thực hành local.

Tuy vậy, không nên vì PostgreSQL rollback sạch trong lab mà chủ quan. Bảng lớn, lock lâu, index concurrently, backfill dữ liệu và migration nhiều step vẫn cần kế hoạch riêng.

## Forward migration và rollback plan

Trong hệ thống thật, rollback database thường khó hơn rollback code. Vì vậy nên nghĩ theo hai hướng:

### Forward migration

Khi migration đã apply và production đã chạy, cách sửa an toàn thường là tạo migration mới để đưa database về trạng thái đúng hơn.

Ví dụ:

```text
V4__add_new_column.sql
V5__fix_new_column_default.sql
```

Không sửa trực tiếp `V4` nếu `V4` đã chạy trên môi trường shared.

### Rollback plan

Rollback plan là kế hoạch nếu migration hoặc deploy gây lỗi.

Một rollback plan tối thiểu nên trả lời:

- Có backup/restore path không?
- Code cũ có chạy được với schema mới không?
- Có feature flag để tắt luồng mới thay vì rollback DB không?
- Nếu migration fail giữa chừng, cần cleanup gì?
- Có cần chia migration thành nhiều bước nhỏ không?
- Có cần migration mới để revert/forward-fix không?
- Có command nào không nên chạy trong transaction không?

Với shared-table SaaS, một lỗi migration trên bảng chung có thể ảnh hưởng tất cả tenant, nên rollback plan không phải phần phụ.

## `psql` thủ công khác gì Flyway?

Nếu chạy SQL trực tiếp bằng `psql`, Flyway không biết chuyện đó và `flyway_schema_history` không thay đổi. Vì vậy không thể dùng `psql` manual để kết luận Flyway đã ghi history hay chưa.

Ngoài ra, nếu `psql` chạy từng statement ở chế độ autocommit, statement thành công trước lỗi có thể đã được commit. Ví dụ câu `ALTER TABLE ADD COLUMN broken_note` đầu tiên có thể giữ lại cột, còn câu thứ hai mới fail. Nếu muốn thử rollback bằng `psql`, cần tự dùng `BEGIN` / `ROLLBACK`.

Trong khi đó, khi chạy qua Flyway trên PostgreSQL, Flyway wrap migration trong transaction nếu có thể. Vì vậy V3 trong lab fail nhưng thay đổi được rollback sạch.

## Undo migrations của Flyway

Flyway có khái niệm undo migration, thường dùng prefix `U`, để viết script đảo ngược một versioned migration tương ứng.

Nhưng cần hiểu giới hạn:

- Undo migrations thuộc edition trả phí của Flyway/Redgate, không phải flow mặc định của lab Spring Boot này.
- Undo không tự sinh ra; mình phải tự viết script undo.
- Undo giả định migration gốc đã chạy xong rồi mới undo toàn bộ version đó.
- Undo không giải quyết tốt trường hợp migration fail giữa chừng trên database không rollback DDL sạch.
- Undo với dữ liệu destructive như `DROP`, `DELETE`, `TRUNCATE` rất rủi ro nếu không có backup hoặc dữ liệu gốc.

Vì vậy ở Phase 1, điều cần học không phải “Flyway có undo nên yên tâm”, mà là: luôn có kế hoạch rollback/forward-fix trước khi chạy migration.

## Vì sao không sửa migration đã apply?

Flyway dùng checksum để phát hiện migration file đã thay đổi so với lúc apply. Nếu sửa một migration đã chạy, `validate` có thể fail vì checksum mismatch.

Đây không phải Flyway khó tính vô lý. Nó bảo vệ khả năng tái tạo schema:

- môi trường A đã chạy `V2` bản cũ;
- môi trường B clone code mới và chạy `V2` bản đã sửa;
- hai môi trường cùng version nhưng schema có thể khác nhau.

Rule cho repo này:

- `V1/V2/V3` đã chạy được thì xem như baseline.
- Nếu cần thay đổi schema, tạo `V4`, `V5`, ...
- Chỉ sửa migration đã apply nếu đang ở local learning rất sớm và đã reset database rõ ràng.

## Liên hệ với SaaS shared-table

Với shared-table multi-tenant, một bảng như `master_data` chứa dữ liệu nhiều tenant. Vì vậy migration có thể tạo blast radius lớn:

- `ALTER TABLE master_data` tác động tất cả tenant.
- Constraint sai có thể chặn dữ liệu hợp lệ của nhiều tenant.
- Backfill lớn có thể tạo lock, IO hoặc CPU pressure.
- Migration fail có thể làm app không start hoặc làm một số request lỗi.

Mindset thực tế:

1. Migration nhỏ.
2. Backward-compatible khi có thể.
3. Test trên local/staging với dữ liệu gần thực tế.
4. Có kế hoạch rollback/forward-fix.
5. Quan sát schema history và log, không sửa tay tùy tiện.

## Checklist trước khi chạy migration

- [ ] Mục tiêu migration rõ: thêm cột, thêm index, thêm constraint hay sửa dữ liệu?
- [ ] Migration có backward-compatible với code cũ không?
- [ ] Có cần tách schema change và data backfill không?
- [ ] Có thể rollback bằng transaction không, hay cần forward-fix migration?
- [ ] Đã chạy local/staging chưa?
- [ ] Đã kiểm tra lock/statement rủi ro như `CREATE INDEX CONCURRENTLY` chưa?
- [ ] Đã biết cách đọc `flyway_schema_history` chưa?
- [ ] Đã chạy hoặc hiểu `validate` trước deploy chưa?
- [ ] Nếu fail, đã kiểm tra schema history và object thật trước khi nghĩ tới `repair` chưa?
- [ ] Với shared-table, đã cân nhắc ảnh hưởng lên tất cả tenant chưa?

## Liên hệ với repo hiện tại

Repo hiện có baseline Flyway:

- `V1__create_tenants.sql`
- `V2__create_master_data.sql`
- `V3__create_indexes.sql`

Không dùng các file này để làm failure lab. Nếu muốn quan sát fail/repair, dùng lab cô lập trong `lab-code/flyway-failure-lab/README.md` hoặc một database/schema local riêng.

## Nguồn tham khảo chuẩn

- [Redgate Flyway - Migrations](https://documentation.red-gate.com/flyway/flyway-concepts/migrations)
- [Redgate Flyway - Migrate command](https://documentation.red-gate.com/flyway/reference/commands/migrate)
- [Redgate Flyway - Schema history table](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/flyway-schema-history-table)
- [Redgate Flyway - Migration transaction handling](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/migration-transaction-handling)
- [Redgate Flyway - Migration error and logging handling](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/migration-error-and-logging-handling)
- [Redgate Flyway - Validate command](https://documentation.red-gate.com/flyway/reference/commands/validate)
- [Redgate Flyway - Repair command](https://documentation.red-gate.com/flyway/reference/commands/repair)
- [Redgate Flyway - Undo migrations](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/undo-migrations)
- [PostgreSQL - CREATE INDEX](https://www.postgresql.org/docs/current/sql-createindex.html)
