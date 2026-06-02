package com.viettel.demo.cache;

import org.springframework.stereotype.Component;

/*
 * ==============================================================
 * MasterDataCacheKeyFactory — sinh Redis key tenant-safe
 * ==============================================================
 *
 * Rule học tập:
 * Nếu response phụ thuộc tenant, Redis key phải có tenantId.
 * Không dùng key kiểu `master-data:code:LAPTOP-01` vì code có thể
 * tồn tại ở nhiều tenant.
 *
 * ==============================================================
 */
@Component
public class MasterDataCacheKeyFactory {

    public String byCode(Long tenantId, String code) {
        /*
         * Redis mini-lab:
         * - Key có tenant scope để tenant 1/2 không dùng chung cache entry.
         * - Không dùng key global kiểu master-data:code:{code}.
         */
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId cannot be null");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code cannot be blank");
        }
        return String.format("tenant:%d:master-data:code:%s", tenantId, code);
    }
}
