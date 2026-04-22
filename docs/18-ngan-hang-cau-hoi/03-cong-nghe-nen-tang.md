# ❓ Ngân hàng câu hỏi — Công nghệ nền tảng

> ⚠️ **KHÔNG có đáp án trong file này.**

---

## API Gateway

### Level 2

1. Nếu demo Phase 1 chỉ có 1 backend service, API Gateway có cần thiết không? Liệt kê 3 chức năng của Gateway mà 1 backend đơn lẻ cũng có thể tự làm.

2. So sánh Kong, Nginx reverse proxy, và Traefik. Trong điều kiện nào thì mỗi cái phù hợp nhất?

### Level 3

3. Thiết kế cách API Gateway inject `X-Tenant-Id` header vào request dựa trên JWT token. Vẽ sequence diagram cho flow này.

---

## PostgreSQL & Data Modeling

### Level 2

4. Tại sao `UNIQUE(code)` là sai nhưng `UNIQUE(tenant_id, code)` là đúng trong hệ thống multi-tenant? Cho ví dụ cụ thể.

5. So sánh soft delete (`is_active = false`) vs hard delete (`DELETE FROM`). Trong hệ thống kế toán, cái nào phù hợp hơn và tại sao?

6. Pagination bằng `OFFSET` có vấn đề gì khi data lớn? Có phương pháp nào tốt hơn?

### Level 3

7. Thiết kế schema cho module "Danh mục vật tư" có hỗ trợ: multi-tenant, soft delete, audit (ai tạo, ai sửa, lúc nào), và phân loại theo nhóm. Vẽ ER diagram.

8. Nếu tenant A có 5 triệu bản ghi và query chậm, liệt kê 5 cách cải thiện hiệu suất mà không cần thay đổi mô hình isolation.

---

## Redis

### Level 2

9. Giải thích "cache stampede" (thundering herd). Khi cache hết hạn, 1000 request đồng thời đều query DB. Giải pháp?

10. Trong hệ thống multi-tenant, nếu invalidate cache cho tất cả tenants mỗi khi bất kỳ tenant nào update data, có vấn đề gì?

### Level 3

11. Thiết kế cache strategy cho API "GET /api/master-data?category=X": khi nào cache, khi nào invalidate, key format, TTL. Đảm bảo multi-tenant safe.

---

## Kafka

### Level 2

12. So sánh synchronous (REST) vs asynchronous (Kafka) communication. Cho 3 ví dụ use case phù hợp với mỗi loại trong dự án kế toán.

13. Kafka giữ message lâu dài (retention), RabbitMQ xóa sau khi consume. Ưu điểm của việc giữ lâu là gì? Khi nào cần replay message?

### Level 3

14. Thiết kế event schema cho event "purchase_voucher_created". Nên bao gồm những field nào? Tại sao?

---

## MinIO

### Level 2

15. Tại sao không nên lưu file trực tiếp vào PostgreSQL (dùng BYTEA column)? Liệt kê ít nhất 4 lý do.

16. Trong hệ thống multi-tenant, file upload cần tenant isolation như thế nào? Mô tả bucket/path strategy.

---

## Observability

### Level 2

17. Phân biệt metrics, logs, và traces. Cho ví dụ cụ thể của mỗi loại trong hệ thống kế toán.

18. Nếu hệ thống chậm và bạn chỉ có logs (không có metrics, không có traces), bạn có thể debug hiệu quả không? Thiếu gì?

### Level 3

19. Thiết kế "minimum viable observability" cho Phase 1 demo: cần monitor những gì, dùng tool gì, tốn bao nhiêu effort?

---

## Clean Code & Design Patterns

### Level 2

20. Giải thích tại sao Controller không nên chứa business logic. Cho ví dụ vi phạm và cách sửa.

21. So sánh Repository pattern trong Spring Data JPA với việc dùng `EntityManager` trực tiếp. Khi nào cần custom query?

22. Strategy pattern giải quyết vấn đề gì trong tenant resolver? Nếu không dùng Strategy, code sẽ trông thế nào?

### Level 3

23. Thiết kế cấu trúc folder cho Spring Boot project sao cho dễ tách module thành microservice sau này. Giải thích nguyên tắc.

---

## Tổng hợp kiến trúc

### Level 3

24. Nếu Phase 1 demo thành công, bước tiếp theo là thêm Keycloak + Kong + Redis. Mô tả thứ tự thêm và lý do.

25. Vẽ **2 kiến trúc khác nhau** cho Phase 1 demo: một kiến trúc "tối giản nhất" và một kiến trúc "đầy đủ hơn". Phân tích khi nào chọn cái nào.

26. Hệ thống target có Debezium CDC. Giải thích CDC giải quyết vấn đề gì và tại sao không dùng REST API call giữa services để sync data.
