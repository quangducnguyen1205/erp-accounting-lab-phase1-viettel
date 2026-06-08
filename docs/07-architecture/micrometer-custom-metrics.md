# Micrometer custom metrics trong `tenant-demo`

## Vai trò tài liệu

Tài liệu này giải thích phần custom metrics nhỏ của Observability mini-lab. Mục tiêu là hiểu cách code app tự ghi metric có ý nghĩa nghiệp vụ/kỹ thuật, rồi Actuator hiển thị metric đó qua `/actuator/metrics`.

Chưa làm Prometheus/Grafana trong bước này.

---

## 1. Built-in metrics vs custom metrics

Actuator/Micrometer tự có nhiều metric built-in, ví dụ JVM memory, HTTP server requests, datasource pool.

Custom metrics là metric do code app chủ động ghi thêm khi có câu hỏi rõ:

- Redis cache hit/miss ra sao?
- Kafka publish thành công/thất bại bao nhiêu lần?
- Lookup `master_data` theo code mất bao lâu?

Custom metric không thay log. Log trả lời “request/event cụ thể đã xảy ra gì”; metric trả lời xu hướng tổng hợp.

---

## 2. `MeterRegistry` là gì?

`MeterRegistry` là nơi app đăng ký/ghi meter:

- `Counter`: số đếm chỉ tăng.
- `Timer`: đo duration và thống kê count/total/max.
- Gauge và DistributionSummary có thể học sau.

Trong repo, class nhỏ:

```text
com.viettel.demo.observability.ApplicationMetrics
```

giữ tên metric/tag ổn định để service/gateway không tự đặt tên lung tung.

---

## 3. Counter

Counter dùng khi muốn đếm số lần một việc xảy ra.

Metric đã thêm:

```text
tenant_demo.master_data.cache.requests{result="hit|miss"}
tenant_demo.master_data.cache.puts
tenant_demo.master_data.cache.errors{operation="read|write"}
tenant_demo.kafka.publish.requests{event="master_data_changed",result="success|failure"}
```

Không dùng Counter cho giá trị có thể tăng giảm như số item hiện tại trong database.

---

## 4. Timer

Timer dùng khi muốn đo thời gian.

Metric đã thêm:

```text
tenant_demo.master_data.get_by_code.duration{cache="enabled|disabled",result="found|not_found|error"}
tenant_demo.kafka.publish.duration{event="master_data_changed",result="success|failure"}
```

Actuator thường trả về các measurement như `COUNT`, `TOTAL_TIME`, `MAX`. Nếu app restart, các giá trị local này reset vì hiện chưa có monitoring backend bên ngoài.

---

## 5. Tag cardinality

Tag giúp chia metric theo vài nhóm cố định. Tag tốt:

- `result=hit|miss`
- `result=success|failure`
- `cache=enabled|disabled`
- `event=master_data_changed`

Tag nguy hiểm trong production:

- `requestId`
- `userId`
- raw `tenantId`
- raw `code`
- JWT subject
- `eventId`
- object key/file id

Lý do: những giá trị này có thể có hàng nghìn/hàng triệu biến thể, làm metric backend phình rất nhanh. Với local lab có vẻ không sao, nhưng thói quen đúng là không đưa high-cardinality data vào tag.

Nếu cần debug một request cụ thể, dùng log/requestId. Nếu cần xu hướng tổng hợp, dùng metric.

---

## 6. Metrics hiện tại nằm ở đâu trong code?

```text
ApplicationMetrics
├── MasterDataCacheGateway: cache hit/miss/put/error
├── KafkaMasterDataEventPublisher: publish success/failure/duration
└── MasterDataService.getByCode: duration theo cache enabled/disabled và result
```

Flow Redis:

```text
GET /api/master-data/code/{code}
-> MasterDataService.getByCode
-> nếu cache enabled: MasterDataCacheGateway.getByCode
-> hit/miss metric
-> miss thì query PostgreSQL và put Redis
```

Flow Kafka:

```text
POST/PUT /api/master-data
-> repository.save(...)
-> KafkaMasterDataEventPublisher.publish(...)
-> success/failure counter + duration timer
```

PostgreSQL vẫn là source of truth; Redis/Kafka chỉ là cache/event layer.

---

## 7. Cách verify

Sau khi app chạy và có token:

```bash
curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/actuator/metrics
```

Sau khi gọi path tương ứng, xem metric cụ thể:

```bash
curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/actuator/metrics/tenant_demo.master_data.get_by_code.duration

curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/actuator/metrics/tenant_demo.master_data.cache.requests

curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/actuator/metrics/tenant_demo.kafka.publish.requests
```

Nếu metric chưa từng được ghi trong process hiện tại, endpoint metric cụ thể có thể chưa có dữ liệu.

---

## 8. Caveat Phase 1

- Metric hiện mới dùng registry local trong app.
- Chưa có Prometheus/Grafana.
- App restart thì giá trị local reset.
- Chưa có alert.
- Chưa thêm nhiều custom metrics để tránh làm nhiễu code học tập.
- Không dùng metric để chứng minh tenant isolation; việc đó vẫn cần test như `DataLeakageTest`.

