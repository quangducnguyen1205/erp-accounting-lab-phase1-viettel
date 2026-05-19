-- ==============================================================
-- 09-acid-isolation-observation.sql
-- ==============================================================
--
-- Mục tiêu:
-- - Quan sát transaction visibility và isolation level ở mức cơ bản.
-- - Hiểu READ COMMITTED khác REPEATABLE READ thế nào qua 2 session.
-- - Liên hệ với shared-table multi-tenant mà không đụng dữ liệu demo thật.
--
-- Đây KHÔNG phải bài benchmark hay bài đào sâu lock internals.
-- Các phần quan trọng cần tự thao tác bằng hai terminal/session psql.
--
-- Cách chuẩn bị:
--   cd lab-code
--   make db-up
--   make db-status
--   make sql-9
--
-- Sau đó mở 2 terminal:
--   Terminal A: make db-shell
--   Terminal B: make db-shell
--
-- Ghi chú:
-- - TEMP table không phù hợp cho bài này vì mỗi session có temp table riêng.
-- - Vì vậy dùng bảng lab riêng `acid_isolation_lab`.
-- - Bảng này chỉ chứa dữ liệu học local và có thể drop sau khi xong.
-- ==============================================================

-- ==============================================================
-- 0. Safe setup chạy được bằng `make sql-9`
-- ==============================================================

DROP TABLE IF EXISTS acid_isolation_lab;

CREATE TABLE acid_isolation_lab (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    account_code VARCHAR(50) NOT NULL,
    balance NUMERIC(12, 2) NOT NULL,
    note VARCHAR(255) NOT NULL
);

INSERT INTO acid_isolation_lab (tenant_id, account_code, balance, note)
VALUES
    (1, 'CASH', 1000.00, 'Tenant 1 baseline'),
    (2, 'CASH', 2000.00, 'Tenant 2 baseline');

SELECT tenant_id, account_code, balance, note
FROM acid_isolation_lab
ORDER BY tenant_id;

-- TODO: chạy và ghi lại isolation level mặc định hiện tại.
SHOW transaction_isolation;
-- read committed

-- ==============================================================
-- 1. Transaction visibility + ROLLBACK
-- ==============================================================
-- Mục tiêu:
-- - Thấy rằng transaction khác không đọc được thay đổi chưa commit.
-- - Thấy ROLLBACK hủy thay đổi chưa commit.
--
-- Terminal A:
  BEGIN;
  UPDATE acid_isolation_lab
  SET balance = balance + 100
  WHERE tenant_id = 1 AND account_code = 'CASH';
  SELECT tenant_id, account_code, balance
  FROM acid_isolation_lab
  WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- Terminal B:
  SELECT tenant_id, account_code, balance
  FROM acid_isolation_lab
  WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- TODO tự quan sát:
-- - Terminal A thấy balance nào?
-- Thấy balance sau khi đã update, tức là 1100.00.
-- - Terminal B có thấy +100 trước khi A commit không?
-- Không thấy, vẫn là 1000.00.
-- - Đây liên hệ gì với dirty read?
-- Không có dirty read vì Terminal B không đọc được thay đổi chưa commit của Terminal A.
-- Theo như tôi chạy thì cũng không có các thuộc tính còn lại như non-repeatable read hay
-- phantom read, vì Terminal A chỉ thực hiện một UPDATE và một SELECT, và Terminal B chỉ
-- thực hiện một SELECT. Tuy nhiên, nếu Terminal A thực hiện thêm một UPDATE hoặc một SELECT
-- khác sau khi Terminal B đã thực hiện SELECT đầu tiên, thì có thể sẽ thấy các hiện tượng này
-- tùy thuộc vào isolation level.
--
-- Terminal A:
  ROLLBACK;
--
-- Terminal B:
  SELECT tenant_id, account_code, balance
  FROM acid_isolation_lab
  WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- TODO: sau ROLLBACK, balance trở về bao nhiêu?

-- ==============================================================
-- 2. READ COMMITTED: hai SELECT trong cùng transaction có thể khác nhau
-- ==============================================================
--
-- Mục tiêu:
-- - Quan sát default PostgreSQL READ COMMITTED.
-- - Mỗi statement có snapshot tại lúc statement bắt đầu.
--
-- Terminal A:
  BEGIN ISOLATION LEVEL READ COMMITTED;
  SELECT tenant_id, account_code, balance
  FROM acid_isolation_lab
  WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- Terminal B:
  UPDATE acid_isolation_lab
  SET balance = balance + 50
  WHERE tenant_id = 1 AND account_code = 'CASH';
--   -- Nếu không viết BEGIN, statement này tự commit khi thành công.
--
-- Terminal A:
  SELECT tenant_id, account_code, balance
  FROM acid_isolation_lab
  WHERE tenant_id = 1 AND account_code = 'CASH';
  COMMIT;
--
-- TODO tự quan sát:
-- - Hai lần SELECT ở Terminal A có giống nhau không?
-- Khác nhau, lần đầu thấy balance là 1000.00, lần thứ hai thấy balance là 1050.00.
-- - Đây là ví dụ của hiện tượng nào?
-- Vì đây là xem các bản ghi nên sẽ là non-repeatable read.
-- - Vì sao READ COMMITTED vẫn không phải dirty read?
-- Vì Terminal A không đọc được thay đổi chưa commit của Terminal B, nên không có dirty read.

-- ==============================================================
-- 3. Optional: REPEATABLE READ giữ snapshot ổn định
-- ==============================================================
--
-- Mục tiêu:
-- - Thấy sự khác biệt với READ COMMITTED.
--
-- Terminal A:
  BEGIN ISOLATION LEVEL REPEATABLE READ;
  SELECT tenant_id, account_code, balance
  FROM acid_isolation_lab
  WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- Terminal B:
  UPDATE acid_isolation_lab
  SET balance = balance + 25
  WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- Terminal A:
  SELECT tenant_id, account_code, balance
  FROM acid_isolation_lab
  WHERE tenant_id = 1 AND account_code = 'CASH';
  COMMIT;
--
-- Terminal A sau COMMIT:
  SELECT tenant_id, account_code, balance
  FROM acid_isolation_lab
  WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- TODO tự quan sát:
-- - Trong transaction REPEATABLE READ, lần SELECT thứ hai có thấy +25 không?
-- Không thấy, vẫn là balance trước khi Terminal B update, tức là 1050.00.
-- - Sau COMMIT và query mới, giá trị có thay đổi không?
-- Có, giá trị được thay đổi thành 1075.00.
-- ==============================================================
-- 4. Optional: cùng shared table nhưng khác tenant
-- ==============================================================
--
-- Mục tiêu:
-- - Nhắc lại tenant-aware filtering và concurrency là hai vấn đề khác nhau.
--
-- Terminal A:
  BEGIN;
  UPDATE acid_isolation_lab
  SET balance = balance + 10
  WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- Terminal B:
  UPDATE acid_isolation_lab
  SET balance = balance + 10
  WHERE tenant_id = 2 AND account_code = 'CASH';
--
-- TODO tự suy nghĩ:
-- - Hai câu UPDATE này đụng cùng bảng nhưng có đụng cùng row không?
-- Không đụng cùng row vì tenant_id khác nhau, nên mỗi câu UPDATE chỉ ảnh hưởng đến một row riêng biệt.
-- - Nếu Terminal B cũng update tenant_id = 1 thì điều gì có thể xảy ra?
-- Tôi nghĩ là do PostgreSQL mặc định sẽ là read committed, nên nếu terminal B cũng update
-- tenant_id = 1 thì có thể bị lock không nhỉ, hay là sẽ chạy được cái update của terminal B dẫn tới
-- không consistent giữa các biến
-- - Vì sao shared-table SaaS vẫn cần transaction mindset dù query đã có tenant_id?
--
--
-- Terminal A:
  ROLLBACK;
--
-- Gợi ý:
-- - Không cần học toàn bộ lock mode ở bài này.
-- - Chỉ cần hiểu: cùng bảng chưa chắc cùng row; cùng row thì concurrency conflict dễ xuất hiện hơn.

-- ==============================================================
-- 5. Reflection tự điền sau khi làm
-- ==============================================================
--
-- TODO:
-- - PostgreSQL default isolation level là gì?
-- - Dirty read có xảy ra trong các quan sát trên không?
-- - READ COMMITTED cho phép hiện tượng gì?
-- - REPEATABLE READ khác READ COMMITTED ở điểm nào?
-- - Tenant isolation và transaction isolation khác nhau thế nào?
-- - Với backend SME SaaS, rule nào mình cần nhớ nhất?

-- ==============================================================
-- 6. Cleanup sau khi hoàn thành
-- ==============================================================
--
-- Khi đã ghi xong kết luận, có thể chạy:
--   DROP TABLE IF EXISTS acid_isolation_lab;
--
-- Không drop tự động ở cuối file, vì bảng cần còn tồn tại để hai session cùng quan sát.
