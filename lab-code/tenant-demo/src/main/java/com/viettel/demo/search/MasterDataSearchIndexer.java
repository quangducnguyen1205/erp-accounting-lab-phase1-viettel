package com.viettel.demo.search;

import com.viettel.demo.entity.MasterData;
import org.springframework.stereotype.Service;

import java.util.List;

/*
 * ==============================================================
 * MasterDataSearchIndexer — skeleton index dữ liệu sang Elasticsearch
 * ==============================================================
 *
 * [Mục tiêu]
 * Đây là nơi sẽ chuyển dữ liệu từ PostgreSQL entity sang search document
 * và gửi sang Elasticsearch.
 *
 * [TODO tự code]
 * 1. Thêm Elasticsearch Java client dependency nếu quyết định dùng client.
 * 2. Tạo index `master_data_search` nếu chưa tồn tại.
 * 3. Convert MasterData -> MasterDataSearchDocument.
 * 4. Index từng document hoặc bulk index.
 * 5. Khi update/delete MasterData, nghĩ cách update/delete document tương ứng.
 *
 * [Cảnh báo]
 * - Không coi Elasticsearch là source of truth.
 * - Reindex endpoint nếu có chỉ dùng local/admin lab, không public tùy tiện.
 *
 * ==============================================================
 */
@Service
public class MasterDataSearchIndexer {

    private final SearchProperties properties;

    public MasterDataSearchIndexer(SearchProperties properties) {
        this.properties = properties;
    }

    public void indexOne(MasterData data) {
        /*
         * TODO:
         * - Kiểm tra properties.isEnabled().
         * - Convert bằng MasterDataSearchDocument.fromEntity(data).
         * - Gửi document sang Elasticsearch.
         */
        throw new UnsupportedOperationException("TODO: implement Elasticsearch indexOne");
    }

    public void reindexAll(List<MasterData> data) {
        /*
         * TODO:
         * - Chỉ chạy khi APP_SEARCH_ENABLED=true.
         * - Bulk index dữ liệu hiện có.
         * - Log số document đã index, không log data nhạy cảm.
         */
        throw new UnsupportedOperationException("TODO: implement Elasticsearch reindexAll");
    }

    public SearchProperties properties() {
        return properties;
    }
}

