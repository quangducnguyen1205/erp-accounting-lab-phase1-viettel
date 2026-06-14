# How to read logs in Grafana Loki

Mục tiêu của guide này không phải học thêm tool mới, mà là biết cách một backend engineer đọc log khi một request đi qua nhiều lớp: Gateway -> business service -> Kafka -> consumer service.

## Mở Grafana Explore

1. Mở `http://localhost:13001`.
2. Login local lab: `admin / admin`.
3. Vào `Explore`.
4. Chọn datasource `Loki`.
5. Chọn time range ngắn, ví dụ `Last 15 minutes`, để log dễ đọc.

Nếu muốn xem từng bước bằng ảnh UI thật, đọc thêm:

- [grafana-loki-ui-screenshot-guide.md](grafana-loki-ui-screenshot-guide.md)

## Using Grafana UI without memorizing LogQL

Bạn không cần nhớ toàn bộ LogQL ngay từ đầu. Cách làm thực tế:

1. Mở `Explore`.
2. Chọn datasource `Loki`.
3. Ở Builder mode, mở `Label filters`.
4. Chọn label `service`.
5. Chọn value như `tenant-demo`, `audit-log-service`, `file-service`, `search-service`, `kong-gateway`, hoặc `web-ui-demo`.
6. Bấm `Run query`.
7. Khi đã đúng service/layer, chuyển sang Code mode hoặc dùng line filter để thêm:

```logql
|= "LOKI-UI-DEMO"
```

hoặc:

```logql
|= "requestId="
```

Tư duy là: chọn label trước để thu hẹp phạm vi, rồi mới search text trong log message.

## Query theo service

Query tổng hợp các service chính:

```logql
{service=~"tenant-demo|audit-log-service|file-service|search-service|kong-gateway|web-ui-demo"}
```

Query này hữu ích để biết hệ thống còn đang phát log không, nhưng thường hơi nhiễu để debug sâu.

Gateway layer:

```logql
{service="kong-gateway"}
```

Dùng query này khi frontend báo API lỗi. Gateway log cho biết request có tới gateway không, method/path/status là gì, và Kong trả gì cho client.

Business service:

```logql
{service="tenant-demo"}
```

Trong repo này, `tenant-demo` thường chạy bằng Maven/IntelliJ trên host. Khi chạy `make app-run-logs`, app ghi vào `lab-code/logs/tenant-demo.log`; Alloy tail file đó vào Loki với `source="file"`. Đây là nơi xem auth/RBAC, tenant context, DB operation, Redis cache, Kafka publish.

Async consumer service:

```logql
{service="audit-log-service"}
```

Dùng query này để xem event Kafka đã được consume/store chưa, hoặc audit API có trả theo tenant đúng không.

`audit-log-service` cũng là Java service host-run. Khi chạy `make audit-log-run-logs`, service ghi vào `lab-code/logs/audit-log-service.log`; Alloy tail file đó vào Loki với `source="file"`.

File service:

```logql
{service="file-service"}
```

Dùng query này để debug upload/download/list/delete file tenant-aware. `file-service` cũng chạy Maven/IntelliJ trên host; khi chạy `make file-run-logs`, service ghi vào `lab-code/logs/file-service.log` và Alloy tail vào Loki với `source="file"`.

Search service:

```logql
{service="search-service"}
```

Dùng query này để debug Kafka consume và Elasticsearch indexing. `search-service` chạy Maven/IntelliJ trên host; khi chạy `make search-run-logs`, service ghi vào `lab-code/logs/search-service.log` và Alloy tail vào Loki với `source="file"`.

Frontend container:

```logql
{service="web-ui-demo"}
```

Đây chủ yếu là log Vite/container. Browser JavaScript console vẫn nằm ở trình duyệt, không tự đi vào Loki.

## Trace theo business code hoặc event

Với demo, cách dễ nhất là tạo code như `LOKI-WATCH-*` hoặc `LOKI-DEMO-*`, rồi search text:

```logql
{service=~"tenant-demo|audit-log-service|file-service|search-service|kong-gateway"} |= "LOKI-WATCH"
```

Nếu dùng code cụ thể:

```logql
{service=~"tenant-demo|audit-log-service|search-service|kong-gateway"} |= "LOKI-DEMO-1781306213"
```

Cách này thường dễ hơn requestId trong flow async, vì event Kafka không tự động mang MDC/requestId qua process khác nếu mình chưa thiết kế propagation.

## Trace theo requestId

Request logging filter ghi `requestId` trong log message:

```logql
{service=~"tenant-demo|audit-log-service|search-service|kong-gateway"} |= "requestId="
```

Nếu UI hiển thị một request id cụ thể, search:

```logql
{service=~"tenant-demo|audit-log-service|search-service|kong-gateway"} |= "web-demo-..."
```

RequestId tốt nhất cho một HTTP request đồng bộ. Với Kafka async, có thể cần eventId hoặc business code để nối producer và consumer.

## Debug theo status/error

Duplicate code:

```logql
{service="tenant-demo"} |= "409"
```

Ý nghĩa: duplicate `master_data.code` đã được map thành API conflict, không còn là server crash `500`.

Viewer forbidden:

```logql
{service=~"tenant-demo|kong-gateway"} |= "403"
```

Ý nghĩa: user đã authenticated nhưng không đủ quyền tạo master data.

Quick error scan:

```logql
{service=~"tenant-demo|audit-log-service|search-service|kong-gateway"} |= "ERROR"
```

Nếu không có kết quả trong window đang xem, đó thường là tín hiệu tốt. Nhưng WARN/ERROR cũ vẫn có thể còn trong Loki volume nếu service từng bị tắt dependency.

## Query theo source

Host file logs:

```logql
{source="file"}
```

Hiện gồm `tenant-demo` khi chạy `make app-run-logs`, `audit-log-service` khi chạy `make audit-log-run-logs`, `file-service` khi chạy `make file-run-logs`, và `search-service` khi chạy `make search-run-logs`.

Docker stdout logs:

```logql
{source="docker"}
```

Gồm `kong-gateway`, `web-ui-demo` và các container/infra tools khác có label phù hợp. Java backend services local hiện ưu tiên file logs.

## Label vs text search

Nên dùng label ít biến động:

- `service`
- `source`
- `environment`
- `container`
- `filename`
- `job`

Không dùng các giá trị sau làm label:

- `requestId`
- `tenantId`
- `userId`
- code nghiệp vụ
- token
- object key

Lý do: chúng có cardinality cao hoặc nhạy cảm. Hãy để chúng trong log message và search bằng `|=`.

## Debug flow gợi ý

1. Bắt đầu từ `{service="kong-gateway"}` để xem request có tới gateway không.
2. Sang `{service="tenant-demo"}` để xem backend auth, tenant, DB, Kafka publish.
3. Sang `{service="audit-log-service"}` để xem Kafka consume/store.
4. Sang `{service="file-service"}` khi debug upload/download MinIO.
5. Sang `{service="search-service"}` khi debug Elasticsearch projection/indexing.
6. Search theo code/eventId nếu flow async.
7. Search theo requestId nếu đang debug một HTTP request cụ thể.
8. Search `409`, `403`, `ERROR` khi cần xem lỗi.

Đây là cách đọc log thực dụng: đi từ lớp ngoài vào lớp trong, rồi nối các dòng log bằng requestId hoặc business key.
