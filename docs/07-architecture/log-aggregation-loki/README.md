# Log Aggregation / Loki

## Folder này chứa gì?

Nhóm này chuẩn bị mini-lab centralized logging cho Phase 1.5. Mục tiêu là không còn phải đọc log thủ công ở nhiều terminal khi repo có Gateway, backend service và Kafka consumer service.

## Reading Order

1. [loki-foundation.md](loki-foundation.md) - Loki là gì, collector/agent, Grafana Explore, labels, Promtail/Alloy caveat.

## Trạng thái

- Foundation doc đã có.
- Runtime Docker lab đã có ở `lab-code/loki-lab/`.
- Stack local dùng Loki + Grafana + Grafana Alloy, không dùng Promtail làm default collector.
- Makefile targets:
  - `make loki-up`
  - `make loki-status`
  - `make loki-info`
  - `make loki-logs`
  - `make loki-down`

## Cách dùng nhanh

Từ `lab-code/`:

```bash
make loki-up
make loki-status
curl -f http://localhost:3100/ready
```

Mở Grafana log lab:

```text
http://localhost:13001
```

Vào `Explore`, chọn datasource `Loki`, query ví dụ:

```logql
{service="web-ui-demo"}
{container="viettel-web-ui-demo"}
{service="web-ui-demo"} |= "web-demo"
```

Lưu ý: Alloy Docker collector chỉ đọc Docker container logs. `tenant-demo` và `gateway-demo` nếu chạy bằng Maven trên host thì log vẫn nằm ở terminal host cho tới khi có Dockerized run path hoặc file-log collector.

## Caveat

Promtail đã EOL theo Grafana docs. Repo này dùng Grafana Alloy làm collector mặc định. Promtail chỉ nên được nhắc như legacy context, không phải hướng mới cho Phase 1.5.
