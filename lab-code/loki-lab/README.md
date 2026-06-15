# Loki log aggregation lab

Trạng thái: local Docker lab đã có Loki + Grafana + Grafana Alloy.

Mini-lab này thêm centralized log checking cho nhiều service/tool local:

```text
Docker container stdout logs
-> Grafana Alloy
-> Loki
-> Grafana Explore
```

Từ demo Phase 1.5, Java backend services chạy bằng Maven/IntelliJ trên host. Khi muốn Loki thấy log của chúng, chạy target file-log:

```text
tenant-demo host Maven logs
-> lab-code/logs/tenant-demo.log
-> Grafana Alloy
-> Loki
-> Grafana Explore

audit-log-service host Maven logs
-> lab-code/logs/audit-log-service.log
-> Grafana Alloy
-> Loki
-> Grafana Explore

file-service host Maven logs
-> lab-code/logs/file-service.log
-> Grafana Alloy
-> Loki
-> Grafana Explore

search-service host Maven logs
-> lab-code/logs/search-service.log
-> Grafana Alloy
-> Loki
-> Grafana Explore
```

## Local ports

| Tool | URL |
|---|---|
| Loki readiness | http://localhost:3100/ready |
| Grafana Explore | http://localhost:13001 |
| Alloy debug UI | http://localhost:12345 |

Grafana local credentials:

```text
admin / admin
```

Đây là default local lab only. Không dùng credential này cho môi trường thật.

## Start/stop

Từ `lab-code/`:

```bash
make -f Makefile.legacy loki-up
make -f Makefile.legacy loki-status
make -f Makefile.legacy loki-info
make -f Makefile.legacy loki-down
```

Hoặc chạy trực tiếp:

```bash
cd lab-code/loki-lab
docker compose up -d
docker compose down
```

## Alloy đang collect gì?

Alloy đọc hai nguồn log local.

Nguồn 1: Docker logs qua Docker socket:

```text
/var/run/docker.sock -> discovery.docker -> loki.source.docker -> Loki
```

Nguồn 2: file log của Java services host-run:

```text
lab-code/logs/tenant-demo.log -> loki.source.file -> Loki
lab-code/logs/audit-log-service.log -> loki.source.file -> Loki
lab-code/logs/file-service.log -> loki.source.file -> Loki
lab-code/logs/search-service.log -> loki.source.file -> Loki
```

Các label chính:

| Label | Ý nghĩa |
|---|---|
| `service` | Docker Compose service hoặc label `logging.service` nếu có |
| `container` | Tên container |
| `compose_project` | Docker Compose project |
| `environment` | `local` |
| `source` | `docker` |

Với file log Java services, label chính là:

| Label | Giá trị |
|---|---|
| `service` | `tenant-demo`, `audit-log-service`, `file-service` hoặc `search-service` |
| `environment` | `local` |
| `source` | `file` |
| `job` | `host-file` |

Không dùng `requestId`, `tenantId`, `userId`, token, event id hoặc object key làm label vì dễ tạo high-cardinality. `requestId` nằm trong nội dung log để search text.

## Chạy Java services để Loki thấy log

Nếu chạy Java service bằng target thường:

```bash
make -f Makefile.legacy app-run
make -f Makefile.legacy audit-log-run
```

log vẫn hiện ở terminal host, nhưng chưa có file để Alloy đọc.

Khi demo centralized logs, dùng file-log targets:

```bash
make -f Makefile.legacy app-run-logs
make -f Makefile.legacy audit-log-run-logs
make -f Makefile.legacy file-run-logs
make -f Makefile.legacy search-run-logs
```

Các target này xóa file log cũ ở đầu lần chạy và set:

```text
LOGGING_FILE_NAME=../logs/tenant-demo.log
LOGGING_FILE_NAME=../logs/audit-log-service.log
LOGGING_FILE_NAME=../logs/file-service.log
LOGGING_FILE_NAME=../logs/search-service.log
```

File `lab-code/logs/*.log` là generated local log, không commit. Logs không bị tự xóa khi dừng service để sau demo vẫn có thể inspect. Dọn thủ công bằng:

```bash
make logs-list
make clean-logs
```

`gateway-demo` nếu chạy host Maven vẫn chưa có file-log collector riêng; Kong container logs thì được collect qua Docker source.

## Verification flow

1. Start một vài container có log:

```bash
cd lab-code
make -f Makefile.legacy web-ui-up
make -f Makefile.legacy kafka-up
```

2. Start Loki lab:

```bash
make -f Makefile.legacy loki-up
make -f Makefile.legacy loki-status
curl -f http://localhost:3100/ready
```

3. Mở Grafana:

```text
http://localhost:13001
```

4. Vào `Explore`, chọn datasource `Loki`.

5. Query theo service/container:

```logql
{service="web-ui-demo"}
{container="viettel-web-ui-demo"}
{service="kafka"}
{service="tenant-demo"}
{service="audit-log-service"}
{service="file-service"}
{service="search-service"}
```

6. Tìm request id nếu log line có request id:

```logql
{service="web-ui-demo"} |= "web-demo"
{service=~"tenant-demo|audit-log-service|file-service|search-service|kong-gateway"} |= "requestId="
{service=~"tenant-demo|audit-log-service|file-service|search-service|kong-gateway"} |= "UI-LOKI-E2E"
{service=~"tenant-demo|audit-log-service|file-service|search-service|kong-gateway"} |= "LOKI-WATCH"
{service="tenant-demo"} |= "409"
{service=~"tenant-demo|kong-gateway"} |= "403"
```

Với Java host-run services, query chỉ có log nếu service được chạy bằng target file-log tương ứng: `make -f Makefile.legacy app-run-logs`, `make -f Makefile.legacy audit-log-run-logs`, `make -f Makefile.legacy file-run-logs`, hoặc `make -f Makefile.legacy search-run-logs`.

Nếu muốn đọc log theo đúng cách demo backend engineer, xem:

- `docs/07-architecture/log-aggregation-loki/how-to-read-logs-in-grafana.md`
- `docs/07-architecture/log-aggregation-loki/grafana-loki-ui-screenshot-guide.md`

## Cleanup

```bash
cd lab-code
make -f Makefile.legacy loki-down
```

Loki/Grafana dùng Docker named volumes. Log history local còn tồn tại khi container restart, miễn là volume chưa bị xóa.

## Out of scope

- Distributed Loki.
- Object storage backend cho Loki.
- Tracing/Tempo/OpenTelemetry.
- Alertmanager/alert rules.
- ELK.
- Kubernetes.
- Production access control, retention policy, tenant log isolation.

Docs nền:

- `docs/07-architecture/log-aggregation-loki/loki-foundation.md`
- `docs/07-architecture/log-aggregation-loki/loki-local-lab-config-walkthrough.md`
