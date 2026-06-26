# Observability

## Thư mục này chứa gì?

Nhóm này giải thích logging, metrics, health check, Actuator, Micrometer và local Prometheus/Grafana lab cho `tenant-demo`.

## Thứ tự đọc đề xuất

1. [observability-foundation.md](observability-foundation.md) - foundation: logs, metrics, tracing, health, alert.
2. [logging-metrics-tracing.md](logging-metrics-tracing.md) - shape/cách đọc log, metric, trace, health.
3. [spring-boot-actuator-code-guide.md](spring-boot-actuator-code-guide.md) - Actuator endpoint/config/security flow.
4. [micrometer-custom-metrics.md](micrometer-custom-metrics.md) - Counter/Timer, MeterRegistry, tag cardinality.
5. [prometheus-grafana-local-lab.md](prometheus-grafana-local-lab.md) - `/actuator/prometheus`, Prometheus scrape, Grafana datasource/dashboard.

## Trạng Thái

- Milestone #16 đã đóng ở Phase 1 level.
- Actuator baseline, request logging, custom metrics và Prometheus/Grafana local lab đã verify.

## Trạng thái hiện tại

Phase 1.5 đã bổ sung Loki + Grafana Alloy cho local log aggregation (`lab-code/loki-lab/`). Observability local hiện bao gồm:

- Prometheus metrics scrape.
- Grafana dashboard/query.
- Loki centralized container/file log search.
- Alloy log collection.

Đọc thêm: `docs/07-architecture/log-aggregation-loki/`.

## Giới hạn hiện tại

Chưa có distributed tracing, Alertmanager, production ELK, production retention, production access hardening hoặc SLO/SLI/alert rules.
