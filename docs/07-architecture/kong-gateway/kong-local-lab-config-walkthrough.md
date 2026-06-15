# Kong Local Lab Config Walkthrough

Doc này giải thích cách đọc Kong DB-less lab trong repo. Theo chuẩn mới, phần đầu nói về model config Kong nói chung, sau đó mới map vào file cụ thể của repo.

## 1. General Kong Config Anatomy

Kong thường có hai mặt:

```text
Client
  -> Kong proxy port
  -> upstream backend service

Operator/developer
  -> Kong Admin API hoặc declarative config
  -> services/routes/plugins
```

Các khái niệm chính:

| Concept | Nghĩa chung |
|---|---|
| Gateway / reverse proxy | Nhận request từ client và forward tới backend. |
| Service | Object Kong đại diện cho upstream backend. |
| Route | Rule match request, ví dụ path `/api/master-data`. |
| Upstream target | URL backend thật mà Kong gọi. |
| Plugin | Hành vi bổ sung, ví dụ CORS, rate limit, request-id, auth. |
| Consumer | Caller identity ở Kong layer. Lab này chưa dùng. |
| DB-less mode | Kong không dùng DB, đọc config từ file. |
| Declarative config file | File YAML mô tả toàn bộ services/routes/plugins. |
| Admin API | API quản trị Kong. Chỉ dùng local trong lab. |
| Proxy port | Port client gọi API qua Kong. |
| Admin port | Port operator gọi Admin API. |

## 2. Generic DB-less File Structure

Một file Kong DB-less tối giản thường có dạng:

```yaml
_format_version: "3.0"
_transform: true

services:
  - name: example-service
    url: http://example-upstream
    routes:
      - name: example-route
        paths:
          - /example
        strip_path: false

plugins:
  - name: cors
    route: example-route
    config:
      origins:
        - http://localhost:5173
```

Ý nghĩa:

- `_format_version`: version format declarative config.
- `_transform`: cho Kong normalize một số field config.
- `services`: danh sách upstream service phía sau Kong.
- `service.url`: URL Kong sẽ forward tới.
- `routes`: điều kiện request vào match service đó.
- `paths`: path prefix hoặc exact-ish path để match.
- `strip_path`: có bỏ phần path match trước khi forward không.
- `plugins`: behavior gắn global/service/route.
- `route: example-route`: plugin chỉ áp dụng cho route có tên đó.

## 3. Repo-specific Mapping

### `lab-code/kong-gateway-lab/docker-compose.yml`

File này chạy Kong container local.

Quan trọng:

- `image: kong:3.7`: Kong OSS image cho lab.
- `KONG_DATABASE=off`: bật DB-less mode.
- `KONG_DECLARATIVE_CONFIG=/kong/declarative/kong.yml`: Kong đọc config từ file mount vào container.
- `KONG_PROXY_LISTEN=0.0.0.0:8000`: Kong nghe proxy trong container.
- `KONG_ADMIN_LISTEN=0.0.0.0:8001`: Admin API trong container.
- `ports: 18000:8000`: browser/curl trên host gọi Kong proxy qua `localhost:18000`.
- `ports: 127.0.0.1:18001:8001`: Admin API chỉ bind localhost host, không public.
- `volumes: ./kong.yml:/kong/declarative/kong.yml:ro`: config Git-tracked, mount read-only.
- `extra_hosts: host.docker.internal:host-gateway`: giúp Linux Docker mới resolve host app qua `host.docker.internal`.

Khi sửa:

- đổi port local nếu bị trùng;
- đổi image version nếu cần;
- không đặt secret production vào compose này.

### `lab-code/kong-gateway-lab/kong.yml`

File này là source of truth route/plugin của Kong local lab.

Hiện có:

- service `tenant-demo-api`;
- route `tenant-demo-master-data-api`;
- service `tenant-demo-health`;
- route `tenant-demo-health-route`;
- service `audit-log-api`;
- route `audit-log-events-api`;
- CORS plugin cho route master data và audit events.

Khi sửa:

- thêm route khác nếu có service boundary thật;
- chỉ expose actuator endpoint cụ thể, không route rộng `/actuator/**`;
- không đưa business logic vào Kong.

### `lab-code/kong-gateway-lab/README.md`

README giữ lệnh chạy và verify nhanh.

### `lab-code/Makefile`

Targets:

```bash
make -f Makefile.legacy kong-up
make -f Makefile.legacy kong-status
make kong-logs
make kong-config
make -f Makefile.legacy kong-down
make -f Makefile.legacy kong-info
```

Kong không được thêm vào `infra-up` mặc định để full infra không quá nặng.

## 4. Actual Routes In This Repo

### Master Data API

Client gọi:

```text
http://localhost:18000/api/master-data
http://localhost:18000/api/master-data/{id}
http://localhost:18000/api/master-data/code/{code}
```

Kong route:

```yaml
service: tenant-demo-api
url: http://host.docker.internal:8080
route path: /api/master-data
strip_path: false
```

Vì `strip_path=false`, request `/api/master-data` vẫn tới backend là `/api/master-data`.

Header:

- `Authorization` được forward mặc định;
- `X-Request-Id` được forward mặc định;
- CORS plugin cho phép local React Web origin `http://localhost:5173`.

Backend vẫn làm:

- validate JWT/Keycloak token;
- map role/RBAC;
- set `TenantContext`;
- query PostgreSQL tenant-aware.

### Health Route

Client gọi:

```text
http://localhost:18000/tenant-demo/actuator/health
```

Kong route:

```yaml
service: tenant-demo-health
url: http://host.docker.internal:8080/actuator/health
route path: /tenant-demo/actuator/health
strip_path: true
```

Kong chỉ expose đúng health endpoint để verify nhanh, không expose toàn bộ actuator.

### Audit Events API

Client gọi:

```text
http://localhost:18000/api/audit-events
http://localhost:18000/api/audit-events/{eventId}
```

Kong route:

```yaml
service: audit-log-api
url: http://host.docker.internal:8082
route path: /api/audit-events
strip_path: false
```

Kong vẫn chỉ forward request. `audit-log-service` tự validate JWT, tự lấy `tenant_id` và tự filter audit table theo tenant hiện tại.

## 5. Docker Networking

Browser trên host:

```text
localhost:18000 -> Kong container port 8000
```

Kong container gọi backend host app:

```text
host.docker.internal:8080 -> tenant-demo chạy bằng Maven trên host
```

Không dùng `localhost:8080` trong `kong.yml`. Bên trong container Kong, `localhost` là chính container Kong, không phải máy host.

## 6. Runtime Flow

```text
React Web hoặc curl
  -> Kong proxy :18000
  -> route /api/master-data hoặc /api/audit-events
  -> tenant-demo :8080 hoặc audit-log-service :8082
  -> Spring Security validates token
  -> JwtTenantContextFilter sets tenant
  -> Service/Repository query tenant-aware
  -> response đi ngược về Kong
  -> client
```

Kong là routing/platform layer. Nó không thay thế backend authorization hoặc tenant isolation.

## 7. Kong vs Spring Cloud Gateway In This Repo

| Điểm | Spring Cloud Gateway lab | Kong lab |
|---|---|---|
| Mục tiêu học | Route/predicate/filter trong Spring | Gateway platform/config/plugin |
| Runtime | Java app port `8081` | Kong container port `18000` |
| Config | `gateway-demo/application.yml` + Java filter | `kong-gateway-lab/kong.yml` |
| Vai trò | Artifact học trước | Phase 1.5 target-platform practice |

Không trộn hai gateway trong một request path khi demo cơ bản. Nếu UI gọi Kong, đặt `VITE_API_BASE_URL=http://localhost:18000`. Nếu UI gọi Spring Cloud Gateway, giữ `http://localhost:8081`.

## 8. Common Mistakes

- Dùng `localhost:8080` từ trong Kong container.
- Expose Admin API public.
- Nghĩ Kong validate JWT khi chưa cấu hình JWT/OIDC plugin.
- Nghĩ gateway thay thế backend security.
- Quên CORS khi browser gọi Kong từ `localhost:5173`.
- Route path mismatch do `strip_path` sai.
- Route quá rộng làm lộ actuator/private endpoints.
- Trộn Spring Cloud Gateway và Kong trong cùng request path mà không nói rõ đang test gateway nào.
- Log token/Authorization header trong gateway.

## 9. Verification

Start backend trước:

```bash
cd lab-code
make -f Makefile.legacy db-up
make -f Makefile.legacy app-run
```

Ở terminal khác:

```bash
cd lab-code
make -f Makefile.legacy kong-up
make -f Makefile.legacy kong-status
```

Verify health:

```bash
curl -i http://localhost:18000/tenant-demo/actuator/health
```

Verify backend security còn hoạt động:

```bash
curl -i http://localhost:18000/api/master-data
```

Kỳ vọng thiếu token trả `401` từ backend.

Verify audit route nếu `audit-log-service` đang chạy:

```bash
curl -i http://localhost:18000/api/audit-events
```

Kỳ vọng thiếu token trả `401` từ `audit-log-service`.

Với token hợp lệ:

```bash
curl -i \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "X-Request-Id: kong-demo-001" \
  http://localhost:18000/api/master-data
```

Kỳ vọng:

- backend trả `200`;
- tenant-demo log có requestId `kong-demo-001`;
- dữ liệu vẫn tenant-scoped.

Stop:

```bash
make -f Makefile.legacy kong-down
```

## 10. Giới hạn production

- Admin API cần network isolation/auth nghiêm túc.
- Production nên có TLS, rate limit, auth plugin policy rõ ràng, log/masking.
- Nếu dùng Kong OIDC/JWT plugin, backend vẫn không nên bỏ tenant-aware checks.
- DB-less phù hợp lab/config Git nhỏ; production có thể dùng DB-backed hoặc hybrid tùy vận hành.
- Chưa có Kubernetes/Ingreess Controller trong Phase 1.5.
