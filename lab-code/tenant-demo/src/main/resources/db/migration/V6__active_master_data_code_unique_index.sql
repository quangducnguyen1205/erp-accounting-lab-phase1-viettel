ALTER TABLE master_data
    DROP CONSTRAINT IF EXISTS unique_tenant_code;

CREATE UNIQUE INDEX IF NOT EXISTS ux_master_data_tenant_code_active
    ON master_data (tenant_id, code)
    WHERE is_active = TRUE;
