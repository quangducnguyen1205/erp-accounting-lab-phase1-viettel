package com.viettel.demo.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/*
 * ==============================================================
 * ApplicationMetrics — custom metrics nhỏ cho Phase 1 lab
 * ==============================================================
 *
 * Counter: đếm số lần một việc xảy ra, chỉ tăng.
 * Timer: đo thời lượng và phân phối duration.
 *
 * Lưu ý quan trọng:
 * - Tags phải low-cardinality: chỉ dùng vài giá trị cố định.
 * - Không tag tenantId, requestId, userId, code, eventId hoặc token.
 * - Metrics dùng để nhìn xu hướng tổng hợp, không thay request log/debug.
 *
 * ==============================================================
 */
@Component
public class ApplicationMetrics {

    private static final String MASTER_DATA_CACHE_REQUESTS = "tenant_demo.master_data.cache.requests";
    private static final String MASTER_DATA_CACHE_PUTS = "tenant_demo.master_data.cache.puts";
    private static final String MASTER_DATA_CACHE_ERRORS = "tenant_demo.master_data.cache.errors";
    private static final String MASTER_DATA_GET_BY_CODE_DURATION = "tenant_demo.master_data.get_by_code.duration";
    private static final String KAFKA_PUBLISH_REQUESTS = "tenant_demo.kafka.publish.requests";
    private static final String KAFKA_PUBLISH_DURATION = "tenant_demo.kafka.publish.duration";

    private static final String EVENT_MASTER_DATA_CHANGED = "master_data_changed";

    private final MeterRegistry meterRegistry;

    public ApplicationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordMasterDataCacheHit() {
        meterRegistry.counter(MASTER_DATA_CACHE_REQUESTS, "result", "hit").increment();
    }

    public void recordMasterDataCacheMiss() {
        meterRegistry.counter(MASTER_DATA_CACHE_REQUESTS, "result", "miss").increment();
    }

    public void recordMasterDataCachePut() {
        meterRegistry.counter(MASTER_DATA_CACHE_PUTS).increment();
    }

    public void recordMasterDataCacheError(String operation) {
        meterRegistry.counter(MASTER_DATA_CACHE_ERRORS, "operation", operation).increment();
    }

    public void recordMasterDataGetByCodeDuration(boolean cacheEnabled, String result, Duration duration) {
        Timer.builder(MASTER_DATA_GET_BY_CODE_DURATION)
                .tag("cache", cacheEnabled ? "enabled" : "disabled")
                .tag("result", result)
                .register(meterRegistry)
                .record(duration);
    }

    public void recordKafkaPublishSuccess(Duration duration) {
        recordKafkaPublish("success", duration);
    }

    public void recordKafkaPublishFailure(Duration duration) {
        recordKafkaPublish("failure", duration);
    }

    private void recordKafkaPublish(String result, Duration duration) {
        meterRegistry.counter(
                KAFKA_PUBLISH_REQUESTS,
                "event", EVENT_MASTER_DATA_CHANGED,
                "result", result
        ).increment();

        Timer.builder(KAFKA_PUBLISH_DURATION)
                .tag("event", EVENT_MASTER_DATA_CHANGED)
                .tag("result", result)
                .register(meterRegistry)
                .record(duration);
    }
}
