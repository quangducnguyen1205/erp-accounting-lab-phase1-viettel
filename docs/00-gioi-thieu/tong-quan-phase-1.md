# Tổng quan Phase 1

Phase 1 tập trung xây nền tảng tư duy backend cho một hệ thống ERP/kế toán SaaS đa tenant. Trọng tâm không phải là code thật nhanh, mà là hiểu đúng các quyết định kiến trúc trước khi đi vào triển khai.

## Mục tiêu học

- Hiểu SaaS là mô hình phân phối phần mềm, không chỉ là "chạy trên cloud".
- Hiểu multi-tenant là một lựa chọn kiến trúc để phục vụ nhiều khách hàng trên cùng nền tảng.
- Biết phân tích tenant isolation theo trade-off, không chọn theo cảm tính.
- Nhận ra các rủi ro production như data leakage, noisy neighbor, migration lock, rollback và blast radius.
- Chuẩn bị nền để học sâu hơn về PostgreSQL và backend production behavior.

## Bối cảnh ERP/kế toán

Trong bài toán ERP/kế toán SaaS:

- Một tenant thường tương ứng với một doanh nghiệp.
- Dữ liệu kế toán nhạy cảm, cần cách ly rõ giữa các tenant.
- Nghiệp vụ kế toán có phần lõi dùng chung, nhưng vẫn cần cấu hình theo từng doanh nghiệp.
- Một lỗi deploy hoặc migration có thể ảnh hưởng nhiều khách hàng cùng lúc nếu dùng chung nền tảng.

Vì vậy, kiến thức SaaS và multi-tenant không chỉ là lý thuyết. Nó ảnh hưởng trực tiếp đến database design, auth, phân quyền, cache, log, migration và vận hành.

## Phạm vi đã gần hoàn tất

- SaaS vs on-premise.
- SaaS vs cloud vs subscription.
- Multi-tenant vs single-tenant.
- Ba mô hình tenant isolation.
- Feature flags và rollout an toàn.
- Zero-downtime deployment ở mức khái niệm.
- Index tenant-aware, noisy neighbor, migration lock và PostgreSQL basics.

## Hướng đang đi sâu

Các phần backend/database cần tiếp tục đào sâu:

- PostgreSQL internals vừa đủ dùng cho backend.
- Index, composite index và query planning.
- Locking và migration strategy.
- Rollback trong môi trường nhiều tenant.
- Partitioning, vacuum và read replica.
- Cách hệ thống backend hành xử khi dữ liệu và traffic tăng.

## Quyết định về code demo

Trong giai đoạn hiện tại, repo này nên giữ vai trò chính là kho kiến thức Phase 1. Chưa cần tạo full coding project.

Khi bắt đầu code:

- Nếu demo nhỏ và phục vụ trực tiếp cho phần lý thuyết, có thể đặt trong `lab-code/` hoặc `demo/`.
- Nếu demo lớn dần thành project backend thật, nên tách sang repository riêng để không trộn code đang thay đổi liên tục với tài liệu kiến thức đã chuẩn hóa.

Workflow học code nên là:

1. Tự viết code theo task trước.
2. Sau đó nhờ Agent review, tìm lỗi, đề xuất sửa và giải thích lý do.
3. Dùng Agent như reviewer/mentor kỹ thuật, không dùng để thay thế quá trình tự học.
