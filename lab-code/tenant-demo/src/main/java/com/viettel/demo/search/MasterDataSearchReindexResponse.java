package com.viettel.demo.search;

/*
 * Response nhỏ cho endpoint reindex local.
 * Không trả raw Elasticsearch bulk response để tránh leak chi tiết không cần thiết.
 */
public record MasterDataSearchReindexResponse(
        String indexName,
        int indexedCount
) {
}
