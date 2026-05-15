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
