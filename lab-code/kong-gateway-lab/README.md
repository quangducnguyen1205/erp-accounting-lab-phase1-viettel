# Kong Gateway lab

Mini-lab này chạy Kong Gateway local theo hướng DB-less/declarative config để luyện gateway platform gần target architecture hơn.

Spring Cloud Gateway lab hiện có vẫn được giữ. Kong lab không xóa hoặc thay thế bắt buộc Spring Cloud Gateway lab.

## Chạy nhanh

Từ `lab-code/`:

```bash
make kong-up
make kong-status
```

URLs:

```text
Kong proxy:     http://localhost:18000
Kong Admin API: http://localhost:18001
```

Admin API chỉ bind local trong Docker Compose:

```text
127.0.0.1:18001:8001
```

Không expose Admin API kiểu này ra môi trường thật.

## Điều kiện để route chạy

Lab này giả định `tenant-demo` đang chạy trên host ở port `8080`:

```bash
cd lab-code
make db-up
make app-run
```

Kong chạy trong container, nên upstream trong `kong.yml` dùng:

```text
http://host.docker.internal:8080
```

Không dùng `localhost:8080` bên trong container Kong, vì `localhost` lúc đó là chính container Kong.

## Route hiện có

| Client gọi Kong | Kong route tới | Ghi chú |
|---|---|---|
| `GET /tenant-demo/actuator/health` | `tenant-demo /actuator/health` | health route để verify nhanh |
| `/api/master-data...` | `tenant-demo /api/master-data...` | giữ nguyên path, preserve `Authorization` và `X-Request-Id` |

Kong không validate JWT trong mini-lab này. Backend `tenant-demo` vẫn validate token, map role, set tenant context và query tenant-aware.

## Verify nhanh

```bash
curl -i http://localhost:18000/tenant-demo/actuator/health
curl -i http://localhost:18000/api/master-data
```

Kỳ vọng:

- health route trả health response nếu `tenant-demo` đang chạy;
- `/api/master-data` thiếu token trả `401` từ backend;
- có token hợp lệ thì backend xử lý như khi gọi trực tiếp.

## Dừng lab

```bash
make kong-down
```

## Docs nên đọc

- `docs/07-architecture/kong-gateway/kong-gateway-foundation.md`
- `docs/07-architecture/kong-gateway/kong-local-lab-config-walkthrough.md`

## Caveats

- Đây là local lab, không phải production API management.
- Kong Admin API không được public trong production.
- Chưa làm Kong JWT/OIDC plugin, rate limit, consumer/plugin matrix hoặc mTLS.
- Linux Docker cũ có thể cần cấu hình thêm nếu `host.docker.internal` không resolve được; compose đã thêm `extra_hosts: host-gateway` cho trường hợp Docker hỗ trợ.
