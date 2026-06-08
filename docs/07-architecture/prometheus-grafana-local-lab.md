# Prometheus + Grafana local lab

## Vai trò tài liệu

Tài liệu này giải thích mini-lab Prometheus + Grafana local cho `tenant-demo`.
Mục tiêu là hiểu vai trò thật của monitoring stack sau khi đã có Actuator,
request logging và custom Micrometer metrics.

Đây vẫn là Phase 1 learning lab, không phải production monitoring platform.

---

## 1. Mental model

```text
Spring Boot tenant-demo
-> /actuator/prometheus
-> Prometheus scrape định kỳ
-> Prometheus lưu time-series
-> Grafana đọc Prometheus datasource
-> dashboard/query
```

Điểm quan trọng: app không push metric trực tiếp lên Grafana.

- App expose metric.
- Prometheus pull/scrape metric.
- Grafana query Prometheus để vẽ.

---

## 2. `/actuator/metrics` khác `/actuator/prometheus` thế nào?

`/actuator/metrics`:

- Endpoint JSON của Spring Boot Actuator.
- Dùng để inspect nhanh metric names và một metric cụ thể.
- Trong repo đang yêu cầu Bearer token.

Ví dụ:

```text
GET /actuator/metrics/tenant_demo.master_data.cache.requests
```

`/actuator/prometheus`:

- Endpoint text format cho Prometheus scrape.
- Chỉ xuất hiện sau khi thêm `micrometer-registry-prometheus`.
- Trong local lab đang public để Prometheus container scrape đơn giản.

Ví dụ output:

```text
tenant_demo_master_data_cache_requests_total{result="hit",} 1.0
```

Production không nên expose endpoint này bừa bãi ra Internet. Thường sẽ
restrict bằng private network, firewall/network policy, gateway, mTLS hoặc
auth proxy.

---

## 3. Vì sao cần `micrometer-registry-prometheus`?

Actuator + Micrometer đã thu metric trong app. Nhưng Prometheus cần format
riêng để scrape. Dependency:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

đăng ký Prometheus registry và làm `/actuator/prometheus` có output đúng
Prometheus text exposition format.

---

## 4. Time-series là gì?

Time-series là chuỗi giá trị theo thời gian:

```text
time=10:00 value=1
time=10:05 value=3
time=10:10 value=8
```

Prometheus scrape endpoint định kỳ, ví dụ mỗi 5 giây, rồi lưu các sample đó.
Vì vậy dashboard không chỉ thấy “giá trị hiện tại”, mà còn thấy xu hướng.

Trong local lab, Prometheus container có named volume nên lịch sử scrape còn
tồn tại sau khi restart container. Nhưng đây vẫn là retention local, không
phải long-term production storage.

---

## 5. Local lab files

```text
lab-code/observability-lab/
├── docker-compose.yml
├── prometheus.yml
├── grafana/provisioning/datasources/prometheus.yml
├── grafana/provisioning/dashboards/dashboards.yml
└── grafana/dashboards/tenant-demo-observability.json
```

Ports:

| Tool | URL |
|---|---|
| Prometheus | `http://localhost:19090` |
| Grafana | `http://localhost:13000` |

Grafana local credential:

```text
admin / admin
```

Đây là local default để học. Không dùng cho môi trường thật.

---

## 6. Prometheus scrape target

`prometheus.yml` scrape app ở:

```text
host.docker.internal:8080/actuator/prometheus
```

Vì Spring Boot app chạy trên host, còn Prometheus chạy trong Docker container.

- Docker Desktop Mac/Windows: `host.docker.internal` có sẵn.
- Linux: compose thêm `extra_hosts: host.docker.internal:host-gateway`.

---

## 7. Metric names sau khi qua Prometheus

Micrometer metric name có dấu chấm:

```text
tenant_demo.master_data.cache.requests
```

Prometheus expose thành dạng underscore, và Counter thường thêm `_total`:

```text
tenant_demo_master_data_cache_requests_total
```

Timer thường có nhiều series:

```text
tenant_demo_master_data_get_by_code_duration_seconds_count
tenant_demo_master_data_get_by_code_duration_seconds_sum
tenant_demo_master_data_get_by_code_duration_seconds_max
```

Metric names đã observe trong lab:

```text
tenant_demo_master_data_cache_requests_total
tenant_demo_master_data_cache_puts_total
tenant_demo_master_data_get_by_code_duration_seconds_count
tenant_demo_master_data_get_by_code_duration_seconds_sum
tenant_demo_kafka_publish_requests_total
tenant_demo_kafka_publish_duration_seconds_count
tenant_demo_kafka_publish_duration_seconds_sum
```

---

## 8. Verification flow

1. Start app dependencies:

```bash
cd lab-code
make db-up
make redis-up
make kafka-up
```

2. Start app on host:

```bash
cd lab-code/tenant-demo
APP_AUTH_MODE=local-jwt \
JWT_DEV_TOKEN_ENABLED=true \
APP_CACHE_ENABLED=true \
APP_MESSAGING_ENABLED=true \
KAFKA_BOOTSTRAP_SERVERS=localhost:19092 \
APP_SEARCH_ENABLED=false \
APP_FILE_STORAGE_ENABLED=false \
./mvnw spring-boot:run
```

3. Verify scrape endpoint:

```bash
curl http://localhost:8080/actuator/prometheus
```

4. Generate activity:

- call `GET /api/master-data/code/LAPTOP-01` twice to create cache miss/hit.
- create/update `master_data` once to publish Kafka event.

5. Start observability tools:

```bash
cd lab-code
make observability-up
make observability-status
```

6. Open Prometheus:

```text
http://localhost:19090/targets
```

Target `tenant-demo` should be `UP`.

7. Query custom metrics:

```text
tenant_demo_master_data_cache_requests_total
tenant_demo_kafka_publish_requests_total
tenant_demo_master_data_get_by_code_duration_seconds_count
```

8. Open Grafana:

```text
http://localhost:13000
```

Dashboard:

```text
Dashboards -> Phase 1 Lab -> Tenant Demo Observability
```

---

## 9. Production caveats

Chưa làm trong Phase 1:

- Alertmanager/alert rules.
- Distributed tracing.
- Loki/ELK log aggregation.
- Kubernetes/service discovery.
- Auth/network hardening cho Prometheus endpoint.
- Long-term retention/remote write.
- Dashboard governance.
- SLO/SLI definition.

Health/log/metric/tracing đều hỗ trợ vận hành, nhưng không thay correctness
tests như `DataLeakageTest`.

---

## Nguồn tham khảo chuẩn

- [Spring Boot Actuator endpoints](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html)
- [Spring Boot Actuator metrics](https://docs.spring.io/spring-boot/reference/actuator/metrics.html)
- [Micrometer Prometheus registry](https://docs.micrometer.io/micrometer/reference/implementations/prometheus.html)
- [Prometheus overview](https://prometheus.io/docs/introduction/overview/)
- [Grafana Prometheus data source](https://grafana.com/docs/grafana/latest/datasources/prometheus/)

