# Loki local lab config walkthrough

## 1. Big Picture

Mini-lab này gom log từ Docker container vào một nơi để không phải mở nhiều terminal khi demo nhiều service.

Flow hiện tại có hai nguồn log:

```text
Docker container stdout logs
-> Docker daemon
-> Grafana Alloy
-> Loki
-> Grafana Explore

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

Ý nghĩa từng chặng:

- Container ghi log ra stdout/stderr.
- Docker daemon giữ log container.
- Alloy đọc Docker logs qua Docker socket.
- Alloy gắn label như `service`, `container`, `compose_project`.
- Alloy gửi log sang Loki.
- Grafana dùng Loki datasource để search log trong Explore.

Giới hạn quan trọng:

- Lab này collect Docker container stdout logs.
- Lab này cũng tail file `lab-code/logs/tenant-demo.log`, `lab-code/logs/audit-log-service.log`, `lab-code/logs/file-service.log` và `lab-code/logs/search-service.log` để demo Java service host-run logs trong Loki.
- `tenant-demo` chỉ ghi file đó khi chạy bằng `make app-run-logs`.
- `audit-log-service` chỉ ghi file đó khi chạy bằng `make audit-log-run-logs`.
- `file-service` chỉ ghi file đó khi chạy bằng `make file-run-logs`.
- `search-service` chỉ ghi file đó khi chạy bằng `make search-run-logs`.
- `gateway-demo` nếu chạy bằng `make gateway-run` thì log vẫn nằm ở host terminal; Kong container logs vẫn được collect qua Docker source.
- `requestId` nên nằm trong log message để search text, không dùng làm Loki label.

## 2. File Map

| File | Điều khiển gì? | Khi nào sửa? | Không nên đặt gì vào đây? |
|---|---|---|---|
| `lab-code/loki-lab/docker-compose.yml` | Chạy Loki, Grafana, Alloy local bằng Docker. | Khi đổi image, port, volume, service lab. | Secret thật, production credential, cấu hình cloud/private IP. |
| `lab-code/loki-lab/loki-config.yml` | Cấu hình Loki single-node local. | Khi học storage/schema/retention local. | Production retention/security config nếu chưa hiểu. |
| `lab-code/loki-lab/alloy/config.alloy` | Pipeline collect Docker logs, tail Java service file logs và forward sang Loki. | Khi đổi log source, label, collector pipeline. | Label high-cardinality như requestId/userId/tenantId/token. |
| `lab-code/loki-lab/grafana/provisioning/datasources/loki.yml` | Tự add Loki datasource vào Grafana. | Khi đổi tên datasource hoặc URL Loki nội bộ. | Password/secret datasource thật. |
| `lab-code/loki-lab/README.md` | Lệnh chạy, URL, query mẫu, cleanup. | Khi đổi workflow/port/Makefile target. | Lý thuyết quá dài hoặc log/token thật. |
| `lab-code/Makefile` | Target `make loki-*`, `make app-run-logs`, `make audit-log-run-logs`, `make file-run-logs`, `make search-run-logs`, `make logs-clean`. | Khi thêm/sửa command chạy lab. | Lệnh destructive hoặc phụ thuộc tool local ngoài Docker. |

## 3. `docker-compose.yml` Walkthrough

File: `lab-code/loki-lab/docker-compose.yml`

### `loki` service

```yaml
loki:
  image: grafana/loki:3.3.2
  container_name: viettel-loki
  command: -config.file=/etc/loki/loki-config.yml
  ports:
    - "3100:3100"
  volumes:
    - ./loki-config.yml:/etc/loki/loki-config.yml:ro
    - loki-data:/loki
```

Vai trò:

- Loki là nơi nhận và lưu log.
- Loki không tự đọc log Docker. Nó chỉ nhận log từ collector như Alloy.
- Port `3100` là HTTP API của Loki.
- Host gọi được `http://localhost:3100/ready`.
- Grafana/Alloy trong Docker network gọi Loki bằng tên service `http://loki:3100`.

Volume:

- `./loki-config.yml` mount vào container ở chế độ read-only để Loki đọc config.
- `loki-data:/loki` là named volume lưu log/chunks/index local.

Label:

```yaml
labels:
  logging.service: loki
  logging.environment: local
```

Label này giúp Alloy override service label thành `loki`, dễ query trong Loki.

### `alloy` service

```yaml
alloy:
  image: grafana/alloy:v1.5.1
  container_name: viettel-alloy
  command:
    - run
    - --server.http.listen-addr=0.0.0.0:12345
    - --storage.path=/var/lib/alloy/data
    - /etc/alloy/config.alloy
  ports:
    - "12345:12345"
  volumes:
    - ./alloy/config.alloy:/etc/alloy/config.alloy:ro
    - /var/run/docker.sock:/var/run/docker.sock:ro
    - ../logs:/var/log/viettel-lab:ro
    - alloy-data:/var/lib/alloy/data
  depends_on:
    - loki
```

Vai trò:

- Alloy là collector.
- Alloy đọc Docker container logs.
- Alloy forward log sang Loki.

Port `12345`:

- Đây là Alloy HTTP/debug UI local.
- Dùng để xem Alloy có chạy không, không phải nơi query log chính.

Mount Docker socket:

```yaml
- /var/run/docker.sock:/var/run/docker.sock:ro
```

Vì Alloy cần hỏi Docker daemon container nào đang chạy và đọc log container.

Security caveat:

- Docker socket rất quyền lực. Dù mount read-only, container có thể thấy metadata/log của Docker host.
- Local lab chấp nhận được để học.
- Production cần cân nhắc collector deployment, quyền, network boundary và hardening kỹ hơn.

Mount file log directory:

```yaml
- ../logs:/var/log/viettel-lab:ro
```

Vì `tenant-demo`, `audit-log-service` và `file-service` thường chạy bằng Maven/IntelliJ trên host, log không nằm trong Docker stdout. Mount này cho phép Alloy đọc:

- `lab-code/logs/tenant-demo.log` trong container ở path `/var/log/viettel-lab/tenant-demo.log`;
- `lab-code/logs/audit-log-service.log` trong container ở path `/var/log/viettel-lab/audit-log-service.log`;
- `lab-code/logs/file-service.log` trong container ở path `/var/log/viettel-lab/file-service.log`.

Điểm cần nhớ:

- Đây chỉ là file-log bridge cho local demo.
- Generated `*.log` không được commit.
- File source không biến requestId thành label; requestId vẫn nằm trong nội dung log.

### `grafana` service

```yaml
grafana:
  image: grafana/grafana:11.3.0
  container_name: viettel-loki-grafana
  ports:
    - "13001:3000"
  environment:
    GF_SECURITY_ADMIN_USER: admin
    GF_SECURITY_ADMIN_PASSWORD: admin
    GF_USERS_ALLOW_SIGN_UP: "false"
  volumes:
    - grafana-loki-data:/var/lib/grafana
    - ./grafana/provisioning:/etc/grafana/provisioning:ro
  depends_on:
    - loki
```

Vai trò:

- Grafana là UI để query log trong Explore.
- Grafana không collect logs trực tiếp.
- Grafana đọc Loki qua datasource được provision.

Vì sao port `13001`?

- Repo đã có Prometheus/Grafana metrics lab dùng Grafana ở `13000`.
- Log lab dùng `13001` để tránh conflict.

Browser và Docker network:

- Browser mở `http://localhost:13001`.
- Grafana container tự gọi Loki bằng URL nội bộ `http://loki:3100`.
- Browser không cần gọi Loki trực tiếp để query log qua Explore.

## 4. `loki-config.yml` Walkthrough

File: `lab-code/loki-lab/loki-config.yml`

### `auth_enabled: false`

```yaml
auth_enabled: false
```

Tắt auth nội bộ của Loki cho local lab.

Ý nghĩa:

- Dễ chạy local.
- Không đại diện cho production.
- Production cần auth/network policy/gateway/mTLS hoặc cơ chế kiểm soát truy cập phù hợp.

### `server`

```yaml
server:
  http_listen_port: 3100
  grpc_listen_port: 9096
```

- `http_listen_port`: HTTP API của Loki.
- `grpc_listen_port`: cổng gRPC nội bộ Loki dùng cho một số component.

Trong lab, mình chủ yếu dùng HTTP API:

```bash
curl -f http://localhost:3100/ready
```

### `common`

```yaml
common:
  instance_addr: 127.0.0.1
  path_prefix: /loki
  storage:
    filesystem:
      chunks_directory: /loki/chunks
      rules_directory: /loki/rules
  replication_factor: 1
  ring:
    kvstore:
      store: inmemory
```

Ý nghĩa:

- `path_prefix`: thư mục gốc Loki dùng trong container.
- `chunks_directory`: nơi lưu log chunks.
- `rules_directory`: nơi lưu rules local nếu dùng.
- `replication_factor: 1`: local single-node, không nhân bản.
- `ring.kvstore.store: inmemory`: metadata ring nằm trong memory, đủ cho lab local.

Loki stores:

- Log content theo chunks.
- Index chủ yếu dựa trên labels.

Vì vậy label design quan trọng hơn việc nhét mọi thứ thành label.

### `query_range`

```yaml
query_range:
  results_cache:
    cache:
      embedded_cache:
        enabled: true
        max_size_mb: 100
```

Cache kết quả query range trong Loki process.

Trong lab:

- Giúp query lặp lại nhanh hơn.
- Không phải nội dung chính cần đào sâu.

### `schema_config`

```yaml
schema_config:
  configs:
    - from: 2024-04-01
      store: tsdb
      object_store: filesystem
      schema: v13
      index:
        prefix: index_
        period: 24h
```

Đây là cấu hình schema/index storage của Loki.

Giải thích beginner-friendly:

- `store: tsdb`: dùng TSDB index store hiện đại hơn cho Loki local.
- `object_store: filesystem`: lưu data trên local filesystem, không dùng S3/MinIO.
- `schema: v13`: version schema Loki dùng.
- `index.period: 24h`: index được chia theo ngày.

Trong production:

- Có thể cần object storage backend.
- Cần retention, compaction, backup, sizing.
- Không nên copy nguyên lab config sang production.

### `ruler`

```yaml
ruler:
  alertmanager_url: http://localhost:9093
```

Ruler dùng cho alerting/rules nếu cấu hình.

Trong lab này:

- Alerting chưa dùng.
- Alertmanager nằm ngoài scope.
- Dòng này không làm lab trở thành alerting stack.

## 5. `alloy/config.alloy` Walkthrough

File: `lab-code/loki-lab/alloy/config.alloy`

Alloy config là pipeline. Đọc từ trên xuống sẽ thấy:

```text
discovery.docker
-> discovery.relabel
-> loki.source.docker
-> loki.write

local.file_match
-> loki.source.file
-> loki.write
```

### `discovery.docker "containers"`

```alloy
discovery.docker "containers" {
  host             = "unix:///var/run/docker.sock"
  refresh_interval = "5s"
}
```

Ý nghĩa:

- Kết nối Docker daemon qua socket.
- Phát hiện container đang chạy.
- `refresh_interval = "5s"` giúp lab cập nhật nhanh khi bật/tắt container.

Output của block này là danh sách targets có metadata Docker, ví dụ:

- container name;
- compose project;
- compose service;
- Docker labels.

### `discovery.relabel "docker_logs"`

```alloy
discovery.relabel "docker_logs" {
  targets = discovery.docker.containers.targets
  ...
}
```

Block này lấy metadata Docker và biến thành label Loki dễ đọc hơn.

#### Container name

```alloy
rule {
  source_labels = ["__meta_docker_container_name"]
  regex         = "/(.*)"
  target_label  = "container"
  replacement   = "$1"
}
```

Docker container name thường có dạng `/viettel-web-ui-demo`.

Rule này:

- bỏ dấu `/` đầu;
- tạo label `container="viettel-web-ui-demo"`.

#### Compose project

```alloy
rule {
  source_labels = ["__meta_docker_container_label_com_docker_compose_project"]
  target_label  = "compose_project"
}
```

Tạo label như:

```text
compose_project="web-ui-demo"
```

Hữu ích khi nhiều compose project chạy song song.

#### Compose service

```alloy
rule {
  source_labels = ["__meta_docker_container_label_com_docker_compose_service"]
  target_label  = "service"
}
```

Mặc định service label lấy từ Docker Compose service name.

Ví dụ:

```text
service="web-ui-demo"
```

#### Custom `logging.service`

```alloy
rule {
  source_labels = ["__meta_docker_container_label_logging_service"]
  regex         = "(.+)"
  target_label  = "service"
  replacement   = "$1"
}
```

Repo thêm Docker label `logging.service` cho một số container để override service name khi cần.

Ví dụ:

```yaml
labels:
  logging.service: web-ui-demo
```

Lợi ích:

- Service name ổn định hơn khi Compose naming thay đổi.
- Query dễ nhớ hơn.

#### Environment và job

```alloy
rule {
  target_label = "environment"
  replacement  = "local"
}

rule {
  target_label = "job"
  replacement  = "docker"
}
```

Hai label này giúp phân biệt:

- log local hay environment khác;
- source pipeline là Docker.

### `loki.source.docker "containers"`

```alloy
loki.source.docker "containers" {
  host             = "unix:///var/run/docker.sock"
  targets          = discovery.relabel.docker_logs.output
  labels           = {"source" = "docker"}
  refresh_interval = "5s"
  forward_to       = [loki.write.local.receiver]
}
```

Vai trò:

- Đọc stdout/stderr logs từ Docker containers.
- Dùng targets đã relabel ở block trước.
- Thêm label tĩnh `source="docker"`.
- Forward log sang `loki.write.local`.

Nói cách khác, đây là đoạn biến Docker logs thành Loki streams.

### `local.file_match "java_service_host_logs"`

```alloy
local.file_match "java_service_host_logs" {
  path_targets = [
    {
      __path__    = "/var/log/viettel-lab/tenant-demo.log",
      service     = "tenant-demo",
      environment = "local",
      source      = "file",
      job         = "host-file",
    },
    {
      __path__    = "/var/log/viettel-lab/audit-log-service.log",
      service     = "audit-log-service",
      environment = "local",
      source      = "file",
      job         = "host-file",
    },
    {
      __path__    = "/var/log/viettel-lab/file-service.log",
      service     = "file-service",
      environment = "local",
      source      = "file",
      job         = "host-file",
    },
    {
      __path__    = "/var/log/viettel-lab/search-service.log",
      service     = "search-service",
      environment = "local",
      source      = "file",
      job         = "host-file",
    },
  ]
  sync_period = "5s"
}
```

Vai trò:

- Chỉ ra file log nào Alloy cần tail.
- Gắn label ổn định cho stream log file.
- `sync_period = "5s"` giúp Alloy phát hiện file nhanh trong demo.

Repo-specific mapping:

- Host path `lab-code/logs/tenant-demo.log` được mount vào Alloy thành `/var/log/viettel-lab/tenant-demo.log`.
- Host path `lab-code/logs/audit-log-service.log` được mount vào Alloy thành `/var/log/viettel-lab/audit-log-service.log`.
- Host path `lab-code/logs/file-service.log` được mount vào Alloy thành `/var/log/viettel-lab/file-service.log`.
- Host path `lab-code/logs/search-service.log` được mount vào Alloy thành `/var/log/viettel-lab/search-service.log`.
- Service label là `tenant-demo`, `audit-log-service`, `file-service` hoặc `search-service` để query cùng tên với backend service.
- `source="file"` giúp phân biệt với Docker stdout logs.

Không làm:

- Không label theo `requestId`, `tenantId`, `userId`, code hoặc token.
- Không collect mọi file trong repo.

### `loki.source.file "java_service_host_logs"`

```alloy
loki.source.file "java_service_host_logs" {
  targets    = local.file_match.java_service_host_logs.targets
  forward_to = [loki.write.local.receiver]
}
```

Vai trò:

- Tail file targets từ `local.file_match`.
- Forward từng log line sang `loki.write.local`.

Nói cách khác, đây là đoạn biến file `tenant-demo.log`, `audit-log-service.log`, `file-service.log` và `search-service.log` thành Loki streams.

### `loki.write "local"`

```alloy
loki.write "local" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
  }
}
```

Vai trò:

- Nhận log từ `loki.source.docker` và `loki.source.file`.
- Push log sang Loki.

Vì sao URL là `http://loki:3100`?

- Đây là DNS service name trong Docker Compose network.
- `localhost:3100` là URL từ host/browser.
- Container Alloy không nên gọi `localhost:3100` vì `localhost` bên trong Alloy là chính container Alloy.

## 6. Label Design

Nên làm Loki labels:

- `service`
- `container`
- `compose_project`
- `environment`
- `job`
- `source`

Không nên làm Loki labels:

- `requestId`
- `tenantId`
- `userId`
- JWT subject
- token
- email
- object key
- raw URL/path nếu quá nhiều giá trị
- eventId

Vì sao?

- Loki tối ưu query bằng labels.
- Labels có quá nhiều giá trị khác nhau sẽ tạo high-cardinality.
- High-cardinality làm Loki nặng, tốn bộ nhớ, khó vận hành.

Trong repo này:

- `requestId` nằm trong log message.
- Query bằng text filter:

```logql
{service="tenant-demo"} |= "requestId=demo-001"
```

Query này dùng được khi chạy `tenant-demo` bằng `make app-run-logs`.
Với `audit-log-service`, dùng `make audit-log-run-logs` rồi query tương tự theo `service="audit-log-service"`.

## 7. Grafana Datasource Provisioning

File: `lab-code/loki-lab/grafana/provisioning/datasources/loki.yml`

```yaml
apiVersion: 1

datasources:
  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    isDefault: true
    editable: true
```

Ý nghĩa:

- Khi Grafana start, nó tự tạo datasource tên `Loki`.
- `type: loki` nói Grafana dùng Loki plugin/datasource.
- `access: proxy` nghĩa là browser gửi query tới Grafana, Grafana server gọi Loki.
- `url: http://loki:3100` là URL nội bộ trong Docker network.
- `isDefault: true` giúp Explore mặc định chọn Loki.
- `editable: true` cho phép sửa datasource trong UI local lab.

Phân biệt URL:

| Ai gọi? | URL |
|---|---|
| Browser mở Grafana | `http://localhost:13001` |
| Host kiểm tra Loki | `http://localhost:3100/ready` |
| Grafana container gọi Loki | `http://loki:3100` |
| Alloy container push log | `http://loki:3100/loki/api/v1/push` |

## 8. Makefile Targets

File: `lab-code/Makefile`

Các target chính:

```bash
make loki-up
make loki-status
make loki-info
make loki-logs
make loki-down
make app-run-logs
make audit-log-run-logs
make file-run-logs
make logs-list
make logs-clean
```

Ý nghĩa:

- `loki-up`: chạy Docker Compose trong `lab-code/loki-lab`.
- `loki-status`: xem container Loki/Alloy/Grafana đang chạy không.
- `loki-info`: in URL và lưu ý nhanh.
- `loki-logs`: tail logs của chính Loki lab stack.
- `loki-down`: dừng/xóa container lab, giữ named volumes.
- `app-run-logs`: chạy `tenant-demo` host Maven với file logging `lab-code/logs/tenant-demo.log`, Keycloak mode và Kafka enabled mặc định.
- `audit-log-run-logs`: chạy `audit-log-service` host Maven với file logging `lab-code/logs/audit-log-service.log`.
- `file-run-logs`: chạy `file-service` host Maven với file logging `lab-code/logs/file-service.log`.
- `search-run-logs`: chạy `search-service` host Maven với file logging `lab-code/logs/search-service.log`.
- `logs-list`: xem file log local hiện có và kích thước.
- `logs-clean`: xóa generated `*.log`, giữ `logs/.gitkeep`.

Aliases:

```bash
make logs-up
make logs-down
```

Loki không tự nằm trong `infra-up` để full infra mặc định không quá nặng.

## 9. How To Verify

Từ repo:

```bash
cd lab-code
make loki-up
make loki-status
curl -f http://localhost:3100/ready
```

Mở Grafana:

```text
http://localhost:13001
```

Login local:

```text
admin / admin
```

Vào:

```text
Explore -> Datasource: Loki
```

Query ví dụ:

```logql
{service="web-ui-demo"}
{container=~".*web.*"}
{service="loki"}
{service="tenant-demo"}
{service="audit-log-service"}
{service="file-service"}
{service="search-service"}
{service="web-ui-demo"} |= "web-demo"
{service=~"tenant-demo|audit-log-service|file-service|search-service|kong-gateway"} |= "requestId="
{service=~"tenant-demo|audit-log-service|file-service|search-service|kong-gateway"} |= "UI-LOKI-E2E"
```

Nếu muốn có log từ `web-ui-demo`:

```bash
cd lab-code
make web-ui-up
curl -I http://localhost:5173
```

Sau đó quay lại Grafana Explore query:

```logql
{service="web-ui-demo"}
```

Nếu muốn có log từ `tenant-demo` host Maven:

```bash
cd lab-code
make app-run-logs
```

Nếu muốn có log từ `audit-log-service` host Maven:

```bash
cd lab-code
make audit-log-run-logs
```

Nếu muốn có log từ `file-service` host Maven:

```bash
cd lab-code
make file-run-logs
```

Nếu muốn có log từ `search-service` host Maven:

```bash
cd lab-code
make search-run-logs
```

Nếu query `tenant-demo`, `audit-log-service`, `file-service`, `search-service` hoặc `gateway-demo` chưa ra kết quả, kiểm tra cách chạy:

- `tenant-demo` chạy bằng `make app-run`: log chỉ ở terminal, không chắc có file để tail.
- `tenant-demo` chạy bằng `make app-run-logs`: log vào `lab-code/logs/tenant-demo.log` và được Alloy tail.
- `audit-log-service` chạy bằng `make audit-log-run`: log chỉ ở terminal.
- `audit-log-service` chạy bằng `make audit-log-run-logs`: log vào `lab-code/logs/audit-log-service.log` và được Alloy tail.
- `file-service` chạy bằng `make file-run`: log chỉ ở terminal.
- `file-service` chạy bằng `make file-run-logs`: log vào `lab-code/logs/file-service.log` và được Alloy tail.
- `search-service` chạy bằng `make search-run`: log chỉ ở terminal.
- `search-service` chạy bằng `make search-run-logs`: log vào `lab-code/logs/search-service.log` và được Alloy tail.
- Dockerized services: Alloy collect qua Docker stdout.
- `gateway-demo` Maven host chưa có file-log collector riêng; dùng Kong container logs cho gateway platform demo.

Cleanup:

```bash
cd lab-code
make loki-down
make web-ui-down
```

## 10. Common Mistakes

- Mong đợi Maven host logs tự xuất hiện trong Loki khi không chạy `make app-run-logs`.
- Mong đợi audit service host logs tự xuất hiện trong Loki khi không chạy `make audit-log-run-logs`.
- Nhầm `/actuator/metrics` với logs. Actuator/Micrometer là metrics, Loki là logs.
- Tưởng Grafana collect logs. Grafana chỉ query datasource; Alloy mới collect logs.
- Dùng Loki như database nghiệp vụ.
- Dùng `requestId`, `tenantId`, `userId`, token làm label.
- Log full Authorization header, access token, password, request body nhạy cảm.
- Mount Docker socket mà không hiểu rủi ro quyền.
- Dùng `localhost:3100` bên trong container Alloy/Grafana thay vì `http://loki:3100`.
- Quên rằng Prometheus/Grafana metrics lab và Loki/Grafana logs lab là hai lab khác nhau.

## 11. Production Caveats

Local lab chưa giải quyết:

- auth/access control cho Loki/Grafana;
- retention policy;
- object storage backend cho Loki;
- sizing/log volume/cost;
- PII/secrets masking;
- multi-tenant log isolation;
- alerting rules/Alertmanager;
- distributed Loki;
- secure collector deployment;
- Docker socket risk;
- audit/compliance policy.

Production thường cần thiết kế riêng:

- collector chạy ở đâu;
- logs nào được collect;
- label schema;
- retention bao lâu;
- ai được xem log tenant nào;
- cách mask token/password/email/PII;
- log query có được audit không.

Phase 1.5 chỉ cần hiểu flow và dùng Grafana Explore để debug local demo.
