package com.viettel.demo.messaging;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
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
 * [Consumer]
 * - deserialize JSON về MasterDataChangedEvent
 * - consumer group lấy từ app.messaging.consumer-group-id
 *
 * Đây là config học tập, chưa có retry topic, DLT hoặc schema registry.
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

    @Bean
    ConsumerFactory<String, MasterDataChangedEvent> masterDataConsumerFactory(
            MessagingProperties properties
    ) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getConsumerGroupId());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.viettel.demo.messaging");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, MasterDataChangedEvent.class.getName());
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, MasterDataChangedEvent> masterDataKafkaListenerContainerFactory(
            ConsumerFactory<String, MasterDataChangedEvent> masterDataConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, MasterDataChangedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(masterDataConsumerFactory);
        return factory;
    }
}
