package com.viettel.search.event;

import com.viettel.search.search.MasterDataSearchProjectionService;
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

    private final MasterDataSearchProjectionService projectionService;

    public MasterDataChangedEventConsumer(MasterDataSearchProjectionService projectionService) {
        this.projectionService = projectionService;
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
        log.info(
                "Consumed search projection event eventId={}, tenantId={}, aggregateId={}, code={}, changeType={}, key={}",
                event.eventId(),
                event.tenantId(),
                event.aggregateId(),
                event.code(),
                event.changeType(),
                key
        );
        projectionService.apply(event);
    }
}
