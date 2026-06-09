# Service discovery / load balancing awareness

## 1. Vấn đề cần giải quyết

Khi chỉ có một service:

```text
Gateway -> http://localhost:8080
```

Static URL là đủ.

Khi có nhiều service hoặc nhiều instance:

```text
tenant-service instance A
tenant-service instance B
tenant-service instance C
```

Gateway/client cần biết nên gọi instance nào. Đây là lúc service discovery và load balancing xuất hiện.

## 2. Static URL routing

Static route dùng URL cố định:

```yaml
uri: http://localhost:8080
```

Ưu điểm:

- dễ hiểu;
- ít dependency;
- phù hợp local lab;
- đủ khi chỉ có một backend instance.

Nhược điểm:

- không tự phát hiện service mới;
- không tự cân bằng tải nhiều instance;
- đổi địa chỉ phải đổi config.

Repo này dùng static route trước vì đó là bài học nhỏ nhất.

## 3. DNS-based discovery

Client gọi một hostname ổn định:

```text
http://tenant-demo.internal
```

DNS hoặc platform resolve hostname này đến IP thật.

Phù hợp khi hạ tầng đã có DNS/service name rõ.

## 4. Eureka / Consul

Eureka/Consul là service registry:

```text
Service instance -> đăng ký với registry
Gateway/client -> hỏi registry có instance nào sống
Gateway/client -> chọn một instance để gọi
```

Hữu ích trong hệ microservices truyền thống, nhưng Phase 1 chưa cần vì repo chưa có nhiều service instance.

## 5. Kubernetes Service

Trong Kubernetes:

```text
Gateway -> http://tenant-demo-service
Kubernetes Service -> load balance đến pods
```

Kubernetes đã có discovery/load balancing ở platform level. Khi deploy thật, nhiều bài toán không cần Eureka nữa.

## 6. Client-side vs server-side load balancing

Client-side:

- client/gateway biết danh sách instances;
- client tự chọn instance.

Server-side:

- client gọi một endpoint/load balancer;
- load balancer chọn instance phía sau.

Phase 1 chỉ cần hiểu khái niệm, chưa cần implement.

## 7. Gateway với load balancing trong Spring Cloud

Spring Cloud Gateway có thể dùng URI dạng:

```text
lb://tenant-demo
```

Khi đó cần service discovery/load balancer setup tương ứng. Repo chưa dùng vì static route rõ hơn cho một service.

## 8. Tenant/security caveat

Dù routing/load balancing nằm ở đâu:

- token vẫn phải được validate ở backend boundary;
- `tenantId` vẫn phải đi từ validated token vào service/repository;
- repository vẫn filter theo `tenantId`;
- load balancer không hiểu business tenant isolation.

## 9. Khi nào repo nên học sâu hơn?

Học tiếp khi:

- có ít nhất 2 backend service trong lab;
- muốn chạy 2 instance `tenant-demo` ở 2 port khác nhau;
- muốn demo gateway cân bằng request;
- muốn giải thích deployment target có Kubernetes/API Gateway thật.

Chưa cần trong milestone hiện tại.
