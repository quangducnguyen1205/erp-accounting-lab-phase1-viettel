package com.viettel.search.event;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.kafka")
public class SearchKafkaProperties {

    private boolean enabled = true;
    private String bootstrapServers = "localhost:19092";
    private String masterDataTopic = "master-data-events";
    private String consumerGroupId = "search-service";

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
