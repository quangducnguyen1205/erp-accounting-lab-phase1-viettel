# 🔬 PostgreSQL Internals cho Multi-tenant — Phần 2: Partitioning, Migration, Lock và Vì sao PostgreSQL

> Tài liệu bổ sung cho các câu hỏi multi-tenant Part 1 (câu 7, 11, 13).
> Nguồn chính: [PostgreSQL Official Documentation](https://www.postgresql.org/docs/current/).

---

## 1. Table Partitioning — Cơ chế và Ứng dụng

### 1.1. Partitioning là gì?

Partitioning chia 1 bảng logic thành nhiều bảng vật lý (partitions). PostgreSQL hỗ trợ **declarative partitioning** từ version 10+.

3 kiểu partition:

| Kiểu | Cách chia | Ví dụ use case |
|------|----------|----------------|
| **RANGE** | Theo khoảng giá trị | Chia theo tháng: `created_at` |
| **LIST** | Theo danh sách giá trị cụ thể | Chia theo `tenant_id` cụ thể |
| **HASH** | Theo hash value | Phân đều N nhóm tenant |

### 1.2. Partition by LIST cho multi-tenant

```sql
-- Bảng parent (không chứa data trực tiếp)
CREATE TABLE invoice (
    id         bigserial,
    tenant_id  bigint NOT NULL,
    invoice_no varchar(50) NOT NULL,
    amount     numeric(18,2) NOT NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    PRIMARY KEY (id, tenant_id)  -- PK phải chứa partition key
) PARTITION BY LIST (tenant_id);

-- Partition cho từng tenant (hoặc nhóm tenant)
CREATE TABLE invoice_tenant_1 PARTITION OF invoice FOR VALUES IN (1);
CREATE TABLE invoice_tenant_2 PARTITION OF invoice FOR VALUES IN (2);
CREATE TABLE invoice_tenant_3 PARTITION OF invoice FOR VALUES IN (3);

-- Partition "default" cho tenant chưa có partition riêng
CREATE TABLE invoice_default PARTITION OF invoice DEFAULT;
```

### 1.3. Partition Pruning — Cơ chế

Khi query có điều kiện trên partition key, PostgreSQL **tự động loại bỏ các partition không liên quan** trước khi thực thi.

```sql
EXPLAIN ANALYZE
SELECT * FROM invoice WHERE tenant_id = 2 AND amount > 1000;
```

```
Append
  ->  Seq Scan on invoice_tenant_2       ← CHỈ scan partition của tenant 2
        Filter: (amount > 1000)
  (2 rows)
  
-- Các partition invoice_tenant_1, invoice_tenant_3 KHÔNG BỊ ĐỌC.
```

**Điều kiện để partition pruning hoạt động:**
- Query phải có điều kiện trên **partition key** (`tenant_id`)
- Điều kiện phải là constant tại thời điểm planning (không phải subquery)
- Cài đặt `enable_partition_pruning = on` (mặc định ON)

### 1.4. Partition Pruning giải quyết noisy neighbor thế nào?

```
TRƯỚC partition:
┌──────────────────────────────────────┐
│        invoice (10M rows)            │
│  Tenant A: 5M rows                  │
│  Tenant B: 100 rows                 │  ← Query B vẫn phải operate
│  Tenant C: 4.9M rows                │     trên bảng chung 10M rows
└──────────────────────────────────────┘

SAU partition:
┌────────────────────┐ ┌────────┐ ┌────────────────────┐
│ invoice_tenant_a   │ │ _b     │ │ invoice_tenant_c   │
│ 5M rows            │ │100 rows│ │ 4.9M rows          │
└────────────────────┘ └────────┘ └────────────────────┘
                         ↑
                    Pruning: chỉ scan partition nhỏ này
```

- VACUUM chạy trên từng partition riêng → tenant A có nhiều dead rows không ảnh hưởng partition B
- Index trên mỗi partition nhỏ hơn → nhanh hơn
- Shared buffers: partition nhỏ chiếm ít cache hơn

### 1.5. Trade-off của partitioning

| Ưu điểm | Nhược điểm |
|---------|------------|
| ✅ Partition pruning → query nhanh hơn | ⚠️ PK phải chứa partition key |
| ✅ VACUUM nhanh hơn (per-partition) | ⚠️ Quản lý partition phức tạp hơn |
| ✅ Giảm noisy neighbor | ⚠️ Cross-partition query chậm hơn |
| ✅ Có thể DROP partition (xóa data tenant nhanh) | ⚠️ Số partition nhiều → planning time tăng |

> 📌 **Kết luận Phase 1:** Chưa cần partition. Nhưng biết rằng khi data tăng, partition by tenant_id (LIST hoặc HASH) là giải pháp tự nhiên cho shared table.

---

## 2. Schema Migration — Tại sao khó và Lock thực sự là gì

### 2.1. ALTER TABLE và Lock trong PostgreSQL

Khi chạy `ALTER TABLE`, PostgreSQL phải lấy **lock** trên bảng. Mức lock phụ thuộc loại thay đổi.

**8 mức lock trong PostgreSQL** (từ yếu đến mạnh):

| Mức | Tên | Block gì |
|-----|-----|----------|
| 1 | ACCESS SHARE | Không block gì (SELECT bình thường) |
| 2 | ROW SHARE | Rất ít conflict |
| 3 | ROW EXCLUSIVE | INSERT/UPDATE/DELETE lấy lock này |
| 4 | SHARE UPDATE EXCLUSIVE | VACUUM, CREATE INDEX CONCURRENTLY |
| 5 | SHARE | CREATE INDEX (non-concurrent) |
| 6 | SHARE ROW EXCLUSIVE | |
| 7 | EXCLUSIVE | |
| **8** | **ACCESS EXCLUSIVE** | **Block TẤT CẢ** — kể cả SELECT |

**Quan trọng nhất cho migration:**

| Lệnh ALTER TABLE | Lock cần | Ảnh hưởng |
|-------------------|----------|-----------|
| `ADD COLUMN ... DEFAULT NULL` | ACCESS EXCLUSIVE | Block rất ngắn (metadata change only, PG 11+) |
| `ADD COLUMN ... DEFAULT value NOT NULL` | ACCESS EXCLUSIVE | PG 11+: nhanh (lưu default trong catalog). PG < 11: rewrite toàn bộ bảng! |
| `DROP COLUMN` | ACCESS EXCLUSIVE | Ngắn (chỉ đánh dấu invisible) |
| `ALTER COLUMN TYPE` | ACCESS EXCLUSIVE | **Rewrite toàn bộ bảng** → rất lâu trên bảng lớn |
| `ADD CONSTRAINT ... NOT VALID` | SHARE UPDATE EXCLUSIVE | Không block read/write |
| `VALIDATE CONSTRAINT` | SHARE UPDATE EXCLUSIVE | Scan bảng nhưng không block write |
| `CREATE INDEX` | SHARE | Block INSERT/UPDATE/DELETE |
| `CREATE INDEX CONCURRENTLY` | SHARE UPDATE EXCLUSIVE | **Không block** read/write (nhưng chậm hơn) |

### 2.2. Tại sao ACCESS EXCLUSIVE nguy hiểm trong SaaS?

```
Timeline:
─────────────────────────────────────────────►
  │                                    │
  │  ALTER TABLE invoice               │
  │  ADD COLUMN tax_code VARCHAR(20);  │
  │                                    │
  │  ← ACCESS EXCLUSIVE lock ──────►   │
  │                                    │
  │  ❌ Mọi SELECT bị block            │
  │  ❌ Mọi INSERT bị block            │
  │  ❌ Mọi UPDATE bị block            │
  │                                    │
  │  ALL TENANTS bị ảnh hưởng          │
```

Trong shared table (MH1), `ALTER TABLE` trên bảng `invoice` = **tất cả tenant bị block** trong thời gian lock được giữ.

**Thời gian lock phụ thuộc:**
- `ADD COLUMN ... DEFAULT NULL`: vài milliseconds (PG 11+, chỉ sửa metadata)
- `ALTER COLUMN TYPE`: **hàng phút đến hàng giờ** trên bảng hàng triệu rows (phải rewrite)
- `ADD INDEX`: tùy kích thước bảng, có thể vài phút

### 2.3. Backward-compatible migration pattern

```sql
-- ✅ AN TOÀN: Thêm cột nullable (lock rất ngắn)
ALTER TABLE invoice ADD COLUMN tax_code VARCHAR(20);

-- ✅ AN TOÀN: Thêm cột có default (PG 11+, không rewrite)
ALTER TABLE invoice ADD COLUMN tax_code VARCHAR(20) DEFAULT '';

-- ✅ AN TOÀN: Tạo index không block write
CREATE INDEX CONCURRENTLY idx_invoice_tax ON invoice(tenant_id, tax_code);

-- ⚠️ NGUY HIỂM: Đổi kiểu cột = rewrite bảng
ALTER TABLE invoice ALTER COLUMN tax_code TYPE TEXT;
-- → Cách an toàn: tạo cột mới, migrate data, đổi tên, xóa cột cũ

-- ⚠️ NGUY HIỂM: Thêm NOT NULL constraint
ALTER TABLE invoice ALTER COLUMN tax_code SET NOT NULL;
-- → Cách an toàn: ADD CONSTRAINT ... NOT VALID → VALIDATE riêng
ALTER TABLE invoice ADD CONSTRAINT chk_tax_code
    CHECK (tax_code IS NOT NULL) NOT VALID;
ALTER TABLE invoice VALIDATE CONSTRAINT chk_tax_code; -- scan nhưng không block write
```

> 📌 **Liên hệ câu 6 (zero-downtime):** "Backward-compatible migration" nghĩa là: (1) chỉ dùng các lệnh lock ngắn, (2) code cũ vẫn chạy được với schema mới, (3) không rewrite bảng.

### 2.4. Schema per tenant — Migration complexity chi tiết

Với schema per tenant (MH2), mỗi tenant có schema riêng → migration phải loop:

```python
# Pseudo-code migration cho schema per tenant
schemas = get_all_tenant_schemas()  # ["tenant_1", "tenant_2", ..., "tenant_500"]

for schema in schemas:
    try:
        execute(f"SET search_path TO {schema}")
        execute("ALTER TABLE invoice ADD COLUMN tax_code VARCHAR(20)")
        log(f"✅ {schema} migrated")
    except Exception as e:
        log(f"❌ {schema} FAILED: {e}")
        # Vấn đề: 200 schema đã migrate, schema này fail
        # → inconsistency: một số tenant có cột mới, một số không
        # → rollback 200 schema? Hay skip và fix sau?
```

**Rủi ro cụ thể:**

| Rủi ro | Giải thích |
|--------|-----------|
| **Partial migration** | 200/500 schema thành công, 1 fail → 299 chưa chạy. Code mới expect cột mới → 300 tenant bị lỗi |
| **Lock per-schema** | Mỗi ALTER TABLE lock 1 schema → nếu schema đang có transaction chạy → phải chờ hoặc timeout |
| **Thời gian tổng** | 500 schema × 2 giây mỗi schema = ~17 phút downtime (tối thiểu) |
| **Rollback** | Phải loop ngược lại 200 schema đã migrate → nếu rollback cũng fail → tình huống rất tệ |

**So sánh migration complexity:**

| Mô hình | Số lần migrate | Lock ảnh hưởng | Rollback difficulty |
|---------|:---:|:---:|:---:|
| tenant_id column (MH1) | 1 | Tất cả tenant cùng lúc (ngắn) | Đơn giản — 1 lệnh |
| Schema per tenant (MH2) | N | Mỗi lần lock 1 tenant | Phức tạp — N lệnh, có thể partial |
| DB per tenant (MH3) | N | Mỗi lần lock 1 tenant, hoàn toàn independent | Trung bình — mỗi DB rollback riêng |

> 📌 **Liên hệ câu 11:** Đây là giải thích chi tiết tại sao "phải migrate TẤT CẢ N schema" và rủi ro cụ thể là gì.

---

## 3. Vì sao PostgreSQL phù hợp cho bài toán SaaS kế toán

### 3.1. ACID và tính chính xác dữ liệu tài chính

Hệ thống kế toán yêu cầu **dữ liệu phải chính xác tuyệt đối**. Không chấp nhận "gần đúng" hay "eventually consistent".

PostgreSQL cung cấp **ACID transactions** mạnh:

| Tính chất | Ý nghĩa | Tại sao quan trọng cho kế toán |
|-----------|---------|-------------------------------|
| **Atomicity** | Transaction hoặc thành công hoàn toàn, hoặc rollback hoàn toàn | Hạch toán nợ/có phải đồng thời — không được ghi nợ mà quên ghi có |
| **Consistency** | DB luôn ở trạng thái hợp lệ | Tổng nợ phải bằng tổng có (constraint) |
| **Isolation** | Transactions chạy song song không ảnh hưởng nhau | 2 kế toán cùng ghi sổ không được conflict mà mất data |
| **Durability** | Data đã commit không bao giờ mất | Chứng từ đã ghi sổ phải tồn tại vĩnh viễn |

```sql
-- Ví dụ: hạch toán phải atomic
BEGIN;
  INSERT INTO journal_entry (tenant_id, account_id, debit, credit)
  VALUES (1, 111, 10000000, 0);        -- Nợ: Hàng tồn kho

  INSERT INTO journal_entry (tenant_id, account_id, debit, credit)
  VALUES (1, 331, 0, 10000000);        -- Có: Phải trả nhà cung cấp
COMMIT;
-- Nếu bất kỳ INSERT nào fail → cả 2 đều rollback
```

### 3.2. Schema support cho multi-tenant

PostgreSQL là 1 trong số ít DB hỗ trợ **schema** (namespace) tốt → có thể dùng schema per tenant (MH2) nếu cần.

```sql
-- Tạo schema cho tenant
CREATE SCHEMA tenant_abc;

-- Switch context
SET search_path TO tenant_abc;

-- Mọi query tự động trong schema tenant_abc
SELECT * FROM invoice; -- = tenant_abc.invoice
```

MySQL không có khái niệm schema tương đương (MySQL "database" ≈ PostgreSQL "schema", nhưng isolation semantics khác).

### 3.3. Row Level Security

Đã trình bày ở Phần 1. Đây là tính năng PostgreSQL mà MySQL, MongoDB KHÔNG có ở mức tương đương.

### 3.4. So sánh với alternatives cho bài toán này

| Tiêu chí | PostgreSQL | MySQL | MongoDB |
|----------|:---------:|:-----:|:-------:|
| **ACID transactions** | ✅ Mạnh, mặc định | ✅ (InnoDB) | ⚠️ Có từ 4.0, nhưng không mặc định |
| **Multi-tenant schema** | ✅ Schema native | ❌ Phải dùng separate DB | N/A (schemaless) |
| **Row Level Security** | ✅ Built-in | ❌ Không có | ❌ Không có |
| **Partitioning** | ✅ Declarative (PG 10+) | ✅ Có | ✅ Sharding |
| **JSONB (dynamic fields)** | ✅ Rất mạnh, có index | ⚠️ JSON type, index hạn chế | ✅ Native |
| **Connection model** | Process-per-connection (cần PgBouncer) | Thread-per-connection (nhẹ hơn) | Thread pool |
| **Phù hợp kế toán** | ✅✅✅ | ✅✅ | ❌ Không phù hợp (eventual consistency risk) |
| **Open-source** | ✅ Hoàn toàn | ⚠️ Oracle sở hữu | ⚠️ SSPL license |

**Tại sao KHÔNG dùng MongoDB cho kế toán?**
- MongoDB mặc định **eventual consistency** cho read — có thể đọc data cũ
- Không có foreign key constraint — dữ liệu kế toán có quan hệ chặt (chứng từ → bút toán → tài khoản)
- Không có ACID transaction mặc định trên nhiều collection
- Dữ liệu kế toán có cấu trúc rõ ràng → relational model phù hợp hơn document model

> 📌 **Kết luận:** PostgreSQL là lựa chọn tự nhiên cho SaaS kế toán vì: ACID mạnh + schema support + RLS + partitioning + open-source. MySQL có thể dùng được nhưng thiếu schema và RLS. MongoDB không phù hợp cho dữ liệu tài chính.

---

## 4. EXPLAIN ANALYZE — Công cụ quan trọng nhất cho backend developer

### 4.1. Cách đọc output

```sql
EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
SELECT * FROM invoice
WHERE tenant_id = 2 AND status = 'PAID'
ORDER BY created_at DESC
LIMIT 10;
```

```
Limit (cost=0.43..12.50 rows=10) (actual time=0.05..0.08 rows=10 loops=1)
  -> Index Scan Backward using idx_inv_tenant_created on invoice
       Index Cond: (tenant_id = 2)
       Filter: (status = 'PAID')
       Rows Removed by Filter: 3
       Buffers: shared hit=5           ← 5 pages đọc từ cache (tốt!)
 Planning Time: 0.15 ms
 Execution Time: 0.10 ms              ← Rất nhanh
```

**Các chỉ số quan trọng:**
- `shared hit`: page đọc từ cache (nhanh)
- `shared read`: page đọc từ disk (chậm)
- `Rows Removed by Filter`: rows phải đọc rồi bỏ (càng nhiều = càng lãng phí)
- `Seq Scan` trên bảng lớn: 🚩 cảnh báo noisy neighbor

### 4.2. Khi nào dùng EXPLAIN trong thực tế

- **Development:** Mọi query mới → chạy EXPLAIN trước khi merge
- **Debugging:** Query chậm → EXPLAIN ANALYZE để tìm bottleneck
- **Review:** Kiểm tra query có dùng index đúng không

---

## Những gì cần nắm ở mức Phase 1

| Chủ đề | Mức độ | Ghi chú |
|--------|:------:|---------|
| B-Tree index hoạt động thế nào | ⭐⭐ Hiểu concept | Biết nó skip đến đúng chỗ thay vì scan toàn bộ |
| Composite index thứ tự cột | ⭐⭐⭐ Phải nắm | `tenant_id` LUÔN ĐẦU TIÊN |
| EXPLAIN ANALYZE | ⭐⭐⭐ Phải dùng được | Biết đọc Seq Scan vs Index Scan, shared hit vs read |
| Full table scan + noisy neighbor | ⭐⭐⭐ Phải hiểu | Biết tại sao thiếu index là nguy hiểm |
| RLS | ⭐⭐ Biết concept | Biết PostgreSQL có thể enforce tenant isolation ở DB level |
| Lock levels (ACCESS EXCLUSIVE) | ⭐⭐ Biết concept | Biết ALTER TABLE block queries, dùng safe migration patterns |
| Backward-compatible migration | ⭐⭐⭐ Phải nắm | ADD COLUMN nullable = an toàn. ALTER TYPE = nguy hiểm |
| Partition pruning | ⭐ Biết tồn tại | Chưa cần implement, biết là giải pháp cho scale |
| VACUUM / bloat | ⭐ Biết tồn tại | Biết shared table → VACUUM lâu hơn |
| Connection pooling (PgBouncer) | ⭐ Biết tồn tại | Demo chưa cần, production cần |

## Những gì có thể đào sâu thêm ở Phase 2

- **Query optimization thực tế:** Viết query cho module kế toán, optimize với EXPLAIN
- **Partitioning implementation:** Partition bảng `journal_entry` theo tenant_id khi data tăng
- **RLS implementation:** Bật RLS cho shared tables trong demo
- **PgBouncer configuration:** Setup connection pooling cho production
- **Monitoring slow queries:** `pg_stat_statements` extension, log_min_duration_statement
- **Replication setup:** Primary + Read Replica cho báo cáo kế toán nặng
- **Advanced indexing:** Partial index (`WHERE is_active = true`), expression index, covering index (INCLUDE)

---

*Quay lại: [Phần 1: Index, Query và Noisy Neighbor](./postgresql-internals-phan1-index-va-query.md)*
