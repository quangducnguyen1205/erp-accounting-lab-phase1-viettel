# API Gateway foundation

> [!NOTE]
> **Phạm vi tài liệu:** Gateway chính thức cho Phase 1.5 demo hiện là **Kong Gateway**. Nội dung Spring Cloud Gateway trong thư mục này được giữ lại làm tài liệu tham khảo lịch sử.

## 1. API Gateway là gì?

API Gateway là entry point nằm trước các backend services. Client gọi Gateway, Gateway quyết định request đó đi tới service nào.

Trong kiến trúc nhiều service:

```text
Client / React
-> API Gateway
-> tenant service / accounting service / file service / search service
```

Gateway giúp gom một số cross-cutting concerns ở rìa hệ thống:

- routing;
- path rewrite;
- CORS;
- TLS termination;
- request id/correlation id;
- rate limit;
- auth pre-check;
- observability ở entry point.

Nhưng Gateway **không nên chứa business logic**. Logic “tenant 1 có được đọc record tenant 2 không?” vẫn thuộc backend service/repository.

## 2. Reverse proxy vs API Gateway

Reverse proxy cơ bản nhận request rồi forward đến backend.

API Gateway thường là reverse proxy có thêm rule theo API/backend architecture:

- route theo path/header/method;
- filter request/response;
- validate token hoặc gọi auth service nếu thiết kế yêu cầu;
- rate limit/throttle;
- expose metrics/logs;
- có thể tích hợp service discovery.

Phase 1 của repo chỉ cần phần nhỏ nhất: **static route**.

## 3. Route, predicate, filter

Theo Spring Cloud Gateway, một route gồm:

- `id`: tên route.
- `uri`: backend đích.
- `predicates`: điều kiện match request.
- `filters`: xử lý request/response trước hoặc sau khi forward.

Ví dụ mental model:

```yaml
route:
  id: tenant-demo-api
  uri: http://localhost:8080
  predicate: Path=/api/**
```

Ý nghĩa:

- request tới Gateway có path `/api/master-data`;
- predicate `Path=/api/**` match;
- Gateway forward request sang `http://localhost:8080/api/master-data`.

## 4. Gateway routing khác business logic thế nào?

Gateway trả lời câu hỏi:

- Request này đi tới service nào?
- Có cần rewrite path/header không?
- Có cần reject sớm vì rate limit/auth pre-check không?

Backend service trả lời câu hỏi:

- User này là ai?
- Role/permission có đủ không?
- Tenant context là gì?
- Query có filter đúng `tenantId` không?
- Business rule có pass không?

Trong repo này:

```text
Gateway
-> chỉ route /api/** sang tenant-demo

tenant-demo
-> validate JWT/Keycloak token
-> JwtTenantContextFilter đọc tenant_id
-> Service/Repository filter tenantId
```

## 5. Auth ở Gateway vs Auth ở service

Production có thể validate token ở Gateway để chặn request xấu sớm. Tuy vậy backend services vẫn phải bảo vệ boundary của mình, nhất là khi:

- service có thể được gọi từ internal network;
- có nhiều gateway/internal client;
- có bug/misconfig ở gateway;
- tenant-aware data access phải nằm gần dữ liệu.

Phase 1 rule:

> Gateway có thể hỗ trợ auth, nhưng không thay thế Resource Server validation và tenant-aware query trong backend.

## 6. Header propagation

Gateway nên forward các header cần thiết:

- `Authorization`: để backend validate token.
- `X-Request-Id`: để nối log giữa Gateway và backend.
- các header trace/correlation khác nếu có.

Không nên log:

- token;
- password;
- request body nhạy cảm;
- full payload chứng từ/hóa đơn.

## 7. Static route trong repo này

Mini-lab hiện tại:

```text
Client
-> http://localhost:8081/api/master-data
-> gateway-demo
-> http://localhost:8080/api/master-data
-> tenant-demo
```

Vì chỉ có một backend service, static URL `http://localhost:8080` là đủ. Chưa cần Eureka/Consul/Kubernetes.

## 8. Production caveats

Chưa làm trong Phase 1:

- multi-instance backend load balancing;
- gateway-level JWT validation;
- rate limiting;
- circuit breaker;
- retry policy;
- centralized CORS policy;
- WAF/security policy;
- service discovery thật;
- gateway dashboard/alerting.

## Nguồn chính

- Spring Cloud Gateway Reference: https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/
- Spring Cloud release train compatibility: https://spring.io/projects/spring-cloud
