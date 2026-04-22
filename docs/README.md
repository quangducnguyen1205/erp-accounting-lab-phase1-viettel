# 📚 Phase 1 — Tài liệu học tập nền tảng kỹ thuật

> **Người học:** Quang Đức Nguyễn  
> **Dự án:** Nền tảng kế toán / ERP SaaS đa tenant — Viettel  
> **Giai đoạn:** Phase 1 — Xây dựng nền tảng kỹ thuật & tư duy kiến trúc  
> **Thời gian bắt đầu:** Tháng 4/2026

---

## 🎯 Phase 1 là gì?

Phase 1 tập trung vào việc **xây dựng nền tảng kỹ thuật vững chắc** và **tư duy kiến trúc hệ thống** cho một sản phẩm SaaS multi-tenant hướng backend.

**Mục tiêu cốt lõi:**
- Hiểu sâu SaaS, multi-tenant, microservices
- Nắm vững các công nghệ nền tảng (PostgreSQL, Redis, Kafka, Kong, MinIO, ...)
- Xây được demo backend + frontend đơn giản
- Tạo thói quen: **học → suy luận → so sánh → áp dụng → ghi chép**

**Không nằm trong scope Phase 1:**
- Nghiệp vụ kế toán chuyên sâu (General Ledger, Purchase flow, hạch toán...)
- Production-ready architecture
- Performance optimization nâng cao

---

## 🗂️ Cấu trúc tài liệu

```
docs/
├── README.md                          ← Bạn đang ở đây
├── 00-huong-dan-hoc/                  ← Hướng dẫn đọc & lộ trình
├── 01-tong-quan-phase-1/              ← Mục tiêu, kỳ vọng, triết lý
├── 02-saas/                           ← SaaS là gì, tại sao quan trọng
├── 03-multi-tenant/                   ← Multi-tenant design sâu
├── 04-phan-tach-service/              ← Service decomposition & reasoning
├── 05-auth-authz-rbac/                ← Login, xác thực, phân quyền
├── 06-api-gateway-kong/               ← API Gateway, Kong, routing
├── 07-postgresql-data-modeling/       ← PostgreSQL, data modeling đa tenant
├── 08-redis-cache/                    ← Cache, Redis, invalidation
├── 09-kafka-async/                    ← Message broker, Kafka, event-driven
├── 10-minio-file-storage/             ← Object storage, MinIO
├── 11-observability/                  ← Prometheus, Grafana, Loki
├── 12-clean-code-conventions/         ← Convention code, clean architecture
├── 13-design-patterns/                ← Design patterns áp dụng thực tế
├── 14-react-cho-demo/                 ← React vừa đủ cho demo Phase 1
├── 15-kien-truc-demo-phase-1/         ← Kiến trúc đề xuất cho demo
├── 16-lo-trinh-thuc-hien/             ← Roadmap chi tiết từng milestone
├── 17-tu-duy-kien-truc/               ← Handbook tư duy kiến trúc
├── 18-ngan-hang-cau-hoi/              ← Câu hỏi tự học (KHÔNG có đáp án)
├── 19-dap-an/                         ← Đáp án mẫu tách riêng
├── 20-ghi-chu-xac-nhan-mentor/        ← Giả định cần verify với mentor
├── 21-mau-ghi-chu/                    ← Template ghi chú hàng ngày/tuần
```

---

## 📖 Thứ tự đọc khuyến nghị

### Tuần 1: Nền tảng lý thuyết
1. [`00-huong-dan-hoc/`](./00-huong-dan-hoc/) — Đọc hướng dẫn trước
2. [`01-tong-quan-phase-1/`](./01-tong-quan-phase-1/) — Hiểu bức tranh lớn
3. [`02-saas/`](./02-saas/) — SaaS foundation
4. [`03-multi-tenant/`](./03-multi-tenant/) — Multi-tenant design

### Tuần 2: Kiến trúc & phân tách
5. [`04-phan-tach-service/`](./04-phan-tach-service/) — Service decomposition
6. [`05-auth-authz-rbac/`](./05-auth-authz-rbac/) — Authentication & Authorization
7. [`06-api-gateway-kong/`](./06-api-gateway-kong/) — API Gateway

### Tuần 3: Công nghệ nền tảng
8. [`07-postgresql-data-modeling/`](./07-postgresql-data-modeling/) — Data modeling
9. [`08-redis-cache/`](./08-redis-cache/) — Cache strategy
10. [`09-kafka-async/`](./09-kafka-async/) — Async messaging
11. [`10-minio-file-storage/`](./10-minio-file-storage/) — File storage

### Tuần 4: Vận hành & chất lượng code
12. [`11-observability/`](./11-observability/) — Monitoring
13. [`12-clean-code-conventions/`](./12-clean-code-conventions/) — Code quality
14. [`13-design-patterns/`](./13-design-patterns/) — Design patterns

### Tuần 5–6: Thiết kế & xây demo
15. [`14-react-cho-demo/`](./14-react-cho-demo/) — React basics
16. [`15-kien-truc-demo-phase-1/`](./15-kien-truc-demo-phase-1/) — Demo architecture
17. [`16-lo-trinh-thuc-hien/`](./16-lo-trinh-thuc-hien/) — Implementation roadmap

### Xuyên suốt
18. [`17-tu-duy-kien-truc/`](./17-tu-duy-kien-truc/) — Đọc bất cứ lúc nào cần suy luận
19. [`18-ngan-hang-cau-hoi/`](./18-ngan-hang-cau-hoi/) — Tự trả lời trước
20. [`19-dap-an/`](./19-dap-an/) — Kiểm tra sau khi đã tự suy nghĩ
21. [`20-ghi-chu-xac-nhan-mentor/`](./20-ghi-chu-xac-nhan-mentor/) — Ghi câu hỏi cho mentor
22. [`21-mau-ghi-chu/`](./21-mau-ghi-chu/) — Dùng template ghi chép hàng ngày

---

## 🏷️ Quy ước đặt tên file

| Loại | Quy ước | Ví dụ |
|------|---------|-------|
| Thư mục chủ đề | `XX-ten-chu-de/` | `03-multi-tenant/` |
| File nội dung chính | `01-ten-noi-dung.md` | `01-khai-niem-co-ban.md` |
| File so sánh | `XX-so-sanh-*.md` | `02-so-sanh-cac-mo-hinh.md` |
| File trade-off | `XX-trade-off-*.md` | `03-trade-off-va-lua-chon.md` |
| File áp dụng demo | `XX-ap-dung-demo.md` | `04-ap-dung-vao-demo.md` |
| Template | `template-*.md` | `template-ghi-chu-hang-ngay.md` |

**Nguyên tắc chung:**
- Dùng tiếng Việt không dấu, viết thường, nối bằng dấu gạch ngang
- Đánh số thứ tự trong mỗi folder
- Mỗi file tập trung một chủ đề, không quá 300 dòng

---

## 🧭 Cách sử dụng ngân hàng câu hỏi

1. **Đọc lý thuyết** ở thư mục chủ đề tương ứng
2. **Mở câu hỏi** ở [`18-ngan-hang-cau-hoi/`](./18-ngan-hang-cau-hoi/)
3. **Tự trả lời** — viết ra giấy hoặc file trước
4. **So sánh với đáp án** ở [`19-dap-an/`](./19-dap-an/)
5. **Ghi lại điểm sai/thiếu** để review lại

> ⚠️ **Quan trọng:** Không đọc đáp án trước khi tự suy nghĩ. Mục đích là rèn tư duy, không phải ghi nhớ.

---

## 📝 Triết lý học tập

```
Học → Suy luận → So sánh → Áp dụng → Ghi chép → Xác minh
```

Với mỗi chủ đề kỹ thuật, luôn tự hỏi:
- **Nó là gì?** — Định nghĩa rõ ràng
- **Nó giải quyết vấn đề gì?** — Problem statement
- **Tại sao chọn nó?** — Lý do trong ngữ cảnh dự án
- **Có lựa chọn nào khác?** — Alternatives
- **Trade-off là gì?** — Đánh đổi
- **Áp dụng vào demo Phase 1 như thế nào?** — Thực hành

---

*Tài liệu này được duy trì liên tục trong suốt Phase 1.*
