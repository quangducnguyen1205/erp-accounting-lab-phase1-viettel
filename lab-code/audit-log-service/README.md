# Audit Log Service

`audit-log-service` là service split đầu tiên trong Phase 1.5.

Mục tiêu của service này:

- consume `MasterDataChangedEvent` từ Kafka;
- lưu audit record vào schema riêng `audit_log`;
- expose read-only API để đọc audit event theo tenant hiện tại;
- giúp Kafka trở thành cross-service flow thật, không còn chỉ same-app producer/consumer.

## Kiến trúc local

```text
tenant-demo
  -> publish MasterDataChangedEvent
  -> Kafka topic master-data-events
  -> audit-log-service consumer group audit-log-service
  -> PostgreSQL schema audit_log
  -> GET /api/audit-events
```

Trong Docker Compose, service kết nối:

- PostgreSQL host qua `host.docker.internal:5432`;
- Keycloak JWKS qua `host.docker.internal:18080`;
- Kafka qua Docker network `viettel-kafka-net`, bootstrap server `kafka:9092`.

## Chạy bằng Makefile

Từ `lab-code/`:

```bash
make audit-log-up
make audit-log-status
make audit-log-logs
```

Stop:

```bash
make audit-log-down
```

Validate Maven:

```bash
make audit-log-validate
```

## API

Service chạy ở `http://localhost:8082`.

Kong route sau khi chạy `make kong-up`:

```text
http://localhost:18000/api/audit-events
```

Endpoints:

- `GET /api/audit-events?limit=50`
- `GET /api/audit-events/{eventId}`
- `GET /actuator/health`

Audit API yêu cầu Bearer token hợp lệ. Service lấy `tenant_id` từ JWT đã validate và luôn filter theo tenant hiện tại.

## Demo nhanh

1. Start PostgreSQL, Keycloak, Kafka.
2. Start `tenant-demo` với `APP_MESSAGING_ENABLED=true`.
3. Start `audit-log-service`.
4. Create/update `master_data` qua `tenant-demo` hoặc Kong.
5. Mở Kafka UI để xem topic `master-data-events`.
6. Gọi `GET /api/audit-events` qua Kong bằng token cùng tenant.

E2E đã verify ở local:

- `tenant1-user` có role `ACCOUNTANT` create `master_data` qua Kong và nhận `201`.
- `tenant-demo` publish `MasterDataChangedEvent` với key dạng `tenant:{tenantId}:master-data:{id}`.
- `audit-log-service` consume/store event vào `audit_log.audit_events`.
- `tenant1-user` gọi `GET /api/audit-events` qua Kong và thấy audit event vừa tạo.
- `tenant2-user` có role `VIEWER` không thấy event tenant 1.
- `tenant2-user` create `master_data` trả `403`, nên không tạo audit event cho action fail.

## Caveat

- Chưa có outbox pattern, nên DB write trong `tenant-demo` và Kafka publish chưa atomic.
- Chưa có retry topic/DLT.
- Event DTO đang duplicate giữa services; production nên dùng shared contract hoặc schema registry.
- PostgreSQL local dùng cùng container/database `erpdb` nhưng schema riêng `audit_log` để giữ demo đơn giản.
