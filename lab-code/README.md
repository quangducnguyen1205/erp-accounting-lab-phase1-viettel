# Lab Code — Thực hành Phase 1

Thư mục này chứa source code thực hành song song với lý thuyết Phase 1.

## Mục đích

- Kiểm chứng kiến thức SaaS/multi-tenant bằng code thật.
- Thực hành shared table + `tenant_id` trên PostgreSQL.
- Tự viết code trước, sau đó nhờ Agent review.

## Cấu trúc đề xuất

```text
lab-code/
├── README.md                          ← File này
├── tenant-demo/                       ← PoC chính: Spring Boot + PostgreSQL
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/viettel/demo/
│   │       │       ├── TenantDemoApplication.java
│   │       │       ├── config/
│   │       │       │   └── TenantFilter.java         ← Servlet filter: JWT → tenant context
│   │       │       ├── context/
│   │       │       │   └── TenantContext.java         ← ThreadLocal giữ tenant_id
│   │       │       ├── entity/
│   │       │       │   ├── TenantAwareEntity.java     ← Base entity có tenant_id
│   │       │       │   └── MasterData.java            ← Entity ví dụ
│   │       │       ├── repository/
│   │       │       │   ├── TenantAwareRepository.java ← Base repo auto-filter tenant
│   │       │       │   └── MasterDataRepository.java
│   │       │       ├── service/
│   │       │       │   └── MasterDataService.java
│   │       │       └── controller/
│   │       │           └── MasterDataController.java
│   │       └── resources/
│   │           ├── application.yml
│   │           └── db/migration/                      ← Flyway migrations
│   │               ├── V1__create_tenants.sql
│   │               ├── V2__create_master_data.sql
│   │               └── V3__create_indexes.sql
│   └── pom.xml
├── sql-playground/                    ← Script SQL để test trực tiếp trên psql
│   ├── 01-setup-tables.sql
│   ├── 02-insert-sample-data.sql
│   ├── 03-query-with-explain.sql
│   ├── 04-index-comparison.sql
│   └── 05-data-leakage-test.sql
└── docker/
    └── docker-compose.yml             ← PostgreSQL local cho dev
```

## Lộ trình push code lên GitHub

| Bước | Nội dung | Đồng bộ với lý thuyết |
|:---:|---------|----------------------|
| 1 | `docker-compose.yml` + SQL playground | `docs/03-backend-database-mo-rong/` |
| 2 | Spring Boot skeleton + TenantContext + TenantFilter | `docs/02-multi-tenant/tong-quan-multi-tenant.md` |
| 3 | TenantAwareEntity + TenantAwareRepository | `docs/02-multi-tenant/tinh-huong-va-trade-off.md` (data leakage) |
| 4 | MasterData CRUD + Flyway migration | `docs/03-backend-database-mo-rong/migration-lock-rollback.md` |
| 5 | Integration test chống data leakage | `docs/02-multi-tenant/tinh-huong-va-trade-off.md` (câu 8) |
| 6 | EXPLAIN ANALYZE trên query thật | `docs/03-backend-database-mo-rong/index-va-query-tenant-aware.md` |

## Nguyên tắc

1. Tự viết code trước. Nhờ Agent review sau.
2. Code phải chạy được (không chỉ là pseudo-code).
3. Mỗi bước nhỏ, commit riêng, message rõ ràng.
4. Nếu project lớn dần → tách sang repository riêng.
