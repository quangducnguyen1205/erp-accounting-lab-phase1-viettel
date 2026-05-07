# ROADMAP — Phase 1: ERP/Kế toán SaaS Multi-tenant

> **Bắt đầu:** Thứ Ba 28/04/2026 · **Deadline:** Thứ Hai 25/05/2026
> **Chu kỳ báo cáo:** mỗi 2-3 ngày phải có output có thể trình bày
> **Phương châm:** tự học + tự code trước, Agent review sau

---

## Quick Stats

| Chỉ số | Giá trị |
|--------|:-------:|
| **Tiến độ** | 24% |
| **Tổng task** | 51 |
| **Đã hoàn thành** | 12 / 51 |
| **Focus hiện tại** | Migration & Locking bridge → Spring Boot PoC |
| **Milestone tiếp theo** | #2 — Migration & Locking / PostgreSQL safety mindset |

---

## Chú thích

| Tag | Ý nghĩa | File/khu vực thường dùng |
|-----|---------|--------------------------|
| `[LÝ THUYẾT]` | Đọc, hiểu, ghi lại ý chính | `docs/` |
| `[THỰC HÀNH]` | Tự code/chạy lệnh/verify output | `lab-code/` |
| `[BÁO CÁO]` | Tổng hợp ngắn để báo cáo mentor/leader | `docs/99-tong-ket/`, `reports/`, `presentation-notes/` |
| `[REVIEW]` | Nhờ Agent review sau khi đã tự làm | code/docs đã viết |
| `[MILESTONE]` | Checkpoint có output cụ thể | demo, summary, curl output, test result |

---

## Trạng thái repo sau Milestone #1

- SQL playground đã có đủ `01` đến `05` trong `lab-code/sql-playground/`.
- Đã verify lại baseline: schema, sample data, EXPLAIN, index comparison bằng temp table, data leakage proof.
- `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` đã có summary Milestone #1.
- `lab-code/tenant-demo/` hiện vẫn là skeleton: có `pom.xml`, `application.yml`, Flyway migration files dạng TODO, nhưng chưa có Java implementation.
- `reports/latex/bao-cao-saas-multi-tenant.tex` và `presentation-notes/thuyet-trinh-saas-multi-tenant.md` đã tồn tại, nhưng cần cập nhật dần khi có Spring Boot output thật.

Quyết định sắp tới: làm một bridge ngắn về Migration & Locking để đủ nền tảng DB, sau đó chuyển nhanh sang Spring Boot PoC để kịp có runnable demo trước deadline.

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

> **Output artifact:** SQL playground results + summary trong `docs/99-tong-ket/nhung-gi-da-nam-duoc.md`.
> **Files để show:** `lab-code/sql-playground/01-05`, `docs/03-backend-database-mo-rong/index-va-query-tenant-aware.md`, `docs/99-tong-ket/nhung-gi-da-nam-duoc.md`.
> **Done criteria:** `make sql-all`, `make sql-3`, `make sql-4`, `make sql-5` chạy pass trên database local sạch.
> **Out of scope:** Spring Boot, Flyway app migration, API demo.

- [x] `[MILESTONE]` Tổng hợp output 5 file SQL playground → ghi chú kết quả
- [x] `[BÁO CÁO]` Cập nhật `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` — thêm phần SQL thực hành

---

## Sprint 1.5 (07/05 → 08/05): Migration & Locking Bridge

Mục tiêu của sprint này là đủ hiểu mindset schema change an toàn trước khi đem Flyway vào Spring Boot. Không học quá sâu PostgreSQL internals ở đây.

### Thứ Năm 07/05 — Migration safety tối thiểu

- [ ] `[LÝ THUYẾT]` Đọc `docs/03-backend-database-mo-rong/migration-lock-rollback.md` — output: 5-7 bullet về safe vs risky migration trong `docs/99-tong-ket/nhung-gi-da-nam-duoc.md`
- [ ] `[THỰC HÀNH]` Tự tạo khi bắt đầu task: `lab-code/sql-playground/06-migration-lock-observation.sql` — thử một vài `ALTER TABLE` nhỏ trên `master_data`; verify bằng `make db-up`, `make sql-reset`, `make sql-all`, rồi chạy script thủ công bằng `psql`
- [ ] `[BÁO CÁO]` Ghi summary ngắn: lock là gì, vì sao migration có thể ảnh hưởng nhiều tenant, rollback cần chuẩn bị gì; không quá 1 trang

### Milestone #2 — Migration & Locking / PostgreSQL Safety Mindset

> **Output artifact:** summary ngắn trong `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` và script quan sát lock nếu đã tạo.
> **Files để show:** `docs/03-backend-database-mo-rong/migration-lock-rollback.md`, `docs/99-tong-ket/nhung-gi-da-nam-duoc.md`, `lab-code/sql-playground/06-migration-lock-observation.sql` nếu có.
> **Demo/report expectation:** giải thích được vì sao schema change trong shared-table SaaS cần backward-compatible và tránh lock dài.
> **Done criteria:** có ghi chú ngắn + ít nhất một quan sát thực hành về `ALTER TABLE`/lock trên PostgreSQL local.
> **Out of scope:** partitioning sâu, VACUUM tuning, zero-downtime migration production thật.

- [ ] `[MILESTONE]` Chốt Milestone #2 — có summary và output thực hành nhỏ về migration/locking

---

## Sprint 2 (09/05 → 10/05): Spring Boot Bootstrap + Flyway + Tenant Context

Mục tiêu là biến `lab-code/tenant-demo/` từ skeleton thành app Spring Boot khởi động được, migrate schema được và nhận diện tenant từ request.

### Thứ Bảy 09/05 — Bootstrap app và Flyway baseline

- [ ] `[THỰC HÀNH]` Tự hoàn thiện `lab-code/tenant-demo/pom.xml` bằng Spring Initializr hoặc viết tay — dependencies: Web, JPA, PostgreSQL, Flyway, Test
- [ ] `[THỰC HÀNH]` Tự hoàn thiện `lab-code/tenant-demo/src/main/resources/application.yml` — kết nối DB local, Flyway enabled, JPA `ddl-auto` không tự tạo schema
- [ ] `[THỰC HÀNH]` Tự code `TenantDemoApplication.java` và Flyway `V1`, `V2`, `V3` dựa trên SQL baseline; verify: `cd lab-code && make db-up && make app-run`

### Chủ Nhật 10/05 — TenantContext và TenantFilter

- [ ] `[THỰC HÀNH]` Tự code `TenantContext.java` và `TenantFilter.java` — đọc `X-Tenant-Id`, set/clear context đúng vòng đời request
- [ ] `[THỰC HÀNH]` Verify bằng curl/log: request có `X-Tenant-Id` thì app log đúng tenant; request thiếu header được xử lý rõ ràng
- [ ] `[BÁO CÁO]` Cập nhật `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` — thêm mục ngắn về Spring Boot bootstrap, Flyway và TenantFilter

### Milestone #3 — Spring Boot Foundation

> **Output artifact:** app khởi động được, Flyway tạo schema, TenantFilter nhận `X-Tenant-Id`.
> **Files để show:** `lab-code/tenant-demo/pom.xml`, `application.yml`, `TenantDemoApplication.java`, `TenantContext.java`, `TenantFilter.java`, `db/migration/V1-V3`.
> **Demo/report expectation:** chạy `make app-run`, gửi một curl có `X-Tenant-Id`, giải thích request đi qua filter thế nào.
> **Done criteria:** app start pass; Flyway migrate pass; log hoặc response chứng minh tenant context hoạt động.
> **Out of scope:** CRUD đầy đủ, auth/JWT thật, RBAC, production security.

- [ ] `[MILESTONE]` Demo: Spring Boot start → Flyway migrate → TenantFilter hoạt động

---

## Sprint 3 (11/05 → 14/05): Tenant-aware MasterData API

Mục tiêu là có endpoint thật để chứng minh tenant-scoped data access. Ưu tiên rõ, chạy được, dễ giải thích; chưa cần generic framework quá phức tạp.

### Thứ Hai 11/05 — Entity và repository tenant-aware

- [ ] `[LÝ THUYẾT]` Đọc lại `docs/02-multi-tenant/tong-quan-multi-tenant.md` và phần data leakage trong `docs/03-backend-database-mo-rong/index-va-query-tenant-aware.md` — output: 3 rule backend query phải nhớ
- [ ] `[THỰC HÀNH]` Tự code `TenantAwareEntity.java` và `MasterData.java` — mapping đúng `tenant_id`, unique constraint, `is_active`
- [ ] `[THỰC HÀNH]` Tự code `MasterDataRepository.java` với method explicit có `tenantId`; không overdo base repository nếu chưa thật sự cần

### Thứ Ba 12/05 — Service, controller và curl verification

- [ ] `[THỰC HÀNH]` Tự code `MasterDataService.java` và `MasterDataController.java` — endpoint đọc danh sách và tìm theo `code` trong tenant hiện tại
- [ ] `[THỰC HÀNH]` Verify bằng curl: `X-Tenant-Id: 1` chỉ thấy data tenant 1; `X-Tenant-Id: 2` chỉ thấy data tenant 2
- [ ] `[REVIEW]` Sau khi tự chạy được, nhờ Agent review code trong `lab-code/tenant-demo/` — chỉ sửa lỗi rõ ràng, không refactor lớn
- [ ] `[BÁO CÁO]` Ghi curl output/pattern vào `docs/99-tong-ket/nhung-gi-da-nam-duoc.md`

### Milestone #4 — Tenant-aware API Demo

> **Output artifact:** REST endpoint tenant-scoped cho `master_data`.
> **Files để show:** entity, repository, service, controller, curl commands/output.
> **Demo/report expectation:** mở app, gọi curl với 2 tenant khác nhau, giải thích vì sao dữ liệu không lẫn.
> **Done criteria:** app chạy; endpoint trả data scoped theo `X-Tenant-Id`; query/service không tin tenant từ request body.
> **Out of scope:** full ERP CRUD, auth/JWT thật, UI, pagination nâng cao.

- [ ] `[MILESTONE]` Demo: Tenant A GET chỉ thấy data A; Tenant B GET chỉ thấy data B

---

## Sprint 4 (15/05 → 17/05): Data Leakage Tests + Code Review

Mục tiêu là biến bài học SQL leakage thành test backend có thể chạy lại.

### Thứ Sáu 15/05 — Integration test chống leakage

- [ ] `[THỰC HÀNH]` Tự code `DataLeakageTest.java` — tối thiểu 3 case: tenant A không thấy tenant B, missing/invalid tenant bị chặn, query by code vẫn scoped theo tenant
- [ ] `[THỰC HÀNH]` Chạy `cd lab-code && make app-test` — test pass hoặc ghi lỗi cụ thể nếu fail

### Thứ Bảy 16/05 → Chủ Nhật 17/05 — Review nhỏ và chốt chất lượng

- [ ] `[REVIEW]` Nhờ Agent review focused: tenant context lifecycle, repository/service có quên `tenant_id` không, migration có khớp SQL baseline không
- [ ] `[BÁO CÁO]` Cập nhật `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` — thêm phần test chống data leakage và giới hạn hiện tại

### Milestone #5 — Testable Tenant Isolation

> **Output artifact:** integration test hoặc test gần tương đương chứng minh tenant isolation.
> **Files để show:** `DataLeakageTest.java`, service/repository/controller liên quan, test output.
> **Demo/report expectation:** chạy `make app-test`, giải thích 3 test case và lỗi nào sẽ bắt được.
> **Done criteria:** test pass hoặc nếu còn blocker thì có lỗi cụ thể, nguyên nhân, hướng sửa.
> **Out of scope:** testcontainers nâng cao nếu làm chậm tiến độ, performance benchmark API.

- [ ] `[MILESTONE]` Chốt test isolation — có test output và summary ngắn

---

## Sprint 5 (18/05 → 20/05): Báo cáo và Talk-through Gắn Với Demo

Mục tiêu là cập nhật tài liệu đủ để báo cáo mentor/leader mà không đợi tới sát deadline.

### Thứ Hai 18/05 — LaTeX report update

- [ ] `[BÁO CÁO]` Cập nhật `reports/latex/bao-cao-saas-multi-tenant.tex` — thêm kết quả SQL playground, Spring Boot PoC, test leakage; không viết thành sách giáo khoa
- [ ] `[BÁO CÁO]` Compile thử LaTeX nếu môi trường có `xelatex`; nếu không có, ghi rõ chưa verify PDF

### Thứ Ba 19/05 → Thứ Tư 20/05 — Presentation và demo script

- [ ] `[BÁO CÁO]` Cập nhật `presentation-notes/thuyet-trinh-saas-multi-tenant.md` — thêm flow demo: SQL → Flyway → TenantFilter → API → test
- [ ] `[THỰC HÀNH]` Tạo khi đến task: checklist demo ngắn trong `presentation-notes/` hoặc cuối file thuyết trình — gồm command, curl, điểm nói chính
- [ ] `[REVIEW]` Đọc thử report/talk-through một lượt; nhờ Agent review tính mạch lạc nếu cần

### Milestone #6 — Mentor-facing Report Package

> **Output artifact:** report LaTeX cập nhật + presentation notes có demo flow.
> **Files để show:** `reports/latex/bao-cao-saas-multi-tenant.tex`, `presentation-notes/thuyet-trinh-saas-multi-tenant.md`, `docs/99-tong-ket/nhung-gi-da-nam-duoc.md`.
> **Demo/report expectation:** có thể trình bày 10-15 phút về theory + runnable output.
> **Done criteria:** report không còn chỉ nói theory; presentation có đường dẫn tới demo thật.
> **Out of scope:** thiết kế slide đẹp, PDF publish nếu chưa cần.

- [ ] `[MILESTONE]` Chốt package báo cáo — đủ nội dung để báo cáo mentor trước deadline

---

## Sprint 6 (21/05 → 23/05): Final Dry-run và Buffer Kỹ Thuật

Mục tiêu là đảm bảo demo chạy từ đầu đến cuối trên máy local, không chỉ chạy từng mảnh.

### Thứ Năm 21/05 — Full demo rehearsal

- [ ] `[THỰC HÀNH]` Chạy full flow từ đầu: `make db-up`, reset DB nếu cần, `make app-run`, curl 2 tenant, `make app-test`
- [ ] `[THỰC HÀNH]` Ghi lại command thành demo checklist cuối cùng; đánh dấu command nào bắt buộc, command nào optional

### Thứ Sáu 22/05 → Thứ Bảy 23/05 — Final review

- [ ] `[BÁO CÁO]` Cập nhật `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` — bản gần cuối: đã làm được gì, giới hạn, hướng tiếp
- [ ] `[REVIEW]` Nhờ Agent review final repo ở mức rủi ro demo: lệnh nào fail, docs nào stale, roadmap nào sai

### Milestone #7 — Final Dry-run

> **Output artifact:** demo checklist đã chạy qua + test/curl output.
> **Files để show:** `lab-code/Makefile`, `lab-code/tenant-demo/`, presentation notes, report.
> **Demo/report expectation:** chạy thử như ngày bảo vệ; biết fallback nếu một phần fail.
> **Done criteria:** demo chính chạy; test pass hoặc có caveat rõ; tài liệu không hứa quá mức.
> **Out of scope:** thêm feature mới sau dry-run nếu không cần thiết.

- [ ] `[MILESTONE]` Dry-run: trình bày → chạy demo → chạy test → ghi lại vấn đề còn lại

---

## Sprint 7 (24/05 → 25/05): Đóng Gói và Bảo Vệ Phase 1

### Chủ Nhật 24/05 — Polish cuối

- [ ] `[BÁO CÁO]` Polish report/presentation theo feedback dry-run; chỉ sửa nội dung sai hoặc thiếu, không mở scope mới
- [ ] `[THỰC HÀNH]` Final rehearsal: mở tài liệu, chạy command demo chính, chuẩn bị Q&A

### Thứ Hai 25/05 — Milestone #8: Bảo vệ Phase 1

> **Output artifact:** trình bày + demo + Q&A.
> **Files để show:** report, presentation notes, SQL playground, Spring Boot PoC, roadmap.
> **Done criteria:** giải thích được trade-off shared-table multi-tenant, chứng minh demo chạy, nói rõ giới hạn hiện tại.
> **Out of scope:** production ERP hoàn chỉnh, auth/RBAC đầy đủ, distributed system nâng cao.

- [ ] `[MILESTONE]` Trình bày Phase 1 + demo runnable + Q&A với leader/mentor
- [ ] `[BÁO CÁO]` Ghi feedback leader vào `local/` hoặc ghi chú local-only, không commit nội dung riêng tư

---

## Tổng quan Milestones

| # | Ngày mục tiêu | Output báo cáo |
|:-:|:-------------:|---------------|
| 1 | 01/05 | SQL playground results + tenant-aware data isolation proof |
| 2 | 08/05 | Migration & Locking safety summary + small PostgreSQL observation |
| 3 | 10/05 | Spring Boot starts + Flyway baseline + TenantFilter |
| 4 | 14/05 | Tenant-aware MasterData API demo bằng curl |
| 5 | 17/05 | Data leakage tests + focused code review |
| 6 | 20/05 | Mentor-facing report package + demo talk-through |
| 7 | 23/05 | Final dry-run: app, curl, tests, presentation |
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
| Ghi chú private | `local/` — chỉ local-only, không dùng làm source of truth công khai |

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
3. **Không overdo.** Phase 1 cần runnable demo nhỏ, không cần production ERP.
4. **Report dần.** Mỗi milestone cập nhật một đoạn ngắn, không dồn tới cuối.
5. **Không commit local-only notes.** Nháp, prompt, feedback riêng tư ở `local/`.
6. **Nếu stuck hơn 2 tiếng:** ghi blocker, tạo fallback nhỏ hơn, tiếp tục tiến độ.
