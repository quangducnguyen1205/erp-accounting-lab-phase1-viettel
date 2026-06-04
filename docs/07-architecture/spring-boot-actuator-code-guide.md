# Spring Boot Actuator code guide cho `tenant-demo`

## Vai trò tài liệu

Tài liệu này là code guide cho mini-lab Observability/logging/metrics. Mục tiêu là giải thích Actuator/Micrometer nhỏ, an toàn, không dựng full monitoring stack.

Trạng thái hiện tại: Actuator baseline đã được thêm vào `tenant-demo`. Bước tiếp theo là student chạy/đọc endpoint, rồi nếu cần mới tự thêm request logging hoặc custom metric nhỏ.

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

## 5. Request logging nhỏ nên đặt ở đâu?

Nếu muốn tự code request logging:

- Dùng `OncePerRequestFilter` hoặc `HandlerInterceptor`.
- Log method, path, status, duration.
- Nếu có `TenantContext`, có thể log tenantId sau khi token đã validate.
- Không log Authorization header/token.
- Không log full body.

Package gợi ý:

```text
com.viettel.demo.observability
├── RequestLoggingFilter.java      # optional
└── ObservabilityProperties.java   # optional nếu cần bật/tắt
```

Không nên đưa logging trực tiếp vào từng controller nếu mục tiêu là quan sát request chung.

---

## 6. Metrics bằng Micrometer

Actuator dùng Micrometer để thu metrics. Nếu muốn tự thêm counter/timer nhỏ, inject `MeterRegistry`.

Ví dụ conceptual:

```java
Counter.builder("tenant_demo.kafka.publish.total")
        .tag("topic", topic)
        .tag("result", "success")
        .register(meterRegistry)
        .increment();
```

Chỉ thêm khi có câu hỏi rõ ràng:

- Kafka publish success/failure bao nhiêu lần?
- Redis cache hit/miss bao nhiêu lần?
- MinIO upload/download bao nhiêu lần?

Không nên thêm metric cho mọi dòng code.

---

## 7. Class shape đề xuất cho mini-lab

Giữ nhỏ:

```text
com.viettel.demo.observability
├── ObservabilityProperties.java       # optional: bật/tắt request logging nếu cần
├── RequestLoggingFilter.java          # optional: log request duration/status
└── AppMetrics.java                    # optional: helper rất nhỏ cho counter nếu tránh lặp
```

Không tạo framework lớn. Nếu chỉ bật Actuator và đọc metrics built-in, có thể chưa cần class nào.

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
3. Nếu còn thời gian, thêm request logging filter nhỏ.
4. Nếu còn thời gian nữa, thêm một metric dễ hiểu: Kafka publish success/failure hoặc cache hit/miss.

---

## Nguồn tham khảo chuẩn

- [Spring Boot Actuator endpoints](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html)
- [Spring Boot Actuator metrics](https://docs.spring.io/spring-boot/reference/actuator/metrics.html)
- [Micrometer documentation](https://docs.micrometer.io/micrometer/reference/index.html)
