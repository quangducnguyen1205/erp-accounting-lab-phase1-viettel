# Audit Log Service Split Plan

Doc này chốt boundary cho service split đầu tiên của Phase 1.5.

## 1. Vì sao chọn audit-log-service trước?

`tenant-demo` đã có `MasterDataChangedEvent`. Nếu thêm một service nhỏ consume event này, Kafka lập tức chuyển từ same-app demo sang cross-service flow thật:

```text
tenant-demo
  -> save master_data
  -> publish MasterDataChangedEvent
  -> Kafka
  -> audit-log-service
  -> store audit event
```

Lý do chọn hướng này:

- không cần mở domain kế toán mới;
- trách nhiệm service rõ: ghi nhận event thay đổi;
- demo được Kafka topic/message/consumer group trong Kafka UI;
- demo được logs nhiều service trong Loki/Grafana;
- Kong có thêm route thật ngoài `/api/master-data`.

## 2. Service Responsibility

`audit-log-service` sở hữu:

- Kafka consumer cho `MasterDataChangedEvent`;
- bảng/schema audit của riêng nó;
- read-only API để đọc audit event theo tenant hiện tại;
- idempotency tối thiểu bằng `eventId`.

Service này không sở hữu:

- master data business rule;
- bảng `master_data`;
- Keycloak user/role management;
- route/gateway policy;
- file/search/cache business logic.

## 3. Data Ownership

Trong demo local, service dùng chung PostgreSQL container với `tenant-demo` để đỡ nặng hạ tầng, nhưng dữ liệu nằm trong schema riêng:

```text
PostgreSQL container erp-postgres
  database erpdb
    schema public      -> tenant-demo owns tenants/master_data/file_metadata
    schema audit_log   -> audit-log-service owns audit_events
```

Đây là demo-level split. Production có thể dùng database riêng cho service.

## 4. Event Ownership

`tenant-demo` publish:

```text
MasterDataChangedEvent
```

Event bắt buộc có:

- `eventId`: idempotency;
- `tenantId`: tenant safety;
- `aggregateType` / `aggregateId`;
- `code`;
- `changeType`;
- `occurredAt`;
- `source`.

`audit-log-service` duplicate event DTO có chủ đích, không import class từ `tenant-demo`. Sau này có thể thay bằng shared contract module hoặc schema registry.

## 5. Tenant Isolation

Rule:

- consumer lưu `tenantId` từ event;
- read API không nhận `tenantId` từ query/body;
- read API validate JWT, lấy `tenant_id` claim rồi filter database theo tenant đó;
- tenant 2 không đọc được audit event tenant 1.

## 6. Idempotency

Kafka có thể deliver trùng message, nên audit table có unique constraint:

```text
UNIQUE(event_id)
```

Nếu duplicate:

- service log ngắn;
- skip;
- không crash app.

## 7. Giới hạn hiện tại

- Chưa có outbox pattern: DB write trong `tenant-demo` và Kafka publish chưa atomic.
- Chưa có retry topic/DLT.
- Chưa có schema registry/schema evolution.
- Chưa có audit compliance đầy đủ.
- Service split này để học boundary, không phải production audit platform.

## 8. Tiêu chí hoàn thành

- `audit-log-service` chạy riêng ở port `8082`.
- Consume topic `master-data-events` bằng group `audit-log-service`.
- Lưu event vào `audit_log.audit_events`.
- Kong route `/api/audit-events` tới service.
- API read audit events tenant-aware.
- Kafka UI thấy topic/message/consumer group.
- Logs có thể xem theo container/service trong Loki nếu chạy log aggregation.
