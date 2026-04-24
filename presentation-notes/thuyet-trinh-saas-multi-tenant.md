# Thuyết trình nhanh: SaaS, Multi-tenant và nền tảng Backend/Database

Tài liệu này dùng để mở ra và nói theo mạch. Đây không phải slide thiết kế.

---

## 1. Mở đầu: Tôi đã học gì?

```text
Phase 1
├── SaaS foundation
├── Multi-tenant architecture
├── Tenant isolation trade-off
└── Backend/database expansion
    ├── PostgreSQL
    ├── Index và query tenant-aware
    ├── Noisy neighbor
    └── Migration, lock, rollback
```

Ý chính:

- Tôi đã gần hoàn tất phần nền tảng SaaS và multi-tenant.
- Hiện tại đang tổng hợp lại kiến thức và đi sâu hơn vào database/backend.
- Repo này là nơi lưu bản kiến thức đã chuẩn hóa, không lưu toàn bộ nháp học tập.

Ghi chú khi trình bày:

- Nói rõ đây là kết quả học nền tảng, chưa overclaim là đã làm production hoàn chỉnh.
- Nhấn mạnh hướng học: hiểu trade-off trước, code demo sau.

---

## 2. SaaS là gì?

```text
SaaS = Software as a Service

Nhà cung cấp
├── host phần mềm
├── vận hành
├── bảo trì
└── cập nhật

Khách hàng
└── truy cập qua internet và sử dụng
```

SaaS là mô hình phân phối phần mềm. Điểm chính là nhà cung cấp chịu trách nhiệm vận hành hệ thống, khách hàng không tự cài đặt và quản lý server.

| Hiểu đúng | Không nên hiểu nhầm |
|---|---|
| SaaS là delivery model | SaaS không chỉ là "chạy trên cloud" |
| Nhà cung cấp vận hành | Không bắt buộc phải public cloud |
| Khách hàng dùng như dịch vụ | Không đồng nghĩa với subscription |

Ghi chú khi trình bày:

- Có thể nói ví dụ Google Workspace, Slack, MISA online.
- Với ERP/kế toán, SaaS giúp nhiều doanh nghiệp dùng hệ thống mà không tự vận hành server.

---

## 3. Multi-tenant là gì?

```text
Một nền tảng ERP/kế toán SaaS
┌─────────────────────────────────────────┐
│                 App                     │
│                                         │
│  Tenant A      Tenant B      Tenant C   │
│  Công ty A     Công ty B     Công ty C  │
│  data riêng    data riêng    data riêng │
└─────────────────────────────────────────┘
```

Multi-tenant là pattern kiến trúc cho phép nhiều khách hàng dùng chung nền tảng nhưng dữ liệu và quyền truy cập phải được cách ly.

Chia sẻ:

- Codebase.
- Hạ tầng.
- Pipeline deploy.
- Logic nghiệp vụ lõi.

Cách ly:

- Dữ liệu.
- User và quyền.
- Cấu hình.
- Cache.
- Log/metrics theo tenant.

Ghi chú khi trình bày:

- Mỗi doanh nghiệp trong bài toán kế toán có thể xem là một tenant.
- Rủi ro lớn nhất là data leakage giữa tenant.

---

## 4. SaaS và multi-tenant khác nhau thế nào?

```text
SaaS
└── Mô hình phân phối
    └── Ai host và vận hành phần mềm?

Multi-tenant
└── Pattern kiến trúc
    └── Hệ thống phục vụ nhiều khách hàng thế nào?

Hai khái niệm thường đi cùng
nhưng không đồng nghĩa.
```

| Khái niệm | Trục quyết định | Câu hỏi |
|---|---|---|
| SaaS | Delivery model | Ai vận hành phần mềm? |
| Subscription | Pricing model | Khách hàng trả tiền thế nào? |
| Multi-tenant | Architecture pattern | Nhiều khách hàng được phục vụ ra sao? |

Ghi chú khi trình bày:

- Đây là điểm tôi đã sửa lại trong quá trình học: ban đầu dễ đánh đồng SaaS với multi-tenant.
- Có SaaS single-tenant và cũng có multi-tenant không phải SaaS.

---

## 5. Các mô hình tenant isolation

```text
MH1: Shared table + tenant_id

invoice
┌────┬───────────┬────────────┐
│ id │ tenant_id │ invoice_no │
├────┼───────────┼────────────┤
│  1 │     10    │ INV-001    │
│  2 │     20    │ INV-002    │
└────┴───────────┴────────────┘

MH2: Schema per tenant

database
├── tenant_10.invoice
└── tenant_20.invoice

MH3: Database per tenant

tenant_10_db
tenant_20_db
```

| Mô hình | Chi phí | Isolation | Migration | Phù hợp |
|---|---:|---:|---:|---|
| Shared table + `tenant_id` | Thấp | Thấp/TB | Dễ nhất | Phase 1, SME |
| Schema per tenant | TB | TB | N schema | Tenant vừa, cần cách ly hơn |
| DB per tenant | Cao | Cao | N DB | Enterprise |

Ghi chú khi trình bày:

- Phase 1 chọn shared table + `tenant_id` vì đơn giản và học đúng được vấn đề.
- Nếu enterprise yêu cầu mạnh hơn, có thể chuyển sang hybrid.

---

## 6. Feature flags và giảm rủi ro rollout

```text
new_report_v2
├── Tenant A: bật
├── Tenant B: tắt
└── Tenant C: bật
```

Feature flag cho phép deploy code nhưng chưa bật tính năng cho tất cả tenant.

Lợi ích:

- Bật thử cho một số tenant.
- Tắt nhanh khi có lỗi.
- Giảm blast radius.
- Hỗ trợ gói dịch vụ khác nhau.

Data model tối giản:

| Bảng | Vai trò |
|---|---|
| `feature_flags` | Định nghĩa tính năng |
| `tenant_feature_flags` | Tenant nào bật/tắt tính năng nào |

Ghi chú khi trình bày:

- Feature flag không thay thế test, nhưng giúp kiểm soát rollout trong SaaS.
- Với ERP/kế toán, có thể dùng để bật module báo cáo mới cho một nhóm tenant trước.

---

## 7. Zero-downtime deployment

```text
Deploy an toàn hơn

1. Migration tương thích ngược
2. Rolling hoặc blue-green deployment
3. Health check
4. Feature flag
5. Theo dõi logs/metrics
6. Rollback plan
```

Rolling deployment:

```text
Pod cũ:  A A A
Step 1:  B A A
Step 2:  B B A
Step 3:  B B B
```

Blue-green deployment:

```text
Blue  = version đang chạy
Green = version mới đã chuẩn bị

Traffic -> Blue
Kiểm tra Green ổn
Traffic -> Green
Rollback nhanh: Traffic -> Blue
```

Ghi chú khi trình bày:

- Điểm khó nhất là database migration, vì rollback schema khó hơn rollback code.
- Backward-compatible migration giúp code cũ và code mới cùng chạy được trong lúc rollout.

---

## 8. Noisy neighbor trong shared table

```text
Shared database
├── Tenant A: chạy report nặng
│   ├── ăn CPU
│   ├── tăng disk I/O
│   └── chiếm connection
└── Tenant B: query nhỏ nhưng bị chậm theo
```

Noisy neighbor là khi một tenant dùng nhiều tài nguyên chung và làm tenant khác bị ảnh hưởng.

Các tầng thường gặp:

| Tầng | Ảnh hưởng |
|---|---|
| CPU | Query nặng chiếm xử lý |
| Disk I/O | Full scan đọc nhiều dữ liệu |
| Shared buffers | Cache của tenant khác bị đẩy ra |
| Connection pool | Tenant lớn chiếm hết connection |
| VACUUM | Bảng chung nhiều dead rows làm dọn dẹp nặng |

Giảm rủi ro:

- Query luôn có `tenant_id`.
- Index `(tenant_id, ...)`.
- Giới hạn report/export nặng.
- Resource quota hoặc connection pool riêng cho tenant lớn.
- Read replica cho workload đọc nặng.
- Cân nhắc partitioning hoặc DB riêng khi cần.

Ghi chú khi trình bày:

- Shared table không sai, nhưng phải hiểu cái giá khi dữ liệu tăng.
- Index tốt giúp nhiều query tenant nhỏ không phải quét dữ liệu tenant lớn.

---

## 9. Migration complexity trong schema-per-tenant

```text
Thêm cột tax_code

tenant_001.invoice -> OK
tenant_002.invoice -> OK
tenant_003.invoice -> FAIL
tenant_004.invoice -> chưa chạy
...
tenant_500.invoice -> chưa chạy
```

Với schema per tenant, một thay đổi schema phải chạy qua nhiều schema. Rủi ro chính là partial migration.

| Rủi ro | Ý nghĩa |
|---|---|
| Partial migration | Một số tenant schema mới, một số tenant schema cũ |
| Lock per schema | Tenant đang dùng có thể bị block |
| Thời gian tổng dài | N tenant nhân với thời gian mỗi schema |
| Rollback khó | Rollback cũng phải chạy N lần |

Ghi chú khi trình bày:

- Đây là lý do không nên vội chọn schema per tenant khi chưa cần.
- Phase 1 dùng shared table để tập trung học data isolation, index và tenant-aware query trước.

---

## 10. Vì sao cần hiểu sâu database/backend?

```text
SaaS backend không chỉ là CRUD

CRUD
└── thêm tenant context
    ├── query isolation
    ├── index strategy
    ├── migration safety
    ├── locking behavior
    ├── cache isolation
    └── production observability
```

Những phần cần đào sâu tiếp:

- PostgreSQL.
- Index và query planning.
- Locking.
- Migration strategy.
- Partitioning.
- Read replicas.
- Production backend behavior.

Ghi chú khi trình bày:

- Lý thuyết SaaS/multi-tenant đã đủ nền để chuyển sang thực hành backend có kiểm soát.
- Cần thực hành bằng dữ liệu, query plan và migration thật, không chỉ đọc khái niệm.

---

## 11. Kết luận và hướng tiếp theo

```text
Đã nắm nền tảng
├── SaaS là delivery model
├── Multi-tenant là architecture pattern
├── Tenant isolation có trade-off
├── Shared table cần tenant-aware mọi lớp
└── Database behavior ảnh hưởng trực tiếp production

Tiếp theo
├── Học sâu PostgreSQL
├── Thực hành index/query plan
├── Thực hành migration an toàn
└── Tự code demo nhỏ rồi nhờ Agent review
```

Hướng repo:

- Repo này giữ vai trò kho kiến thức Phase 1.
- Chưa tạo full coding project ngay.
- Demo nhỏ có thể đặt tạm trong `lab-code/` hoặc `demo/`.
- Nếu demo lớn, tách repository riêng cho code.

Ghi chú khi trình bày:

- Kết lại bằng tinh thần học tiếp: đã có nền, bước sau là kiểm chứng bằng code và database thực tế.
- Nhấn mạnh mình sẽ tự implement code trước rồi nhờ Agent review để học sâu hơn.
