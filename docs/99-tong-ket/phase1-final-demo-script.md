# Phase 1 final demo script

Tài liệu này là route demo thực tế cho Phase 1. Mục tiêu là trình bày flow backend architecture đã học, không overclaim production ERP.

Sau feedback mentor ngày 11/06/2026, script này vẫn là baseline demo đã hoàn thành. Demo nghiêm túc hơn tuần sau sẽ đi theo plan Phase 1.5: `phase1-5-production-like-demo-plan.md`.

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

Từ repo root:

```bash
cd lab-code
make infra-up
```

`infra-up` bật PostgreSQL + Keycloak + Elasticsearch + MinIO + Redis + Kafka. Prometheus/Grafana chạy riêng để không làm full infra mặc định quá nặng:

```bash
make observability-up
```

Start backend ở Keycloak mode. File `tenant-demo/.env` nên dùng `APP_AUTH_MODE=keycloak`. Bật thêm feature flags theo phần muốn demo:

```text
APP_CACHE_ENABLED=true
APP_MESSAGING_ENABLED=true
APP_SEARCH_ENABLED=true
APP_FILE_STORAGE_ENABLED=true
```

Sau đó chạy app:

```bash
make app-run
```

Mở terminal khác cho Gateway:

```bash
cd lab-code
make gateway-run
```

Start React Web UI Docker-first:

```bash
cd lab-code
make web-ui-up
```

Các URL chính:

| Tool | URL |
|---|---|
| React Web UI | `http://localhost:5173` |
| Keycloak Admin Console | `http://localhost:18080` |
| Gateway health | `http://localhost:8081/actuator/health` |
| Prometheus | `http://localhost:19090` |
| Grafana | `http://localhost:13000` |

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
7. Ghi lại `requestId` trên UI và đối chiếu log `tenant-demo`.

Điểm cần nói: UI gọi Gateway, Gateway forward `Authorization` và `X-Request-Id`, backend vẫn validate JWT, map role, set tenant context và query tenant-aware.

### B. Redis cache-aside

Nếu `APP_CACHE_ENABLED=true`:

1. Dùng UI `Load by code` hoặc `lab-code/tenant-demo/http/cache-api.http`.
2. Gọi cùng code hai lần.
3. Quan sát log/metric backend:
   - lần đầu miss -> PostgreSQL -> set Redis với TTL;
   - lần sau hit Redis;
   - key có tenant scope nên tenant 1 và tenant 2 không dùng chung cache.

UI không tự kết luận cache hit/miss; UI chỉ gọi endpoint thật và hiển thị request/result.

### C. Kafka async messaging

Nếu `APP_MESSAGING_ENABLED=true`:

1. Tạo hoặc update `master_data`.
2. Quan sát log backend:
   - producer log `Published Kafka event`;
   - consumer log `Consumed Kafka event`.
3. Giải thích event có `tenantId`, Kafka key tenant-aware, PostgreSQL vẫn là source of truth.

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

Điểm cần nói: `401` là chưa authenticated/token sai; `403` là token đúng nhưng không đủ quyền. Role không thay tenant-aware query.

## 4. Endpoint/demo coverage

| Capability | Existing endpoint | Demo method | UI supported? | HTTP file supported? | Status |
|---|---|---|---|---|---|
| MasterData list | `GET /api/master-data` | UI qua Gateway | Có | `master-data-api.http`, `keycloak-api.http` | Ready |
| MasterData create | `POST /api/master-data` | UI qua Gateway | Có | `master-data-api.http`, `keycloak-authorization-api.http`, `kafka-api.http` | Ready |
| MasterData get by code / Redis | `GET /api/master-data/code/{code}` | UI hoặc HTTP, gọi hai lần để quan sát cache | Có | `cache-api.http` | Ready nếu `APP_CACHE_ENABLED=true` |
| MasterData update / Kafka | `PUT /api/master-data/{id}` | HTTP/manual | Không | `kafka-api.http` | Ready nếu `APP_MESSAGING_ENABLED=true` |
| Elasticsearch search | `GET /api/search/master-data?keyword=...` | HTTP/manual | Không | `search-api.http` | Ready nếu `APP_SEARCH_ENABLED=true` |
| Elasticsearch reindex | `POST /api/search/master-data/reindex` | HTTP/manual | Không | `search-api.http` | Ready nếu `APP_SEARCH_ENABLED=true` |
| MinIO upload | `POST /api/files` multipart | HTTP/manual | Không | `file-storage-api.http` | Ready nếu `APP_FILE_STORAGE_ENABLED=true` |
| MinIO download | `GET /api/files/{fileId}` | HTTP/manual | Không | `file-storage-api.http` | Ready nếu `APP_FILE_STORAGE_ENABLED=true` |
| Actuator health | `GET /actuator/health` | Browser/curl | Không | `actuator-api.http`, `observability-api.http` | Ready, public |
| Actuator metrics | `GET /actuator/metrics` | HTTP/manual | Không | `observability-api.http` | Ready, authenticated |
| Prometheus scrape | `GET /actuator/prometheus` | Prometheus/curl | Không | `observability-api.http` | Ready for local lab |
| Gateway route | `http://localhost:8081/api/**` | UI + HTTP | Có | `gateway-api.http` nếu cần | Ready |

Không thêm UI cho Elasticsearch/MinIO trong Phase 1 vì HTTP mini-lab đã đủ và UI không nên phình thành frontend product.

## 5. Layer-by-layer explanation

| Layer | Demo chứng minh gì? |
|---|---|
| React Web UI | Thin client login Keycloak, gọi Gateway bằng Bearer token, sinh `X-Request-Id`. |
| Keycloak | AuthN/token issuer, role/tenant claim cho backend validate. |
| Gateway | Entry point/static route, forward auth header/request id, không chứa business logic. |
| tenant-demo | Resource Server, RBAC, TenantContext, service/repository tenant-aware. |
| PostgreSQL | Source of truth cho business data/metadata. |
| Redis | Cache-aside read path, key có tenant scope, không thay database. |
| Kafka | Async event propagation sau DB save, không thay database/query API. |
| MinIO | Binary object storage, metadata/tenant ownership nằm ở PostgreSQL. |
| Elasticsearch | Search projection, query có tenant filter. |
| Observability | Health/info/metrics/log/requestId/Prometheus/Grafana local view. |

## 6. Stop demo

```bash
cd lab-code
make web-ui-down
make observability-down
make infra-down
```

Nếu đang chạy `make app-run` hoặc `make gateway-run` foreground, dừng bằng `Ctrl+C` trong terminal tương ứng.

## 7. Caveat cần nói rõ

- Đây là Phase 1 learning lab, không phải production ERP.
- Gateway dùng static route, chưa có service discovery/load balancing thật.
- Keycloak local chưa phải production IAM/RBAC platform.
- Kafka chưa có outbox, idempotent consumer, retry/DLT hoặc schema registry.
- Observability chưa có tracing, Loki/ELK log aggregation, Alertmanager, SLO/SLI.
- MinIO advanced object management như presigned URL expiry, lifecycle, versioning, retention/object lock để backlog optional.
- React Web UI là demo mỏng; không dùng React Native/Expo trong repo này.
