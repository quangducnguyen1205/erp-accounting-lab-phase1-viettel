# Redis local mini-lab

## Mục tiêu

Folder này chạy Redis local để học cache-aside và tenant-safe cache key. Đây là lab nhỏ cho Phase 1, không phải production Redis deployment.

Đọc trước:

- `../../docs/07-architecture/redis-cache-strategy.md`
- `../../docs/07-architecture/redis-code-guide-spring-boot.md`
- `../../docs/07-architecture/redis-mini-lab-plan.md`

## Local service

| Service | URL/Port |
|---|---|
| Redis | `localhost:16379` -> container `6379` |

Port `16379` được dùng để tránh đụng Redis local khác trên máy.

## Start/stop

Từ `lab-code/`:

```bash
make redis-up
make redis-status
make redis-down
```

Hoặc chạy trực tiếp:

```bash
cd lab-code/redis-lab
docker compose up -d
docker compose down
```

## Kiểm tra Redis sống

```bash
docker exec -it viettel-redis redis-cli PING
```

Expected:

```text
PONG
```

## Redis CLI cho mini-lab

```bash
docker exec -it viettel-redis redis-cli
```

Các lệnh hữu ích:

```text
PING
KEYS tenant:*
TTL tenant:1:master-data:code:LAPTOP-01
GET tenant:1:master-data:code:LAPTOP-01
DEL tenant:1:master-data:code:LAPTOP-01
FLUSHDB
```

Ghi chú: `KEYS` và `FLUSHDB` chỉ dùng trong local lab nhỏ. Không dùng tùy tiện trên production.

## Config dự kiến cho tenant-demo

Khi tự code Redis cache sau này, `.env` local có thể dùng:

```env
APP_CACHE_ENABLED=true
APP_CACHE_MASTER_DATA_TTL_SECONDS=300
REDIS_HOST=localhost
REDIS_PORT=16379
```

Default trong app nên là `APP_CACHE_ENABLED=false` để `make app-test` không phụ thuộc Redis.

## Mini-lab flow dự kiến

1. Start PostgreSQL + Redis.
2. Start tenant-demo khi cache đã tự implement.
3. Gọi API lookup `master_data` lần 1 -> cache miss.
4. Gọi lại cùng tenant/key -> cache hit.
5. Gọi tenant khác cùng code/id -> key khác, không dùng cache tenant 1.
6. Kiểm tra key bằng Redis CLI.

HTTP Client file:

- `../tenant-demo/http/cache-api.http`

## Cleanup

```bash
make redis-down
```

Compose hiện không mount volume, nên dữ liệu Redis là ephemeral cho lab. Nếu cần reset nhanh khi container đang chạy:

```bash
docker exec -it viettel-redis redis-cli FLUSHDB
```

## Out of scope hiện tại

- Redis cluster/sentinel;
- persistence tuning;
- distributed lock;
- Redis Streams;
- pub/sub;
- session storage;
- cache stampede mitigation nâng cao;
- production memory sizing/eviction tuning.
