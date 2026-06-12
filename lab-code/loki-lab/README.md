# Loki log aggregation lab

Trạng thái: local Docker lab đã có Loki + Grafana + Grafana Alloy.

Mini-lab này thêm centralized log checking cho nhiều container local:

```text
Docker container stdout logs
-> Grafana Alloy
-> Loki
-> Grafana Explore
```

Từ demo Phase 1.5, lab cũng collect `tenant-demo` host Maven logs nếu app chạy bằng target file-log:

```text
tenant-demo host Maven logs
-> lab-code/logs/tenant-demo.log
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
make loki-up
make loki-status
make loki-info
make loki-down
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

Nguồn 2: file log của `tenant-demo` khi chạy `make app-run-logs`:

```text
lab-code/logs/tenant-demo.log -> loki.source.file -> Loki
```

Các label chính:

| Label | Ý nghĩa |
|---|---|
| `service` | Docker Compose service hoặc label `logging.service` nếu có |
| `container` | Tên container |
| `compose_project` | Docker Compose project |
| `environment` | `local` |
| `source` | `docker` |

Với `tenant-demo` file log, label chính là:

| Label | Giá trị |
|---|---|
| `service` | `tenant-demo` |
| `environment` | `local` |
| `source` | `file` |
| `job` | `host-file` |

Không dùng `requestId`, `tenantId`, `userId`, token, event id hoặc object key làm label vì dễ tạo high-cardinality. `requestId` nằm trong nội dung log để search text.

## Chạy tenant-demo để Loki thấy log

Nếu chạy app bằng target thường:

```bash
make app-run
```

log vẫn hiện ở terminal host, nhưng chưa chắc có file để Alloy đọc.

Khi demo centralized logs, dùng:

```bash
make app-run-logs
```

Target này bật Keycloak + Kafka mode mặc định và set:

```text
LOGGING_FILE_NAME=../logs/tenant-demo.log
```

File `lab-code/logs/*.log` là generated local log, không commit. `gateway-demo` nếu chạy host Maven vẫn chưa có file-log collector riêng; Kong container logs thì được collect qua Docker source.

## Verification flow

1. Start một vài container có log:

```bash
cd lab-code
make web-ui-up
make kafka-up
```

2. Start Loki lab:

```bash
make loki-up
make loki-status
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
```

6. Tìm request id nếu log line có request id:

```logql
{service="web-ui-demo"} |= "web-demo"
{service=~"tenant-demo|audit-log-service|kong-gateway"} |= "requestId="
{service=~"tenant-demo|audit-log-service|kong-gateway"} |= "UI-LOKI-E2E"
```

Với `tenant-demo`, query chỉ có log nếu app được chạy bằng `make app-run-logs`.

## Cleanup

```bash
cd lab-code
make loki-down
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
