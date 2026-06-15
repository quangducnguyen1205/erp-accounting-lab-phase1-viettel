# Demo rehearsal transcript

## Setup

- Presenter: Quang Đức
- Audience: mentor / reviewer
- Duration: 10-15 phút
- Goal: trình bày hệ thống `Master Data Portal` như một production-like local lab, nối từ theory route đến runtime evidence.

## Transcript

### 0:00-1:00 - Opening

Presenter says:

> Em sẽ demo `Master Data Portal`.
> Đây không phải ERP production, mà là lab production-like để chứng minh các boundary backend:
> Keycloak/OIDC, Kong Gateway, multi-tenant API, PostgreSQL source of truth,
> Redis cache, Kafka event flow, MinIO file storage,
> Elasticsearch search projection và Loki/Grafana observability.

Action:

- Mở README hoặc nói sơ đồ:

```text
React Web UI -> Keycloak -> Kong -> tenant-demo/audit/file/search services -> infra
```

Expected result:

- Người xem hiểu scope: demo kiến trúc và learning outcome, không phải sản phẩm ERP hoàn chỉnh.

Audience may ask:

> Local demo này chứng minh được production chưa?

Presenter answers:

> Chưa. Nó chứng minh đúng hướng kiến trúc và runtime flow. Production cần thêm HA, TLS/secrets, CI/CD, load test, alerting/SLO, tracing, outbox, retry/DLT và schema registry.

### 1:00-2:30 - Startup and status

Presenter says:

> Workflow final được gom về Makefile ngắn để không bị lạc giữa các mini-lab cũ.

Action:

```bash
cd lab-code
make status
```

Expected screen/result:

- Docker services hiện ra.
- Java PIDs cho `tenant-demo`, `audit-log-service`, `file-service`, `search-service`.
- Log files trong `lab-code/logs/`.

If service is not up:

Presenter says:

> Nếu service chưa lên, em sẽ không đoán. Em xem `logs/*-runner.log` và `logs/*.log` để biết lỗi config, port hay dependency.

Audience may ask:

> Tại sao Java service không chạy Docker hết?

Presenter answers:

> Vì repo này là local learning lab. Java services chạy Maven/IntelliJ giúp debug dễ hơn. Docker giữ vai trò infra/tooling: PostgreSQL, Keycloak, Kafka, MinIO, Elasticsearch, Kong, Loki/Grafana và Web UI.

### 2:30-4:00 - Login/Auth

Presenter says:

> Em bắt đầu từ login. Keycloak là Identity Provider. UI không tự quyết định user hay tenant.

Action:

- Mở `http://localhost:5173`.
- Bấm đăng nhập.
- Keycloak themed login xuất hiện.
- Login `tenant1-user/password`.

Expected screen/result:

- UI vào `Master Data Portal`.
- Có tenant/role/account info ở mức business-friendly.
- Token không hiển thị full value.

Audience may ask:

> Tại sao không cho frontend gọi thẳng service?

Presenter answers:

> Frontend đi qua Kong để có một entry point thống nhất. Nhưng backend service vẫn validate JWT. Gateway route request, không thay business authorization.

Audience may ask:

> Kong có validate token không?

Presenter answers:

> Trong lab này Kong chủ yếu route và forward header. Mỗi Spring Boot service là Resource Server và tự validate token. Production có thể thêm plugin ở Kong, nhưng backend vẫn nên enforce authorization.

### 4:00-5:30 - 401 vs 403

Presenter says:

> Em show nhanh khác nhau giữa unauthenticated và unauthorized.

Action:

- Dùng `.http` hoặc curl gọi API không token.
- Login tenant2 `VIEWER`, thử tạo master data.

Expected result:

- No token -> `401`.
- Viewer write -> `403`.

Audience may ask:

> 401 và 403 khác nhau thế nào?

Presenter answers:

> 401 là chưa authenticated hoặc token sai. 403 là token hợp lệ nhưng role không được phép action đó. Tenant isolation lại là lớp khác: user tenant 2 dù có quyền đọc cũng không thấy dữ liệu tenant 1.

### 5:30-7:00 - Master Data CRUD and tenant isolation

Presenter says:

> Đây là flow business chính. PostgreSQL là source of truth và mọi query phải tenant-aware.

Action:

- Vào màn Danh mục.
- Load list.
- Tạo record `DEMO-<timestamp>`.
- Update record.
- Thử duplicate code để thấy `409`.
- Deactivate record.

Expected result:

- Tenant 1 thao tác thành công.
- Duplicate code có thông báo thân thiện.
- Deactivate xong record không còn trong active lookup/list.

Audience may ask:

> Nếu user sửa `tenant_id` trong request thì sao?

Presenter answers:

> Backend không tin `tenant_id` từ request body/param. Tenant context lấy từ JWT đã validate. Repository query filter theo tenant context.

If data is already present:

Presenter says:

> Nếu dữ liệu demo đã có, em dùng code mới có timestamp để không phụ thuộc vào state cũ. Nếu gặp duplicate, đó cũng là case `409` để demo.

### 7:00-8:00 - Redis cache-aside

Presenter says:

> Redis chỉ là cache-aside cho read path, không phải source of truth.

Action:

- Gọi load-by-code cùng một code hai lần.
- Mở log/metric nếu cache enabled.

Expected result:

- Có dấu hiệu miss rồi hit trong log/metric.

Audience may ask:

> Cache có gây leak tenant không?

Presenter answers:

> Cache key phải chứa tenant scope. Nếu key chỉ theo code mà không theo tenant thì có nguy cơ tenant 2 đọc nhầm dữ liệu tenant 1.

### 8:00-9:30 - Kafka flow and Activity Log

Presenter says:

> Sau khi master data thay đổi, `tenant-demo` publish event. Audit và search consume event bằng consumer group riêng.

Action:

- Mở Kafka UI.
- Topic `master-data-events`.
- Consumer groups `audit-log-service`, `search-service`.
- Mở Activity Log trong UI.

Expected result:

- Event xuất hiện.
- Lag về 0 hoặc có thể giải thích nếu vừa publish.
- Activity Log có event tenant 1.

Audience may ask:

> Nếu Kafka publish fail thì sao?

Presenter answers:

> Hiện tại đây là production gap. Chưa có outbox nên DB write và Kafka publish chưa atomic. Production cần outbox, retry/DLT và monitoring lag/error.

If Kafka lag is briefly non-zero:

Presenter says:

> Lag non-zero ngay sau khi tạo event có thể bình thường. Điều em kiểm là consumer có lỗi không và lag có quay về 0 không.

### 9:30-10:45 - File service + MinIO

Presenter says:

> File được tách thành `file-service`. UI không gọi MinIO trực tiếp.

Action:

- Mở màn Tệp tin.
- Upload file nhỏ.
- List metadata.
- Download file.
- Login tenant2, thử xem/download file tenant1.

Expected result:

- Tenant 1 upload/download được.
- Tenant 2 không thấy hoặc không tải được file tenant 1.

Audience may ask:

> Nếu nhiều user cùng upload file thì bottleneck ở đâu?

Presenter answers:

> Có thể ở file size/network, backend streaming, MinIO throughput, metadata DB query, JVM memory hoặc Kong timeout. Production cần size limit, streaming config, quota, lifecycle, versioning và scan nếu domain yêu cầu.

### 10:45-12:00 - Search service + Elasticsearch

Presenter says:

> Search dùng Elasticsearch như projection, không phải source of truth.

Action:

- Search keyword vừa tạo.
- Nếu chưa thấy, retry sau vài giây.
- Login tenant2 và search cùng keyword.

Expected result:

- Tenant 1 thấy result.
- Tenant 2 không thấy result tenant 1.

If search projection takes a few seconds:

Presenter says:

> Đây là eventual consistency. PostgreSQL write đã xong trước, sau đó Kafka event mới cập nhật Elasticsearch projection.

Audience may ask:

> Search có phải source of truth không?

Presenter answers:

> Không. Source of truth là PostgreSQL. Elasticsearch chỉ là projection để query nhanh và có thể rebuild bằng reindex.

### 12:00-13:00 - Admin-only reindex

Presenter says:

> Reindex là operation cho admin/manual test, không phải user feature nên không có React UI.

Action:

- Mở `lab-code/search-service/http/search-api.http`.
- No-token reindex -> `401`.
- Tenant user token -> `403`.
- `platform-admin` token -> `200`.

Expected result:

- Admin response có `tenantId`, `indexName`, `indexedCount`, `requestedAt`.

Audience may ask:

> Tại sao reindex tenant-scoped?

Presenter answers:

> Phase này chỉ cần rebuild projection cho tenant hiện tại, lấy tenant từ admin token. Global/all-tenant backfill là production operation phức tạp hơn.

### 13:00-14:30 - Observability

Presenter says:

> Cuối cùng em show cách đọc hệ thống đang chạy, không chỉ đọc code.

Action:

- Mở Grafana Loki.
- Query:

```logql
{service=~"tenant-demo|audit-log-service|file-service|search-service|kong-gateway"} |= "requestId="
```

- Query theo code `DEMO-`.
- Mở Kafka UI lag nếu cần.

Expected result:

- Có log theo service.
- Có thể trace request/event bằng `requestId` hoặc business code.

Audience may ask:

> Duration 200ms/500ms/1s trong log có đáng lo không?

Presenter answers:

> Đây là local observation, không phải benchmark.
> Local Docker + Maven + laptop resource có thể dao động.
> Em dùng duration để debug trend và phân biệt timeout/error/cold start/cache hit.
> Muốn claim performance phải load test với p50/p95/p99, throughput và error rate.

### 14:30-15:00 - Closing

Presenter says:

> Demo này chứng minh em hiểu cách ghép các pattern backend: auth boundary, gateway boundary, tenant-aware data, async events, object storage, search projection và observability. Nhưng em không claim production-ready.

Audience may ask:

> Phần nào còn thiếu để production hóa?

Presenter answers:

> Outbox, retry/DLT, schema registry, Kubernetes/service discovery, TLS/secrets/HA, tracing/SLO/alerting, load test, backup/restore và production file lifecycle.
