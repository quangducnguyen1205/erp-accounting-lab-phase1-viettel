# API Gateway mini-lab plan

## 1. Mục tiêu

Chạy một Gateway nhỏ ở local để thấy:

```text
Client
-> gateway-demo :8081
-> tenant-demo :8080
-> Spring Security + TenantContext + Service/Repository
```

Gateway chỉ route request. Business logic, authorization và tenant isolation vẫn nằm trong `tenant-demo`.

## 2. Artifact

- `lab-code/gateway-demo/`: Spring Cloud Gateway app nhỏ.
- `lab-code/gateway-demo/http/gateway-api.http`: request mẫu.
- `docs/07-architecture/api-gateway-service-discovery/`: docs foundation/code guide/awareness.
- `lab-code/Makefile`: target `gateway-run`.

## 3. Cách chạy

### Terminal 1 - start dependencies/app chính

Tùy mode auth bạn muốn demo:

```bash
cd lab-code
make -f Makefile.legacy db-up
make -f Makefile.legacy app-run
```

Nếu chạy Keycloak mode thì start Keycloak và env tương ứng như các lab trước.

### Terminal 2 - start gateway

```bash
cd lab-code
make -f Makefile.legacy gateway-run
```

Gateway mặc định:

```text
http://localhost:8081
```

Backend target mặc định:

```text
http://localhost:8080
```

Có thể override:

```bash
GATEWAY_PORT=8081 TENANT_DEMO_URI=http://localhost:8080 make -f Makefile.legacy gateway-run
```

## 4. Verify bằng HTTP Client

Mở:

```text
lab-code/gateway-demo/http/gateway-api.http
```

Cases chính:

- `GET http://localhost:8081/api/master-data` với token hợp lệ -> expected `200`.
- request thiếu token -> expected `401` từ `tenant-demo`.
- `GET http://localhost:8081/tenant-demo/actuator/health` -> expected health response từ `tenant-demo`.
- request có `X-Request-Id: gateway-demo-001` -> `tenant-demo` log cùng request id.

## 5. Điều cần quan sát

- Gateway không tự đọc business data.
- Gateway không tự tạo `TenantContext`.
- Header `Authorization` đi qua Gateway đến backend.
- Header `X-Request-Id` đi qua Gateway đến backend.
- Nếu backend reject token, response qua Gateway vẫn là `401`.

## 6. Tiêu chí hoàn thành

- Gateway app compile được.
- `tenant-demo` tests vẫn pass.
- Gateway route `/api/**` đến `tenant-demo`.
- Missing token qua Gateway vẫn `401`.
- `X-Request-Id` truyền qua được đến log backend.
- Docs nói rõ service discovery/load balancing chỉ là awareness.

## 7. Giới hạn hiện tại

Chưa làm:

- gateway-level token validation;
- service discovery thật;
- load balancing nhiều instances;
- rate limit;
- circuit breaker/retry;
- production API gateway deployment.
