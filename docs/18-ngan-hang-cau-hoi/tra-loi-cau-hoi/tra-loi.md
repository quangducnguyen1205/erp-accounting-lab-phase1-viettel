# ❓ Trả lời câu hỏi tự học — SaaS & Multi-tenant

> ⚠️** Tự trả lời trước, sau đó kiểm tra ở [19-dap-an/](../19-dap-an/).

---

## SaaS

### Câu hỏi suy luận (Level 2)

1. So sánh SaaS và On-Premise từ góc nhìn của **nhà phát triển phần mềm** (không phải góc nhìn khách hàng). Phía nào phức tạp hơn và tại sao?

Dưới góc nhìn của nhà phát triển phần mềm thì có thể thấy kiến trúc
SaaS phức tạp hơn nhiều so với on-premise. Lý do là vì SaaS phải phục 
vụ nhiều khách hàng (tenant) trên cùng một nền tảng, trong khi on-premise 
chỉ cần phục vụ một khách hàng duy nhất trên server riêng của họ.
Do đó cần phải redesign kiến trúc để đảm bảo xác thực giữa các tenant,
phân quyền giữa các user trong cùng tenant, cách ly dữ liệu
giữa các tenant, rồi còn cần config cho từng nhu cầu của từng
tenant khác nhau, quản lý cập nhật thiết kế sao cho khách hàng
này không ảnh hưởng đến khách hàng khác v.v
2. Nếu một khách hàng doanh nghiệp lớn yêu cầu dữ liệu kế toán **phải nằm trên server riêng** (data residency requirement), kiến trúc SaaS cần điều chỉnh thế nào? Có mâu thuẫn với bản chất SaaS không?

Có thể để dữ liệu kế toán của doanh nghiệp này ở 1 db riêng,
có thể thiết kế ở phần config để cho phép tenant này chọn db riêng, 
còn các tenant khác dùng db chung. Tương ứng với chi phí bỏ ra cho 
tenant này sẽ nhiều hơn rất nhiều. Nó vẫn không mâu thuẫn với 
bản chất SaaS vì vẫn đảm bảo các tính chất của SaaS.
3. Tại sao SaaS thường dùng mô hình **subscription** (trả phí theo tháng) thay vì **license** (mua đứt)? Lợi ích kỹ thuật và kinh doanh là gì?

Vì đáp ứng cho nhiều tenant, mỗi tenant lại có những nhu cầu
sử dụng khác nhau, nên mô hình subscription sẽ linh hoạt hơn rất nhiều so với license. 
Với subscription, khách hàng có thể chọn gói dịch vụ 
phù hợp với nhu cầu của họ và có thể thay đổi gói dịch vụ 
khi cần thiết. Trong khi đó, với license, khách hàng phải 
mua đứt một gói dịch vụ và không thể thay đổi được. 
Về mặt kỹ thuật, mô hình subscription cũng giúp nhà cung 
cấp dễ dàng quản lý và cập nhật sản phẩm cho nhiều khách 
hàng cùng lúc, trong khi với license thì việc cập nhật 
sẽ phức tạp hơn rất nhiều. Và việc sử dụng subscription 
cũng giúp nhà cung cấp có doanh thu ổn định hơn, và tiếp
cận đến nhiều doanh nghiệp SME hơn
4. Khi deploy **một bản cập nhật** cho SaaS platform, nếu bản cập nhật gây lỗi, ảnh hưởng bao nhiêu khách hàng? So sánh với on-premise khi cùng tình huống.

Nếu bản cập nhật gây lỗi khi dùng SaaS platform thì sẽ ảnh hưởng đến
toàn bộ khách hàng có sử dụng đến tính năng đó. Còn với 
on-premise thì chỉ ảnh hưởng đến duy nhất khách hàng đó,
cho nên ở SaaS khi deploy cần phải có các quy trình rất 
cẩn thận.