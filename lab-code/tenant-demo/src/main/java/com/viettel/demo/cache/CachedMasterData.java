package com.viettel.demo.cache;

import com.viettel.demo.entity.MasterData;

import java.time.LocalDateTime;

/*
 * ==============================================================
 * CachedMasterData — DTO an toàn để lưu trong Redis
 * ==============================================================
 *
 * [Vì sao không cache thẳng JPA Entity?]
 * Entity thuộc persistence layer và có lifecycle riêng của JPA.
 * Cache nên lưu một projection nhỏ, đủ field để trả API read response.
 *
 * ==============================================================
 */
public record CachedMasterData(
        Long id,
        Long tenantId,
        String code,
        String name,
        String category,
        Boolean isActive,
        LocalDateTime createdAt
) {
    /*
     * Redis mini-lab:
     * - Redis lưu DTO nhỏ này, không lưu raw JPA Entity.
     * - Khi cache hit, map DTO về detached read copy để giữ API response hiện tại.
     */
    public static CachedMasterData fromEntity(MasterData data) {
        return new CachedMasterData(
                data.getId(),
                data.getTenantId(),
                data.getCode(),
                data.getName(),
                data.getCategory(),
                data.getIsActive(),
                data.getCreatedAt()
        );
    }

    public MasterData toDetachedEntity() {
        return MasterData.detachedReadCopy(
                id,
                tenantId,
                code,
                name,
                category,
                isActive,
                createdAt
        );
    }
}
