-- ==============================================================
-- TASK 6: Quan sát migration, lock và rollback mindset
-- ==============================================================
--
-- [Mục tiêu]
--
-- Bài lab này giúp mình quan sát ở mức cơ bản:
-- - migration/schema change là gì;
-- - BEGIN, COMMIT, ROLLBACK dùng thế nào khi thử migration local;
-- - vì sao cần nghĩ rollback/cleanup trước khi chạy migration;
-- - vì sao lock trong shared-table multi-tenant có thể ảnh hưởng nhiều tenant.
--
-- Đây KHÔNG phải:
-- - bài benchmark performance;
-- - hướng dẫn migration production đầy đủ;
-- - bài học sâu về mọi lock mode của PostgreSQL.
--
-- Chỉ chạy bài này ở local learning database.
-- Dù là lab local, vẫn nên đọc lệnh trước, hiểu mình đang đổi gì,
-- và chuẩn bị cleanup trước khi schema change.
--
-- Cách chạy an toàn:
--   cd lab-code
--   make db-up
--   make db-status
--   make sql-reset
--   make sql-all
--   make sql-6
--
-- Lưu ý:
-- - `make sql-6` chỉ chạy các phần an toàn/non-interactive.
-- - Phần quan sát lock hai terminal nằm trong comment để tự làm thủ công.
--
-- ==============================================================


-- ==============================================================
-- 0. Cleanup trước để script có thể chạy lại
-- ==============================================================
--
-- Nếu lần chạy trước bị dừng giữa chừng, các cột lab-only có thể còn lại.
-- Dọn trước giúp bài lab dễ rerun trên local.

ALTER TABLE master_data DROP COLUMN IF EXISTS lab_observation;
ALTER TABLE master_data DROP COLUMN IF EXISTS lab_rollback_test;
ALTER TABLE master_data DROP COLUMN IF EXISTS lab_lock_observation;


-- ==============================================================
-- 1. Baseline pre-check
-- ==============================================================
--
-- Mục tiêu:
-- Xác nhận `sql-1` và `sql-2` đã tạo baseline đúng trước khi thử
-- schema change.

-- Inspect các cột hiện tại của bảng `master_data`.
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'master_data'
ORDER BY ordinal_position;

-- Đếm số dòng sample hiện có.
SELECT COUNT(*) AS master_data_row_count
FROM master_data;

-- Inspect index/constraint-backed index hiện có.
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'master_data'
ORDER BY indexname;

-- Nhận xét:
-- - Baseline đúng khi có các cột nghiệp vụ chính như `tenant_id`, `code`,
--   `name`, `category`.
-- - Dữ liệu sample hiện tại nhỏ, nhưng đủ để quan sát schema change an toàn.
-- - Index từ PRIMARY KEY/UNIQUE constraint là một phần của business schema,
--   không nên drop chỉ để thử nghiệm migration.


-- ==============================================================
-- 2. Thử migration trong transaction với BEGIN + ROLLBACK
-- ==============================================================
--
-- Mục tiêu:
-- Thấy rằng PostgreSQL cho phép thử một số DDL trong transaction.
-- Nếu dùng ROLLBACK, thay đổi schema trong transaction sẽ không được giữ lại.
--
-- BEGIN:
-- - mở một transaction mới;
-- - các lệnh sau đó nằm trong cùng một đơn vị thay đổi.
--
-- ROLLBACK:
-- - hủy các thay đổi chưa COMMIT trong transaction;
-- - hữu ích khi thử migration ở local để quan sát mà không giữ lại schema rác.
--
-- Đây là cách học local, không thay thế quy trình migration thật bằng Flyway.

BEGIN;

ALTER TABLE master_data
ADD COLUMN lab_rollback_test VARCHAR(255);

-- Trong transaction, cột tạm có thể được nhìn thấy ở chính session này.
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'master_data'
  AND column_name = 'lab_rollback_test';

ROLLBACK;

-- Sau ROLLBACK, cột `lab_rollback_test` không còn tồn tại.
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'master_data'
  AND column_name = 'lab_rollback_test';

-- Kết luận ngắn:
-- - BEGIN + ROLLBACK hữu ích để học và thử thay đổi local.
-- - Nếu thay ROLLBACK bằng COMMIT, schema change sẽ được giữ lại.
-- - Với migration production, rollback thường không đơn giản như một lệnh
--   ROLLBACK sau khi migration đã được deploy và dữ liệu mới đã phát sinh.


-- ==============================================================
-- 3. Safe/simple migration observation
-- ==============================================================
--
-- Mục tiêu:
-- Thử một schema change nhỏ, dễ cleanup: thêm một cột nullable phục vụ lab.
--
-- Vì sao chọn nullable column?
-- - Không bắt dữ liệu cũ phải có giá trị ngay.
-- - Dễ quan sát schema change mà không cần backfill phức tạp.
-- - Phù hợp để học mindset "thêm nhỏ, tương thích ngược".

ALTER TABLE master_data
ADD COLUMN lab_observation VARCHAR(255);

-- Inspect lại schema sau migration nhỏ.
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'master_data'
ORDER BY ordinal_position;

-- Query dữ liệu cũ để kiểm tra dữ liệu vẫn còn.
SELECT COUNT(*) AS master_data_row_count_after_add_column
FROM master_data;

-- Reflection:
-- - Schema change thành công khi cột `lab_observation` xuất hiện.
-- - Dữ liệu cũ vẫn được giữ lại; các row cũ có giá trị NULL ở cột mới.
-- - Nếu đây là bảng shared-table rất lớn trong SaaS, cần cân nhắc lock,
--   thời điểm chạy migration, khả năng rollback, backfill và việc code cũ/mới
--   có cùng chạy được trong giai đoạn chuyển tiếp không.


-- ==============================================================
-- 4. Rollback/cleanup mindset
-- ==============================================================
--
-- Mục tiêu:
-- Trước khi migration, phải nghĩ đường quay lại hoặc cleanup.
--
-- Với bài lab này, cleanup đơn giản là drop cột lab-only.
-- Nếu migration đã ghi dữ liệu thật vào cột mới, rollback sẽ khó hơn:
-- cần nghĩ đến dữ liệu đã sinh ra, code đang chạy, và migration tiếp theo.

ALTER TABLE master_data
DROP COLUMN IF EXISTS lab_observation;

-- Kiểm tra cột lab đã được dọn.
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'master_data'
  AND column_name = 'lab_observation';

-- Kết luận:
-- - Rollback/cleanup nên được nghĩ trước, không phải đợi lỗi mới nghĩ.
-- - Migration tốt nên nhỏ, có thể kiểm chứng, và tương thích ngược nếu có thể.
-- - Feature flag hoặc deploy nhiều bước giúp giảm rủi ro hơn rollback DB vội.


-- ==============================================================
-- 5. Lock observation: cần hai terminal/session
-- ==============================================================
--
-- Mục tiêu:
-- Quan sát rằng một transaction/schema operation có thể giữ lock và làm
-- session khác phải chờ.
--
-- Phần này KHÔNG chạy tự động bằng `make sql-6`, vì một file SQL chạy trong
-- một session không thể mô phỏng đúng hai terminal đang cạnh tranh lock.
-- Hãy tự chạy thủ công nếu muốn quan sát blocking thật.
--
-- --------------------------------------------------------------
-- Terminal / Session A
-- --------------------------------------------------------------
--
-- cd lab-code
-- make db-shell
--
-- BEGIN;
-- ALTER TABLE master_data ADD COLUMN lab_lock_observation VARCHAR(255);
-- -- Giữ session A mở, chưa COMMIT/ROLLBACK.
--
-- --------------------------------------------------------------
-- Terminal / Session B
-- --------------------------------------------------------------
--
-- cd lab-code
-- make db-shell
--
-- SELECT COUNT(*) FROM master_data;
--
-- Optional: nhìn activity/lock ở mức cơ bản.
--
-- SELECT pid, state, wait_event_type, wait_event, query
-- FROM pg_stat_activity
-- WHERE datname = current_database();
--
-- --------------------------------------------------------------
-- Kết thúc quan sát
-- --------------------------------------------------------------
--
-- Quay lại Session A:
--
-- ROLLBACK;
--
-- Nếu lỡ COMMIT thay vì ROLLBACK, cleanup bằng:
--
-- ALTER TABLE master_data DROP COLUMN IF EXISTS lab_lock_observation;
--
-- Ghi chú:
-- - Không cần thuộc tên mọi lock mode ở giai đoạn này.
-- - Điều cần thấy là DDL trong transaction có thể giữ lock và làm việc khác
--   bị chờ/block.
-- - Nếu chưa chạy phần hai terminal, không nên tự bịa timing/output; chỉ ghi
--   lại sau khi quan sát local thật.


-- ==============================================================
-- 6. Multi-tenant impact reflection
-- ==============================================================
--
-- 1. Vì sao một `ALTER TABLE master_data` có thể ảnh hưởng tất cả tenant
--    trong mô hình shared-table?
-- Trả lời:
-- Vì tất cả tenant cùng dùng chung một bảng vật lý `master_data`. Một schema
-- change hoặc lock trên bảng này không chỉ tác động một tenant, mà có thể tác
-- động mọi request đang đọc/ghi dữ liệu trong bảng chung đó.
--
-- 2. Vì sao lock lâu nguy hiểm trong SaaS?
-- Trả lời:
-- Lock lâu có thể làm request bị chờ, timeout hoặc giảm trải nghiệm của nhiều
-- khách hàng cùng lúc. Trong SaaS, blast radius của một thay đổi DB sai có thể
-- rộng hơn một tenant đơn lẻ.
--
-- 3. Vì sao migration nên backward-compatible nếu có thể?
-- Trả lời:
-- Vì code cũ và code mới có thể cùng tồn tại trong lúc rolling deploy. Thay đổi
-- tương thích ngược giúp deploy nhiều bước, giảm rủi ro phải rollback database
-- ngay khi code mới gặp lỗi.
--
-- 4. Vì sao nên test migration trên local/staging data trước production?
-- Trả lời:
-- Vì local/staging giúp phát hiện lỗi cú pháp, dữ liệu cũ không phù hợp, thời
-- gian chạy quá lâu hoặc khả năng lock/block trước khi ảnh hưởng người dùng thật.


-- ==============================================================
-- 7. Final summary block
-- ==============================================================
--
-- 1. Tôi đã quan sát được gì?
-- Trả lời:
-- Đã quan sát được việc thêm cột nullable nhỏ, kiểm tra schema sau migration,
-- dọn cột lab-only, và dùng BEGIN + ROLLBACK để thử DDL mà không giữ lại thay
-- đổi. Phần blocking hai terminal cần được ghi nhận từ quan sát thủ công nếu
-- muốn có output cụ thể.
--
-- 2. Điều gì làm tôi bất ngờ?
-- Trả lời:
-- Một lệnh `ALTER TABLE` nhìn rất đơn giản nhưng vẫn là thay đổi trên bảng
-- nghiệp vụ chung. Trong shared-table multi-tenant, việc nhỏ ở schema có thể
-- có tác động lớn nếu bảng lớn hoặc đang có nhiều request.
--
-- 3. Rule tôi cần nhớ cho backend work sau này là gì?
-- Trả lời:
-- Trước khi chạy migration, luôn hỏi: thay đổi này có tương thích ngược không,
-- có cleanup/rollback plan không, có thể block bảng chung bao lâu, và đã được
-- thử trên local/staging chưa.


-- ==============================================================
-- 8. Cleanup cuối
-- ==============================================================
--
-- Dọn mọi cột lab-only để database quay về baseline sau bài học.

ALTER TABLE master_data DROP COLUMN IF EXISTS lab_observation;
ALTER TABLE master_data DROP COLUMN IF EXISTS lab_rollback_test;
ALTER TABLE master_data DROP COLUMN IF EXISTS lab_lock_observation;

-- Nếu local database bị rối, restore baseline bằng:
--
--   cd lab-code
--   make sql-reset
--   make sql-all
