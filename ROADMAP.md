# ROADMAP — Phase 1: ERP/Kế toán SaaS Multi-tenant

> **Bắt đầu:** Thứ Ba 28/04/2026 · **Deadline:** Thứ Hai 25/05/2026
> **Chu kỳ báo cáo:** mỗi 2-3 ngày phải có output có thể trình bày
> **Phương châm:** tự học + tự code trước, Agent review sau

---

## Quick Stats

| Chỉ số | Giá trị |
|--------|:-------:|
| **Tiến độ** | 25% |
| **Tổng task** | 48 |
| **Đã hoàn thành** | 12 / 48 |
| **Focus hiện tại** | Migration & Locking bridge + bám lại bản đồ công nghệ Phase 1 |
| **Milestone tiếp theo** | #2 — PostgreSQL migration safety + technology scope alignment |

---

## Chú thích

| Tag | Ý nghĩa | File/khu vực thường dùng |
|-----|---------|--------------------------|
| `[LÝ THUYẾT]` | Đọc, hiểu, ghi lại ý chính | `docs/`, tham khảo `local/` nếu cần |
| `[THỰC HÀNH]` | Tự code/chạy lệnh/verify output | `lab-code/` |
| `[BÁO CÁO]` | Tổng hợp ngắn để báo cáo mentor/leader | `docs/99-tong-ket/`, `reports/`, `presentation-notes/` |
| `[REVIEW]` | Nhờ Agent review sau khi đã tự làm | code/docs đã viết |
| `[MILESTONE]` | Checkpoint có output cụ thể | demo, summary, curl output, test result |

---

## Trạng thái repo sau Milestone #1

- SQL playground đã có đủ `01` đến `05` trong `lab-code/sql-playground/`.
- Đã verify lại baseline: schema, sample data, EXPLAIN, index comparison bằng temp table, data leakage proof.
- `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` đã có summary Milestone #1.
- `lab-code/tenant-demo/` có skeleton đầy đủ: `pom.xml`, `application.yml`, Java TODO files, Flyway TODO migrations, `DataLeakageTest.java`. Chưa phải app runnable.
- `reports/latex/bao-cao-saas-multi-tenant.tex` và `presentation-notes/thuyet-trinh-saas-multi-tenant.md` đã tồn tại, nhưng cần cập nhật dần khi có Spring Boot output thật.
- `local/` có backup topic list cũ về kiến trúc target. Dùng làm context để lập kế hoạch, không dùng làm source of truth công khai.

Quyết định thiết kế Phase 1: demo runnable vẫn là Spring Boot + PostgreSQL multi-tenant nhỏ. Các công nghệ target như Keycloak, Kong, Redis, Kafka, Debezium, MinIO, Elasticsearch, Prometheus/Grafana/Loki sẽ được phủ theo tầng: một số có mini-note, phần lớn ở awareness-level, không ép chạy hết trong demo.

---

## Phase 1 Technology Coverage Map

| Topic | Mức phủ Phase 1 | Artifact dự kiến | Milestone | Trạng thái |
|---|---|---|---:|---|
| SaaS / ERP accounting SaaS context | Core theory | `docs/01-saas/`, `docs/00-gioi-thieu/`, report summary | #1, #6 | Đã có nền |
| Multi-tenant shared-table design | Core / must practice | SQL playground + Spring Boot demo | #1, #4 | SQL đã xong, app chưa |
| Tenant isolation & data leakage | Core / must practice | `05-data-leakage-test.sql`, curl proof, `DataLeakageTest.java` | #1, #5 | SQL đã xong, test app chưa |
| PostgreSQL schema, indexing, EXPLAIN | Core / must practice | SQL `01-04`, docs query plan | #1 | Đã xong baseline |
| Migration, locking, rollback mindset | Important / mini-lab | `06-migration-lock-observation.sql`, summary ngắn | #2 | Chưa làm |
| Spring Boot backend PoC | Core / must practice | `lab-code/tenant-demo/`, `make app-run` | #3 | Skeleton |
| Flyway migration baseline | Core / must practice | `V1-V3` migration chạy được | #3 | Skeleton |
| TenantContext / TenantFilter | Core / must practice | Java implementation + curl/log proof | #3 | Skeleton |
| Tenant-aware repository/service/controller | Core / must practice | MasterData API + curl proof | #4 | Skeleton |
| Basic verification / curl / tests | Core / must practice | curl commands + `make app-test` | #4, #5, #7 | Chưa làm |
| Auth/AuthZ, JWT, RBAC, Keycloak | Important / concise notes | Summary: tự implement vs Keycloak, RBAC tenant-scope | #5 | Có local context, chưa chuẩn hóa public |
| Service decomposition / modular monolith | Important / concise notes | Architecture note: modular monolith vs microservices | #6 | Có local context, chưa chuẩn hóa public |
| API Gateway, service discovery, load balancing, Kong | Awareness / theory | 1 bảng: dùng khi nào, vì sao chưa chạy trong demo | #6 | Có local context |
| Redis cache strategy | Important / mini design | Tenant-safe cache key + invalidation note | #6 | Có local context |
| Kafka / async messaging | Awareness / theory | Sync vs async, use case kế toán, không implement | #6 | Có local context |
| Debezium CDC + Kafka | Awareness / theory | CDC giải quyết gì, vì sao không làm Phase 1 | #6 | Có local context |
| Elasticsearch / search service basics | Awareness / theory | Search service là gì, khác DB query thế nào | #6 | Missing public note |
| MinIO / object storage | Awareness / theory | File storage strategy theo tenant | #6 | Có local context |
| gRPC / internal service communication | Awareness / theory | REST vs gRPC vs Kafka | #6 | Có local context gián tiếp |
| Realtime: SignalR / WebSocket / SSE / long polling | Awareness / theory | Realtime dùng cho notification/status nào | #6 | Mention trong local architecture |
| Observability: Prometheus, Grafana, Loki | Important / concise notes | Minimum viable observability + tenant-aware logging | #6 | Có local context |
| External integrations trong ERP/SME | Awareness / theory | Ví dụ hóa đơn, ngân hàng, thuế, đối tác | #6 | Có local context nghiệp vụ |
| LLM/AI provider integration | Awareness / theory | Nhận diện vị trí trong kiến trúc, không implement | #6 | Mention trong local context |
| React frontend | Awareness / optional | Không bắt buộc trong final demo; chỉ hiểu vai trò portal | #6/#8 | Có local context |
| Full production microservices stack | Out of scope | Không chạy toàn bộ stack trong Phase 1 | #8 | Out of scope |
| Full ERP/accounting domain implementation | Out of scope | Chỉ dùng `master_data` làm module minh họa | #8 | Out of scope |

---

## Sprint 1 (28/04 → 01/05): SQL Playground Baseline — Đã đóng

### Thứ Ba 28/04 — Setup & Multi-tenant Core

- [x] `[LÝ THUYẾT]` Review `docs/02-multi-tenant/cac-mo-hinh-tenant-isolation.md` — nắm 3 mô hình tenant isolation
- [x] `[THỰC HÀNH]` Chạy `make db-up` trong `lab-code/` — verify PostgreSQL container
- [x] `[THỰC HÀNH]` Chạy `make sql-1` — verify bảng `tenants` và `master_data`
- [x] `[THỰC HÀNH]` Chạy `make sql-2` — verify sample data và `UNIQUE (tenant_id, code)`

### Thứ Tư 29/04 — Index & EXPLAIN

- [x] `[LÝ THUYẾT]` Đọc `docs/03-backend-database-mo-rong/index-va-query-tenant-aware.md`
- [x] `[THỰC HÀNH]` Tự code `lab-code/sql-playground/03-query-with-explain.sql` — chạy `EXPLAIN ANALYZE`
- [x] `[THỰC HÀNH]` Tự code `lab-code/sql-playground/04-index-comparison.sql` — so sánh no index, `tenant_id` index, composite index

### Thứ Năm 30/04 — Data Leakage & Noisy Neighbor

- [x] `[LÝ THUYẾT]` Đọc `docs/03-backend-database-mo-rong/noisy-neighbor-shared-table.md`
- [x] `[LÝ THUYẾT]` Tổng hợp ý chính về noisy neighbor trong shared-table multi-tenant
- [x] `[THỰC HÀNH]` Tự code `lab-code/sql-playground/05-data-leakage-test.sql` — chứng minh query thiếu `tenant_id` có thể lộ data

### Milestone #1 — SQL Playground Tenant-aware

> **Theory covered:** SaaS/multi-tenant baseline, shared-table model, tenant-aware unique constraint, noisy neighbor.
> **Practice output:** SQL schema, sample data, EXPLAIN, index comparison, temp table experiment, data leakage proof.
> **Report artifact:** `docs/99-tong-ket/nhung-gi-da-nam-duoc.md`.
> **Files để show:** `lab-code/sql-playground/01-05`, `docs/03-backend-database-mo-rong/index-va-query-tenant-aware.md`, `docs/99-tong-ket/nhung-gi-da-nam-duoc.md`.
> **Done criteria:** `make sql-all`, `make sql-3`, `make sql-4`, `make sql-5` chạy pass trên database local sạch.
> **Out of scope:** Spring Boot, Flyway app migration, API demo.

- [x] `[MILESTONE]` Tổng hợp output 5 file SQL playground → ghi chú kết quả
- [x] `[BÁO CÁO]` Cập nhật `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` — thêm phần SQL thực hành

---

## Sprint 1.5 (07/05 → 08/05): Migration & Locking Bridge

Mục tiêu: đủ hiểu mindset schema change an toàn trước khi dùng Flyway trong Spring Boot. Không học quá sâu PostgreSQL internals ở đây.

### Thứ Năm 07/05 — Migration safety tối thiểu

- [ ] `[LÝ THUYẾT]` Đọc `docs/03-backend-database-mo-rong/migration-lock-rollback.md` — output: 5-7 bullet về safe vs risky migration trong `docs/99-tong-ket/nhung-gi-da-nam-duoc.md`; không overdo thành guide production
- [ ] `[THỰC HÀNH]` Tự tạo khi bắt đầu task: `lab-code/sql-playground/06-migration-lock-observation.sql` — thử vài `ALTER TABLE` nhỏ trên `master_data`; verify baseline bằng `cd lab-code && make db-up && make sql-reset && make sql-all`
- [ ] `[BÁO CÁO]` Ghi summary ngắn cho Milestone #2: lock là gì, vì sao migration ảnh hưởng nhiều tenant, rollback cần chuẩn bị gì

### Milestone #2 — PostgreSQL Migration Safety + Scope Alignment

> **Theory covered:** migration, lock, rollback, backward-compatible schema mindset.
> **Practice output:** một script/ghi chú quan sát `ALTER TABLE` hoặc lock trên PostgreSQL local.
> **Report artifact:** cập nhật `docs/99-tong-ket/nhung-gi-da-nam-duoc.md`.
> **Files để show:** `docs/03-backend-database-mo-rong/migration-lock-rollback.md`, `lab-code/sql-playground/06-migration-lock-observation.sql` nếu có, roadmap technology coverage map.
> **Done criteria:** có summary ngắn + ít nhất một quan sát thực hành.
> **Out of scope:** partitioning sâu, VACUUM tuning, zero-downtime migration production thật.

- [ ] `[MILESTONE]` Chốt Milestone #2 — PostgreSQL safety summary + output thực hành nhỏ

---

## Sprint 2 (09/05 → 10/05): Spring Boot Bootstrap + Flyway + Tenant Context

Mục tiêu: biến `lab-code/tenant-demo/` từ skeleton thành app Spring Boot khởi động được, migrate schema được và nhận diện tenant từ request.

### Thứ Bảy 09/05 — Bootstrap app và Flyway baseline

- [ ] `[THỰC HÀNH]` Tự hoàn thiện `lab-code/tenant-demo/pom.xml` và `application.yml` — dependencies Web, JPA, PostgreSQL, Flyway, Test; verify bằng `cd lab-code && make app-run`
- [ ] `[THỰC HÀNH]` Tự code `TenantDemoApplication.java` và Flyway `V1`, `V2`, `V3` dựa trên SQL baseline; verify Flyway tạo bảng, không dùng Hibernate auto-create schema

### Chủ Nhật 10/05 — TenantContext và TenantFilter

- [ ] `[THỰC HÀNH]` Tự code `TenantContext.java` và `TenantFilter.java` — Phase 1 dùng `X-Tenant-Id`, clear bằng `finally`; không overdo JWT thật ở bước này
- [ ] `[THỰC HÀNH]` Verify bằng curl/log: request có `X-Tenant-Id` thì app nhận đúng tenant; request thiếu/invalid header được xử lý rõ
- [ ] `[BÁO CÁO]` Cập nhật `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` — thêm mục Spring Boot bootstrap, Flyway và TenantFilter

### Milestone #3 — Spring Boot Foundation

> **Theory covered:** request flow, TenantContext, Flyway baseline, vì sao dùng header giả lập thay JWT thật ở Phase 1.
> **Practice output:** app start, Flyway migrate, TenantFilter hoạt động.
> **Report artifact:** summary ngắn trong `docs/99-tong-ket/`.
> **Files để show:** `pom.xml`, `application.yml`, `TenantDemoApplication.java`, `TenantContext.java`, `TenantFilter.java`, `db/migration/V1-V3`.
> **Done criteria:** `make app-run` pass; curl/log chứng minh tenant context hoạt động.
> **Out of scope:** CRUD đầy đủ, Keycloak, RBAC production, API Gateway.

- [ ] `[MILESTONE]` Demo: Spring Boot start → Flyway migrate → TenantFilter hoạt động

---

## Sprint 3 (11/05 → 14/05): Tenant-aware MasterData API

Mục tiêu: có endpoint thật để chứng minh tenant-scoped data access. Ưu tiên rõ, chạy được, dễ giải thích; chưa cần framework generic quá phức tạp.

### Thứ Hai 11/05 — Entity và repository tenant-aware

- [ ] `[THỰC HÀNH]` Tự code `TenantAwareEntity.java`, `MasterData.java`, `MasterDataRepository.java` — method explicit có `tenantId`; không overdo custom base repository nếu chưa cần
- [ ] `[LÝ THUYẾT]` Ghi 3 rule backend query phải nhớ vào `docs/99-tong-ket/`: tenant từ trusted context, query luôn scoped, không tin request body

### Thứ Ba 12/05 → Thứ Tư 13/05 — Service, controller và curl verification

- [ ] `[THỰC HÀNH]` Tự code `MasterDataService.java` và `MasterDataController.java` — endpoint đọc danh sách và tìm theo `code` trong tenant hiện tại
- [ ] `[THỰC HÀNH]` Verify bằng curl: `X-Tenant-Id: 1` chỉ thấy data tenant 1; `X-Tenant-Id: 2` chỉ thấy data tenant 2; ghi command/output ngắn
- [ ] `[REVIEW]` Sau khi tự chạy được, nhờ Agent review focused: có query nào quên `tenantId` không, service/controller có nhận tenant sai nguồn không

### Milestone #4 — Tenant-aware API Demo

> **Theory covered:** tenant-aware repository/service/controller flow, data leakage ở tầng API.
> **Practice output:** REST endpoint tenant-scoped cho `master_data` + curl proof.
> **Report artifact:** curl pattern trong `docs/99-tong-ket/`.
> **Files để show:** entity, repository, service, controller, curl commands/output.
> **Done criteria:** app chạy; endpoint trả data scoped theo `X-Tenant-Id`; query/service không tin tenant từ request body.
> **Out of scope:** full ERP CRUD, UI, pagination nâng cao, auth/JWT thật.

- [ ] `[MILESTONE]` Demo: Tenant A GET chỉ thấy data A; Tenant B GET chỉ thấy data B

---

## Sprint 4 (15/05 → 17/05): Tests + Auth/RBAC Awareness

Mục tiêu: biến bài học SQL leakage thành test backend, đồng thời phủ Auth/AuthZ/Keycloak ở mức hiểu đúng bối cảnh target.

### Thứ Sáu 15/05 — Test chống leakage

- [ ] `[THỰC HÀNH]` Tự code `DataLeakageTest.java` — tối thiểu 3 case: tenant A không thấy tenant B, missing/invalid tenant bị chặn, query by code vẫn scoped theo tenant
- [ ] `[THỰC HÀNH]` Chạy `cd lab-code && make app-test` — test pass hoặc ghi lỗi cụ thể nếu fail

### Thứ Bảy 16/05 → Chủ Nhật 17/05 — Auth/AuthZ/Keycloak awareness

- [ ] `[LÝ THUYẾT]` Dựa trên public docs hiện có và local context cũ, ghi summary ngắn trong `docs/99-tong-ket/`: AuthN vs AuthZ, JWT, RBAC tenant-scope, Keycloak/OAuth2/OIDC dùng để làm gì; không implement Keycloak
- [ ] `[BÁO CÁO]` Cập nhật milestone summary: test chống data leakage + giới hạn Phase 1 chưa có auth production

### Milestone #5 — Testable Tenant Isolation + Auth Awareness

> **Theory covered:** AuthN/AuthZ, JWT, RBAC tenant-scope, Keycloak ở mức awareness.
> **Practice output:** `DataLeakageTest.java` hoặc test gần tương đương.
> **Report artifact:** summary trong `docs/99-tong-ket/`.
> **Files để show:** test file, service/repository/controller liên quan, test output.
> **Done criteria:** test pass hoặc có blocker cụ thể; giải thích được vì sao role check không thay tenant ownership check.
> **Out of scope:** Keycloak container, OAuth2/OIDC full flow, production-grade Spring Security.

- [ ] `[MILESTONE]` Chốt test isolation + Auth/RBAC awareness summary

---

## Sprint 5 (18/05 → 19/05): Technology Overview Sprint

Mục tiêu: phủ bức tranh kiến trúc target đủ để báo cáo leader, nhưng không biến Phase 1 thành việc chạy toàn bộ stack microservices.

### Thứ Hai 18/05 — Architecture technologies map

- [ ] `[LÝ THUYẾT]` Tạo/cập nhật một section ngắn trong `docs/99-tong-ket/` hoặc `presentation-notes/`: modular monolith vs microservices, API Gateway/Kong, service discovery, load balancing; không implement Kong
- [ ] `[LÝ THUYẾT]` Ghi mini design Redis cache strategy: tenant-safe cache key, TTL, invalidation, vì sao cache key thiếu tenant gây leakage
- [ ] `[LÝ THUYẾT]` Ghi awareness note: Kafka vs REST/gRPC, Debezium CDC, Elasticsearch/search, MinIO/object storage, realtime, external integrations, LLM provider; mỗi topic 3-5 bullet, không overdo
- [ ] `[LÝ THUYẾT]` Ghi minimum viable observability: metrics/logs/traces, Prometheus/Grafana/Loki, tenant-aware logging; chưa cần chạy stack

### Milestone #6 — Target Architecture Awareness Package

> **Theory covered:** Keycloak/Kong/Redis/Kafka/Debezium/MinIO/Elasticsearch/gRPC/realtime/observability ở mức awareness hoặc mini-note.
> **Practice output:** không yêu cầu chạy stack; output là architecture map, bảng trade-off, và liên hệ với demo Spring Boot.
> **Report artifact:** `docs/99-tong-ket/` và/hoặc `presentation-notes/thuyet-trinh-saas-multi-tenant.md`.
> **Files để show:** roadmap coverage map, summary notes, presentation note.
> **Done criteria:** giải thích được công nghệ nào thuộc demo, công nghệ nào chỉ awareness, vì sao không nhồi hết vào Phase 1.
> **Out of scope:** chạy Kafka/Keycloak/Kong/Redis/MinIO/Prometheus/Grafana/Loki/Elasticsearch cùng lúc.

- [ ] `[MILESTONE]` Chốt architecture awareness package — đủ nói chuyện với mentor về target stack

---

## Sprint 6 (20/05 → 22/05): Report, Presentation và Demo Script

Mục tiêu: cập nhật tài liệu để có thể báo cáo mentor/leader mà không đợi sát deadline.

### Thứ Tư 20/05 — LaTeX report update

- [ ] `[BÁO CÁO]` Cập nhật `reports/latex/bao-cao-saas-multi-tenant.tex` — thêm kết quả SQL playground, Spring Boot PoC, test leakage, technology coverage map; không viết thành sách giáo khoa
- [ ] `[BÁO CÁO]` Compile thử LaTeX nếu môi trường có `xelatex`; nếu không có, ghi rõ chưa verify PDF

### Thứ Năm 21/05 → Thứ Sáu 22/05 — Presentation và dry-run script

- [ ] `[BÁO CÁO]` Cập nhật `presentation-notes/thuyet-trinh-saas-multi-tenant.md` — thêm flow demo: SQL → Flyway → TenantFilter → API → test → technology map
- [ ] `[THỰC HÀNH]` Ghi demo checklist cuối file thuyết trình: `make db-up`, `make app-run`, curl 2 tenant, `make app-test`
- [ ] `[REVIEW]` Nhờ Agent review report/presentation ở mức mạch lạc và không overclaim

### Milestone #7 — Mentor-facing Report + Demo Dry-run

> **Theory covered:** tổng hợp SaaS, multi-tenant, PostgreSQL, Spring Boot PoC, technology target awareness.
> **Practice output:** demo checklist đã chạy qua + curl/test output.
> **Report artifact:** LaTeX report + presentation notes.
> **Files để show:** `reports/latex/`, `presentation-notes/`, `lab-code/tenant-demo/`, `docs/99-tong-ket/`.
> **Done criteria:** có thể trình bày 10-15 phút và chạy demo chính từ đầu đến cuối.
> **Out of scope:** thiết kế slide đẹp, publish PDF nếu chưa cần, thêm feature mới sau dry-run.

- [ ] `[MILESTONE]` Dry-run: trình bày → chạy app/curl/test → ghi lại vấn đề còn lại

---

## Sprint 7 (23/05 → 25/05): Đóng Gói và Bảo Vệ Phase 1

### Thứ Bảy 23/05 → Chủ Nhật 24/05 — Polish cuối

- [ ] `[BÁO CÁO]` Polish `docs/99-tong-ket/`, report, presentation theo feedback dry-run; chỉ sửa nội dung sai hoặc thiếu, không mở scope mới
- [ ] `[THỰC HÀNH]` Final rehearsal: mở tài liệu, chạy command demo chính, chuẩn bị Q&A và fallback nếu demo lỗi

### Thứ Hai 25/05 — Milestone #8: Bảo vệ Phase 1

> **Theory covered:** giải thích được bức tranh target architecture và phần đã thực hành sâu.
> **Practice output:** runnable demo nhỏ + test/curl proof.
> **Report artifact:** report, presentation notes, roadmap.
> **Files để show:** SQL playground, Spring Boot PoC, docs summary, report, presentation.
> **Done criteria:** trình bày được trade-off shared-table multi-tenant, chứng minh demo chạy, nói rõ giới hạn hiện tại.
> **Out of scope:** production ERP hoàn chỉnh, auth/RBAC đầy đủ, distributed system nâng cao.

- [ ] `[MILESTONE]` Trình bày Phase 1 + demo runnable + Q&A với leader/mentor
- [ ] `[BÁO CÁO]` Ghi feedback leader vào `local/` hoặc ghi chú local-only, không commit nội dung riêng tư

---

## Tổng quan Milestones

| # | Ngày mục tiêu | Output báo cáo |
|:-:|:-------------:|---------------|
| 1 | 01/05 | SQL playground results + tenant-aware data isolation proof |
| 2 | 08/05 | Migration & Locking safety summary + scope alignment |
| 3 | 10/05 | Spring Boot starts + Flyway baseline + TenantFilter |
| 4 | 14/05 | Tenant-aware MasterData API demo bằng curl |
| 5 | 17/05 | Data leakage tests + Auth/RBAC/Keycloak awareness |
| 6 | 19/05 | Target architecture technology awareness package |
| 7 | 22/05 | Mentor-facing report + runnable demo dry-run |
| 8 | 25/05 | Phase 1 defense: trình bày + runnable demo + Q&A |

---

## File Reference

| Cần làm gì | File/khu vực |
|------------|--------------|
| SaaS lý thuyết | `docs/01-saas/tong-quan-saas.md` |
| Multi-tenant | `docs/02-multi-tenant/*.md` |
| PostgreSQL/backend DB | `docs/03-backend-database-mo-rong/*.md` |
| Tổng kết tiến độ | `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` |
| SQL thực hành | `lab-code/sql-playground/*.sql` |
| Spring Boot PoC | `lab-code/tenant-demo/` |
| Make commands | `lab-code/Makefile` → `make help` |
| Báo cáo LaTeX | `reports/latex/bao-cao-saas-multi-tenant.tex` |
| Thuyết trình/demo script | `presentation-notes/thuyet-trinh-saas-multi-tenant.md` |
| Context private cũ | `local/` — chỉ dùng tham khảo, không làm source of truth công khai |

---

## Việc Làm Ngay

Mở trước: `docs/03-backend-database-mo-rong/migration-lock-rollback.md`.

Task tiếp theo:

1. Đọc tài liệu migration/locking, ghi 5-7 bullet vào `docs/99-tong-ket/nhung-gi-da-nam-duoc.md`.
2. Tạo khi bắt đầu task `lab-code/sql-playground/06-migration-lock-observation.sql`.
3. Chạy baseline: `cd lab-code && make db-up && make sql-reset && make sql-all`.
4. Thực hành một vài `ALTER TABLE` nhỏ để quan sát lock/ảnh hưởng.
5. Done khi có summary ngắn + output/nhận xét thực hành đủ báo cáo Milestone #2.

---

## Nguyên tắc

1. **Tự code trước.** Nhờ Agent review sau khi đã tự viết và chạy thử.
2. **Milestone phải có output.** Không chốt milestone chỉ bằng đọc lý thuyết.
3. **Không overdo.** Phase 1 cần runnable demo nhỏ và technology awareness, không cần production ERP.
4. **Layered coverage.** Core topics phải thực hành; important topics có mini-note; awareness topics chỉ cần giải thích đúng.
5. **Report dần.** Mỗi milestone cập nhật một đoạn ngắn, không dồn tới cuối.
6. **Không commit local-only notes.** Nháp, prompt, feedback riêng tư ở `local/`.
7. **Nếu stuck hơn 2 tiếng:** ghi blocker, tạo fallback nhỏ hơn, tiếp tục tiến độ.
