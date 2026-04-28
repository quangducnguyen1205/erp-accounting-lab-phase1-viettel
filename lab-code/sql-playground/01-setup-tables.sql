-- ==============================================================
-- TODO TASK 1: Tạo bảng cho multi-tenant demo
-- ==============================================================
--
-- [Mục tiêu]
-- Tạo schema database cho mô hình Shared Table + tenant_id.
-- Đây là nền tảng để tất cả các bài thực hành sau dựa vào.
--
-- [Nhiệm vụ của tôi]
-- 1. Tạo bảng `tenants` để lưu danh sách tenant (doanh nghiệp).
--    - Cần có: id (PK), code (unique), name, is_active, created_at.
-- 2. Tạo bảng `master_data` (ví dụ: danh mục vật tư) với cột `tenant_id`.
--    - Cần có: id (PK), tenant_id (FK → tenants), code, name, category,
--      is_active, created_at.
-- 3. Tạo UNIQUE constraint tenant-aware: (tenant_id, code).
--    - Suy nghĩ: tại sao UNIQUE(code) là sai trong multi-tenant?
-- Vì code của sản phẩm chỉ unique với từng tenant, không phải toàn bộ database.
-- Nếu dùng UNIQUE(code) thì sẽ không thể có 2 tenant khác nhau có cùng code sản phẩm.
-- Và việc đó cũng gián tiếp leak data giữa các tenant, vì khi cố gắng tạo một code đã tồn tại,
-- hệ thống sẽ trả về lỗi cho biết code đó đã tồn tại, từ đó có thể suy ra được thông tin về các tenant khác.
-- 4. Tạo composite index bắt đầu bằng tenant_id.
--    - Suy nghĩ: tại sao tenant_id phải là cột đầu tiên? (leftmost prefix)
--
-- [Kiến thức cần tự research]
-- - PostgreSQL CREATE TABLE syntax
-- - BIGSERIAL vs BIGINT
-- - REFERENCES (foreign key)
-- - UNIQUE constraint trên nhiều cột
-- - CREATE INDEX vs UNIQUE constraint
-- - Composite index và leftmost prefix rule
-- - Đọc lại: docs/03-backend-database-mo-rong/index-va-query-tenant-aware.md
--
-- ==============================================================

-- Viết SQL của bạn ở đây:
CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE master_data (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    code VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_master_data_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    UNIQUE (tenant_id, code)
);

CREATE INDEX idx_master_data_tenant_category ON master_data (tenant_id, category);