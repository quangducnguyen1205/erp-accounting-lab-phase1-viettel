package com.viettel.search.search;

import java.time.Instant;

public record MasterDataReindexResponse(
        Long tenantId,
        String indexName,
        int indexedCount,
        long deletedCount,
        Instant requestedAt
) {
}
