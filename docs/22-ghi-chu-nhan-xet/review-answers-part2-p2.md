# Review câu trả lời SaaS & Multi-tenant — Phần 2 (Multi-tenant)

## Multi-tenant — Câu 7

**Câu hỏi:**
> So sánh 3 mô hình tenant isolation. 500 SME + 5 enterprise → chọn chiến lược nào?

**Câu trả lời của tôi:**
> 3 mô hình này tiến hoá từ đơn giản đến phức tạp [...] Tôi sẽ chọn chiến lược hybrid. Tức là với 500 doanh nghiệp SME, sẽ dùng mô hình tenant_id column, còn với 5 doanh nghiệp enterprise sẽ dùng mô hình DB per tenant. Lý do là vì với 500 doanh nghiệp SME thì việc dùng tenant_id column sẽ giúp tiết kiệm chi phí hơn rất nhiều [...]

**Đánh giá:**
- Mức độ đúng: **Đúng**
- Nhận xét nhanh: Đây là câu trả lời tốt nhất trong bộ. Đúng hướng, có reasoning, có trade-off.

**Chỗ còn thiếu (nhẹ):**
- Không nói tại sao **không chọn schema per tenant** cho segment nào — cần nêu: 500 schema quá nhiều để migration, 5 enterprise thì DB per tenant tốt hơn schema vì isolation mạnh hơn
- Thiếu đề cập trade-off của hybrid: codebase phải support 2 mode → phức tạp hơn
- Thiếu kết luận Phase 1: demo dùng tenant_id column, nhưng thiết kế repository layer với interface để switch sau

**Nếu so với mentor:** Mentor sẽ hài lòng với hướng đi. Chỉ cần bổ sung lý do loại bỏ MH2 và trade-off hybrid.

---

## Multi-tenant — Câu 8

**Câu hỏi:**
> Developer quên `WHERE tenant_id = ?` → hậu quả? Đề xuất ít nhất 3 cách phòng tránh.

**Câu trả lời của tôi:**
> Vì mô hình tenant_id column, chỉ dùng 1 db, nên nếu một developer quên thêm `WHERE tenant_id = ?` trong một query thì sẽ dẫn đến việc truy vấn dữ liệu của tất cả các tenant, gây ra lỗi bảo mật nghiêm trọng.
> 3 cách phòng tránh: // Chưa nghĩ ra, cần hỗ trợ đáp án

**Đánh giá:**
- Mức độ đúng: **Đúng phần hậu quả, chưa trả lời phần giải pháp**
- Nhận xét nhanh: Hiểu đúng hậu quả (data leakage). Nhưng câu hỏi yêu cầu 3 cách phòng tránh — chưa trả lời được.

**Chỗ tôi đang hiểu chưa chuẩn:**
- "truy vấn dữ liệu của tất cả các tenant" — đúng hướng nhưng cần nói rõ hơn: **tenant A có thể thấy data của tenant B, C, D...** Đây là data leakage — lỗi bảo mật nghiêm trọng nhất trong multi-tenant.

**Tôi đang thiếu gì:**
- 3 cách phòng tránh cụ thể. Xem đáp án tham chiếu ở file bổ sung.
- Gợi ý: nghĩ theo 3 tầng: (1) framework-level auto-filter, (2) codebase convention, (3) testing

---

## Multi-tenant — Câu 9

**Câu hỏi:**
> Cache key `"categories"` vs `"tenant:1:categories"` — tại sao cách đầu tiên sai nghiêm trọng?

**Câu trả lời của tôi:**
> Cách đầu tiên là sai lầm nghiêm trọng vì nó không phân biệt được dữ liệu của các tenant khác nhau. Nếu cache key chỉ là `"categories"` thì khi một tenant truy cập vào cache, họ sẽ nhận được dữ liệu của tất cả các tenant khác

**Đánh giá:**
- Mức độ đúng: **Cơ bản đúng nhưng chưa chặt**
- Nhận xét nhanh: Hiểu đúng vấn đề (data leakage qua cache). Nhưng giải thích chưa chính xác về cơ chế.

**Chỗ tôi đang hiểu chưa chuẩn:**
- "nhận được dữ liệu của tất cả các tenant khác" — **không chính xác.** Họ nhận được data của **tenant nào cache trước** (tenant đầu tiên gây cache miss), không phải "tất cả các tenant". Cơ chế cụ thể:
  1. Tenant A request → cache miss → query DB → cache `"categories"` = [data A]
  2. Tenant B request → cache HIT → trả [data A] cho Tenant B → **B thấy data A!**

**Tôi nên sửa cách hiểu:**
- Vấn đề là **cache collision** — nhiều tenant map vào cùng 1 key → data của tenant cache trước bị trả cho tenant khác
- Giải pháp: convention `tenant:{id}:{resource}`, enforce bằng helper method

---

## Multi-tenant — Câu 10

**Câu hỏi:**
> Tenant A có 1 triệu bản ghi, Tenant B có 100. Hiệu suất B có bị ảnh hưởng?

**Câu trả lời của tôi:**
> Hiệu suất của tenant B sẽ bị ảnh hưởng do đáng lẽ B chỉ có 100 bản ghi nhưng những query của B sẽ phải scan qua 1 triệu bản ghi của A để lọc ra 100 bản ghi của B [...] Giải pháp được đề xuất là có chiến lược index phù hợp cho bản ghi B [...]

**Đánh giá:**
- Mức độ đúng: **Đúng một phần**
- Nhận xét nhanh: Đúng hướng khi nói có ảnh hưởng và đề xuất index. Nhưng giải thích cơ chế chưa chính xác.

**Chỗ tôi đang hiểu chưa chuẩn:**
- "query của B sẽ phải scan qua 1 triệu bản ghi của A" — **chỉ đúng nếu không có index.** Nếu có composite index `(tenant_id, ...)`, DB sẽ skip đến đúng partition của B mà KHÔNG scan data A. Vấn đề thực sự là:
  - Table size lớn → index tree lớn → disk I/O tăng, vacuum chậm
  - Nếu A chạy heavy query → lock contention, buffer pool bị chiếm
  - Đây là **noisy neighbor ở tầng DB**, không phải full table scan
- "index phù hợp cho bản ghi B" — index không phải "cho B", mà là composite index `(tenant_id, column)` trên bảng chung → B-tree skip đến đúng tenant

**Tôi đang thiếu gì:**
- Composite index `(tenant_id, ...)` — giải pháp số 1
- Table partitioning by tenant_id
- Connection pool isolation cho tenant lớn
- Cân nhắc migrate tenant lớn sang DB riêng

---

## Multi-tenant — Câu 11 ⚠️ SAI NGHIÊM TRỌNG

**Câu hỏi:**
> Schema per tenant — khi thêm cột mới, phải migrate bao nhiêu schema? Quy trình? Rủi ro?

**Câu trả lời của tôi:**
> Khi có thay đổi thì chỉ cần migrate 1 schema của bảng mà có thay đổi.

**Đánh giá:**
- Mức độ đúng: **Sai**
- Nhận xét nhanh: Hiểu sai hoàn toàn mô hình schema per tenant.

**Chỗ tôi đang hiểu sai:**
- **Schema per tenant = MỖI tenant có 1 schema riêng.** Nếu có 500 tenant → có 500 schema. Khi thêm cột mới → phải chạy migration cho **TẤT CẢ 500 schema**, không phải 1.
- Đây chính là trade-off lớn nhất của schema per tenant: migration complexity tỷ lệ thuận với số tenant.

**Đáp án đúng:**
- Phải migrate **N schema** (N = số tenant)
- Quy trình: viết migration script → chạy loop qua tất cả schema → handle failure per-schema
- Rủi ro: (1) 1 schema fail → inconsistency giữa các tenant, (2) thời gian migration dài, (3) khó rollback nếu chỉ 1 số schema đã migrate

**Tôi nên sửa cách hiểu:**
- Schema per tenant ≠ "1 schema dùng chung". Nó nghĩa là **mỗi tenant có namespace riêng trong DB**.
- Đọc lại ghi chú lý thuyết về 3 mô hình isolation.

---

## Multi-tenant — Câu 12

**Câu hỏi:**
> Thiết kế cơ chế tự động inject tenant_id vào mọi query.

**Câu trả lời của tôi:**
> // Cần học thêm chưa biết làm

**Đánh giá:**
- Mức độ đúng: **Chưa trả lời**
- Nhận xét: Chấp nhận được ở giai đoạn này. Đây là câu Level 3 cần kiến thức Spring Boot/Hibernate. Xem đáp án tham chiếu để học.

---

## Multi-tenant — Câu 13

**Câu hỏi:**
> Data portability — export toàn bộ dữ liệu tenant. Cách thực hiện và độ khó?

**Câu trả lời của tôi:**
> - Với tenant_id column: query WHERE tenant_id = ? → export CSV/JSON. Độ khó trung bình.
> - Với schema per tenant: kết nối đến schema → export. Độ khó thấp.
> - Với DB per tenant: kết nối đến DB → export. Độ khó thấp.

**Đánh giá:**
- Mức độ đúng: **Cơ bản đúng**
- Nhận xét nhanh: Hướng đi đúng, thứ tự độ khó hợp lý.

**Chỗ còn thiếu:**
- Tenant_id column: "độ khó trung bình" — đúng, nhưng cần nói rõ TẠI SAO: phải query TỪNG bảng với tenant_id filter, phải biết TẤT CẢ bảng nào chứa data tenant, phải handle foreign key / relationship giữa các bảng
- Schema/DB per tenant: có thể dùng `pg_dump` trực tiếp → đơn giản hơn nhiều vì không cần filter
- Thiếu: format export (JSON? CSV? SQL dump?), compliance (GDPR data portability), performance impact khi export data lớn

---

## Multi-tenant — Câu 14

**Câu hỏi:**
> Một user thuộc nhiều tenant (freelancer kế toán). Data model và auth flow?

**Câu trả lời của tôi:**
> Có thể tạo 1 db riêng cho những user này để quản lý với các trường như user_id, tenant_id, role_in_tenant [...] nếu ban đầu đi vào auth là true thì sẽ chuyển đến cái db riêng đó để kiểm tra, sau đó sẽ được chọn một trong các tenant mà user đó sẽ sử dụng trong session đó [...]

**Đánh giá:**
- Mức độ đúng: **Đúng một phần**
- Nhận xét nhanh: Ý tưởng tenant selection đúng. Nhưng giải pháp "tạo DB riêng" quá phức tạp và không cần thiết.

**Chỗ tôi đang hiểu chưa chuẩn:**
- "tạo 1 db riêng" — không cần DB riêng. Chỉ cần **bảng mapping** `user_tenant_role(user_id, tenant_id, role_id)` trong DB chính.
- "thêm 1 trường flag để check" — không cần flag đặc biệt. Chỉ cần check xem user có bao nhiêu entries trong bảng mapping → nếu > 1 → hiện tenant picker.
- Auth flow không thay đổi quá nhiều — chỉ thêm bước "chọn tenant" sau login.

**Tôi nên sửa cách hiểu:**
- Data model: tách quan hệ user-tenant thành bảng riêng (many-to-many), mỗi row có role
- Auth flow: Login → check số tenant → nếu 1 thì auto-select → nếu nhiều thì hiện picker → set tenant vào JWT/session → tiếp tục bình thường
- JWT claim: `tenant_id` = tenant đang active trong session hiện tại
