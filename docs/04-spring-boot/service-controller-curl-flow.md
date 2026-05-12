# Service, Controller và curl verification cho tenant-demo

## Mục tiêu

Ghi chú này giúp mình tự code nhanh:

- `lab-code/tenant-demo/src/main/java/com/viettel/demo/service/MasterDataService.java`
- `lab-code/tenant-demo/src/main/java/com/viettel/demo/controller/MasterDataController.java`

Trọng tâm: API chạy được, tenant được lấy từ backend context, và curl chứng minh dữ liệu được scoped theo tenant.

## Controller, Service, Repository khác nhau thế nào?

| Tầng | Trách nhiệm chính | Không nên làm |
|---|---|---|
| Controller | Nhận HTTP request, đọc path/body/header cần thiết, trả HTTP response | Nhồi business logic dài |
| Service | Xử lý nghiệp vụ, lấy tenant hiện tại, gọi repository đúng cách | Tin `tenant_id` từ body tùy tiện |
| Repository | Query database bằng JPA/Hibernate | Tự quyết định flow nghiệp vụ |

Trong lab này, controller càng mỏng càng tốt. Service là nơi quyết định query nào được gọi và tenant hiện tại được áp dụng ra sao.

## Luồng tenant từ Filter đến Database

```text
Client
  -> Header X-Tenant-Id
  -> TenantFilter validate header
  -> TenantContext.setCurrentTenant(...)
  -> MasterDataController
  -> MasterDataService
       -> TenantContext.getCurrentTenant()
       -> MasterDataRepository.findByTenantId...
  -> PostgreSQL query có WHERE tenant_id = ...
  -> finally TenantContext.clear()
```

Điểm học chính: tenant id đi vào hệ thống ở filter, nhưng service/repository mới là nơi dùng tenant đó để bảo vệ truy vấn dữ liệu.

## Endpoint scope cho lab này

Để bắt kịp nhanh, không cần full CRUD ngay. Nên ưu tiên endpoint đọc dữ liệu vì nó chứng minh tenant isolation rõ nhất.

Gợi ý scope tối thiểu:

| Endpoint | Mục đích |
|---|---|
| `GET /api/master-data` | List master data của tenant hiện tại |
| `GET /api/master-data/code/{code}` | Tìm một code trong tenant hiện tại |

Có thể hoãn:

- create/update/delete;
- pagination/sorting nâng cao;
- DTO framework phức tạp;
- validation framework đầy đủ;
- Spring Security/Keycloak.

Nếu muốn thêm create sau, nhớ: client không nên tự quyết định `tenantId` trong body; tenant phải đến từ `TenantContext`.

## Validate missing tenant ở đâu?

Trong Sprint này, nơi rõ nhất để chặn missing/invalid tenant là `TenantFilter`.

Filter nên đảm bảo:

- request hợp lệ mới đi tiếp vào controller;
- request thiếu `X-Tenant-Id` bị trả lỗi rõ;
- request có `X-Tenant-Id` không parse được bị trả lỗi rõ.

Service vẫn nên phòng thủ ở mức tối thiểu: nếu `TenantContext.getCurrentTenant()` rỗng, fail rõ ràng. Đây là safety net nếu sau này có endpoint hoặc test bypass filter.

## Service nên làm gì?

`MasterDataService` nên:

- inject `MasterDataRepository`;
- lấy tenant hiện tại từ `TenantContext`;
- gọi repository method có `tenantId`;
- nếu không tìm thấy dữ liệu trong tenant hiện tại, trả lỗi phù hợp;
- không nhận tenant id từ request body làm nguồn tin cậy.

Với mục tiêu catch up, service chỉ cần đủ cho list và find-by-code trước.

## Controller nên làm gì?

`MasterDataController` nên:

- dùng `@RestController`;
- có base path rõ, ví dụ `/api/master-data`;
- map endpoint bằng `@GetMapping`;
- gọi service;
- trả response đơn giản, dễ curl;
- không tự query repository;
- không tự xử lý tenant isolation thay service.

Nếu chưa tạo DTO, có thể trả entity trong lab để tiết kiệm thời gian. Nhưng cần biết production thường dùng DTO để kiểm soát response contract.

## Không overdo trong task này

- Chưa cần full CRUD.
- Chưa cần Keycloak/JWT thật.
- Chưa cần generic base service/repository framework.
- Chưa cần pagination nâng cao.
- Chưa cần cache.
- Chưa cần async/Kafka.
- Chưa cần UI.

Mục tiêu hôm nay: app chạy, endpoint đọc được, tenant 1 và tenant 2 không thấy dữ liệu của nhau.

## Curl verification checklist

Chạy app:

```bash
cd lab-code
make db-up
make app-run
```

Terminal khác, sau khi có controller:

```bash
curl -i -H "X-Tenant-Id: 1" http://localhost:8080/api/master-data
curl -i -H "X-Tenant-Id: 2" http://localhost:8080/api/master-data
curl -i http://localhost:8080/api/master-data
curl -i -H "X-Tenant-Id: abc" http://localhost:8080/api/master-data
```

Nếu có endpoint find by code:

```bash
curl -i -H "X-Tenant-Id: 1" http://localhost:8080/api/master-data/code/LAPTOP-01
curl -i -H "X-Tenant-Id: 2" http://localhost:8080/api/master-data/code/LAPTOP-01
```

## Pattern kết quả mong đợi

Không cần invent exact JSON. Chỉ cần kiểm tra pattern:

| Case | Pattern mong đợi |
|---|---|
| Tenant 1 list | Chỉ có dữ liệu tenant 1 |
| Tenant 2 list | Chỉ có dữ liệu tenant 2 |
| Missing `X-Tenant-Id` | Bị trả lỗi rõ, không đi vào business query |
| Invalid `X-Tenant-Id` | Bị trả lỗi rõ |
| Cùng code ở 2 tenant | Mỗi tenant chỉ thấy record thuộc tenant mình |

Nếu tenant 1 nhìn thấy record tenant 2, đó là bug tenant isolation, không phải chỉ là lỗi formatting response.

## Ghi chú khi đọc logs

Log hữu ích ở mức học:

- app start thành công;
- Flyway migration đã chạy hoặc đã validate;
- request đi qua filter;
- tenant id được set rồi clear;
- Hibernate SQL có điều kiện tenant nếu bật SQL logging.

Không cần log quá nhiều dữ liệu nhạy cảm. Không log password hoặc secret.

## Nguồn tham khảo chuẩn

- Spring Framework `@Service`: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/stereotype/Service.html
- Spring Framework `@RestController`: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/bind/annotation/RestController.html
- Spring Framework request mapping: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html
- Spring Framework `@GetMapping`: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/bind/annotation/GetMapping.html
- Spring Framework `ResponseEntity`: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/responseentity.html
- Spring Framework `ResponseStatusException`: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/server/ResponseStatusException.html
- Spring Data JPA query methods: https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html
