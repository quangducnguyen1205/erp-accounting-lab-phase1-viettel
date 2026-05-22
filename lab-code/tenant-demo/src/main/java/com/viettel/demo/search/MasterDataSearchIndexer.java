package com.viettel.demo.search;

import com.viettel.demo.entity.MasterData;
import com.viettel.demo.repository.MasterDataRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/*
 * ==============================================================
 * MasterDataSearchIndexer — index dữ liệu sang Elasticsearch
 * ==============================================================
 *
 * [Mục tiêu]
 * Đây là nơi sẽ chuyển dữ liệu từ PostgreSQL entity sang search document
 * và gửi sang Elasticsearch.
 *
 * [Cách hoạt động hiện tại]
 * 1. Convert MasterData -> MasterDataSearchDocument.
 * 2. Gọi MasterDataSearchGateway để index/bulk index bằng official Java API Client.
 * 3. Reindex hiện tại là local/admin lab flow, đọc lại từ PostgreSQL.
 *
 * [TODO sau mini-lab]
 * - Khi update/delete MasterData, nghĩ cách update/delete document tương ứng.
 * - Với production, không public endpoint reindex tùy tiện.
 *
 * [Cảnh báo]
 * - Không coi Elasticsearch là source of truth.
 * - Reindex endpoint nếu có chỉ dùng local/admin lab, không public tùy tiện.
 *
 * ==============================================================
 */
@Service
@ConditionalOnProperty(prefix = "app.search", name = "enabled", havingValue = "true")
public class MasterDataSearchIndexer {

    private final MasterDataRepository repository;
    private final MasterDataSearchGateway gateway;

    public MasterDataSearchIndexer(
            MasterDataRepository repository,
            MasterDataSearchGateway gateway) {
        this.repository = repository;
        this.gateway = gateway;
    }

    public void indexOne(MasterData data) {
        gateway.indexOne(MasterDataSearchDocument.fromEntity(data));
    }

    public MasterDataSearchReindexResponse reindexAll() {
        /*
         * Local/admin lab use case:
         * - PostgreSQL là source of truth.
         * - Reindex đọc toàn bộ master_data từ DB rồi đưa sang Elasticsearch.
         * - Search query sau đó vẫn phải filter tenantId, không search toàn index.
         */
        List<MasterDataSearchDocument> documents = repository.findAll().stream()
                .map(MasterDataSearchDocument::fromEntity)
                .toList();
        int indexedCount = gateway.bulkIndex(documents);
        return new MasterDataSearchReindexResponse(gateway.indexName(), indexedCount);
    }
}
