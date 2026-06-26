# API Gateway / Service Discovery

## Thư mục này chứa gì?

Nhóm này giải thích API Gateway, static routing, service discovery và load balancing ở mức Phase 1. Repo có một mini-lab nhỏ dùng Spring Cloud Gateway để route request đến `tenant-demo`; service discovery/load balancing vẫn giữ ở mức awareness.

## Thứ tự đọc đề xuất

1. [api-gateway-foundation.md](api-gateway-foundation.md) - API Gateway là gì, route/predicate/filter, gateway vs backend service.
2. [spring-cloud-gateway-code-guide.md](spring-cloud-gateway-code-guide.md) - cách đọc cấu hình/code gateway demo trong repo.
3. [service-discovery-load-balancing-awareness.md](service-discovery-load-balancing-awareness.md) - static URL, DNS, Eureka/Consul, Kubernetes Service, load balancing.

## Trạng thái

- Mini-lab static route đã được chuẩn bị và verify bằng `lab-code/gateway-demo/`.
- Gateway route `/api/**` đến `tenant-demo` ở `http://localhost:8080`.
- React Web UI gọi Gateway ở `http://localhost:8081`; Gateway forward `Authorization` và `X-Request-Id` sang backend.
- Service discovery/load balancing chưa implement vì repo hiện chỉ có một backend service.

## Giới hạn hiện tại

Gateway trong lab này không thay thế Spring Security/tenant-aware query trong `tenant-demo`. Backend vẫn là security và tenant isolation boundary chính.
