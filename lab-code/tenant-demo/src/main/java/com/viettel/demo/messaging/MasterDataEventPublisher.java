package com.viettel.demo.messaging;

import org.springframework.stereotype.Component;

/*
 * ==============================================================
 * MasterDataEventPublisher — boundary cho async messaging
 * ==============================================================
 *
 * Service nghiệp vụ sau này chỉ nên gọi publisher bằng event DTO.
 * Chi tiết KafkaTemplate/topic/key nên nằm ở adapter/publisher, không đặt
 * trực tiếp trong Controller.
 *
 * Hiện tại đây là skeleton compile-safe. Chưa thêm spring-kafka và chưa gửi
 * Kafka thật để bạn tự code phần producer/consumer chính.
 *
 * ==============================================================
 */
@Component
public class MasterDataEventPublisher {

    private final MessagingProperties properties;

    public MasterDataEventPublisher(MessagingProperties properties) {
        this.properties = properties;
    }

    public void publish(MasterDataChangedEvent event) {
        if (!properties.isEnabled()) {
            return;
        }

        /*
         * TODO Kafka mini-lab:
         * 1. Thêm dependency spring-kafka vào pom.xml.
         * 2. Tạo Kafka producer/adapter dùng KafkaTemplate.
         * 3. Gửi event vào properties.getMasterDataTopic().
         * 4. Dùng event.kafkaKey() để giữ key tenant-aware.
         * 5. Log ngắn eventId/tenantId/aggregateId/changeType.
         *
         * Không publish event trước khi DB write thành công.
         * Không dùng Kafka thay PostgreSQL source of truth.
         */
        throw new UnsupportedOperationException(
                "TODO Kafka mini-lab: implement producer with Spring Kafka before enabling app.messaging"
        );
    }
}
