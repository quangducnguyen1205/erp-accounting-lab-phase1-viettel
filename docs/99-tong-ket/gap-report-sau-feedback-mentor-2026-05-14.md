# Gap report sau feedback mentor - 14/05/2026

## Mục đích

Tài liệu này ghi lại các lỗ hổng học tập được phát hiện sau buổi báo cáo với mentor. Đây không phải báo cáo hoàn thành kiến thức, mà là bản định hướng để cập nhật roadmap: học đúng phần còn nông, dùng nguồn chuẩn, tạo mini-lab khi cần và không mở rộng demo quá sớm.

## Những phần đã có nền

- SQL playground đã có schema `tenants` / `master_data`, `tenant_id`, `UNIQUE (tenant_id, code)`, EXPLAIN, index comparison và data leakage proof.
- Migration/locking đã có lab quan sát cơ bản với `06-migration-lock-observation.sql`.
- Spring Boot tenant demo đã có Flyway baseline, tenant-aware API, regression tests chống leakage và JWT tạm để chuyển từ `X-Tenant-Id` sang Bearer token.
- Docs hiện có trong `docs/03-backend-database-mo-rong/`, `docs/04-spring-boot/`, `docs/05-security/` đủ làm nền, nhưng chưa đủ sâu ở một số chủ đề mentor nhắc.

## Gap 1 - PostgreSQL index query patterns

**Hiện trạng:** đã hiểu index ở mức cơ bản: composite index, `tenant_id`, `code`, EXPLAIN, Seq Scan / Index Scan / Bitmap Scan.

**Còn thiếu:** chưa học kỹ query pattern nào dùng được index và query pattern nào khó dùng index:

- `LIKE 'abc%'` so với `LIKE '%abc%'`.
- contains search và khi nào cần chiến lược khác như trigram/GIN.
- composite index leftmost prefix trong các query `tenant_id + code/category/search keyword`.
- selectivity và lý do planner vẫn có thể chọn Seq Scan.
- expression/function index như `lower(column)` khi query dùng hàm.

**Artifact cần tạo:** `docs/03-backend-database-mo-rong/index-query-patterns-postgresql.md` và `lab-code/sql-playground/07-index-query-patterns.sql`.

**Nguồn chuẩn cần đọc:** PostgreSQL docs về [indexes](https://www.postgresql.org/docs/current/indexes.html), [multicolumn indexes](https://www.postgresql.org/docs/current/indexes-multicolumn.html), [expression indexes](https://www.postgresql.org/docs/current/indexes-expressional.html), [pattern matching](https://www.postgresql.org/docs/current/functions-matching.html), [`pg_trgm`](https://www.postgresql.org/docs/current/pgtrgm.html).

## Gap 2 - Flyway rollback và failure handling

**Hiện trạng:** đã có mindset migration/locking/rollback ở mức local SQL, biết cần tránh schema change dài trên shared-table.

**Còn thiếu:** chưa học đủ cách Flyway quản lý migration khi chạy thật:

- Versioned migrations và schema history table.
- `validate`, `repair` dùng để làm gì.
- Điều gì xảy ra nếu migration fail giữa chừng.
- Transaction behavior của migration tùy database.
- Forward migration vs rollback plan.
- Undo migrations trong Flyway có điều kiện/giới hạn như thế nào.

**Artifact cần tạo:** `docs/03-backend-database-mo-rong/flyway-rollback-failure-handling.md`, có thể thêm mini-lab/note quan sát lỗi migration local nếu an toàn.

**Nguồn chuẩn cần đọc:** Flyway docs về [schema history table](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/flyway-schema-history-table), [validate](https://documentation.red-gate.com/flyway/reference/commands/validate), [repair](https://documentation.red-gate.com/flyway/reference/commands/repair), [undo migrations](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/undo-migrations), [migrations](https://documentation.red-gate.com/fd/migrations-271585107.html).

## Gap 3 - ACID và isolation levels

**Hiện trạng:** đã chạm tới transaction qua `BEGIN`, `COMMIT`, `ROLLBACK`, nhưng chưa có note riêng về ACID/isolation.

**Còn thiếu:**

- ACID: atomicity, consistency, isolation, durability.
- Dirty read, non-repeatable read, phantom read.
- PostgreSQL default isolation.
- Trade-off giữa consistency, concurrency, lock và performance.
- Vì sao shared-table SaaS nhiều tenant cần hiểu concurrent read/write và lock impact.

**Artifact cần tạo:** `docs/03-backend-database-mo-rong/acid-isolation-levels-postgresql.md` và optional `lab-code/sql-playground/09-acid-isolation-observation.sql`.

**Nguồn chuẩn cần đọc:** PostgreSQL docs về [transaction isolation](https://www.postgresql.org/docs/current/transaction-iso.html).

## Gap 4 - Công nghệ kiến trúc phải học theo feature thật

**Hiện trạng:** roadmap cũ có xu hướng liệt kê nhiều công nghệ ở mức awareness. Mentor muốn khi đã chạm feature thì dùng công nghệ thật nếu feasible.

**Điều chỉnh:**

- JWT tạm chỉ là bridge để hiểu Security/JWT flow.
- Keycloak/OIDC nên được nâng lên mini-lab nếu còn thời gian, vì auth là feature đã chạm.
- Redis chỉ học sâu khi có cache/optimization scenario.
- MinIO chỉ học sâu khi có file upload/object storage scenario.
- Elasticsearch chỉ học sâu khi search vượt quá PostgreSQL `LIKE`/basic query.
- Kafka/Debezium/observability/gRPC/realtime vẫn có thể là awareness trong Phase 1 nếu chưa có feature cụ thể.

## Gap 5 - DDD để sau

DDD là hướng học hợp lý cho service decomposition/domain boundary, nhưng chưa phải việc gấp trước khi đóng demo và các gap nền tảng PostgreSQL/Flyway/ACID. Roadmap đưa DDD về cuối Phase 1 mở rộng ở mức awareness/post-demo design improvement.

## Ưu tiên sau báo cáo

1. Đóng summary JWT tạm và giới hạn của nó so với Keycloak/OIDC.
2. Học PostgreSQL index query patterns và làm mini-lab `07`.
3. Học Flyway rollback/failure handling.
4. Học ACID/isolation levels và quan sát local nếu phù hợp.
5. Làm Keycloak mini-lab nhỏ nếu còn thời gian.
6. Sau đó mới quyết định React UI hoặc demo script backend chắc chắn.

## Nguyên tắc thực hiện

- Mỗi gap bắt đầu từ official/standard docs.
- Tạo note ngắn trước, không viết textbook dài.
- Nếu có thực hành, tạo skeleton/TODO để tự code hoặc tự chạy.
- Sau khi làm xong mới nhờ Codex review và cập nhật summary mentor-facing.
- Không mở rộng stack chỉ để “có công nghệ”; công nghệ phải gắn với feature hoặc câu hỏi học cụ thể.
