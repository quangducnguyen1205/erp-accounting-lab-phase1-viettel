package com.viettel.demo.cache;

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
     * TODO Redis mini-lab:
     * - Tự viết mapper từ MasterData entity sang CachedMasterData.
     * - Nếu muốn trả API response từ cache, cân nhắc map DTO này về response DTO
     *   thay vì biến nó thành managed JPA entity.
     */
}
