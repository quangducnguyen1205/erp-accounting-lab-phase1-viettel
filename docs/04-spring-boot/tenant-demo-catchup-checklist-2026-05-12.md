# Checklist catch up tenant-demo — 2026-05-12

## Mục tiêu hôm nay

Do bị bận ngày 10/05 và 11/05, hôm nay cần catch up ba nhóm việc:

1. `TenantContext` / `TenantFilter`
2. `TenantAwareEntity` / `MasterData` / `MasterDataRepository`
3. `MasterDataService` / `MasterDataController` / curl verification

Nguyên tắc: tự code phần chính, chạy được từng bước nhỏ, sau đó mới nhờ Agent review.

## Đọc nhanh theo thứ tự

1. `docs/04-spring-boot/request-filter-threadlocal.md`
2. `docs/04-spring-boot/tenant-context-filter-design.md`
3. `docs/04-spring-boot/jpa-entity-repository-tenant-aware.md`
4. `docs/04-spring-boot/service-controller-curl-flow.md`
5. Nếu bị lỗi config/database: đọc lại `docs/04-spring-boot/spring-boot-database-stack.md`

## Bước 0 — kiểm tra baseline

Mở trước:

- `lab-code/tenant-demo/src/main/resources/application.yml`
- `lab-code/tenant-demo/src/main/resources/db/migration/V1__create_tenants.sql`
- `lab-code/tenant-demo/src/main/resources/db/migration/V2__create_master_data.sql`
- `lab-code/tenant-demo/src/main/resources/db/migration/V3__create_indexes.sql`

Chạy:

```bash
cd lab-code
make db-up
make db-status
cd tenant-demo
./mvnw validate
```

Done khi:

- Maven validate không lỗi dependency;
- PostgreSQL container đang chạy;
- hiểu app đang dùng Flyway schema, không dùng Hibernate auto-create.

## Bước 1 — code TenantContext

Mở:

- `lab-code/tenant-demo/src/main/java/com/viettel/demo/context/TenantContext.java`

Tự code:

- `ThreadLocal<Long>`;
- `setCurrentTenant(Long tenantId)`;
- `getCurrentTenant()`;
- `clear()` dùng `remove()`.

Done khi:

- class compile được;
- không có logic HTTP trong `TenantContext`;
- hiểu vì sao phải clear.

Lệnh kiểm tra nhanh:

```bash
cd lab-code/tenant-demo
./mvnw validate
```

## Bước 2 — code TenantFilter

Mở:

- `lab-code/tenant-demo/src/main/java/com/viettel/demo/config/TenantFilter.java`

Tự code:

- filter chạy một lần mỗi request;
- đọc header `X-Tenant-Id`;
- validate missing/invalid tenant;
- set tenant context;
- gọi filter chain;
- clear trong `finally`.

Done khi:

- app compile;
- request thiếu/sai tenant có policy rõ;
- chưa đưa JWT/Keycloak thật vào.

Lệnh kiểm tra:

```bash
cd lab-code/tenant-demo
./mvnw validate
```

Nếu app đã có endpoint health hoặc endpoint tạm nào đó, có thể curl thử. Nếu chưa có controller, để curl đầy đủ sang bước 5.

## Bước 3 — code entity và repository

Mở:

- `lab-code/tenant-demo/src/main/java/com/viettel/demo/entity/TenantAwareEntity.java`
- `lab-code/tenant-demo/src/main/java/com/viettel/demo/entity/MasterData.java`
- `lab-code/tenant-demo/src/main/java/com/viettel/demo/repository/MasterDataRepository.java`

Tự code:

- `TenantAwareEntity` là mapped superclass có `tenantId`;
- `MasterData` map đúng bảng `master_data`;
- field Java khớp column Flyway;
- repository method luôn có `tenantId`.

Done khi:

- `./mvnw validate` pass;
- app không fail vì Hibernate validate lệch schema;
- không có method nguy hiểm như find by code thiếu tenant.

Lệnh kiểm tra:

```bash
cd lab-code/tenant-demo
./mvnw validate
```

## Bước 4 — code service

Mở:

- `lab-code/tenant-demo/src/main/java/com/viettel/demo/service/MasterDataService.java`

Tự code tối thiểu:

- inject repository;
- lấy tenant hiện tại từ `TenantContext`;
- list master data theo tenant;
- find by code theo tenant.

Tạm hoãn nếu thiếu thời gian:

- create/update/delete;
- soft delete;
- custom exception hierarchy.

Done khi:

- service không nhận tenant id từ request body;
- mọi repository call đều scoped theo tenant;
- compile pass.

## Bước 5 — code controller và curl

Mở:

- `lab-code/tenant-demo/src/main/java/com/viettel/demo/controller/MasterDataController.java`

Tự code tối thiểu:

- `GET /api/master-data`;
- `GET /api/master-data/code/{code}` nếu đủ thời gian;
- gọi service, không gọi repository trực tiếp.

Chạy app:

```bash
cd lab-code
make app-run
```

Terminal khác:

```bash
curl -i -H "X-Tenant-Id: 1" http://localhost:8080/api/master-data
curl -i -H "X-Tenant-Id: 2" http://localhost:8080/api/master-data
curl -i http://localhost:8080/api/master-data
curl -i -H "X-Tenant-Id: abc" http://localhost:8080/api/master-data
```

Nếu có endpoint by code:

```bash
curl -i -H "X-Tenant-Id: 1" http://localhost:8080/api/master-data/code/LAPTOP-01
curl -i -H "X-Tenant-Id: 2" http://localhost:8080/api/master-data/code/LAPTOP-01
```

Done khi:

- tenant 1 chỉ thấy dữ liệu tenant 1;
- tenant 2 chỉ thấy dữ liệu tenant 2;
- missing/invalid tenant bị xử lý rõ;
- cùng code ở hai tenant không bị lẫn dữ liệu.

## Có thể hoãn sang sau

- Full CRUD.
- DTO layer đẹp.
- Pagination/sorting.
- Spring Security/Keycloak/JWT thật.
- Base repository generic.
- Test tự động `DataLeakageTest`.
- Redis/cache/Kafka/observability.

## Nên nhờ Agent review sau khi tự code xong

Prompt review nên tập trung:

- `TenantContext.clear()` có chắc chạy trong `finally` không?
- Filter có chặn missing/invalid `X-Tenant-Id` rõ không?
- Entity mapping có khớp Flyway schema không?
- Repository method nào còn thiếu `tenantId` không?
- Service có lấy tenant từ `TenantContext`, không lấy từ body không?
- Curl output có chứng minh tenant isolation chưa?

## Output nên lưu lại sau hôm nay

- Một đoạn ngắn trong `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` sau khi tự verify xong.
- Các curl command quan trọng và pattern kết quả.
- Nếu fail, lưu lỗi chính và nguyên nhân đã hiểu.

## Nguồn tham khảo chuẩn

- Java `ThreadLocal`: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ThreadLocal.html
- Jakarta Servlet `Filter`: https://jakarta.ee/specifications/platform/11/apidocs/jakarta/servlet/filter
- Spring `OncePerRequestFilter`: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/filter/OncePerRequestFilter.html
- Jakarta Persistence annotations: https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/package-summary
- Spring Data JPA repositories: https://docs.spring.io/spring-data/jpa/reference/repositories/core-concepts.html
- Spring MVC request mapping: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html
- PostgreSQL constraints: https://www.postgresql.org/docs/current/ddl-constraints.html
