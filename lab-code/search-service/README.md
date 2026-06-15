# Search Service

`search-service` sở hữu Elasticsearch projection cho `Master Data Portal`.

Service này chạy Maven/IntelliJ-first giống các Java backend service khác:

```text
tenant-demo
  -> Kafka MasterDataChangedEvent
    -> search-service :8084
      -> Elasticsearch index master_data_search

React Web UI
  -> Kong Gateway /api/search/master-data
    -> search-service :8084
```

PostgreSQL trong `tenant-demo` vẫn là source of truth. Elasticsearch chỉ là projection phục vụ tìm kiếm nhanh và có thể trễ vài giây sau create/update/deactivate.

## Chạy service

Start Docker infra trước:

```bash
cd lab-code
make keycloak-up
make keycloak-setup
make -f Makefile.legacy kafka-up
make -f Makefile.legacy elastic-up
make -f Makefile.legacy kong-up
```

Start `tenant-demo` với Kafka enabled ở terminal khác để publish `MasterDataChangedEvent`:

```bash
cd lab-code
APP_AUTH_MODE=keycloak \
APP_MESSAGING_ENABLED=true \
KAFKA_BOOTSTRAP_SERVERS=localhost:19092 \
make -f Makefile.legacy app-run-logs
```

Run search-service:

```bash
cd lab-code
make -f Makefile.legacy search-run
```

Run với file log để Loki/Alloy tail:

```bash
cd lab-code
make -f Makefile.legacy search-run-logs
```

Log file:

```text
lab-code/logs/search-service.log
```

Final demo path khuyến nghị là:

```bash
cd lab-code
make up
```

## API

Mọi endpoint validate JWT bằng Spring Security Resource Server và lấy `tenant_id` từ token đã validate.

### Search

```text
GET /api/search/master-data?keyword=...
```

Role được phép:

- `ADMIN`
- `ACCOUNTANT`
- `VIEWER`

Kết quả luôn filter theo tenant hiện tại và `active=true`.

### Reindex tenant hiện tại

```text
POST /api/search/master-data/reindex
```

Chỉ role `ADMIN` được gọi. Endpoint này dành cho IntelliJ `.http` hoặc manual operational test, không expose trên React UI.

Luồng reindex:

1. `search-service` lấy `tenant_id` từ token admin.
2. Gọi `tenant-demo` endpoint `GET /api/master-data` bằng chính token admin để lấy dữ liệu nguồn của tenant hiện tại.
3. Xóa document search cũ của tenant đó trong Elasticsearch.
4. Bulk index lại danh sách master data đang active.
5. Trả summary ngắn:

```json
{
  "tenantId": 1,
  "indexName": "master_data_search",
  "indexedCount": 12,
  "deletedCount": 12,
  "requestedAt": "2026-06-14T00:00:00Z"
}
```

Reindex hiện là tenant-scoped, không phải global all-tenant job.

## HTTP Client

Xem:

```text
lab-code/search-service/http/search-api.http
lab-code/README-http-client.md
```

Các case chính:

- thiếu token -> `401`;
- `tenant1-user` / `tenant2-user` gọi reindex -> `403`;
- `platform-admin` gọi reindex -> `200`;
- search sau reindex trả dữ liệu trong tenant hiện tại.

## Logs

Khi chạy:

```bash
make -f Makefile.legacy search-run-logs
```

Spring Boot ghi thêm file log:

```text
lab-code/logs/search-service.log
```

Grafana Alloy tail file này vào Loki với label:

```text
service="search-service"
source="file"
environment="local"
```

Search request text như `requestId`, code hoặc keyword nên search bằng LogQL `|=`, không thêm thành Loki label.

## Giới hạn hiện tại

- Chưa có outbox, nên DB write và Kafka publish chưa atomic.
- Chưa có retry/DLT cho failed indexing.
- Chưa có schema registry; event DTO đang duplicated có chủ đích để học service boundary.
- Reindex là admin/manual endpoint local, chưa phải production backfill workflow.
