# Kịch bản demo cuối Phase 1 / Phase 1.5

## 1. Mục tiêu demo

Kịch bản này là nguồn chính thức để demo `Master Data Portal` trong 10-15 phút.

Mục tiêu không phải chứng minh đây là ERP production, mà là chứng minh người học đã hiểu và chạy được một local system có các boundary giống production:

- user login qua Keycloak/OIDC;
- request đi qua Kong Gateway;
- backend services validate JWT như Spring Boot Resource Server;
- dữ liệu nghiệp vụ có tenant isolation;
- PostgreSQL là source of truth;
- Redis là cache;
- Kafka là event backbone;
- audit, file và search được tách thành service riêng;
- MinIO, Elasticsearch, Loki/Grafana/Alloy và Kafka UI giúp demo gần thực tế hơn.

## 2. Người xem nên hiểu gì sau demo?

Sau demo, người xem nên nắm được:

- SaaS và multi-tenant khác nhau thế nào.
- Tenant isolation nằm ở backend/data layer, không phải chỉ ở UI.
- Keycloak là Identity Provider/Auth Service, còn backend services là Resource Server.
- Kong là gateway boundary, không thay thế authorization trong backend.
- PostgreSQL là source of truth; Redis/Elasticsearch/audit table là bản sao phục vụ mục đích riêng.
- Kafka giúp tách flow đồng bộ và bất đồng bộ.
- Loki/Grafana/Kafka UI giúp đọc bằng chứng runtime thay vì chỉ nhìn code.
- Repo này là production-like lab, chưa phải production-ready system.

## 3. Chuẩn bị trước demo

Chạy từ thư mục `lab-code/`:

```bash
make info
make up
make status
```

Nếu demo xong:

```bash
make down
make clean-logs
```

Nếu `make up` không phù hợp với máy local, có thể chạy Java services bằng IntelliJ/Maven foreground, nhưng demo vẫn nên giữ cùng model:

```text
Java services chạy Maven/IntelliJ trên host
Docker chạy infra/tooling/web UI
Kong là đường vào API chính
```

## 4. Tài khoản demo

| User | Password | tenant_id | role | Dùng để demo |
|---|---|---:|---|---|
| `tenant1-user` | `password` | `1` | `ACCOUNTANT` | CRUD danh mục, upload/download file, xem activity, search |
| `tenant2-user` | `password` | `2` | `VIEWER` | đọc dữ liệu tenant 2, chứng minh không thấy dữ liệu tenant 1, write bị `403` |
| `platform-admin` | `password` | `1` | `ADMIN` | gọi admin-only reindex bằng IntelliJ `.http`, không dùng trong React UI |

Credential này chỉ dùng cho local demo.

## 5. Cổng và công cụ local

| Công cụ | URL |
|---|---|
| React Web UI | `http://localhost:5173` |
| Keycloak | `http://localhost:18080` |
| Kong Gateway | `http://localhost:18000` |
| Kafka UI | `http://localhost:18082` |
| Grafana Loki | `http://localhost:13001` |
| Loki ready endpoint | `http://localhost:3100/ready` |
| Prometheus metrics lab | `http://localhost:19090` nếu bật |
| Grafana metrics lab | `http://localhost:13000` nếu bật |

Các Java service host-run:

| Service | Port | Log file |
|---|---:|---|
| `tenant-demo` | `8081` hoặc port cấu hình hiện tại của service | `lab-code/logs/tenant-demo.log` |
| `audit-log-service` | `8082` | `lab-code/logs/audit-log-service.log` |
| `file-service` | `8083` | `lab-code/logs/file-service.log` |
| `search-service` | `8084` | `lab-code/logs/search-service.log` |

Khi trình bày, ưu tiên gọi API qua Kong, ví dụ `http://localhost:18000/api/master-data`.

## 6. Luồng demo ngắn 5 phút

Nếu chỉ có 5 phút:

1. Mở `http://localhost:5173`.
2. Login `tenant1-user/password`.
3. Tạo một bản ghi danh mục với code dễ nhận diện, ví dụ `DEMO-<timestamp>`.
4. Sửa bản ghi đó, sau đó tạm ngưng/deactivate.
5. Mở Activity Log để thấy event.
6. Mở Kafka UI, show topic `master-data-events` và consumer groups `audit-log-service`, `search-service`.
7. Mở Grafana Loki, query theo code hoặc `requestId`.
8. Login `tenant2-user/password`, chứng minh không thấy dữ liệu tenant 1 và write bị `403`.

Kết luận ngắn:

```text
UI không tự quyết định tenant.
Keycloak phát token.
Kong route request.
Backend validate JWT và enforce tenant/role.
PostgreSQL là source of truth.
Kafka đẩy event sang audit/search.
Loki/Kafka UI là bằng chứng runtime.
```

## 7. Luồng demo đầy đủ 10-15 phút

Thứ tự khuyến nghị:

1. Mở kiến trúc tổng quan.
2. Login và giải thích token.
3. Demo `401` và `403`.
4. CRUD master data tenant-aware.
5. Redis cache-aside nếu đã bật cache.
6. Kafka event flow.
7. Audit log.
8. File upload/download với MinIO.
9. Search service với Elasticsearch.
10. Admin-only reindex bằng `.http`.
11. Observability bằng `make status`, log files, Loki/Grafana, Kafka UI.
12. Kết thúc bằng production caveats.

## 8. Kịch bản từng bước

### Scene A - Open architecture

Action:

- Mở README hoặc nói bằng sơ đồ ngắn:

```text
React Web UI
-> Keycloak
-> Kong Gateway
  -> tenant-demo
  -> audit-log-service
  -> file-service
  -> search-service
-> PostgreSQL / Redis / Kafka / MinIO / Elasticsearch / Loki
```

Điểm cần nói:

- Đây là production-like learning lab, không phải ERP production.
- Demo tập trung vào boundary: auth, gateway, tenant-aware data, async event, object storage, search projection và observability.

Evidence:

```bash
cd lab-code
make status
```

### Scene B - Login and token

Action:

1. Mở `http://localhost:5173`.
2. Bấm đăng nhập.
3. Keycloak themed login page xuất hiện.
4. Login `tenant1-user/password`.
5. Vào màn Account hoặc topbar để thấy tenant/role.

Điểm cần nói:

- Keycloak là Identity Provider.
- React UI không hiển thị full token.
- Backend services validate JWT bằng Spring Security Resource Server.
- `tenant_id` dùng để set tenant context trong backend, không tin tenant do frontend tự gửi.

Evidence:

- UI hiện user/role/tenant ở mức đủ demo.
- Token status chỉ là `available (hidden)`.

### Scene C - 401 vs 403

Action:

- Dùng `.http` hoặc curl gọi API không token:

```text
GET http://localhost:18000/api/master-data
```

- Login `tenant2-user/password`, thử tạo master data.

Expected:

- No token -> `401`.
- VIEWER write -> `403`.

Điểm cần nói:

- `401` là chưa authenticated hoặc token không hợp lệ.
- `403` là authenticated rồi nhưng không đủ quyền.
- Tenant isolation và role authorization là hai lớp khác nhau.

### Scene D - Master Data tenant-aware CRUD

Action với tenant 1:

1. Load danh sách danh mục.
2. Tạo record `DEMO-<timestamp>`.
3. Sửa tên hoặc loại.
4. Thử tạo/sửa trùng code để thấy `409`.
5. Tạm ngưng/deactivate record.

Action với tenant 2:

1. Logout tenant 1.
2. Login tenant 2.
3. Load danh sách.
4. Tìm code tenant 1 vừa tạo.
5. Thử write action.

Expected:

- Tenant 1 thao tác được theo role `ACCOUNTANT`.
- Tenant 2 không thấy dữ liệu tenant 1.
- Tenant 2 write bị `403`.

Điểm cần nói:

- PostgreSQL là source of truth.
- Shared-table model có `tenant_id`.
- Repository/service query phải tenant-aware.
- UI không truyền tenant đáng tin cậy.

### Scene E - Redis cache-aside

Action nếu cache bật:

1. Gọi get-by-code cùng một code hai lần.
2. Xem log hoặc metrics.

Expected:

- Lần đầu cache miss, fallback PostgreSQL.
- Lần sau cache hit.

Điểm cần nói:

- Redis tăng tốc read path, không sở hữu dữ liệu.
- Key phải tenant-scoped để tránh leak dữ liệu.
- TTL/invalidation là trade-off.

Evidence:

- `tenant-demo.log`.
- Optional metrics nếu Prometheus/Grafana metrics lab đang chạy.

### Scene F - Kafka event flow

Action:

1. Tạo/update/deactivate master data.
2. Mở Kafka UI `http://localhost:18082`.
3. Xem topic `master-data-events`.
4. Xem consumer groups:
   - `audit-log-service`;
   - `search-service`.
5. Kiểm lag.

Điểm cần nói:

- `tenant-demo` là producer.
- Audit/search là consumers.
- Topic chứa event, consumer group quản lý offset.
- Lag `0` trong demo nghĩa là consumer đã bắt kịp.
- PostgreSQL vẫn là source of truth.

Production gap:

- Chưa có outbox.
- Chưa có retry/DLT hoàn chỉnh.
- Chưa có schema registry.

### Scene G - Audit log

Action:

1. Mở Activity Log trong UI.
2. Load activity sau khi tạo/sửa/tạm ngưng master data.
3. Login tenant 2 và load Activity Log.

Expected:

- Tenant 1 thấy activity của tenant 1.
- Tenant 2 không thấy activity tenant 1.

Điểm cần nói:

- `audit-log-service` sở hữu audit read model.
- Service consume Kafka event và lưu bảng riêng.
- Đây là service boundary, không chỉ thêm controller trong `tenant-demo`.

### Scene H - File service + MinIO

Action:

1. Mở màn Tệp tin.
2. Upload file nhỏ bằng tenant 1.
3. Load danh sách file.
4. Download file.
5. Login tenant 2 và thử xem/download file tenant 1.

Expected:

- Tenant 1 upload/download được.
- Tenant 2 không thấy hoặc không tải được file tenant 1.
- VIEWER write/delete bị từ chối nếu role không cho phép.

Điểm cần nói:

- MinIO giữ binary object.
- PostgreSQL giữ metadata và tenant ownership.
- UI không gọi MinIO trực tiếp.
- Backend kiểm tra tenant/role trước khi stream object.

### Scene I - Search service + Elasticsearch

Action:

1. Tạo hoặc sửa record với keyword dễ nhận diện.
2. Đợi vài giây.
3. Tìm kiếm trong UI.
4. Login tenant 2 và tìm cùng keyword.

Expected:

- Tenant 1 thấy record.
- Tenant 2 không thấy record tenant 1.
- Có thể cần retry nhẹ vì eventual consistency.

Điểm cần nói:

- Elasticsearch là projection, không phải source of truth.
- `search-service` consume Kafka event để update index.
- Search query phải filter theo tenant.

### Scene J - Admin-only reindex

Action bằng IntelliJ `.http`, không dùng UI:

1. No token gọi:

```text
POST /api/search/master-data/reindex
```

2. Token tenant1/accountant hoặc tenant2/viewer gọi.
3. Token `platform-admin` gọi.
4. Search lại keyword.

Expected:

- No token -> `401`.
- Non-admin -> `403`.
- Admin -> `200` với summary.
- Search sau reindex có dữ liệu tenant hiện tại.

Điểm cần nói:

- Reindex là operational/admin function.
- Tenant-scoped trong phase này.
- Rebuild projection từ `tenant-demo`/PostgreSQL source data, không replay Kafka history.
- Không expose trên React UI.

### Scene K - Observability

Action:

1. Chạy:

```bash
cd lab-code
make status
```

2. Xem log files:

```text
lab-code/logs/tenant-demo.log
lab-code/logs/audit-log-service.log
lab-code/logs/file-service.log
lab-code/logs/search-service.log
```

3. Mở Grafana Loki `http://localhost:13001`.
4. Query:

```logql
{service=~"tenant-demo|audit-log-service|file-service|search-service|kong-gateway"}
{service=~"tenant-demo|audit-log-service|file-service|search-service|kong-gateway"} |= "requestId="
{service=~"tenant-demo|audit-log-service|file-service|search-service|kong-gateway"} |= "DEMO-"
```

Điểm cần nói:

- `requestId` giúp trace một UI action qua nhiều service.
- `durationMs` là quan sát local, không phải benchmark production.
- Health/metrics/logs là observability signals khác nhau.
- Browser console error không tự động nằm trong Loki.

### Scene L - Close with caveats

Điểm cần nói:

- Đây là production-like demo, không production-ready.
- Còn thiếu:
  - outbox;
  - retry/DLT;
  - schema registry;
  - Kubernetes/service discovery;
  - production HA/secrets/TLS;
  - distributed tracing/SLO/alerting;
  - production file lifecycle/versioning;
  - load testing có p50/p95/p99.

## 9. Điểm cần nói ở mỗi bước

| Bước | Một câu nói chính |
|---|---|
| Architecture | "Em muốn chứng minh boundary, không phải làm ERP production." |
| Login | "Keycloak phát token, backend validate token." |
| 401/403 | "401 là chưa đăng nhập, 403 là đăng nhập rồi nhưng không đủ quyền." |
| CRUD | "PostgreSQL là source of truth, query luôn gắn tenant." |
| Redis | "Cache tăng tốc đọc, không thay DB." |
| Kafka | "Kafka tách write path khỏi audit/search projection." |
| Audit | "Audit là service sở hữu read model riêng." |
| File | "MinIO lưu binary, PostgreSQL lưu ownership." |
| Search | "Elasticsearch là projection eventually consistent." |
| Reindex | "Admin endpoint phục hồi projection, không phải user feature." |
| Observability | "Log, metric, lag giúp đọc hệ thống đang chạy." |
| Caveat | "Production-like nghĩa là đúng hướng kiến trúc, chưa phải production-ready." |

## 10. Evidence cần show

| Khái niệm | Runtime component | Action chứng minh | Evidence |
|---|---|---|---|
| SaaS/multi-tenant | UI + backend | tenant1/tenant2 login | tenant/role khác nhau |
| Tenant isolation | PostgreSQL/repository | tenant2 không thấy data tenant1 | UI/API result |
| Source of truth | PostgreSQL | CRUD master data | DB-backed list/lookup |
| Flyway migration | backend startup | service boot thành công | migration table/log |
| OIDC/JWT | Keycloak + Resource Server | login, call API | token hidden, backend auth |
| RBAC | Spring Security | viewer write | `403` |
| Gateway boundary | Kong | UI gọi `localhost:18000` | Kong route works |
| Redis cache | Redis + tenant-demo | get-by-code 2 lần | cache hit/miss log/metric |
| Kafka | Kafka UI | create/update/deactivate | topic, key, message |
| Consumer group/lag | Kafka UI | xem groups | lag 0 hoặc giải thích lag |
| Audit read model | audit-log-service | load Activity Log | audit events |
| Search projection | search-service | search keyword | result appears after delay |
| Reindex | search-service | admin `.http` | `200` summary |
| File boundary | file-service + MinIO | upload/download | metadata + stream |
| Log aggregation | Loki/Grafana/Alloy | query service/requestId | log lines |
| Actuator | backend services | health endpoint | health response |

## 11. Câu lệnh / HTTP request / UI action

Startup:

```bash
cd lab-code
make up
make status
```

No-token check:

```bash
curl -i http://localhost:18000/api/master-data
curl -i http://localhost:18000/api/audit-events
curl -i http://localhost:18000/api/files
curl -i "http://localhost:18000/api/search/master-data?keyword=test"
```

HTTP Client files:

```text
lab-code/keycloak-lab/http/keycloak-token-flow.http
lab-code/tenant-demo/http/master-data-api.http
lab-code/file-service/http/file-api.http
lab-code/search-service/http/search-api.http
lab-code/audit-log-service/http/audit-api.http
```

Health checks:

```bash
curl -i http://localhost:18000/tenant-demo/actuator/health
curl -f http://localhost:3100/ready
```

UI actions:

```text
Login -> Danh mục -> tạo/sửa/tạm ngưng -> Tệp tin -> Lịch sử hoạt động -> Tìm kiếm -> Tài khoản
```

## 12. Log/Grafana/Kafka UI evidence

Kafka UI:

```text
Cluster: viettel-local
Topic: master-data-events
Consumer groups:
  audit-log-service
  search-service
Lag: 0 hoặc giải thích nếu vừa có event mới
```

Grafana Loki:

```logql
{service="tenant-demo"}
{service="audit-log-service"}
{service="file-service"}
{service="search-service"}
{service="kong-gateway"}
{service=~"tenant-demo|audit-log-service|file-service|search-service|kong-gateway"} |= "requestId="
{service=~"tenant-demo|audit-log-service|file-service|search-service|kong-gateway"} |= "DEMO-"
{service=~"tenant-demo|kong-gateway"} |= "403"
```

Local log files:

```bash
tail -n 80 logs/tenant-demo.log
tail -n 80 logs/audit-log-service.log
tail -n 80 logs/file-service.log
tail -n 80 logs/search-service.log
```

## 13. Điều không nên nói quá

Không nói:

- "Hệ thống này production-ready."
- "Latency local này chứng minh performance production."
- "Kafka đảm bảo không mất event trong mọi tình huống."
- "Elasticsearch là database chính."
- "Redis là source of truth."
- "Kong đã làm toàn bộ security."
- "File upload đã sẵn sàng enterprise lifecycle."

Nên nói:

- "Đây là production-like lab để chứng minh boundary và flow."
- "Các con số local chỉ dùng để debug và quan sát."
- "Phần production hardening còn nằm ở caveat."

## 14. Production caveats

- Chưa có outbox để đảm bảo DB write và Kafka publish atomic.
- Chưa có retry/DLT và backoff strategy hoàn chỉnh.
- Chưa có schema registry hoặc versioning event contract.
- Chưa có Kubernetes/service discovery/load balancing thật.
- Chưa có production TLS, secrets management, HA.
- Chưa có distributed tracing, alerting, SLO/SLI.
- Chưa có load test với p50/p95/p99, throughput và error rate.
- File lifecycle, versioning, retention, antivirus scan và quota chưa làm production-grade.
- Elasticsearch reindex hiện tenant-scoped/manual, chưa phải backfill platform.

## 15. Checklist trước khi demo

- [ ] `git status --short` sạch hoặc chỉ có thay đổi có chủ đích.
- [ ] `cd lab-code && make info` đọc lại URLs.
- [ ] `cd lab-code && make up`.
- [ ] `cd lab-code && make status`.
- [ ] Web UI mở được `http://localhost:5173`.
- [ ] Keycloak login page có brand `Master Data Portal`.
- [ ] Token helper `.http` lấy được tenant1, tenant2 và admin token nếu cần.
- [ ] Kafka UI mở được.
- [ ] Grafana Loki mở được.
- [ ] Log files không rỗng sau vài request.
- [ ] Chuẩn bị code demo có prefix dễ tìm, ví dụ `DEMO-<timestamp>`.

## 16. Checklist sau khi demo

- [ ] Dừng stack:

```bash
cd lab-code
make down
```

- [ ] Nếu không cần giữ log:

```bash
make clean-logs
```

- [ ] Không commit:
  - `logs/*.log`;
  - `.pids/`;
  - token;
  - local IntelliJ HTTP env;
  - Docker/MinIO/Elasticsearch data.

- [ ] Ghi lại câu hỏi mentor và phần nào trả lời chưa chắc.
