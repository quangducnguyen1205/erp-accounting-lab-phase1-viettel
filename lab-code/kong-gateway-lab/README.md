# Kong Gateway lab

Mini-lab này chạy Kong Gateway local theo hướng DB-less/declarative config để luyện gateway platform gần target architecture hơn.

Spring Cloud Gateway lab hiện có vẫn được giữ. Kong lab không xóa hoặc thay thế bắt buộc Spring Cloud Gateway lab.

## Chạy nhanh

Từ `lab-code/`:

```bash
make -f Makefile.legacy kong-up
make -f Makefile.legacy kong-status
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

Lab này giả định các backend service đang chạy trên host:

```bash
cd lab-code
make -f Makefile.legacy db-up
make -f Makefile.legacy app-run
```

Kong chạy trong container, nên upstream trong `kong.yml` dùng:

```text
http://host.docker.internal:8080
```

Không dùng `localhost:8080` bên trong container Kong, vì `localhost` lúc đó là chính container Kong.

## Route hiện có

| Client gọi Kong | Kong route tới | Ghi chú |
|---|---|---|
| `GET /tenant-demo/actuator/health` | `tenant-demo :8080 /actuator/health` | health route để verify nhanh |
| `/api/master-data...` | `tenant-demo :8080 /api/master-data...` | giữ nguyên path, preserve `Authorization` và `X-Request-Id` |
| `/api/audit-events...` | `audit-log-service :8082 /api/audit-events...` | read-only audit API sau service split |
| `/api/files...` | `file-service :8083 /api/files...` | tenant-aware file upload/download/delete |
| `/api/search/master-data...` | `search-service :8084 /api/search/master-data...` | Elasticsearch search projection |

Trong mini-lab này, Kong không thực hiện validate JWT. Thay vào đó, mỗi backend service đứng sau Kong sẽ tự chịu trách nhiệm validate JWT và áp dụng authorization / tenant scope. Riêng `tenant-demo` đảm nhận thêm việc map role, thiết lập tenant context và thực hiện các truy vấn tenant-aware.

## Verify nhanh

```bash
curl -i http://localhost:18000/tenant-demo/actuator/health
curl -i http://localhost:18000/api/master-data
curl -i http://localhost:18000/api/audit-events
curl -i http://localhost:18000/api/files
curl -i http://localhost:18000/api/search/master-data
```

Kỳ vọng:

- health route trả health response nếu `tenant-demo` đang chạy;
- `/api/master-data` thiếu token trả `401` từ backend;
- `/api/audit-events` thiếu token trả `401` từ `audit-log-service` nếu service đang chạy;
- `/api/files` thiếu token trả `401` từ `file-service` nếu service đang chạy;
- `/api/search/master-data` thiếu token trả `401` từ `search-service` nếu service đang chạy;
- có token hợp lệ thì backend xử lý như khi gọi trực tiếp.

## Dừng lab

```bash
make -f Makefile.legacy kong-down
```

## Docs nên đọc

- `docs/07-architecture/kong-gateway/kong-gateway-foundation.md`
- `docs/07-architecture/kong-gateway/kong-local-lab-config-walkthrough.md`

## Giới hạn hiện tại

- Đây là local lab, không phải production API management.
- Kong Admin API không được public trong production.
- Chưa làm Kong JWT/OIDC plugin, rate limit, consumer/plugin matrix hoặc mTLS.
- Linux Docker cũ có thể cần cấu hình thêm nếu `host.docker.internal` không resolve được; compose đã thêm `extra_hosts: host-gateway` cho trường hợp Docker hỗ trợ.
