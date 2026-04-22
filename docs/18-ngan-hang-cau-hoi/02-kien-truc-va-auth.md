# ❓ Ngân hàng câu hỏi — Kiến trúc & Service Decomposition

> ⚠️ **KHÔNG có đáp án trong file này.** Tự suy nghĩ trước.

---

## Service Decomposition

### Câu hỏi suy luận (Level 2)

1. So sánh **modular monolith** và **microservices**. Nếu team chỉ có 2–3 developer, phương án nào phù hợp hơn cho Phase 1 và tại sao?

2. Trong kiến trúc target, Auth Service và Authorization Service (RBAC) là hai service riêng. Tại sao có thể tách chúng? Khi nào nên hợp nhất?

3. Notification Service trong sơ đồ kiến trúc được tách riêng. Liệt kê 3 lý do kỹ thuật biện minh cho việc tách này.

4. Giải thích "Conway's Law" (cấu trúc phần mềm phản ánh cấu trúc tổ chức). Nếu chỉ có 1 team backend, việc tách 10 microservices có hợp lý không?

5. Giải thích sự khác nhau giữa "service boundary by business domain" và "service boundary by technical capability". Cho ví dụ cụ thể từ kiến trúc dự án.

### Câu hỏi kiến trúc (Level 3)

6. Đề xuất 2 cách phân tách service khác nhau cho Phase 1 demo. Phân tích ưu nhược điểm của mỗi cách và lập luận cách nào tốt hơn.

7. Nếu bắt đầu bằng modular monolith, mô tả cụ thể các bước kỹ thuật để tách module Auth thành microservice riêng. Cần thay đổi gì ở code, database, deployment?

---

## Auth / AuthZ / RBAC

### Câu hỏi suy luận (Level 2)

8. JWT stateless vs Session-based auth: trong hệ thống multi-tenant SaaS, cái nào phù hợp hơn? Nếu cần revoke token ngay lập tức (ví dụ: sa thải nhân viên), JWT thuần gặp vấn đề gì?

9. Trong RBAC, nếu Admin của Tenant A gửi request xóa bản ghi có ID = 42, nhưng bản ghi ID = 42 thực ra thuộc Tenant B, hệ thống nên xử lý thế nào? Chỉ check role có đủ không?

10. So sánh tự implement JWT auth vs dùng Keycloak. Liệt kê ít nhất 5 tính năng Keycloak cung cấp sẵn mà tự implement phải code từ đầu.

11. Permission nên check ở **API Gateway** hay ở **Backend Service**? Hoặc cả hai? Phân tích trade-off.

### Câu hỏi kiến trúc (Level 3)

12. Thiết kế RBAC cho demo Phase 1 sao cho: mỗi tenant có thể **tự định nghĩa role** của mình (không chỉ dùng role mặc định ADMIN/VIEWER). Data model cần thay đổi gì?

13. Nếu hệ thống sau này cần hỗ trợ **SSO (Single Sign-On)** giữa nhiều ứng dụng Viettel, kiến trúc auth hiện tại cần điều chỉnh thế nào?
