# Mục lục backend/database

## Thư mục này chứa gì?

Các ghi chú PostgreSQL và database backend phục vụ Phase 1 multi-tenant SaaS:

- schema shared-table;
- tenant-aware query;
- index và query plan;
- migration/locking/Flyway;
- transaction, ACID và isolation level;
- một số chủ đề vận hành PostgreSQL để học sau.

## Thứ tự đọc đề xuất

1. [postgres-va-bai-toan-multi-tenant.md](postgres-va-bai-toan-multi-tenant.md)
2. [index-va-query-tenant-aware.md](index-va-query-tenant-aware.md)
3. [temporary-table-va-bang-lab-trong-postgresql.md](temporary-table-va-bang-lab-trong-postgresql.md)
4. [index-query-patterns-postgresql.md](index-query-patterns-postgresql.md)
5. [migration-lock-rollback.md](migration-lock-rollback.md)
6. [flyway-rollback-failure-handling.md](flyway-rollback-failure-handling.md)
7. [acid-isolation-levels-postgresql.md](acid-isolation-levels-postgresql.md)
8. [partitioning-vacuum-read-replica.md](partitioning-vacuum-read-replica.md)

## Nhóm cốt lõi

- [postgres-va-bai-toan-multi-tenant.md](postgres-va-bai-toan-multi-tenant.md)
- [index-va-query-tenant-aware.md](index-va-query-tenant-aware.md)
- [index-query-patterns-postgresql.md](index-query-patterns-postgresql.md)
- [migration-lock-rollback.md](migration-lock-rollback.md)
- [flyway-rollback-failure-handling.md](flyway-rollback-failure-handling.md)
- [acid-isolation-levels-postgresql.md](acid-isolation-levels-postgresql.md)

## Mở rộng sau

- [partitioning-vacuum-read-replica.md](partitioning-vacuum-read-replica.md)

Chủ đề partition, vacuum và read replica chỉ nên học sâu sau khi đã vững query plan, transaction và migration. Đây là kiến thức vận hành production, không phải điều kiện để chạy final demo local.

## Lab liên quan

- `lab-code/sql-playground/01-setup-tables.sql`
- `lab-code/sql-playground/02-insert-sample-data.sql`
- `lab-code/sql-playground/03-query-with-explain.sql`
- `lab-code/sql-playground/04-index-comparison.sql`
- `lab-code/sql-playground/05-data-leakage-test.sql`
- `lab-code/sql-playground/06-migration-lock-observation.sql`
- `lab-code/sql-playground/07-index-query-patterns.sql`
- `lab-code/sql-playground/09-acid-isolation-observation.sql`
- `lab-code/flyway-failure-lab/README.md`

## Nguồn chuẩn nên đối chiếu

- PostgreSQL official docs: Indexes, Concurrency Control, Performance Tips, Routine Database Maintenance.
- Flyway official docs: migration lifecycle, repair, validation.
- Spring Data JPA docs: repository/query behavior và transaction integration.
