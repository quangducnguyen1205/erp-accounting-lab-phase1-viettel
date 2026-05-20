# ROADMAP - Phase 1 Fast-track: ERP/Kế toán SaaS Multi-tenant

> **Bắt đầu:** Thứ Ba 28/04/2026
> **Deadline ban đầu:** Thứ Hai 25/05/2026
> **Cửa sổ học mở rộng đề xuất:** đến Thứ Hai 01/06/2026
> **Cập nhật roadmap:** 19/05/2026
> **Chu kỳ báo cáo:** mỗi 2-3 ngày phải có output có thể trình bày
> **Phương châm:** tự học + tự code trước, Codex tạo note/skeleton và review sau

---

## Quick Stats

| Chỉ số | Giá trị |
|--------|:-------:|
| **Tiến độ** | 75% |
| **Tổng task** | 79 |
| **Đã hoàn thành** | 59 / 79 |
| **Focus hiện tại** | Keycloak/OIDC mini-lab - manual token flow |
| **Milestone tiếp theo** | #9 - Keycloak/OIDC mini-lab hoặc awareness evidence |
| **Demo cuối Phase 1** | Spring Boot + PostgreSQL/Flyway + tenant-aware API + JWT tạm, có hướng Keycloak mini-lab nếu kịp |

Ghi chú: phần trăm giảm vì roadmap được mở rộng sau feedback mentor. Trọng tâm mới không chỉ demo chạy được, mà còn vá các lỗ hổng nền tảng về PostgreSQL index/query pattern, Flyway rollback/failure, ACID/isolation và học công nghệ theo ngữ cảnh thật.

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

---

## Hiện trạng repo đã audit

### Đã hoàn thành hoặc gần hoàn thành

- SQL playground có `01` đến `06`: schema baseline, sample data, EXPLAIN, index comparison, temp table experiment, data leakage proof, migration/locking observation.
- Spring Boot tenant demo đã có Maven wrapper, `pom.xml`, `application.yml`, `.env.example`, PostgreSQL Docker Compose và Flyway `V1-V3`.
- `TenantContext` / `TenantFilter` đã implement và từng được runtime verify bằng header `X-Tenant-Id`.
- Entity/repository/service/controller layer đã có API `master_data` tenant-aware cơ bản.
- `DataLeakageTest.java` đã có regression tests cho tenant isolation bằng `make app-test`.
- JWT tạm đã implement bằng Spring Security: Bearer token local, `tenant_id` claim, `JwtTenantContextFilter`, dev token endpoint và test MockMvc.
- Các note Spring Boot đã có trong `docs/04-spring-boot/`; các note JWT/Spring Security đã có trong `docs/05-security/`.

### Gap mới sau feedback mentor

- PostgreSQL index hiện mới học ở mức baseline. Cần học sâu hơn query pattern: `LIKE 'abc%'`, `LIKE '%abc%'`, contains search, B-tree prefix, trigram/GIN, composite index leftmost prefix, expression index như `lower(column)`, selectivity và lý do planner vẫn chọn Seq Scan.
- Migration/locking hiện mới có local observation. Cần bổ sung cách migration được execute bằng Flyway, schema history, validate/repair, migration fail giữa chừng, forward migration vs rollback và undo migration theo điều kiện của Flyway.
- ACID/isolation levels chưa có note riêng. Cần học dirty read, non-repeatable read, phantom read, PostgreSQL default isolation và tác động với shared-table SaaS.
- Keycloak/OIDC không nên chỉ dừng ở awareness mãi. Sau JWT tạm cần có mini-lab hoặc ít nhất một flow thật nhỏ nếu còn kịp.
- Redis/MinIO/Elasticsearch nên học khi có feature tương ứng, không chỉ ghi lý thuyết rời rạc.
- DDD là chủ đề hợp lý nhưng chưa urgent; để awareness/post-demo design improvement.
- `README.md` vẫn mô tả repo thiên về knowledge base; có thể cập nhật sau khi demo scope ổn định hơn.

### Bài học từ sơ đồ kiến trúc target

Sơ đồ target có React frontend, API Gateway/service discovery/load balancer, Keycloak/OAuth2/OIDC, nhiều backend services, PostgreSQL cluster, Redis, Kafka, Debezium CDC, MinIO, Elasticsearch/Elastic Stack, gRPC, realtime, observability, LLM providers và external services. Phase 1 cần hiểu vai trò của các phần này theo ngữ cảnh thật, nhưng chỉ implement sâu những phần phục vụ demo hoặc mini-lab gần nhất.

---

## Demo cuối Phase 1 - phạm vi thực tế

### Demo chính sẽ có

- Spring Boot backend chạy được bằng `make app-run`.
- PostgreSQL local + Flyway tạo schema.
- Shared-table multi-tenant với `tenant_id`.
- Tenant-aware API cho `master_data`.
- Test hoặc curl chứng minh không lộ data cross-tenant.
- JWT tạm: token local có `tenant_id` claim, backend validate để set tenant context.
- Report/presentation note có sơ đồ flow, trade-off và giới hạn hiện tại.
- Nếu còn thời gian sau các gap PostgreSQL/Flyway/ACID: React UI nhỏ để minh họa tenant-aware flow.

### Demo/mini-lab mở rộng nếu khả thi

- Keycloak mini-lab: lấy token OIDC thật ở mức local/dev và so sánh với JWT tạm.
- Redis mini-lab chỉ khi có bài cache tenant-aware.
- MinIO mini-lab chỉ khi có bài upload file.
- Elasticsearch mini-lab chỉ khi có bài search vượt quá `LIKE`/PostgreSQL đơn giản.

### Không đưa vào Phase 1 implementation

- Full production Keycloak/RBAC platform.
- API Gateway/Kong thật.
- Kafka/Debezium/Grafana stack chạy đầy đủ.
- Full ERP/accounting workflow.
- Full production deployment, HA database cluster, audit/compliance hoàn chỉnh.

---

## Phase 1 Technology Coverage Map

| Topic trong kiến trúc | Mức phủ Phase 1 | Theory doc dự kiến | Code/demo artifact | Verification | Milestone | Trạng thái |
|---|---|---|---|---|---:|---|
| SaaS / ERP accounting context | Core theory | `docs/01-saas/`, `docs/00-gioi-thieu/` | Không cần code riêng | Report/presentation giải thích đúng | #1, #12 | Đã có nền |
| Multi-tenant shared-table | Core demo | `docs/02-multi-tenant/` | SQL playground + Spring Boot `master_data` | SQL, curl, test | #1, #4 | Đã có |
| PostgreSQL schema/index/EXPLAIN baseline | Core demo | `docs/03-backend-database-mo-rong/index-va-query-tenant-aware.md` | `lab-code/sql-playground/01-04` | `make sql-*` | #1 | Đã có |
| PostgreSQL index query patterns | Core mini-lab | `docs/03-backend-database-mo-rong/index-query-patterns-postgresql.md` | `07-index-query-patterns.sql` | EXPLAIN cho prefix/contains/function/composite cases | #6 | Gap mới |
| Migration/locking baseline | Mini-lab | `docs/03-backend-database-mo-rong/migration-lock-rollback.md` | `06-migration-lock-observation.sql` | Local SQL observation | #2 | Đã có |
| Flyway rollback/failure handling | Core theory + mini-lab | `docs/03-backend-database-mo-rong/flyway-rollback-failure-handling.md` | optional `08-flyway-failure-observation.md/sql` | Flyway command/log summary | #7 | Gap mới |
| ACID/isolation levels | Core theory + mini-lab | `docs/03-backend-database-mo-rong/acid-isolation-levels-postgresql.md` | `09-acid-isolation-observation.sql` | Two-session observation nếu cần | #8 | Gap mới |
| Spring Boot backend | Core demo | `docs/04-spring-boot/` | `lab-code/tenant-demo/` | `make app-run` | #3, #4 | Đã có |
| Flyway schema baseline | Core demo | `docs/04-spring-boot/spring-boot-bootstrap-config.md` | `V1-V3` migrations | Startup logs/Flyway logs | #3 | Đã có |
| TenantContext/TenantFilter | Core demo | `docs/04-spring-boot/request-filter-threadlocal.md` | `TenantContext.java`, `TenantFilter.java` | curl/log/test | #3, #4 | Đã có |
| Tenant-aware service/controller | Core demo | `docs/04-spring-boot/service-controller-curl-flow.md` | `MasterDataService`, `MasterDataController` | curl tenant 1/2 | #4 | Đã có |
| Data leakage tests | Core demo | `docs/04-spring-boot/testing-tenant-isolation.md` | `DataLeakageTest.java` | `make app-test` | #4 | Đã có |
| Temporary JWT auth | Core bridge | `docs/05-security/jwt-spring-security-temporary.md` | `SecurityConfig`, `JwtTokenService`, `JwtTenantContextFilter` | MockMvc + HTTP valid/invalid JWT | #5 | Đã implement, cần summary đóng |
| Keycloak/OAuth2/OIDC | Mini-lab nếu khả thi | `docs/05-security/keycloak-oidc-mental-model.md`, `docs/05-security/keycloak-oauth2-oidc-awareness.md`, `docs/05-security/keycloak-admin-console-guide.md` | optional `lab-code/keycloak-lab/` hoặc Docker Compose profile | Lấy token local/dev, giải thích issuer/JWKS/claims | #9 | Nâng từ awareness lên mini-lab |
| RBAC/tenant-scope | Important theory | `docs/05-security/rbac-tenant-scope.md` | JWT claim note, không role matrix lớn | Test/summary tenant vs role | #5, #9 | Chưa có |
| React frontend | Optional core demo | `docs/06-frontend/react-tenant-demo-ui.md` | `lab-code/tenant-ui/` | browser/UI + curl fallback | #10 | Chưa có |
| API Gateway/service discovery/load balancer | Awareness | `docs/07-architecture/api-gateway-service-discovery.md` | Không chạy gateway | Architecture summary | #11 | Chưa có |
| Redis cache strategy | Mini-lab khi có cache need | `docs/07-architecture/redis-tenant-cache.md` | tenant-safe cache key mini example | Explain/cache-key review | #11 | Just-in-time |
| Kafka async messaging | Awareness | `docs/07-architecture/kafka-async-messaging.md` | Không chạy Kafka | Use-case summary | #11 | Chưa có |
| Debezium CDC + Kafka | Awareness | `docs/07-architecture/debezium-cdc.md` | Không chạy CDC | CDC role summary | #11 | Chưa có |
| MinIO / S3 object storage | Mini-lab khi có file feature | `docs/07-architecture/minio-object-storage.md` | optional upload mini-lab | Upload/download nếu làm feature | #11 | Just-in-time |
| Elasticsearch / Elastic Stack | Mini-lab khi có search need | `docs/07-architecture/elasticsearch-search-service.md` | optional search mini-lab | Search behavior summary | #11 | Just-in-time |
| gRPC internal communication | Awareness | `docs/07-architecture/grpc-internal-communication.md` | Không chạy gRPC | REST vs gRPC vs Kafka table | #11 | Chưa có |
| Realtime: SignalR/Socket/SSE/Long polling | Awareness | `docs/07-architecture/realtime-communication.md` | Không chạy realtime | When to use which note | #11 | Chưa có |
| Observability: Prometheus/Grafana/Loki | Important mini-note | `docs/07-architecture/observability-prometheus-grafana-loki.md` | log/health/metric awareness only | Explain metric/log/tracing role | #11 | Chưa có |
| LLM providers: OpenAI/OpenRouter/others | Awareness | `docs/07-architecture/llm-provider-integration.md` | Không gọi API thật | Integration role note | #11 | Chưa có |
| External services: e-contract, eCommerce, CRM, HR, documents, digital signing | Awareness | `docs/07-architecture/external-integrations-erp.md` | Không tích hợp thật | Boundary/use-case summary | #11 | Chưa có |
| DDD/domain boundaries | Later awareness | `docs/08-design/ddd-awareness.md` | Không refactor code theo DDD ở Phase 1 | Post-demo design note | #12 | Later |
| Full production microservices stack | Out of scope | Chỉ ghi giới hạn trong report | Không implement | Nêu rõ không thuộc Phase 1 | #12 | Out of scope |
| Full ERP/accounting domain | Out of scope | Chỉ dùng ví dụ nghiệp vụ | Không implement | Nêu rõ `master_data` chỉ là slice demo | #12 | Out of scope |

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

### Sprint 9 - 22/05 đến 24/05: Keycloak mini-lab nếu khả thi

Mục tiêu: nâng auth từ JWT tạm sang hiểu công nghệ thật theo feedback mentor, nhưng vẫn giữ phạm vi nhỏ.

- [x] `[LÝ THUYẾT]` Tạo/read `docs/05-security/keycloak-oidc-mental-model.md`, `docs/05-security/keycloak-oauth2-oidc-awareness.md` và `docs/05-security/keycloak-admin-console-guide.md` từ Keycloak/Spring Security docs: Authorization Server, Resource Server, issuer, JWKS, access token, client, realm, Admin Console cơ bản.
- [x] `[SKELETON]` Chuẩn bị `lab-code/keycloak-lab/` hoặc Docker Compose profile có TODO comments nếu chạy Keycloak local là khả thi trên máy.
- [ ] `[THỰC HÀNH]` Tự thử flow nhỏ: tạo realm/client/user hoặc lấy token dev; nếu không kịp thì ghi rõ blocker và giữ JWT tạm.
- [ ] `[THỰC HÀNH]` So sánh JWT tạm hiện tại với Keycloak/OIDC thật: phần nào giống, phần nào khác, phần nào production mới cần.
- [ ] `[REVIEW]` Nhờ Codex review note/lab: không biến Keycloak thành full IAM project, không overclaim RBAC production.
- [ ] `[MILESTONE]` Chốt Milestone #9 - Keycloak/OIDC mini-lab hoặc awareness có evidence rõ.

### Sprint 10 - 25/05 đến 26/05: React UI hoặc demo script thay thế

Mục tiêu: nếu nền tảng backend/security/database đã ổn, thêm UI nhỏ; nếu không kịp, ưu tiên demo script backend chắc chắn.

- [ ] `[LÝ THUYẾT]` Tạo/read `docs/06-frontend/react-tenant-demo-ui.md` - React gọi REST API, env config, CORS ở mức tối thiểu, không học frontend lan man.
- [ ] `[SKELETON]` Nhờ Codex scaffold `lab-code/tenant-ui/` với TODO comments nếu chọn UI; nếu không, tạo demo script backend chi tiết.
- [ ] `[THỰC HÀNH]` Tự code UI tối thiểu hoặc hoàn thiện backend demo script: token tenant 1/2, danh sách `master_data`, lỗi auth, cross-tenant not found.
- [ ] `[REVIEW]` Nhờ Codex review UI/script; không thêm state management/library nặng nếu chưa cần.
- [ ] `[MILESTONE]` Chốt Milestone #10 - có đường demo mentor-facing, bằng UI nhỏ hoặc curl/script chắc chắn.

### Sprint 11 - 27/05 đến 29/05: Just-in-time architecture tech adoption map

Mục tiêu: phủ các công nghệ trong sơ đồ kiến trúc theo ngữ cảnh thật, nhưng chỉ mini-lab khi có feature cần.

- [ ] `[LÝ THUYẾT]` Tạo `docs/07-architecture/target-architecture-map.md` - map React, gateway, Keycloak, backend services, PostgreSQL, Redis, Kafka, MinIO, Elastic, observability, LLM, external systems vào vai trò tổng thể.
- [ ] `[LÝ THUYẾT]` Ghi chiến lược adoption: Redis khi cache, MinIO khi file upload, Elasticsearch khi search, Kafka khi async event, observability khi cần vận hành.
- [ ] `[SKELETON]` Nếu còn thời gian, chọn đúng một mini-lab nhỏ nhất theo nhu cầu demo: Redis cache key hoặc MinIO upload; không làm nhiều stack cùng lúc.
- [ ] `[BÁO CÁO]` Cập nhật `presentation-notes/thuyet-trinh-saas-multi-tenant.md` - phần implemented / mini-lab / awareness / out of scope.
- [ ] `[MILESTONE]` Chốt Milestone #11 - architecture coverage map đủ nói chuyện với mentor.

### Sprint 12 - 30/05 đến 01/06: Final report, demo script và DDD awareness

Mục tiêu: đóng gói Phase 1 mở rộng, báo cáo trung thực: cái gì đã chạy, cái gì đã học, cái gì còn là awareness.

- [ ] `[LÝ THUYẾT]` Tạo `docs/08-design/ddd-awareness.md` ngắn: entity/domain/service boundary ở mức nhận biết, không refactor demo theo DDD.
- [ ] `[BÁO CÁO]` Cập nhật `reports/latex/bao-cao-saas-multi-tenant.tex` - SQL, migration, Spring Boot API, JWT/Keycloak gap, PostgreSQL mentor gaps, architecture coverage; không overclaim production.
- [ ] `[BÁO CÁO]` Cập nhật `presentation-notes/thuyet-trinh-saas-multi-tenant.md` - demo script: DB → backend → auth → API → tests → PostgreSQL gaps → architecture map.
- [ ] `[THỰC HÀNH]` Dry-run demo: `make db-up`, `make app-run`, `make app-test`, HTTP/curl/UI nếu có; ghi issue nếu có.
- [ ] `[REVIEW]` Nhờ Codex review report/presentation/dry-run ở mức mạch lạc, đúng phạm vi, không overclaim.
- [ ] `[MILESTONE]` Trình bày Phase 1 mở rộng: runnable demo + gap learning + Q&A + giới hạn hiện tại + next steps sau Phase 1.

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
| 9 | 24/05 | Keycloak/OIDC mini-lab hoặc awareness evidence | Kế tiếp |
| 10 | 26/05 | React UI nhỏ hoặc backend demo script chắc chắn | Planned |
| 11 | 29/05 | Target architecture adoption map | Planned |
| 12 | 01/06 | Final report + runnable demo + Q&A | Planned |

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
| Security/JWT/Keycloak notes | `docs/05-security/` - đã có JWT tạm, cần Keycloak/RBAC awareness hoặc mini-lab |
| React UI notes | `docs/06-frontend/` - tạo khi tới task UI |
| Architecture awareness notes | `docs/07-architecture/` - tạo khi tới Sprint 7 |
| DDD awareness | `docs/08-design/ddd-awareness.md` - để cuối Phase 1 mở rộng |
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

### Ngày 14/05 đến 15/05 - đóng JWT bridge và gap report

1. Milestone #5 đã đóng: JWT tạm + gap report sau mentor feedback.
2. Verify đã pass:

```bash
cd lab-code
make app-test
```

3. File HTTP Client đã được làm sạch token thật; nếu cần token lặp lại, dùng private env local hoặc paste thủ công.

### Task tiếp theo: Keycloak/OIDC mini-lab

1. Đọc `docs/05-security/keycloak-oauth2-oidc-awareness.md` và `docs/05-security/keycloak-mini-lab-plan.md`.
2. Chạy `cd lab-code/keycloak-lab && docker compose up -d`.
3. Tạo realm/client/user theo `lab-code/keycloak-lab/README.md`.
4. Dùng `lab-code/keycloak-lab/http/keycloak-token-flow.http` để lấy token và kiểm tra `issuer`, `jwks_uri`, `tenant_id`.
5. Chưa chuyển sang React trước khi có Keycloak/OIDC awareness evidence hoặc mini-lab result rõ.

---

## Nguyên tắc out-of-scope cho Phase 1

1. Không triển khai full production Keycloak/OIDC/IAM platform; Keycloak mini-lab nhỏ vẫn có thể làm nếu kịp.
2. Không chạy Kafka, Debezium, Redis, MinIO, Elastic, Grafana stack thật nếu chưa có feature trực tiếp cần.
3. Không biến `master_data` demo thành full ERP/accounting domain.
4. Không thêm Swagger/OpenAPI, pagination nâng cao, role matrix phức tạp hoặc UI design lớn trước khi các gap nền tảng mentor chỉ ra được xử lý.
5. Không commit local/private notes, mentor feedback riêng tư hoặc prompt thô.
