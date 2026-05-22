package com.viettel.demo.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/*
 * ==============================================================
 * MasterDataSearchGateway — adapter gọi Elasticsearch
 * ==============================================================
 *
 * [Mục tiêu]
 * Tập trung toàn bộ code nói chuyện với Elasticsearch vào một class.
 * Service/Indexer không cần tự biết chi tiết Java API Client.
 *
 * [Vì sao không để RestClient raw ở Service?]
 * - Raw JSON dễ sai shape: bulk phải là NDJSON, search phải đúng Query DSL.
 * - Official Elasticsearch Java API Client cho request/response typed hơn.
 * - Gateway giúp gom exception handling và giữ Controller/Service mỏng.
 *
 * ==============================================================
 */
@Component
@ConditionalOnProperty(prefix = "app.search", name = "enabled", havingValue = "true")
public class MasterDataSearchGateway {

    private final ElasticsearchClient client;
    private final SearchProperties properties;

    public MasterDataSearchGateway(ElasticsearchClient client, SearchProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public void ensureIndexExists() {
        run("check/create search index", () -> {
            String indexName = properties.getMasterDataIndex();
            BooleanResponse exists = client.indices().exists(e -> e.index(indexName));
            if (!exists.value()) {
                client.indices().create(c -> c.index(indexName));
            }
            return null;
        });
    }

    public void indexOne(MasterDataSearchDocument document) {
        ensureIndexExists();
        run("index one master_data document", () -> {
            client.index(i -> i
                    .index(properties.getMasterDataIndex())
                    .id(String.valueOf(document.id()))
                    .document(document)
            );
            refreshIndexForLocalLab();
            return null;
        });
    }

    public int bulkIndex(List<MasterDataSearchDocument> documents) {
        if (documents.isEmpty()) {
            return 0;
        }

        ensureIndexExists();

        BulkResponse response = run("bulk index master_data documents", () -> client.bulk(b -> {
            for (MasterDataSearchDocument document : documents) {
                b.operations(op -> op.index(idx -> idx
                        .index(properties.getMasterDataIndex())
                        .id(String.valueOf(document.id()))
                        .document(document)
                ));
            }
            return b;
        }));

        if (response.errors()) {
            String firstReason = response.items().stream()
                    .filter(item -> item.error() != null)
                    .findFirst()
                    .map(item -> item.error().reason())
                    .orElse("unknown bulk indexing error");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Elasticsearch bulk indexing failed: " + firstReason);
        }

        refreshIndexForLocalLab();
        return documents.size();
    }

    public List<MasterDataSearchDocument> search(Long tenantId, String keyword) {
        ensureIndexExists();

        SearchResponse<MasterDataSearchDocument> response = run("search master_data documents", () -> client.search(s -> s
                        .index(properties.getMasterDataIndex())
                        .size(50)
                        .query(q -> q.bool(b -> b
                                .must(m -> m.multiMatch(mm -> mm
                                        .query(keyword)
                                        .fields("code", "name", "category")
                                ))
                                .filter(f -> f.term(t -> t
                                        .field("tenantId")
                                        .value(tenantId)
                                ))
                                .filter(f -> f.term(t -> t
                                        .field("active")
                                        .value(true)
                                ))
                        )),
                MasterDataSearchDocument.class
        ));

        return response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .toList();
    }

    private void refreshIndexForLocalLab() {
        run("refresh search index for local lab", () -> {
            client.indices().refresh(r -> r.index(properties.getMasterDataIndex()));
            return null;
        });
    }

    private <T> T run(String description, ElasticsearchCall<T> call) {
        try {
            return call.execute();
        } catch (ElasticsearchException | IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Elasticsearch operation failed while trying to " + description,
                    exception
            );
        }
    }

    @FunctionalInterface
    private interface ElasticsearchCall<T> {
        T execute() throws IOException;
    }
}
