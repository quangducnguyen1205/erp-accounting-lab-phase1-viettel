# Spring Boot Actuator code guide cho `tenant-demo`

## Vai trò tài liệu

Tài liệu này là code guide cho mini-lab Observability/logging/metrics. Mục tiêu là giải thích Actuator/Micrometer + request logging nhỏ, an toàn, không dựng full monitoring stack.

Trạng thái hiện tại: Actuator baseline và request logging baseline đã được thêm vào `tenant-demo`. Bước tiếp theo là student chạy/đọc endpoint/log, rồi nếu cần mới tự thêm custom metric nhỏ.

---

## 1. Spring Boot Actuator là gì?

Spring Boot Actuator thêm các endpoint vận hành cho app, ví dụ:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/prometheus` nếu thêm Prometheus registry sau

Actuator không thay test nghiệp vụ. Nó giúp quan sát app đang chạy.

---

## 2. Dependency tối thiểu

Đã thêm vào `lab-code/tenant-demo/pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Không cần thêm Prometheus ngay. Nếu sau này muốn `/actuator/prometheus`, mới thêm registry phù hợp.

---

## 3. Config tối thiểu đề xuất

Trong `application.yml`, repo chỉ expose endpoint cần học:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when_authorized
  info:
    env:
      enabled: true
  health:
    redis:
      enabled: false
    elasticsearch:
      enabled: false

info:
  app:
    name: tenant-demo
    phase: phase-1-learning-lab
    description: Spring Boot tenant-aware backend learning demo
```

Lý do:

- `health`: verify service sống.
- `info`: metadata app nếu muốn.
- `metrics`: xem metric names và giá trị cơ bản.
- `management.health.redis.enabled=false`: vì Redis là mini-lab optional, không để Redis down làm health baseline của app bị `DOWN`.
- `management.health.elasticsearch.enabled=false`: tương tự cho Elasticsearch/search mini-lab.
- Không expose `env`, `beans`, `configprops` public trong lab vì dễ lộ config nhạy cảm.

---

## 4. SecurityConfig cần nghĩ gì?

`tenant-demo` đang có custom `SecurityFilterChain`. Vì vậy Actuator endpoint sẽ chịu rule security hiện tại nếu không cấu hình riêng.

Gợi ý Phase 1:

- Có thể permit public `/actuator/health` cho local health check.
- Có thể để `/actuator/info` public hoặc authenticated tùy lab.
- Nên giữ `/actuator/metrics` authenticated trong app này.
- Không expose endpoint nhạy cảm public.

Rule đã chọn trong repo:

- `GET /actuator/health`: public.
- `GET /actuator/info`: authenticated.
- `GET /actuator/metrics`: authenticated.
- Business APIs vẫn giữ rule cũ, không bị permit all.

Lưu ý: `JwtTenantContextFilter` chỉ set `TenantContext` khi có JWT đã validate. Vì vậy health public không cần tenant context; còn info/metrics nếu gọi bằng token thì token lab nên có `tenant_id`.

---

## 5. Request logging nhỏ đặt ở đâu?

Repo dùng:

```text
com.viettel.demo.observability.RequestLoggingFilter
```

Class này dùng `OncePerRequestFilter` để log một dòng sau mỗi HTTP request:

- method.
- path, không log query string.
- status.
- durationMs.
- requestId từ header `X-Request-Id`, hoặc UUID tự sinh.
- tenantId nếu JWT đã validate và tenant context flow set được request attribute nội bộ.

Filter không log Authorization header/token/body/response body.

Package gợi ý:

```text
com.viettel.demo.observability
├── RequestLoggingFilter.java      # log request duration/status/requestId
└── ObservabilityProperties.java   # optional nếu cần bật/tắt
```

Không nên đưa logging trực tiếp vào từng controller nếu mục tiêu là quan sát request chung.

### MDC trong log pattern

`RequestLoggingFilter` đưa `requestId` vào MDC:

```java
MDC.put("requestId", requestId);
```

`application.yml` đọc giá trị này trong console pattern:

```yaml
logging:
  pattern:
    console: "... [requestId=%X{requestId:-no-request}] ..."
```

Với log ngoài HTTP request, `requestId` sẽ là `no-request`.

Lưu ý async/Kafka: MDC không tự lan qua thread/process khác. Nếu muốn nối HTTP request với Kafka event, cần truyền correlation id qua event/header riêng.

---

## 6. Metrics bằng Micrometer

Actuator dùng Micrometer để thu metrics. Khi tự thêm counter/timer nhỏ, inject `MeterRegistry`.

Repo đã thêm class nhỏ:

```text
com.viettel.demo.observability.ApplicationMetrics
```

Class này giữ tên metric/tag ổn định để service/gateway chỉ gọi method rõ nghĩa.

Metrics hiện có:

| Metric | Loại | Tags | Ý nghĩa |
|---|---|---|---|
| `tenant_demo.master_data.cache.requests` | Counter | `result=hit|miss` | Cache read hit/miss |
| `tenant_demo.master_data.cache.puts` | Counter | none | Số lần set Redis cache |
| `tenant_demo.master_data.cache.errors` | Counter | `operation=read|write` | Lỗi cache read/write/serialize |
| `tenant_demo.master_data.get_by_code.duration` | Timer | `cache=enabled|disabled`, `result=found|not_found|error` | Duration lookup by code |
| `tenant_demo.kafka.publish.requests` | Counter | `event=master_data_changed`, `result=success|failure` | Kafka publish result |
| `tenant_demo.kafka.publish.duration` | Timer | `event=master_data_changed`, `result=success|failure` | Duration publish Kafka |

Không tag bằng `tenantId`, `requestId`, `code`, `eventId`, `userId` vì đó là high-cardinality data.

Không nên thêm metric cho mọi dòng code.

---

## 7. Class shape đề xuất cho mini-lab

Giữ nhỏ:

```text
com.viettel.demo.observability
├── ObservabilityProperties.java       # optional: bật/tắt request logging nếu cần
├── RequestLoggingFilter.java          # log request duration/status/requestId
└── ApplicationMetrics.java            # custom Counter/Timer nhỏ, low-cardinality tags
```

Không tạo framework lớn. Hiện repo chỉ thêm một filter nhỏ và một metrics component nhỏ.

---

## 8. Verification checklist

Sau khi thay đổi Actuator baseline:

```bash
cd lab-code/tenant-demo
./mvnw validate

cd ../
make app-test
```

Chạy app:

```bash
cd lab-code
make db-up
make app-run
```

Manual check:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/info
curl http://localhost:8080/actuator/metrics
```

`/actuator/info` và `/actuator/metrics` đang yêu cầu token. Dùng `lab-code/tenant-demo/http/actuator-api.http` và paste token vào biến local/private env, không commit token thật.

---

## 9. Common mistakes khi code

- Thêm Actuator rồi public quá nhiều endpoint.
- Log token để debug auth.
- Gắn metrics vào controller thay vì service/infrastructure point phù hợp.
- Dùng tenantId từ request param/body.
- Tạo quá nhiều abstraction cho vài counter đơn giản.
- Quên rằng metrics/logs không thay integration tests.

---

## 10. Bước tự code tiếp theo

1. Chạy app và curl endpoints bằng `actuator-api.http`.
2. Đọc response của `health`, `info`, `metrics`.
3. Gọi `observability-api.http` để xem request log có/không có `X-Request-Id`.
4. Gọi các endpoint `/actuator/metrics/tenant_demo...` trong `observability-api.http`.
5. Nếu còn thời gian nữa, thử bật Redis/Kafka để thấy metric tăng sau hit/miss hoặc publish event.

---

## Nguồn tham khảo chuẩn

- [Spring Boot Actuator endpoints](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html)
- [Spring Boot Actuator metrics](https://docs.spring.io/spring-boot/reference/actuator/metrics.html)
- [Micrometer documentation](https://docs.micrometer.io/micrometer/reference/index.html)
