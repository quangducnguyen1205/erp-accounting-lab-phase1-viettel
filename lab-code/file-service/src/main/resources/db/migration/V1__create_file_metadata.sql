CREATE SCHEMA IF NOT EXISTS file_service;

CREATE TABLE IF NOT EXISTS file_service.file_metadata (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    file_id VARCHAR(64) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    original_filename VARCHAR(512) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_file_service_tenant_file UNIQUE (tenant_id, file_id)
);

CREATE INDEX IF NOT EXISTS idx_file_metadata_tenant_created_at
    ON file_service.file_metadata (tenant_id, created_at DESC);
