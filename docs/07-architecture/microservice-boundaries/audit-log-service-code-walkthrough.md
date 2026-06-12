# Audit Log Service Code Walkthrough

Doc này giải thích code/config của `lab-code/audit-log-service`. Theo chuẩn repo, phần đầu nói model chung của một Kafka consumer service, sau đó mới map vào file cụ thể.

## 1. General Anatomy Of A Kafka Consumer Service

Một service consume event thường có các phần:

| Phần | Vai trò |
|---|---|
| Event contract | DTO/schema mô tả message Kafka. |
| Kafka consumer config | Bootstrap server, topic, group id, deserializer. |
| Listener/handler | Nhận message và gọi use-case service. |
| Persistence model | Entity/table riêng của service. |
| Idempotency guard | Chống xử lý duplicate event. |
| Read API | API để service expose dữ liệu nó sở hữu. |
| Security/tenant filter | Validate token, lấy tenant context, filter data. |
| Observability | Logs, health, metrics cơ bản. |

Consumer service không nên import JPA entity/repository từ service khác. Event là contract, database là ownership riêng.

## 2. Repo-specific Mapping

| Concept chung | File trong repo | Vì sao chọn |
|---|---|---|
| App entrypoint | `AuditLogServiceApplication.java` | Spring Boot app độc lập, port riêng. |
| Event contract | `event/MasterDataChangedEvent.java` | Duplicate contract để không couple source code với `tenant-demo`. |
| Kafka config | `event/KafkaConsumerConfig.java`, `event/AuditKafkaProperties.java` | Typed JSON deserialization về event DTO. |
| Kafka listener | `event/MasterDataChangedEventConsumer.java` | Consume topic `master-data-events`, group `audit-log-service`. |
| Audit entity | `audit/AuditEvent.java` | Dữ liệu service sở hữu. |
| Repository | `audit/AuditEventRepository.java` | Query tenant-aware theo `tenantId`. |
| Use-case service | `audit/AuditEventService.java` | Idempotency, save audit, read current tenant events. |
| API | `audit/AuditEventController.java` | Read-only endpoints `/api/audit-events`. |
| Security | `security/*` | Validate Keycloak JWT, map roles, set tenant context. |
| Request logs | `observability/RequestLoggingFilter.java` | requestId + method/path/status/duration. |
| DB migration | `db/migration/V1__create_audit_events.sql` | Schema/table/index riêng. |
| Docker runtime | `Dockerfile`, `docker-compose.yml` | Docker-first service run. |

## 3. Runtime Flow

```text
ACCOUNTANT creates master_data
  -> tenant-demo saves PostgreSQL row
  -> tenant-demo publishes MasterDataChangedEvent
  -> Kafka topic master-data-events
  -> audit-log-service @KafkaListener consumes event
  -> AuditEventService checks eventId duplicate
  -> audit_log.audit_events insert
  -> user calls GET /api/audit-events
  -> audit-log-service validates JWT
  -> JwtTenantContextFilter extracts tenant_id
  -> repository filters by tenantId
  -> safe response DTO
```

## 4. Code Notes

### Event DTO

`MasterDataChangedEvent` trong audit service có cùng fields với producer event. Đây là contract copy có chủ đích.

Không làm ở Phase 1.5:

- shared Maven module;
- schema registry;
- Avro/Protobuf;
- version negotiation.

### Consumer Group

Group id:

```text
audit-log-service
```

Ý nghĩa: Kafka nhớ offset riêng cho service này. Nếu sau này chạy nhiều instance audit service cùng group, Kafka chia partition giữa các instance.

### Idempotency

`audit_events.event_id` unique.

Code check trước bằng `existsByEventId`, sau đó vẫn catch `DataIntegrityViolationException` vì duplicate có thể xảy ra giữa check và insert.

### Tenant Safety

Audit read API không có `tenantId` param. Tenant đến từ JWT:

```text
validated Jwt -> tenant_id claim -> TenantContext -> repository tenant filter
```

## 5. Docker Config Anatomy

`docker-compose.yml` có ba nhóm config quan trọng:

- DB: `DB_HOST=host.docker.internal`, `DB_SCHEMA=audit_log`;
- Keycloak: issuer vẫn là `localhost`, JWKS dùng `host.docker.internal` khi chạy trong container;
- Kafka: container dùng `kafka:9092` trên Docker network `viettel-kafka-net`.

Lý do:

- token issuer phải khớp claim `iss`, thường là `http://localhost:18080/...`;
- container không thể dùng `localhost:18080` để gọi Keycloak host, nên cần `KEYCLOAK_JWK_SET_URI=http://host.docker.internal:18080/.../certs`;
- Kafka container-to-container phải dùng internal listener `kafka:9092`, không dùng host listener `localhost:19092`.

## 6. Common Mistakes

- Import entity/repository từ `tenant-demo`.
- Consumer ghi vào bảng `master_data`.
- Read API nhận `tenantId` từ query param.
- Quên unique `event_id`, tạo duplicate audit rows.
- Dùng `localhost:19092` từ trong container audit service.
- Dùng `localhost:18080` làm JWKS URL từ trong container.
- Nghĩ audit service làm cho Kafka publish atomic với DB commit.
- Log full token/payload nhạy cảm.

## 7. Verification

Validate:

```bash
mvn -f lab-code/audit-log-service/pom.xml validate
docker compose -f lab-code/audit-log-service/docker-compose.yml config
```

Run:

```bash
cd lab-code
make db-up
make keycloak-up
make keycloak-setup
make kafka-up
make audit-log-up
make kong-up
```

Verify:

```bash
curl -i http://localhost:8082/actuator/health
curl -i http://localhost:18000/api/audit-events
```

Missing token should return `401`. With a valid token, response only includes current tenant audit events.

Manual E2E đã verify:

- `tenant1-user` create `master_data` qua Kong nhận `201`;
- tenant-demo log có `Published Kafka event`;
- audit service log có `Consumed cross-service event` và `Stored audit event`;
- `GET /api/audit-events` qua Kong bằng tenant 1 token trả event vừa tạo;
- tenant 2 token trả list rỗng cho event tenant 1;
- VIEWER create bị `403`.
