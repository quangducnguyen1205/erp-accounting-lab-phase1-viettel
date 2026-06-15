# Log Aggregation / Loki

## Thư mục này chứa gì?

Nhóm này chuẩn bị mini-lab centralized logging cho Phase 1.5. Mục tiêu là không còn phải đọc log thủ công ở nhiều terminal khi repo có Gateway, backend service và Kafka consumer service.

## Thứ tự đọc đề xuất

1. [loki-foundation.md](loki-foundation.md) - Loki là gì, collector/agent, Grafana Explore, labels, Promtail/Alloy caveat.
2. [loki-local-lab-config-walkthrough.md](loki-local-lab-config-walkthrough.md) - giải thích từng file config trong `lab-code/loki-lab/` và log flow Docker -> Alloy -> Loki -> Grafana.
3. [how-to-read-logs-in-grafana.md](how-to-read-logs-in-grafana.md) - cách đọc log trong Grafana Explore theo service/layer/requestId/business code/status.
4. [grafana-loki-ui-screenshot-guide.md](grafana-loki-ui-screenshot-guide.md) - walkthrough bằng screenshot UI thật: chọn datasource, label, service, query và đọc log lines.
5. [../../../lab-code/loki-lab/README.md](../../../lab-code/loki-lab/README.md) - command chạy lab, URL, query mẫu và cleanup.

## Trạng thái

- Foundation doc đã có.
- Runtime Docker lab đã có ở `lab-code/loki-lab/`.
- Stack local dùng Loki + Grafana + Grafana Alloy, không dùng Promtail làm default collector.
- Makefile targets cho final demo và legacy lab:
  - `make up/status/down` nếu muốn chạy toàn bộ final demo.
  - `make -f Makefile.legacy loki-up`
  - `make -f Makefile.legacy loki-status`
  - `make -f Makefile.legacy loki-info`
  - `make -f Makefile.legacy loki-logs`
  - `make -f Makefile.legacy loki-down`

## Cách dùng nhanh

Từ `lab-code/`:

```bash
make -f Makefile.legacy loki-up
make -f Makefile.legacy loki-status
curl -f http://localhost:3100/ready
```

Mở Grafana log lab:

```text
http://localhost:13001
```

Vào `Explore`, chọn datasource `Loki`, query ví dụ:

```logql
{service=~"tenant-demo|audit-log-service|file-service|search-service|kong-gateway|web-ui-demo"}
{service="tenant-demo"}
{service="file-service"}
{service="search-service"}
{service=~"tenant-demo|audit-log-service|file-service|search-service|kong-gateway"} |= "requestId="
```

Xem thêm guide thực hành: [how-to-read-logs-in-grafana.md](how-to-read-logs-in-grafana.md).

Lưu ý: lab dùng mô hình hybrid. Docker services được collect từ stdout; `tenant-demo`, `audit-log-service`, `file-service` và `search-service` host Maven được collect qua `lab-code/logs/*.log` khi chạy `make -f Makefile.legacy app-run-logs`, `make -f Makefile.legacy audit-log-run-logs`, `make -f Makefile.legacy file-run-logs` hoặc `make -f Makefile.legacy search-run-logs`.

## Giới hạn hiện tại

Promtail đã EOL theo Grafana docs. Repo này dùng Grafana Alloy làm collector mặc định. Promtail chỉ nên được nhắc như legacy context, không phải hướng mới cho Phase 1.5.
