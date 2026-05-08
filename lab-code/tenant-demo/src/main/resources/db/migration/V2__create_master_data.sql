-- ==============================================================
-- TODO TASK: Flyway Migration V2 — Tạo bảng master_data
-- ==============================================================
--
-- [Mục tiêu]
-- Tạo bảng nghiệp vụ đầu tiên, có cột tenant_id.
--
-- [Nhiệm vụ của tôi]
-- 1. Tạo bảng master_data với: id, tenant_id, code, name,
--    category, is_active, created_at.
-- 2. tenant_id phải là FK → tenants(id).
-- 3. UNIQUE constraint trên (tenant_id, code).
-- 4. Suy nghĩ: nếu viết UNIQUE(code) thay vì UNIQUE(tenant_id, code),
--    vấn đề gì xảy ra khi 2 doanh nghiệp dùng cùng mã "VT001"?
--
-- [Kiến thức cần tự research]
-- - REFERENCES (foreign key) syntax
-- - UNIQUE constraint trên nhiều cột
-- - NOT NULL constraints
-- - DEFAULT values
-- - Dựa lại SQL playground: lab-code/sql-playground/01-setup-tables.sql
-- - Không dựa vào Hibernate ddl-auto để tự tạo bảng
-- ==============================================================

-- Viết SQL migration ở đây:
CREATE TABLE master_data (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    code VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT unique_tenant_code UNIQUE (tenant_id, code)
)
