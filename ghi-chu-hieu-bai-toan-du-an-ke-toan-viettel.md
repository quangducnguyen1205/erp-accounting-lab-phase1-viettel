# Ghi chú hiểu bài toán dự án kế toán / ERP Viettel

**Người dùng:** Quang Đức Nguyễn  
**Mục đích tài liệu:** Ghi chú nền để hiểu nhanh bài toán dự án trong giai đoạn đầu thực tập, dựa trên các trao đổi, ảnh Figma, sơ đồ kiến trúc và định hướng từ mentor/đầu mối.  
**Tình trạng hiện tại:** Đây là bản note làm việc nội bộ, mang tính tổng hợp và suy luận có kiểm soát từ những gì đã được chia sẻ; **chưa phải tài liệu đặc tả chính thức của team**.

---

## 1. Bức tranh tổng quan: mình đang tham gia vào bài toán gì?

Dự án hiện tại nhiều khả năng là một **nền tảng kế toán / ERP cho doanh nghiệp**, được xây theo hướng **microservices**.

### Nhận định cốt lõi
- Khách hàng mục tiêu gần như chắc chắn là **doanh nghiệp**.
- Doanh nghiệp có các hoạt động nghiệp vụ như:
  - mua hàng
  - bán hàng
  - quản lý kho
  - quản lý thu chi / tiền gửi / công nợ
  - hóa đơn đầu vào / đầu ra
  - hạch toán kế toán
  - lương thưởng, tài sản, báo cáo
- Vì vậy sản phẩm không chỉ là một app CRUD nhập liệu, mà là **hệ thống ghi nhận và theo dõi các giao dịch tài chính - kế toán trong doanh nghiệp**.

### Khác biệt giữa “kế toán” và “kiểm toán”
Điều mình nhớ mang máng ban đầu theo hướng “kiểm toán” **không đúng bằng hướng “kế toán / ERP”**.

Lý do:
- Hệ thống có các module như:
  - General Ledger
  - Purchase
  - Sales
  - Treasury
  - Inventory
  - Assets
  - Payroll
- Đây là các module rất điển hình của **ERP / accounting system**.
- Một hệ thống kiểm toán thuần thường sẽ thiên về hồ sơ kiểm toán, bằng chứng, finding, workpaper, review, sign-off... hơn là các module nghiệp vụ vận hành như trên.

### Kết luận ngắn gọn
Đây nhiều khả năng là một **nền tảng ERP tài chính - kế toán đa tenant cho doanh nghiệp**.

---

## 2. Kiến trúc hệ thống đã ghi nhận được

Từ sơ đồ kiến trúc và phần mô tả được gửi, hệ thống hiện đang được định hình với các lớp chính sau.

### 2.1. Lớp giao diện và truy cập
- **Portal website (ReactJS)**: giao diện để khách hàng doanh nghiệp sử dụng.
- **API Gateway + Service discovery + Load balancer (Kong API Gateway)**: nhận request từ frontend, định tuyến tới backend, cân bằng tải.
- **Keycloak**: xác thực người dùng và dịch vụ.

### 2.2. Backend và dịch vụ nghiệp vụ
Mentor có nói theo hướng:
- **BE:** Spring Boot
- **FE:** React
- **DB:** PostgreSQL

Ngoài Java/Spring Boot, sơ đồ cũng cho thấy hệ thống có tính **polyglot**:
- Java cho nhiều business service chính
- .NET cho một số service chuyên biệt
- Python cho một số service hỗ trợ / file / AI / integration ở sơ đồ

### 2.3. Dữ liệu, cache, event, file
- **PostgreSQL**: dữ liệu nghiệp vụ
- **Redis**: cache
- **Kafka**: giao tiếp giữa các dịch vụ / đồng bộ dữ liệu
- **Debezium**: CDC - capture thay đổi dữ liệu để đồng bộ sang các service khác
- **MinIO**: lưu file

### 2.4. Monitoring / observability
- **Prometheus**
- **Grafana**
- **Loki**

### 2.5. Thành phần chức năng nền tảng
Các dịch vụ nền tảng được nhắc đến:
- Tenant
- Template
- MasterData
- Configuration
- Notification
- File service
- Schedule

### 2.6. Thành phần nghiệp vụ kế toán
Các dịch vụ nghiệp vụ được nhắc đến:
- General Ledger
- Purchase
- Sales
- Treasury
- Inventory
- Assets
- Payroll

### 2.7. Ý nghĩa thực tế của kiến trúc này
Kiến trúc như trên nói lên rằng:
- hệ thống được định hướng lớn ngay từ đầu
- không chỉ để nhập dữ liệu, mà còn để:
  - tách domain rõ ràng
  - dễ mở rộng theo nhiều tenant
  - tích hợp hệ thống ngoài
  - theo dõi log/metrics bài bản
  - hỗ trợ đồng bộ qua event / CDC

---

## 3. Một số khái niệm cần hiểu nền trước khi đi sâu

### 3.1. ERP là gì?
**ERP (Enterprise Resource Planning)** là hệ thống giúp doanh nghiệp quản lý nhiều quy trình nghiệp vụ trên cùng một nền tảng.

Ví dụ:
- mua hàng
- bán hàng
- kho
- kế toán
- tài sản
- nhân sự

Điểm quan trọng:
- dữ liệu không rời rạc từng phần
- các nghiệp vụ liên kết với nhau
- một giao dịch ở module này có thể tác động tới module khác

Ví dụ:
- mua hàng -> nhập kho -> công nợ nhà cung cấp -> hạch toán kế toán

### 3.2. PMKT là gì?
Ở ngữ cảnh trao đổi, “PMKT” có thể hiểu là **phần mềm kế toán** hoặc lớp sản phẩm giải quyết bài toán kế toán của doanh nghiệp.

Nó thường phải đáp ứng:
- ghi nhận chứng từ
- theo dõi hóa đơn
- theo dõi công nợ
- hạch toán nợ/có
- lên sổ, báo cáo, tổng hợp số liệu

### 3.3. Kế toán trong bài toán phần mềm nghĩa là gì?
Về góc nhìn hệ thống:
- kế toán không chỉ là “ghi số tiền”
- mà là ghi nhận **một sự kiện kinh tế** theo cấu trúc kế toán

Ví dụ:
- mua hàng nhập kho chưa thanh toán
- mua hàng và thanh toán ngay
- nhận hóa đơn sau
- phát sinh thuế GTGT
- ghi nhận công nợ phải trả

### 3.4. Hạch toán là gì?
Hạch toán là bước biến một giao dịch thực tế thành **bút toán kế toán**.

Ví dụ đơn giản:
- doanh nghiệp mua hàng nhập kho trị giá 10 triệu, chưa thanh toán
- có thể sinh logic kiểu:
  - Nợ hàng tồn kho
  - Có phải trả nhà cung cấp

Trong hệ thống, tab “Hạch toán” cho thấy phần mềm này đã đi sâu tới lớp kế toán thực sự.

### 3.5. Ghi sổ là gì?
“Ghi sổ” là bước xác nhận rằng chứng từ được đưa vào hệ thống kế toán chính thức.

Ý nghĩa:
- trước khi ghi sổ, chứng từ có thể mới ở mức nháp / nhập liệu
- sau khi ghi sổ, nó trở thành dữ liệu kế toán có hiệu lực hơn
- thường sẽ có các hạn chế sửa/xóa chặt hơn sau bước này

### 3.6. Công nợ phải trả là gì?
Khi doanh nghiệp mua hàng nhưng chưa thanh toán ngay, khoản tiền còn nợ nhà cung cấp được gọi là **công nợ phải trả**.

### 3.7. Tenant là gì?
Tenant hiểu đơn giản là một **khách hàng tổ chức / doanh nghiệp dùng hệ thống**.

Nếu hệ thống multi-tenant:
- doanh nghiệp A và doanh nghiệp B dùng chung nền tảng
- nhưng dữ liệu, cấu hình, quyền phải tách biệt

---

## 4. Nhận định về giai đoạn hiện tại của dự án

Qua trao đổi với mentor:
- hệ thống **mới đang xây dựng**
- tài liệu chi tiết chưa nhiều
- hiện mới có bản **kiến trúc tổng thể** và **Figma** khá sâu cho một số flow nghiệp vụ

Điều này dẫn tới kỳ vọng thực tế:
- chưa có đầy đủ docs như hệ thống lâu năm
- phải hiểu bài toán thông qua:
  - kiến trúc tổng thể
  - Figma / UI flow
  - giải thích từ mentor / team
  - code / repo khi được cấp quyền

### Ý nghĩa với bản thân ở giai đoạn đầu
Cách học nhanh nhất sẽ là:
1. hiểu bức tranh sản phẩm
2. hiểu flow người dùng
3. map flow sang bài toán backend
4. xác định dữ liệu / trạng thái / xử lý cần có

---

## 5. Module Figma đã review: MUA HÀNG -> Chứng từ mua hàng

Đây là module đã được review kỹ nhất tới thời điểm hiện tại.

### 5.1. Menu đã thấy
Trong page MUA HÀNG có các mục:
- Chứng từ mua hàng
- Mua dịch vụ
- Hóa đơn mua
- Trả lại hàng mua

Trọng tâm hiện tại là **Chứng từ mua hàng**.

### 5.2. Hai lớp màn hình chính
Module này hiện thấy rõ hai nhóm chính:
- **Danh sách chứng từ mua hàng**
- **Form / popup thêm mới / xem chi tiết chứng từ mua hàng**

### 5.3. Ý nghĩa tổng quát của “chứng từ mua hàng”
Một chứng từ mua hàng trong hệ thống này không phải chỉ là “phiếu nhập hàng” đơn giản.

Nó gần như là một **hồ sơ nghiệp vụ mua hàng tổng hợp**, bao gồm:
- thông tin chung giao dịch
- nhà cung cấp
- chi tiết hàng hóa
- chi phí phát sinh
- thuế
- hạch toán
- tham chiếu chứng từ khác
- trạng thái hóa đơn
- trạng thái thanh toán
- trạng thái chứng từ / ghi sổ

Nói kiểu backend hơn: nó giống một **aggregate lớn của domain Purchase**.

---

## 6. Review nhanh các màn hình / form đã thấy

## 6.1. Danh sách chứng từ mua hàng

### Nó là gì?
Là màn hình quản lý toàn bộ các chứng từ mua hàng đã phát sinh trong hệ thống.

### Tại sao cần?
Vì doanh nghiệp cần:
- tra cứu giao dịch mua hàng
- biết chứng từ nào đã có hóa đơn
- biết chứng từ nào đã thanh toán
- biết chứng từ nào đã ghi sổ
- mở chi tiết / xử lý tiếp từng chứng từ

### Các cột chính đã thấy
- Ngày hạch toán
- Số chứng từ
- Số hóa đơn
- Nhà cung cấp
- Tổng tiền thanh toán
- Chi phí mua hàng
- Giá trị nhập kho
- Trạng thái hóa đơn
- Trạng thái thanh toán
- Trạng thái chứng từ
- Chức năng

### Ý nghĩa nghiệp vụ
Màn list này phản ánh ít nhất 3 lớp trạng thái song song:
- trạng thái hóa đơn
- trạng thái thanh toán
- trạng thái chứng từ / ghi sổ

=> Đây là màn điều phối nghiệp vụ, không chỉ là list hiển thị record.

---

## 6.2. Form thêm mới chứng từ mua hàng

### Nó là gì?
Màn tạo một nghiệp vụ mua hàng mới.

### Tại sao cần?
Vì người dùng phải bắt đầu một giao dịch mua hàng từ một điểm vào thống nhất.

### Những trường / công tắc đầu form đã thấy
- Loại nghiệp vụ: Mua hàng nhập kho
- Hình thức mua hàng: Trong nước / Nhập khẩu
- Đã có hóa đơn
- Nhiều hóa đơn
- Thanh toán ngay
- Hình thức thanh toán / Ủy nhiệm chi

### Ý nghĩa ngắn gọn
Đây là **header + bộ điều khiển flow nghiệp vụ**.

Các công tắc này quyết định:
- flow nào sẽ xảy ra
- tab nào có ý nghĩa
- validation nào cần áp dụng

---

## 6.3. Tab Chi tiết hàng hóa

### Nó là gì?
Nơi nhập các dòng hàng hóa được mua.

### Đã thấy các field kiểu:
- Mã hàng hóa
- Tên hàng hóa
- TK Nợ
- TK Có
- ĐVT
- Kho
- Ngày hóa đơn
- Số hóa đơn
- Mẫu số HĐ
- Ký hiệu HĐ
- Số lượng
- Đơn giá
- Thành tiền
- Thuế suất GTGT
- Tiền thuế GTGT
- TK thuế GTGT
- các cột mở rộng khác

### Tại sao cần?
Vì chứng từ mua hàng phải biết rõ:
- mua cái gì
- số lượng bao nhiêu
- nhập kho nào
- trị giá bao nhiêu
- gắn với tài khoản gì

### Hiểu ngắn gọn
Đây là **ruột của chứng từ**.

---

## 6.4. Tab Chi phí

### Nó là gì?
Nơi nhập các chi phí phát sinh ngoài tiền hàng.

### Ví dụ
- vận chuyển
- lưu kho
- bốc dỡ
- phí mua hàng khác

### Tại sao cần?
Vì trong mua hàng thực tế, doanh nghiệp không chỉ trả tiền hàng.

### Hiểu ngắn gọn
Tab này tách:
- tiền hàng
- chi phí mua hàng

để sau đó hệ thống có thể:
- phân bổ chi phí
- cộng vào giá trị nhập kho
- hạch toán đúng

---

## 6.5. Popup Phân bổ chi phí mua hàng

### Nó là gì?
Popup chia chi phí mua hàng vào các dòng hàng hóa.

### Các mode đã thấy
- theo số lượng
- theo giá trị
- tự nhập

### Tại sao cần?
Nếu một chứng từ có nhiều mặt hàng, chi phí vận chuyển / lưu kho không thể để treo chung mãi, mà phải phân xuống từng dòng.

### Hiểu ngắn gọn
Đây là chỗ quyết định **chi phí được chia vào hàng như thế nào**.

### Ý nghĩa backend lớn
Phần này nhiều khả năng là một điểm xử lý khó:
- lưu allocation rule
- lưu allocation result theo từng item
- recalc giá trị nhập kho
- đảm bảo tổng chi phí phân bổ khớp tổng chi phí gốc

---

## 6.6. Tab Thuế

### Nó là gì?
Nơi tổng hợp dữ liệu thuế đầu vào của giao dịch mua.

### Đã thấy các cột kiểu:
- Nhóm HHDV mua vào
- Thuế suất GTGT
- Giá trị tính thuế
- Thuế GTGT
- TK thuế

### Tại sao cần?
Vì giao dịch mua hàng trong doanh nghiệp phải theo dõi cả lớp thuế, không chỉ giá hàng.

### Hiểu ngắn gọn
Đây là lớp “thuế đầu vào” của giao dịch mua.

---

## 6.7. Tab Hạch toán

### Nó là gì?
Nơi thể hiện / sinh ra các bút toán kế toán từ chứng từ mua hàng.

### Đã thấy các field:
- Tài khoản nợ
- Tài khoản có
- Số tiền
- Quy đổi (VND)
- Loại tiền
- Tỷ giá

### Tại sao cần?
Vì hệ thống không dừng ở ghi nhận mua hàng, mà phải chuyển sang ngôn ngữ kế toán.

### Hiểu ngắn gọn
Đây là cầu nối giữa **nghiệp vụ mua hàng** và **sổ cái / kế toán**.

---

## 6.8. Tab Tham chiếu

### Nó là gì?
Nơi liên kết chứng từ hiện tại với các chứng từ khác.

### Đã thấy các cột kiểu:
- Ngày chứng từ
- Số chứng từ
- Loại chứng từ
- Diễn giải
- Tổng thanh toán
- Trạng thái

### Tại sao cần?
Vì một giao dịch mua hàng có thể liên quan tới:
- phiếu chi
- hóa đơn
- chứng từ khác trong cùng flow

### Hiểu ngắn gọn
Đây là phần “liên kết hồ sơ” của chứng từ.

---

## 6.9. Phần Hóa đơn

### Nó là gì?
Lớp dữ liệu về hóa đơn đầu vào gắn với chứng từ mua hàng.

### Các case đã thấy
- đã có hóa đơn
- 1 hóa đơn
- nhiều hóa đơn

### Tại sao cần?
Vì thực tế có nhiều trường hợp:
- hàng về trước, hóa đơn về sau
- một chứng từ mua hàng gắn nhiều hóa đơn

### Hiểu ngắn gọn
Hóa đơn là **một phần của flow mua hàng**, không phải toàn bộ flow.

---

## 6.10. Phần Thanh toán ngay

### Nó là gì?
Nhánh flow khi doanh nghiệp trả tiền ngay lúc mua.

### Đã thấy các biến thể
- séc / chuyển khoản / UNC
- séc tiền mặt
- phiếu chi

### Tại sao cần?
Không phải giao dịch nào cũng treo công nợ; có giao dịch trả ngay.

### Hiểu ngắn gọn
Đây là nhánh **mua + thanh toán trong cùng flow**.

---

## 6.11. Chứng từ ghi nợ / không thanh toán ngay

### Nó là gì?
Trường hợp mua hàng nhưng chưa thanh toán ngay.

### Tại sao cần?
Đây là case rất phổ biến của doanh nghiệp: đã nhận hàng nhưng để công nợ nhà cung cấp.

### Hiểu ngắn gọn
Đây là nhánh **mua trước, trả tiền sau**.

---

## 6.12. Màn xem chi tiết chứng từ

### Nó là gì?
Màn xem toàn bộ chứng từ sau khi đã tạo.

### Đã thấy các action cuối màn hình
- Đóng
- In
- Sửa
- Ghi sổ
- Thanh toán
- Nhận hóa đơn

### Tại sao cần?
Sau khi tạo xong, người dùng vẫn phải tiếp tục quản lý vòng đời chứng từ.

### Hiểu ngắn gọn
Nếu form tạo mới là nơi “nhập”, thì màn này là nơi “vận hành chứng từ”.

---

## 6.13. Biến thể nhập khẩu

### Nó là gì?
Flow chứng từ mua hàng dành cho mua hàng nhập khẩu.

### Đã thấy thêm các lớp thuế / cột mở rộng
- thuế nhập khẩu
- thuế TTĐB
- thuế BVMT
- các chỉ tiêu trị giá tính thuế khác

### Tại sao cần?
Mua hàng nhập khẩu phức tạp hơn trong nước.

### Hiểu ngắn gọn
Đây là nhánh nâng cao hơn với nhiều rule tài chính / thuế hơn.

---

## 6.14. Chiết khấu

### Nó là gì?
Cơ chế giảm trừ giá trị giao dịch.

### Tại sao cần?
Doanh nghiệp có thể được giảm giá theo hóa đơn / tổng đơn hàng.

### Hiểu ngắn gọn
Chiết khấu làm thay đổi:
- tổng tiền hàng
- thuế
- tổng thanh toán
- giá trị nhập kho
- hạch toán

---

## 6.15. Đơn vị tính quy đổi

### Nó là gì?
Cơ chế nhập theo đơn vị mua bán thực tế nhưng quy đổi về đơn vị quản lý chuẩn.

### Ví dụ
- mua theo hộp, quản lý theo cái
- mua theo kiện, quản lý theo chiếc

### Hiểu ngắn gọn
Đây là cầu nối giữa **đơn vị giao dịch thực tế** và **đơn vị quản lý nội bộ**.

---

## 7. Cách nên đọc toàn bộ flow Figma

Không nên đọc theo thứ tự chụp ảnh, mà nên đọc theo thứ tự nghiệp vụ.

### Thứ tự đọc đề xuất
1. **Danh sách chứng từ mua hàng**
2. **Danh sách sau khi chọn bản ghi**
3. **Form thêm mới chứng từ mua hàng - form chính**
4. **Các biến thể đầu form**
   - trong nước / nhập khẩu
   - đã có hóa đơn / chưa có hóa đơn
   - 1 hóa đơn / nhiều hóa đơn
   - thanh toán ngay / chưa thanh toán
5. **Tab Chi tiết hàng hóa**
6. **Tab Chi phí**
7. **Popup phân bổ chi phí**
8. **Tab Thuế**
9. **Tab Hạch toán**
10. **Tab Tham chiếu**
11. **Màn xem chi tiết / action sau tạo chứng từ**
12. **Biến thể nhập khẩu / chiết khấu / đơn vị tính quy đổi**

### Lý do
Flow này phản ánh đúng cách người dùng vận hành nghiệp vụ từ ngoài vào trong, từ tạo mới tới xử lý sâu.

---

## 8. Flow người dùng tổng quát của module Chứng từ mua hàng

### Flow khung
1. Vào Danh sách chứng từ mua hàng
2. Bấm Thêm mới
3. Chọn kịch bản nghiệp vụ
   - trong nước / nhập khẩu
   - đã có hóa đơn / chưa có hóa đơn
   - 1 hóa đơn / nhiều hóa đơn
   - thanh toán ngay / chưa thanh toán
4. Nhập thông tin chung chứng từ
5. Nhập chi tiết hàng hóa
6. Nhập chi phí nếu có
7. Phân bổ chi phí nếu cần
8. Kiểm tra thuế
9. Kiểm tra hạch toán
10. Gắn tham chiếu nếu có
11. Lưu nháp hoặc Lưu và ghi sổ
12. Sau đó tiếp tục:
   - sửa
   - nhận hóa đơn
   - thanh toán
   - in
   - xem lại chi tiết

### Ý nghĩa
Đây là flow gốc để hiểu nhanh nhất toàn bộ bài toán.

---

## 9. Các tình huống nghiệp vụ thực tế đã phân tích

## 9.1. Case 1 - Mua hàng trong nước, có hàng, có 1 hóa đơn, chưa thanh toán ngay

### Mô tả
Doanh nghiệp mua hàng trong nước, nhập kho, đã có 1 hóa đơn, nhưng chưa thanh toán ngay.

### Ý nghĩa nghiệp vụ
Đây là case cơ bản nhất.

### Backend cần xử lý
- tạo header chứng từ mua hàng
- tạo các dòng hàng hóa
- lưu dữ liệu hóa đơn
- tính tổng tiền, thuế, tổng thanh toán
- sinh bút toán kế toán
- đặt trạng thái hóa đơn = đã có
- đặt trạng thái thanh toán = chưa thanh toán
- đặt trạng thái chứng từ tùy action ghi sổ

### Hiểu ngắn gọn
Đây là case **mua hàng + ghi nhận công nợ phải trả**.

---

## 9.2. Case 2 - Mua hàng trong nước, có 1 hóa đơn, thanh toán ngay

### Mô tả
Doanh nghiệp mua hàng và trả tiền luôn.

### Backend cần xử lý thêm so với case 1
- tạo chứng từ thanh toán đi kèm
- liên kết chứng từ mua hàng với chứng từ thanh toán
- cập nhật trạng thái thanh toán = đã thanh toán
- xử lý transaction / idempotency cẩn thận để tránh tạo trùng

### Hiểu ngắn gọn
Đây là case **mua và trả tiền ngay**.

---

## 9.3. Case 3 - Hàng đã nhập kho nhưng chưa có hóa đơn

### Mô tả
Hàng đã về, nhưng hóa đơn chưa nhận được.

### Backend cần xử lý
- cho phép tạo purchase voucher mà chưa có invoice data đầy đủ
- trạng thái hóa đơn = chưa có hóa đơn
- hỗ trợ flow cập nhật hóa đơn sau này
- cần audit trail vì chứng từ được hoàn thiện dần

### Hiểu ngắn gọn
Đây là case **hàng về trước, hóa đơn về sau**.

---

## 9.4. Case 4 - Một chứng từ gắn nhiều hóa đơn

### Mô tả
Một giao dịch mua hàng liên quan đến nhiều hóa đơn.

### Backend cần xử lý
- support quan hệ one-to-many giữa voucher và invoice
- mapping invoice với dòng hàng / dữ liệu thuế
- tổng hợp và đối soát tổng invoice với tổng chứng từ

### Hiểu ngắn gọn
Đây là case **một giao dịch nhưng lớp hóa đơn bị tách nhiều phần**.

---

## 9.5. Case 5 - Có chi phí mua hàng cần phân bổ

### Mô tả
Ngoài tiền hàng, phát sinh chi phí như vận chuyển, lưu kho...

### Backend cần xử lý
- lưu expense lines
- lưu allocation rule
- lưu allocation result cho từng item
- recalc giá trị nhập kho
- cập nhật hạch toán tương ứng

### Điểm dễ khó
- tổng chi phí phân bổ phải khớp
- sửa quantity / item sau khi phân bổ có thể bắt buộc recalc

### Hiểu ngắn gọn
Đây là case **tiền ngoài tiền hàng nhưng vẫn phải chia đúng vào từng món**.

---

## 9.6. Case 6 - Có chiết khấu

### Backend cần xử lý
- xác định loại chiết khấu
- tính lại tổng tiền và có thể cả thuế
- cập nhật giá trị nhập kho / công nợ / thanh toán / hạch toán

### Hiểu ngắn gọn
Chiết khấu là phần điều chỉnh giá trị giao dịch.

---

## 9.7. Case 7 - Mua hàng nhập khẩu

### Backend cần xử lý
- thêm các field đặc thù nhập khẩu
- tính các loại thuế liên quan
- hỗ trợ quy đổi / tỷ giá nếu có ngoại tệ
- sinh hạch toán phức tạp hơn case trong nước

### Hiểu ngắn gọn
Đây là nhánh nâng cao hơn của flow mua hàng.

---

## 9.8. Case 8 - Ghi sổ

### Ý nghĩa
Bước xác nhận chứng từ đi vào hệ thống kế toán chính thức.

### Backend cần xử lý
- validate toàn bộ chứng từ
- sinh bút toán kế toán chính thức
- idempotency khi post
- cập nhật trạng thái đã ghi sổ
- giới hạn sửa/xóa sau khi ghi sổ

### Hiểu ngắn gọn
Đây là bước chuyển từ **dữ liệu nhập liệu** sang **dữ liệu kế toán chính thức**.

---

## 9.9. Case 9 - Vòng đời sau khi chứng từ đã tồn tại

### Các hành động có thể có
- xem chi tiết
- sửa
- in
- ghi sổ
- thanh toán
- nhận hóa đơn

### Backend cần xử lý
- load full aggregate
- permission theo trạng thái
- chỉ cho phép action hợp lệ
- có audit log cho các thay đổi quan trọng

### Hiểu ngắn gọn
Đây là vòng đời tiếp theo sau khi chứng từ đã được tạo.

---

## 10. Từ góc nhìn backend: mình có thể sẽ phải nghĩ theo hướng nào?

Mặc dù chưa có repo chính thức trong tay, nhưng từ Figma và flow hiện tại có thể dự đoán rằng backend sẽ phải giải quyết ít nhất các lớp vấn đề sau.

### 10.1. Dữ liệu / aggregate
Một chứng từ mua hàng nhiều khả năng không chỉ là 1 bảng.

Có thể sẽ gồm:
- voucher header
- voucher items
- expense lines
- expense allocations
- invoice data
- tax data / tax summary
- accounting entries
- references
- status history / audit

### 10.2. Validation theo nhánh nghiệp vụ
Ví dụ:
- tick “Đã có hóa đơn” thì invoice fields trở thành bắt buộc
- tick “Thanh toán ngay” thì payment fields phải hợp lệ
- chọn “Nhiều hóa đơn” thì mô hình dữ liệu / validate đổi theo
- chọn “Nhập khẩu” thì thêm nhiều rule mới

### 10.3. Tính toán
Backend rất có thể phải tính:
- tổng tiền hàng
- chiết khấu
- chi phí
- thuế
- tổng thanh toán
- giá trị nhập kho
- số tiền quy đổi VND

### 10.4. Trạng thái
Ít nhất sẽ có các nhóm trạng thái:
- trạng thái hóa đơn
- trạng thái thanh toán
- trạng thái chứng từ / ghi sổ

### 10.5. Transaction và idempotency
Các action như:
- lưu và ghi sổ
- tạo chứng từ + tạo thanh toán cùng lúc
- nhận hóa đơn sau

đều dễ phát sinh vấn đề nếu backend không xử lý transaction / chống tạo trùng tốt.

### 10.6. Event / sync liên service
Vì hệ thống microservices có Kafka, Debezium:
- một thay đổi ở purchase có thể cần đồng bộ sang service khác
- trạng thái / report / notification có thể không chỉ nằm trong 1 service

---

## 11. Cách nên học và hiểu nhanh bài toán trong giai đoạn đầu

### 11.1. Cách đi đúng
Không cố nhảy ngay vào code khi chưa hiểu domain.

Thứ tự nên là:
1. hiểu sản phẩm đang giải quyết gì
2. hiểu flow người dùng
3. hiểu từng tình huống nghiệp vụ thực tế
4. map từng tình huống sang dữ liệu / trạng thái / xử lý backend
5. sau đó mới map vào service / code / DB / API

### 11.2. 5 câu hỏi nên dùng để phân tích mọi màn hình / flow
1. Người dùng đang làm gì?
2. Hệ thống đang ghi nhận sự kiện gì?
3. Có các nhánh nào?
4. Backend phải lưu cái gì?
5. Backend dễ vỡ ở đâu?

### 11.3. 3 trục nên giữ trong đầu với module Purchase
**Trục 1 - Header / Flow switch**
- trong nước / nhập khẩu
- đã có hóa đơn / chưa có
- 1 hóa đơn / nhiều hóa đơn
- thanh toán ngay / chưa thanh toán

**Trục 2 - Detail tabs**
- hàng hóa
- chi phí
- thuế
- hạch toán
- tham chiếu

**Trục 3 - Lifecycle**
- lưu nháp
- ghi sổ
- thanh toán
- nhận hóa đơn
- xem / in / sửa

---

## 12. Những câu hỏi nên xác nhận thêm với mentor / team trong thời gian tới

### 12.1. Về sản phẩm
- khách hàng mục tiêu hiện tại là nhóm doanh nghiệp nào?
- sản phẩm đang ưu tiên build module nào trước?
- flow chuẩn của “chứng từ mua hàng” ngoài thực tế bên team đang hiểu như thế nào?

### 12.2. Về nghiệp vụ
- chứng từ mua hàng là aggregate trung tâm hay còn bị tách thêm bởi service khác?
- “ghi sổ” hiện đang được hiểu theo rule nào?
- nhận hóa đơn sau có làm recalc / re-post không?
- chi phí mua hàng khi phân bổ sẽ cộng vào inventory value theo rule nào?
- payment flow thực tế đang đi cùng purchase hay qua service khác?

### 12.3. Về kỹ thuật
- service nào hiện đang phụ trách module Purchase?
- local setup chuẩn gồm những gì?
- schema DB / migration đang do team nào giữ?
- purchase và general ledger sync qua API hay Kafka / Debezium?
- Keycloak hiện dùng ở mức auth thôi hay có kéo role/claim xuống service? 

### 12.4. Về task cho bản thân
- 1-2 tuần đầu mình nên ưu tiên đọc gì?
- service / module nào mình sẽ follow?
- task đầu tiên thường ở mức đọc code, setup, fix bug hay thêm API nhỏ?

---

## 13. Các nhận định tạm thời cần nhớ rõ là “suy luận”, chưa phải kết luận chính thức

Các điểm sau hiện là nhận định khá mạnh, nhưng vẫn cần xác nhận chính thức từ team:
- đây là hệ thống kế toán / ERP đa tenant cho doanh nghiệp
- chứng từ mua hàng là một aggregate nghiệp vụ lớn
- flow mua hàng đang bao luôn các lớp hóa đơn / thanh toán / công nợ / hạch toán
- backend nhiều khả năng phải xử lý validation theo nhiều nhánh và state transitions phức tạp

Các điểm này hiện **rất hợp lý theo Figma + kiến trúc**, nhưng vẫn nên xem là **working hypothesis** cho tới khi có thêm:
- repo
- docs
- BA spec
- giải thích chính thức từ mentor / team

---

## 14. Tóm tắt ngắn nhất để tự nhẩm lại

### Dự án là gì?
Một nền tảng kế toán / ERP cho doanh nghiệp, xây theo microservices.

### Module đã hiểu sâu nhất là gì?
Module **MUA HÀNG -> Chứng từ mua hàng**.

### Chứng từ mua hàng là gì?
Một hồ sơ nghiệp vụ mua hàng tổng hợp, gồm:
- header
- hàng hóa
- chi phí
- thuế
- hạch toán
- tham chiếu
- trạng thái hóa đơn / thanh toán / ghi sổ

### Hiểu flow nhanh nhất bằng cách nào?
Đi từ **flow người dùng** và **tình huống thực tế**, rồi hỏi backend phải làm gì ở mỗi bước.

### Mục tiêu học giai đoạn đầu là gì?
Hiểu:
- sản phẩm giải quyết bài toán gì
- flow mua hàng vận hành ra sao
- dữ liệu / trạng thái / xử lý nào là cốt lõi
- mình sẽ đứng ở đâu trong hệ thống

---

## 15. Hướng cập nhật tài liệu này về sau

Khi có thêm thông tin mới, nên tiếp tục bổ sung theo các mục sau:
- repo / service nào đang phụ trách Purchase
- entity / table / API thực tế
- rule nghiệp vụ do mentor giải thích
- tài liệu chính thức về kế toán / ERP nội bộ
- các module khác ngoài Purchase
- timeline và task đầu tiên được giao

Mục tiêu là biến tài liệu này thành một **living note** về dự án trong giai đoạn đầu thực tập.

