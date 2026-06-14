package com.viettel.search.search;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/*
 * ==============================================================
 * SearchProperties — cấu hình search mini-lab
 * ==============================================================
 *
 * [Mục tiêu]
 * Gom các config liên quan đến Elasticsearch/search vào một nơi.
 *
 * [Lưu ý]
 * - search-service là runtime projection riêng, nên cần Elasticsearch khi chạy.
 * - Đọc config này để biết index name và URI.
 *
 * ==============================================================
 */
@Component
@ConfigurationProperties(prefix = "app.search")
public class SearchProperties {

    private String elasticsearchUris = "http://localhost:9200";

    private String masterDataIndex = "master_data_search";

    public String getElasticsearchUris() {
        return elasticsearchUris;
    }

    public void setElasticsearchUris(String elasticsearchUris) {
        this.elasticsearchUris = elasticsearchUris;
    }

    public String getMasterDataIndex() {
        return masterDataIndex;
    }

    public void setMasterDataIndex(String masterDataIndex) {
        this.masterDataIndex = masterDataIndex;
    }
}
