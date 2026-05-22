package com.viettel.demo.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.viettel.demo.search.SearchProperties;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

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
     * ==============================================================
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.search", name = "enabled", havingValue = "true")
    RestClient elasticsearchLowLevelRestClient(SearchProperties properties) {
        return RestClient.builder(parseHosts(properties.getElasticsearchUris())).build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.search", name = "enabled", havingValue = "true")
    ElasticsearchTransport elasticsearchTransport(RestClient elasticsearchLowLevelRestClient) {
        return new RestClientTransport(elasticsearchLowLevelRestClient, new JacksonJsonpMapper());
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.search", name = "enabled", havingValue = "true")
    ElasticsearchClient elasticsearchClient(ElasticsearchTransport elasticsearchTransport) {
        return new ElasticsearchClient(elasticsearchTransport);
    }

    private HttpHost[] parseHosts(String elasticsearchUris) {
        return Arrays.stream(elasticsearchUris.split(","))
                .map(String::trim)
                .filter(uri -> !uri.isBlank())
                .map(HttpHost::create)
                .toArray(HttpHost[]::new);
    }
}
