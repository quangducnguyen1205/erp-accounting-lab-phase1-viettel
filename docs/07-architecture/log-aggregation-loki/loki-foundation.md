# Loki foundation

## 1. Vì sao cần centralized logs?

Khi chỉ có một app `tenant-demo`, đọc log terminal còn ổn. Khi Phase 1.5 có Kong, master-data-service, audit-log-service, Kafka consumer và nhiều container, cách đọc từng terminal không scale.

Centralized logging giúp:

- gom log từ nhiều service vào một nơi;
- tìm theo `requestId`, service name, level hoặc keyword;
- debug một request đi qua Gateway -> backend -> Kafka consumer;
- xem lỗi sau khi terminal đã scroll mất hoặc container restart.

Logs trả lời câu hỏi **"chuyện gì đã xảy ra?"**. Metrics trả lời **"bao nhiêu, nhanh/chậm, lỗi tăng không?"**. Traces trả lời **"một request đi qua các component nào?"**.

## 2. Loki là gì?

Loki là log aggregation system trong hệ sinh thái Grafana. Mental model đơn giản:

```text
App/container logs
-> collector/agent
-> Loki
-> Grafana Explore
```

Loki lưu log theo stream labels và nội dung log. Grafana dùng Loki datasource để query log bằng LogQL.

## 3. Agent/collector role

App thường không tự gọi Loki. App chỉ ghi log ra stdout/file. Một collector đọc log đó rồi gửi vào Loki.

Các lựa chọn collector phổ biến:

- Grafana Alloy: hướng hiện đại trong Grafana ecosystem.
- Fluent Bit/Fluentd: collector/log forwarder phổ biến.
- Docker logging driver: có thể push container logs theo driver.
- Promtail: agent cũ của Loki.

Theo Grafana Loki docs, Promtail đã EOL từ 02/03/2026, commercial support đã kết thúc và future feature development chuyển sang Grafana Alloy. Vì vậy Phase 1.5 nên ưu tiên Alloy nếu không quá nặng. Nếu tạm dùng Promtail để học local, phải ghi caveat rõ.

Nguồn: [Grafana Loki Promtail docs](https://grafana.com/docs/loki/latest/send-data/promtail/) và [Grafana Loki Alloy docs](https://grafana.com/docs/loki/latest/send-data/alloy/).

## 4. Grafana Explore role

Grafana không chỉ vẽ dashboard metric. Với Loki datasource, Grafana Explore có thể:

- query log theo label, ví dụ `{service="tenant-demo"}`;
- filter keyword, ví dụ `|= "requestId=web-demo"`;
- xem log theo thời gian;
- chuyển từ metric spike sang log cùng thời điểm.

Trong demo, mục tiêu là mở Grafana Explore và tìm được:

- log của Gateway;
- log của master-data-service/tenant-demo;
- log Kafka consumer hoặc audit-log-service;
- cùng một `requestId` nếu header được propagate.

## 5. Labels và high-cardinality caveat

Loki dùng labels để chọn log stream. Labels nên là giá trị ít biến động:

- `service=tenant-demo`
- `service=gateway`
- `env=local`
- `level=INFO`

Không nên biến các giá trị quá nhiều thành label:

- `requestId`
- `userId`
- raw `tenantId`
- JWT subject
- object key
- eventId

Các giá trị này nên nằm trong log line/structured fields để search, không phải label chính. High-cardinality labels làm Loki nặng và khó vận hành.

## 6. Áp dụng vào repo này

Phase 1.5 nên log aggregation theo hướng:

```text
Kong / Spring Gateway logs
tenant-demo logs
future audit-log-service logs
-> Alloy or simple collector
-> Loki
-> Grafana Explore
```

Request logging hiện đã có `X-Request-Id`/MDC. Khi log vào Loki, demo có thể:

1. gọi UI tạo `master_data`;
2. copy requestId từ UI;
3. vào Grafana Explore;
4. tìm requestId để thấy log backend;
5. sau khi có audit service, thấy thêm consumer log.

## 7. Local lab direction

Mini-lab nên Docker-first:

- Loki container.
- Collector container đọc Docker logs hoặc mounted log file.
- Grafana datasource Loki.
- Makefile targets: `loki-up`, `loki-status`, `loki-down`.

Không cần:

- production retention;
- object storage backend cho Loki;
- distributed Loki;
- alerting rule;
- Kubernetes.

## 8. Common mistakes

- Log full token/password/body.
- Dùng `tenantId`/`requestId` làm Loki label.
- Nghĩ Loki thay Prometheus. Loki là logs, Prometheus là metrics.
- Nghĩ Grafana tự lấy log từ app. Grafana query Loki; collector mới là thứ gửi log vào Loki.
- Dựng stack quá lớn trước khi có nhiều service thật.
