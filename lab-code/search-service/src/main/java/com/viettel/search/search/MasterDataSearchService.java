package com.viettel.search.search;

import com.viettel.common.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

/*
 * ==============================================================
 * MasterDataSearchService — tenant-aware search
 * ==============================================================
 *
 * [Mục tiêu]
 * Search service phải lấy tenantId từ TenantContext rồi filter query
 * Elasticsearch theo tenantId.
 *
 * [Cách hoạt động hiện tại]
 * 1. Validate keyword.
 * 2. Lấy tenantId từ TenantContext.
 * 3. Gọi gateway tạo Elasticsearch bool query:
 *    - must: keyword search trên code/name/category.
 *    - filter: tenantId và active=true.
 * 4. Trả về document shape an toàn, không trả raw Elasticsearch response.
 *
 * [Rule quan trọng]
 * Không nhận tenantId từ request body/query param.
 *
 * ==============================================================
 */
@Service
public class MasterDataSearchService {

    private static final Logger log = LoggerFactory.getLogger(MasterDataSearchService.class);

    private final MasterDataSearchGateway gateway;
    private final MasterDataSourceClient sourceClient;

    public MasterDataSearchService(MasterDataSearchGateway gateway, MasterDataSourceClient sourceClient) {
        this.gateway = gateway;
        this.sourceClient = sourceClient;
    }

    public List<MasterDataSearchDocument> search(String keyword) {
        Long tenantId = currentTenantId();
        validate(keyword);
        return gateway.search(tenantId, keyword.trim());
    }

    public MasterDataReindexResponse reindexCurrentTenant(String accessToken) {
        Long tenantId = currentTenantId();
        Instant requestedAt = Instant.now();

        List<MasterDataSourceRecord> sourceRecords = sourceClient.listCurrentTenantMasterData(accessToken);
        List<MasterDataSearchDocument> documents = sourceRecords.stream()
                .map(record -> MasterDataSearchDocument.fromSourceRecord(tenantId, record, requestedAt))
                .toList();

        long deletedCount = gateway.deleteTenantDocuments(tenantId);
        int indexedCount = gateway.bulkIndex(documents);

        log.info(
                "Reindexed master data search projection tenantId={}, indexName={}, deletedCount={}, indexedCount={}",
                tenantId,
                gateway.indexName(),
                deletedCount,
                indexedCount
        );

        return new MasterDataReindexResponse(
                tenantId,
                gateway.indexName(),
                indexedCount,
                deletedCount,
                requestedAt
        );
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
}
