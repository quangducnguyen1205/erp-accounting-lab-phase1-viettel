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

Trong local dev, `audit-log-service` là Java backend service chạy bằng Maven/IntelliJ trên host, giống `tenant-demo`.

Service kết nối mặc định tới:

- PostgreSQL local Docker qua `localhost:5432`;
- Keycloak local Docker qua `http://localhost:18080`;
- Kafka local Docker qua host listener `localhost:19092`.

Docker vẫn dùng cho infra/tooling như PostgreSQL, Keycloak, Kafka, Kong, Kafka UI và Loki/Grafana. Không cần Dockerize Java service này cho workflow local chính.

## Chạy bằng Makefile

Từ `lab-code/`:

```bash
make audit-log-run
make audit-log-status
```

Khi muốn Grafana Loki đọc được log của service, chạy target file-log:

```bash
make audit-log-run-logs
```

Target này ghi log vào:

```text
lab-code/logs/audit-log-service.log
```

`make audit-log-run-logs` xóa file log cũ ở đầu lần chạy để demo mới dễ đọc. Nó không tự xóa log khi dừng service, vì sau demo thường vẫn muốn mở Grafana/Loki hoặc đọc file. Dọn thủ công bằng:

```bash
make logs-list
make logs-clean
```

Validate Maven:

```bash
make audit-log-validate
```

Dừng service bằng `Ctrl+C` trong terminal Maven/IntelliJ đang chạy.

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
3. Start `audit-log-service` bằng `make audit-log-run` hoặc `make audit-log-run-logs`.
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
- Docker runtime riêng cho Java service này đã được bỏ khỏi workflow chính để local dev nhẹ hơn; Docker vẫn dùng cho infra/tooling.
