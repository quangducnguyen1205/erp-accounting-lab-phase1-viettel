# Backend/database notes index

## Thư mục này chứa gì?

Các ghi chú PostgreSQL và database backend phục vụ Phase 1 multi-tenant SaaS:

- schema shared-table;
- tenant-aware query;
- index và query plan;
- migration/locking/Flyway;
- transaction, ACID và isolation level.

## Reading order

1. `postgres-va-bai-toan-multi-tenant.md`
2. `index-va-query-tenant-aware.md`
3. `temporary-table-va-bang-lab-trong-postgresql.md`
4. `index-query-patterns-postgresql.md`
5. `migration-lock-rollback.md`
6. `flyway-rollback-failure-handling.md`
7. `acid-isolation-levels-postgresql.md`
8. `partitioning-vacuum-read-replica.md`

## Core

- `index-va-query-tenant-aware.md`
- `index-query-patterns-postgresql.md`
- `migration-lock-rollback.md`
- `flyway-rollback-failure-handling.md`
- `acid-isolation-levels-postgresql.md`

## Optional / later

- `partitioning-vacuum-read-replica.md`
- Chủ đề partition/vacuum/read replica chỉ nên học sâu sau khi đã vững query plan, transaction và migration.

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

