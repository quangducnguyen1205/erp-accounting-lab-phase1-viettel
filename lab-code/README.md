# Lab Code — Thực hành Phase 1

Thư mục này là không gian thực hành code, song song với lý thuyết Phase 1.

## Trạng thái final demo

Phase 1 hiện có đường demo end-to-end:

```text
React Web UI / Master Data Portal
-> Keycloak login
-> Kong Gateway
-> tenant-demo backend + audit-log-service + file-service + search-service
-> PostgreSQL / Redis / Kafka / MinIO / Elasticsearch / Observability
```

Spring Cloud Gateway vẫn được giữ như lab gateway concept cũ; Phase 1.5 final demo mặc định dùng Kong. File upload/download đã được tách sang `file-service`; Elasticsearch search đã được tách sang `search-service`. UI không gọi trực tiếp PostgreSQL, Redis, Kafka, MinIO, Elasticsearch, Prometheus hoặc Grafana trong business flow.

Phase 1.5 đã bắt đầu chuyển một số stub thành runtime lab:

- `loki-lab/`: centralized logs bằng Loki + Grafana + Grafana Alloy.
- `kafka-ui-lab/`: inspect topic/message/consumer group.
- `kong-gateway-lab/`: Kong DB-less/declarative gateway lab.
- `audit-log-service/`: service split đầu tiên, consume Kafka event và expose audit API.
- `file-service/`: service split cho upload/download file tenant-aware qua MinIO.
- `search-service/`: service split cho Elasticsearch projection/search tenant-aware.
- `common-security/`: shared Maven module cho tenant context, JWT tenant filter và Keycloak role converter dùng chung.

`loki-lab/`, `kafka-ui-lab/` và `kong-gateway-lab/` đã có Docker Compose và Makefile targets riêng. `audit-log-service/`, `file-service/` và `search-service/` là Java service độc lập nhưng chạy Maven/IntelliJ trên host giống `tenant-demo`. Cross-service Kafka flow đã verify; React Web UI là `Master Data Portal`, một business UI nhỏ cho master data, file, search và activity log thay vì architecture console.

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
├── common-security/                   ← Shared security plumbing cho các Resource Server
│   └── src/main/java/com/viettel/common/security/
│       ├── TenantContext.java                 ← ThreadLocal tenant context dùng chung
│       ├── JwtTenantContextFilter.java        ← Đọc tenant_id từ JWT đã validate
│       └── KeycloakRoleConverter.java         ← Map Keycloak roles sang ROLE_*
├── tenant-demo/                       ← PoC chính: Spring Boot + PostgreSQL
│   ├── src/main/java/com/viettel/demo/
│   │   ├── TenantDemoApplication.java         ← Entry point
│   │   ├── security/
│   │   │   └── SecurityConfig.java            ← Resource Server + endpoint rules
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

## Makefile workflow hiện tại

Chạy từng lab riêng khi chỉ cần một phần hạ tầng:

```bash
make db-up          # PostgreSQL cho SQL/Spring Boot baseline
make keycloak-up    # Keycloak cho auth/OIDC/RBAC mini-lab
make elastic-up     # Elasticsearch cho search mini-lab
make minio-up       # MinIO cho file storage mini-lab
make redis-up       # Redis cho cache mini-lab
make kafka-up       # Kafka cho async messaging mini-lab
make kafka-ui-up    # Kafka UI cho inspect topic/message/consumer group
make common-security-install # Install shared security module cho local Maven services
make audit-log-run  # Audit service Maven host-run, consume Kafka event, expose /api/audit-events
make audit-log-run-logs # Audit service host-run + ghi lab-code/logs/audit-log-service.log cho Loki
make file-run       # File service Maven host-run, expose /api/files
make file-run-logs  # File service host-run + ghi lab-code/logs/file-service.log cho Loki
make search-run     # Search service Maven host-run, expose /api/search/master-data
make search-run-logs # Search service host-run + ghi lab-code/logs/search-service.log cho Loki
make observability-up # Prometheus + Grafana cho observability mini-lab
make loki-up        # Loki + Alloy + Grafana cho centralized logs
make gateway-run    # Spring Cloud Gateway static route mini-lab
make web-ui-up      # React Web UI demo Docker-first
```

Khi cần demo nhiều phần cùng lúc, dùng:

```bash
make infra-up
make infra-status
```

`infra-up` bật PostgreSQL + Keycloak + Elasticsearch + MinIO + Redis + Kafka. Prometheus/Grafana metrics chạy riêng bằng `make observability-up`, Loki/Grafana logs chạy riêng bằng `make loki-up`, Kafka UI chạy riêng bằng `make kafka-ui-up`, audit/file/search services chạy riêng bằng Maven/IntelliJ qua `make audit-log-run`, `make file-run`, `make search-run` hoặc các target `*-run-logs`, gateway chạy riêng bằng `make gateway-run` hoặc `make kong-up`, React Web UI chạy riêng bằng `make web-ui-up` để full infra mặc định không quá nặng. Khi chỉ học một lab nhỏ, vẫn nên dùng target riêng như `make kafka-up`, `make kafka-ui-up`, `make audit-log-run`, `make file-run`, `make search-run`, `make redis-up`, `make observability-up`, `make loki-up`, `make gateway-run` hoặc `make web-ui-up` để máy nhẹ hơn và dễ debug hơn.

Spring Boot app vẫn chạy riêng bằng:

```bash
make app-run
```

Khi demo Loki/Grafana log aggregation và muốn thấy cả log `tenant-demo` trong Grafana Explore, dùng target file-log:

```bash
make app-run-logs
```

Target này chạy app ở Keycloak + Kafka mode mặc định và ghi log vào `lab-code/logs/tenant-demo.log` để Alloy tail sang Loki. Với audit service, dùng:

```bash
make audit-log-run-logs
```

Target này ghi `lab-code/logs/audit-log-service.log`. Các target `*-run-logs` xóa file log cũ ở đầu lần chạy để demo mới dễ đọc, nhưng không tự xóa khi dừng service để bạn còn inspect sau demo. Khi muốn dọn thủ công:

Với file service, dùng:

```bash
make file-run-logs
```

Target này ghi `lab-code/logs/file-service.log`.

Với search service, dùng:

```bash
make search-run-logs
```

Target này ghi `lab-code/logs/search-service.log`.

```bash
make logs-list
make logs-clean
```

File `lab-code/logs/*.log` là local artifact, không commit.

Mục tiêu là giữ từng mini-lab cô lập được, nhưng vẫn có một đường nhanh để bật hạ tầng demo chung.

Khi muốn bật toàn bộ demo local nhanh hơn, dùng one-command runner:

```bash
make demo-info
make demo-up
make demo-status
```

`demo-up` bật Docker infra/tooling/web UI gồm PostgreSQL, Keycloak, Redis, Kafka, Kafka UI, MinIO, Elasticsearch, Kong, Loki/Grafana/Alloy và React Web UI. Sau đó target chạy bốn Java service chính bằng Maven trên host ở background:

```text
tenant-demo
audit-log-service
file-service
search-service
```

PID local nằm trong `.pids/`; log Java service nằm trong `logs/`. Nếu đang phát triển một service thủ công trong IntelliJ, có thể không dùng `demo-up` hoặc dừng process tương ứng rồi chạy `make app-run-logs`, `make audit-log-run-logs`, `make file-run-logs` hoặc `make search-run-logs` ở terminal riêng để debug rõ hơn.

Dừng demo:

```bash
make demo-down
make logs-clean   # optional, chỉ khi muốn xóa generated *.log
```

React Web UI demo nằm ở `web-ui-demo/`. UI chạy bằng Docker, mặc định gọi Kong Gateway, và không gọi trực tiếp PostgreSQL/Redis/Kafka/MinIO/Prometheus/Grafana trong business flow. Product direction mới là `Master Data Portal`.

`common-security/` không phải runtime service. Keycloak vẫn là Auth Service/Identity Provider; `tenant-demo`, `audit-log-service`, `file-service` và `search-service` tự validate JWT như Resource Server, rồi dùng shared module để tránh duplicate `TenantContext`, tenant claim filter và role converter. Khi chạy Maven service riêng từ local, Makefile sẽ install module này trước qua `make common-security-install`.
