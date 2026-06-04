package com.viettel.demo.messaging;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/*
 * ==============================================================
 * KafkaMasterDataEventPublisher — producer thật cho mini-lab
 * ==============================================================
 *
 * Adapter này giữ chi tiết Kafka:
 * - topic lấy từ config;
 * - key lấy từ event.kafkaKey();
 * - value là MasterDataChangedEvent JSON.
 *
 * Mini-lab cố tình wait kết quả send để Kafka unavailable fail rõ ràng.
 * Production thường cần retry/outbox/monitoring thay vì block request đơn giản.
 *
 * ==============================================================
 */
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class KafkaMasterDataEventPublisher implements MasterDataEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaMasterDataEventPublisher.class);

    private final KafkaTemplate<String, MasterDataChangedEvent> kafkaTemplate;
    private final MessagingProperties properties;

    public KafkaMasterDataEventPublisher(
            KafkaTemplate<String, MasterDataChangedEvent> kafkaTemplate,
            MessagingProperties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(MasterDataChangedEvent event) {
        String topic = properties.getMasterDataTopic();
        String key = event.kafkaKey();

        try {
            // send(...) trả về CompletableFuture; .get(...) làm request chờ broker ack để mini-lab fail rõ khi Kafka lỗi.
            SendResult<String, MasterDataChangedEvent> result = kafkaTemplate
                    .send(topic, key, event)
                    .get(5, TimeUnit.SECONDS);

            RecordMetadata metadata = result.getRecordMetadata();
            log.info(
                    "Published Kafka event eventId={}, type={}, tenantId={}, aggregateId={}, changeType={}, topic={}, key={}, partition={}, offset={}",
                    event.eventId(),
                    event.eventType(),
                    event.tenantId(),
                    event.aggregateId(),
                    event.changeType(),
                    topic,
                    key,
                    metadata.partition(),
                    metadata.offset()
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to publish MasterDataChangedEvent to Kafka. DB write may already be committed in this mini-lab.",
                    e
            );
        }
    }
}
