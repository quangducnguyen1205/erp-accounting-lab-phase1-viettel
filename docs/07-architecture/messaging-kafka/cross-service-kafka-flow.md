# Cross-service Kafka Flow

Doc này nối Kafka mini-lab cũ với Phase 1.5 service split.

## 1. General Anatomy

Một cross-service Kafka flow có các phần:

```text
Producer service
  -> event contract
  -> topic
  -> consumer group
  -> consumer service
  -> own database/projection
```

Khác với same-app demo:

- producer và consumer có process/config/log riêng;
- consumer group có ý nghĩa rõ hơn;
- message trở thành integration contract;
- lỗi/retry/idempotency quan trọng hơn.

## 2. Repo-specific Mapping

| Concept | Repo |
|---|---|
| Producer service | `tenant-demo` |
| Producer code | `com.viettel.demo.messaging.KafkaMasterDataEventPublisher` |
| Event | `MasterDataChangedEvent` |
| Topic | `master-data-events` |
| Kafka key | `tenant:{tenantId}:master-data:{aggregateId}` |
| Consumer service | `audit-log-service` and `search-service` |
| Consumer group | `audit-log-service`, `search-service` |
| Consumer code | `com.viettel.audit.event.MasterDataChangedEventConsumer`, `com.viettel.search.event.MasterDataChangedEventConsumer` |
| Projection/table | `audit_log.audit_events`, Elasticsearch index `master_data_search` |
| Read API | `/api/audit-events`, `/api/search/master-data` |

## 3. Runtime Flow

```text
POST /api/master-data
  -> tenant-demo validates token/RBAC/tenant
  -> tenant-demo writes PostgreSQL master_data
  -> tenant-demo publishes MasterDataChangedEvent
  -> Kafka stores message in topic master-data-events
  -> audit-log-service polls message
  -> audit-log-service stores audit row
  -> search-service polls same message
  -> search-service updates Elasticsearch projection
  -> GET /api/audit-events reads audit rows by tenant
  -> GET /api/search/master-data searches projection by tenant
```

## 4. What Kafka UI Should Show

Open Kafka UI:

```text
http://localhost:18082
```

Inspect:

- topic `master-data-events`;
- message key starts with `tenant:`;
- value contains `eventId`, `tenantId`, `aggregateId`, `changeType`;
- consumer group `audit-log-service`;
- consumer group `search-service` if search service is running;
- lag should go back near 0 after consumer handles messages.

## 5. Tenant Safety

Tenant appears in two places:

- event payload: `tenantId`;
- read API: `tenant_id` claim from JWT.

The consumer stores event tenant. The read API still filters by JWT tenant. Do not trust client-provided tenant id.

## 6. Caveats

- No outbox pattern yet.
- DB save and Kafka publish are not atomic.
- No retry topic/DLT yet.
- Event schema is duplicated DTO, not schema registry.
- Kafka is not source of truth; PostgreSQL remains source of truth for business data.

## 7. Verification Commands

```bash
cd lab-code
make kafka-up
make kafka-ui-up
make kong-up
```

Mở terminal riêng cho audit service Maven host-run:

```bash
cd lab-code
make audit-log-run-logs
```

Mở terminal riêng cho search service Maven host-run nếu demo Elasticsearch projection:

```bash
cd lab-code
make elastic-up
make search-run-logs
```

Then create/update `master_data` through Kong or HTTP file:

```text
lab-code/tenant-demo/http/cross-service-kafka-demo.http
```

Finally call:

```bash
curl -i \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "X-Request-Id: audit-read-001" \
  http://localhost:18000/api/audit-events
```

Kết quả đã verify ở local:

- topic `master-data-events` có message `MasterDataChangedEvent`;
- Kafka key dạng `tenant:1:master-data:<id>`;
- consumer group `audit-log-service` có lag `0`;
- consumer group `search-service` có lag `0` nếu search service đang chạy;
- audit service lưu event vào `audit_log.audit_events`;
- search service index/update document trong Elasticsearch;
- tenant 1 đọc được audit event qua Kong;
- tenant 2 không thấy event tenant 1.
