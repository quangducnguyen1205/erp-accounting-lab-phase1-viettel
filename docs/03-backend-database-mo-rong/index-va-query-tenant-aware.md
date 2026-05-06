# Index và query tenant-aware

## Vì sao index quan trọng trong shared table?

Trong mô hình shared table, nhiều tenant cùng nằm trong một bảng. Nếu bảng `invoice` có hàng triệu dòng, query thiếu index phù hợp có thể quét qua dữ liệu của nhiều tenant khác nhau.

Ví dụ:

```sql
SELECT *
FROM invoice
WHERE tenant_id = 2
  AND status = 'PAID';
```

Index phù hợp:

```sql
CREATE INDEX idx_invoice_tenant_status
ON invoice (tenant_id, status);
```

Index này giúp database đi thẳng vào nhóm dữ liệu của tenant 2 rồi lọc tiếp theo `status`.

## Composite index

Composite index là index trên nhiều cột. Thứ tự cột rất quan trọng.

```sql
CREATE INDEX idx_invoice_tenant_created_at
ON invoice (tenant_id, created_at);
```

Index trên `(tenant_id, created_at)` hữu ích cho:

```sql
WHERE tenant_id = :tenant_id
WHERE tenant_id = :tenant_id AND created_at >= :from_date
```

Nhưng không tối ưu cho:

```sql
WHERE created_at >= :from_date
```

Lý do là B-tree index mạnh nhất khi query dùng cột đầu tiên hoặc nhóm cột đầu theo thứ tự index. Đây thường được gọi là leftmost prefix.

## Nguyên tắc trong multi-tenant

Với shared table, phần lớn index nghiệp vụ nên bắt đầu bằng `tenant_id` nếu query luôn chạy trong phạm vi một tenant.

Ví dụ:

```sql
CREATE INDEX idx_invoice_tenant_status
ON invoice (tenant_id, status);

CREATE INDEX idx_invoice_tenant_created_at
ON invoice (tenant_id, created_at);

CREATE INDEX idx_customer_tenant_code
ON customer (tenant_id, code);
```

Không phải mọi index đều bắt đầu bằng `tenant_id`, nhưng với query nghiệp vụ của tenant, đây là default cần nghĩ đến đầu tiên.

## Full table scan

Full table scan hoặc sequential scan xảy ra khi PostgreSQL đọc toàn bộ bảng để lọc dữ liệu.

Ví dụ nguy hiểm:

```sql
SELECT *
FROM invoice
WHERE status = 'PAID';
```

Vấn đề:

- Thiếu tenant filter nên có nguy cơ trả dữ liệu cross-tenant.
- Nếu bảng lớn, DB phải đọc nhiều page.
- CPU, I/O và cache bị tiêu thụ.
- Tenant khác có thể bị chậm theo.

## Dùng EXPLAIN

### EXPLAIN là gì?

`EXPLAIN` là lệnh dùng để hỏi database:

> "Nếu chạy câu SQL này, bạn dự định chạy theo cách nào?"

Kết quả trả về gọi là **query plan**. Query plan cho biết PostgreSQL dự kiến đọc dữ liệu bằng cách nào: đọc cả bảng, dùng index, join theo thứ tự nào, ước lượng bao nhiêu dòng dữ liệu, v.v.

Ở giai đoạn này, chưa cần hiểu hết mọi chi tiết trong query plan. Mục tiêu trước mắt chỉ là biết nhìn những dấu hiệu cơ bản:

- PostgreSQL có dùng index không?
- PostgreSQL có phải quét cả bảng không?
- Query có đang lọc đúng theo `tenant_id` không?
- Index mình tạo có thật sự được database dùng không?

Lưu ý: phần này đang nói theo PostgreSQL. Các database khác cũng có công cụ tương tự, nhưng cú pháp và output không nhất thiết giống PostgreSQL.

### EXPLAIN khác EXPLAIN ANALYZE như thế nào?

`EXPLAIN` chỉ hiển thị **kế hoạch dự kiến**. Nó không thật sự chạy câu `SELECT`.

Ví dụ:

```sql
EXPLAIN
SELECT *
FROM master_data
WHERE tenant_id = 1
  AND code = 'LAPTOP-01';
```

`EXPLAIN ANALYZE` thì **chạy thật câu SQL** rồi hiển thị kế hoạch kèm thời gian thực tế.

```sql
EXPLAIN ANALYZE
SELECT *
FROM master_data
WHERE tenant_id = 1
  AND code = 'LAPTOP-01';
```

Hiểu đơn giản:

| Lệnh | Có chạy query thật không? | Dùng khi nào? |
|---|---:|---|
| `EXPLAIN` | Không | Muốn xem kế hoạch dự kiến nhanh |
| `EXPLAIN ANALYZE` | Có | Muốn kiểm chứng thời gian và số dòng thực tế |

Trong các bài thực hành SQL ban đầu, mình dùng `EXPLAIN ANALYZE` với `SELECT` để nhìn thấy PostgreSQL thực sự xử lý query thế nào. Không nên dùng bừa `EXPLAIN ANALYZE` với `INSERT`, `UPDATE`, `DELETE` nếu chưa hiểu rõ, vì câu lệnh có thể được thực thi thật.

### Vì sao backend engineer cần biết EXPLAIN?

Backend engineer không chỉ viết API. Nhiều API chậm hoặc lỗi production thực chất đến từ query database.

Biết đọc `EXPLAIN` ở mức cơ bản giúp:

- Kiểm tra query có dùng index mình mong muốn không.
- Phát hiện query đang đọc quá nhiều dữ liệu.
- Hiểu vì sao một endpoint CRUD đơn giản vẫn có thể chậm.
- Kiểm chứng thiết kế database thay vì chỉ tin rằng "tạo index là sẽ nhanh".
- Trao đổi tốt hơn với DBA hoặc senior engineer khi debug performance.

Với Phase 1, mục tiêu không phải trở thành chuyên gia tối ưu PostgreSQL. Mục tiêu là biết tự kiểm tra những câu query cơ bản mình viết ra.

### Liên hệ với multi-tenant shared-table

Trong mô hình shared table, nhiều tenant nằm chung một bảng:

```text
master_data
├── tenant_id = 1: dữ liệu doanh nghiệp A
├── tenant_id = 2: dữ liệu doanh nghiệp B
└── tenant_id = 3: dữ liệu doanh nghiệp C
```

Vì vậy, một query nghiệp vụ gần như luôn phải bắt đầu từ tenant hiện tại.

Ví dụ đúng hướng:

```sql
SELECT *
FROM master_data
WHERE tenant_id = 1
  AND code = 'LAPTOP-01';
```

Index phù hợp:

```sql
CREATE INDEX idx_master_data_tenant_code
ON master_data (tenant_id, code);
```

Index `(tenant_id, code)` hữu ích vì PostgreSQL có thể tìm theo `tenant_id` trước, sau đó tìm tiếp theo `code` trong phạm vi tenant đó. Đây đúng với cách backend multi-tenant thường truy vấn: "lấy dữ liệu của tenant hiện tại".

Nếu query thiếu `tenant_id`:

```sql
SELECT *
FROM master_data
WHERE code = 'LAPTOP-01';
```

thì có hai vấn đề:

- Về bảo mật: có nguy cơ trả dữ liệu của tenant khác.
- Về hiệu năng: index bắt đầu bằng `tenant_id` có thể không giúp tốt cho query chỉ lọc theo `code`.

`EXPLAIN` giúp mình nhìn thấy thiết kế index và cách viết query có đi cùng nhau không.

### Đọc query plan ở mức cơ bản

Ở giai đoạn này, chỉ cần tập trung vào vài phần dễ hiểu.

#### 1. `Seq Scan`

`Seq Scan` là sequential scan: PostgreSQL đọc tuần tự qua bảng để tìm dòng phù hợp.

Ví dụ plan có thể giống như:

```text
Seq Scan on master_data
  Filter: (code = 'LAPTOP-01')
```

Với bảng nhỏ, `Seq Scan` không phải lúc nào cũng xấu. Nhưng với shared table lớn, nếu query của một tenant khiến PostgreSQL đọc nhiều dữ liệu của toàn bảng, đó là dấu hiệu cần xem lại.

#### 2. `Index Scan`

`Index Scan` nghĩa là PostgreSQL dùng index để tìm dòng phù hợp.

Ví dụ plan có thể giống như:

```text
Index Scan using idx_master_data_tenant_code on master_data
  Index Cond: ((tenant_id = 1) AND (code = 'LAPTOP-01'))
```

Dấu hiệu tốt ở đây là:

- Có dùng index `idx_master_data_tenant_code`.
- Điều kiện index có cả `tenant_id` và `code`.
- Query đang đi đúng theo thiết kế tenant-aware.

#### 3. `Bitmap Index Scan` và `Bitmap Heap Scan`

Khi chạy lab với bảng lớn hơn một chút, bạn có thể thấy plan dạng:

```text
Bitmap Heap Scan on master_data_index_lab
  Recheck Cond: (tenant_id = 1)
  -> Bitmap Index Scan on idx_lab_master_data_tenant_id
       Index Cond: (tenant_id = 1)
```

Hiểu đơn giản:

- `Bitmap Index Scan`: PostgreSQL dùng index để tìm danh sách vị trí các dòng có khả năng phù hợp.
- `Bitmap Heap Scan`: PostgreSQL quay lại bảng thật để lấy các dòng đầy đủ theo danh sách đó.

PostgreSQL có thể chọn kiểu này khi index giúp tìm candidate rows, nhưng số dòng cần lấy không quá ít. Thay vì đi từng dòng một qua index, nó gom vị trí các dòng lại rồi đọc bảng theo cách hiệu quả hơn.

Điều quan trọng trong bài lab:

- `Bitmap Heap Scan` không giống full `Seq Scan`.
- Nó vẫn là plan có index hỗ trợ.
- Nếu thấy `Bitmap Index Scan` dùng index trên `tenant_id`, query tenant-aware đang được index hỗ trợ.

Chưa cần hiểu sâu bitmap hoạt động bên trong. Ở Phase 1, chỉ cần đọc được: đây là một dạng plan trung gian giữa `Seq Scan` và `Index Scan`, và nó thường là tín hiệu tốt hơn full table scan khi bảng lớn.

#### 4. `Filter` và `Index Cond`

Nhìn rất đơn giản:

- `Index Cond`: điều kiện được dùng để đi vào index.
- `Filter`: điều kiện lọc sau khi đã đọc dữ liệu.

Nếu `tenant_id` nằm trong `Index Cond`, đó thường là tín hiệu tốt cho query tenant-aware.

#### 5. Các trường cơ bản trong `EXPLAIN ANALYZE`

Khi dùng `EXPLAIN ANALYZE`, output có thể trông giống như:

```text
Seq Scan on master_data_index_lab
  (cost=0.00..1020.00 rows=500 width=72)
  (actual time=0.050..6.200 rows=503 loops=1)
  Filter: (tenant_id = 1)
  Buffers: local hit=320
```

Ở giai đoạn này, chỉ cần hiểu các trường sau:

| Trường | Ý nghĩa thực tế |
|---|---|
| `cost=...` | Chi phí ước lượng của planner. Đây không phải milliseconds, mà là đơn vị nội bộ để PostgreSQL so sánh các plan. |
| `rows=...` | Số dòng PostgreSQL ước lượng sẽ trả về ở bước đó. |
| `width=...` | Kích thước trung bình ước lượng của mỗi dòng, tính theo byte. |
| `actual time=...` | Thời gian thực tế khi chạy bước đó, tính bằng milliseconds. |
| `actual rows=...` hoặc `rows=...` trong phần actual | Số dòng thực tế trả về khi query chạy thật. |
| `loops=...` | Bước đó được chạy bao nhiêu lần. Query đơn giản thường là `loops=1`. |
| `Planning Time` | Thời gian PostgreSQL dùng để chọn plan. |
| `Execution Time` | Thời gian thực thi query thật sự. |
| `Buffers` | Khi dùng `EXPLAIN (ANALYZE, BUFFERS)`, cho biết query đọc dữ liệu từ cache hay phải đọc page mới. |

Không cần tối ưu theo từng con số nhỏ. Mục tiêu là biết so sánh trước/sau khi thêm index hoặc sửa query.

#### 6. Ước lượng khác thực tế như thế nào?

Trong output của PostgreSQL thường có hai nhóm thông tin:

- **Estimated values**: PostgreSQL đoán trước khi chạy query, ví dụ `cost`, `rows`, `width`.
- **Actual values**: kết quả đo được khi query chạy thật, ví dụ `actual time`, `actual rows`, `loops`.

Ví dụ:

```text
(cost=0.00..1020.00 rows=500 width=72)
(actual time=0.050..6.200 rows=503 loops=1)
```

Ở đây:

- PostgreSQL ước lượng query trả khoảng `500` dòng.
- Thực tế query trả `503` dòng.
- Ước lượng như vậy là khá gần.

Nếu ước lượng lệch quá xa, ví dụ:

```text
(cost=0.00..1020.00 rows=10 width=72)
(actual time=0.050..8.500 rows=5000 loops=1)
```

thì PostgreSQL đã đánh giá sai số dòng. Điều này có thể khiến planner chọn plan không phù hợp. Với Phase 1, chỉ cần nhận ra: nếu estimated rows và actual rows lệch rất nhiều, query plan có thể đáng nghi và cần xem lại dữ liệu, statistics hoặc cách viết query.

#### 7. Buffers đọc thế nào ở mức cơ bản?

Khi dùng:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM master_data
WHERE tenant_id = 1;
```

PostgreSQL có thể hiện:

```text
Buffers: shared hit=20
Buffers: shared read=5
Buffers: local hit=320
```

Hiểu đơn giản:

- `hit`: PostgreSQL đọc được page từ cache/buffer, không phải đọc mới từ disk.
- `read`: PostgreSQL phải đọc page mới từ disk hoặc storage layer.
- `shared`: buffer dùng cho bảng bình thường.
- `local`: buffer dùng cho temp table.

Trong lab `04-index-comparison.sql`, nếu dùng temp table, bạn có thể thấy `local hit` vì dữ liệu nằm trong bảng tạm của session.

Chưa cần phân tích sâu buffer. Chỉ cần biết: nếu một query phải đọc rất nhiều buffer, nó có thể tốn tài nguyên hơn.

#### 8. Vì sao bảng nhỏ vẫn có thể dùng `Seq Scan`?

Một điểm dễ gây nhầm: tạo index không có nghĩa PostgreSQL luôn dùng index.

Với bảng rất nhỏ, PostgreSQL có thể nghĩ:

> "Đọc cả bảng vài dòng còn rẻ hơn mở index rồi quay lại đọc bảng."

Vì vậy, trong các bảng có 5-10 dòng, `Seq Scan` vẫn là bình thường. Đây là lý do bài lab index nên có bảng tạm với dữ liệu đủ lớn hơn để mình dễ quan sát khác biệt giữa:

- Không có index: thường thấy `Seq Scan`.
- Có index phù hợp: có thể thấy `Index Scan` hoặc `Bitmap Index Scan`.

Điều quan trọng không phải là ép PostgreSQL luôn dùng index, mà là hiểu PostgreSQL đang chọn plan nào và vì sao.

### Mini example

Giả sử có bảng `master_data` và index:

```sql
CREATE INDEX idx_master_data_tenant_code
ON master_data (tenant_id, code);
```

Query tenant-aware:

```sql
EXPLAIN ANALYZE
SELECT *
FROM master_data
WHERE tenant_id = 1
  AND code = 'LAPTOP-01';
```

Kỳ vọng học tập:

- Nếu dữ liệu đủ lớn và statistics phù hợp, PostgreSQL có thể dùng `Index Scan`.
- Nếu bảng đang rất nhỏ, PostgreSQL vẫn có thể chọn `Seq Scan` vì đọc cả bảng nhỏ đôi khi rẻ hơn dùng index.
- Điều quan trọng là mình biết kiểm chứng bằng `EXPLAIN`, không đoán mò.

Query thiếu tenant:

```sql
EXPLAIN ANALYZE
SELECT *
FROM master_data
WHERE code = 'LAPTOP-01';
```

Câu này nên bị xem là nguy hiểm trong multi-tenant vì nó không giới hạn theo tenant hiện tại. Dù query chạy nhanh hay chậm, về mặt thiết kế backend nó vẫn sai hướng.

### Điều cần nhớ ở giai đoạn này

1. `EXPLAIN` cho biết PostgreSQL dự định chạy query thế nào.
2. `EXPLAIN ANALYZE` chạy thật query và cho biết kết quả thực tế.
3. Backend engineer cần biết `EXPLAIN` để kiểm chứng query và index.
4. Trong shared-table multi-tenant, query nghiệp vụ phải tenant-aware.
5. Index như `(tenant_id, code)` chỉ thật sự có ý nghĩa khi query cũng lọc theo `tenant_id`.
6. `Seq Scan` là đọc tuần tự qua bảng; `Index Scan` là dùng index để tìm dữ liệu.
7. `Bitmap Heap Scan` đi cùng `Bitmap Index Scan` vẫn là plan được index hỗ trợ.
8. `EXPLAIN` không chỉ là lý thuyết, mà là công cụ kiểm chứng thiết kế database.

Nếu thấy `Index Scan` trên index tenant-aware, query có khả năng đi đúng hướng. Nếu thấy `Seq Scan` trên bảng lớn, cần kiểm tra:

- Có thiếu index không?
- Điều kiện query có dùng đúng cột index không?
- Query trả quá nhiều dòng không?
- Query có thiếu `tenant_id` không?

## Unique constraint tenant-aware

Trong ERP/kế toán, nhiều mã chỉ cần unique trong phạm vi doanh nghiệp, không phải toàn hệ thống.

Sai trong multi-tenant:

```sql
UNIQUE (code)
```

Đúng hơn:

```sql
UNIQUE (tenant_id, code)
```

Ví dụ mã khách hàng `KH001` có thể tồn tại ở nhiều doanh nghiệp khác nhau. Không nên bắt mọi tenant dùng mã duy nhất toàn hệ thống nếu nghiệp vụ không yêu cầu.

### Lưu ý: UNIQUE constraint cũng tạo index

Trong PostgreSQL, khi tạo `PRIMARY KEY` hoặc `UNIQUE` constraint, database sẽ tự tạo B-tree index để enforce constraint đó. Ví dụ `UNIQUE (tenant_id, code)` thường tạo một unique index trên `(tenant_id, code)`.

Index này phục vụ **tính đúng đắn dữ liệu trước**, không phải chỉ để tăng tốc query. Nó đảm bảo trong cùng một tenant không có hai bản ghi trùng `code`.

Vì index này bắt đầu bằng `tenant_id`, PostgreSQL cũng có thể dùng nó cho một số query tenant-aware. Điều này là tốt, nhưng khi làm bài lab so sánh performance, không nên drop business constraint chỉ để ép database dùng `Seq Scan`. Nếu cần thí nghiệm tự do, hãy dùng bảng tạm hoặc bảng lab riêng.

## Kết luận

Tenant-aware query không chỉ là thêm `WHERE tenant_id`. Nó còn kéo theo cách thiết kế index, unique constraint, test, monitoring và thói quen đọc query plan. Với shared table, composite index bắt đầu bằng `tenant_id` là một nền tảng rất quan trọng.
