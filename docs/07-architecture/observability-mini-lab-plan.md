# Observability/logging/metrics mini-lab plan

## Vai trò tài liệu

Đây là checklist cho Milestone #16. Mục tiêu là chạy một mini-lab nhỏ quanh Spring Boot Actuator + logging/metrics cơ bản, không dựng full observability platform.

Trạng thái hiện tại: Actuator baseline đã được implement bằng built-in Spring Boot features. Request logging/custom metrics vẫn là bước tự code hoặc optional sau.

Đọc trước:

- `observability-foundation.md`
- `logging-metrics-tracing.md`
- `spring-boot-actuator-code-guide.md`

---

## 1. Mục tiêu mini-lab

Flow học đề xuất:

```text
Spring Boot app đang chạy
-> Actuator health/info/metrics
-> request logging hoặc một vài metrics nhỏ
-> verify bằng curl/HTTP Client
-> summary: logs/metrics/health giúp vận hành nhưng không thay test
```

---

## 2. Phạm vi nên làm

### Đã làm baseline

- Thêm `spring-boot-starter-actuator`.
- Expose đúng `health`, `info`, `metrics`, không expose `*`.
- Cấu hình endpoint an toàn:
  - `/actuator/health` public local.
  - `/actuator/info` và `/actuator/metrics` authenticated.
- Thêm `info.app.*` metadata explicit, không chứa secret.
- Disable Redis/Elasticsearch health mặc định vì đây là optional labs; nếu muốn quan sát riêng thì bật `ACTUATOR_REDIS_HEALTH_ENABLED=true` hoặc `ACTUATOR_ELASTICSEARCH_HEALTH_ENABLED=true` khi infra tương ứng đang chạy.
- Thêm `lab-code/tenant-demo/http/actuator-api.http` để verify thủ công.

### Có thể làm tiếp

- Thêm request logging nhỏ nếu muốn.
- Thêm 1 metric nhỏ nếu phù hợp:
  - Kafka publish success/failure; hoặc
  - Redis cache hit/miss; hoặc
  - MinIO upload/download count.

### Không làm ngay

- Không dựng Prometheus/Grafana/Loki đầy đủ.
- Không làm tracing distributed.
- Không expose endpoint nhạy cảm.
- Không log token/secret/password.
- Không biến observability thành framework riêng trong repo.

---

## 3. Artifact hiện tại

Artifact đã có:

- `pom.xml`: thêm Actuator.
- `application.yml`: `management.endpoints.web.exposure.include=health,info,metrics`.
- `SecurityConfig`: rule rõ cho actuator endpoints.
- `http/actuator-api.http`: request mẫu cho health/info/metrics.

Artifact optional sau:

- `com.viettel.demo.observability.RequestLoggingFilter`.
- metric nhỏ với `MeterRegistry`.

---

## 4. Verification checklist

- [ ] `cd lab-code/tenant-demo && ./mvnw validate` pass.
- [ ] `cd lab-code && make app-test` pass.
- [ ] `/actuator/health` trả response đúng, không cần token.
- [ ] `/actuator/info` trả `401` nếu thiếu token, `200` nếu có token hợp lệ.
- [ ] `/actuator/metrics` trả `401` nếu thiếu token, `200` nếu có token hợp lệ.
- [ ] Không log token/Authorization header.
- [ ] Request log nếu có không chứa payload nhạy cảm.
- [ ] Summary ghi rõ giới hạn: chưa có Prometheus/Grafana/Loki/tracing production.

---

## 5. Demo nhỏ cho mentor

Có thể trình bày theo thứ tự:

1. Mở `observability-foundation.md` nói logs/metrics/traces/health khác nhau thế nào.
2. Chạy app và gọi `/actuator/health`.
3. Gọi `/actuator/metrics` hoặc giải thích vì sao endpoint được protected.
4. Chỉ một log/request hoặc metric nhỏ nếu đã tự code.
5. Nói caveat: health check không chứng minh tenant isolation; vẫn cần `DataLeakageTest`.

---

## 6. Done criteria

Milestone #16 đóng khi:

- Đã đọc foundation docs.
- Actuator mini-lab hoặc skeleton được tự code/review.
- App tests vẫn pass.
- Endpoint health/metrics được verify local.
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
