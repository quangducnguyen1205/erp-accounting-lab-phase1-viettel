# 📝 Template ghi chú chủ đề

> Copy template này khi bắt đầu học một chủ đề mới. Ví dụ: `redis-cache.md`

---

## Chủ đề: [SaaS - Software as a Service]

### Ngày bắt đầu: [2026-04-22]
### Trạng thái: 🔄 Đang học

---

### 1. Nó là gì?
> Giải thích bằng lời mình, không copy-paste
...

Là hệ thống phần mềm được host tập trung trên một hệ thống 
cloud của một tổ chức, cung cấp dịch vụ qua internet cho 
nhiều khách hàng (tenant) cùng sử dụng.

### 2. Nó giải quyết vấn đề gì?
Nó giải quyết vấn đề tạo ra một sản phẩm cho nhiều người
dùng với logic nghiệp vụ gần như tái sử dụng, có khả năng
truy cập mọi nơi, không cần cài đặt, và có thể mở rộng 
dễ dàng khi số lượng khách hàng tăng lên, đồng thời giúp 
nhà cung cấp dễ dàng quản lý, cập nhật, và vận hành sản 
phẩm. Ngoài ra chi phí ban đầu rẻ, dễ tiếp cận với khách 
hàng hơn.

### 3. Tại sao quan trọng trong dự án này?
Trong dự án này, chúng ta đang xây dựng một sản phẩm phần mềm
kế toán/ERP cho nhiều doanh nghiệp. Mỗi doanh nghiệp sẽ là một
tenant. Do đặc thù ngành kế toán có các nghiệp vụ tương tự nhau,
cũng như được vận hành dựa trên luật kế toán, nên khi có thay
đổi thì cần đồng bộ trên tất cả các tenant. Do đó, xây dựng 
theo kiến trúc SaaS sẽ giúp chúng ta dễ dàng quản lý, cập nhật
và vận hành sản phẩm cho nhiều khách hàng cùng lúc.

### 4. Có lựa chọn nào khác?

| Lựa chọn | Ưu điểm                                               | Nhược điểm                                                             |
|---------|-------------------------------------------------------|------------------------------------------------------------------------|
| On-premise | Customize được rất sâu với từng doanh nghiệp riêng    | Khi có thay đổi rất khó cập nhật và đồng bộ chung cho nhiều khách hàng |
| Hybrid     | Có thể phục vụ cả SME và enterprise với cùng codebase | Chi phí rất lớn, khó phục vụ được đại chúng SME                        |

### 5. Trade-off khi chọn phương án này?
Chí phí để thiết kế kiến trúc SaaS phức tạp hơn nhiều so với on-premise
đặt biết với việc xác thực và phân quyền tenant, cách ly dữ liệu, quản lý cập nhật, monitoring, v.v.
nhưng đổi lại sẽ dễ dàng quản lý, cập nhật và vận hành cho nhiều khách 
hàng cùng lúc. Nếu chọn on-premise thì sẽ dễ dàng customize sâu cho từng 
khách hàng, nhưng khi có thay đổi thì sẽ rất khó cập nhật và đồng bộ chung 
cho nhiều khách hàng. Hybrid thì có thể phục vụ cả SME và enterprise, 
nhưng chi phí rất lớn, khó phục vụ được đại chúng SME.

### 6. Áp dụng vào demo Phase 1 như thế nào?
Trong demo Phase 1, chúng ta sẽ xây dựng một phiên bản SaaS đơn giản để 
phục vụ một số khách hàng đầu tiên. Chúng ta sẽ thiết kế kiến trúc multi-tenant, 
đảm bảo cách ly dữ liệu giữa các tenant, và xây dựng hệ thống quản lý tenant để dễ 
dàng thêm/sửa/xóa khách hàng. Chúng ta cũng sẽ triển khai trên cloud để tận dụng lợi 
ích của centralized hosting và scalability.

### 7. Câu hỏi tự đặt
- ...
- ...

### 8. Câu hỏi chưa giải đáp (hỏi mentor)
- ...

### 9. Tài liệu tham khảo
- [link 1]
- [link 2]
