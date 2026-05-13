# ROADMAP - Phase 1 Fast-track: ERP/Kế toán SaaS Multi-tenant

> **Bắt đầu:** Thứ Ba 28/04/2026
> **Deadline:** Thứ Hai 25/05/2026
> **Cập nhật roadmap:** 13/05/2026
> **Chu kỳ báo cáo:** mỗi 2-3 ngày phải có output có thể trình bày
> **Phương châm:** tự học + tự code trước, Codex tạo note/skeleton và review sau

---

## Quick Stats

| Chỉ số | Giá trị |
|--------|:-------:|
| **Tiến độ** | 51% |
| **Tổng task** | 53 |
| **Đã hoàn thành** | 27 / 53 |
| **Focus hiện tại** | DataLeakageTest / testing tenant isolation |
| **Milestone tiếp theo** | #5 - Data leakage tests + temporary JWT + Keycloak awareness |
| **Demo cuối Phase 1** | Spring Boot + PostgreSQL/Flyway + tenant-aware API + JWT tạm + React UI nhỏ |

Ghi chú: phần trăm giảm so với roadmap cũ vì phạm vi Phase 1 được mở rộng lại theo sơ đồ kiến trúc target, gồm cả React UI, JWT tạm và awareness package cho các công nghệ nền.

---

## Chú thích

| Tag | Ý nghĩa | File/khu vực thường dùng |
|-----|---------|--------------------------|
| `[LÝ THUYẾT]` | Đọc official/standard docs, ghi lại ý chính ngắn | `docs/` |
| `[SKELETON]` | Codex tạo khung code/TODO comments để mình tự code | `lab-code/` |
| `[THỰC HÀNH]` | Tự code/chạy lệnh/verify output | `lab-code/` |
| `[BÁO CÁO]` | Tổng hợp ngắn để báo cáo mentor/leader | `docs/99-tong-ket/`, `reports/`, `presentation-notes/` |
| `[REVIEW]` | Nhờ Codex review sau khi đã tự làm | code/docs đã viết |
| `[MILESTONE]` | Checkpoint có output cụ thể | demo, summary, curl/UI output, test result |

---

## Nguyên tắc fast-track

1. **Không học tất cả lý thuyết upfront.** Công nghệ nào sắp dùng trong demo/mini-lab thì tạo note ngắn trước, rồi code.
2. **Mỗi task kỹ thuật đi theo vòng lặp:** note từ nguồn chuẩn → skeleton/TODO → tự code → verify → Codex review → summary.
3. **Demo chính phải chạy được.** Không biến Phase 1 thành full microservices stack.
4. **Core demo thực hành sâu, công nghệ lớn học theo tầng.** JWT tạm và React UI vào demo; Keycloak/Kafka/Redis/MinIO/Elastic/Observability chủ yếu awareness hoặc mini-note.
5. **Không overclaim production.** JWT tạm không phải Keycloak/OIDC production; React UI chỉ là demo tenant flow; backend chưa phải ERP thật.
6. **Giữ learning-first.** Codex không tự implement toàn bộ future feature nếu chưa được yêu cầu rõ.
7. **Local notes chỉ là context.** `local/` có thể giúp nhớ phạm vi kiến trúc, nhưng source of truth public là `docs/`, `ROADMAP.md`, code và report đã chuẩn hóa.

---

## Hiện trạng repo đã audit

### Đã hoàn thành hoặc gần hoàn thành

- SQL playground có `01` đến `06`: schema baseline, sample data, EXPLAIN, index comparison, temp table experiment, data leakage proof, migration/locking observation.
- Spring Boot tenant demo đã có Maven wrapper, `pom.xml`, `application.yml`, `.env.example`, PostgreSQL Docker Compose và Flyway `V1-V3`.
- `TenantContext` / `TenantFilter` đã implement và từng được runtime verify bằng header `X-Tenant-Id`.
- Entity/repository layer đã có `TenantAwareEntity`, `MasterData`, `MasterDataRepository`; repository method chính đều có `tenantId`.
- Service/controller layer hiện đã có API `master_data` tenant-aware cơ bản.
- Các note Spring Boot đã có trong `docs/04-spring-boot/`: filter/threadlocal, JPA entity/repository, service/controller, database stack, component/bean/DI.

### Còn thiếu hoặc chưa đóng

- `DataLeakageTest.java` vẫn là guided skeleton, chưa thành test pass thật.
- `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` đã có summary Milestone #4 cho Spring Boot tenant-aware API/curl demo.
- Chưa có JWT/Spring Security tạm, chưa có Keycloak/OIDC awareness note public.
- Chưa có React UI demo.
- Chưa có public architecture coverage docs cho Redis, Kafka, Debezium, MinIO, Elasticsearch, gRPC, realtime, observability, LLM provider và external services.
- `README.md` vẫn mô tả repo thiên về knowledge base; chưa phản ánh rõ demo fast-track mới.

### Bài học từ sơ đồ kiến trúc target

Sơ đồ target có React frontend, API Gateway/service discovery/load balancer, Keycloak/OAuth2/OIDC, nhiều backend services, PostgreSQL cluster, Redis, Kafka, Debezium CDC, MinIO, Elasticsearch/Elastic Stack, gRPC, realtime, observability, LLM providers và external services. Phase 1 cần hiểu vai trò của các phần này, nhưng không cần chạy toàn bộ.

---

## Demo cuối Phase 1 - phạm vi thực tế

### Demo chính sẽ có

- Spring Boot backend chạy được bằng `make app-run`.
- PostgreSQL local + Flyway tạo schema.
- Shared-table multi-tenant với `tenant_id`.
- Tenant-aware API cho `master_data`.
- Test hoặc curl chứng minh không lộ data cross-tenant.
- JWT tạm: token local có `tenant_id` claim, backend validate đơn giản để set tenant context.
- React UI nhỏ: chọn/nhập token, gọi API, hiển thị data scoped theo tenant.
- Report/presentation note có sơ đồ flow và giới hạn hiện tại.

### Không đưa vào demo chính

- Keycloak thật.
- API Gateway/Kong thật.
- Redis/Kafka/Debezium/MinIO/Elasticsearch/Grafana stack chạy thật.
- Full ERP/accounting workflow.
- Full production RBAC, audit log, deployment pipeline hoặc HA database cluster.

---

## Phase 1 Technology Coverage Map

| Topic trong kiến trúc | Mức phủ Phase 1 | Theory doc dự kiến | Code/demo artifact | Verification | Milestone | Trạng thái |
|---|---|---|---|---|---:|---|
| SaaS / ERP accounting context | Core theory | `docs/01-saas/`, `docs/00-gioi-thieu/` | Không cần code riêng | Report/presentation giải thích đúng | #1, #8 | Đã có nền |
| Multi-tenant shared-table | Core demo | `docs/02-multi-tenant/` | SQL playground + Spring Boot `master_data` | SQL, curl, test | #1, #4 | SQL + API cơ bản đã xong |
| PostgreSQL schema/index/EXPLAIN | Core demo | `docs/03-backend-database-mo-rong/` | `lab-code/sql-playground/01-04` | `make sql-*` | #1 | Đã xong baseline |
| Migration/locking/rollback | Mini-lab | `docs/03-backend-database-mo-rong/migration-lock-rollback.md` | `06-migration-lock-observation.sql` | Local SQL observation | #2 | Đã đóng |
| Spring Boot backend | Core demo | `docs/04-spring-boot/` | `lab-code/tenant-demo/` | `make app-run` | #3, #4 | App/API cơ bản đã có |
| Flyway | Core demo | `docs/04-spring-boot/spring-boot-bootstrap-config.md` | `V1-V3` migrations | Startup logs/Flyway logs | #3 | Đã chạy baseline |
| TenantContext/TenantFilter | Core demo | `docs/04-spring-boot/request-filter-threadlocal.md` | `TenantContext.java`, `TenantFilter.java` | curl/log | #3 | Đã implement/review |
| Tenant-aware service/controller | Core demo | `docs/04-spring-boot/service-controller-curl-flow.md` | `MasterDataService`, `MasterDataController` | curl tenant 1/2 | #4 | Đã verify và đóng #4 |
| Data leakage tests | Core demo | `docs/04-spring-boot/testing-tenant-isolation.md` | `DataLeakageTest.java` | `make app-test` | #4 | Skeleton, chưa tự code |
| Temporary JWT auth | Core demo | `docs/05-security/jwt-spring-security-temporary.md` | Security config/filter/service TODO | curl valid/invalid JWT | #5 | Chưa có |
| Keycloak/OAuth2/OIDC | Awareness | `docs/05-security/keycloak-oauth2-oidc-awareness.md` | Không chạy Keycloak | Summary + diagram | #5, #7 | Chưa có public note |
| RBAC/tenant-scope | Core concept + awareness | `docs/05-security/rbac-tenant-scope.md` | JWT tạm có claim đơn giản | curl/test role/tenant notes | #5 | Chưa có |
| React frontend | Core demo | `docs/06-frontend/react-tenant-demo-ui.md` | `lab-code/tenant-ui/` | browser/UI + curl fallback | #6 | Chưa có |
| API Gateway/service discovery/load balancer | Awareness | `docs/07-architecture/api-gateway-service-discovery.md` | Không chạy gateway | Architecture summary | #7 | Chưa có public note |
| Redis cache strategy | Mini-lab/design note | `docs/07-architecture/redis-tenant-cache.md` | Optional pseudo-code/mini example | Explain tenant-safe key | #7 | Chưa có public note |
| Kafka async messaging | Awareness | `docs/07-architecture/kafka-async-messaging.md` | Không chạy Kafka | Use-case summary | #7 | Chưa có public note |
| Debezium CDC + Kafka | Awareness | `docs/07-architecture/debezium-cdc.md` | Không chạy CDC | CDC role summary | #7 | Chưa có public note |
| MinIO / S3 object storage | Awareness | `docs/07-architecture/minio-object-storage.md` | Không chạy MinIO | Tenant file strategy note | #7 | Chưa có public note |
| Elasticsearch / Elastic Stack | Awareness | `docs/07-architecture/elasticsearch-search-service.md` | Không chạy Elastic | Search vs DB query note | #7 | Chưa có public note |
| gRPC internal communication | Awareness | `docs/07-architecture/grpc-internal-communication.md` | Không chạy gRPC | REST vs gRPC vs Kafka table | #7 | Chưa có public note |
| Realtime: SignalR/Socket/SSE/Long polling | Awareness | `docs/07-architecture/realtime-communication.md` | Không chạy realtime | When to use which note | #7 | Chưa có public note |
| Observability: Prometheus/Grafana/Loki | Mini-note | `docs/07-architecture/observability-prometheus-grafana-loki.md` | Optional log/health notes only | Explain metric/log/tracing role | #7 | Chưa có public note |
| LLM providers: OpenAI/OpenRouter/others | Awareness | `docs/07-architecture/llm-provider-integration.md` | Không gọi API thật | Integration role note | #7 | Chưa có public note |
| External services: e-contract, eCommerce, CRM, HR, documents, digital signing | Awareness | `docs/07-architecture/external-integrations-erp.md` | Không tích hợp thật | Boundary/use-case summary | #7 | Chưa có public note |
| Full production microservices stack | Out of scope | Chỉ ghi giới hạn trong report | Không implement | Nêu rõ không thuộc Phase 1 | #8 | Out of scope |
| Full ERP/accounting domain | Out of scope | Chỉ dùng ví dụ nghiệp vụ | Không implement | Nêu rõ `master_data` chỉ là slice demo | #8 | Out of scope |

---

## Lịch sử đã hoàn thành

### Milestone #1 - SQL Playground Tenant-aware

- [x] `[LÝ THUYẾT]` Review `docs/02-multi-tenant/cac-mo-hinh-tenant-isolation.md` - nắm 3 mô hình tenant isolation.
- [x] `[THỰC HÀNH]` Chạy `make db-up` trong `lab-code/` - verify PostgreSQL container.
- [x] `[THỰC HÀNH]` Chạy `make sql-1` - verify bảng `tenants` và `master_data`.
- [x] `[THỰC HÀNH]` Chạy `make sql-2` - verify sample data và `UNIQUE (tenant_id, code)`.
- [x] `[LÝ THUYẾT]` Đọc `docs/03-backend-database-mo-rong/index-va-query-tenant-aware.md`.
- [x] `[THỰC HÀNH]` Tự code `03-query-with-explain.sql` - chạy `EXPLAIN ANALYZE`.
- [x] `[THỰC HÀNH]` Tự code `04-index-comparison.sql` - so sánh no index, `tenant_id` index, composite index.
- [x] `[LÝ THUYẾT]` Đọc `docs/03-backend-database-mo-rong/noisy-neighbor-shared-table.md`.
- [x] `[LÝ THUYẾT]` Tổng hợp ý chính về noisy neighbor trong shared-table multi-tenant.
- [x] `[THỰC HÀNH]` Tự code `05-data-leakage-test.sql` - chứng minh query thiếu `tenant_id` có thể lộ data.
- [x] `[MILESTONE]` Tổng hợp output 5 file SQL playground.
- [x] `[BÁO CÁO]` Cập nhật `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` - thêm phần SQL thực hành.

### Milestone #2 - Migration & Locking Bridge

- [x] `[LÝ THUYẾT]` Đọc `migration-lock-rollback.md` và ghi 5-7 bullet về safe/risky migration.
- [x] `[THỰC HÀNH]` Tạo/chạy `06-migration-lock-observation.sql` ở mức local learning.
- [x] `[BÁO CÁO]` Ghi summary ngắn: lock là gì, migration ảnh hưởng nhiều tenant thế nào, rollback cần chuẩn bị gì.
- [x] `[MILESTONE]` Chốt PostgreSQL migration safety summary + output thực hành nhỏ.

### Milestone #3 - Spring Boot Foundation

- [x] `[THỰC HÀNH]` Hoàn thiện `pom.xml` và `application.yml` - Web, JPA, PostgreSQL, Flyway, Test.
- [x] `[THỰC HÀNH]` Tự code `TenantDemoApplication.java` và Flyway `V1`, `V2`, `V3`.
- [x] `[THỰC HÀNH]` Tự code `TenantContext.java` và `TenantFilter.java` - dùng `X-Tenant-Id`, clear bằng `finally`.
- [x] `[THỰC HÀNH]` Verify bằng curl/log: request có tenant header đi tiếp, thiếu/invalid header bị chặn.

### Milestone #4 - Tenant-aware API implementation slice

- [x] `[THỰC HÀNH]` Tự code `TenantAwareEntity.java`, `MasterData.java`, `MasterDataRepository.java` - method explicit có `tenantId`.
- [x] `[THỰC HÀNH]` Tự code `MasterDataService.java` và `MasterDataController.java` - endpoint đọc danh sách và tìm theo `code`.
- [x] `[THỰC HÀNH]` Verify bằng curl: tenant 1 chỉ thấy data tenant 1, tenant 2 chỉ thấy data tenant 2.
- [x] `[REVIEW]` Codex review focused: kiểm tra query quên `tenantId`, service/controller lấy tenant sai nguồn, thiết kế over-engineer.

---

## Kế hoạch fast-track từ 13/05

### Sprint 3.5 - 13/05: Đóng API tenant-aware và chuẩn bị test

Mục tiêu: biến phần API đã chạy được thành artifact báo cáo ngắn, rồi chuyển ngay sang test chống leakage.

- [x] `[BÁO CÁO]` Cập nhật `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` - thêm 3 rule backend tenant-aware: tenant từ trusted context, query luôn scoped, không tin `tenant_id` trong request body.
- [x] `[THỰC HÀNH]` Rerun ngắn: `cd lab-code && make db-up && make app-run`; verify curl tenant 1/2, missing/invalid tenant, cross-tenant id trả `404`; ghi pattern ngắn, không paste log dài.
- [x] `[MILESTONE]` Chốt Milestone #4 - API tenant-aware demo bằng curl, chưa tính JWT/React/test nâng cao.

### Sprint 4 - 14/05 đến 15/05: Test chống leakage + chuẩn bị JWT

Mục tiêu: khóa lại correctness của backend trước khi đổi cơ chế tenant context từ header giả lập sang JWT tạm.

- [ ] `[LÝ THUYẾT]` Tạo/read `docs/04-spring-boot/testing-tenant-isolation.md` từ Spring Boot testing/MockMvc docs - chỉ học đủ để viết test API tenant isolation.
- [ ] `[THỰC HÀNH]` Tự code `DataLeakageTest.java` - tối thiểu: tenant A không thấy data B, missing/invalid tenant bị chặn, query by code vẫn scoped theo tenant.
- [ ] `[REVIEW]` Nhờ Codex review `DataLeakageTest.java` sau khi `cd lab-code && make app-test` pass hoặc có lỗi rõ.
- [ ] `[LÝ THUYẾT]` Tạo/read `docs/05-security/jwt-spring-security-temporary.md` từ Spring Security/JWT chuẩn - phân biệt JWT tạm trong lab với Keycloak/OIDC production.
- [ ] `[SKELETON]` Nhờ Codex tạo skeleton/TODO comments cho security package, không tự động fill toàn bộ logic JWT.

### Sprint 5 - 16/05 đến 17/05: Temporary JWT auth + Keycloak awareness

Mục tiêu: demo tenant context không còn dựa trực tiếp vào `X-Tenant-Id`, nhưng vẫn không overdo full Keycloak.

- [ ] `[THỰC HÀNH]` Tự code JWT tạm: validate Bearer token local, đọc `tenant_id` claim, set `TenantContext`; giữ code rõ, không làm full auth platform.
- [ ] `[THỰC HÀNH]` Verify bằng curl: token tenant 1 thấy data tenant 1, token tenant 2 thấy data tenant 2, missing/invalid token bị chặn.
- [ ] `[REVIEW]` Nhờ Codex review security flow: không tin request body, không hardcode secret thật, không nhầm JWT tạm với Keycloak production.
- [ ] `[BÁO CÁO]` Ghi summary ngắn trong `docs/99-tong-ket/`: AuthN vs AuthZ, JWT, RBAC tenant-scope, Keycloak/OAuth2/OIDC dùng để làm gì.
- [ ] `[MILESTONE]` Chốt Milestone #5 - backend tenant API có test leakage + JWT tạm + Keycloak awareness.

### Sprint 6 - 18/05 đến 19/05: React UI nhỏ cho demo tenant flow

Mục tiêu: có một giao diện nhỏ để leader thấy flow tenant-aware, không chỉ curl.

- [ ] `[LÝ THUYẾT]` Tạo/read `docs/06-frontend/react-tenant-demo-ui.md` - React gọi REST API, env config, CORS ở mức tối thiểu, không học frontend lan man.
- [ ] `[SKELETON]` Nhờ Codex scaffold `lab-code/tenant-ui/` với TODO comments: nhập token, gọi list master data, search by code/category, hiển thị lỗi auth.
- [ ] `[THỰC HÀNH]` Tự code UI cơ bản: tenant 1/tenant 2 token, danh sách `master_data`, trạng thái loading/error, không làm thiết kế phức tạp.
- [ ] `[THỰC HÀNH]` Verify UI: backend running, UI gọi API thành công, đổi token thì data đổi theo tenant; curl vẫn là fallback.
- [ ] `[REVIEW]` Nhờ Codex review UI flow và README/run commands; không thêm state management/library nặng nếu chưa cần.
- [ ] `[MILESTONE]` Chốt Milestone #6 - runnable backend + React UI nhỏ chứng minh tenant-aware flow.

### Sprint 7 - 20/05 đến 21/05: Architecture awareness package

Mục tiêu: phủ bức tranh target architecture từ sơ đồ Viettel đủ để báo cáo, không chạy full stack.

- [ ] `[LÝ THUYẾT]` Tạo `docs/07-architecture/target-architecture-map.md` - map React, gateway, Keycloak, backend services, PostgreSQL, Redis, Kafka, MinIO, Elastic, observability, LLM, external systems vào vai trò tổng thể.
- [ ] `[LÝ THUYẾT]` Tạo mini-note Redis + observability: tenant-safe cache key, log/metric/tracing cần tenant context; không chạy Redis/Grafana thật.
- [ ] `[LÝ THUYẾT]` Tạo awareness summary ngắn cho Kafka, Debezium, Elasticsearch, MinIO, gRPC, realtime, LLM provider, external integrations; mỗi topic 3-5 bullet, ưu tiên đúng vai trò.
- [ ] `[BÁO CÁO]` Cập nhật `presentation-notes/thuyet-trinh-saas-multi-tenant.md` - thêm sơ đồ demo hiện tại so với target architecture, nêu rõ phần nào implemented/awareness/out of scope.
- [ ] `[MILESTONE]` Chốt Milestone #7 - architecture awareness package đủ nói chuyện với mentor.

### Sprint 8 - 22/05 đến 25/05: Report, demo script và Phase 1 defense

Mục tiêu: đóng gói để trình bày sớm, tránh sát deadline mới gom tài liệu.

- [ ] `[BÁO CÁO]` Cập nhật `reports/latex/bao-cao-saas-multi-tenant.tex` - SQL, migration, Spring Boot API, JWT tạm, React UI, architecture coverage; không overclaim production.
- [ ] `[BÁO CÁO]` Cập nhật `presentation-notes/thuyet-trinh-saas-multi-tenant.md` - demo script: DB → backend → JWT → API → React UI → tests → architecture map.
- [ ] `[THỰC HÀNH]` Dry-run demo: `make db-up`, `make app-run`, `make app-test`, chạy UI, verify tenant 1/2; ghi issue nếu có.
- [ ] `[REVIEW]` Nhờ Codex review report/presentation/dry-run ở mức mạch lạc, đúng phạm vi, không overclaim.
- [ ] `[MILESTONE]` Trình bày Phase 1: runnable demo + Q&A + giới hạn hiện tại + next steps sau Phase 1.

---

## Milestone summary

| # | Ngày mục tiêu | Output báo cáo | Trạng thái |
|:-:|:-------------:|----------------|------------|
| 1 | 01/05 | SQL playground + tenant-aware data isolation proof | Đã đóng |
| 2 | 08/05 | Migration & locking safety summary | Đã đóng |
| 3 | 10/05 | Spring Boot start + Flyway + TenantFilter | Practice đã xong, summary gộp vào #4 |
| 4 | 13/05 | Tenant-aware API demo + curl proof | Đã đóng |
| 5 | 17/05 | Data leakage tests + temporary JWT + Keycloak awareness | Kế tiếp |
| 6 | 19/05 | React UI tenant demo | Planned |
| 7 | 21/05 | Target architecture awareness package | Planned |
| 8 | 25/05 | Final report + runnable demo + Q&A | Planned |

---

## File Reference

| Cần làm gì | File/khu vực |
|------------|--------------|
| SaaS lý thuyết | `docs/01-saas/tong-quan-saas.md` |
| Multi-tenant | `docs/02-multi-tenant/*.md` |
| PostgreSQL/backend DB | `docs/03-backend-database-mo-rong/*.md` |
| Spring Boot learning notes | `docs/04-spring-boot/*.md` |
| Security/JWT notes | `docs/05-security/` - tạo khi tới task JWT |
| React UI notes | `docs/06-frontend/` - tạo khi tới task UI |
| Architecture awareness notes | `docs/07-architecture/` - tạo khi tới Sprint 7 |
| Tổng kết tiến độ | `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` |
| SQL thực hành | `lab-code/sql-playground/*.sql` |
| Spring Boot PoC | `lab-code/tenant-demo/` |
| React UI demo | `lab-code/tenant-ui/` - chưa có |
| Make commands | `lab-code/Makefile` |
| Báo cáo LaTeX | `reports/latex/bao-cao-saas-multi-tenant.tex` |
| Thuyết trình/demo script | `presentation-notes/thuyet-trinh-saas-multi-tenant.md` |
| Context private cũ | `local/` - chỉ dùng tham khảo, không làm source of truth công khai |

---

## Việc làm ngay trong 1-2 ngày tới

### Ngày 13/05

1. Mở `docs/99-tong-ket/nhung-gi-da-nam-duoc.md`.
2. Ghi 3 rule backend tenant-aware và curl pattern ngắn cho Milestone #4.
3. Chạy lại:

```bash
cd lab-code
make db-up
make app-run
```

4. Curl verify lại tenant 1/2, missing/invalid token/header theo cơ chế hiện tại.
5. Nhờ Codex review summary nếu muốn đóng Milestone #4 thật gọn.

### Ngày 14/05

1. Mở `lab-code/tenant-demo/src/test/java/com/viettel/demo/DataLeakageTest.java`.
2. Đọc/tạo note `docs/04-spring-boot/testing-tenant-isolation.md`.
3. Tự code test bằng MockMvc hoặc cách test tối giản phù hợp.
4. Verify:

```bash
cd lab-code
make app-test
```

5. Sau khi pass hoặc có lỗi cụ thể, nhờ Codex review test và chuẩn bị note JWT/Spring Security.

---

## Nguyên tắc out-of-scope cho Phase 1

1. Không triển khai full Keycloak/OIDC flow.
2. Không chạy Kafka, Debezium, Redis, MinIO, Elastic, Grafana stack thật nếu không phục vụ trực tiếp demo.
3. Không biến `master_data` demo thành full ERP/accounting domain.
4. Không thêm Swagger/OpenAPI, pagination nâng cao, role matrix phức tạp hoặc UI design lớn trước khi JWT/test/UI tối thiểu xong.
5. Không commit local/private notes, mentor feedback riêng tư hoặc prompt thô.
