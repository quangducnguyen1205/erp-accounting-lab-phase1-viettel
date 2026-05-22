package com.viettel.demo.search;

import com.viettel.demo.entity.MasterData;

/*
 * ==============================================================
 * MasterDataSearchDocument — document đưa vào Elasticsearch
 * ==============================================================
 *
 * [Mục tiêu]
 * Tách rõ search document khỏi JPA entity.
 *
 * PostgreSQL entity là source of truth.
 * Elasticsearch document là bản projection phục vụ search.
 *
 * [TODO tự implement sau]
 * - Quyết định field nào là keyword/exact, field nào là text search.
 * - Nếu dùng Elasticsearch Java client, class này có thể là DTO để index.
 * - Đảm bảo mọi document luôn có tenantId.
 *
 * ==============================================================
 */
public record MasterDataSearchDocument(
        Long id,
        Long tenantId,
        String code,
        String name,
        String category,
        Boolean active
) {

    public static MasterDataSearchDocument fromEntity(MasterData data) {
        return new MasterDataSearchDocument(
                data.getId(),
                data.getTenantId(),
                data.getCode(),
                data.getName(),
                data.getCategory(),
                data.getIsActive()
        );
    }
}

