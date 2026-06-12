package com.viettel.audit.audit;

import com.viettel.audit.event.MasterDataChangedEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "aggregate_code", nullable = false, length = 255)
    private String aggregateCode;

    @Column(name = "change_type", nullable = false, length = 50)
    private String changeType;

    @Column(name = "source", nullable = false, length = 100)
    private String source;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "consumed_at", nullable = false)
    private Instant consumedAt;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    protected AuditEvent() {
    }

    private AuditEvent(
            String eventId,
            Long tenantId,
            String eventType,
            String aggregateType,
            Long aggregateId,
            String aggregateCode,
            String changeType,
            String source,
            Instant occurredAt,
            Instant consumedAt,
            String payloadJson
    ) {
        this.eventId = eventId;
        this.tenantId = tenantId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.aggregateCode = aggregateCode;
        this.changeType = changeType;
        this.source = source;
        this.occurredAt = occurredAt;
        this.consumedAt = consumedAt;
        this.payloadJson = payloadJson;
    }

    public static AuditEvent from(MasterDataChangedEvent event, String payloadJson) {
        return new AuditEvent(
                event.eventId(),
                event.tenantId(),
                event.eventType(),
                event.aggregateType(),
                event.aggregateId(),
                event.code(),
                event.changeType(),
                event.source(),
                event.occurredAt(),
                Instant.now(),
                payloadJson
        );
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public Long getAggregateId() {
        return aggregateId;
    }

    public String getAggregateCode() {
        return aggregateCode;
    }

    public String getChangeType() {
        return changeType;
    }

    public String getSource() {
        return source;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public String getPayloadJson() {
        return payloadJson;
    }
}
