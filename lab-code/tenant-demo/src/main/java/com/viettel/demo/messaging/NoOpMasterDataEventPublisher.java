package com.viettel.demo.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/*
 * ==============================================================
 * NoOpMasterDataEventPublisher — messaging disabled path
 * ==============================================================
 *
 * Khi APP_MESSAGING_ENABLED=false, app giữ hành vi cũ: chỉ ghi PostgreSQL,
 * không cần Kafka đang chạy. Đây là đường mặc định cho app-test.
 *
 * ==============================================================
 */
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpMasterDataEventPublisher implements MasterDataEventPublisher {

    @Override
    public void publish(MasterDataChangedEvent event) {
        // Messaging disabled: intentionally do nothing.
    }
}
