# Log Aggregation / Loki

## Folder này chứa gì?

Nhóm này chuẩn bị mini-lab centralized logging cho Phase 1.5. Mục tiêu là không còn phải đọc log thủ công ở nhiều terminal khi repo có Gateway, backend service và Kafka consumer service.

## Reading Order

1. [loki-foundation.md](loki-foundation.md) - Loki là gì, collector/agent, Grafana Explore, labels, Promtail/Alloy caveat.
2. [loki-local-lab-config-walkthrough.md](loki-local-lab-config-walkthrough.md) - giải thích từng file config trong `lab-code/loki-lab/` và log flow Docker -> Alloy -> Loki -> Grafana.
3. [how-to-read-logs-in-grafana.md](how-to-read-logs-in-grafana.md) - cách đọc log trong Grafana Explore theo service/layer/requestId/business code/status.
4. [grafana-loki-ui-screenshot-guide.md](grafana-loki-ui-screenshot-guide.md) - walkthrough bằng screenshot UI thật: chọn datasource, label, service, query và đọc log lines.
5. [../../../lab-code/loki-lab/README.md](../../../lab-code/loki-lab/README.md) - command chạy lab, URL, query mẫu và cleanup.

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
{service=~"tenant-demo|audit-log-service|kong-gateway|web-ui-demo"}
{service="tenant-demo"}
{service=~"tenant-demo|audit-log-service|kong-gateway"} |= "requestId="
```

Xem thêm guide thực hành: [how-to-read-logs-in-grafana.md](how-to-read-logs-in-grafana.md).

Lưu ý: lab dùng mô hình hybrid. Docker services được collect từ stdout; `tenant-demo` host Maven được collect qua file `lab-code/logs/tenant-demo.log` khi chạy `make app-run-logs`.

## Caveat

Promtail đã EOL theo Grafana docs. Repo này dùng Grafana Alloy làm collector mặc định. Promtail chỉ nên được nhắc như legacy context, không phải hướng mới cho Phase 1.5.
