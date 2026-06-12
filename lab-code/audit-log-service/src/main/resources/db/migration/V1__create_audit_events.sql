CREATE SCHEMA IF NOT EXISTS audit_log;

CREATE TABLE IF NOT EXISTS audit_log.audit_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    tenant_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    aggregate_code VARCHAR(255) NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    source VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    payload_json TEXT NOT NULL,
    CONSTRAINT uq_audit_events_event_id UNIQUE (event_id)
);

CREATE INDEX IF NOT EXISTS idx_audit_events_tenant_consumed_at
    ON audit_log.audit_events (tenant_id, consumed_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_events_tenant_aggregate
    ON audit_log.audit_events (tenant_id, aggregate_type, aggregate_id);
