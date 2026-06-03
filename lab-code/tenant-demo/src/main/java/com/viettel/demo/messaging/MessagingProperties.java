package com.viettel.demo.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/*
 * ==============================================================
 * MessagingProperties — cấu hình Kafka/async mini-lab
 * ==============================================================
 *
 * Messaging tắt mặc định để app-test không cần Kafka.
 * Khi bạn tự code producer/consumer thật, class này là nơi đọc topic,
 * bootstrap servers và consumer group từ application.yml/.env.
 *
 * ==============================================================
 */
@Component
@ConfigurationProperties(prefix = "app.messaging")
public class MessagingProperties {

    private boolean enabled = false;

    private String bootstrapServers = "localhost:19092";

    private String masterDataTopic = "master-data-events";

    private String consumerGroupId = "tenant-demo-master-data";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getMasterDataTopic() {
        return masterDataTopic;
    }

    public void setMasterDataTopic(String masterDataTopic) {
        this.masterDataTopic = masterDataTopic;
    }

    public String getConsumerGroupId() {
        return consumerGroupId;
    }

    public void setConsumerGroupId(String consumerGroupId) {
        this.consumerGroupId = consumerGroupId;
    }
}
