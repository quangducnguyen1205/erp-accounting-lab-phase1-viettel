# Kong Gateway lab

Trạng thái: planning stub cho Phase 1.5.

Mini-lab này sẽ thêm Kong Gateway theo hướng DB-less/declarative config để luyện gateway platform gần target architecture hơn.

Spring Cloud Gateway lab hiện có vẫn được giữ. Kong lab không xóa hoặc thay thế bắt buộc Spring Cloud Gateway lab.

## Mục tiêu khi implement

- Docker-first.
- Kong DB-less, declarative config trong repo.
- Route `/api/master-data/**` tới `tenant-demo` hoặc future `master-data-service`.
- Sau khi có `audit-log-service`, thêm route `/api/audit/**`.
- Preserve `Authorization` và `X-Request-Id`.
- Không đưa business logic vào Gateway.

## Chưa làm trong stub này

- Chưa có `docker-compose.yml`.
- Chưa có declarative `kong.yml`.
- Chưa có Makefile targets.

Doc nền: `docs/07-architecture/kong-gateway/`.
