# ❓ Ngân hàng câu hỏi tự học — SaaS & Multi-tenant

> ⚠️ **KHÔNG có đáp án trong file này.** Tự trả lời trước, sau đó kiểm tra ở [19-dap-an/](../19-dap-an/).

---

## SaaS

### Câu hỏi suy luận (Level 2)

1. So sánh SaaS và On-Premise từ góc nhìn của **nhà phát triển phần mềm** (không phải góc nhìn khách hàng). Phía nào phức tạp hơn và tại sao?

2. Nếu một khách hàng doanh nghiệp lớn yêu cầu dữ liệu kế toán **phải nằm trên server riêng** (data residency requirement), kiến trúc SaaS cần điều chỉnh thế nào? Có mâu thuẫn với bản chất SaaS không?

3. Tại sao SaaS thường dùng mô hình **subscription** (trả phí theo tháng) thay vì **license** (mua đứt)? Lợi ích kỹ thuật và kinh doanh là gì?

4. Khi deploy **một bản cập nhật** cho SaaS platform, nếu bản cập nhật gây lỗi, ảnh hưởng bao nhiêu khách hàng? So sánh với on-premise khi cùng tình huống.

### Câu hỏi kiến trúc (Level 3)

5. Thiết kế một cơ chế **feature flags** cho SaaS platform sao cho: một số tenant được dùng tính năng mới, một số chưa. Mô tả data model và logic xử lý.

6. Đề xuất chiến lược **zero-downtime deployment** cho SaaS platform có 100 tenants đang active. Cần xem xét những gì?

---

## Multi-tenant

### Câu hỏi suy luận (Level 2)

7. So sánh 3 mô hình tenant isolation (tenant_id column, schema per tenant, DB per tenant). Nếu hệ thống kế toán có 500 doanh nghiệp SME và 5 doanh nghiệp enterprise, bạn sẽ chọn chiến lược nào? Giải thích rõ ràng.

8. Trong mô hình `tenant_id` column, nếu một developer **quên** thêm `WHERE tenant_id = ?` trong một query, hậu quả là gì? Đề xuất ít nhất 3 cách phòng tránh.

9. Cache key `"categories"` vs `"tenant:1:categories"` — giải thích tại sao cách đầu tiên là sai lầm nghiêm trọng trong hệ thống multi-tenant.

10. Tenant A có 1 triệu bản ghi master data, Tenant B có 100 bản ghi. Cả hai dùng chung bảng với `tenant_id`. Hiệu suất query của Tenant B có bị ảnh hưởng không? Tại sao? Giải pháp?

11. Trong hệ thống kế toán, nếu dùng schema per tenant, khi có thay đổi schema (ví dụ: thêm cột mới), phải migrate bao nhiêu schema? Quy trình ra sao? Rủi ro gì?

### Câu hỏi kiến trúc (Level 3)

12. Thiết kế cơ chế **tự động inject tenant_id** vào mọi query mà developer không cần tự thêm. Mô tả kiến trúc (interceptor, aspect, Hibernate filter, hoặc cách khác).

13. Một tenant yêu cầu **export toàn bộ dữ liệu** của họ (data portability). Với mỗi mô hình isolation, mô tả cách thực hiện và độ khó.

14. Thiết kế cơ chế cho phép **một user thuộc nhiều tenant** (ví dụ: kế toán freelancer làm cho 3 công ty). Data model và auth flow thay đổi thế nào?
