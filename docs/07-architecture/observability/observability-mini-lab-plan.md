# Observability/logging/metrics mini-lab plan

## Vai trò tài liệu

Đây là checklist cho Milestone #16. Mục tiêu là chạy một mini-lab nhỏ quanh Spring Boot Actuator + logging/metrics cơ bản, không dựng full observability platform.

Trạng thái hiện tại: đã đóng ở Phase 1 learning level. Actuator baseline, request logging baseline, custom Micrometer metrics baseline và local Prometheus/Grafana lab đã được implement, đọc hiểu và verify. Phase 1.5 đã bổ sung Loki + Alloy cho local log aggregation (`lab-code/loki-lab/`). Tracing và alerting vẫn là optional sau.

Đọc trước:

- `observability-foundation.md`
- `logging-metrics-tracing.md`
- `spring-boot-actuator-code-guide.md`
- `micrometer-custom-metrics.md`
- `prometheus-grafana-local-lab.md`

---

## 1. Mục tiêu mini-lab

Flow học đề xuất:

```text
Spring Boot app đang chạy
-> Actuator health/info/metrics
-> request logging hoặc một vài metrics nhỏ
-> /actuator/prometheus cho Prometheus scrape
-> Grafana đọc Prometheus datasource
-> verify bằng curl/HTTP Client
-> summary: logs/metrics/health giúp vận hành nhưng không thay test
```

---

## 2. Phạm vi nên làm

### Đã làm baseline

- Thêm `spring-boot-starter-actuator`.
- Expose đúng `health`, `info`, `metrics`, `prometheus`, không expose `*`.
- Cấu hình endpoint an toàn:
  - `/actuator/health` public local.
  - `/actuator/prometheus` public trong local lab để Prometheus container scrape đơn giản.
  - `/actuator/info` và `/actuator/metrics` authenticated.
- Thêm `info.app.*` metadata explicit, không chứa secret.
- Disable Redis/Elasticsearch health mặc định vì đây là optional labs; nếu muốn quan sát riêng thì bật `ACTUATOR_REDIS_HEALTH_ENABLED=true` hoặc `ACTUATOR_ELASTICSEARCH_HEALTH_ENABLED=true` khi infra tương ứng đang chạy.
- Thêm `lab-code/tenant-demo/http/actuator-api.http` để verify thủ công.
- Thêm `RequestLoggingFilter`:
  - nhận `X-Request-Id` hoặc tự sinh UUID;
  - đưa `requestId` vào MDC;
  - log method/path/status/duration/requestId sau request;
  - log được cả request bị `401`;
  - có thể log tenantId nếu token đã validate và tenant context flow set được request attribute nội bộ;
  - không log body/query string/token;
  - skip `/actuator/health` để giảm noise.
- Đổi Redis cache hit/miss từ `System.out/System.err` sang SLF4J.
- Thêm `lab-code/tenant-demo/http/observability-api.http` để verify request id.
- Thêm `ApplicationMetrics`:
  - Redis cache request counter: `tenant_demo.master_data.cache.requests{result=hit|miss}`.
  - Redis cache put counter: `tenant_demo.master_data.cache.puts`.
  - Redis cache error counter: `tenant_demo.master_data.cache.errors{operation=read|write}`.
  - Kafka publish counter/timer: `tenant_demo.kafka.publish.requests`, `tenant_demo.kafka.publish.duration`.
  - MasterData getByCode timer: `tenant_demo.master_data.get_by_code.duration`.
  - Không dùng high-cardinality tags như tenantId, requestId, code, eventId.
- Thêm `micrometer-registry-prometheus` để có `/actuator/prometheus`.
- Thêm `lab-code/observability-lab/`:
  - Prometheus scrape `tenant-demo` qua `host.docker.internal:8080/actuator/prometheus`.
  - Grafana đọc Prometheus datasource đã provision sẵn.
  - Dashboard nhỏ: JVM memory, Redis cache requests, Kafka publish requests, getByCode duration.

### Đã verify local

- Redis cache enabled:
  - Gọi `GET /api/master-data/code/LAPTOP-01` hai lần tạo miss -> put -> hit.
  - `/actuator/metrics/tenant_demo.master_data.cache.requests` có `result=hit|miss`, count tăng.
  - `/actuator/metrics/tenant_demo.master_data.cache.puts` có count tăng.
  - `/actuator/metrics/tenant_demo.master_data.get_by_code.duration` có tag `cache=enabled`, `result=found`.
- Kafka messaging enabled:
  - Create `master_data` trả `201`.
  - Log có `Published Kafka event` và `Consumed Kafka event`.
  - `/actuator/metrics/tenant_demo.kafka.publish.requests` có `event=master_data_changed`, `result=success`.
  - `/actuator/metrics/tenant_demo.kafka.publish.duration` có timer measurement.
- Không thấy tag high-cardinality như tenantId, requestId, code, eventId, userId trong custom metric tags.
- Prometheus/Grafana local lab:
  - `/actuator/prometheus` trả Prometheus text format.
  - Prometheus target `tenant-demo` phải `UP`.
  - Grafana datasource `Prometheus` phải hoạt động.

### Có thể làm tiếp

- Thêm 1 metric nhỏ nếu có câu hỏi vận hành rõ:
  - MinIO upload/download count; hoặc
  - HTTP business error count nếu có câu hỏi rõ.
- Polish dashboard rất nhẹ nếu cần demo mentor.

### Không làm ngay (ghi chú phạm vi Phase 1 ban đầu)

- Không dựng production observability platform.
- Không làm tracing distributed.
- Không thêm Loki/ELK log aggregation trong Milestone #16. *(Ghi chú Phase 1.5: Loki + Alloy local lab đã được bổ sung sau trong `lab-code/loki-lab/`.)*
- Không thêm Alertmanager/alert rules.
- Không expose endpoint nhạy cảm.
- Không log token/secret/password.
- Không biến observability thành framework riêng trong repo.

---

## 3. Artifact hiện tại

Artifact đã có:

- `pom.xml`: thêm Actuator.
- `application.yml`: `management.endpoints.web.exposure.include=health,info,metrics,prometheus`.
- `SecurityConfig`: rule rõ cho actuator endpoints.
- `http/actuator-api.http`: request mẫu cho health/info/metrics.
- `com.viettel.demo.observability.RequestLoggingFilter`.
- `com.viettel.demo.observability.ApplicationMetrics`.
- `http/observability-api.http`: request mẫu để quan sát `X-Request-Id` và request log.
  Đồng thời có request mẫu đọc custom metrics.
- `lab-code/observability-lab/`: Docker Compose cho Prometheus + Grafana local.
- `docs/07-architecture/observability/prometheus-grafana-local-lab.md`: hướng dẫn scrape/dashboard local.

Artifact optional sau:

- Loki/tracing/Alertmanager/production dashboard governance.

---

## 4. Verification checklist

- [x] `cd lab-code/tenant-demo && ./mvnw validate` pass.
- [x] `cd lab-code && make app-test` pass.
- [x] `/actuator/health` trả response đúng, không cần token.
- [x] `/actuator/prometheus` trả Prometheus text format, không cần token trong local lab.
- [x] `/actuator/info` trả `401` nếu thiếu token, `200` nếu có token hợp lệ.
- [x] `/actuator/metrics` trả `401` nếu thiếu token, `200` nếu có token hợp lệ.
- [x] Không log token/Authorization header.
- [x] Request có `X-Request-Id` tạo log chứa đúng request id đó.
- [x] Request không có `X-Request-Id` tạo log với UUID do app sinh.
- [x] Request log không chứa payload nhạy cảm.
- [x] Custom metrics xuất hiện trong `/actuator/metrics/{name}` sau khi gọi code path tương ứng.
- [x] Custom metric tags không có tenantId/requestId/code/eventId/userId.
- [x] `make -f Makefile.legacy observability-up` chạy Prometheus + Grafana local.
- [x] Prometheus target `tenant-demo` là `UP`.
- [x] Grafana datasource Prometheus hoạt động.
- [x] Summary ghi rõ giới hạn: chưa có Loki/tracing/alerting/production monitoring.

---

## 5. Demo nhỏ cho mentor

Có thể trình bày theo thứ tự:

1. Mở `observability-foundation.md` nói logs/metrics/traces/health khác nhau thế nào.
2. Chạy app và gọi `/actuator/health`.
3. Gọi `/actuator/metrics` hoặc giải thích vì sao endpoint được protected.
4. Gọi `/actuator/prometheus` để thấy Prometheus scrape format.
5. Mở Prometheus target `tenant-demo` và Grafana dashboard nhỏ nếu đã start lab.
6. Nói caveat: health check không chứng minh tenant isolation; vẫn cần `DataLeakageTest`.

---

## 6. Tiêu chí hoàn thành

Milestone #16 đóng khi:

- Đã đọc foundation docs.
- Actuator mini-lab hoặc skeleton được tự code/review.
- App tests vẫn pass.
- Endpoint health/metrics được verify local.
- Prometheus/Grafana local lab chạy được ở mức scrape + datasource/dashboard nhỏ.
- Có summary ngắn trong `docs/99-tong-ket/nhung-gi-da-nam-duoc.md`.
- Không overclaim full observability platform.

---

## 7. Câu hỏi tự kiểm

- Log khác metric thế nào?
- Health check khác business correctness thế nào?
- Có endpoint Actuator nào không nên expose public?
- Có log token/secret không?
- Metric mình thêm trả lời câu hỏi vận hành nào?
- TenantId trong log/metric có cần thiết không, và có rủi ro gì?
