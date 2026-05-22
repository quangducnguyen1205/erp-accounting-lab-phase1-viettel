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
 * [Shape hiện tại]
 * - id: dùng làm document id trong Elasticsearch.
 * - tenantId: bắt buộc có để mọi search query filter theo tenant.
 * - code/name/category: các field search cơ bản của mini-lab.
 * - active: dùng để bỏ qua bản ghi đã soft delete nếu cần.
 *
 * [Ghi nhớ]
 * Đây là DTO/projection cho search index, không phải source of truth.
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
