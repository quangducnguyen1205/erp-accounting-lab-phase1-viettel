# 🗺️ ROADMAP — Phase 1: ERP/Kế toán SaaS Multi-tenant

> **Deadline:** 25/05/2026 · **Bắt đầu:** 28/04/2026 (Thứ Tư)
> **Chu kỳ báo cáo:** Mỗi 3-4 ngày có 1 milestone
> **Phương châm:** Lý thuyết + Thực hành đan xen → Báo cáo không bao giờ rỗng

---

## 📊 Quick Stats

| Chỉ số |      Giá trị       |
|--------|:------------------:|
| **Tiến độ** |       6.25%        |
| **Tổng task** |         64         |
| **Đã hoàn thành** |       4 / 64       |
| **Sprint hiện tại** |      Sprint 1      |
| **Milestone tiếp theo** | #1 — Thứ Bảy 01/05 |

---

## 🏷️ Chú thích

| Tag | Mở file nào |
|-----|-------------|
| `[LÝ THUYẾT]` | `docs/`, `local/` |
| `[THỰC HÀNH]` | `lab-code/` |
| `[BÁO CÁO]` | `reports/`, `presentation-notes/` |
| `[MILESTONE]` | Cột mốc tổng hợp, sẵn sàng báo cáo |

---

## Sprint 1 (T4 28/04 → T3 04/05): Nền tảng & SQL Thực chiến

### Thứ Tư 28/04 — Setup & Multi-tenant core

- [x] `[LÝ THUYẾT]` Review `docs/02-multi-tenant/cac-mo-hinh-tenant-isolation.md` — nắm chắc 3 mô hình
- [x] `[THỰC HÀNH]` Chạy `make db-up`, verify PostgreSQL container
- [x] `[THỰC HÀNH]` Chạy `make sql-1` — verify bảng tenants + master_data
- [x] `[THỰC HÀNH]` Chạy `make sql-2` — verify 4 kịch bản insert

### Thứ Năm 29/04 — Index & EXPLAIN

- [ ] `[LÝ THUYẾT]` Đọc `docs/03-backend-database-mo-rong/index-va-query-tenant-aware.md`
- [ ] `[THỰC HÀNH]` Tự code `03-query-with-explain.sql` — chạy EXPLAIN ANALYZE
- [ ] `[THỰC HÀNH]` Tự code `04-index-comparison.sql` — so sánh Seq Scan vs Index Scan

### Thứ Sáu 30/04 — Data Leakage + Noisy Neighbor

- [ ] `[LÝ THUYẾT]` Đọc `docs/03-backend-database-mo-rong/noisy-neighbor-shared-table.md`
- [ ] `[THỰC HÀNH]` Tự code `05-data-leakage-test.sql` — chứng minh query thiếu tenant_id lộ data
- [ ] `[LÝ THUYẾT]` Đọc `local/.../23-kien-thuc-mo-rong/vi-du-ve-noisy-neighbor.md`

### 🚩 Thứ Bảy 01/05 — MILESTONE BÁO CÁO #1

> **Nội dung báo cáo:** Multi-tenant isolation 3 mô hình + Kết quả SQL (bảng, insert, EXPLAIN, data leakage).

- [ ] `[MILESTONE]` Tổng hợp kết quả 5 file SQL playground → ghi chú output
- [ ] `[BÁO CÁO]` Cập nhật `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` — thêm phần SQL thực hành

### Chủ Nhật 02/05 — Migration & Locking

- [ ] `[LÝ THUYẾT]` Đọc `docs/03-backend-database-mo-rong/migration-lock-rollback.md`
- [ ] `[LÝ THUYẾT]` Đọc `local/.../23-kien-thuc-mo-rong/postgresql-internals-phan2-partition-migration-lock.md`
- [ ] `[THỰC HÀNH]` Thử ALTER TABLE trên master_data trong psql, quan sát lock

### Thứ Hai 03/05 — PostgreSQL Internals tổng hợp

- [ ] `[LÝ THUYẾT]` Đọc `local/.../23-kien-thuc-mo-rong/postgresql-internals-phan1-index-va-query.md`
- [ ] `[LÝ THUYẾT]` Đọc `local/.../07-postgresql-data-modeling/01-postgresql-va-data-modeling.md`
- [ ] `[LÝ THUYẾT]` Liệt kê: ALTER TABLE an toàn vs nguy hiểm (ghi vào ghi chú riêng)

### 🚩 Thứ Ba 04/05 — MILESTONE BÁO CÁO #2

> **Nội dung báo cáo:** Index strategy (EXPLAIN output) + Migration safety + PostgreSQL internals tổng hợp.

- [ ] `[MILESTONE]` Chốt Sprint 1: toàn bộ SQL playground xong, lý thuyết DB nắm vững
- [ ] `[BÁO CÁO]` Preview `docs/01-saas/tong-quan-saas.md` — xác nhận SaaS lý thuyết vững

---

## Sprint 2 (T4 05/05 → T3 11/05): Backend Core — PoC Spring Boot

### Thứ Tư 05/05 — Project Bootstrap

- [ ] `[THỰC HÀNH]` Generate project từ https://start.spring.io (Web, JPA, PostgreSQL, Flyway)
- [ ] `[THỰC HÀNH]` Tự code `pom.xml` + `application.yml` — kết nối PostgreSQL
- [ ] `[THỰC HÀNH]` Tự code `TenantDemoApplication.java` — verify Spring Boot khởi động

### Thứ Năm 06/05 — Tenant Context & Filter

- [ ] `[LÝ THUYẾT]` Đọc lại sơ đồ sequence `docs/02-multi-tenant/tong-quan-multi-tenant.md`
- [ ] `[THỰC HÀNH]` Tự code `TenantContext.java` — ThreadLocal
- [ ] `[THỰC HÀNH]` Tự code `TenantFilter.java` — OncePerRequestFilter
- [ ] `[THỰC HÀNH]` Test curl: header `X-Tenant-Id` → verify log in đúng tenant

### Thứ Sáu 07/05 — Entity & Flyway Migration

- [ ] `[THỰC HÀNH]` Tự code `TenantAwareEntity.java` — @MappedSuperclass, @PrePersist
- [ ] `[THỰC HÀNH]` Tự code `MasterData.java` — entity + uniqueConstraints
- [ ] `[THỰC HÀNH]` Tự code Flyway `V1`, `V2`, `V3` — chạy Spring Boot, verify bảng tạo xong

### 🚩 Thứ Bảy 08/05 — MILESTONE BÁO CÁO #3

> **Nội dung báo cáo:** Tenant filter flow hoạt động + Entity auto-set tenant_id + Flyway migration chạy thành công.

- [ ] `[MILESTONE]` Demo: Spring Boot start → Flyway tạo bảng → TenantFilter log tenant_id
- [ ] `[LÝ THUYẾT]` Đọc `local/.../05-auth-authz-rbac/01-auth-authz-rbac.md` — preview Auth

### Chủ Nhật 09/05 — Repository auto-filter

- [ ] `[THỰC HÀNH]` Tự code `TenantAwareRepository.java` — base repo auto WHERE tenant_id
- [ ] `[THỰC HÀNH]` Tự code `MasterDataRepository.java` — kế thừa base
- [ ] `[THỰC HÀNH]` Bật `show-sql=true`, verify query có WHERE tenant_id

### Thứ Hai 10/05 — Service + Controller + CRUD

- [ ] `[THỰC HÀNH]` Tự code `MasterDataService.java` — CRUD + soft delete
- [ ] `[THỰC HÀNH]` Tự code `MasterDataController.java` — REST endpoints
- [ ] `[THỰC HÀNH]` Test curl: CRUD với 2 tenant, verify data isolation

### 🚩 Thứ Ba 11/05 — MILESTONE BÁO CÁO #4

> **Nội dung báo cáo:** CRUD hoạt động hoàn chỉnh, 2 tenant tách biệt qua API.

- [ ] `[MILESTONE]` Demo: Tenant A POST → GET chỉ thấy data A. Tenant B không thấy data A.
- [ ] `[THỰC HÀNH]` Commit code sạch theo Conventional Commits

---

## Sprint 3 (T4 12/05 → T3 18/05): Testing & Advanced Topics

### Thứ Tư 12/05 — Integration Test

- [ ] `[THỰC HÀNH]` Tự code `DataLeakageTest.java` — 3 test cases (xem TODO trong file)
- [ ] `[THỰC HÀNH]` Chạy `make app-test` — tất cả test pass

### Thứ Năm 13/05 — Partitioning & VACUUM

- [ ] `[LÝ THUYẾT]` Đọc `docs/03-backend-database-mo-rong/partitioning-vacuum-read-replica.md`
- [ ] `[LÝ THUYẾT]` Tổng hợp: khi nào partition? VACUUM ảnh hưởng gì? Read replica dùng khi nào?

### Thứ Sáu 14/05 — Kiến thức bổ trợ

- [ ] `[LÝ THUYẾT]` Đọc `local/.../04-phan-tach-service/01-tu-duy-phan-tach.md`
- [ ] `[LÝ THUYẾT]` Đọc `local/.../06-api-gateway-kong/01-api-gateway-va-kong.md`
- [ ] `[LÝ THUYẾT]` Đọc `local/.../08-redis-cache/01-redis-cache.md`

### 🚩 Thứ Bảy 15/05 — MILESTONE BÁO CÁO #5

> **Nội dung báo cáo:** Integration test pass + Partitioning/VACUUM theory + Kiến thức bổ trợ (Service, Kong, Redis).

- [ ] `[MILESTONE]` Screenshot test pass + ghi chú 1-dòng summary mỗi topic bổ trợ
- [ ] `[BÁO CÁO]` Cập nhật `docs/99-tong-ket/nhung-gi-da-nam-duoc.md`

### Chủ Nhật 16/05 — Code Review & Refactor

- [ ] `[THỰC HÀNH]` Nhờ Agent review toàn bộ `lab-code/tenant-demo/`
- [ ] `[THỰC HÀNH]` Fix lỗi, refactor theo suggestion, đảm bảo clean code

### Thứ Hai 17/05 — Extra topics từ `local/`

- [ ] `[LÝ THUYẾT]` Đọc `local/.../09-kafka-async/01-kafka-va-async.md`
- [ ] `[LÝ THUYẾT]` Đọc `local/.../11-observability/01-observability.md`
- [ ] `[LÝ THUYẾT]` Đọc `local/.../13-design-patterns/01-design-patterns.md`

### 🚩 Thứ Ba 18/05 — MILESTONE BÁO CÁO #6

> **Nội dung báo cáo:** Code clean + Knowledge summary (Kafka, Observability, Design Patterns).

- [ ] `[MILESTONE]` Commit code đã refactor. Liệt kê "hướng học tiếp" cho Phase 2.
- [ ] `[BÁO CÁO]` Draft phần "Giới hạn và hướng tiếp" trong LaTeX report

---

## Sprint 4 (T4 19/05 → T3 25/05): Đóng gói & Bảo vệ Phase 1

### Thứ Tư 19/05 — LaTeX Report

- [ ] `[BÁO CÁO]` Review `reports/latex/bao-cao-saas-multi-tenant.tex` — cập nhật 6 sections
- [ ] `[BÁO CÁO]` Bổ sung kết quả thực hành (EXPLAIN output, test screenshot)
- [ ] `[BÁO CÁO]` Compile LaTeX → verify PDF sạch

### Thứ Năm 20/05 — Presentation Polish

- [ ] `[BÁO CÁO]` Review `presentation-notes/thuyet-trinh-saas-multi-tenant.md`
- [ ] `[BÁO CÁO]` Đọc to speaker script, bấm giờ (15-20 phút)
- [ ] `[BÁO CÁO]` Bổ sung FAQ, chuẩn bị kịch bản demo

### Thứ Sáu 21/05 — Demo Rehearsal lần 1

- [ ] `[THỰC HÀNH]` `make db-up` → `make app-run` → CRUD demo 2 tenant
- [ ] `[THỰC HÀNH]` `make app-test` — tất cả pass

### 🚩 Thứ Bảy 22/05 — MILESTONE BÁO CÁO #7

> **Nội dung báo cáo:** LaTeX PDF xong + Demo chạy trơn tru + Presentation sẵn sàng.

- [ ] `[MILESTONE]` Dry-run toàn bộ: mở PDF → trình bày → chạy demo → Q&A
- [ ] `[BÁO CÁO]` Cập nhật `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` — phiên bản cuối

### Chủ Nhật 23/05 — Buffer & Fix

- [ ] `[BÁO CÁO]` Sửa lỗi cuối cùng, polish tài liệu
- [ ] `[THỰC HÀNH]` Final rehearsal: demo từ đầu đến cuối

### Thứ Hai 24/05 — Final Commit

- [ ] `[BÁO CÁO]` Cập nhật ROADMAP.md — tick hết, Quick Stats = 100%
- [ ] `[BÁO CÁO]` Final commit: chốt version Phase 1

### 🚩 Thứ Ba 25/05 — MILESTONE BÁO CÁO #8: BẢO VỆ PHASE 1

> **🎯 BẢO VỆ PHASE 1 VỚI LEADER**

- [ ] `[MILESTONE]` Trình bày + Demo + Q&A
- [ ] `[BÁO CÁO]` Ghi chú feedback Leader vào `local/`

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

1. **Tự code trước.** Không copy solution. Nhờ Agent review SAU KHI đã tự viết.
2. **Commit mỗi ngày.** Message theo Conventional Commits.
3. **Stuck > 2 tiếng** → ghi chú, skip, quay lại sau.
4. **Cập nhật Quick Stats** cuối mỗi ngày.
5. **Milestone = phải có output cụ thể** (SQL output, curl result, test pass, PDF).
