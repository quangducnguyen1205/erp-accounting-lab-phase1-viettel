-- ==============================================================
-- Task 2: Insert dữ liệu mẫu cho multi-tenant demo
-- ==============================================================
-- Chạy sau khi đã nạp 01-setup-tables.sql
-- Lệnh: make sql-2  (hoặc psql -f sql-playground/02-insert-sample-data.sql)
-- ==============================================================


-- ============================================================
-- KỊCH BẢN 1: Khởi tạo Tenants
-- ============================================================
-- Insert 2 doanh nghiệp. Bỏ qua id (BIGSERIAL tự tăng),
-- is_active (DEFAULT TRUE) và created_at (DEFAULT NOW()).

INSERT INTO tenants (code, name)
VALUES ('VIETTEL', 'Tập đoàn Công nghiệp - Viễn thông Quân đội');

INSERT INTO tenants (code, name)
VALUES ('FPT', 'Công ty Cổ phần FPT');

-- Sau 2 lệnh trên:
--   Tenant 1 (id=1): VIETTEL
--   Tenant 2 (id=2): FPT
-- Các bước sau sẽ dùng id = 1 và id = 2.


-- ============================================================
-- KỊCH BẢN 2: Insert dữ liệu nghiệp vụ bình thường
-- ============================================================

-- >>> Tenant 1 (VIETTEL): 3 vật tư <<<
INSERT INTO master_data (tenant_id, code, name, category)
VALUES (1, 'LAPTOP-01', 'Laptop Dell Latitude 5540', 'Laptop');

INSERT INTO master_data (tenant_id, code, name, category)
VALUES (1, 'MOUSE-01', 'Chuột Logitech M330', 'Chuột');

INSERT INTO master_data (tenant_id, code, name, category)
VALUES (1, 'MOUSE-02', 'Chuột Logitech MX Master 3S', 'Chuột');

-- >>> Tenant 2 (FPT): 2 vật tư <<<
INSERT INTO master_data (tenant_id, code, name, category)
VALUES (2, 'MON-01', 'Màn hình Dell U2723QE 27 inch', 'Màn hình');

INSERT INTO master_data (tenant_id, code, name, category)
VALUES (2, 'MON-02', 'Màn hình LG 32UN880-B 32 inch', 'Màn hình');


-- ============================================================
-- KỊCH BẢN 3: Chứng minh Data Isolation
-- ============================================================
-- Tenant 2 (FPT) insert mã code 'LAPTOP-01' — TRÙNG với Tenant 1.
--
-- Câu lệnh này SẼ THÀNH CÔNG vì ràng buộc là UNIQUE(tenant_id, code),
-- không phải UNIQUE(code). Hai tenant khác nhau được phép có cùng mã.
-- Đây chính là lý do phải dùng tenant-aware unique constraint.

INSERT INTO master_data (tenant_id, code, name, category)
VALUES (2, 'LAPTOP-01', 'Laptop HP EliteBook 840 G9', 'Laptop');


-- ============================================================
-- KỊCH BẢN 4: Chứng minh chặn Duplicate trong cùng Tenant
-- ============================================================
-- Cố tình insert lại mã 'LAPTOP-01' cho chính Tenant 1.
--
-- ⚠️  BỎ COMMENT DÒNG DƯỚI ĐÂY ĐỂ CHẠY THỬ.
--     Kết quả: PostgreSQL sẽ văng lỗi vi phạm Unique Constraint:
--     ERROR: duplicate key value violates unique constraint "master_data_tenant_id_code_key"
--     DETAIL: Key (tenant_id, code)=(1, LAPTOP-01) already exists.
--
-- INSERT INTO master_data (tenant_id, code, name, category)
-- VALUES (1, 'LAPTOP-01', 'Laptop Lenovo ThinkPad X1 Carbon', 'Laptop');


-- ============================================================
-- Kiểm tra kết quả
-- ============================================================

-- Xem toàn bộ tenants:
SELECT id, code, name FROM tenants ORDER BY id;

-- Xem toàn bộ master_data (gồm cả 2 tenant):
SELECT md.id, t.code AS tenant, md.code, md.name, md.category
FROM master_data md
JOIN tenants t ON t.id = md.tenant_id
ORDER BY md.tenant_id, md.id;

-- Đếm số vật tư mỗi tenant:
SELECT t.code AS tenant, COUNT(*) AS so_vat_tu
FROM master_data md
JOIN tenants t ON t.id = md.tenant_id
GROUP BY t.code
ORDER BY t.code;
