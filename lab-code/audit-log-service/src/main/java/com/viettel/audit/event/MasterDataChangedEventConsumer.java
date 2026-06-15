package com.viettel.audit.event;

import com.viettel.audit.audit.AuditEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class MasterDataChangedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(MasterDataChangedEventConsumer.class);

    private final AuditEventService auditEventService;

    public MasterDataChangedEventConsumer(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @KafkaListener(
            topics = "${app.kafka.master-data-topic}",
            groupId = "${app.kafka.consumer-group-id}",
            containerFactory = "masterDataChangedEventListenerContainerFactory"
    )
    public void handle(
            @Payload MasterDataChangedEvent event,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key
    ) {
        log.debug(
                "Consumed cross-service event eventId={}, type={}, tenantId={}, aggregateId={}, code={}, changeType={}, key={}",
                event.eventId(),
                event.eventType(),
                event.tenantId(),
                event.aggregateId(),
                event.code(),
                event.changeType(),
                key
        );
        auditEventService.recordConsumedEvent(event);
    }
}
