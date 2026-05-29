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
         * TODO Redis mini-lab:
         * 1. Build key bằng keyFactory.byCode(tenantId, code).
         * 2. Dùng redisTemplate.opsForValue().get(key).
         * 3. Nếu null -> log/cache miss và return Optional.empty().
         * 4. Nếu có JSON -> dùng objectMapper.readValue(...) thành CachedMasterData.
         * 5. Không catch/nuốt lỗi âm thầm; lỗi parse cache nên rõ ràng để dễ học.
         */
        throw new UnsupportedOperationException("TODO: read MasterData from Redis cache");
    }

    public void putByCode(Long tenantId, String code, MasterData data) {
        /*
         * TODO Redis mini-lab:
         * 1. Map MasterData -> CachedMasterData.
         * 2. Serialize JSON bằng objectMapper.writeValueAsString(...).
         * 3. Set Redis value với TTL: cacheProperties.getMasterDataTtl().
         */
        throw new UnsupportedOperationException("TODO: write MasterData to Redis cache");
    }

    public void evictByCode(Long tenantId, String code) {
        /*
         * TODO Redis mini-lab:
         * - Khi update/delete MasterData sau này, xóa key liên quan để tránh stale data.
         * - Chỉ implement sau khi read cache path đã chạy rõ.
         */
        throw new UnsupportedOperationException("TODO: evict MasterData Redis cache by code");
    }
}
