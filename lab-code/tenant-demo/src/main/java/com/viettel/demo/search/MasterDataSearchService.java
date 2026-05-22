package com.viettel.demo.search;

import com.viettel.demo.context.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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

    public List<MasterDataSearchDocument> search(String keyword) {
        Long tenantId = currentTenantId();
        /*
         * TODO:
         * - Dùng tenantId này trong filter Elasticsearch.
         * - Không search toàn index.
         * - Nếu keyword blank, quyết định trả 400 hoặc list top documents
         *   trong tenant hiện tại.
         */
        throw new ResponseStatusException(
                HttpStatus.NOT_IMPLEMENTED,
                "TODO: implement tenant-aware Elasticsearch search for tenant " + tenantId
        );
    }

    private Long currentTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant context is missing");
        }
        return tenantId;
    }
}

