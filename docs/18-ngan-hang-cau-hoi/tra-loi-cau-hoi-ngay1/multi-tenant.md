## Multi-tenant

### Câu hỏi suy luận (Level 2)

7. So sánh 3 mô hình tenant isolation (tenant_id column, schema per tenant, DB per tenant). Nếu hệ thống kế toán có 500 doanh nghiệp SME và 5 doanh nghiệp enterprise, bạn sẽ chọn chiến lược nào? Giải thích rõ ràng.

3 mô hình này tiến hoá từ đơn giản đến phức tạp, do đó những
tiêu chí tỉ lệ thuận với sự đơn giản sẽ tiến hoá dần lên, ngược
lại thì sẽ giảm dần đi. Tính từ trái qua phải,
ví dụ việc quản lý tenant sẽ dễ dần với số lượng giảm dần, 
chi phí sẽ tăng dần, tính insolation sẽ tăng dần, khả năng customize 
sẽ tăng dần, khả năng scale sẽ giảm dần, v.v
Với hệ thống kế toán có 500 doanh nghiệp SME và 5 doanh nghiệp 
enterprise, tôi sẽ chọn chiến lượng hybrid. Tức là với 500 doanh
nghiệp SME, sẽ dùng mô hình tenant_id column, còn với 5 doanh nghiệp enterprise sẽ dùng mô hình DB per tenant. 
Lý do là vì với 500 doanh nghiệp SME thì việc dùng tenant_id 
column sẽ giúp tiết kiệm chi phí hơn rất nhiều, và với 5 doanh 
nghiệp enterprise thì việc dùng DB per tenant sẽ giúp cách ly 
dữ liệu tốt hơn, cũng như có thể customize sâu hơn cho từng 
doanh nghiệp lớn đó, mà chi phí thì cũng không quá lớn vì chỉ 
có 5 doanh nghiệp.
8. Trong mô hình `tenant_id` column, nếu một developer **quên** thêm `WHERE tenant_id = ?` trong một query, hậu quả là gì? Đề xuất ít nhất 3 cách phòng tránh.

- Vì mô hình tenant_id column, chỉ dùng 1 db, nên nếu một developer
quên thêm `WHERE tenant_id = ?` trong một query thì sẽ dẫn đến 
việc truy vấn dữ liệu của tất cả các tenant, gây ra lỗi bảo
mật nghiêm trọng.
- 3 cách phóng tránh được đề xuất:
// Chưa nghĩ ra, cần hỗ trợ đáp án
9. Cache key `"categories"` vs `"tenant:1:categories"` — giải thích tại sao cách đầu tiên là sai lầm nghiêm trọng trong hệ thống multi-tenant.

Cách đầu tiên là sai lầm nghiêm trọng vì nó không phân biệt 
được dữ liệu của các tenant khác nhau. Nếu cache key chỉ 
là `"categories"` thì khi một tenant truy cập vào cache, họ 
sẽ nhận được dữ liệu của tất cả các tenant khác,
10. Tenant A có 1 triệu bản ghi master data, Tenant B có 100 bản ghi. Cả hai dùng chung bảng với `tenant_id`. Hiệu suất query của Tenant B có bị ảnh hưởng không? Tại sao? Giải pháp?

Hiệu suất của tenant B sẽ bị ảnh hưởng do đáng lẽ B chỉ có 100 bản ghi
nhưng những query của B sẽ phải scan qua 1 triệu bản ghi của A 
để lọc ra 100 bản ghi của B, điều này sẽ làm chậm hiệu suất 
đáng kể.
Giải pháp được đề xuất là có chiến lược index phù hợp cho bản
ghi B để truy xuất nhanh hơn, hoặc có thể cân nhắc chuyển sang mô hình schema per tenant hoặc DB per tenant để cách ly dữ liệu tốt hơn.
11. Trong hệ thống kế toán, nếu dùng schema per tenant, khi có thay đổi schema (ví dụ: thêm cột mới), phải migrate bao nhiêu schema? Quy trình ra sao? Rủi ro gì?

Khi có thay đổi thì chỉ cần migrate 1 schema của bảng mà có thay
đổi. Quy trình sẽ là tạo file migration để thêm cột mới vào bảng đó, 
sau đó chạy file migration này cho schema tương ứng. Rủi ro là 
nếu có lỗi trong file migration thì sẽ ảnh hưởng đến tất cả 
các tenant, và việc rollback sẽ phức tạp hơn so với mô hình 
tenant_id column.
### Câu hỏi kiến trúc (Level 3)

12. Thiết kế cơ chế **tự động inject tenant_id** vào mọi query mà developer không cần tự thêm. Mô tả kiến trúc (interceptor, aspect, Hibernate filter, hoặc cách khác).

// Cần học thêm chưa biết làm
13. Một tenant yêu cầu **export toàn bộ dữ liệu** của họ (data portability). Với mỗi mô hình isolation, mô tả cách thực hiện và độ khó.
- Với mô hình tenant_id column, để export toàn bộ dữ liệu của một tenant, chúng ta có thể thực hiện một query với `WHERE tenant_id = ?` để lọc ra dữ liệu của tenant đó, sau đó xuất ra file CSV hoặc JSON. Độ khó là trung bình, vì cần phải đảm bảo rằng query được viết đúng và có hiệu suất tốt.
- Với mô hình schema per tenant, để export toàn bộ dữ liệu của một tenant, chúng ta có thể kết nối đến schema tương ứng với tenant đó và thực hiện export dữ liệu từ schema đó. Độ khó là thấp, vì mỗi tenant đã được cách ly hoàn toàn về dữ liệu.
- Với mô hình DB per tenant, để export toàn bộ dữ liệu của một tenant, chúng ta có thể kết nối đến database tương ứng với tenant đó và thực hiện export dữ liệu từ database đó. Độ khó là thấp, vì mỗi tenant đã được cách ly hoàn toàn về dữ liệu.
14. Thiết kế cơ chế cho phép **một user thuộc nhiều tenant** (ví dụ: kế toán freelancer làm cho 3 công ty). Data model và auth flow thay đổi thế nào?

Có thể tạo 1 db riêng cho những user này để quản lý
với các trường như user_id, tenant_id, role_in_tenant, v.v
Khi đó có thể tạo kiểu thêm 1 trường flag nào đó để check,
nếu ban đầu đi vào auth là true thì sẽ chuyển đến cái db riêng
đó để kiểm tra, sau đó sẽ được chọn một trong các tenant mà
user đó sẽ sử dụng trong session đó. Khi đó thì user đó sẽ 
giống như một user bình thường của tenant đó với
quyền đã có ở trong bảng, nhưng có thể chuyển đổi giữa các 
tenant mà họ thuộc về trong phiên làm việc của mình.