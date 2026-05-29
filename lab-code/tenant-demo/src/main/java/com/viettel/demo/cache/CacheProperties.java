package com.viettel.demo.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/*
 * ==============================================================
 * CacheProperties — cấu hình Redis/cache mini-lab
 * ==============================================================
 *
 * [Mục tiêu]
 * Gom config cache vào một nơi:
 * - bật/tắt cache;
 * - TTL cho master_data cache.
 *
 * Cache tắt mặc định để app-test không cần Redis. Khi bật cache,
 * Redis chỉ là bản sao tạm; PostgreSQL vẫn là source of truth.
 *
 * ==============================================================
 */
@Component
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

    private boolean enabled = false;

    private long masterDataTtlSeconds = 300;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getMasterDataTtlSeconds() {
        return masterDataTtlSeconds;
    }

    public void setMasterDataTtlSeconds(long masterDataTtlSeconds) {
        this.masterDataTtlSeconds = masterDataTtlSeconds;
    }

    public Duration getMasterDataTtl() {
        return Duration.ofSeconds(masterDataTtlSeconds);
    }
}
