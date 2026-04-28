# Lab Code — Thực hành Phase 1

Thư mục này là không gian thực hành code, song song với lý thuyết Phase 1.

## Nguyên tắc tối thượng

1. **TỰ VIẾT CODE TRƯỚC.** Không copy solution.
2. Mỗi file có TODO task hướng dẫn. Đọc task → tự research → tự implement.
3. Sau khi tự viết xong → nhờ Agent review, tìm lỗi, đề xuất sửa.
4. Code phải chạy được thật, không chỉ pseudo-code.
5. Mỗi bước nhỏ, commit riêng, message rõ ràng.

## Cấu trúc

```text
lab-code/
├── README.md                          ← File này
├── tenant-demo/                       ← PoC chính: Spring Boot + PostgreSQL
│   ├── src/main/java/com/viettel/demo/
│   │   ├── TenantDemoApplication.java         ← Entry point
│   │   ├── config/
│   │   │   └── TenantFilter.java              ← TODO: Servlet filter
│   │   ├── context/
│   │   │   └── TenantContext.java             ← TODO: ThreadLocal
│   │   ├── entity/
│   │   │   ├── TenantAwareEntity.java         ← TODO: Base entity
│   │   │   └── MasterData.java                ← TODO: Business entity
│   │   ├── repository/
│   │   │   ├── TenantAwareRepository.java     ← TODO: Base repo
│   │   │   └── MasterDataRepository.java      ← TODO: Repo cụ thể
│   │   ├── service/
│   │   │   └── MasterDataService.java         ← TODO: Business logic
│   │   └── controller/
│   │       └── MasterDataController.java      ← TODO: REST API
│   ├── src/main/resources/
│   │   ├── application.yml                    ← TODO: Cấu hình DB
│   │   └── db/migration/
│   │       ├── V1__create_tenants.sql         ← TODO: Migration
│   │       ├── V2__create_master_data.sql     ← TODO: Migration
│   │       └── V3__create_indexes.sql         ← TODO: Index strategy
│   ├── src/test/java/com/viettel/demo/
│   │   └── DataLeakageTest.java               ← TODO: Integration test
│   └── pom.xml                                ← TODO: Dependencies
├── sql-playground/
│   ├── 01-setup-tables.sql                    ← TODO: Tạo bảng
│   ├── 02-insert-sample-data.sql              ← TODO: Data mẫu
│   ├── 03-query-with-explain.sql              ← TODO: EXPLAIN
│   ├── 04-index-comparison.sql                ← TODO: So sánh index
│   └── 05-data-leakage-test.sql               ← TODO: Test leakage
└── docker/
    └── docker-compose.yml                     ← TODO: PostgreSQL local
```

## Lộ trình thực hành

| Bước | Task | Liên hệ lý thuyết | Files liên quan |
|:---:|------|-------------------|----------------|
| 1 | Setup PostgreSQL local | `docs/03-backend-database-mo-rong/postgres-va-bai-toan-multi-tenant.md` | `docker/`, `sql-playground/01-02` |
| 2 | Tạo Spring Boot project + cấu hình DB | Spring Initializr docs | `pom.xml`, `application.yml`, `TenantDemoApplication.java` |
| 3 | Implement TenantContext + TenantFilter | `docs/02-multi-tenant/tong-quan-multi-tenant.md` (tenant-aware everything) | `context/`, `config/` |
| 4 | Implement Base Entity + Base Repository | `docs/02-multi-tenant/tinh-huong-va-trade-off.md` (data leakage) | `entity/TenantAwareEntity`, `repository/TenantAwareRepository` |
| 5 | Implement MasterData CRUD + Flyway | `docs/03-backend-database-mo-rong/migration-lock-rollback.md` | `entity/`, `repository/`, `service/`, `controller/`, `db/migration/` |
| 6 | Viết Integration Test chống data leakage | `docs/02-multi-tenant/tinh-huong-va-trade-off.md` (câu 8) | `DataLeakageTest.java` |
| 7 | Chạy EXPLAIN ANALYZE trên query thật | `docs/03-backend-database-mo-rong/index-va-query-tenant-aware.md` | `sql-playground/03-04` |

## Workflow mỗi bước

```
1. Đọc TODO task trong file skeleton
2. Đọc tài liệu lý thuyết liên quan
3. Tự research keyword được gợi ý
4. Tự viết code
5. Test thử
6. Commit
7. Nhờ Agent review nếu cần
```
