package com.viettel.demo.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.viettel.demo.context.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.beans.Customizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/*
 * ==============================================================
 * MasterDataSearchService — tenant-aware search skeleton
 * ==============================================================
 *
 * [Mục tiêu]
 * Search service phải lấy tenantId từ TenantContext rồi filter query
 * Elasticsearch theo tenantId.
 *
 * [TODO tự code]
 * 1. Validate keyword.
 * 2. Lấy tenantId từ TenantContext.
 * 3. Tạo Elasticsearch bool query:
 *    - must: keyword search trên code/name/category.
 *    - filter: tenantId và active=true.
 * 4. Map kết quả Elasticsearch về DTO/document.
 *
 * [Rule quan trọng]
 * Không nhận tenantId từ request body/query param.
 *
 * ==============================================================
 */
@Service
public class MasterDataSearchService {

    private final RestClient elasticsearchClient;
    private final SearchProperties properties;

    public MasterDataSearchService(RestClient elasticsearchClient, SearchProperties properties) {
        this.elasticsearchClient = elasticsearchClient;
        this.properties = properties;
    }

    public List<MasterDataSearchDocument> search(String keyword) {
        Long tenantId = currentTenantId();
        /*
         * TODO:
         * - Dùng tenantId này trong filter Elasticsearch.
         * - Không search toàn index.
         * - Nếu keyword blank, quyết định trả 400 hoặc list top documents
         *   trong tenant hiện tại.
         */
        validate(keyword);

        Map<String, Object> query = new HashMap<>();
        query.put("bool", new HashMap<>());
        Map<String, Object> bool = (Map<String, Object>) query.get("bool");
        bool.put("must", new HashMap<>());
        bool.put("filter", new HashMap<>());
        Map<String, Object> must = (Map<String, Object>) bool.get("must");
        must.put("keyword", keyword);
        Map<String, Object> filter = (Map<String, Object>) bool.get("filter");
        filter.put("term", Map.of("tenantId", tenantId));
        JsonNode responseBody = execute(() ->
            elasticsearchClient.post()
                    .uri("{indexName}/_search", properties.getElasticsearchUris())
                    .body(Map.of("query", query))
                    .retrieve()
                    .body(JsonNode.class), "execute Elasticsearch query");
        return toSearchResponse(responseBody);
    }

    private Long currentTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant context is missing");
        }
        return tenantId;
    }

    private void validate(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Keyword cannot be blank");
        }
    }

    private List<MasterDataSearchDocument> toSearchResponse(JsonNode responseBody) {
        JsonNode hits = responseBody.path("hits").path("hits");
        if (!hits.isArray()) {
            throw new RuntimeException("Elasticsearch response is not an array");
        }
        List<MasterDataSearchDocument> results = new ArrayList<>();
        for (JsonNode hit : hits) {
            JsonNode source = hit.path("_source");
            results.add(new MasterDataSearchDocument(
                    source.path("id").asLong(),
                    source.path("tenantId").asLong(),
                    source.path("code").asText(),
                    source.path("name").asText(),
                    source.path("category").asText(),
                    source.path("active").asBoolean()
            ));
        }
        return results;
    }

    private <T> T execute(SearchFunction<T> operation, String description) {
        try {
            return operation.run();
        } catch (Exception exception) {
            throw new RuntimeException("Elasticsearch operation failed while trying to " + description, exception);
        }
    }

    @FunctionalInterface
    private interface SearchFunction<T> {
        T run();
    }
}

