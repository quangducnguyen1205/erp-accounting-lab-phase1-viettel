# Gateway demo

## Mục tiêu

Folder này chạy một Spring Cloud Gateway nhỏ để học API Gateway ở mức Phase 1. Gateway chỉ route request đến `tenant-demo`; không thay thế auth, authorization hoặc tenant-aware query của backend.

Đọc trước:

- `../../docs/07-architecture/api-gateway-service-discovery/api-gateway-foundation.md`
- `../../docs/07-architecture/api-gateway-service-discovery/spring-cloud-gateway-code-guide.md`
- `../../docs/07-architecture/api-gateway-service-discovery/service-discovery-load-balancing-awareness.md`
- `../../docs/07-architecture/api-gateway-service-discovery/api-gateway-mini-lab-plan.md`

## Local ports

| App | URL |
|---|---|
| gateway-demo | `http://localhost:8081` |
| tenant-demo target | `http://localhost:8080` |
| React Web UI origin | `http://localhost:5173` |

## Run

Terminal 1:

```bash
cd lab-code
make -f Makefile.legacy db-up
make -f Makefile.legacy app-run
```

Terminal 2:

```bash
cd lab-code
make -f Makefile.legacy gateway-run
```

## Verify

```bash
curl http://localhost:8081/tenant-demo/actuator/health
```

Expected: health response từ `tenant-demo`.

Business API vẫn cần token:

```bash
curl -i http://localhost:8081/api/master-data
```

Expected: `401` từ backend.

## React Web UI / CORS

Gateway có CORS local cho origin mặc định:

```text
WEB_UI_ORIGIN=http://localhost:5173
```

Điều này chỉ để browser demo gọi `http://localhost:8081/api/**` từ Vite container/dev server. Backend `tenant-demo` vẫn validate JWT và enforce tenant-aware query.

## Caveat

- Không có service discovery thật.
- Không có load balancing nhiều backend instances.
- Không có gateway-level JWT validation.
- Không có rate limit/circuit breaker.
- Backend `tenant-demo` vẫn là security và tenant isolation boundary chính.
