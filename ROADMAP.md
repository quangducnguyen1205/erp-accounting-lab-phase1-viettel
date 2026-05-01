# 🗺️ ROADMAP — Phase 1: ERP/Kế toán SaaS Multi-tenant

> **Bắt đầu:** Thứ Ba 28/04/2026 · **Deadline:** Thứ Hai 25/05/2026
> **Chu kỳ báo cáo:** 8 milestones, mỗi 3-4 ngày
> **Phương châm:** Lý thuyết + Thực hành đan xen → Báo cáo luôn có output

---

## 📊 Quick Stats

| Chỉ số | Giá trị |
|--------|:-------:|
| **Tiến độ** | 6% |
| **Tổng task** | 64 |
| **Đã hoàn thành** | 4 / 64 |
| **Sprint hiện tại** | Sprint 1 |
| **Milestone tiếp theo** | #1 — Thứ Sáu 01/05 |

---

## 🏷️ Chú thích

| Tag | Mở file nào |
|-----|-------------|
| `[LÝ THUYẾT]` | `docs/`, `local/` |
| `[THỰC HÀNH]` | `lab-code/` |
| `[BÁO CÁO]` | `reports/`, `presentation-notes/` |
| `[MILESTONE]` | Cột mốc — phải có output cụ thể để báo cáo |

---

## Sprint 1 (T3 28/04 → T2 04/05): Nền tảng & SQL Thực chiến

### Thứ Ba 28/04 — Setup & Multi-tenant core

- [x] `[LÝ THUYẾT]` Review `docs/02-multi-tenant/cac-mo-hinh-tenant-isolation.md` — nắm chắc 3 mô hình
- [x] `[THỰC HÀNH]` Chạy `make db-up`, verify PostgreSQL container
- [x] `[THỰC HÀNH]` Chạy `make sql-1` — verify bảng tenants + master_data
- [x] `[THỰC HÀNH]` Chạy `make sql-2` — verify 4 kịch bản insert

### Thứ Tư 29/04 — Index & EXPLAIN

- [ ] `[LÝ THUYẾT]` Đọc `docs/03-backend-database-mo-rong/index-va-query-tenant-aware.md`
- [ ] `[THỰC HÀNH]` Tự code `03-query-with-explain.sql` — chạy EXPLAIN ANALYZE
- [ ] `[THỰC HÀNH]` Tự code `04-index-comparison.sql` — so sánh Seq Scan vs Index Scan

### Thứ Năm 30/04 _(hôm nay)_ — Data Leakage & Noisy Neighbor

- [ ] `[LÝ THUYẾT]` Đọc `docs/03-backend-database-mo-rong/noisy-neighbor-shared-table.md`
- [ ] `[LÝ THUYẾT]` Đọc `local/.../23-kien-thuc-mo-rong/vi-du-ve-noisy-neighbor.md`
- [ ] `[THỰC HÀNH]` Tự code `05-data-leakage-test.sql` — chứng minh query thiếu tenant_id lộ data

### 🚩 Thứ Sáu 01/05 — MILESTONE #1

> **Output báo cáo:** Multi-tenant 3 mô hình + Kết quả SQL (EXPLAIN output, data leakage proof).

- [ ] `[MILESTONE]` Tổng hợp output 5 file SQL playground → ghi chú kết quả
- [ ] `[BÁO CÁO]` Cập nhật `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` — thêm phần SQL thực hành

### Thứ Bảy 02/05 — Migration & Locking

- [ ] `[LÝ THUYẾT]` Đọc `docs/03-backend-database-mo-rong/migration-lock-rollback.md`
- [ ] `[LÝ THUYẾT]` Đọc `local/.../23-kien-thuc-mo-rong/postgresql-internals-phan2-partition-migration-lock.md`
- [ ] `[THỰC HÀNH]` Thử ALTER TABLE trên master_data trong psql, quan sát lock

### Chủ Nhật 03/05 — PostgreSQL Internals tổng hợp

- [ ] `[LÝ THUYẾT]` Đọc `local/.../23-kien-thuc-mo-rong/postgresql-internals-phan1-index-va-query.md`
- [ ] `[LÝ THUYẾT]` Đọc `local/.../07-postgresql-data-modeling/01-postgresql-va-data-modeling.md`
- [ ] `[LÝ THUYẾT]` Liệt kê ALTER TABLE an toàn vs nguy hiểm → ghi vào ghi chú riêng

### 🚩 Thứ Hai 04/05 — MILESTONE #2

> **Output báo cáo:** Index strategy + Migration safety + PostgreSQL internals tổng hợp.

- [ ] `[MILESTONE]` Chốt Sprint 1 — SQL playground xong, lý thuyết DB vững
- [ ] `[BÁO CÁO]` Review `docs/01-saas/tong-quan-saas.md` — xác nhận SaaS lý thuyết vững

---

## Sprint 2 (T3 05/05 → T2 11/05): Backend Core — PoC Spring Boot

### Thứ Ba 05/05 — Project Bootstrap

- [ ] `[THỰC HÀNH]` Generate project từ https://start.spring.io (Web, JPA, PostgreSQL, Flyway)
- [ ] `[THỰC HÀNH]` Tự code `pom.xml` + `application.yml` — kết nối PostgreSQL
- [ ] `[THỰC HÀNH]` Tự code `TenantDemoApplication.java` — verify Spring Boot khởi động

### Thứ Tư 06/05 — Tenant Context & Filter

- [ ] `[LÝ THUYẾT]` Đọc lại sơ đồ sequence `docs/02-multi-tenant/tong-quan-multi-tenant.md`
- [ ] `[THỰC HÀNH]` Tự code `TenantContext.java` — ThreadLocal
- [ ] `[THỰC HÀNH]` Tự code `TenantFilter.java` — OncePerRequestFilter
- [ ] `[THỰC HÀNH]` Test curl: header `X-Tenant-Id` → verify log đúng tenant

### 🚩 Thứ Năm 07/05 — MILESTONE #3

> **Output báo cáo:** Spring Boot chạy + TenantFilter log tenant_id + Flyway tạo bảng.

- [ ] `[THỰC HÀNH]` Tự code `TenantAwareEntity.java` — @MappedSuperclass, @PrePersist
- [ ] `[THỰC HÀNH]` Tự code `MasterData.java` — entity + uniqueConstraints
- [ ] `[THỰC HÀNH]` Tự code Flyway `V1`, `V2`, `V3` — verify bảng tạo xong
- [ ] `[MILESTONE]` Demo: Spring Boot start → Flyway migrate → TenantFilter hoạt động

### Thứ Sáu 08/05 — Repository auto-filter

- [ ] `[LÝ THUYẾT]` Đọc `local/.../05-auth-authz-rbac/01-auth-authz-rbac.md` — preview Auth
- [ ] `[THỰC HÀNH]` Tự code `TenantAwareRepository.java` — base repo auto WHERE tenant_id
- [ ] `[THỰC HÀNH]` Tự code `MasterDataRepository.java` — kế thừa base repo
- [ ] `[THỰC HÀNH]` Bật `show-sql=true`, verify query có WHERE tenant_id

### Thứ Bảy 09/05 + Chủ Nhật 10/05 — Service + Controller

- [ ] `[THỰC HÀNH]` Tự code `MasterDataService.java` — CRUD + soft delete
- [ ] `[THỰC HÀNH]` Tự code `MasterDataController.java` — REST endpoints
- [ ] `[THỰC HÀNH]` Test curl: CRUD với 2 tenant, verify data isolation

### 🚩 Thứ Hai 11/05 — MILESTONE #4

> **Output báo cáo:** CRUD hoàn chỉnh, 2 tenant tách biệt qua API.

- [ ] `[MILESTONE]` Demo: Tenant A POST → GET chỉ thấy data A. Tenant B không thấy data A.
- [ ] `[THỰC HÀNH]` Commit code sạch theo Conventional Commits

---

## Sprint 3 (T3 12/05 → T2 18/05): Testing & Advanced Topics

### Thứ Ba 12/05 — Integration Test

- [ ] `[THỰC HÀNH]` Tự code `DataLeakageTest.java` — 3 test cases (xem TODO trong file)
- [ ] `[THỰC HÀNH]` Chạy `make app-test` — tất cả test pass

### Thứ Tư 13/05 — Partitioning & VACUUM

- [ ] `[LÝ THUYẾT]` Đọc `docs/03-backend-database-mo-rong/partitioning-vacuum-read-replica.md`
- [ ] `[LÝ THUYẾT]` Tổng hợp: khi nào partition? VACUUM? Read replica?

### 🚩 Thứ Năm 14/05 — MILESTONE #5

> **Output báo cáo:** Integration test pass + Partitioning/VACUUM theory.

- [ ] `[LÝ THUYẾT]` Đọc `local/.../04-phan-tach-service/01-tu-duy-phan-tach.md`
- [ ] `[LÝ THUYẾT]` Đọc `local/.../06-api-gateway-kong/01-api-gateway-va-kong.md`
- [ ] `[MILESTONE]` Screenshot test pass + ghi chú summary mỗi topic

### Thứ Sáu 15/05 — Kiến thức bổ trợ

- [ ] `[LÝ THUYẾT]` Đọc `local/.../08-redis-cache/01-redis-cache.md`
- [ ] `[LÝ THUYẾT]` Đọc `local/.../09-kafka-async/01-kafka-va-async.md`
- [ ] `[LÝ THUYẾT]` Đọc `local/.../11-observability/01-observability.md`

### Thứ Bảy 16/05 + Chủ Nhật 17/05 — Code Review & Refactor

- [ ] `[THỰC HÀNH]` Nhờ Agent review toàn bộ `lab-code/tenant-demo/`
- [ ] `[THỰC HÀNH]` Fix lỗi, refactor theo suggestion, đảm bảo clean code
- [ ] `[LÝ THUYẾT]` Đọc `local/.../13-design-patterns/01-design-patterns.md`

### 🚩 Thứ Hai 18/05 — MILESTONE #6

> **Output báo cáo:** Code clean + Knowledge summary (Redis, Kafka, Observability, Patterns).

- [ ] `[MILESTONE]` Commit code đã refactor. Liệt kê "hướng học tiếp" cho Phase 2.
- [ ] `[BÁO CÁO]` Cập nhật `docs/99-tong-ket/nhung-gi-da-nam-duoc.md`
- [ ] `[BÁO CÁO]` Draft phần "Giới hạn và hướng tiếp" trong LaTeX report

---

## Sprint 4 (T3 19/05 → T2 25/05): Đóng gói & Bảo vệ Phase 1

### Thứ Ba 19/05 — LaTeX Report

- [ ] `[BÁO CÁO]` Review `reports/latex/bao-cao-saas-multi-tenant.tex` — cập nhật 6 sections
- [ ] `[BÁO CÁO]` Bổ sung kết quả thực hành (EXPLAIN output, test screenshot)
- [ ] `[BÁO CÁO]` Compile LaTeX → verify PDF sạch

### Thứ Tư 20/05 — Presentation Polish

- [ ] `[BÁO CÁO]` Review `presentation-notes/thuyet-trinh-saas-multi-tenant.md`
- [ ] `[BÁO CÁO]` Đọc to speaker script, bấm giờ (15-20 phút)
- [ ] `[BÁO CÁO]` Bổ sung FAQ + chuẩn bị kịch bản demo

### 🚩 Thứ Năm 21/05 — MILESTONE #7

> **Output báo cáo:** LaTeX PDF xong + Presentation sẵn sàng + Demo chạy trơn tru.

- [ ] `[THỰC HÀNH]` `make db-up` → `make app-run` → CRUD demo 2 tenant
- [ ] `[THỰC HÀNH]` `make app-test` — tất cả pass
- [ ] `[MILESTONE]` Dry-run: mở PDF → trình bày → chạy demo → thử Q&A

### Thứ Sáu 22/05 — Buffer & Fix

- [ ] `[BÁO CÁO]` Sửa lỗi cuối cùng, polish tài liệu
- [ ] `[THỰC HÀNH]` Final rehearsal: demo từ đầu đến cuối

### Thứ Bảy 23/05 + Chủ Nhật 24/05 — Final Commit

- [ ] `[BÁO CÁO]` Cập nhật `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` — phiên bản cuối
- [ ] `[BÁO CÁO]` Cập nhật ROADMAP.md — Quick Stats = 100%
- [ ] `[BÁO CÁO]` Final commit: chốt version Phase 1

### 🚩 Thứ Hai 25/05 — MILESTONE #8: 🎯 BẢO VỆ PHASE 1

> **BẢO VỆ PHASE 1 VỚI LEADER**

- [ ] `[MILESTONE]` Trình bày + Demo + Q&A
- [ ] `[BÁO CÁO]` Ghi chú feedback Leader vào `local/`

---

## 📅 Tổng quan 8 Milestones

| # | Ngày | Thứ | Output báo cáo |
|:-:|:----:|:---:|---------------|
| 1 | 01/05 | T6 | SQL playground results + Multi-tenant isolation |
| 2 | 04/05 | T2 | Index/EXPLAIN + Migration safety + PG internals |
| 3 | 07/05 | T5 | Spring Boot + TenantFilter + Flyway working |
| 4 | 11/05 | T2 | CRUD hoàn chỉnh, 2 tenant tách biệt qua API |
| 5 | 14/05 | T5 | Integration test pass + Advanced DB theory |
| 6 | 18/05 | T2 | Code clean + Knowledge summary 5 topics |
| 7 | 21/05 | T5 | LaTeX PDF + Demo + Presentation ready |
| 8 | 25/05 | T2 | 🎯 **BẢO VỆ PHASE 1** |

---

## 📁 File Reference

| Cần làm gì | File |
|------------|------|
| SaaS lý thuyết | `docs/01-saas/tong-quan-saas.md` |
| Multi-tenant | `docs/02-multi-tenant/*.md` |
| PostgreSQL sâu | `docs/03-backend-database-mo-rong/*.md` |
| Nháp cũ | `local/pre-rebuild-backup-20260424-165714/docs/...` |
| SQL thực hành | `lab-code/sql-playground/*.sql` |
| Java PoC | `lab-code/tenant-demo/src/...` |
| Make commands | `lab-code/Makefile` → `make help` |
| Báo cáo LaTeX | `reports/latex/bao-cao-saas-multi-tenant.tex` |
| Thuyết trình | `presentation-notes/thuyet-trinh-saas-multi-tenant.md` |

---

## ⚠️ Nguyên tắc

1. **Tự code trước.** Nhờ Agent review SAU KHI đã tự viết.
2. **Commit mỗi ngày.** Message theo Conventional Commits.
3. **Stuck > 2 tiếng** → ghi chú, skip, quay lại sau.
4. **Cập nhật Quick Stats** cuối mỗi ngày.
5. **Milestone = phải có output cụ thể** (SQL output, curl result, test pass, PDF).
