-- ============================================================
-- SQL Playground: Setup bảng cho multi-tenant demo
-- Dùng với: psql -d tenant_demo -f 01-setup-tables.sql
-- ============================================================

-- Bảng tenant
CREATE TABLE IF NOT EXISTS tenants (
    id          bigserial PRIMARY KEY,
    code        varchar(50) NOT NULL UNIQUE,
    name        varchar(255) NOT NULL,
    is_active   boolean NOT NULL DEFAULT true,
    created_at  timestamp NOT NULL DEFAULT now()
);

-- Bảng master_data (ví dụ: danh mục vật tư, nhà cung cấp, v.v.)
CREATE TABLE IF NOT EXISTS master_data (
    id          bigserial PRIMARY KEY,
    tenant_id   bigint NOT NULL REFERENCES tenants(id),
    code        varchar(50) NOT NULL,
    name        varchar(255) NOT NULL,
    category    varchar(50) NOT NULL,
    is_active   boolean NOT NULL DEFAULT true,
    created_at  timestamp NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, code)
);

-- Index tenant-aware
CREATE INDEX IF NOT EXISTS idx_master_data_tenant
    ON master_data (tenant_id);

CREATE INDEX IF NOT EXISTS idx_master_data_tenant_category
    ON master_data (tenant_id, category);

CREATE INDEX IF NOT EXISTS idx_master_data_tenant_active
    ON master_data (tenant_id, is_active);
