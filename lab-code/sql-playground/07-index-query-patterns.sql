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
 * Cách dùng đề xuất:
 * 1. Chạy baseline trước:
 *      make sql-reset
 *      make sql-all
 * 2. Mở file này và tự điền từng TODO.
 * 3. Chạy từng phần, đọc EXPLAIN, ghi nhận scan type.
 *
 * Lưu ý:
 * - Đây là guided skeleton. Không có full lời giải sẵn.
 * - Nếu bảng nhỏ, PostgreSQL có thể chọn Seq Scan dù có index.
 * - Sau khi tạo/sinh nhiều dữ liệu hoặc tạo index, nhớ ANALYZE.
 *
 * ==============================================================
 */

-- ==============================================================
-- 0. Pre-check: đọc lại bảng thật
-- ==============================================================

-- TODO:
-- - Inspect bảng master_data hiện có bao nhiêu dòng.
-- - Inspect index/constraint hiện có trên master_data.
-- - Nhắc lại vì sao không drop constraint thật UNIQUE (tenant_id, code).

SELECT COUNT(*) FROM master_data;
SELECT schemaname, tablename, indexname, indexdef
FROM pg_indexes
WHERE tablename = 'master_data'
  AND schemaname = 'public'
ORDER by indexname;

-- Gợi ý một phần:
-- SELECT COUNT(*) FROM master_data;
-- SELECT indexname, indexdef FROM pg_indexes WHERE tablename = 'master_data';


-- ==============================================================
-- 1. Chuẩn bị bảng tạm cho lab
-- ==============================================================

-- TODO:
-- - Tạo TEMP TABLE master_data_pattern_lab copy từ master_data.
-- - Sinh thêm dữ liệu vừa đủ lớn để planner có cơ hội chọn index.
-- - Dữ liệu nên có nhiều tenant, nhiều category, name/code có prefix lặp lại.
-- - Không cần tạo constraint business trên bảng tạm.
-- - Sau khi chuẩn bị dữ liệu, chạy ANALYZE.

-- Gợi ý một phần:
-- DROP TABLE IF EXISTS master_data_pattern_lab;
-- CREATE TEMP TABLE master_data_pattern_lab AS
-- SELECT * FROM master_data;

-- TODO:
-- - Dùng generate_series để thêm dữ liệu lab.
-- - Có thể tạo code dạng 'ITEM-' || tenant_id || '-' || n.
-- - Có thể tạo name dạng 'Laptop ...', 'Monitor ...', 'Mouse ...'
--   để test prefix/contains search.

-- TODO:
-- ANALYZE master_data_pattern_lab;

DROP TABLE IF EXISTS master_data_pattern_lab;

CREATE TEMP TABLE master_data_pattern_lab AS
SELECT id, tenant_id, code, name, category, is_active, created_at
FROM master_data;

INSERT INTO master_data_pattern_lab (id, tenant_id, code, name, category, is_active, created_at)
SELECT
    1000000 + gs AS id,
    (gs - 1 % 100) + 1 AS tenant_id, -- 100 tenants
    'ITEM-' || lpad(gs::text, 8, '0') AS code,
    'Du lieu lab ' || gs AS name,
    CASE gs % 5
        WHEN 0 THEN 'Laptop'
        WHEN 1 THEN 'Monitor'
        WHEN 2 THEN 'Mouse'
        WHEN 3 THEN 'Keyboard'
        ELSE 'Accessory'
    END AS category,
    TRUE AS is_active,
    now() - ((gs % 365) * interval '1 day') AS created_at
FROM generate_series(1, 5000000) AS gs; -- 5 triệu bản ghi

-- ==============================================================
-- 2. Case A: exact match và B-tree index
-- ==============================================================

-- Mục tiêu:
-- - So sánh query exact match trước/sau khi có index.
-- - Quan sát Seq Scan, Index Scan hoặc Bitmap Scan.

-- TODO:
-- - Chạy EXPLAIN (ANALYZE, BUFFERS) với:
--   WHERE tenant_id = ... AND code = ...
-- - Ghi nhận plan khi chưa có index thử nghiệm.
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM master_data_pattern_lab
WHERE tenant_id = 1 AND code = 'ITEM-00000001';
-- Chạy Seq Scan

-- TODO:
-- - Tạo index phù hợp trên bảng tạm.
-- - Chạy ANALYZE.
-- - Chạy lại cùng query.
CREATE INDEX idx_master_data_pattern_lab_code ON master_data_pattern_lab (tenant_id, code);

EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM master_data_pattern_lab
WHERE tenant_id = 1 AND code = 'ITEM-00000001';

-- Câu hỏi tự trả lời:
-- - Scan type thay đổi không? -- Thay đổi sang Bitmap Scan.
-- - Estimated rows và actual rows có gần nhau không? -- Với riêng query hiện tại thì lệch rất nhiều
-- - Query có selective không? -- Rất selective, chỉ trả về 1 dòng.


-- ==============================================================
-- 3. Case B: prefix LIKE
-- ==============================================================

-- Mục tiêu:
-- - Quan sát pattern dạng name LIKE 'Lap%'.
-- - Đây là prefix search, PostgreSQL có thể cân nhắc B-tree index
--   nếu điều kiện/collation/operator class phù hợp.

-- TODO:
-- - Chạy EXPLAIN cho query tenant-aware:
--   WHERE tenant_id = ... AND name LIKE 'Lap%'
-- - Tạo index phù hợp để thử.
-- - Nếu PostgreSQL vẫn chọn Seq Scan, ghi lại giả thuyết:
--   bảng nhỏ, selectivity thấp, collation/operator class, hoặc cost estimate.
CREATE INDEX idx_master_data_pattern_lab_category ON master_data_pattern_lab (tenant_id, category);

EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM master_data_pattern_lab
WHERE tenant_id = 1 AND category LIKE 'Lap%';

-- Không cần xử lý mọi vấn đề collation trong bài này.
-- Chỉ cần biết prefix search khác contains search.


-- ==============================================================
-- 4. Case C: leading wildcard / contains LIKE
-- ==============================================================

-- Mục tiêu:
-- - Quan sát pattern name LIKE '%top%' hoặc '%Laptop%'.
-- - B-tree index thường không phù hợp tự nhiên với contains search.

-- TODO:
-- - Chạy EXPLAIN cho:
--   WHERE tenant_id = ... AND name LIKE '%top%'
-- - So sánh với prefix search ở Case B.

EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM master_data_pattern_lab
WHERE tenant_id = 1 AND category LIKE '%top%';

-- Câu hỏi tự trả lời:
-- - PostgreSQL có dùng B-tree index không?
-- - Nếu vẫn Seq Scan, điều đó có hợp lý không?
-- - Vì sao contains search thường cần chiến lược khác?

-- Thấy cả 2 vẫn sử dụng Bitmap Heap Scan, chắc là nhờ Codex tự chạy và review lại

-- ==============================================================
-- 5. Case D: expression index với lower(name)
-- ==============================================================

-- Mục tiêu:
-- - Hiểu vì sao query dùng lower(name) có thể cần expression index.

-- TODO:
-- - Chạy EXPLAIN cho:
--   WHERE tenant_id = ... AND lower(name) = lower('...')
-- - Sau đó tự tạo expression index phù hợp trên bảng tạm.
-- - Chạy ANALYZE và chạy lại EXPLAIN.
EXPLAIN(ANALYZE, BUFFERS)
SELECT * FROM master_data_pattern_lab
WHERE tenant_id = 1 AND lower(name) = lower('Du lieu lab 1');

CREATE INDEX idx_master_data_pattern_lab_lower_name ON master_data_pattern_lab (tenant_id, lower(name));
EXPLAIN(ANALYZE, BUFFERS)
SELECT * FROM master_data_pattern_lab
WHERE tenant_id = 1 AND lower(name) = lower('Du lieu lab 1');

-- Câu hỏi tự trả lời:
-- - Index thường trên name có đủ không?
-- - Expression index có giúp query lower(name) không?
-- - Trade-off khi dùng expression index là gì?


-- ==============================================================
-- 6. Case E: composite index và leftmost prefix
-- ==============================================================

-- Mục tiêu:
-- - Quan sát index nhiều cột như (tenant_id, category, code/name).
-- - Hiểu cột bên trái trong B-tree multicolumn index quan trọng thế nào.

-- TODO:
-- - Tạo một composite index trên bảng tạm, ví dụ bắt đầu bằng tenant_id.
-- - Chạy EXPLAIN cho các pattern:
--   1. WHERE tenant_id = ...
--   2. WHERE tenant_id = ... AND category = ...
--   3. WHERE category = ...    -- thiếu tenant_id, chỉ dùng để quan sát
--
-- Lưu ý:
-- - Pattern 3 không phải query backend an toàn.
-- - Dùng nó để quan sát leftmost prefix, không dùng làm design production.

-- Câu hỏi tự trả lời:
-- - Pattern nào tận dụng index tốt hơn?
-- - Vì sao query thiếu tenant_id vừa nguy hiểm, vừa có thể không hợp index?
CREATE INDEX idx_master_data_pattern_lab_tenant_category_code ON master_data_pattern_lab (tenant_id, category, code);

EXPLAIN(ANALYZE, BUFFERS)
SELECT * FROM master_data_pattern_lab
WHERE tenant_id = 1;

EXPLAIN(ANALYZE, BUFFERS)
SELECT * FROM master_data_pattern_lab
WHERE tenant_id = 1 AND category = 'Laptop';

EXPLAIN(ANALYZE, BUFFERS)
SELECT * FROM master_data_pattern_lab
WHERE category = 'Laptop';

-- ==============================================================
-- 7. Optional: pg_trgm / GIN cho contains search
-- ==============================================================

-- Mục tiêu:
-- - Chỉ nhận biết hướng xử lý khi cần contains/fuzzy search thật.
-- - Không bắt buộc làm trong Phase 1 nếu chưa sẵn sàng.

-- TODO optional:
-- - Đọc docs PostgreSQL về pg_trgm.
-- - Nếu database cho phép CREATE EXTENSION, thử trên bảng tạm.
-- - Nếu không làm, ghi comment giải thích khi nào sẽ cân nhắc pg_trgm/GIN.

-- Gợi ý KHÔNG hoàn chỉnh:
-- -- CREATE EXTENSION IF NOT EXISTS pg_trgm;
-- -- CREATE INDEX ... USING GIN (... gin_trgm_ops);

-- ==============================================================
-- 8. Reflection
-- ==============================================================

-- TODO trả lời ngắn sau khi tự chạy:
-- - Query pattern nào dễ dùng B-tree index?
-- - Pattern nào khó dùng B-tree index?
-- - Vì sao PostgreSQL vẫn có thể chọn Seq Scan?
-- - Composite index leftmost prefix ảnh hưởng gì?
-- - Trong backend multi-tenant, rule query tenant-aware là gì?
-- - Khi nào nên cân nhắc pg_trgm/GIN thay vì B-tree?


-- ==============================================================
-- 9. Cleanup
-- ==============================================================

-- TODO:
-- - Drop bảng tạm nếu muốn cleanup rõ ràng.
-- - TEMP TABLE cũng tự biến mất khi session kết thúc, nhưng explicit cleanup
--   giúp lab dễ đọc hơn.

-- Gợi ý:
DROP TABLE IF EXISTS master_data_pattern_lab;
