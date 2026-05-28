-- ==============================================================
-- TODO TASK: Flyway Migration V5 — Tạo bảng file_metadata
-- ==============================================================
--
-- [Mục tiêu]
-- Lưu metadata tenant-aware cho file upload lên MinIO.
--
-- [Nhiệm vụ của tôi]
-- 1. Tạo bảng file_metadata với: id, tenant_id, file_id, object_key,
--    original_filename, content_type, size_bytes, created_at.
-- 2. tenant_id là FK -> tenants(id).
-- 3. UNIQUE (tenant_id, file_id) để fileId không đụng giữa tenant.
--
-- [Ghi chú]
-- - file_id là String để dễ thao tác trong mini-lab.
-- - object_key là key thực lưu trên MinIO.
-- ==============================================================

CREATE TABLE file_metadata (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    file_id VARCHAR(64) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    original_filename VARCHAR(512) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_file_metadata_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uq_file_metadata_tenant_file UNIQUE (tenant_id, file_id)
);

