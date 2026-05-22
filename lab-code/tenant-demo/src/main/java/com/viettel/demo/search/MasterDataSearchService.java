package com.viettel.demo.search;

import com.viettel.demo.context.TenantContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
@ConditionalOnProperty(prefix = "app.search", name = "enabled", havingValue = "true")
public class MasterDataSearchService {

    private final MasterDataSearchGateway gateway;

    public MasterDataSearchService(MasterDataSearchGateway gateway) {
        this.gateway = gateway;
    }

    public List<MasterDataSearchDocument> search(String keyword) {
        Long tenantId = currentTenantId();
        validate(keyword);
        return gateway.search(tenantId, keyword.trim());
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
