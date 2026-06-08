# Observability

## Folder này chứa gì?

Nhóm này giải thích logging, metrics, health check, Actuator, Micrometer và local Prometheus/Grafana lab cho `tenant-demo`.

## Reading Order

1. [observability-foundation.md](observability-foundation.md) - foundation: logs, metrics, tracing, health, alert.
2. [logging-metrics-tracing.md](logging-metrics-tracing.md) - shape/cách đọc log, metric, trace, health.
3. [spring-boot-actuator-code-guide.md](spring-boot-actuator-code-guide.md) - Actuator endpoint/config/security flow.
4. [micrometer-custom-metrics.md](micrometer-custom-metrics.md) - Counter/Timer, MeterRegistry, tag cardinality.
5. [prometheus-grafana-local-lab.md](prometheus-grafana-local-lab.md) - `/actuator/prometheus`, Prometheus scrape, Grafana datasource/dashboard.
6. [observability-mini-lab-plan.md](observability-mini-lab-plan.md) - checklist milestone.

## Trạng Thái

- Milestone #16 đã đóng ở Phase 1 level.
- Actuator baseline, request logging, custom metrics và Prometheus/Grafana local lab đã verify.

## Caveat

Chưa có tracing, Loki/ELK log aggregation, Alertmanager, production retention, production access hardening hoặc SLO/SLI/alert rules.
