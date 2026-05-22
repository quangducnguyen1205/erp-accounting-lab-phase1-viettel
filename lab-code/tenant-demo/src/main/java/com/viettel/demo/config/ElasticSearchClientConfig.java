package com.viettel.demo.config;

import com.viettel.demo.search.SearchProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ElasticSearchClientConfig {
    @Bean
    RestClient elasticsearchClient(SearchProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getElasticsearchUris())
                .build();
    }
}
