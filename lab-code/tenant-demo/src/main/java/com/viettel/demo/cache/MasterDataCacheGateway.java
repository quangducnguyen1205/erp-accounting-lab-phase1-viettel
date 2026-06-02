package com.viettel.demo.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viettel.demo.entity.MasterData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/*
 * ==============================================================
 * MasterDataCacheGateway — adapter nhỏ quanh Redis
 * ==============================================================
 *
 * [Vai trò]
 * Gateway này giữ chi tiết Redis:
 * - build key qua key factory;
 * - serialize/deserialize JSON;
 * - set TTL.
 *
 * Service nghiệp vụ quyết định cache-aside flow. Controller không biết Redis.
 *
 * ==============================================================
 */
@Component
public class MasterDataCacheGateway {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheProperties cacheProperties;
    private final MasterDataCacheKeyFactory keyFactory;

    public MasterDataCacheGateway(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            CacheProperties cacheProperties,
            MasterDataCacheKeyFactory keyFactory
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheProperties = cacheProperties;
        this.keyFactory = keyFactory;
    }

    public Optional<CachedMasterData> getByCode(Long tenantId, String code) {
        /*
         * Redis mini-lab:
         * 1. Build key bằng keyFactory.byCode(tenantId, code).
         * 2. Dùng redisTemplate.opsForValue().get(key).
         * 3. Nếu null -> log/cache miss và return Optional.empty().
         * 4. Nếu có JSON -> dùng objectMapper.readValue(...) thành CachedMasterData.
         * 5. Không catch/nuốt lỗi âm thầm; lỗi parse cache nên rõ ràng để dễ học.
         */
        String key = keyFactory.byCode(tenantId, code);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            // Cache miss
            System.out.printf("Cache miss for key: %s%n", key);
            return Optional.empty();
        }
        CachedMasterData cachedMasterData;
        try {
            cachedMasterData = objectMapper.readValue(json, CachedMasterData.class);
        } catch (Exception e) {
            // Log lỗi parse cache rõ ràng để dễ debug.
            System.err.printf("Failed to parse cache for key: %s, error: %s%n", key, e.getMessage());
            return Optional.empty();
        }
        System.out.printf("Cache hit for key: %s%n", key);
        return Optional.of(cachedMasterData);
    }

    public void putByCode(Long tenantId, String code, MasterData data) {
        /*
         * Redis mini-lab:
         * 1. Map MasterData -> CachedMasterData.
         * 2. Serialize JSON bằng objectMapper.writeValueAsString(...).
         * 3. Set Redis value với TTL: cacheProperties.getMasterDataTtl().
         */
        CachedMasterData cachedMasterData = CachedMasterData.fromEntity(data);
        String json;
        try {
            json = objectMapper.writeValueAsString(cachedMasterData);
        } catch (Exception e) {
            // Log lỗi serialize cache rõ ràng để dễ debug.
            System.err.printf("Failed to serialize cache for tenantId: %d, code: %s, error: %s%n",
                    tenantId, code, e.getMessage());
            return;
        }
        redisTemplate.opsForValue().set(
                keyFactory.byCode(tenantId, code),
                json,
                cacheProperties.getMasterDataTtl()
        );
    }

    public void evictByCode(Long tenantId, String code) {
        /*
         * Redis mini-lab:
         * - Khi update/delete MasterData sau này, xóa key liên quan để tránh stale data.
         * - Method này đã có, nhưng update/delete chưa wire eviction trong mini-lab này.
         */
        String key = keyFactory.byCode(tenantId, code);
        redisTemplate.delete(key);
    }
}
