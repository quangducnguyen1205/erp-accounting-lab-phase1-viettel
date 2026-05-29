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
         * TODO Redis mini-lab:
         * - Tự implement key có tenant scope.
         * - Gợi ý format: tenant:{tenantId}:master-data:code:{code}
         * - Không dùng key global kiểu master-data:code:{code}.
         */
        throw new UnsupportedOperationException("TODO: build tenant-safe Redis key for MasterData by code");
    }
}
