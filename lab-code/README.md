# Lab Code — Master Data Portal local demo

Thư mục này chứa code và Docker lab phục vụ Phase 1 / Phase 1.5. Mục tiêu là học backend architecture bằng hệ thống nhỏ chạy được thật, không phải dựng production platform hoàn chỉnh.

## Final demo workflow

Workflow chính nằm trong `lab-code/Makefile` và cố tình ngắn:

```bash
cd lab-code
make help
make info
make up
make status
make down
make clean-logs
```

`make up` bật Docker infra/tooling/web UI, sau đó chạy bốn Java backend service bằng Maven trên host ở background.

## Runtime hiện tại

```text
React Web UI / Master Data Portal
-> Keycloak login
-> Kong Gateway
-> tenant-demo
-> audit-log-service
-> file-service
-> search-service
-> PostgreSQL / Redis / Kafka / MinIO / Elasticsearch / Loki/Grafana/Alloy
```

## Chạy bằng Maven/IntelliJ hay Docker?

### Maven/IntelliJ-first

Các Java backend service chạy trên host qua Maven/IntelliJ:

- `tenant-demo`
- `audit-log-service`
- `file-service`
- `search-service`

Khi chạy bằng `make up`, các service này ghi log file vào:

```text
lab-code/logs/tenant-demo.log
lab-code/logs/audit-log-service.log
lab-code/logs/file-service.log
lab-code/logs/search-service.log
```

Alloy tail các file log đó để gửi vào Loki. Logs không tự xóa khi stop demo để còn inspect sau khi chạy; dọn thủ công bằng `make clean-logs`.

### Docker-first

Docker vẫn dùng cho infra/tooling/web UI:

- PostgreSQL
- Keycloak
- Redis
- Kafka
- Kafka UI
- MinIO
- Elasticsearch
- Kong
- Loki/Grafana/Alloy
- React Web UI container

## Thư mục runtime chính

| Thư mục | Vai trò |
|---|---|
| `common-security/` | Shared Maven module cho `TenantContext`, JWT tenant filter và Keycloak role converter. Đây không phải runtime service. |
| `tenant-demo/` | Master Data service: CRUD danh mục, tenant-aware PostgreSQL/Flyway/JPA, Redis read path, Kafka producer. |
| `audit-log-service/` | Kafka consumer lưu activity/audit event tenant-aware và expose `/api/audit-events`. |
| `file-service/` | Upload/download/list/delete file tenant-aware; metadata ở PostgreSQL, binary object ở MinIO. |
| `search-service/` | Kafka consumer cập nhật Elasticsearch projection; expose `/api/search/master-data` và admin-only reindex. |
| `web-ui-demo/` | React Web UI `Master Data Portal`, gọi API qua Kong sau Keycloak login. |
| `keycloak-lab/` | Keycloak local, bootstrap realm/client/users/roles/theme. |
| `kong-gateway-lab/` | Kong DB-less declarative config cho final API routes. |
| `loki-lab/` | Loki/Grafana/Alloy log aggregation local. |

## Infra/tooling và legacy learning labs

| Thư mục | Phân loại | Ghi chú |
|---|---|---|
| `docker/` | Final runtime dependency | PostgreSQL compose dùng bởi `make up`. |
| `redis-lab/` | Final runtime dependency + learning lab | Redis cache-aside local. |
| `kafka-lab/` | Final runtime dependency + learning lab | Kafka broker local. |
| `kafka-ui-lab/` | Final runtime tooling | Inspect topic/message/consumer group. |
| `minio-lab/` | Final runtime dependency + learning lab | MinIO object storage local. |
| `elasticsearch-lab/` | Final runtime dependency + learning lab | Elasticsearch local cho `search-service`. |
| `observability-lab/` | Legacy/current metrics lab | Prometheus/Grafana metrics khác với Loki log aggregation. |
| `flyway-failure-lab/` | Legacy learning lab | Học Flyway failure/repair/rollback behavior. |
| `gateway-demo/` | Legacy learning lab | Spring Cloud Gateway concept; final demo dùng Kong. |
| `sql-playground/` | Legacy/current foundation lab | SQL scripts học PostgreSQL multi-tenant, index, locking, isolation. |

Các target mini-lab lịch sử được giữ trong:

```text
lab-code/Makefile.legacy
```

Ví dụ:

```bash
make -f Makefile.legacy kafka-up
make -f Makefile.legacy kafka-ui-up
make -f Makefile.legacy app-run-logs
make -f Makefile.legacy search-run-logs
make -f Makefile.legacy help
```

## Makefile workflow

Main workflow:

```bash
make up          # start full local demo
make status      # show Docker services, Java PIDs and logs
make down        # stop demo, keep volumes and logs
make clean-logs  # delete generated logs/*.log, keep logs/.gitkeep
make info        # print URLs and runtime model
```

Backward-compatible aliases vẫn tồn tại:

```bash
make demo-up
make demo-status
make demo-down
make logs-clean
```

Nhưng tài liệu final nên ưu tiên `make up/status/down/clean-logs`.

## HTTP Client workflow

Các `.http` public dùng biến chung như:

```text
kong_base_url
tenant1_token
tenant2_token
admin_token
```

Xem:

- [README-http-client.md](README-http-client.md)
- [http-client.env.example.json](http-client.env.example.json)
- `keycloak-lab/http/keycloak-token-flow.http`

Không commit local IntelliJ env thật hoặc token thật.

## Tài liệu demo liên quan

Khi cần rehearsal hoặc demo trước mentor, đọc:


## Nguyên tắc học code

1. Tự đọc docs và tự chạy lab trước.
2. Dùng `.http`, logs, Kafka UI, Loki/Grafana để hiểu request/event/log flow.
3. Khi sửa code, verify bằng Maven/build/HTTP smoke trước khi commit.
4. Không biến repo này thành production platform: chưa có outbox, retry/DLT, schema registry, Kubernetes/service discovery production, HA/secrets/TLS production.

## Local artifacts không commit

Các file/thư mục sau là generated/local-only:

- `logs/*.log`
- `.pids/`
- `target/`
- `dist/`
- `node_modules/`
- `.env`
- `http-client.env.json`
- `http-client.private.env.json`

Nếu cần dọn log:

```bash
make clean-logs
```
