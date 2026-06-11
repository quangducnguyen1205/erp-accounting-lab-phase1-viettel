# Loki log aggregation lab

Trạng thái: planning stub cho Phase 1.5.

Mini-lab này sẽ thêm centralized log checking cho nhiều service local:

```text
service logs
-> collector/agent
-> Loki
-> Grafana Explore
```

## Mục tiêu khi implement

- Docker-first, không yêu cầu cài tool local.
- Không thay thế Prometheus metrics lab hiện có.
- Tìm log theo `service` và `requestId`.
- Không log token/secret/body nhạy cảm.
- Ưu tiên Grafana Alloy hoặc collector hiện đại; nếu dùng Promtail cho local learning thì ghi rõ Promtail đã EOL.

## Chưa làm trong stub này

- Chưa có `docker-compose.yml`.
- Chưa có Makefile targets.
- Chưa bật vào `infra-up`.

Doc nền: `docs/07-architecture/log-aggregation-loki/`.
