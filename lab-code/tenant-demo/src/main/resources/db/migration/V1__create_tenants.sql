-- ==============================================================
-- TODO TASK: Flyway Migration V1 — Tạo bảng tenants
-- ==============================================================
--
-- [Mục tiêu]
-- Migration đầu tiên: tạo bảng lưu thông tin tenant.
--
-- [Nhiệm vụ của tôi]
-- 1. Tạo bảng tenants với các cột: id, code, name, is_active, created_at.
-- 2. code phải UNIQUE.
-- 3. Suy nghĩ: đây là bảng ĐẶC BIỆT — nó KHÔNG có cột tenant_id
--    vì nó LÀ bảng chứa danh sách tenant. Bảng nào cần tenant_id,
--    bảng nào không?
--
-- [Kiến thức cần tự research]
-- - Flyway naming convention: V{version}__{description}.sql
-- - Flyway chỉ chạy migration CHƯA từng chạy
-- - PostgreSQL BIGSERIAL, DEFAULT now()
-- - Dựa lại SQL playground: lab-code/sql-playground/01-setup-tables.sql
-- - Không dựa vào Hibernate ddl-auto để tự tạo bảng
-- ==============================================================

-- Viết SQL migration ở đây:
CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);