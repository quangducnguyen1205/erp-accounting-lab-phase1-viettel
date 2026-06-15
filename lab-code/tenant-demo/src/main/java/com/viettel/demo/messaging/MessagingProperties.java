package com.viettel.demo.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/*
 * ==============================================================
 * MessagingProperties — cấu hình Kafka/async mini-lab
 * ==============================================================
 *
 * Messaging tắt mặc định để app-test không cần Kafka.
 * tenant-demo hiện chỉ publish event. Audit/search service là consumer thật.
 *
 * ==============================================================
 */
@Component
@ConfigurationProperties(prefix = "app.messaging")
public class MessagingProperties {

    private boolean enabled = false;

    private String bootstrapServers = "localhost:19092";

    private String masterDataTopic = "master-data-events";

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

}
