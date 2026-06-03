package com.viettel.demo.messaging;

import com.viettel.demo.entity.MasterData;

import java.time.Instant;
import java.util.UUID;

/*
 * ==============================================================
 * MasterDataChangedEvent — event DTO cho Kafka mini-lab
 * ==============================================================
 *
 * Đây là DTO/projection gửi qua message, không phải JPA Entity.
 * Event phải có tenantId để consumer không xử lý dữ liệu như global data.
 *
 * TODO tự code sau:
 * - quyết định publish ở create/update/delete nào;
 * - cân nhắc eventId cho idempotency ở consumer;
 * - không nhét token/secret/raw entity lớn vào event.
 *
 * ==============================================================
 */
public record MasterDataChangedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long tenantId,
        String aggregateType,
        Long aggregateId,
        String code,
        String changeType,
        String source
) {

    public static MasterDataChangedEvent from(MasterData data, String changeType) {
        return new MasterDataChangedEvent(
                UUID.randomUUID().toString(),
                "MASTER_DATA_CHANGED",
                Instant.now(),
                data.getTenantId(),
                "MASTER_DATA",
                data.getId(),
                data.getCode(),
                changeType,
                "tenant-demo"
        );
    }

    public String kafkaKey() {
        return "tenant:%d:master-data:%d".formatted(tenantId, aggregateId);
    }
}
