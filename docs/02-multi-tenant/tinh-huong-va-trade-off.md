# Tình huống và trade-off Multi-tenant

## Kiến trúc là bài toán trade-off

Khi chọn mô hình tenant isolation, câu hỏi không phải là "mô hình nào tốt nhất", mà là "mô hình nào phù hợp nhất với bối cảnh hiện tại".

Các chiều cần đánh giá:

| Chiều đánh giá | Câu hỏi cần hỏi |
|---|---|
| Chi phí | Tenant có thể chia sẻ hạ tầng đến mức nào? |
| Isolation | Mức cách ly dữ liệu cần mạnh đến đâu? |
| Migration | Khi đổi schema, hệ thống có chịu được độ phức tạp không? |
| Blast radius | Một lỗi ảnh hưởng bao nhiêu tenant? |
| Operations | Team có đủ năng lực vận hành mô hình này không? |
| Customization | Có cần schema hoặc logic riêng cho tenant không? |

## Tình huống 1: 500 SME và 5 enterprise

Một hệ thống kế toán có 500 doanh nghiệp SME và 5 khách hàng enterprise. Lựa chọn hợp lý thường là hybrid:

- SME dùng shared table với `tenant_id` để tối ưu chi phí.
- Enterprise dùng database riêng hoặc môi trường riêng nếu có yêu cầu isolation, SLA hoặc data residency cao.

Trade-off:

| Lựa chọn | Lợi ích | Cái giá phải trả |
|---|---|---|
| Tất cả dùng shared table | Rẻ, dễ vận hành ban đầu | Enterprise có thể không chấp nhận isolation yếu |
| Tất cả dùng DB riêng | Isolation mạnh | Chi phí và vận hành quá cao cho SME |
| Hybrid | Cân bằng theo phân khúc khách hàng | Code và tooling phải hỗ trợ nhiều mode |

## Tình huống 2: Quên tenant filter

Query thiếu `tenant_id` là lỗi bảo mật nghiêm trọng:

```sql
SELECT *
FROM invoice
WHERE status = 'OVERDUE';
```

Hậu quả:

- Tenant A có thể thấy hóa đơn của tenant B.
- Export/report có thể trộn dữ liệu.
- Log hoặc cache có thể tiếp tục lan lỗi sang lớp khác.

Cách giảm rủi ro:

- Repository/base query bắt buộc nhận tenant context.
- Middleware lấy tenant từ token và đặt vào request context.
- Integration test kiểm tra không trả dữ liệu tenant khác.
- Code review checklist cho mọi query.
- Cân nhắc PostgreSQL Row Level Security khi cần DB-level guardrail.

## Tình huống 3: Cache key không có tenant prefix

Sai:

```text
categories
```

Đúng hơn:

```text
tenant:42:categories
```

Nếu tenant A request trước, dữ liệu của A được cache dưới key chung. Tenant B request sau có thể nhận lại dữ liệu của A nếu key không chứa tenant. Đây là data leakage qua cache layer.

## Tình huống 4: Feature flags khi rollout

Feature flags giúp giảm rủi ro khi đưa tính năng mới vào hệ thống SaaS.

Ví dụ:

```text
new_tax_report_v2
├── Tenant A: bật
├── Tenant B: tắt
└── Tenant C: bật
```

Lợi ích:

- Deploy code trước nhưng chưa bật cho tất cả.
- Bật thử với một nhóm tenant ít rủi ro.
- Tắt nhanh nếu phát hiện lỗi.
- Hỗ trợ khác biệt theo gói dịch vụ.

## Tình huống 5: Zero-downtime deployment

Trong SaaS, không nên dừng hệ thống để deploy nếu tenant đang sử dụng.

Một quy trình an toàn hơn:

1. Migration backward-compatible.
2. Deploy rolling hoặc blue-green.
3. Health check trước khi nhận traffic.
4. Feature flag để kiểm soát tính năng mới.
5. Theo dõi log/metrics theo tenant.
6. Có kế hoạch rollback.

Điểm khó nhất thường nằm ở database migration. Code có thể rollback nhanh, nhưng schema đã đổi sai có thể rất khó quay lại nếu đã ghi dữ liệu mới.

## Tình huống 6: Customization theo tenant

ERP/kế toán thường có khác biệt theo doanh nghiệp:

- Năm tài chính bắt đầu khác nhau.
- Module bật/tắt khác nhau.
- Hạn mức người dùng khác nhau.
- Quy trình phê duyệt khác nhau.

Ưu tiên ban đầu nên là configuration và feature flags, không fork code riêng cho từng tenant. Fork code làm chi phí vận hành và test tăng rất nhanh.

## Kết luận

Mỗi quyết định multi-tenant kéo theo hệ quả ở nhiều lớp: database, auth, cache, deployment, monitoring và operations. Với Phase 1, mục tiêu là hiểu rõ các trade-off và chọn mô hình đơn giản đủ dùng, đồng thời không khóa đường phát triển sau này.
