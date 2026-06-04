package com.viettel.demo.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/*
 * ==============================================================
 * MasterDataChangedEventConsumer — consumer học tập
 * ==============================================================
 *
 * Consumer hiện chỉ log event để quan sát producer -> Kafka -> consumer.
 * Chưa có idempotency storage, retry topic hoặc dead-letter topic.
 *
 * Vì Kafka có thể deliver lại message, production consumer phải xử lý
 * duplicate bằng eventId hoặc operation idempotent.
 *
 * ==============================================================
 */
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class MasterDataChangedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(MasterDataChangedEventConsumer.class);

    @KafkaListener(
            topics = "${app.messaging.master-data-topic}",
            groupId = "${app.messaging.consumer-group-id}",
            containerFactory = "masterDataKafkaListenerContainerFactory"
    )
    // Method này không phải HTTP endpoint; Spring Kafka listener container poll message rồi gọi method.
    public void handle(
            @Payload MasterDataChangedEvent event,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key
    ) {
        log.info(
                "Consumed Kafka event eventId={}, type={}, tenantId={}, aggregateId={}, code={}, changeType={}, key={}",
                event.eventId(),
                event.eventType(),
                event.tenantId(),
                event.aggregateId(),
                event.code(),
                event.changeType(),
                key
        );
    }
}
