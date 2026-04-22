# 📝 Review ghi chú tự học SaaS — Phần 4-6

---

# Part 4 — Nhật ký lỗi nhận thức

## Lỗi 1: Đánh đồng SaaS với multi-tenancy

**Pattern:** Trong cả ghi chú lý thuyết và câu trả lời, sinh viên viết như thể SaaS = multi-tenant. Ví dụ: "SaaS phải phục vụ nhiều khách hàng (tenant) trên cùng một nền tảng".

**Tại sao quan trọng:** Multi-tenancy là MỘT chiến lược kiến trúc phổ biến trong SaaS, nhưng không phải định nghĩa SaaS. SaaS single-tenant tồn tại (ví dụ: mỗi enterprise client có 1 instance riêng, nhưng nhà cung cấp vẫn host và quản lý). Nếu hiểu sai điều này, khi gặp câu hỏi "SaaS có bắt buộc multi-tenant không?" sẽ trả lời sai.

**Cách cải thiện:** Ghi nhớ: SaaS là **mô hình phân phối** (delivery model). Multi-tenancy là **chiến lược kiến trúc** (architecture pattern). Hai thứ thường đi cùng nhưng không bắt buộc.

---

## Lỗi 2: Lẫn pricing model (subscription) với delivery model (SaaS)

**Pattern:** Câu trả lời câu 3 nói "subscription giúp nhà cung cấp dễ cập nhật sản phẩm" — thực tế cập nhật dễ là do centralized hosting, không phải do cách tính tiền.

**Tại sao quan trọng:** Nếu lẫn 2 khái niệm, khi được hỏi "on-premise có dùng subscription được không?" sẽ bối rối. Thực tế: on-premise subscription license là model rất phổ biến (ví dụ: JetBrains IDE, Adobe Creative Suite trước khi chuyển SaaS).

**Cách cải thiện:** Tách rõ 3 axis: (1) Delivery model: SaaS vs On-premise, (2) Pricing model: Subscription vs License, (3) Architecture: Multi-tenant vs Single-tenant. Ba axis này độc lập với nhau.

---

## Lỗi 3: Trả lời thiên liệt kê, thiếu trade-off

**Pattern:** Hầu hết câu trả lời liệt kê "SaaS cần X, Y, Z" nhưng không nói "cái giá phải trả là gì" hoặc "nếu không dùng SaaS thì alternative là gì".

**Tại sao quan trọng:** Trong interview hoặc design review, câu hỏi thường là "tại sao chọn A mà không chọn B?" Nếu chỉ biết liệt kê đặc điểm A mà không biết trade-off với B, câu trả lời sẽ không thuyết phục.

**Cách cải thiện:** Mỗi câu trả lời nên có cấu trúc: **Lựa chọn → Lý do → Trade-off → Alternative → Kết luận trong ngữ cảnh dự án**.

---

## Lỗi 4: Kết luận vòng (circular reasoning)

**Pattern:** Câu trả lời câu 2: "không mâu thuẫn với bản chất SaaS vì vẫn đảm bảo các tính chất của SaaS" — câu này không có nội dung. Nó nói "A đúng vì A đúng".

**Tại sao quan trọng:** Circular reasoning là dấu hiệu chưa thực sự hiểu sâu. Khi không giải thích được TẠI SAO, thường sẽ rơi vào pattern "lặp lại kết luận thay vì đưa ra bằng chứng".

**Cách cải thiện:** Khi viết kết luận, tự hỏi: "Tôi có thể thay thế câu kết luận này bằng câu trả lời ngược lại mà nó vẫn nghe hợp lý không?" Nếu có → câu kết luận chưa có nội dung thực sự.

---

## Lỗi 5: Thiếu so sánh cả hai phía

**Pattern:** Câu hỏi 1 yêu cầu "so sánh" nhưng câu trả lời chỉ nói về SaaS, gần như không nói gì về on-premise phức tạp ở đâu.

**Tại sao quan trọng:** "So sánh" = phải nói cả hai phía. Chỉ nói một phía là chưa trả lời đủ câu hỏi.

**Cách cải thiện:** Khi gặp câu hỏi "so sánh A và B", bắt buộc phải có ít nhất: (1) A mạnh/yếu ở đâu, (2) B mạnh/yếu ở đâu, (3) Khi nào nên chọn A, khi nào nên chọn B.

---

## Lỗi 6: Nói "cần cẩn thận" nhưng không nói cụ thể

**Pattern:** Câu 4: "ở SaaS khi deploy cần phải có các quy trình rất cẩn thận" — nhưng không nêu quy trình cụ thể nào.

**Tại sao quan trọng:** Trong thực tế, "cần cẩn thận" không phải giải pháp. Cần nêu: rolling deployment, canary release, feature flags, backward-compatible migration, rollback plan, staging environment, automated testing.

**Cách cải thiện:** Mỗi khi viết "cần cẩn thận" hoặc "cần lưu ý", ngay lập tức bổ sung ít nhất 2-3 biện pháp cụ thể.

---

# Part 5 — Câu hỏi follow-up (không có đáp án)

Các câu hỏi dưới đây được thiết kế để sửa chính xác các lỗi nhận thức ở trên.

### Sửa Lỗi 1 (SaaS ≠ multi-tenant)
1. Cho ví dụ 1 sản phẩm SaaS **single-tenant** (mỗi khách hàng có instance riêng nhưng nhà cung cấp vẫn quản lý). Tại sao nó vẫn được gọi là SaaS?
2. Nếu một công ty tự deploy phần mềm multi-tenant trên server riêng của mình để phục vụ nhiều phòng ban — đây có phải SaaS không? Tại sao?

### Sửa Lỗi 2 (Subscription ≠ SaaS)
3. JetBrains IntelliJ IDEA bán dưới dạng subscription nhưng khách hàng download và chạy trên máy mình. Đây là SaaS hay on-premise? Tại sao subscription không tự động làm nó thành SaaS?
4. Nếu nhà cung cấp SaaS chuyển sang tính phí một lần (lifetime license) nhưng vẫn host trên cloud — nó còn là SaaS không?

### Sửa Lỗi 3 (Thiếu trade-off)
5. Nếu dự án kế toán Viettel chọn on-premise thay vì SaaS, liệt kê **3 thứ sẽ đơn giản hơn** và **3 thứ sẽ phức tạp hơn** so với SaaS.
6. Multi-tenancy giúp giảm chi phí hạ tầng. Nhưng cái giá phải trả là gì? Liệt kê ít nhất 4 thứ phức tạp hơn khi dùng multi-tenant so với single-tenant.

### Sửa Lỗi 4 (Circular reasoning) + Lỗi 5 (Thiếu lập luận cụ thể)
7. Trả lời lại câu 2 (data residency) với cấu trúc: **(a)** Mâu thuẫn ở đâu? **(b)** Tại sao vẫn chấp nhận được? **(c)** Kiến trúc thay đổi cụ thể thế nào?

### Sửa Lỗi 6 (Thiếu biện pháp cụ thể)
8. Liệt kê và giải thích ngắn 5 chiến lược deployment cụ thể để giảm blast radius khi deploy SaaS (không được dùng từ "cẩn thận").

### Câu tổng hợp
9. Vẽ bảng 3 cột: **Delivery Model** (SaaS / On-premise / Hybrid) × **Pricing Model** (Subscription / License / Pay-per-use) × **Architecture** (Multi-tenant / Single-tenant). Đánh dấu các tổ hợp nào tồn tại trong thực tế và cho 1 ví dụ mỗi tổ hợp.

---

# Part 6 — Kế hoạch hành động tiếp theo

## ✅ Đã đủ tốt (không cần làm lại)
- Ghi chú lý thuyết file `01-khai-niem-co-ban.md`: cấu trúc tốt, ý chính đúng. Chỉ cần bổ sung 1-2 dòng phân biệt SaaS vs multi-tenancy.
- Ghi chú file `03-ham-y-saas-len-kien-truc.md`: checklist cuối file và bảng "Cần làm / Không cần vội" rất thực tế.

## 🔄 Cần bổ sung / sửa nhẹ
1. **File `01-khai-niem-co-ban.md`:** Thêm 1 section "SaaS không phải là gì" — phân biệt với IaaS/PaaS, phân biệt với multi-tenancy, phân biệt với subscription.
2. **File `02-so-sanh-saas-vs-on-premise.md`:** Sửa dòng Multi-tenant trong bảng so sánh (SaaS "thường" multi-tenant, không phải "luôn"). Thêm cột Hybrid.

## 📝 Cần trả lời lại
3. **Câu 2** (data residency): Trả lời lại với cấu trúc rõ ràng hơn, thừa nhận mâu thuẫn, giải thích tại sao vẫn chấp nhận được, nêu giải pháp kiến trúc cụ thể.
4. **Câu 3** (subscription vs license): Trả lời lại, tách rõ lợi ích kỹ thuật vs kinh doanh, không lẫn subscription với SaaS delivery.

## 🆕 Cần trả lời mới
5. **Câu 5** (feature flags): Đây là câu Level 3, buộc phải suy nghĩ về data model và logic xử lý — rất tốt để luyện tư duy kiến trúc.
6. **Câu 7** (chọn chiến lược isolation): Câu này kết nối SaaS với multi-tenant design, bắt buộc phải trả lời trước khi sang chủ đề multi-tenant.
7. **Câu 8** (developer quên tenant_id): Câu này kiểm tra hiểu biết về data leakage risk — rất quan trọng cho security mindset.

## 📚 Cần đọc thêm
8. **Cloud service model stack:** Đọc thêm về IaaS vs PaaS vs SaaS để phân biệt rõ.
9. **Deployment strategies:** Đọc về rolling deployment, blue-green, canary, feature flags — sẽ cần cho cả Phase 1 và interview.

## 🎯 Thứ tự ưu tiên
1. Trả lời lại câu 2 và câu 3 (sửa lỗi nhận thức hiện tại)
2. Trả lời 9 câu follow-up ở Part 5 (củng cố kiến thức)
3. Trả lời câu 7, 8 từ ngân hàng câu hỏi (chuẩn bị cho chủ đề multi-tenant)
4. Thử câu 5 (feature flags — Level 3)
5. Bổ sung ghi chú lý thuyết
