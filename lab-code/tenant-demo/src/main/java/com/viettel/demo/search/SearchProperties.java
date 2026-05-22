package com.viettel.demo.search;

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
 * - Search tắt mặc định để app/test hiện tại không phụ thuộc Elasticsearch.
 * - Chưa thêm Elasticsearch Java client trong skeleton này.
 * - Khi tự implement, đọc config này để biết index name và URI.
 *
 * ==============================================================
 */
@Component
@ConfigurationProperties(prefix = "app.search")
public class SearchProperties {

    private boolean enabled = false;

    private String elasticsearchUris = "http://localhost:9200";

    private String masterDataIndex = "master_data_search";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

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

