# 1) Shared table là gì

Ví dụ tất cả tenant dùng chung một bảng `invoice`:

```sql
create table invoice (
    id bigserial primary key,
    tenant_id bigint not null,
    invoice_no varchar(50) not null,
    status varchar(20) not null,
    amount numeric(18,2) not null,
    created_at timestamp not null default now()
);
```

Dữ liệu kiểu này:

```sql
insert into invoice (tenant_id, invoice_no, status, amount)
values
    (1, 'INV-001', 'PAID', 1000),
    (1, 'INV-002', 'DRAFT', 2000),
    (2, 'INV-003', 'PAID', 1500),
    (2, 'INV-004', 'OVERDUE', 3000);
```

Ý nghĩa: tenant 1 và tenant 2 nằm chung một bảng, chỉ phân biệt bằng `tenant_id`.

---

# 2) Vì sao cần index `(tenant_id, status)` hoặc `(tenant_id, created_at)`

Giả sử ứng dụng hay chạy query kiểu:

```sql
select *
from invoice
where tenant_id = 2
  and status = 'PAID';
```

Nếu tạo index tốt:

```sql
create index idx_invoice_tenant_status
on invoice (tenant_id, status);
```

thì database có thể tìm nhanh đúng phần dữ liệu của tenant 2 trước, rồi mới lọc tiếp theo `status`.

Bạn có thể hiểu rất đời thường như này:

- Không có index: lật cả chồng hồ sơ ra xem.
- Có index `(tenant_id, status)`: nhảy ngay đến ngăn hồ sơ của tenant 2, rồi tìm tiếp hồ sơ `PAID`.

---

# 3) Full table scan là gì và vì sao noisy neighbor xảy ra

Ví dụ query xấu:

```sql
select *
from invoice
where amount > 1000;
```

Query này không có `tenant_id`. Nếu dữ liệu lớn, database dễ phải quét gần như cả bảng.

Hoặc query này cũng có thể tệ nếu không có index phù hợp:

```sql
select *
from invoice
where tenant_id = 1
  and created_at >= now() - interval '30 days';
```

Nếu bạn chưa tạo index như:

```sql
create index idx_invoice_tenant_created_at
on invoice (tenant_id, created_at);
```

thì DB có thể vẫn quét rất nhiều row.

Vấn đề là: tenant A chạy một query nặng như vậy, thì:

- CPU bị ăn nhiều
- Disk I/O tăng
- Cache RAM của DB bị chiếm
- Query của tenant B cũng chậm theo

Đó chính là kiểu **noisy neighbor**: một tenant “ồn ào” làm tenant khác bị ảnh hưởng.

---

# 4) Ví dụ “đúng” và “sai” trong code query

## Sai hơn

```sql
select *
from invoice
where status = 'PAID';
```

Sai ở đây không phải cú pháp sai, mà là thiếu `tenant_id`.

Trong multi-tenant shared table, đây là dấu hiệu rất nguy hiểm.

## Đúng hơn

```sql
select *
from invoice
where tenant_id = :tenantId
  and status = 'PAID';
```

Nếu ứng dụng dùng Hibernate/JPA thì thường sẽ bind `:tenantId` từ tenant hiện tại.

---

# 5) Composite index nghĩa là gì

Composite index là index nhiều cột.

Ví dụ:

```sql
create index idx_invoice_tenant_status
on invoice (tenant_id, status);
```

Nó hữu ích cho query như:

```sql
select *
from invoice
where tenant_id = 2
  and status = 'PAID';
```

Nhưng nếu query là:

```sql
select *
from invoice
where status = 'PAID';
```

thì index đó có thể không giúp nhiều, vì cột đầu tiên của index là `tenant_id`.

Cách nhớ:

> Index `(tenant_id, status)` mạnh nhất khi query bắt đầu từ `tenant_id`.

---

# 6) Partitioning theo `tenant_id` là gì

Khi bảng quá to, có thể chia bảng thành các phần nhỏ hơn.

Ví dụ ý tưởng:

```sql
create table invoice (
    id bigint not null,
    tenant_id bigint not null,
    invoice_no varchar(50) not null,
    status varchar(20) not null,
    amount numeric(18,2) not null,
    created_at timestamp not null
) partition by hash (tenant_id);
```

Tạo các partition:

```sql
create table invoice_p0 partition of invoice
for values with (modulus 4, remainder 0);

create table invoice_p1 partition of invoice
for values with (modulus 4, remainder 1);

create table invoice_p2 partition of invoice
for values with (modulus 4, remainder 2);

create table invoice_p3 partition of invoice
for values with (modulus 4, remainder 3);
```

Khi query:

```sql
select *
from invoice
where tenant_id = 2;
```

PostgreSQL có thể **partition pruning**, tức là chỉ đọc partition liên quan thay vì đọc toàn bộ.

Bạn có thể hình dung:

- Chưa partition: 1 kho hàng lớn.
- Có partition: chia thành 4 kho nhỏ, biết tenant ở kho nào thì vào đúng kho đó.

---

# 7) Ví dụ connection pool riêng cho tenant lớn

Đây là ý tưởng ở mức app, không phải SQL thuần.

Ví dụ pseudo-code:

```java
DataSource ds;
if (isLargeTenant(tenantId)) {
    ds = largeTenantDataSource;
} else {
    ds = sharedDataSource;
}
```

Ý nghĩa:

- Tenant rất lớn không dùng chung hết connection với tenant nhỏ.
- Tránh trường hợp tenant lớn chiếm sạch kết nối DB.

Trong Phase 1 thường chưa cần làm đến đây, nhưng về mặt scale thì đây là một hướng.

---

# 8) Ví dụ read replica cho heavy query

Query đọc nặng:

```sql
select tenant_id, status, sum(amount)
from invoice
group by tenant_id, status;
```

Loại query tổng hợp lớn này có thể cho chạy ở read replica thay vì DB chính.

Pseudo-code:

```java
if (isHeavyReadQuery) {
    useReadReplica();
} else {
    usePrimary();
}
```

Ý nghĩa:

- DB chính tập trung xử lý ghi và query quan trọng.
- Query báo cáo nặng bị đẩy sang máy đọc riêng.

---

# 9) Chốt lại bằng một ví dụ rất sát bài toán

## Trường hợp tốt

```sql
create index idx_invoice_tenant_created_at
on invoice (tenant_id, created_at);

select *
from invoice
where tenant_id = 5
  and created_at >= '2026-04-01';
```

Query này khá “tenant-aware”, nên tenant khác ít bị ảnh hưởng hơn.

## Trường hợp xấu

```sql
select *
from invoice
where amount > 1000
order by created_at desc;
```

Query này không bám tenant, dễ quét rất rộng. Nếu tenant A chạy liên tục, tenant B có thể bị chậm theo. Đó là noisy neighbor.

---

## Một câu nhớ nhanh

> Shared table không sai, nhưng mọi query phải nghĩ theo tenant trước; nếu không, tenant lớn sẽ kéo hiệu năng của tenant nhỏ xuống.

---