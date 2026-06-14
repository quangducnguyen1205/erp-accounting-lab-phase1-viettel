package com.viettel.search.event;

import java.time.Instant;

/*
 * Intentional duplicate event contract.
 *
 * Search-service should not import tenant-demo classes. Later this contract
 * could move to a shared contract module or schema registry.
 */
public record MasterDataChangedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long tenantId,
        String aggregateType,
        Long aggregateId,
        String code,
        String name,
        String category,
        Boolean active,
        String changeType,
        String source
) {
}
