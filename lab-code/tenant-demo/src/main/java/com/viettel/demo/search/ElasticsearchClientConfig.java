package com.viettel.demo.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchClientConfig {

    /*
     * ==============================================================
     * Elasticsearch Java API Client config
     * ==============================================================
     *
     * Bean này chỉ được tạo khi APP_SEARCH_ENABLED=true.
     * Nhờ vậy app/test mặc định không phụ thuộc Elasticsearch local.
     *
     * Official Java API Client vẫn dùng REST transport ở bên dưới, nhưng
     * code nghiệp vụ dùng request/response typed thay vì tự ghép JSON raw.
     *
     * Mini-lab hiện dùng một Elasticsearch URI local. Nếu sau này cần cluster
     * nhiều node/credential/SSL, lúc đó mới mở rộng config.
     *
     * ==============================================================
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.search", name = "enabled", havingValue = "true")
    RestClient elasticsearchLowLevelRestClient(SearchProperties properties) {
        return RestClient.builder(HttpHost.create(properties.getElasticsearchUris())).build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.search", name = "enabled", havingValue = "true")
    ElasticsearchClient elasticsearchClient(RestClient elasticsearchLowLevelRestClient) {
        ElasticsearchTransport elasticsearchTransport = new RestClientTransport(
                elasticsearchLowLevelRestClient,
                new JacksonJsonpMapper()
        );
        return new ElasticsearchClient(elasticsearchTransport);
    }
}
