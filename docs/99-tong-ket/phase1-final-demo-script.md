# Phase 1 final demo script

Tài liệu này là route demo thực tế cho Phase 1. Mục tiêu là trình bày flow backend architecture đã học, không overclaim production ERP.

Sau feedback mentor ngày 11/06/2026, script này vẫn là baseline demo đã hoàn thành. Demo nghiêm túc hơn tuần sau sẽ đi theo plan Phase 1.5: `phase1-5-production-like-demo-plan.md`.

UI hiện tại đã chạy được flow chính và đã được polish thành multi-screen ops console theo design-first spec ở `docs/06-frontend/final-web-ui-design-plan.md`. Figma full screen export vẫn là backlog do MCP limit, nhưng React implementation đã theo handoff.

## 1. Chuẩn bị nhanh

Kiểm tra Keycloak local nếu vừa reset volume:

| Thành phần | Giá trị demo |
|---|---|
| Realm | `viettel-lab` |
| API client backend | `tenant-demo-api-client` |
| Web public client | `tenant-demo-web` |
| Web redirect URI | `http://localhost:5173/*` |
| Web origin | `http://localhost:5173` |
| User ACCOUNTANT | `tenant1-user` / `password`, `tenant_id=1`, role `ACCOUNTANT` |
| User VIEWER | `tenant2-user` / `password`, `tenant_id=2`, role `VIEWER` |

`tenant-demo-web` phải là public SPA client: client authentication off, standard flow on. Role nên nằm ở `realm_access.roles` hoặc đúng `resource_access.<KEYCLOAK_CLIENT_ID>.roles` mà backend đang đọc.

## 2. Start demo

Terminal 1 - Docker infra/tooling:

```bash
cd lab-code
make db-up
make keycloak-up
make keycloak-setup
make kafka-up
make kong-up
make kafka-ui-up
make loki-up
make web-ui-up
```

Nếu muốn demo Prometheus/Grafana metrics, bật thêm:

```bash
make observability-up
```

Terminal 2 - `tenant-demo` Java service host-run. File `tenant-demo/.env` nên dùng `APP_AUTH_MODE=keycloak`. Bật thêm feature flags theo phần muốn demo:

```text
APP_CACHE_ENABLED=true
APP_MESSAGING_ENABLED=true
APP_SEARCH_ENABLED=true
APP_FILE_STORAGE_ENABLED=true
```

Chạy bằng target file-log để Loki/Grafana đọc được log:

```bash
cd lab-code
APP_AUTH_MODE=keycloak \
APP_MESSAGING_ENABLED=true \
KAFKA_BOOTSTRAP_SERVERS=localhost:19092 \
make app-run-logs
```

Target này ghi log vào `lab-code/logs/tenant-demo.log` để Alloy tail sang Loki. Nếu không cần Loki cho `tenant-demo`, vẫn có thể dùng `make app-run`.

Terminal 3 - `audit-log-service` Java service host-run:

```bash
cd lab-code
make audit-log-run-logs
```

Target này ghi `lab-code/logs/audit-log-service.log` để Loki/Alloy tail sang Grafana Explore. Dừng bằng `Ctrl+C` trong terminal đó.

Các URL chính:

| Tool | URL |
|---|---|
| React Web UI | `http://localhost:5173` |
| Keycloak Admin Console | `http://localhost:18080` |
| Kong proxy | `http://localhost:18000` |
| Audit API through Kong | `http://localhost:18000/api/audit-events` |
| Kafka UI | `http://localhost:18082` |
| Prometheus | `http://localhost:19090` |
| Grafana | `http://localhost:13000` |
| Grafana Loki logs | `http://localhost:13001` |

Grafana local dùng `admin/admin` chỉ cho lab local.

## 3. Demo sequence chính

### A. ACCOUNTANT flow

1. Mở `http://localhost:5173`.
2. Login bằng `tenant1-user/password`.
3. Kiểm panel auth:
   - `authenticated=true`;
   - access token chỉ hiện `available (hidden)`, không hiện full token;
   - `tenant_id=1`;
   - role có `ACCOUNTANT`.
4. Bấm `Load master data`.
5. Bấm `Load by code` với code có thật, ví dụ `LAPTOP-01`.
6. Bấm `Create` với code `UI-DEMO-*`.
7. Đợi một chút rồi bấm `Load audit events`.
8. Ghi lại `requestId` trên UI và đối chiếu log `tenant-demo` / `audit-log-service`.
9. Nếu Loki đang chạy, mở `http://localhost:13001` -> Explore -> Loki và query:

```logql
{service=~"tenant-demo|audit-log-service|kong-gateway|web-ui-demo"}
{service=~"tenant-demo|audit-log-service|kong-gateway"} |= "requestId="
{service=~"tenant-demo|audit-log-service|kong-gateway"} |= "UI-DEMO"
```

Điểm cần nói: UI gọi Kong Gateway, Gateway forward `Authorization` và `X-Request-Id`; từng backend service vẫn validate JWT, map role, set tenant context và query tenant-aware.

Nếu cần demo riêng cách đọc log, dùng guide:

- `docs/07-architecture/log-aggregation-loki/how-to-read-logs-in-grafana.md`
- `docs/07-architecture/log-aggregation-loki/grafana-loki-ui-screenshot-guide.md`

### B. Redis cache-aside

Nếu `APP_CACHE_ENABLED=true`:

1. Dùng UI `Load by code` hoặc `lab-code/tenant-demo/http/cache-api.http`.
2. Gọi cùng code hai lần.
3. Quan sát log/metric backend:
   - lần đầu miss -> PostgreSQL -> set Redis với TTL;
   - lần sau hit Redis;
   - key có tenant scope nên tenant 1 và tenant 2 không dùng chung cache.

UI không tự kết luận cache hit/miss; UI chỉ gọi endpoint thật và hiển thị request/result.

### C. Kafka async messaging + audit-log-service

Nếu `APP_MESSAGING_ENABLED=true`:

1. Tạo hoặc update `master_data`.
2. Quan sát log:
   - `tenant-demo` producer log `Published Kafka event`;
   - `audit-log-service` log `Consumed cross-service event` và `Stored audit event`.
3. Mở Kafka UI `http://localhost:18082`:
   - topic `master-data-events`;
   - message key dạng `tenant:{tenantId}:master-data:{id}`;
   - consumer group `audit-log-service`.
4. Giải thích event có `tenantId`, Kafka key tenant-aware, PostgreSQL vẫn là source of truth.

### D. Observability

Nếu observability lab đang chạy:

1. Mở Prometheus `http://localhost:19090`.
2. Kiểm target scrape app UP.
3. Query metric ví dụ:
   - `tenant_demo_master_data_cache_requests_total`;
   - `tenant_demo_kafka_publish_requests_total`;
   - `tenant_demo_master_data_get_by_code_duration_seconds_count`.
4. Mở Grafana `http://localhost:13000` để xem datasource/dashboard local.

Điểm cần nói: `/actuator/metrics` để inspect trực tiếp, `/actuator/prometheus` để Prometheus scrape, Grafana đọc Prometheus chứ không đọc app trực tiếp.

### E. VIEWER flow

1. Logout UI.
2. Login bằng `tenant2-user/password`.
3. Kiểm `tenant_id=2` và role `VIEWER`.
4. Bấm `Load master data`: kỳ vọng thành công.
5. Bấm `Create`: kỳ vọng `403 Forbidden`.
6. Bấm `Load audit events`: tenant 2 không thấy audit event tenant 1.

Điểm cần nói: `401` là chưa authenticated/token sai; `403` là token đúng nhưng không đủ quyền. Role không thay tenant-aware query.

## 4. Endpoint/demo coverage

| Capability | Existing endpoint | Demo method | UI supported? | HTTP file supported? | Status |
|---|---|---|---|---|---|
| MasterData list | `GET /api/master-data` | UI qua Gateway | Có | `master-data-api.http`, `keycloak-api.http` | Ready |
| MasterData create | `POST /api/master-data` | UI qua Gateway | Có | `master-data-api.http`, `keycloak-authorization-api.http`, `kafka-api.http` | Ready |
| MasterData get by code / Redis | `GET /api/master-data/code/{code}` | UI hoặc HTTP, gọi hai lần để quan sát cache | Có | `cache-api.http` | Ready nếu `APP_CACHE_ENABLED=true` |
| MasterData update / Kafka | `PUT /api/master-data/{id}` | HTTP/manual | Không | `kafka-api.http` | Ready nếu `APP_MESSAGING_ENABLED=true` |
| Audit events | `GET /api/audit-events` | UI qua Kong | Có | `audit-api.http`, `cross-service-kafka-demo.http` | Ready nếu `audit-log-service` chạy |
| Elasticsearch search | `GET /api/search/master-data?keyword=...` | HTTP/manual | Không | `search-api.http` | Ready nếu `APP_SEARCH_ENABLED=true` |
| Elasticsearch reindex | `POST /api/search/master-data/reindex` | HTTP/manual | Không | `search-api.http` | Ready nếu `APP_SEARCH_ENABLED=true` |
| MinIO upload | `POST /api/files` multipart | HTTP/manual | Không | `file-storage-api.http` | Ready nếu `APP_FILE_STORAGE_ENABLED=true` |
| MinIO download | `GET /api/files/{fileId}` | HTTP/manual | Không | `file-storage-api.http` | Ready nếu `APP_FILE_STORAGE_ENABLED=true` |
| Actuator health | `GET /actuator/health` | Browser/curl | Không | `actuator-api.http`, `observability-api.http` | Ready, public |
| Actuator metrics | `GET /actuator/metrics` | HTTP/manual | Không | `observability-api.http` | Ready, authenticated |
| Prometheus scrape | `GET /actuator/prometheus` | Prometheus/curl | Không | `observability-api.http` | Ready for local lab |
| Kong route | `http://localhost:18000/api/**` | UI + HTTP | Có | `cross-service-kafka-demo.http`, `audit-api.http` | Ready |

Không thêm UI cho Elasticsearch/MinIO trong Phase 1 vì HTTP mini-lab đã đủ và UI không nên phình thành frontend product.

## 5. Layer-by-layer explanation

| Layer | Demo chứng minh gì? |
|---|---|
| React Web UI | Thin client login Keycloak, gọi Gateway bằng Bearer token, sinh `X-Request-Id`. |
| Keycloak | AuthN/token issuer, role/tenant claim cho backend validate. |
| Kong Gateway | Entry point/static route, forward auth header/request id, không chứa business logic. |
| tenant-demo | Resource Server, RBAC, TenantContext, service/repository tenant-aware. |
| audit-log-service | Consume Kafka event, lưu audit table riêng, expose tenant-aware read API. |
| PostgreSQL | Source of truth cho business data/metadata. |
| Redis | Cache-aside read path, key có tenant scope, không thay database. |
| Kafka | Async event propagation sau DB save, không thay database/query API. |
| MinIO | Binary object storage, metadata/tenant ownership nằm ở PostgreSQL. |
| Elasticsearch | Search projection, query có tenant filter. |
| Observability | Health/info/metrics/log/requestId/Prometheus/Grafana local view. |

## 6. Stop demo

Đầu tiên dừng các Java service đang chạy foreground bằng `Ctrl+C`:

- terminal `make app-run` hoặc `make app-run-logs`;
- terminal `make audit-log-run` hoặc `make audit-log-run-logs`;

Sau đó dừng Docker infra/tooling:

```bash
cd lab-code
make web-ui-down
make kong-down
make kafka-ui-down
make loki-down
make kafka-down
make keycloak-down
make db-down
make observability-down
make logs-clean
```

Nếu đang chạy `make gateway-run` foreground, dừng bằng `Ctrl+C` trong terminal tương ứng.

## 7. Caveat cần nói rõ

- Đây là Phase 1 learning lab, không phải production ERP.
- Gateway dùng static route, chưa có service discovery/load balancing thật.
- Keycloak local chưa phải production IAM/RBAC platform.
- Kafka chưa có outbox, idempotent consumer, retry/DLT hoặc schema registry.
- Observability đã có metrics + Loki logs local, nhưng chưa có tracing, Alertmanager, production retention, SLO/SLI hoặc log hardening production.
- MinIO advanced object management như presigned URL expiry, lifecycle, versioning, retention/object lock để backlog optional.
- React Web UI là demo mỏng; không dùng React Native/Expo trong repo này.
