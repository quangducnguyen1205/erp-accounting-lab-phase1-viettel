-- ==============================================================
-- 09-acid-isolation-observation.sql
-- ==============================================================
--
-- Mục tiêu:
-- - Quan sát ACID/isolation bằng ví dụ local nhỏ.
-- - Thấy READ COMMITTED, REPEATABLE READ, row lock và SELECT FOR UPDATE.
-- - Liên hệ với shared-table multi-tenant mà không đụng dữ liệu demo thật.
--
-- Cách chạy setup:
--   cd lab-code
--   make db-up
--   make db-status
--   make sql-9
--
-- Sau đó mở 2 terminal:
--   Terminal A: make db-shell
--   Terminal B: make db-shell
--
-- Quan trọng:
-- - `make sql-9` chỉ setup bảng lab và in trạng thái ban đầu.
-- - Các phần Session A/B bên dưới là hướng dẫn copy thủ công, không chạy tự động.
-- - TEMP table không phù hợp ở đây vì mỗi session có temp table riêng.
-- - Bảng lab dùng riêng cho học local, có thể DROP sau khi xong.
-- ==============================================================

-- ==============================================================
-- 0. Safe setup chạy được bằng `make sql-9`
-- ==============================================================

DROP TABLE IF EXISTS acid_booking_lab;
DROP TABLE IF EXISTS acid_isolation_lab;

CREATE TABLE acid_isolation_lab (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    account_code VARCHAR(50) NOT NULL,
    balance NUMERIC(12, 2) NOT NULL,
    note VARCHAR(255) NOT NULL,
    CONSTRAINT uq_acid_isolation_tenant_account UNIQUE (tenant_id, account_code)
);

CREATE INDEX idx_acid_isolation_tenant_account
ON acid_isolation_lab (tenant_id, account_code);

INSERT INTO acid_isolation_lab (tenant_id, account_code, balance, note)
VALUES
    (1, 'CASH', 1000.00, 'Tenant 1 cash baseline'),
    (1, 'BANK', 5000.00, 'Tenant 1 bank baseline'),
    (2, 'CASH', 2000.00, 'Tenant 2 cash baseline');

CREATE TABLE acid_booking_lab (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    booking_code VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_acid_booking_tenant
ON acid_booking_lab (tenant_id);

SELECT tenant_id, account_code, balance, note
FROM acid_isolation_lab
ORDER BY tenant_id, account_code;

SHOW transaction_isolation;

-- Expected setup result:
-- - Có 3 row: tenant 1/CASH, tenant 1/BANK, tenant 2/CASH.
-- - PostgreSQL default isolation thường là `read committed`.

-- ==============================================================
-- 1. Atomicity + ROLLBACK visibility
-- ==============================================================
--
-- Mục tiêu:
-- - Thấy transaction khác không đọc được thay đổi chưa commit.
-- - Thấy ROLLBACK hủy thay đổi.
-- - Không có dirty read trong PostgreSQL.
--
-- Session A:
--   BEGIN;
--   UPDATE acid_isolation_lab
--   SET balance = balance + 100
--   WHERE tenant_id = 1 AND account_code = 'CASH';
--
--   SELECT tenant_id, account_code, balance
--   FROM acid_isolation_lab
--   WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- Expected A:
--   balance = 1100.00 vì transaction thấy update của chính nó.
--
-- Session B, trong lúc A chưa COMMIT/ROLLBACK:
--   SELECT tenant_id, account_code, balance
--   FROM acid_isolation_lab
--   WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- Expected B:
--   balance vẫn là 1000.00. B không thấy dữ liệu chưa commit của A.
--
-- Session A:
--   ROLLBACK;
--
-- Session B:
--   SELECT tenant_id, account_code, balance
--   FROM acid_isolation_lab
--   WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- Expected B:
--   balance vẫn là 1000.00.
--
-- Kết luận:
-- - Đây là Atomicity: update +100 không được giữ lại sau rollback.
-- - Đây cũng cho thấy PostgreSQL không cho dirty read.

-- ==============================================================
-- 2. COMMIT visibility
-- ==============================================================
--
-- Mục tiêu:
-- - Thấy COMMIT làm thay đổi trở thành trạng thái thật.
--
-- Session A:
--   BEGIN;
--   UPDATE acid_isolation_lab
--   SET balance = balance + 50
--   WHERE tenant_id = 1 AND account_code = 'CASH';
--   COMMIT;
--
-- Session B:
--   SELECT tenant_id, account_code, balance
--   FROM acid_isolation_lab
--   WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- Expected B:
--   balance = 1050.00.

-- ==============================================================
-- 3. READ COMMITTED: non-repeatable read
-- ==============================================================
--
-- Mục tiêu:
-- - Thấy mỗi statement trong READ COMMITTED có snapshot mới.
-- - Hai SELECT trong cùng transaction có thể thấy giá trị khác nhau.
--
-- Nếu muốn reset trước case này:
--   UPDATE acid_isolation_lab
--   SET balance = 1000.00
--   WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- Session A:
--   BEGIN ISOLATION LEVEL READ COMMITTED;
--   SELECT tenant_id, account_code, balance
--   FROM acid_isolation_lab
--   WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- Session B:
--   UPDATE acid_isolation_lab
--   SET balance = balance + 50
--   WHERE tenant_id = 1 AND account_code = 'CASH';
--   -- Không viết BEGIN nên statement tự commit khi thành công.
--
-- Session A:
--   SELECT tenant_id, account_code, balance
--   FROM acid_isolation_lab
--   WHERE tenant_id = 1 AND account_code = 'CASH';
--   COMMIT;
--
-- Expected:
-- - SELECT đầu của A thấy 1000.00 nếu đã reset.
-- - SELECT sau của A thấy 1050.00 sau khi B commit.
-- - Đây là non-repeatable read.
-- - Vẫn không phải dirty read, vì A chỉ thấy dữ liệu B sau khi B commit.

-- ==============================================================
-- 4. REPEATABLE READ: stable snapshot
-- ==============================================================
--
-- Mục tiêu:
-- - Thấy snapshot ổn định trong transaction.
--
-- Reset nếu cần:
--   UPDATE acid_isolation_lab
--   SET balance = 1000.00
--   WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- Session A:
--   BEGIN ISOLATION LEVEL REPEATABLE READ;
--   SELECT tenant_id, account_code, balance
--   FROM acid_isolation_lab
--   WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- Session B:
--   UPDATE acid_isolation_lab
--   SET balance = balance + 25
--   WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- Session A:
--   SELECT tenant_id, account_code, balance
--   FROM acid_isolation_lab
--   WHERE tenant_id = 1 AND account_code = 'CASH';
--   COMMIT;
--
-- Session A sau COMMIT, query mới:
--   SELECT tenant_id, account_code, balance
--   FROM acid_isolation_lab
--   WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- Expected:
-- - Trong transaction REPEATABLE READ, A vẫn thấy snapshot cũ.
-- - Sau COMMIT và query mới, A thấy update của B.

-- ==============================================================
-- 5. UPDATE cùng row gây waiting/blocking
-- ==============================================================
--
-- Mục tiêu:
-- - Hiểu lock thực dụng: write cùng row có thể chờ nhau.
-- - Normal SELECT vẫn có thể đọc snapshot cũ.
--
-- Reset nếu cần:
--   UPDATE acid_isolation_lab
--   SET balance = 1000.00
--   WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- Session A:
--   BEGIN;
--   UPDATE acid_isolation_lab
--   SET balance = balance + 10
--   WHERE tenant_id = 1 AND account_code = 'CASH';
--   -- Giữ transaction mở, chưa COMMIT/ROLLBACK.
--
-- Session B:
--   SELECT tenant_id, account_code, balance
--   FROM acid_isolation_lab
--   WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- Expected B:
--   SELECT thường không bị block; nó thấy giá trị committed cũ.
--
-- Session B:
--   UPDATE acid_isolation_lab
--   SET balance = balance + 20
--   WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- Expected B:
--   UPDATE bị chờ cho tới khi A COMMIT hoặc ROLLBACK.
--
-- Session A:
--   COMMIT;
--
-- Expected:
-- - Sau khi A COMMIT, B tiếp tục chạy UPDATE.
-- - Nếu A ROLLBACK, B tiếp tục dựa trên giá trị cũ.
--
-- Kết luận:
-- - Không phải transaction nào cũng block nhau.
-- - Row write cùng row mới là case dễ thấy blocking.

-- ==============================================================
-- 6. SELECT FOR UPDATE row lock
-- ==============================================================
--
-- Mục tiêu:
-- - Thấy `SELECT FOR UPDATE` lock row dù chưa update.
-- - Hữu ích khi backend cần đọc row rồi quyết định update dựa trên row đó.
--
-- Session A:
--   BEGIN;
--   SELECT tenant_id, account_code, balance
--   FROM acid_isolation_lab
--   WHERE tenant_id = 1 AND account_code = 'CASH'
--   FOR UPDATE;
--   -- Giữ transaction mở.
--
-- Session B:
--   UPDATE acid_isolation_lab
--   SET balance = balance + 30
--   WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- Expected B:
--   UPDATE chờ tới khi A COMMIT/ROLLBACK.
--
-- Session A:
--   ROLLBACK;
--
-- Expected:
--   B tiếp tục sau khi lock được thả.
--
-- Ghi nhớ:
-- - `SELECT FOR UPDATE` không dùng cho mọi SELECT.
-- - Chỉ dùng khi thật sự cần chặn transaction khác sửa row mình vừa đọc.

-- ==============================================================
-- 7. Optional: DDL lock bằng ALTER TABLE trên bảng lab
-- ==============================================================
--
-- Mục tiêu:
-- - Liên hệ với migration/locking milestone.
--
-- Session A:
--   BEGIN;
--   ALTER TABLE acid_isolation_lab ADD COLUMN ddl_lock_note VARCHAR(50);
--   -- Giữ transaction mở.
--
-- Session B:
--   SELECT COUNT(*) FROM acid_isolation_lab;
--
-- Expected:
-- - Tùy lock mode của ALTER TABLE, SELECT có thể bị chờ.
-- - Không cần học toàn bộ lock mode; chỉ cần nhớ DDL có thể block mạnh hơn DML thường.
--
-- Session A:
--   ROLLBACK;
--
-- Nếu bạn đã COMMIT nhầm và muốn cleanup:
--   ALTER TABLE acid_isolation_lab DROP COLUMN IF EXISTS ddl_lock_note;

-- ==============================================================
-- 8. Optional: SERIALIZABLE anomaly prevention
-- ==============================================================
--
-- Mục tiêu:
-- - Thấy SERIALIZABLE có thể abort một transaction để bảo vệ invariant.
-- - Behavior có thể phụ thuộc timing; nếu không reproduce ngay, đọc output và thử lại.
--
-- Invariant giả lập:
--   Mỗi tenant chỉ được có tối đa 1 booking active trong bảng lab.
--
-- Reset:
--   DELETE FROM acid_booking_lab WHERE tenant_id = 1;
--
-- Session A:
--   BEGIN ISOLATION LEVEL SERIALIZABLE;
--   SELECT COUNT(*) FROM acid_booking_lab WHERE tenant_id = 1;
--   -- Nếu count = 0, chuẩn bị insert nhưng đừng commit vội.
--   INSERT INTO acid_booking_lab (tenant_id, booking_code)
--   VALUES (1, 'BOOKING-A');
--
-- Session B:
--   BEGIN ISOLATION LEVEL SERIALIZABLE;
--   SELECT COUNT(*) FROM acid_booking_lab WHERE tenant_id = 1;
--   INSERT INTO acid_booking_lab (tenant_id, booking_code)
--   VALUES (1, 'BOOKING-B');
--
-- Session A:
--   COMMIT;
--
-- Session B:
--   COMMIT;
--
-- Expected:
-- - Một transaction có thể fail với serialization failure.
-- - App thật phải retry toàn bộ transaction.
--
-- Nếu cả hai COMMIT trong môi trường local do timing chưa tạo conflict rõ:
-- - Ghi lại là chưa reproduce được.
-- - Không ép kết luận; chỉ cần nhớ SERIALIZABLE có thể abort và cần retry.

-- ==============================================================
-- 9. Shared-table tenant-aware example
-- ==============================================================
--
-- Mục tiêu:
-- - Phân biệt tenant isolation và transaction isolation.
--
-- Session A:
--   BEGIN;
--   UPDATE acid_isolation_lab
--   SET balance = balance + 10
--   WHERE tenant_id = 1 AND account_code = 'CASH';
--
-- Session B:
--   UPDATE acid_isolation_lab
--   SET balance = balance + 10
--   WHERE tenant_id = 2 AND account_code = 'CASH';
--
-- Expected:
-- - Hai UPDATE đụng cùng bảng nhưng khác row, nên thường không chờ nhau ở row-level.
--
-- Nếu Session B cũng update tenant_id = 1/account_code = CASH:
-- - B sẽ chờ A kết thúc vì cùng row.
--
-- Kết luận:
-- - `tenant_id` filtering giải quyết quyền sở hữu dữ liệu.
-- - Transaction/isolation/lock giải quyết concurrent behavior.
-- - Index `(tenant_id, account_code)` giúp PostgreSQL tìm đúng row hẹp hơn.
--
-- Session A:
--   ROLLBACK;

-- ==============================================================
-- 10. Reflection đã chốt ở mức Milestone #8
-- ==============================================================
--
-- - PostgreSQL default isolation level là READ COMMITTED.
-- - Dirty read không xảy ra trong PostgreSQL, kể cả khi request READ UNCOMMITTED.
-- - READ COMMITTED có thể có non-repeatable read và phantom read.
-- - REPEATABLE READ giữ snapshot ổn định trong transaction; PostgreSQL cũng ngăn phantom read ở mức này.
-- - SERIALIZABLE mạnh nhất nhưng có thể abort transaction; backend phải retry toàn bộ transaction.
-- - Transaction là unit of work; isolation là quy tắc nhìn thấy dữ liệu; lock/MVCC là cơ chế thực thi.
-- - Normal SELECT thường không block UPDATE trong PostgreSQL MVCC.
-- - UPDATE/DELETE/SELECT FOR UPDATE trên cùng row có thể block nhau.
-- - Tenant isolation khác transaction isolation: tenant_id trả lời "dữ liệu của ai", transaction isolation trả lời "dữ liệu ở thời điểm/concurrency nào".
-- - Rule thực dụng: query tenant-aware, index đúng pattern, transaction ngắn, retry khi dùng isolation mạnh.

-- ==============================================================
-- 11. Cleanup sau khi hoàn thành
-- ==============================================================
--
-- Khi đã ghi xong kết luận, có thể chạy:
--   DROP TABLE IF EXISTS acid_booking_lab;
--   DROP TABLE IF EXISTS acid_isolation_lab;
--
-- Không drop tự động ở cuối file, vì bảng cần còn tồn tại để hai session cùng quan sát.
