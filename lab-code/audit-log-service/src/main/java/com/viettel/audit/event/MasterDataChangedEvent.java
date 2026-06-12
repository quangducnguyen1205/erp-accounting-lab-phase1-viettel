package com.viettel.audit.event;

import java.time.Instant;

/*
 * Intentional duplicate event contract.
 *
 * For Phase 1.5, audit-log-service should not import tenant-demo classes.
 * Later, this contract could move to a shared module or schema registry.
 */
public record MasterDataChangedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long tenantId,
        String aggregateType,
        Long aggregateId,
        String code,
        String changeType,
        String source
) {
}
