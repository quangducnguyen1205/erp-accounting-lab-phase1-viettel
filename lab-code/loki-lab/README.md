# Loki log aggregation lab

Trạng thái: local Docker lab đã có Loki + Grafana + Grafana Alloy.

Mini-lab này thêm centralized log checking cho nhiều container local:

```text
Docker container stdout logs
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

Alloy đọc Docker logs qua Docker socket:

```text
/var/run/docker.sock -> discovery.docker -> loki.source.docker -> Loki
```

Các label chính:

| Label | Ý nghĩa |
|---|---|
| `service` | Docker Compose service hoặc label `logging.service` nếu có |
| `container` | Tên container |
| `compose_project` | Docker Compose project |
| `environment` | `local` |
| `source` | `docker` |

Không dùng `requestId`, `tenantId`, `userId`, token, event id hoặc object key làm label vì dễ tạo high-cardinality. `requestId` nằm trong nội dung log để search text.

## Quan trọng: app chạy host terminal thì chưa được collect

Hiện `tenant-demo` và `gateway-demo` thường chạy bằng Maven trên host:

```bash
make app-run
make gateway-run
```

Log của hai lệnh này xuất hiện ở terminal host, không phải Docker stdout, nên Alloy Docker collector chưa đọc được. Có 3 hướng sau:

1. Dockerize service sau này để Alloy collect trực tiếp.
2. Thêm file-log collector sau này nếu muốn đọc log từ file host.
3. Trong Phase 1.5 hiện tại, dùng lab này để xem Dockerized services trước, ví dụ `web-ui-demo`, Keycloak, Kafka, Redis, Loki/Grafana/Alloy.

Không ép refactor Docker hóa backend trong task này để tránh làm phình scope.

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
```

6. Tìm request id nếu log line có request id:

```logql
{service="web-ui-demo"} |= "web-demo"
```

Với backend/gateway đang chạy host terminal, request log chỉ hiện trong terminal đó. Sau khi service được containerize hoặc thêm file-log collector, cùng requestId có thể search trong Grafana Explore.

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

Doc nền: `docs/07-architecture/log-aggregation-loki/`.
