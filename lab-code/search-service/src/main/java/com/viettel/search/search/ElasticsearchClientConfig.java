package com.viettel.search.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchClientConfig {

    /*
     * ==============================================================
     * Elasticsearch Java API Client config
     * ==============================================================
     *
     * Bean này thuộc search-service, nên service này cần Elasticsearch khi chạy.
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
    RestClient elasticsearchLowLevelRestClient(SearchProperties properties) {
        return RestClient.builder(HttpHost.create(properties.getElasticsearchUris())).build();
    }

    @Bean
    ElasticsearchClient elasticsearchClient(RestClient elasticsearchLowLevelRestClient) {
        ElasticsearchTransport elasticsearchTransport = new RestClientTransport(
                elasticsearchLowLevelRestClient,
                new JacksonJsonpMapper(elasticsearchObjectMapper())
        );
        return new ElasticsearchClient(elasticsearchTransport);
    }

    private ObjectMapper elasticsearchObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
