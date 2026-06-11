# ROADMAP - Phase 1 Fast-track: ERP/Kế toán SaaS Multi-tenant

> **Bắt đầu:** Thứ Ba 28/04/2026
> **Deadline ban đầu:** Thứ Hai 25/05/2026
> **Cửa sổ học mở rộng đề xuất:** đến Thứ Tư 03/06/2026
> **Cập nhật roadmap:** 11/06/2026
> **Chu kỳ báo cáo:** mỗi 2-3 ngày phải có output có thể trình bày
> **Phương châm:** tự học + tự code trước, Codex tạo note/skeleton và review sau

---

## Quick Stats

| Chỉ số | Giá trị |
|--------|:-------:|
| **Tiến độ** | Phase 1 core done, Phase 1.5 planning started |
| **Tổng task** | 110 |
| **Đã hoàn thành** | 104 / 110 |
| **Focus hiện tại** | Phase 1.5 - production-like architecture demo planning |
| **Milestone tiếp theo** | #19 - Loki log aggregation + Kafka UI inspection |
| **Demo hiện tại** | React Web UI -> Keycloak -> Gateway -> tenant-demo -> PostgreSQL/Redis/Kafka/Observability; Elasticsearch/MinIO qua HTTP mini-lab |

Ghi chú: từ 22/05, demo tới Keycloak đã đủ để báo cáo khi cần. Sau feedback mentor Đạt ngày 25/05, Milestone #12 đã bổ sung Keycloak Authorization/RBAC/tenant-scope để hiểu phần "được phép làm gì" sau khi đã hiểu login/token. Milestone #13 đã chốt MinIO/file storage upload/download tenant-aware; Milestone #14 đã chốt Redis cache-aside tenant-safe read path; Milestone #15 đã chốt Kafka/async messaging reference flow nhỏ; Milestone #16 đã chốt Observability baseline với Actuator, request logging, Micrometer metrics và Prometheus/Grafana local lab. Milestone #17 đã chốt API Gateway static route và React Web UI Docker-first để nhìn flow end-to-end. React Native/Expo không thuộc repo này.

Ghi chú 11/06 sau khi báo cáo mentor Đạt: Phase 1 core learning coi như đủ nền. Phase 1.5 sẽ đi theo hướng demo production-like hơn: centralized logs bằng Loki/Grafana, Kafka UI, Kong Gateway, tách thêm `audit-log-service`, Kafka cross-service flow, rồi mới polish React Web UI cuối.

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
4. **Công nghệ học just-in-time nhưng phải thật nếu đã chạm feature.** Auth thì tiến tới Keycloak mini-lab nếu khả thi; file upload thì MinIO; cache thì Redis; search thì Elasticsearch.
5. **Không overclaim production.** JWT tạm là bridge, không phải Keycloak/OIDC production; backend chưa phải ERP thật.
6. **Giữ learning-first.** Codex không tự implement toàn bộ future feature nếu chưa được yêu cầu rõ.
7. **Local notes chỉ là context.** `local/` có thể giúp nhớ phạm vi kiến trúc, nhưng source of truth public là `docs/`, `ROADMAP.md`, code và report đã chuẩn hóa.
8. **Mentor feedback được xử lý bằng gap-driven learning.** Chủ đề nào mentor chỉ ra còn nông thì ưu tiên official docs + mini-lab nhỏ trước khi mở rộng sang feature mới.
9. **Theory doc phải foundation-first.** Mỗi công nghệ mới cần giải thích kiến thức nền có thể dùng lại, request/response/config shape nếu có, rồi mới áp dụng vào mini-lab hiện tại.

---

## Hiện trạng repo đã audit

### Đã hoàn thành hoặc gần hoàn thành

- SQL playground có `01` đến `07` và `09`: schema baseline, sample data, EXPLAIN, index comparison, temp table experiment, data leakage proof, migration/locking observation, index query-pattern và ACID/isolation observation.
- Spring Boot tenant demo đã có Maven wrapper, `pom.xml`, `application.yml`, `.env.example`, PostgreSQL Docker Compose và Flyway `V1-V3`.
- `TenantContext` / `TenantFilter` đã implement và từng được runtime verify bằng header `X-Tenant-Id`.
- Entity/repository/service/controller layer đã có API `master_data` tenant-aware cơ bản.
- `DataLeakageTest.java` đã có regression tests cho tenant isolation bằng `make app-test`.
- JWT tạm đã implement bằng Spring Security: Bearer token local, `tenant_id` claim, `JwtTenantContextFilter`, dev token endpoint và test MockMvc.
- Keycloak/OIDC mini-lab đã verify với `APP_AUTH_MODE=keycloak`.
- Elasticsearch/search mini-lab đã có concept doc, code guide, Docker lab và implementation tenant-aware đã verify end-to-end.
- Các note Spring Boot đã có trong `docs/04-spring-boot/`; các note JWT/Spring Security/Keycloak đã có trong `docs/05-security/`.

### Gap sau feedback mentor đã xử lý hoặc được đưa vào roadmap

- PostgreSQL index query-pattern đã được xử lý bằng Milestone #6.
- Flyway rollback/failure handling đã được xử lý bằng Milestone #7.
- ACID/isolation levels đã được xử lý bằng Milestone #8.
- Keycloak/OIDC đã chuyển từ awareness sang mini-lab và backend mode bằng Milestone #9.
- Elasticsearch đã đóng mini-lab. Trước khi đi tiếp MinIO/Redis/Kafka/Observability, roadmap bổ sung Keycloak Authorization/RBAC/tenant-scope theo feedback mentor Đạt.
- DDD là chủ đề hợp lý nhưng chưa urgent; để awareness/final reflection, không refactor demo quá sớm.
- `README.md` vẫn mô tả repo thiên về knowledge base; có thể cập nhật sau khi demo scope ổn định hơn.

### Bài học từ sơ đồ kiến trúc target

Sơ đồ target có React frontend, API Gateway/service discovery/load balancer, Keycloak/OAuth2/OIDC, nhiều backend services, PostgreSQL cluster, Redis, Kafka, Debezium CDC, MinIO, Elasticsearch/Elastic Stack, gRPC, realtime, observability, LLM providers và external services. Phase 1 cần hiểu vai trò của các phần này theo ngữ cảnh thật, nhưng chỉ implement sâu những phần phục vụ demo hoặc mini-lab gần nhất.

---

## Demo cuối Phase 1 - phạm vi thực tế

### Demo chính hiện có

- Spring Boot backend chạy được bằng `make app-run`.
- PostgreSQL local + Flyway tạo schema.
- Shared-table multi-tenant với `tenant_id`.
- Tenant-aware API cho `master_data`.
- Test hoặc curl chứng minh không lộ data cross-tenant.
- JWT tạm: token local có `tenant_id` claim, backend validate để set tenant context.
- Keycloak mode: Keycloak local phát token, backend validate bằng issuer/JWKS và giữ tenant-aware service/repository flow.
- Backend demo script đủ dùng để trình bày thủ công nếu cần.

### Demo/mini-lab mở rộng nếu khả thi

- Elasticsearch mini-lab: search trên `master_data`, tenant-aware query filter.
- MinIO mini-lab: upload/download file hoặc chứng từ nhỏ.
- Redis mini-lab: tenant-safe cache key.
- Kafka mini-lab: event producer/consumer nhỏ hoặc focused awareness.
- Observability mini-lab: health/log/metrics ở mức tối thiểu.
- React Web UI: final thin demo để nhìn flow end-to-end; không mở rộng thành frontend product.

### Không đưa vào Phase 1 core implementation

- Full production Keycloak/RBAC platform.
- Production API Gateway/Kong/service discovery/load balancing thật. Kong local lab được đưa sang Phase 1.5.
- Production Kafka/Debezium/Grafana stack chạy đầy đủ. Kafka UI/Loki local lab được đưa sang Phase 1.5.
- Full ERP/accounting workflow.
- Full production deployment, HA database cluster, audit/compliance hoàn chỉnh.

---

## Current state on 22/05/2026

### Đã coi là xong để báo cáo

- PostgreSQL multi-tenant SQL playground: schema, `tenant_id`, `UNIQUE (tenant_id, code)`, EXPLAIN, index comparison, temp table experiment, data leakage proof.
- PostgreSQL mentor gaps: index query-pattern mini-lab, Flyway failure/rollback, ACID/isolation levels.
- Spring Boot tenant-aware backend: Flyway `V1-V3`, entity/repository/service/controller, `TenantContext`, tenant-aware query flow.
- Regression test: `DataLeakageTest` chống missing/invalid token và cross-tenant data leakage.
- Temporary JWT bridge: local Bearer token có `tenant_id`, Spring Security Resource Server, `JwtTenantContextFilter`.
- Keycloak/OIDC mini-lab: Keycloak local issue token, Spring Boot `APP_AUTH_MODE=keycloak` validate issuer/JWKS, tenant-aware API chạy với Keycloak token.
- Architecture adoption map: đã map target diagram vào repo và phân loại implemented / mini-lab / awareness / out of scope.
- Backend demo script: đủ chấp nhận để demo thủ công bằng files, HTTP scripts và command output khi cần.

### Nguyên tắc từ đây trở đi

- Không tiếp tục polish report/demo nếu không chặn học.
- Học công nghệ theo thứ tự, mỗi lần một mini-lab.
- Với mỗi công nghệ: concept doc → code guide → skeleton/TODO → tự code → verify → review → summary.
- React Web UI là demo mỏng cuối Phase 1; chỉ gọi Gateway và không biến repo thành frontend product.

---

## Phase 1 Technology Coverage Map

| Topic trong kiến trúc | Mức phủ Phase 1 | Theory doc dự kiến | Code/demo artifact | Verification | Milestone | Trạng thái |
|---|---|---|---|---|---:|---|
| SaaS / ERP accounting context | Core theory | `docs/01-saas/`, `docs/00-gioi-thieu/` | Không cần code riêng | Report/presentation giải thích đúng | #1, #12 | Đã có nền |
| Multi-tenant shared-table | Core demo | `docs/02-multi-tenant/` | SQL playground + Spring Boot `master_data` | SQL, curl, test | #1, #4 | Đã có |
| PostgreSQL schema/index/EXPLAIN baseline | Core demo | `docs/03-backend-database-mo-rong/index-va-query-tenant-aware.md` | `lab-code/sql-playground/01-04` | `make sql-*` | #1 | Đã có |
| PostgreSQL index query patterns | Core mini-lab | `docs/03-backend-database-mo-rong/index-query-patterns-postgresql.md` | `07-index-query-patterns.sql` | EXPLAIN cho prefix/contains/function/composite cases | #6 | Đã đóng |
| Migration/locking baseline | Mini-lab | `docs/03-backend-database-mo-rong/migration-lock-rollback.md` | `06-migration-lock-observation.sql` | Local SQL observation | #2 | Đã có |
| Flyway rollback/failure handling | Core theory + mini-lab | `docs/03-backend-database-mo-rong/flyway-rollback-failure-handling.md` | `lab-code/flyway-failure-lab/` | Flyway command/log summary | #7 | Đã đóng |
| ACID/isolation levels | Core theory + mini-lab | `docs/03-backend-database-mo-rong/acid-isolation-levels-postgresql.md` | `09-acid-isolation-observation.sql` | Two-session observation nếu cần | #8 | Đã đóng |
| Spring Boot backend | Core demo | `docs/04-spring-boot/` | `lab-code/tenant-demo/` | `make app-run` | #3, #4 | Đã có |
| Flyway schema baseline | Core demo | `docs/04-spring-boot/spring-boot-bootstrap-config.md` | `V1-V3` migrations | Startup logs/Flyway logs | #3 | Đã có |
| TenantContext/TenantFilter | Core demo | `docs/04-spring-boot/request-filter-threadlocal.md` | `TenantContext.java`, `TenantFilter.java` | curl/log/test | #3, #4 | Đã có |
| Tenant-aware service/controller | Core demo | `docs/04-spring-boot/service-controller-curl-flow.md` | `MasterDataService`, `MasterDataController` | curl tenant 1/2 | #4 | Đã có |
| Data leakage tests | Core demo | `docs/04-spring-boot/testing-tenant-isolation.md` | `DataLeakageTest.java` | `make app-test` | #4 | Đã có |
| Temporary JWT auth | Core bridge | `docs/05-security/jwt-spring-security-temporary.md` | `SecurityConfig`, `JwtTokenService`, `JwtTenantContextFilter` | MockMvc + HTTP valid/invalid JWT | #5 | Đã đóng |
| Keycloak/OAuth2/OIDC | Mini-lab AuthN/token validation | `docs/05-security/keycloak-oidc-mental-model.md`, `docs/05-security/keycloak-oauth2-oidc-awareness.md`, `docs/05-security/keycloak-admin-console-guide.md` | `lab-code/keycloak-lab/`, `APP_AUTH_MODE=keycloak` | Lấy token Keycloak, gọi API tenant-aware, verify issuer/JWKS/claims | #9 | Đã verify mini-lab |
| Keycloak Authorization / RBAC / tenant-scope | Mini-lab đã verify | `docs/05-security/keycloak-authorization-rbac-tenant-scope.md`, `docs/05-security/keycloak-authorization-code-guide-spring-boot.md` | Role claim, Spring Security authorities converter, endpoint/service authorization nhỏ | Allowed role `200`, missing role `403`, cross-tenant vẫn `404`/không leak | #12 | Đã đóng |
| React Web frontend | Thin demo Docker-first | `docs/06-frontend/react-web-keycloak-gateway-demo.md` | `lab-code/web-ui-demo/` gọi Gateway bằng Keycloak token | Login Keycloak, ACCOUNTANT load/create, VIEWER load nhưng create `403`, requestId nối log | #17 | Đã verify demo |
| API Gateway/service discovery/load balancer | Mini-lab static route + awareness | `docs/07-architecture/api-gateway-service-discovery/api-gateway-foundation.md`, `docs/07-architecture/api-gateway-service-discovery/service-discovery-load-balancing-awareness.md` | `lab-code/gateway-demo/` route `/api/**` tới `tenant-demo` | Gateway forward Authorization + X-Request-Id, backend vẫn validate token/tenant | #17 | Đã đóng |
| Elasticsearch / Elastic Stack | Mini-lab đã verify | `docs/07-architecture/search-elasticsearch/elasticsearch-search-service.md`, `docs/07-architecture/search-elasticsearch/elasticsearch-request-response-shapes.md`, `docs/07-architecture/search-elasticsearch/elasticsearch-code-guide-spring-boot.md` | `lab-code/elasticsearch-lab/` + `com.viettel.demo.search` | Search tenant 1/2, no leakage | #11 | Đã đóng |
| MinIO / S3 object storage | Mini-lab đã verify | `docs/07-architecture/object-storage-minio/minio-object-storage.md`, `docs/07-architecture/object-storage-minio/minio-s3-api-shapes.md`, `docs/07-architecture/object-storage-minio/minio-code-guide-spring-boot.md` | upload/download mini-lab | HTTP upload/download + tenant metadata | #13 | Đã đóng |
| MinIO advanced object management | Optional/later backlog | `docs/07-architecture/object-storage-minio/minio-advanced-object-management.md` | Presigned URL expiry, lifecycle, versioning, object lock/retention nếu cần | Mini-lab riêng sau core milestones | Optional later | Backlog |
| Redis cache strategy | Mini-lab đã verify | `docs/07-architecture/cache-redis/redis-cache-strategy.md`, `docs/07-architecture/cache-redis/redis-code-guide-spring-boot.md`, `docs/07-architecture/cache-redis/redis-mini-lab-plan.md` | tenant-safe cache-aside path cho `master_data` by code | Hit/miss + tenant-safe cache key + TTL | #14 | Đã đóng |
| Kafka async messaging | Mini-lab đã verify | `docs/07-architecture/messaging-kafka/kafka-async-messaging.md`, `docs/07-architecture/messaging-kafka/kafka-event-shapes.md`, `docs/07-architecture/messaging-kafka/kafka-code-guide-spring-boot.md`, `docs/07-architecture/messaging-kafka/kafka-mini-lab-plan.md` | `MasterDataChangedEvent` producer/consumer log | Create/update publish event, consumer nhận event tenant-aware | #15 | Đã đóng |
| Debezium CDC + Kafka | Awareness | `docs/07-architecture/awareness/debezium-cdc.md` | Không chạy CDC | CDC role summary | #15/#18 | Later awareness |
| gRPC internal communication | Awareness | `docs/07-architecture/awareness/grpc-internal-communication.md` | Không chạy gRPC | REST vs gRPC vs Kafka table | #11 | Chưa có |
| Realtime: SignalR/Socket/SSE/Long polling | Awareness | `docs/07-architecture/awareness/realtime-communication.md` | Không chạy realtime | When to use which note | #11 | Chưa có |
| Observability/logging/metrics | Mini-lab đã verify | `docs/07-architecture/observability/observability-foundation.md`, `docs/07-architecture/observability/logging-metrics-tracing.md`, `docs/07-architecture/observability/micrometer-custom-metrics.md`, `docs/07-architecture/observability/prometheus-grafana-local-lab.md`, `docs/07-architecture/observability/spring-boot-actuator-code-guide.md`, `docs/07-architecture/observability/observability-mini-lab-plan.md` | Actuator/log/custom metric pattern + Prometheus/Grafana local scrape/dashboard | Health/prometheus public local, info/metrics auth, Prometheus target UP, Grafana datasource/dashboard | #16 | Đã đóng |
| LLM providers: OpenAI/OpenRouter/others | Awareness | `docs/07-architecture/awareness/llm-provider-integration.md` | Không gọi API thật | Integration role note | #11 | Chưa có |
| External services: e-contract, eCommerce, CRM, HR, documents, digital signing | Awareness | `docs/07-architecture/awareness/external-integrations-erp.md` | Không tích hợp thật | Boundary/use-case summary | #11 | Chưa có |
| DDD/domain boundaries | Later awareness | `docs/08-design/ddd-awareness.md` | Không refactor code theo DDD ở Phase 1 | Post-demo design note | #18 | Later |
| Full production microservices stack | Out of scope | Chỉ ghi giới hạn trong report | Không implement | Nêu rõ không thuộc Phase 1 | #13 | Out of scope |
| Full ERP/accounting domain | Out of scope | Chỉ dùng ví dụ nghiệp vụ | Không implement | Nêu rõ `master_data` chỉ là slice demo | #13 | Out of scope |

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

## Kế hoạch sau feedback mentor từ 14/05

### Sprint 3.5 - 13/05: Đóng API tenant-aware và chuẩn bị test

Mục tiêu: biến phần API đã chạy được thành artifact báo cáo ngắn, rồi chuyển ngay sang test chống leakage.

- [x] `[BÁO CÁO]` Cập nhật `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` - thêm 3 rule backend tenant-aware: tenant từ trusted context, query luôn scoped, không tin `tenant_id` trong request body.
- [x] `[THỰC HÀNH]` Rerun ngắn: `cd lab-code && make db-up && make app-run`; verify curl tenant 1/2, missing/invalid tenant, cross-tenant id trả `404`; ghi pattern ngắn, không paste log dài.
- [x] `[MILESTONE]` Chốt Milestone #4 - API tenant-aware demo bằng curl, chưa tính JWT/React/test nâng cao.

### Sprint 4 - 14/05 đến 15/05: Test chống leakage + chuẩn bị JWT

Mục tiêu: khóa lại correctness của backend trước khi đổi cơ chế tenant context từ header giả lập sang JWT tạm.

- [x] `[LÝ THUYẾT]` Tạo/read `docs/04-spring-boot/testing-tenant-isolation.md` từ Spring Boot testing/MockMvc docs - chỉ học đủ để viết test API tenant isolation.
- [x] `[THỰC HÀNH]` Tự code `DataLeakageTest.java` - tối thiểu: tenant A không thấy data B, missing/invalid tenant bị chặn, query by code vẫn scoped theo tenant.
- [x] `[REVIEW]` Nhờ Codex review `DataLeakageTest.java` sau khi `cd lab-code && make app-test` pass hoặc có lỗi rõ.
- [x] `[LÝ THUYẾT]` Đọc `docs/05-security/jwt-spring-security-temporary.md` từ Spring Security/JWT chuẩn - phân biệt JWT tạm trong lab với Keycloak/OIDC production.
- [x] `[SKELETON]` Nhờ Codex tạo skeleton/TODO comments cho security package, không tự động fill toàn bộ logic JWT.

### Sprint 5 - 14/05: Temporary JWT auth bridge

Mục tiêu: demo tenant context không còn dựa trực tiếp vào `X-Tenant-Id`, nhưng vẫn không overdo full Keycloak.

- [x] `[THỰC HÀNH]` Tự code JWT tạm: validate Bearer token local, đọc `tenant_id` claim, set `TenantContext`; giữ code rõ, không làm full auth platform.
- [x] `[THỰC HÀNH]` Verify bằng curl: token tenant 1 thấy data tenant 1, token tenant 2 thấy data tenant 2, missing/invalid token bị chặn.
- [x] `[REVIEW]` Nhờ Codex review security flow: không tin request body, không hardcode secret thật, không nhầm JWT tạm với Keycloak production.
- [x] `[BÁO CÁO]` Ghi summary ngắn trong `docs/99-tong-ket/`: AuthN vs AuthZ, JWT, RBAC tenant-scope, Keycloak/OAuth2/OIDC dùng để làm gì.
- [x] `[MILESTONE]` Chốt Milestone #5 - backend tenant API có test leakage + JWT tạm + gap report sau feedback mentor.

### Sprint 5.5 - 14/05 đến 15/05: Mentor feedback gap report

Mục tiêu: không nhảy ngay sang feature mới; trước tiên chốt JWT bridge và map rõ các lỗ hổng mentor đã chỉ ra.

- [x] `[BÁO CÁO]` Tạo `docs/99-tong-ket/gap-report-sau-feedback-mentor-2026-05-14.md` - phân loại gap: index query pattern, Flyway rollback/failure, ACID/isolation, Keycloak/tech adoption, DDD later.
- [x] `[LÝ THUYẾT]` Liệt kê official docs cần đọc cho PostgreSQL index pattern, Flyway failure/rollback, PostgreSQL isolation và Keycloak/OIDC; chưa viết textbook dài.
- [x] `[BÁO CÁO]` Cập nhật `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` bằng summary JWT tạm và giới hạn: vẫn là bridge trước Keycloak.
- [x] `[MILESTONE]` Chốt Milestone #5 - demo backend JWT tạm đã chạy + roadmap/gap report đã align theo mentor feedback.

### Sprint 6 - 16/05 đến 17/05: PostgreSQL index query-pattern deepening

Mục tiêu: sửa điểm mentor nhắc: index không chỉ là “create index”, mà phụ thuộc pattern query và planner.

- [x] `[LÝ THUYẾT]` Tạo/read `docs/03-backend-database-mo-rong/index-query-patterns-postgresql.md` từ PostgreSQL docs: B-tree, multicolumn leftmost prefix, pattern matching, expression index, `pg_trgm`.
- [x] `[SKELETON]` Tạo `lab-code/sql-playground/07-index-query-patterns.sql` với TODO comments: prefix `LIKE`, leading wildcard, contains search, `lower(name)`, tenant-aware `tenant_id + code/category/keyword`.
- [x] `[THỰC HÀNH]` Tự code mini-lab và chạy EXPLAIN: so sánh query có thể dùng B-tree index với query khó dùng index.
- [x] `[THỰC HÀNH]` Ghi quan sát định tính: khi nào Seq Scan vẫn hợp lý do selectivity/bảng nhỏ; không bịa số nếu output không lưu.
- [x] `[REVIEW]` Nhờ Codex review SQL/note: có hiểu đúng prefix search, leftmost prefix, trigram/GIN và expression index chưa.
- [x] `[MILESTONE]` Chốt Milestone #6 - index query-pattern mini-lab + summary mentor-facing.

### Sprint 7 - 18/05 đến 19/05: Flyway rollback/failure handling

Mục tiêu: bổ sung mindset “execute migration và rollback plan”, không chỉ biết `ALTER TABLE`.

- [x] `[LÝ THUYẾT]` Tạo/read `docs/03-backend-database-mo-rong/flyway-rollback-failure-handling.md` từ Flyway docs: versioned migration, schema history, validate, repair, failure midway, transaction behavior, undo migration điều kiện nào có.
- [x] `[SKELETON]` Tạo guided mini-lab cô lập `lab-code/flyway-failure-lab/README.md`; không phá DB thật, ưu tiên schema local riêng.
- [x] `[THỰC HÀNH]` Tự quan sát một failure/validate scenario ở mức an toàn hoặc ghi lại lý do không chạy nếu quá rủi ro.
- [x] `[REVIEW]` Nhờ Codex review phần giải thích forward migration vs rollback và SaaS shared-table migration checklist.
- [x] `[MILESTONE]` Chốt Milestone #7 - Flyway failure/rollback mindset summary.

### Sprint 8 - 20/05 đến 21/05: ACID và isolation levels

Mục tiêu: hiểu transaction behavior trước khi mở rộng backend feature, nhất là shared-table nhiều tenant.

- [x] `[LÝ THUYẾT]` Tạo/read `docs/03-backend-database-mo-rong/acid-isolation-levels-postgresql.md` từ PostgreSQL docs: ACID, isolation levels, dirty read, non-repeatable read, phantom read, default isolation.
- [x] `[SKELETON]` Tạo `lab-code/sql-playground/09-acid-isolation-observation.sql` với TODO comments cho quan sát 2 session ở mức cơ bản.
- [x] `[THỰC HÀNH]` Tự chạy một hoặc hai observation an toàn: concurrent read/write hoặc transaction visibility; không overdo lock internals.
- [x] `[BÁO CÁO]` Ghi summary: vì sao concurrent writes/reads và lock impact quan trọng trong SME SaaS shared table.
- [x] `[MILESTONE]` Chốt Milestone #8 - ACID/isolation note + observation summary.

### Sprint 9 - 22/05: Keycloak/OIDC mini-lab đã đóng

Mục tiêu: nâng auth từ JWT tạm sang Keycloak/OIDC mini-lab thật ở phạm vi nhỏ, giữ local JWT fallback cho test.

- [x] `[LÝ THUYẾT]` Tạo/read `docs/05-security/keycloak-oidc-mental-model.md`, `docs/05-security/keycloak-oauth2-oidc-awareness.md` và `docs/05-security/keycloak-admin-console-guide.md` từ Keycloak/Spring Security docs: Authorization Server, Resource Server, issuer, JWKS, access token, client, realm, Admin Console cơ bản.
- [x] `[SKELETON]` Chuẩn bị `lab-code/keycloak-lab/` hoặc Docker Compose profile có TODO comments nếu chạy Keycloak local là khả thi trên máy.
- [x] `[SKELETON]` Chuẩn bị Spring Boot Keycloak integration skeleton: `app.auth.mode`, `KEYCLOAK_ISSUER_URI`, TODO trong `SecurityConfig`, HTTP Client verify; chưa implement decoder switch.
- [x] `[THỰC HÀNH]` Tự thử flow nhỏ: tạo realm/client/user hoặc lấy token dev; nếu không kịp thì ghi rõ blocker và giữ JWT tạm.
- [x] `[THỰC HÀNH]` So sánh JWT tạm hiện tại với Keycloak/OIDC thật: phần nào giống, phần nào khác, phần nào production mới cần.
- [x] `[REVIEW]` Nhờ Codex review note/lab: không biến Keycloak thành full IAM project, không overclaim RBAC production.
- [x] `[MILESTONE]` Chốt Milestone #9 - Keycloak/OIDC mini-lab hoặc awareness có evidence rõ.

## New post-Keycloak technology learning roadmap

### Sprint 10 - 22/05: Chốt trạng thái post-Keycloak và chuẩn hóa mini-lab style

Mục tiêu: ghi rõ Keycloak demo/report đã đủ, chuyển roadmap sang học công nghệ theo execution plan nhanh.

- [x] `[BÁO CÁO]` Xác nhận trạng thái 22/05: demo tới Keycloak đã xong, backend demo script đủ dùng nếu cần báo cáo nhanh.
- [x] `[LÝ THUYẾT]` Tạo/cập nhật `docs/07-architecture/overview/target-architecture-adoption-map.md` - map target architecture vào implemented / mini-lab / awareness / out of scope.
- [x] `[BÁO CÁO]` Tạo `docs/99-tong-ket/technology-mini-lab-template.md` - template chuẩn cho các mini-lab công nghệ tiếp theo.

### Sprint 11 - 22/05 đến 23/05: Elasticsearch/search mini-lab

Mục tiêu: nối từ PostgreSQL `LIKE`/index query-pattern sang search engine, chỉ trên lát cắt `master_data`.

- [x] `[LÝ THUYẾT]` Tạo/read `docs/07-architecture/search-elasticsearch/elasticsearch-search-service.md`, `docs/07-architecture/search-elasticsearch/elasticsearch-request-response-shapes.md` và `docs/07-architecture/search-elasticsearch/elasticsearch-mini-lab-plan.md` - foundation + API shape + plan mini-lab.
- [x] `[SKELETON]` Tạo/read `docs/07-architecture/search-elasticsearch/elasticsearch-code-guide-spring-boot.md`; chuẩn bị `lab-code/elasticsearch-lab/`, config `APP_SEARCH_ENABLED=false`, package `com.viettel.demo.search`, HTTP Client skeleton.
- [x] `[THỰC HÀNH]` Tự code search mini-lab: reindex `master_data`, search keyword tenant 1/tenant 2, verify tenant filter và eventual consistency caveat.
- [x] `[REVIEW]` Nhờ Codex review implementation: không biến Elasticsearch thành source of truth, không leak tenant, app-test vẫn pass khi search disabled.
- [x] `[MILESTONE]` Chốt Milestone #11 - Elasticsearch/search mini-lab có command, HTTP evidence và summary ngắn.

### Sprint 12 - 25/05 đến 26/05: Keycloak Authorization / RBAC / tenant-scope

Mục tiêu: sau khi đã hiểu Keycloak/OIDC ở mức AuthN/token validation, học tiếp AuthZ/RBAC/tenant-scope để biết user được phép làm gì và vẫn không lộ dữ liệu tenant khác.

- [x] `[LÝ THUYẾT]` Tạo/read `docs/05-security/keycloak-authorization-rbac-tenant-scope.md` - authentication vs authorization vs tenant isolation, realm roles vs client roles, scopes/claims, 401 vs 403, Keycloak role không thay thế tenant-aware query.
- [x] `[SKELETON]` Tạo/read `docs/05-security/keycloak-authorization-code-guide-spring-boot.md`; chuẩn bị TODO cho Spring Security authorities converter, endpoint/service authorization, test/HTTP verification.
- [x] `[THỰC HÀNH]` Tự cấu hình role claim trong Keycloak token: role đơn giản như `ACCOUNTANT`, `ADMIN`, `VIEWER`; không làm full permission matrix.
- [x] `[THỰC HÀNH]` Tự code authorization nhỏ: mapping Keycloak role/claim sang `GrantedAuthority`, dùng URL-level authorization cho endpoint phù hợp.
- [x] `[REVIEW]` Nhờ Codex review: phân biệt endpoint-level authorization, service/business authorization, tenant-scope; verify user thiếu role trả `403`, missing/invalid token trả `401`.
- [x] `[MILESTONE]` Chốt Milestone #12 - Keycloak Authorization/RBAC mini-lab: allowed role gọi được, user thiếu role bị chặn, tenant 1 vẫn không đọc được tenant 2.

Không overdo:

- Chưa làm Keycloak Authorization Services/UMA nếu chưa thật sự cần.
- Chưa làm role/permission matrix ERP đầy đủ.
- Chưa thay tenant-aware repository bằng role claim. Role trả lời “được làm gì”, `tenantId` query trả lời “dữ liệu tenant nào”.

### Sprint 13 - 27/05: MinIO/file storage mini-lab

Mục tiêu: học object storage/S3 API trong ngữ cảnh chứng từ/file attachment, không xây file service production.

- [x] `[LÝ THUYẾT]` Tạo `docs/07-architecture/object-storage-minio/minio-object-storage.md`, `docs/07-architecture/object-storage-minio/minio-s3-api-shapes.md` và `docs/07-architecture/object-storage-minio/minio-code-guide-spring-boot.md` - object storage vs DB, S3 API, tenant/file metadata, presigned URL awareness.
- [x] `[SKELETON]` Chuẩn bị `lab-code/minio-lab/`, config `APP_FILE_STORAGE_ENABLED=false`, package `com.viettel.demo.storage` với TODO comments.
- [x] `[THỰC HÀNH]` Tự code upload/download nhỏ: file lưu MinIO, metadata tenant-aware lưu PostgreSQL hoặc in-memory nếu giữ scope nhỏ.
- [x] `[REVIEW]` Nhờ Codex review: không commit file/secret, không bỏ auth/tenant check, không nhầm MinIO với database source of truth.
- [x] `[MILESTONE]` Chốt Milestone #13 - MinIO mini-lab có upload/download evidence và giới hạn production.

### Sprint 14 - 28/05: Redis/cache mini-lab

Mục tiêu: học cache đúng lúc sau khi đã có API/search/file slice, tập trung tenant-safe cache key.

- [x] `[LÝ THUYẾT]` Tạo `docs/07-architecture/cache-redis/redis-cache-strategy.md` và `docs/07-architecture/cache-redis/redis-code-guide-spring-boot.md` - cache-aside, TTL, invalidation, tenant-safe key.
- [x] `[SKELETON]` Chuẩn bị `lab-code/redis-lab/`, config `APP_CACHE_ENABLED=false`, package/cache service TODO.
- [x] `[THỰC HÀNH]` Tự code cache nhỏ cho read endpoint hoặc lookup config: key phải có tenant prefix, verify hit/miss/log.
- [x] `[REVIEW]` Nhờ Codex review: không cache cross-tenant, không cache data stale mà không ghi caveat, không dùng Redis khi PostgreSQL đủ.
- [x] `[MILESTONE]` Chốt Milestone #14 - Redis mini-lab có cache key pattern và summary.

### Sprint 15 - 29/05: Kafka/async messaging mini-lab

Mục tiêu: hiểu async/event-driven communication ở mức nhỏ, không chạy full event platform nếu chưa cần.

- [x] `[LÝ THUYẾT]` Tạo `docs/07-architecture/messaging-kafka/kafka-async-messaging.md` và `docs/07-architecture/messaging-kafka/kafka-code-guide-spring-boot.md` - producer, consumer, topic, event contract, retry/idempotency awareness.
- [x] `[SKELETON]` Nếu máy chịu được Docker: chuẩn bị `lab-code/kafka-lab/` và package `com.viettel.demo.messaging`; nếu quá nặng, tạo code skeleton + sequence diagram awareness.
- [x] `[THỰC HÀNH]` Tự làm một event nhỏ hoặc simulation: `MasterDataChanged`/`FileUploaded` log consumer, không ép production Kafka stack.
- [x] `[MILESTONE]` Chốt Milestone #15 - Kafka/async có flow, caveat và quyết định rõ chạy thật hay awareness.

### Sprint 16 - 30/05: Observability/logging/metrics mini-lab

Mục tiêu: biết app production cần log/metric/health thế nào, rồi chạy Prometheus/Grafana local ở mức vừa đủ để hiểu monitoring flow thật.

- [x] `[LÝ THUYẾT]` Tạo foundation docs cho Observability: `observability-foundation.md`, `logging-metrics-tracing.md`, `spring-boot-actuator-code-guide.md`, `observability-mini-lab-plan.md`.
- [x] `[SKELETON]` Implement Actuator baseline: `health/info/metrics`, health public, info/metrics authenticated, không expose sensitive endpoints bừa bãi.
- [x] `[THỰC HÀNH]` Implement request logging baseline: `X-Request-Id`, MDC, method/path/status/duration, không log token/body/query string.
- [x] `[THỰC HÀNH]` Implement + verify custom Micrometer metrics baseline: Redis hit/miss, Kafka publish, getByCode timer; không dùng high-cardinality tags.
- [x] `[THỰC HÀNH]` Thêm Prometheus/Grafana local lab: `/actuator/prometheus`, Prometheus scrape target, Grafana datasource/dashboard nhỏ.
- [x] `[MILESTONE]` Chốt Milestone #16 - observability summary đủ giải thích trong target architecture.

### Sprint 17 - 31/05: API Gateway/service discovery awareness + React Web optional demo

Mục tiêu: hiểu vị trí gateway/load balancer/service discovery trong target architecture; React Web UI chỉ là thin demo để thấy flow end-to-end.

- [x] `[LÝ THUYẾT]` Tạo docs `docs/07-architecture/api-gateway-service-discovery/` - API Gateway foundation, Spring Cloud Gateway code guide, service discovery/load balancing awareness.
- [x] `[SKELETON/THỰC HÀNH]` Tạo `lab-code/gateway-demo/` - Spring Cloud Gateway static route `/api/**` tới `tenant-demo`, giữ/ sinh `X-Request-Id`.
- [x] `[BÁO CÁO]` Cập nhật architecture map: gateway static route đã có mini-lab; service discovery/load balancing vẫn awareness vì repo chỉ có một backend service.
- [x] `[SKELETON/THỰC HÀNH]` Tạo React Web UI demo Docker-first: `docs/06-frontend/react-web-keycloak-gateway-demo.md`, `lab-code/web-ui-demo/`; không dùng React Native/Expo và không yêu cầu local npm.
- [x] `[THỰC HÀNH]` Verify end-to-end bằng browser: Keycloak login -> Gateway -> `tenant-demo` -> requestId trong log.
- [x] `[MILESTONE]` Chốt Milestone #17 - API Gateway/static route + React Web UI demo đủ trả lời mentor ở mức Phase 1.

### Sprint 18 - 01/06 đến 03/06: DDD awareness + final reflection

Mục tiêu: đóng Phase 1 mở rộng bằng summary trung thực: đã implement gì, mini-lab gì, awareness gì, còn giới hạn gì.

- [ ] `[LÝ THUYẾT]` Tạo `docs/08-design/ddd-awareness.md` ngắn: domain boundary/entity/service/module ở mức nhận biết, không refactor demo theo DDD.
- [ ] `[BÁO CÁO]` Cập nhật `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` theo template: SQL, Spring Boot, JWT/Keycloak, Search, MinIO/Redis/Kafka/Observability nếu đã làm.
- [ ] `[BÁO CÁO]` Cập nhật `presentation-notes/thuyet-trinh-saas-multi-tenant.md` và/hoặc `reports/latex/bao-cao-saas-multi-tenant.tex`; không overclaim production.
- [ ] `[THỰC HÀNH]` Dry-run demo tối thiểu: `make db-up`, `make app-test`, Keycloak/manual HTTP, và mini-lab đã hoàn thành gần nhất.
- [ ] `[REVIEW]` Nhờ Codex review report/presentation/dry-run ở mức mạch lạc, đúng phạm vi, không overclaim.
- [ ] `[MILESTONE]` Trình bày Phase 1 mở rộng: runnable backend demo + technology mini-labs + gap learning + next steps sau Phase 1.

---

## Milestone summary

| # | Ngày mục tiêu | Output báo cáo | Trạng thái |
|:-:|:-------------:|----------------|------------|
| 1 | 01/05 | SQL playground + tenant-aware data isolation proof | Đã đóng |
| 2 | 08/05 | Migration & locking safety summary | Đã đóng |
| 3 | 10/05 | Spring Boot start + Flyway + TenantFilter | Practice đã xong, summary gộp vào #4 |
| 4 | 13/05 | Tenant-aware API demo + curl proof | Đã đóng |
| 5 | 15/05 | Temporary JWT bridge + mentor gap report | Đã đóng |
| 6 | 17/05 | PostgreSQL index query-pattern mini-lab | Đã đóng |
| 7 | 19/05 | Flyway rollback/failure handling | Đã đóng |
| 8 | 21/05 | ACID/isolation levels observation | Đã đóng |
| 9 | 22/05 | Keycloak/OIDC mini-lab + backend Keycloak mode evidence | Đã đóng |
| 10 | 22/05 | Post-Keycloak roadmap + mini-lab template | Đã đóng |
| 11 | 23/05 | Elasticsearch/search mini-lab | Đã đóng |
| 12 | 26/05 | Keycloak Authorization/RBAC/tenant-scope mini-lab | Đã đóng |
| 13 | 27/05 | MinIO/file storage mini-lab | Đã đóng |
| 14 | 28/05 | Redis/cache mini-lab | Đã đóng |
| 15 | 29/05 | Kafka/async messaging mini-lab hoặc focused awareness | Đã đóng |
| 16 | 30/05 | Observability/logging/metrics mini-lab | Đã đóng |
| 17 | 31/05 | API Gateway/service discovery awareness + React Web UI final demo | Đã đóng |
| 18 | 03/06 | DDD awareness + final reflection + demo dry-run | Planned |

---

## Phase 1.5 - Production-like architecture demo

Phase 1.5 bắt đầu sau buổi báo cáo mentor Đạt ngày 11/06/2026. Mục tiêu không phải làm production ERP, mà là biến demo hiện tại từ một backend app nhiều mini-lab thành mô hình gần kiến trúc target hơn.

### Vì sao cần Phase 1.5?

- Đã hiểu từng công nghệ riêng lẻ, nhưng demo vẫn còn nặng monolith `tenant-demo`.
- Kafka hiện mới là same-app producer/consumer, chưa tạo cảm giác event đi giữa service thật.
- Log hiện đọc được trong terminal, nhưng nhiều service sẽ cần centralized log search.
- Spring Cloud Gateway đã đủ để hiểu gateway concept; target architecture lại dùng Kong, nên cần một Kong lab.
- React Web UI baseline đã chạy, nhưng chưa nên polish UI sâu trước khi backend/service boundaries rõ hơn.

### Thứ tự thực hiện đề xuất

| Thứ tự | Milestone | Mục tiêu | Artifact chính | Done criteria | Trạng thái |
|---:|---|---|---|---|---|
| 1 | Loki/log aggregation | Gom log nhiều service vào Grafana Explore | `docs/07-architecture/log-aggregation-loki/`, `lab-code/loki-lab/` | Tìm log theo service/requestId trong Grafana | Planned |
| 2 | Kafka UI | Nhìn topic/message/consumer group/lag thay vì chỉ đọc log | `docs/07-architecture/kafka-ui/`, `lab-code/kafka-ui-lab/` | Mở UI thấy `master-data-events`, message key/value, consumer group | Planned |
| 3 | Kong Gateway | Practice gateway platform gần target architecture | `docs/07-architecture/kong-gateway/`, `lab-code/kong-gateway-lab/` | Route `/api/master-data/**`, sau này `/api/audit/**`, giữ auth/requestId | Planned |
| 4 | Audit Log Service split | Tạo service thứ hai có trách nhiệm rõ | `docs/07-architecture/microservice-boundaries/`, `lab-code/audit-log-service/` sau này | `master-data-service` publish event, `audit-log-service` consume và lưu/log audit | Planned |
| 5 | Cross-service Kafka flow | Biến Kafka thành event giữa services | `MasterDataChangedEvent` từ service A sang service B | Kafka UI thấy event, audit service log/store được, không dùng Kafka như database | Planned |
| 6 | Final React Web polish | UI demo sau khi backend boundaries ổn | `lab-code/web-ui-demo/` | UI gọi Kong, load/create master data, xem audit nếu endpoint có thật | Planned |

### Service split được chọn

Recommended split: giữ `tenant-demo` như `master-data-service` về mặt trách nhiệm, rồi thêm `audit-log-service` làm service thứ hai.

Lý do:

- tận dụng event `MasterDataChangedEvent` hiện có;
- Kafka trở thành cross-service flow thật;
- domain audit nhỏ, dễ hiểu, không cần dựng nghiệp vụ kế toán mới;
- Kong có thêm route thật sau này: `/api/master-data/**` và `/api/audit/**`;
- Loki trở nên có ý nghĩa vì có log từ nhiều service.

Các split chưa chọn ngay:

- `file-service`: hợp lý với MinIO nhưng kéo thêm upload/security/UI work.
- `search-service`: thực tế hơn cho projection nhưng Elasticsearch + reindex + eventual consistency dễ phình scope.
- notification/reporting service: dễ bị giả tạo nếu chưa có use case rõ.

---

## File Reference

| Cần làm gì | File/khu vực |
|------------|--------------|
| SaaS lý thuyết | `docs/01-saas/tong-quan-saas.md` |
| Multi-tenant | `docs/02-multi-tenant/*.md` |
| PostgreSQL/backend DB | `docs/03-backend-database-mo-rong/*.md` |
| PostgreSQL index query patterns | `docs/03-backend-database-mo-rong/index-query-patterns-postgresql.md`, `lab-code/sql-playground/07-index-query-patterns.sql` |
| Flyway rollback/failure | `docs/03-backend-database-mo-rong/flyway-rollback-failure-handling.md`, `lab-code/flyway-failure-lab/README.md` |
| ACID/isolation | `docs/03-backend-database-mo-rong/acid-isolation-levels-postgresql.md`, `lab-code/sql-playground/09-acid-isolation-observation.sql` |
| Spring Boot learning notes | `docs/04-spring-boot/*.md` |
| Security/JWT/Keycloak notes | `docs/05-security/` - đã có JWT tạm, Keycloak/OIDC và Keycloak Authorization/RBAC |
| Keycloak Authorization/RBAC mini-lab | `docs/05-security/keycloak-authorization-rbac-tenant-scope.md`, `docs/05-security/keycloak-authorization-code-guide-spring-boot.md`, `docs/05-security/keycloak-authorization-admin-console-guide.md`, `docs/05-security/keycloak-authorization-mini-lab-plan.md`, `lab-code/tenant-demo/http/keycloak-authorization-api.http` |
| Backend Keycloak demo script | `presentation-notes/demo-script-keycloak-tenant-flow.md` |
| React Web UI notes | `docs/06-frontend/react-web-keycloak-gateway-demo.md` |
| Architecture awareness notes | `docs/07-architecture/` - tạo khi tới Sprint 7 |
| Target architecture adoption map | `docs/07-architecture/overview/target-architecture-adoption-map.md` |
| Elasticsearch/search mini-lab | `docs/07-architecture/search-elasticsearch/elasticsearch-search-service.md`, `docs/07-architecture/search-elasticsearch/elasticsearch-request-response-shapes.md`, `docs/07-architecture/search-elasticsearch/elasticsearch-code-guide-spring-boot.md`, `docs/07-architecture/search-elasticsearch/elasticsearch-design-patterns-spring-boot.md`, `docs/07-architecture/search-elasticsearch/elasticsearch-mini-lab-plan.md`, `lab-code/elasticsearch-lab/`, `lab-code/tenant-demo/src/main/java/com/viettel/demo/search/` |
| MinIO/file storage mini-lab | `docs/07-architecture/object-storage-minio/minio-object-storage.md`, `docs/07-architecture/object-storage-minio/minio-code-guide-spring-boot.md`, `lab-code/minio-lab/` |
| Redis/cache mini-lab | `docs/07-architecture/cache-redis/redis-cache-strategy.md`, `docs/07-architecture/cache-redis/redis-code-guide-spring-boot.md`, `docs/07-architecture/cache-redis/redis-mini-lab-plan.md`, `lab-code/redis-lab/` |
| Kafka/async mini-lab | `docs/07-architecture/messaging-kafka/kafka-async-messaging.md`, `docs/07-architecture/messaging-kafka/kafka-event-shapes.md`, `docs/07-architecture/messaging-kafka/kafka-code-guide-spring-boot.md`, `docs/07-architecture/messaging-kafka/kafka-mini-lab-plan.md`, `lab-code/kafka-lab/` |
| Observability mini-lab | `docs/07-architecture/observability/observability-foundation.md`, `docs/07-architecture/observability/logging-metrics-tracing.md`, `docs/07-architecture/observability/micrometer-custom-metrics.md`, `docs/07-architecture/observability/prometheus-grafana-local-lab.md`, `docs/07-architecture/observability/spring-boot-actuator-code-guide.md`, `docs/07-architecture/observability/observability-mini-lab-plan.md`, `lab-code/observability-lab/` |
| API Gateway/service discovery mini-lab | `docs/07-architecture/api-gateway-service-discovery/api-gateway-foundation.md`, `docs/07-architecture/api-gateway-service-discovery/spring-cloud-gateway-code-guide.md`, `docs/07-architecture/api-gateway-service-discovery/service-discovery-load-balancing-awareness.md`, `docs/07-architecture/api-gateway-service-discovery/api-gateway-mini-lab-plan.md`, `lab-code/gateway-demo/` |
| Phase 1.5 plan | `docs/99-tong-ket/phase1-5-production-like-demo-plan.md` |
| Loki/log aggregation | `docs/07-architecture/log-aggregation-loki/`, `lab-code/loki-lab/` |
| Kafka UI | `docs/07-architecture/kafka-ui/`, `lab-code/kafka-ui-lab/` |
| Kong Gateway | `docs/07-architecture/kong-gateway/`, `lab-code/kong-gateway-lab/` |
| Microservice boundaries | `docs/07-architecture/microservice-boundaries/` |
| Mini-lab template | `docs/99-tong-ket/technology-mini-lab-template.md` |
| DDD awareness | `docs/08-design/ddd-awareness.md` - để cuối Phase 1 mở rộng |
| Tổng kết tiến độ | `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` |
| SQL thực hành | `lab-code/sql-playground/*.sql` |
| Spring Boot PoC | `lab-code/tenant-demo/` |
| React Web UI demo | `lab-code/web-ui-demo/` - thin UI gọi Gateway, chạy bằng Docker |
| Make commands | `lab-code/Makefile` |
| Báo cáo LaTeX | `reports/latex/bao-cao-saas-multi-tenant.tex` |
| Thuyết trình/demo script | `presentation-notes/thuyet-trinh-saas-multi-tenant.md` |
| Context private cũ | `local/` - chỉ dùng tham khảo, không làm source of truth công khai |

---

## Việc làm ngay trong 1-2 ngày tới

### Task tiếp theo: Phase 1 final demo dry-run

1. Đọc:
   - `docs/99-tong-ket/phase1-final-demo-script.md`.
   - `docs/06-frontend/react-web-keycloak-gateway-demo.md`.
   - `docs/07-architecture/overview/target-architecture-adoption-map.md`.
2. Chuẩn bị Keycloak public client `tenant-demo-web` nếu reset volume:
   - public client / client authentication off;
   - `Valid redirect URIs = http://localhost:5173/*`;
   - `Web origins = http://localhost:5173`;
   - `tenant1-user` có role `ACCOUNTANT`, `tenant2-user` có role `VIEWER`.
3. Chạy `tenant-demo` ở `8080`, gateway ở `8081`, React Web UI ở `5173`, rồi dry-run script:
   - ACCOUNTANT load/create qua Gateway;
   - VIEWER load được nhưng create trả `403`;
   - `X-Request-Id` trên UI xuất hiện trong log `tenant-demo`;
   - Redis/Kafka/Observability quan sát qua backend nếu feature flag tương ứng bật.
4. Dùng infra chung khi cần demo nhiều thành phần, hoặc target riêng khi chỉ test từng lab:

```bash
cd lab-code
make infra-up
make infra-status
```

4. Giữ nguyên baseline đã verify:

```bash
cd lab-code
make db-up
make app-test
```

5. Sau dry-run, chỉ polish báo cáo/demo script nếu có lỗi mạch trình bày.

### Tạm hoãn

- React Web UI: đã verify final demo trực quan; không mở rộng thành frontend product.
- Report polish: chỉ cập nhật summary ngắn sau mỗi mini-lab, không làm report lớn giữa chừng.
- MinIO advanced object management: presigned URL expiry, lifecycle/expiration, versioning, object lock/retention - để backlog sau core demo/UI hoặc sau các milestone công nghệ chính.
- Debezium: chỉ làm sau Kafka/Observability nếu còn cần; mỗi sprint một công nghệ và giữ scope nhỏ.

---

## Nguyên tắc out-of-scope cho Phase 1

1. Không triển khai full production Keycloak/OIDC/IAM platform; Keycloak mini-lab hiện chỉ là local learning flow.
2. Không chạy nhiều stack cùng lúc; mỗi sprint chỉ một công nghệ chính.
3. Không biến `master_data` demo thành full ERP/accounting domain.
4. Không thêm Swagger/OpenAPI, pagination nâng cao, role matrix phức tạp hoặc UI design lớn khi mini-lab công nghệ hiện tại chưa xong.
5. Không commit local/private notes, mentor feedback riêng tư hoặc prompt thô.
