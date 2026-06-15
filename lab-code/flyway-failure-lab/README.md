# Flyway failure lab - hướng dẫn quan sát an toàn

## Mục tiêu

Lab này giúp mình quan sát Flyway khi migration thành công, migration fail, `validate` báo lỗi và `repair` có ý nghĩa gì.

Đây là lab cô lập. Không dùng chuỗi migration thật của `tenant-demo` để cố tình tạo lỗi.

```text
tenant-demo V1/V2/V3  -> giữ ổn định để app start
flyway-failure-lab   -> dùng schema riêng để quan sát failure
```

## Vì sao không dùng migration thật?

Nếu cố tình làm hỏng `lab-code/tenant-demo/src/main/resources/db/migration/`, Spring Boot app có thể fail startup và làm nhiễu demo chính. Bài này chỉ cần học behavior của Flyway, nên dùng schema riêng như `flyway_failure_lab`.

## Chuẩn bị

Chạy PostgreSQL local:

```bash
cd lab-code
make -f Makefile.legacy db-up
make db-status
```

Lab này giả định database local theo Docker Compose hiện tại:

```text
host: localhost
port: 5432
database: erpdb
user: erpuser
password: erpuser
schema lab: flyway_failure_lab
```

Không commit password thật nếu sau này đổi `.env`.

## Cách chạy đề xuất

Có hai cách:

1. Dùng Flyway CLI nếu máy đã cài.
2. Dùng Flyway Maven plugin từ thư mục `tenant-demo`.

Ở Phase 1, chỉ cần chọn một cách. Nếu command lỗi do thiếu Flyway CLI/plugin, ghi lại blocker và hỏi Codex review, không cần cố cài quá sâu.

Ví dụ với Maven plugin từ `lab-code/tenant-demo`:

```bash
cd lab-code/tenant-demo

./mvnw org.flywaydb:flyway-maven-plugin:9.22.3:migrate \
  -Dflyway.url=jdbc:postgresql://localhost:5432/erpdb \
  -Dflyway.user=erpuser \
  -Dflyway.password=erpuser \
  -Dflyway.schemas=flyway_failure_lab \
  -Dflyway.defaultSchema=flyway_failure_lab \
  -Dflyway.createSchemas=true \
  -Dflyway.locations=filesystem:../flyway-failure-lab/migrations
```

Ghi chú: phiên bản plugin nên khớp hoặc tương thích với Flyway đang dùng trong project. Nếu sau này dependency Flyway đổi, kiểm tra lại version.

Quan trọng:

- `./mvnw ... flyway-maven-plugin ...` là Flyway command, có thể ghi vào `flyway_schema_history`.
- `docker exec ... psql ...` là SQL/manual command, không tự ghi vào Flyway history.
- Nếu chạy file SQL bằng `psql`, mình đang test PostgreSQL/SQL trực tiếp, không phải test Flyway migration behavior.

## Bước 1: tạo migration thành công

Tạo thư mục local:

```bash
mkdir -p lab-code/flyway-failure-lab/migrations
```

Ghi chú: các file trong `migrations/` là artifact thực hành. Nếu còn chứa migration cố tình fail, không nên commit trước khi đã review hoặc ghi chú rõ mục đích.

Tạo file:

```text
lab-code/flyway-failure-lab/migrations/V1__create_lab_table.sql
```

Gợi ý nội dung:

```sql
CREATE TABLE flyway_lab_item (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
```

Kết quả mong đợi sau khi chạy `migrate`:

- [x] Migration `V1` success.
- [x] Schema `flyway_failure_lab` được tạo.
- [x] Bảng history nằm ở `flyway_failure_lab.flyway_schema_history`.

Quan sát schema history:

```bash
docker exec -it erp-postgres psql -U erpuser -d erpdb
```

```sql
SELECT installed_rank, version, description, script, checksum, success
FROM flyway_failure_lab.flyway_schema_history
ORDER BY installed_rank;
```

## Bước 2: tạo migration thứ hai thành công

Tạo file:

```text
lab-code/flyway-failure-lab/migrations/V2__add_note_column.sql
```

Gợi ý nội dung:

```sql
ALTER TABLE flyway_failure_lab.flyway_lab_item
ADD COLUMN note VARCHAR(255);
```

Kết quả mong đợi:

- [x] Chạy lại `migrate`.
- [x] Schema history có `V2` success.
- [x] Bảng `flyway_lab_item` có cột `note`.

## Bước 3: tạo migration cố tình fail

Tạo file:

```text
lab-code/flyway-failure-lab/migrations/V3__intentional_failure.sql
```

Gợi ý tạo lỗi đơn giản:

```sql
ALTER TABLE flyway_failure_lab.flyway_lab_item
ADD COLUMN broken_note VARCHAR(255);

ALTER TABLE flyway_failure_lab.flyway_lab_item
ADD COLUMN broken_note VARCHAR(255);
```

Lệnh thứ hai sẽ lỗi vì cột đã tồn tại. Với PostgreSQL, nhiều DDL có thể rollback trong transaction, nên cần quan sát xem cột `broken_note` có còn lại không.

Kết quả đã verify trên PostgreSQL 16 + Flyway Maven plugin 9.22.3:

- [x] Chạy `migrate` và Flyway dừng ở `V3`.
- [x] Error do thêm trùng cột `broken_note`.
- [x] Flyway log ghi: `Changes successfully rolled back`.
- [x] Schema history không có row `V3` failed.
- [x] Bảng thật không còn cột `broken_note`.

Kết luận: trong lab PostgreSQL này, failed migration được rollback sạch. `V3` vẫn là pending migration, không phải failed migration trong schema history.

Gợi ý kiểm tra:

```sql
SELECT column_name
FROM information_schema.columns
WHERE table_schema = 'flyway_failure_lab'
  AND table_name = 'flyway_lab_item'
ORDER BY ordinal_position;
```

## Bước 4: thử `validate`

Sau khi migration fail, thử chạy validate bằng cùng config.

Ví dụ với Maven plugin:

```bash
cd lab-code/tenant-demo

./mvnw org.flywaydb:flyway-maven-plugin:9.22.3:validate \
  -Dflyway.url=jdbc:postgresql://localhost:5432/erpdb \
  -Dflyway.user=erpuser \
  -Dflyway.password=erpuser \
  -Dflyway.schemas=flyway_failure_lab \
  -Dflyway.defaultSchema=flyway_failure_lab \
  -Dflyway.locations=filesystem:../flyway-failure-lab/migrations
```

Kết quả đã verify:

- [x] `validate` báo còn migration `V3` resolved nhưng chưa applied.
- [x] Đây là trạng thái pending migration, không phải failed row trong schema history.
- [x] Không cần `repair` trong scenario PostgreSQL rollback sạch này.

Thông báo chính:

```text
Detected resolved migration not applied to database: 3.
To fix this error, either run migrate, or set -ignoreMigrationPatterns='*:pending'.
```

Trong lab này, `V3` là file pending nhưng đang cố tình lỗi. Cách xử lý học tập là sửa `V3` thành migration hợp lệ, hoặc reset schema lab nếu muốn làm lại. Không dùng `repair` chỉ để “xóa lỗi” khi chưa hiểu trạng thái thật.

Có thể xem trạng thái rõ hơn bằng:

```bash
./mvnw org.flywaydb:flyway-maven-plugin:9.22.3:info \
  -Dflyway.url=jdbc:postgresql://localhost:5432/erpdb \
  -Dflyway.user=erpuser \
  -Dflyway.password=erpuser \
  -Dflyway.schemas=flyway_failure_lab \
  -Dflyway.defaultSchema=flyway_failure_lab \
  -Dflyway.locations=filesystem:../flyway-failure-lab/migrations
```

Kết quả mong đợi: `V1` và `V2` là `Success`, `V3` là `Pending`.

## Bước 5: hiểu `repair`

`repair` chỉ sửa metadata trong schema history. Nó không tự drop cột, drop bảng hay sửa dữ liệu lỗi.

Không chạy `repair` chỉ vì thấy migration fail. Chỉ cân nhắc `repair` sau khi:

- đã hiểu migration fail ở đâu;
- đã biết database thật đang ở trạng thái nào;
- đã cleanup schema/data nếu cần;
- đã quyết định schema history cần được sửa.

Trong lab PostgreSQL này, `repair` không cần thiết sau failed `V3`, vì Flyway rollback sạch và không ghi failed entry.

`repair` thường liên quan đến các tình huống như:

- schema history có failed migration entry cần remove;
- migration đã apply bị sửa nội dung làm checksum/description/type lệch;
- migration file bị missing và team quyết định cập nhật metadata.

Ví dụ command:

```bash
cd lab-code/tenant-demo

./mvnw org.flywaydb:flyway-maven-plugin:9.22.3:repair \
  -Dflyway.url=jdbc:postgresql://localhost:5432/erpdb \
  -Dflyway.user=erpuser \
  -Dflyway.password=erpuser \
  -Dflyway.schemas=flyway_failure_lab \
  -Dflyway.defaultSchema=flyway_failure_lab \
  -Dflyway.locations=filesystem:../flyway-failure-lab/migrations
```

Kết luận cho lab này:

- [x] Không cần chạy `repair` vì không có failed row.
- [x] `repair` không tự sửa database object thật.
- [x] Không dùng `repair` như một “nút sửa lỗi tự động”; phải hiểu schema history và trạng thái object thật trước.

## Bước 6: sửa migration fail hoặc reset lab

Vì đây là lab local, có hai hướng an toàn:

### Hướng A: sửa migration chưa success

Nếu `V3` chưa được apply thành công và database không còn object dở dang, có thể sửa `V3__intentional_failure.sql` thành SQL hợp lệ rồi chạy lại `migrate`.

TODO:

- [ ] Sửa `V3`.
- [ ] Chạy `migrate`.
- [ ] Chạy `validate`.
- [ ] Ghi lại kết luận.

### Hướng B: reset schema lab

Nếu muốn làm lại từ đầu:

```sql
DROP SCHEMA IF EXISTS flyway_failure_lab CASCADE;
```

Có thể chạy bằng:

```bash
docker exec -it erp-postgres psql -U erpuser -d erpdb
```

TODO:

- [ ] Reset schema lab.
- [ ] Chạy lại từ `V1`.
- [ ] Không đụng schema `public` của tenant-demo nếu đang dùng app.

## Reflection sau lab

Kết luận đã verify:

- Migration fail ở `V3__intentional_failure.sql`.
- Flyway dừng ở `V3`; không chạy tiếp migration sau.
- `flyway_schema_history` chỉ ghi schema creation, `V1` success và `V2` success; không có `V3` failed row trong PostgreSQL lab này.
- PostgreSQL rollback toàn bộ `V3`, nên cột `broken_note` không còn lại.
- `validate` phát hiện `V3` vẫn là pending migration chưa apply.
- `repair` sửa metadata trong schema history, không sửa object thật; trong lab này không cần `repair`.
- Nếu đây là bảng shared-table nhiều tenant, migration fail có thể làm deploy/app startup fail và ảnh hưởng tất cả tenant dùng chung bảng.
- Rollback plan tối thiểu cần có: backup/restore path, backward-compatible schema, cleanup/forward-fix plan, test trên local/staging và hiểu rõ lock/transaction behavior.

## Done criteria

Hoàn thành lab khi mình có thể giải thích ngắn:

- `migrate`, `validate`, `repair` khác nhau thế nào;
- failure không nên xử lý bằng cách sửa tay bừa trong schema history;
- `repair` không thay thế cleanup/rollback thật;
- migration đã apply không nên sửa lại;
- shared-table SaaS cần plan rollback/forward-fix trước khi deploy migration.
