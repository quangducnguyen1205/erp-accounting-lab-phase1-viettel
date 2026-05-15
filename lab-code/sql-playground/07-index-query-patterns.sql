/*
 * ==============================================================
 * 07-index-query-patterns.sql
 * ==============================================================
 *
 * Mục tiêu:
 * - Quan sát index usage theo query pattern trong PostgreSQL.
 * - So sánh exact match, prefix LIKE, leading wildcard/contains LIKE,
 *   expression index và composite index theo tenant-aware query.
 * - Giữ bảng thật master_data an toàn; chỉ thử trên bảng tạm.
 *
 * Bối cảnh học:
 * - Index không chỉ là CREATE INDEX.
 * - PostgreSQL chỉ dùng index khi query pattern phù hợp và planner thấy rẻ.
 * - Trong shared-table multi-tenant, tenant_id vẫn là điều kiện correctness,
 *   không chỉ là điều kiện performance.
 *
 * Cách đọc bài này:
 * - Xem Index Cond để biết phần nào thật sự được dùng để truy cập index.
 * - Xem Filter / Rows Removed by Filter để biết phần nào chỉ lọc sau khi lấy
 *   candidate rows.
 * - Nếu query tenant-aware luôn có tenant_id, PostgreSQL có thể dùng index
 *   nhờ tenant_id. Điều đó chưa chứng minh điều kiện LIKE/lower(...) cũng
 *   dùng index.
 *
 * ==============================================================
 */

\echo '=============================================================='
\echo '0. Pre-check: bảng thật master_data'
\echo '=============================================================='

SELECT COUNT(*) AS master_data_rows
FROM master_data;

SELECT schemaname, tablename, indexname, indexdef
FROM pg_indexes
WHERE tablename = 'master_data'
  AND schemaname = 'public'
ORDER BY indexname;

/*
 * Ghi nhớ:
 * - master_data thật có business constraint UNIQUE (tenant_id, code).
 * - PostgreSQL tự tạo index để enforce unique constraint đó.
 * - Không drop constraint thật chỉ để benchmark.
 */

\echo '=============================================================='
\echo '1. Chuẩn bị bảng tạm cho lab'
\echo '=============================================================='

DROP TABLE IF EXISTS master_data_pattern_lab;

CREATE TEMP TABLE master_data_pattern_lab AS
SELECT id, tenant_id, code, name, category, is_active, created_at
FROM master_data;

/*
 * Dataset vừa phải cho máy local:
 * - 200.000 row lab.
 * - 49 tenant, mỗi tenant khoảng 4.000 row.
 * - Có category/name lặp lại để test prefix và contains search.
 *
 * Lưu ý: công thức tenant_id phải là ((gs - 1) % 49) + 1.
 * Nếu viết sai precedence, tenant_id có thể tăng theo gs và làm lab lệch.
 * Không dùng 50 tenant ở đây vì 50 chia hết cho 5 category, dễ làm mỗi tenant
 * bị lệch về một category cố định.
 */
INSERT INTO master_data_pattern_lab (id, tenant_id, code, name, category, is_active, created_at)
SELECT
    1000000 + gs AS id,
    ((gs - 1) % 49) + 1 AS tenant_id,
    'ITEM-' || lpad((((gs - 1) % 49) + 1)::text, 2, '0') || '-' || lpad(gs::text, 6, '0') AS code,
    CASE gs % 5
        WHEN 0 THEN 'Laptop Dell Lab ' || gs
        WHEN 1 THEN 'Monitor Dell Lab ' || gs
        WHEN 2 THEN 'Mouse Logitech Lab ' || gs
        WHEN 3 THEN 'Keyboard Mechanical Lab ' || gs
        ELSE 'Accessory USB Lab ' || gs
    END AS name,
    CASE gs % 5
        WHEN 0 THEN 'Laptop'
        WHEN 1 THEN 'Monitor'
        WHEN 2 THEN 'Mouse'
        WHEN 3 THEN 'Keyboard'
        ELSE 'Accessory'
    END AS category,
    TRUE AS is_active,
    now() - ((gs % 365) * interval '1 day') AS created_at
FROM generate_series(1, 200000) AS gs;

ANALYZE master_data_pattern_lab;

SELECT COUNT(*) AS lab_rows
FROM master_data_pattern_lab;

SELECT tenant_id, COUNT(*) AS rows_per_tenant
FROM master_data_pattern_lab
GROUP BY tenant_id
ORDER BY tenant_id
LIMIT 5;

\echo '=============================================================='
\echo '2. Case A: exact match trước/sau composite B-tree index'
\echo '=============================================================='

\echo 'Case A1 - Chưa có index thử nghiệm: thường Seq Scan'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM master_data_pattern_lab
WHERE tenant_id = 1
  AND code = 'ITEM-01-000001';

CREATE INDEX idx_pattern_lab_tenant_code
ON master_data_pattern_lab (tenant_id, code);

ANALYZE master_data_pattern_lab;

\echo 'Case A2 - Có index (tenant_id, code): search condition nên nằm trong Index Cond'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM master_data_pattern_lab
WHERE tenant_id = 1
  AND code = 'ITEM-01-000001';

/*
 * Kết luận kỳ vọng:
 * - Trước index: Seq Scan.
 * - Sau index: Index Scan hoặc Bitmap Index Scan.
 * - Nếu nhìn thấy Index Cond gồm cả tenant_id và code, nghĩa là cả hai điều kiện
 *   đều giúp truy cập index.
 */

\echo '=============================================================='
\echo '3. Case B: prefix LIKE và tenant_id index confounding'
\echo '=============================================================='

\echo 'Case B1 - Prefix LIKE khi chỉ có index (tenant_id, code)'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM master_data_pattern_lab
WHERE tenant_id = 1
  AND name LIKE 'Laptop%';

/*
 * Nếu plan dùng idx_pattern_lab_tenant_code, hãy đọc kỹ:
 * - Index Cond thường chỉ có tenant_id.
 * - name LIKE 'Laptop%' thường nằm ở Filter.
 * Điều này nghĩa là index giúp giới hạn tenant, chưa chắc giúp search theo name.
 */

CREATE INDEX idx_pattern_lab_tenant_name_prefix
ON master_data_pattern_lab (tenant_id, name text_pattern_ops);

ANALYZE master_data_pattern_lab;

\echo 'Case B2 - Prefix LIKE sau index (tenant_id, name text_pattern_ops)'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM master_data_pattern_lab
WHERE tenant_id = 1
  AND name LIKE 'Laptop%';

/*
 * Kết luận kỳ vọng:
 * - Prefix search có cơ hội dùng B-tree tốt hơn contains search.
 * - Với text_pattern_ops, điều kiện prefix có thể xuất hiện trong Index Cond
 *   dưới dạng range trên name.
 */

\echo '=============================================================='
\echo '4. Case C: leading wildcard / contains LIKE'
\echo '=============================================================='

\echo 'Case C - Contains LIKE với cùng index prefix'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM master_data_pattern_lab
WHERE tenant_id = 1
  AND name LIKE '%Dell%';

/*
 * Kết luận kỳ vọng:
 * - Plan vẫn có thể dùng index vì tenant_id.
 * - Nhưng name LIKE '%Dell%' thường nằm ở Filter, không phải Index Cond.
 * - Đây là điểm dễ nhầm: có index-assisted plan không đồng nghĩa contains search
 *   đã được B-tree index hỗ trợ tốt.
 *
 * Nếu requirement thật là contains/fuzzy search, cân nhắc pg_trgm + GIN/GiST.
 */

\echo '=============================================================='
\echo '5. Case D: lower(name) và expression index'
\echo '=============================================================='

\echo 'Case D1 - lower(name) trước expression index'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM master_data_pattern_lab
WHERE tenant_id = 1
  AND lower(name) = lower('Laptop Dell Lab 50');

CREATE INDEX idx_pattern_lab_tenant_lower_name
ON master_data_pattern_lab (tenant_id, lower(name));

ANALYZE master_data_pattern_lab;

\echo 'Case D2 - lower(name) sau expression index'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM master_data_pattern_lab
WHERE tenant_id = 1
  AND lower(name) = lower('Laptop Dell Lab 50');

/*
 * Kết luận kỳ vọng:
 * - Trước expression index: có thể chỉ dùng tenant_id rồi Filter lower(name).
 * - Sau expression index: lower(name) có thể đi vào Index Cond.
 * - Expression index hữu ích khi query thật sự dùng expression đó thường xuyên.
 */

\echo '=============================================================='
\echo '6. Case E: composite index và leftmost prefix'
\echo '=============================================================='

CREATE INDEX idx_pattern_lab_tenant_category_code
ON master_data_pattern_lab (tenant_id, category, code);

ANALYZE master_data_pattern_lab;

\echo 'Case E1 - WHERE tenant_id = ...'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM master_data_pattern_lab
WHERE tenant_id = 1;

\echo 'Case E2 - WHERE tenant_id = ... AND category = ...'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM master_data_pattern_lab
WHERE tenant_id = 1
  AND category = 'Laptop';

\echo 'Case E3 - WHERE category = ... thiếu tenant_id, chỉ quan sát leftmost prefix'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM master_data_pattern_lab
WHERE category = 'Laptop';

/*
 * Kết luận kỳ vọng:
 * - E1 có thể dùng phần tenant_id của index.
 * - E2 có thể dùng tenant_id + category tốt hơn.
 * - E3 thiếu cột leftmost tenant_id nên thường kém phù hợp với index này.
 * - Trong backend thật, E3 còn là query không tenant-aware nên nguy hiểm.
 */

\echo '=============================================================='
\echo '7. Optional: pg_trgm / GIN cho contains search'
\echo '=============================================================='

/*
 * Không bắt buộc chạy trong Phase 1.
 *
 * Nếu sau này cần search kiểu contains/fuzzy:
 * - Đọc docs pg_trgm.
 * - Cân nhắc CREATE EXTENSION IF NOT EXISTS pg_trgm.
 * - Cân nhắc GIN/GiST index với gin_trgm_ops.
 * - Đo lại bằng EXPLAIN và cân nhắc index size/write cost.
 *
 * Gợi ý KHÔNG hoàn chỉnh:
 * -- CREATE EXTENSION IF NOT EXISTS pg_trgm;
 * -- CREATE INDEX ... USING GIN (name gin_trgm_ops);
 */

\echo '=============================================================='
\echo '8. Reflection'
\echo '=============================================================='

/*
 * Tự trả lời sau khi chạy:
 *
 * 1. Query pattern nào dễ dùng B-tree index?
 *    - Exact match và prefix search phù hợp điều kiện/operator class.
 *
 * 2. Pattern nào khó dùng B-tree index?
 *    - Leading wildcard / contains LIKE như '%Dell%'.
 *
 * 3. Vì sao PostgreSQL vẫn có thể chọn Seq Scan?
 *    - Bảng nhỏ, điều kiện ít selective, statistics/cost estimate, hoặc pattern
 *      không phù hợp index.
 *
 * 4. Làm sao biết search condition thật sự dùng index?
 *    - Xem điều kiện đó có nằm trong Index Cond không.
 *    - Nếu chỉ nằm trong Filter, PostgreSQL lấy candidate rows trước rồi lọc sau.
 *
 * 5. Rule backend multi-tenant là gì?
 *    - Query nghiệp vụ phải scoped theo tenant_id từ trusted context.
 */

\echo '=============================================================='
\echo '9. Cleanup'
\echo '=============================================================='

DROP TABLE IF EXISTS master_data_pattern_lab;
