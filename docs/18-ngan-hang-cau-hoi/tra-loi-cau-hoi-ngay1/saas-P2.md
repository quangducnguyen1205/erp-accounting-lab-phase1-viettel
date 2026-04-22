# ❓ Trả lời câu hỏi tự học — SaaS & Multi-tenant

> ⚠️** Tự trả lời trước, sau đó kiểm tra ở [19-dap-an/](../19-dap-an/).

---

## SaaS

### Câu hỏi suy luận (Level 2)

1. So sánh SaaS và On-Premise từ góc nhìn của **nhà phát triển phần mềm** (không phải góc nhìn khách hàng). Phía nào phức tạp hơn và tại sao?

Dưới góc nhìn của nhà phát triển phần mềm thì có thể thấy mô hình phân phối phần mềm
SaaS so với on-premise mỗi cái sẽ có ưu nhược điểm cũng như sự phức
tạp khác nhau tuỳ trường hợp. 
- Với SaaS sẽ có 2 cách triển khai kiến trúc tổ chức hệ thống 
đó là signle-tenant và multi-tenant. Với kiến trúc single-tenant
thì nhà phát triển sẽ host từng app trên 1 server riêng như một 
instance, và mỗi tenant sẽ được sử dụng với một instance. Do đó
với bất kỳ thay đổi gì như cập nhật version, thêm tính năng mới
thì việc đồng bộ sẽ phức tạp hơn. Tuy nhiên sẽ có lợi thế về việc cách ly
các tenant một cách tự nhiên. Với kiến trúc multi-tenant, thì
toàn bộ tenant sẽ được phục vụ trên cùng một nền tảng, do nhà phát triển
quản lý, vận hành. Do đó khi có sự thay đổi nào chỉ cần 1 deploy là
sẽ được cập nhật đồng bộ tất cả, nhưng nếu có lỗi xảy ra thì cũng sẽ 
ảnh hưởng đến toàn bộ tenant. Ngoài ra sẽ phức tạp hơn rất nhiều ở việc
triển khai sao cho cách ly được các tenant với nhau ở nhiều cấp độ cũng
như nhiều phần như phần quyền, cung cấp tính năng, config...
- Với on-premise thì mô hình này mỗi tenant sẽ được quản lý, vận hành riêng.
Khá giống với kiến trúc thiết kế hệ thống kiểu single-tenant của SaaS, 
nhưng sẽ phức tạp hơn rất nhiều ở việc cập nhật, đồng bộ khi có thay 
đổi. Cũng như phải customize sâu cho từng tenant riêng, nên cũng sẽ
phức tạp hơn ở mức phía yêu cầu của tenant. Và rất kho để có thể scale
mở rộng người dùng như mô hình SaaS 
2. Nếu một khách hàng doanh nghiệp lớn yêu cầu dữ liệu kế toán **phải nằm trên server riêng** (data residency requirement), kiến trúc SaaS cần điều chỉnh thế nào? Có mâu thuẫn với bản chất SaaS không?

Nếu yêu cầu phải nằm trên server riêng thì có 2 tình huống:
- Nếu server là của riêng doanh nghiệp đó, thì đó là mô hình
on-premise rồi, không còn là SaaS nữa
- Nếu server vẫn là của SaaS thì riêng với doanh nghiệp lớn
sẽ là mô hình single-tenant

Do đó mô hình SaaS có thể điều chỉnh thành mô hình hybrid, tức là
vẫn sẽ giữ mô hình SaaS với các đối tượng công ty SME, còn với các doanh
nghiệp lớn thì sử dụng on-premise hoặc là single-tenant.
3. Tại sao SaaS thường dùng mô hình **subscription** (trả phí theo tháng) thay vì **license** (mua đứt)? Lợi ích kỹ thuật và kinh doanh là gì?

Vì SaaS thường dùng mô hình multi-tenant, và mỗi tenant sẽ có nhu
cầu cũng như mức độ sử dụng khác nhau, nên mô hình subscription sẽ 
linh hoạt hơn rất nhiều so với license.
- Với subscription, khách hàng có thể chọn gói dịch vụ
phù hợp với nhu cầu của họ và có thể thay đổi gói dịch vụ
khi cần thiết. Về mặt kỹ thuật, mô hình subscription cũng giúp 
nhà cung cấp dễ dàng quản lý và cập nhật sản phẩm cho nhiều khách
hàng cùng lúc, ví dụ như khi có một thay đổi gì đó áp dụng
cho 1 subcription nào đó chẳng hạn. Cũng như với các doanh nghiệp
cỡ SME thì chi phí ban đầu sẽ rẻ hơn, dễ tiếp cận hơn không cần
hạ tầng mà có thể sử dụng được luôn
- Trong khi đó, với license, khách hàng phải mua đứt một gói 
dịch vụ ngay từ đầu. 

Và việc sử dụng subscription cũng giúp nhà cung cấp có doanh 
thu ổn định, tiếp cận đến nhiều doanh nghiệp SME hơn
4. Khi deploy **một bản cập nhật** cho SaaS platform, nếu bản cập nhật gây lỗi, ảnh hưởng bao nhiêu khách hàng? So sánh với on-premise khi cùng tình huống.

Nếu bản cập nhật gây lỗi khi dùng SaaS platform thì tuỳ cách
triển khai của mô hình sẽ ảnh hưởng đến tập khách hàng có 
sử dụng đến tính năng đó và trong cách triển khai đó. Còn với
on-premise thì chỉ ảnh hưởng đến duy nhất khách hàng đó.
Tuy nhiên đánh đổi lại với mô hình SaaS thì việc cập nhật, sửa lỗi
cũng rất nhanh chỉ trong 1 lần deploy, còn với on-premise thì
sẽ phải sửa lỗi với từng khách hàng riêng sẽ chậm và khó khăn
hơn.

### Câu hỏi kiến trúc (Level 3)

5. Thiết kế một cơ chế **feature flags** cho SaaS platform sao cho: một số tenant được dùng tính năng mới, một số chưa. Mô tả data model và logic xử lý.

Tôi sẽ thiết kế phần config sao cho có một trường flags gì đó
để xem xét xem những tenant nào được sử dụng tính năng mới. Ví dụ
chia theo subscription chẳng hạn. Và trong các tính năng mới đó,
những tính năng nào phục vụ cho subscription nào thì sẽ được bật lên. 
Hoặc có thể thiết kế một bảng riêng để quản lý các feature flags này, 
với các trường như tenant_id, feature_name, is_enabled. Khi xử lý logic, 
trước khi thực hiện một tính năng mới nào đó, hệ thống sẽ kiểm tra bảng 
này để xác định xem tenant đó có được phép sử dụng tính năng mới hay không.
6. Đề xuất chiến lược **zero-downtime deployment** cho SaaS platform có 100 tenants đang active. Cần xem xét những gì?

Tuỳ trường hợp mà có thể có nhiều chiến lược khác nhau, nhưng tôi sẽ đề xuất một chiến lược
- Với trường hợp là sửa db, thì cần migration cẩn thận để sao cho 
db mới không làm ảnh hưởng đến hệ thống đang chạy, ví dụ có thêm 1
trường dữ liệu gì đó, thì phải thiết kế sao cho thêm nó thì db cũ
không lỗi, sau đó dần dần đẩy toàn bộ output về giống nhau ở cả cũ
và mới, sau đó xoá dần cái cũ đi khi đã thật sự verify ổn
- Với trường hợp là thêm tính năng mới hoặc cập nhật gì mới, thì có thể sử dụng cơ chế
blue-green deployment, tức là có 2 môi trường chạy song song, một 
môi trường đang chạy với phiên bản cũ, một môi trường chạy với 
phiên bản mới. Khi đã verify ổn định ở môi trường mới thì sẽ 
chuyển traffic từ môi trường cũ sang môi trường mới, sau đó tắt 
môi trường cũ đi.
- Hoặc một cách khác đó là cho chạy song song cả 2 tính năng,
nhưng sẽ thông báo tính năng cũ sẽ bị deprecate, và khuyến 
khích khách hàng chuyển sang tính năng mới, sau đó dần dần 
sẽ xoá tính năng cũ đi khi đã verify ổn định ở tính năng mới