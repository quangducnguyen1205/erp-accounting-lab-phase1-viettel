# Observability local lab: Prometheus + Grafana

Lab này chạy Prometheus và Grafana local để scrape metrics từ `tenant-demo`.
Đây là Phase 1 learning setup, không phải production monitoring stack.

## Local ports

| Tool | URL |
|---|---|
| Prometheus | http://localhost:19090 |
| Grafana | http://localhost:13000 |

Grafana local credentials:

```text
admin / admin
```

Đây là default local lab only. Không dùng credential này cho môi trường thật.

## App target

Prometheus scrape Spring Boot app đang chạy trên host:

```text
host.docker.internal:8080/actuator/prometheus
```

Trên Docker Desktop Mac/Windows, `host.docker.internal` trỏ về máy host.
Trên Linux, compose có `extra_hosts: host.docker.internal:host-gateway`.

## Start/stop

Từ `lab-code/`:

```bash
make observability-up
make observability-status
make observability-down
```

Hoặc chạy trực tiếp:

```bash
cd lab-code/observability-lab
docker compose up -d
docker compose down
```

## Verification flow

1. Start PostgreSQL and optional lab dependencies:

```bash
cd lab-code
make db-up
make redis-up
make kafka-up
```

2. Start `tenant-demo` on host with the features you want to observe.

Example for Redis + Kafka metrics:

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

3. Verify Prometheus endpoint:

```bash
curl http://localhost:8080/actuator/prometheus
```

4. Start observability tools:

```bash
cd lab-code
make observability-up
make observability-status
```

5. Open Prometheus:

```text
http://localhost:19090/targets
```

Target `tenant-demo` should be `UP`.

6. Query metrics:

```text
tenant_demo_master_data_cache_requests_total
tenant_demo_kafka_publish_requests_total
tenant_demo_master_data_get_by_code_duration_seconds_count
```

7. Open Grafana:

```text
http://localhost:13000
```

Datasource `Prometheus` is provisioned automatically. Starter dashboard:

```text
Dashboards -> Phase 1 Lab -> Tenant Demo Observability
```

## Cleanup

```bash
cd lab-code
make observability-down
```

Prometheus/Grafana use Docker named volumes. Scraped history survives container restart while volumes exist.

## Out of scope

- Alertmanager.
- Distributed tracing.
- Loki/ELK log aggregation.
- Kubernetes.
- Production auth/network hardening.
- SLO/SLI design.

