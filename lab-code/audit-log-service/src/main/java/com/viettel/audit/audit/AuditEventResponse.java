package com.viettel.audit.audit;

import java.time.Instant;

public record AuditEventResponse(
        String eventId,
        Long tenantId,
        String eventType,
        String aggregateType,
        Long aggregateId,
        String aggregateCode,
        String changeType,
        String source,
        Instant occurredAt,
        Instant consumedAt
) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getEventId(),
                event.getTenantId(),
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getAggregateCode(),
                event.getChangeType(),
                event.getSource(),
                event.getOccurredAt(),
                event.getConsumedAt()
        );
    }
}
