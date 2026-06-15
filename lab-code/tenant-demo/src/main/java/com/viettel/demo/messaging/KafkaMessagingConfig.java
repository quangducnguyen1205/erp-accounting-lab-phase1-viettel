package com.viettel.demo.messaging;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/*
 * ==============================================================
 * KafkaMessagingConfig — Spring Kafka wiring cho mini-lab
 * ==============================================================
 *
 * Config này chỉ bật khi APP_MESSAGING_ENABLED=true.
 *
 * [Producer]
 * - key: String, ví dụ tenant:1:master-data:101
 * - value: MasterDataChangedEvent serialize thành JSON
 *
 * tenant-demo hiện là producer của `master-data-events`.
 * Consumer thật nằm ở audit-log-service và search-service để flow giống
 * production-like service split hơn.
 *
 * ==============================================================
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class KafkaMessagingConfig {

    @Bean
    ProducerFactory<String, MasterDataChangedEvent> masterDataProducerFactory(
            MessagingProperties properties
    ) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    KafkaTemplate<String, MasterDataChangedEvent> masterDataKafkaTemplate(
            ProducerFactory<String, MasterDataChangedEvent> masterDataProducerFactory
    ) {
        return new KafkaTemplate<>(masterDataProducerFactory);
    }
}
