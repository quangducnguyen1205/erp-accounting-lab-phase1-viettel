# Spring Cloud Gateway code guide

## 1. Repo dùng cách nào?

Repo tạo app nhỏ:

```text
lab-code/gateway-demo/
```

App này dùng:

- Spring Boot `3.2.12`;
- Spring Cloud `2023.0.x` vì release train này tương thích Spring Boot `3.2.x`;
- Spring Cloud Gateway;
- static route tới `tenant-demo`.

Không dùng Eureka/Consul/Kubernetes trong code.

## 2. Package/code shape

```text
com.viettel.gateway
├── GatewayDemoApplication
└── RequestIdGatewayFilter
```

### `GatewayDemoApplication`

Entry point của gateway app, giống một Spring Boot app bình thường.

### `RequestIdGatewayFilter`

Global filter nhỏ:

- nếu request đã có `X-Request-Id`, giữ nguyên;
- nếu thiếu, sinh UUID;
- forward header này sang downstream service;
- không log token/body.

Filter này chỉ phục vụ bài học observability/correlation id. Không phải auth filter.

## 3. Route config

File:

```text
lab-code/gateway-demo/src/main/resources/application.yml
```

Route chính:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: tenant-demo-api
          uri: ${TENANT_DEMO_URI:http://localhost:8080}
          predicates:
            - Path=/api/**
```

Ý nghĩa:

- Gateway nghe ở port `8081`.
- Request `GET http://localhost:8081/api/master-data`.
- Gateway forward tới `http://localhost:8080/api/master-data`.
- Header `Authorization` được forward mặc định.
- Backend `tenant-demo` vẫn validate token.

Route health:

```yaml
- id: tenant-demo-health
  uri: ${TENANT_DEMO_URI:http://localhost:8080}
  predicates:
    - Path=/tenant-demo/actuator/health
  filters:
    - RewritePath=/tenant-demo/actuator/health, /actuator/health
```

Ý nghĩa:

- client gọi Gateway path `/tenant-demo/actuator/health`;
- Gateway rewrite path thành `/actuator/health`;
- request được forward tới `tenant-demo`.

## 4. Vì sao không auth ở Gateway trong lab này?

Để giữ bài học rõ:

- Gateway học routing/predicate/filter/header propagation.
- `tenant-demo` học Resource Server/AuthZ/tenant-aware query.

Nếu thêm auth ở Gateway quá sớm, người học dễ hiểu nhầm rằng Gateway đã thay thế security trong backend. Production có thể validate token ở Gateway, nhưng backend vẫn phải bảo vệ chính nó.

## 5. Request id đi qua Gateway

Flow:

```text
Client
-> X-Request-Id: gateway-demo-001
-> Gateway giữ header
-> tenant-demo nhận cùng X-Request-Id
-> RequestLoggingFilter log requestId=gateway-demo-001
```

Nếu client không gửi `X-Request-Id`, Gateway sinh một UUID rồi forward.

## 6. Chạy app

Từ `lab-code/`:

```bash
make gateway-run
```

Gateway chạy ở:

```text
http://localhost:8081
```

`tenant-demo` vẫn phải chạy riêng ở:

```text
http://localhost:8080
```

## 7. Common mistakes

- Nghĩ Gateway thay thế backend auth.
- Quên forward `Authorization`.
- Rewrite path sai làm backend nhận path không tồn tại.
- Put business rule vào Gateway.
- Log full token/header/body trong Gateway.
- Dựng service discovery khi mới có một service.

## 8. Khi nào nâng cấp tiếp?

Chỉ học sâu hơn khi có trigger:

- có nhiều backend service;
- có nhiều instance cần load balancing;
- cần CORS/rate limit thật;
- cần gateway auth pre-check;
- cần deploy qua Kubernetes hoặc service discovery.
