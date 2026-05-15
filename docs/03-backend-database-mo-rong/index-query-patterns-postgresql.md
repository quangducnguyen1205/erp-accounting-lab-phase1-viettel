# PostgreSQL index và query pattern trong multi-tenant shared-table

## Mục tiêu học

Bài này bổ sung cho `index-va-query-tenant-aware.md`. Điểm cần nhớ: index không chỉ là “tạo index rồi query sẽ nhanh”. PostgreSQL chỉ cân nhắc dùng index khi điều kiện query phù hợp với loại index, dữ liệu đủ chọn lọc và planner ước lượng index plan rẻ hơn scan tuần tự.

Trong backend multi-tenant shared-table, query pattern thường có dạng:

```sql
WHERE tenant_id = ?
WHERE tenant_id = ? AND code = ?
WHERE tenant_id = ? AND category = ?
WHERE tenant_id = ? AND name LIKE ?
WHERE tenant_id = ? AND lower(name) = ?
```

Mỗi pattern có cách dùng index khác nhau.

## B-tree index phù hợp với gì?

Trong PostgreSQL, `CREATE INDEX` mặc định tạo B-tree index. B-tree phù hợp nhất với:

- so sánh bằng: `=`;
- so sánh range: `<`, `<=`, `>`, `>=`;
- `BETWEEN`, `IN`;
- một số pattern prefix như `LIKE 'abc%'` nếu pattern là hằng số và điều kiện collation/operator class phù hợp.

Ví dụ:

```sql
WHERE code = 'LAPTOP-01'
WHERE tenant_id = 1 AND code = 'LAPTOP-01'
WHERE name LIKE 'Lap%'
```

Nhưng planner vẫn có quyền chọn `Seq Scan` nếu bảng nhỏ hoặc điều kiện không đủ selective.

## `LIKE 'abc%'` khác gì `LIKE '%abc%'`?

`LIKE 'abc%'` là prefix search: chuỗi phải bắt đầu bằng `abc`. Về mặt B-tree, đây gần giống tìm một khoảng giá trị bắt đầu từ `abc`, nên PostgreSQL có thể cân nhắc dùng B-tree index.

`LIKE '%abc%'` là contains search: chuỗi có thể chứa `abc` ở bất kỳ vị trí nào. B-tree index thông thường không biết nhảy thẳng tới phần giữa chuỗi, nên pattern này thường không tận dụng B-tree tốt.

`LIKE '%abc'` là suffix search: chuỗi kết thúc bằng `abc`. Đây cũng không phù hợp tự nhiên với B-tree index thông thường.

Ghi nhớ ở mức Phase 1:

| Pattern | Ý nghĩa | B-tree thường hỗ trợ tốt không? |
|---|---|---|
| `name = 'Laptop'` | exact match | Có |
| `name LIKE 'Lap%'` | prefix search | Có thể |
| `name LIKE '%top%'` | contains search | Thường không |
| `name LIKE '%top'` | suffix search | Thường không |

Không nên kết luận cứng rằng mọi database đều giống PostgreSQL, hoặc mọi PostgreSQL setup đều ra cùng plan. Luôn dùng `EXPLAIN` để verify.

Trong lab 07 có dùng `text_pattern_ops` cho index prefix search:

```sql
CREATE INDEX ... ON master_data_pattern_lab (tenant_id, name text_pattern_ops);
```

Đây là cách giúp PostgreSQL dùng B-tree rõ hơn cho pattern dạng `LIKE 'Laptop%'` trong bài lab. Chưa cần học sâu operator class ở giai đoạn này; chỉ cần hiểu rằng prefix search có điều kiện kỹ thuật riêng, và phải verify bằng `EXPLAIN`.

## Contains search và `pg_trgm`

Khi bài toán thật cần tìm chứa chuỗi như:

```sql
WHERE name LIKE '%laptop%'
```

B-tree index thường không phải lựa chọn chính. PostgreSQL có extension `pg_trgm` để hỗ trợ similarity/substring search bằng trigram, thường đi cùng GIN hoặc GiST index.

Trong Phase 1, chỉ cần biết:

- `pg_trgm` phù hợp hơn cho contains/fuzzy search so với B-tree thường;
- đây là hướng học sau, không cần đưa ngay vào demo chính;
- trước khi thêm extension/index mới, phải đo bằng `EXPLAIN` và hiểu trade-off ghi dữ liệu/index size.

## Composite index và leftmost prefix

Composite index như:

```sql
CREATE INDEX ... ON master_data (tenant_id, code);
```

phù hợp nhất khi query dùng cột bên trái trước:

```sql
WHERE tenant_id = ? AND code = ?
WHERE tenant_id = ?
```

Với B-tree multicolumn index, điều kiện trên leading/leftmost column rất quan trọng để giảm phạm vi index cần scan. Vì vậy trong shared-table multi-tenant, đặt `tenant_id` đầu index thường hợp lý cho query scoped theo tenant.

Query nguy hiểm:

```sql
WHERE code = ?
```

không chỉ kém tenant-aware, mà còn có thể không tận dụng tốt index `(tenant_id, code)` vì thiếu cột đầu tiên. Quan trọng hơn: đây là rủi ro data leakage.

## Expression index: ví dụ `lower(column)`

Nếu query dùng hàm trên cột:

```sql
WHERE lower(name) = 'laptop dell'
```

index thường trên `name` không nhất thiết giúp trực tiếp cho biểu thức `lower(name)`. PostgreSQL hỗ trợ index trên expression:

```sql
CREATE INDEX ... ON master_data (lower(name));
```

Ghi nhớ:

- query phải khớp với expression đã index;
- expression index có chi phí maintain khi insert/update;
- chỉ thêm khi pattern query thật sự thường xuyên và cần tối ưu.

## Selectivity và vì sao vẫn có Seq Scan

Planner chọn plan có chi phí ước lượng thấp nhất, không phải plan “có index là dùng index”.

Các lý do thường gặp khiến PostgreSQL vẫn chọn `Seq Scan`:

- bảng quá nhỏ;
- điều kiện trả về phần lớn số dòng;
- statistics chưa cập nhật, cần `ANALYZE`;
- pattern query không phù hợp với index hiện có;
- chi phí đọc index rồi quay lại table cao hơn đọc tuần tự.

Trong lab, nếu thấy `Seq Scan` dù đã tạo index, chưa vội kết luận index sai. Hãy kiểm tra:

- dữ liệu có đủ lớn không;
- query có đúng pattern không;
- đã `ANALYZE` chưa;
- index có đúng thứ tự cột không;
- điều kiện có dùng function/wildcard làm index khó dùng không.

## Đọc EXPLAIN trong bài lab 07

Ở bài lab 07, không nên chỉ nhìn `Execution Time`. Thời gian chạy có thể dao động do cache, máy local, Docker, dữ liệu vừa mới đọc, hoặc lần chạy đầu/chạy sau. Hãy đọc thêm các phần sau:

### `Index Cond` và `Filter`

`Index Cond` cho biết điều kiện nào được dùng để truy cập index.

Ví dụ tốt:

```text
Index Cond: ((tenant_id = 1) AND (code = 'ITEM-01-000001'))
```

Nghĩa là PostgreSQL dùng cả `tenant_id` và `code` trong index lookup.

`Filter` cho biết điều kiện được áp dụng sau khi PostgreSQL đã lấy candidate rows.

Ví dụ cần chú ý:

```text
Index Cond: (tenant_id = 1)
Filter: (name ~~ '%Dell%')
```

Plan này vẫn dùng index, nhưng chủ yếu dùng index để giới hạn `tenant_id`. Điều kiện contains search `name LIKE '%Dell%'` không thật sự giúp truy cập B-tree index; nó chỉ lọc sau.

Đây là lý do trong multi-tenant query bạn có thể thấy index xuất hiện rất nhiều: vì `tenant_id` gần như luôn có trong query. Muốn biết điều kiện search có dùng index không, hãy xem điều kiện đó nằm ở `Index Cond` hay chỉ ở `Filter`.

### Estimated rows và actual rows

Trong plan thường có:

```text
rows=...
actual rows=...
```

- `rows` là số dòng planner ước lượng.
- `actual rows` là số dòng thật sự trả về khi chạy `EXPLAIN ANALYZE`.

Nếu hai con số lệch nhiều, planner có thể chọn plan chưa tối ưu vì thống kê dữ liệu không phản ánh đúng thực tế hoặc điều kiện query khó ước lượng. Ở mức hiện tại, chỉ cần biết sự lệch này là tín hiệu cần xem lại data distribution, statistics hoặc query pattern.

### `Rows Removed by Filter`

`Rows Removed by Filter` cho biết PostgreSQL đã lấy một số candidate rows rồi loại bỏ sau bằng `Filter`.

Nếu con số này lớn, nghĩa là index chỉ giúp thu hẹp một phần, còn điều kiện lọc chính vẫn phải làm nhiều việc sau đó.

Trong lab 07, case contains search có thể cho thấy:

- `tenant_id` nằm trong `Index Cond`;
- `name LIKE '%Dell%'` nằm trong `Filter`;
- nhiều dòng bị loại bởi filter.

Kết luận: plan có index-assisted, nhưng B-tree chưa giải quyết tốt contains search.

### Buffers ở mức cơ bản

`BUFFERS` cho biết PostgreSQL đụng tới bao nhiêu block dữ liệu/index.

Ở mức học hiện tại:

- buffer ít hơn thường là tín hiệu tốt;
- buffer nhiều nghĩa là query phải đọc/chạm nhiều dữ liệu hơn;
- không cần tối ưu buffer sâu trong Phase 1.

Đừng dùng buffer như chỉ số duy nhất. Hãy đọc cùng scan type, `Index Cond`, `Filter`, rows estimate và actual rows.

### Cách kết luận trong lab này

Khi so sánh hai plan, hãy tự hỏi:

1. Scan type là gì: `Seq Scan`, `Index Scan`, `Bitmap Index Scan`, `Bitmap Heap Scan`?
2. Điều kiện nào nằm trong `Index Cond`?
3. Điều kiện nào nằm trong `Filter`?
4. Có nhiều `Rows Removed by Filter` không?
5. Estimated rows và actual rows lệch nhiều không?
6. `Execution Time` có khác biệt ổn định qua nhiều lần chạy không, hay chỉ nhiễu local?

## Query pattern nên nhớ cho tenant-aware backend

| Backend need | Query pattern nên ưu tiên | Ghi chú |
|---|---|---|
| Tìm theo code | `tenant_id = ? AND code = ?` | Phù hợp với `(tenant_id, code)` |
| Lọc theo category | `tenant_id = ? AND category = ?` | Có thể cần `(tenant_id, category)` nếu dùng thường xuyên |
| Prefix search theo name | `tenant_id = ? AND name LIKE 'abc%'` | Có thể cần index phù hợp và verify bằng EXPLAIN |
| Contains search | `tenant_id = ? AND name LIKE '%abc%'` | Cân nhắc `pg_trgm`/GIN nếu là requirement thật |
| Case-insensitive search | `tenant_id = ? AND lower(name) = ?` | Cân nhắc expression index |
| Query thiếu tenant | `code = ?` hoặc `name LIKE ?` | Sai về isolation, không chỉ performance |

## Điều cần nhớ ở giai đoạn này

1. Index phải đi cùng query pattern cụ thể.
2. Với shared-table multi-tenant, query nghiệp vụ phải có `tenant_id`.
3. Composite index nên phản ánh pattern query thật, ví dụ `(tenant_id, code)`.
4. Prefix search khác contains search.
5. Function như `lower(name)` có thể cần expression index.
6. Planner có thể chọn `Seq Scan` nếu đó là plan rẻ nhất.
7. `EXPLAIN` là công cụ verify thiết kế index, không chỉ là lý thuyết.

## Nguồn tham khảo chuẩn

- PostgreSQL Documentation: [Chapter 11. Indexes](https://www.postgresql.org/docs/current/indexes.html)
- PostgreSQL Documentation: [Index Types](https://www.postgresql.org/docs/current/indexes-types.html)
- PostgreSQL Documentation: [Multicolumn Indexes](https://www.postgresql.org/docs/current/indexes-multicolumn.html)
- PostgreSQL Documentation: [Indexes on Expressions](https://www.postgresql.org/docs/current/indexes-expressional.html)
- PostgreSQL Documentation: [Pattern Matching](https://www.postgresql.org/docs/current/functions-matching.html)
- PostgreSQL Documentation: [`pg_trgm`](https://www.postgresql.org/docs/current/pgtrgm.html)
- PostgreSQL Documentation: [`EXPLAIN`](https://www.postgresql.org/docs/current/sql-explain.html)
