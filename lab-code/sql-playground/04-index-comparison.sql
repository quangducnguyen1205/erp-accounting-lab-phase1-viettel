-- ==============================================================
-- TASK 4: So sánh chiến lược index trong shared-table multi-tenant
-- ==============================================================
--
-- [Mục tiêu của bài lab]
--
-- Tự kiểm chứng bằng `EXPLAIN (ANALYZE, BUFFERS)`:
--
-- 1. Query tenant-aware chạy thế nào khi KHÔNG có index thử nghiệm?
-- 2. Query thay đổi thế nào khi có index đơn cột trên `tenant_id`?
-- 3. Query thay đổi thế nào khi có composite index trên `(tenant_id, code)`?
-- 4. Query thiếu `tenant_id` nguy hiểm thế nào trong backend multi-tenant?
--
-- Quan trọng:
-- - Bảng thật `master_data` có business constraint:
--   `UNIQUE (tenant_id, code)`.
-- - Trong PostgreSQL, UNIQUE constraint này tự tạo một unique B-tree index.
-- - Không được drop constraint này chỉ để benchmark.
-- - Bài lab dùng bảng TEMP riêng để thí nghiệm index, không phá schema thật.
--
-- Lưu ý học tập:
-- - PostgreSQL chọn plan có chi phí ước lượng rẻ nhất, không phải lúc nào
--   cũng dùng index chỉ vì index tồn tại.
-- - Với bảng rất nhỏ, `Seq Scan` thường rẻ hơn `Index Scan`.
-- - Vì vậy lab này tạo thêm dữ liệu TEMP vừa đủ lớn để dễ quan sát hơn.
--
-- ==============================================================


-- ==============================================================
-- 0. Cleanup đầu file để script có thể chạy lại an toàn trong cùng session
-- ==============================================================
--
-- Temp table tự biến mất khi session kết thúc, nhưng nếu bạn chạy lại file
-- trong cùng một psql session thì bảng temp cũ vẫn có thể còn tồn tại.

DROP TABLE IF EXISTS master_data_index_lab;


-- ==============================================================
-- 1. Inspect index hiện có trên bảng thật `master_data`
-- ==============================================================
--
-- Mục tiêu:
-- Nhìn thấy bảng thật đang có những index nào trước khi làm thí nghiệm.
--
-- Khi đọc kết quả, hãy tự xác định:
-- - Index do PRIMARY KEY tạo ra.
-- - Index do UNIQUE (tenant_id, code) tạo ra.
-- - Index thủ công `idx_master_data_tenant_category`.
--
-- Ghi nhớ:
-- `UNIQUE (tenant_id, code)` không chỉ là rule logic.
-- PostgreSQL cần một unique index để enforce rule này.

SELECT schemaname, tablename, indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename = 'master_data'
ORDER BY indexname;


-- ==============================================================
-- 2. Vì sao KHÔNG drop UNIQUE constraint?
-- ==============================================================
--
-- `UNIQUE (tenant_id, code)` bảo vệ tính đúng đắn nghiệp vụ:
-- - Cùng một tenant không được có 2 dòng trùng `code`.
-- - Hai tenant khác nhau vẫn có thể dùng cùng một `code`.
--
-- Nếu drop constraint này chỉ để ép query ra `Seq Scan`,
-- mình đang phá business rule thật của schema.
--
-- Đây là thói quen không tốt trong backend:
-- không nên phá correctness để làm thí nghiệm performance.
--
-- Ghi chú của tôi:
-- Constraint `UNIQUE (tenant_id, code)` giúp mỗi tenant có bộ mã riêng,
-- không bị conflict với tenant khác nhưng vẫn chặn trùng mã trong cùng tenant.
-- Drop constraint này trong bài lab là không an toàn vì nó làm mất rule nghiệp vụ
-- mà schema thật cần bảo vệ.


-- ==============================================================
-- 3. Tạo bảng TEMP để thí nghiệm index
-- ==============================================================
--
-- `CREATE TEMP TABLE ... AS SELECT ...` chỉ copy dữ liệu/kết quả SELECT.
-- Nó KHÔNG copy PRIMARY KEY, UNIQUE constraint, foreign key, hoặc index.
--
-- Bước đầu tiên: copy dữ liệu thật đang có để giữ liên hệ với bài lab trước.

CREATE TEMP TABLE master_data_index_lab AS
SELECT id, tenant_id, code, name, category, is_active, created_at
FROM master_data;


-- ==============================================================
-- 3.1. Sinh thêm dữ liệu lab vừa đủ lớn
-- ==============================================================
--
-- Vì dữ liệu mẫu ban đầu chỉ có vài dòng, PostgreSQL thường chọn `Seq Scan`
-- ngay cả khi đã tạo index. Điều đó không sai: đọc cả bảng nhỏ rẻ hơn đi qua index.
--
-- Để dễ quan sát sự khác biệt giữa `Seq Scan` và index-related scan,
-- ta sinh thêm dữ liệu TEMP. Dữ liệu này chỉ phục vụ học tập và không đụng
-- vào bảng thật `master_data`.
--
-- Cấu hình mặc định:
-- - 50.000 dòng lab.
-- - 100 tenant giả lập.
-- - Mỗi tenant có khoảng 500 dòng.
--
-- Nếu máy yếu, có thể giảm generate_series xuống 10.000.
-- Nếu muốn plan khác biệt rõ hơn, có thể tăng lên 100.000 hoặc 200.000.
-- Không nên dùng hàng triệu dòng mặc định trong lab beginner.

INSERT INTO master_data_index_lab (
    id,
    tenant_id,
    code,
    name,
    category,
    is_active,
    created_at
)
SELECT
    100000 + gs AS id,
    ((gs - 1) % 100) + 1 AS tenant_id,
    'ITEM-' || lpad(gs::text, 6, '0') AS code,
    'Du lieu lab ' || gs AS name,
    CASE gs % 5
        WHEN 0 THEN 'Laptop'
        WHEN 1 THEN 'Mouse'
        WHEN 2 THEN 'Monitor'
        WHEN 3 THEN 'Keyboard'
        ELSE 'Printer'
    END AS category,
    TRUE AS is_active,
    now() - ((gs % 365) * interval '1 day') AS created_at
FROM generate_series(1, 50000) AS gs;

-- Cập nhật statistics để planner có thông tin tương đối đúng về bảng temp.
ANALYZE master_data_index_lab;

-- Kiểm tra nhanh phân bố dữ liệu.
-- Quan sát:
-- - Tổng số dòng trong bảng temp là bao nhiêu?
-- - Mỗi tenant có khoảng bao nhiêu dòng?

SELECT COUNT(*) AS total_rows
FROM master_data_index_lab;

SELECT tenant_id, COUNT(*) AS rows_per_tenant
FROM master_data_index_lab
GROUP BY tenant_id
ORDER BY tenant_id
LIMIT 10;


-- ==============================================================
-- 4. Verify bảng TEMP chưa có index
-- ==============================================================
--
-- Kỳ vọng:
-- - Không thấy index business constraint như bảng thật `master_data`.
-- - Nếu chưa tự tạo index nào, kết quả có thể rỗng.

SELECT schemaname, tablename, indexname, indexdef
FROM pg_indexes
WHERE tablename = 'master_data_index_lab'
ORDER BY indexname;


-- ==============================================================
-- 5. Case 1: Query tenant-aware khi CHƯA có index thử nghiệm
-- ==============================================================
--
-- Mục tiêu:
-- Quan sát plan khi query có `tenant_id` nhưng bảng temp chưa có index.
--
-- Câu hỏi cần quan sát:
-- - Bạn thấy scan type nào? `Seq Scan` hay `Index Scan`?
-- - `cost`, `rows`, `width` ước lượng là bao nhiêu?
-- - `actual rows` thực tế là bao nhiêu?
-- - Query nhanh/chậm ra sao khi phải đọc bảng temp lớn hơn?

EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM master_data_index_lab
WHERE tenant_id = 1;

-- QUERY PLAN
-- -----------------------------------------------------------------------------------------------------------------------
--  Seq Scan on master_data_index_lab  (cost=0.00..1194.08 rows=520 width=60) (actual time=0.046..9.572 rows=503 loops=1)
--    Filter: (tenant_id = 1)
--    Rows Removed by Filter: 49503
--    Buffers: local hit=569
--  Planning:
--    Buffers: shared hit=23 dirtied=2
--  Planning Time: 0.570 ms
--  Execution Time: 9.706 ms
-- (8 rows)
--
-- Nhận xét:
-- Đây là baseline "chưa có index thử nghiệm". PostgreSQL phải đọc bảng temp
-- rồi lọc `tenant_id = 1`, nên `Rows Removed by Filter` lớn.
-- Đây là dấu hiệu tốt cho bài học: query đúng về tenant, nhưng chưa có index hỗ trợ.

-- ==============================================================
-- 6. Case 2: Tạo index đơn cột trên `tenant_id`
-- ==============================================================
--
-- Mục tiêu:
-- Kiểm tra index đơn cột có giúp query theo tenant không.
--
-- Lưu ý:
-- Sau khi tạo index, chạy `ANALYZE` để planner cập nhật statistics.

CREATE INDEX idx_lab_master_data_tenant_id
ON master_data_index_lab (tenant_id);

ANALYZE master_data_index_lab;

EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM master_data_index_lab
WHERE tenant_id = 1;

-- Câu hỏi cần so sánh với Case 1:
-- - Scan type có đổi từ `Seq Scan` sang `Index Scan` hoặc `Bitmap Index Scan` không?
-- - PostgreSQL đọc ít buffer hơn hay không?
-- - `actual rows` có giống Case 1 không?
-- - Nếu vẫn là `Seq Scan`, có thể do bảng vẫn nhỏ hoặc planner thấy scan cả bảng rẻ hơn.

-- QUERY PLAN
-- ------------------------------------------------------------------------------------------------------------------------------------------
--  Bitmap Heap Scan on master_data_index_lab  (cost=8.07..586.74 rows=488 width=60) (actual time=0.385..0.831 rows=503 loops=1)
--    Recheck Cond: (tenant_id = 1)
--    Heap Blocks: exact=500
--    Buffers: shared hit=3, local hit=500 read=2
--    ->  Bitmap Index Scan on idx_lab_master_data_tenant_id  (cost=0.00..7.95 rows=488 width=0) (actual time=0.315..0.315 rows=503 loops=1)
--          Index Cond: (tenant_id = 1)
--          Buffers: shared hit=3, local read=2
--  Planning:
--    Buffers: shared hit=35, local read=1
--  Planning Time: 0.985 ms
--  Execution Time: 1.121 ms
-- (11 rows)
--
-- Nhận xét:
-- Plan đã chuyển sang Bitmap Index Scan + Bitmap Heap Scan.
-- Đây vẫn là index-assisted plan: PostgreSQL dùng index để tìm các dòng ứng viên
-- có `tenant_id = 1`, rồi quay lại heap để lấy row đầy đủ.
-- So với Case 1, query đọc ít dữ liệu thừa hơn và execution time giảm rõ rệt
-- trong lần chạy local này.

-- ==============================================================
-- 7. Case 3: Composite index trên `(tenant_id, code)`
-- ==============================================================
--
-- Mục tiêu:
-- Kiểm tra index nhiều cột cho query rất phổ biến trong backend:
-- "Tìm một mã dữ liệu trong phạm vi tenant hiện tại".
--
-- Vì sao chọn `(tenant_id, code)`?
-- - `tenant_id` đứng đầu vì hầu hết query nghiệp vụ chạy trong phạm vi tenant.
-- - `code` đứng sau vì trong tenant đó, mình muốn tìm một mã cụ thể.
--
-- Code `ITEM-000001` thuộc tenant 1 theo công thức sinh dữ liệu ở trên:
-- tenant_id = ((gs - 1) % 100) + 1.

CREATE INDEX idx_lab_master_data_tenant_code
ON master_data_index_lab (tenant_id, code);

ANALYZE master_data_index_lab;

EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM master_data_index_lab
WHERE tenant_id = 1
  AND code = 'ITEM-000001';

-- Câu hỏi cần quan sát:
-- - PostgreSQL có dùng composite index không?
-- - `tenant_id` và `code` có xuất hiện trong `Index Cond` không?
-- - `actual rows` có ít hơn nhiều so với query chỉ có `tenant_id` không?
-- - Trường hợp này khác gì với index chỉ có `(tenant_id)`?

-- QUERY PLAN
-- --------------------------------------------------------------------------------------------------------------------------------------------------------
--  Index Scan using idx_lab_master_data_tenant_code on master_data_index_lab  (cost=0.41..8.43 rows=1 width=60) (actual time=0.139..0.140 rows=1 loops=1)
--    Index Cond: ((tenant_id = 1) AND ((code)::text = 'ITEM-000001'::text))
--    Buffers: local hit=1 read=3
--  Planning:
--    Buffers: shared hit=39, local read=1
--  Planning Time: 4.831 ms
--  Execution Time: 0.254 ms
-- (7 rows)
--
-- Nhận xét:
-- Composite index `(tenant_id, code)` phù hợp hơn khi query có cả hai điều kiện.
-- `actual rows=1` cho thấy query rất selective: chỉ tìm một mã cụ thể trong
-- phạm vi một tenant cụ thể.
-- Đây là pattern gần với backend thật: lấy một record theo mã trong tenant hiện tại.

-- ==============================================================
-- 8. Case 4: Query thiếu `tenant_id`
-- ==============================================================
--
-- Mục tiêu:
-- Quan sát query chỉ lọc theo `code`, thiếu tenant context.
--
-- Query này có thể nhanh hoặc chậm tùy dữ liệu/index,
-- nhưng trong backend multi-tenant nó vẫn nguy hiểm vì không giới hạn tenant.

EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM master_data_index_lab
WHERE code = 'ITEM-000001';

-- Ghi chú:
-- Query thiếu `tenant_id` có thể gây data leakage.
-- Ví dụ nếu API `GET /master-data/{code}` chỉ filter theo `code`,
-- tenant B có thể nhìn thấy dữ liệu thuộc tenant A nếu `code` trùng hoặc đoán được.
--
-- Câu hỏi cần quan sát:
-- - Composite index `(tenant_id, code)` có giúp tốt cho query thiếu tenant không?
-- - Nếu PostgreSQL vẫn chọn `Seq Scan`, điều đó liên hệ gì với leftmost prefix?
-- - Dù query nhanh, vì sao vẫn sai về mặt thiết kế multi-tenant?

-- QUERY PLAN
-- --------------------------------------------------------------------------------------------------------------------
--  Seq Scan on master_data_index_lab  (cost=0.00..1194.08 rows=1 width=60) (actual time=0.147..12.268 rows=1 loops=1)
--    Filter: ((code)::text = 'ITEM-000001'::text)
--    Rows Removed by Filter: 50005
--    Buffers: local hit=569
--  Planning Time: 0.626 ms
--  Execution Time: 12.425 ms
-- (6 rows)
--
-- Nhận xét:
-- Query thiếu `tenant_id` nên không tận dụng tốt composite index `(tenant_id, code)`.
-- Plan dùng Seq Scan và phải loại bỏ nhiều dòng bằng Filter.
-- Quan trọng hơn: đây là lỗi thiết kế trong multi-tenant, vì query không giới hạn
-- dữ liệu theo tenant hiện tại.

-- ==============================================================
-- 9. Kết luận tự học
-- ==============================================================
--
-- Sau khi chạy xong các case, tự ghi kết luận ngắn:
--
-- 1. Khi không có index thử nghiệm, query tenant-aware chạy theo plan nào?
-- 2. Index `(tenant_id)` giúp gì?
-- 3. Index `(tenant_id, code)` giúp gì?
-- 4. Vì sao không nên drop UNIQUE constraint thật để benchmark?
-- 5. Vì sao query thiếu `tenant_id` là lỗi thiết kế trong backend multi-tenant?
--
-- Kết luận của tôi:
-- 1. Khi chưa có index thử nghiệm, query tenant-aware dùng Seq Scan trên bảng temp.
-- 2. Index `(tenant_id)` giúp PostgreSQL tìm các dòng thuộc một tenant nhanh hơn;
--    trong lần chạy local, plan chuyển sang Bitmap Index Scan + Bitmap Heap Scan.
-- 3. Index `(tenant_id, code)` phù hợp cho query tìm một mã cụ thể trong một tenant,
--    ví dụ `WHERE tenant_id = ... AND code = ...`.
-- 4. Không nên drop `UNIQUE (tenant_id, code)` của bảng thật để benchmark,
--    vì đó là business constraint bảo vệ tính đúng đắn dữ liệu.
-- 5. Query thiếu `tenant_id` là rủi ro data leakage. Performance có thể nhanh/chậm
--    tùy dữ liệu, nhưng về correctness thì vẫn sai trong backend multi-tenant.


-- ==============================================================
-- 10. Cleanup
-- ==============================================================
--
-- Temp table sẽ tự biến mất khi session PostgreSQL kết thúc.
-- Tuy nhiên, trong bài lab, cleanup rõ ràng sẽ dễ hiểu hơn.

DROP TABLE IF EXISTS master_data_index_lab;
