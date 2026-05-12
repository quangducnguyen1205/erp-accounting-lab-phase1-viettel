# JPA Entity, Repository và tenant-aware query

## Mục tiêu

Ghi chú này giúp mình tự code nhanh các file:

- `lab-code/tenant-demo/src/main/java/com/viettel/demo/entity/TenantAwareEntity.java`
- `lab-code/tenant-demo/src/main/java/com/viettel/demo/entity/MasterData.java`
- `lab-code/tenant-demo/src/main/java/com/viettel/demo/repository/MasterDataRepository.java`

Trọng tâm: mapping entity phải khớp Flyway schema, và repository query phải luôn có `tenantId`.

## Entity là gì?

Trong JPA, entity là class Java đại diện cho một bảng hoặc một phần dữ liệu persistent trong database.

Trong lab này:

```text
MasterData entity
  -> table master_data
  -> column id, tenant_id, code, name, category, is_active, created_at
```

Entity không phải nơi tự quyết định tenant hiện tại từ HTTP request. Entity chỉ mô tả dữ liệu và mapping với database.

## Các annotation mapping cơ bản

| Annotation | Dùng để làm gì |
|---|---|
| `@Entity` | Đánh dấu class là JPA entity |
| `@Table(name = "...")` | Chỉ rõ bảng database được mapping |
| `@Id` | Chỉ rõ primary key của entity |
| `@GeneratedValue` | Chỉ rõ cách sinh id |
| `@Column(name = "...")` | Mapping field Java với column database |
| `@MappedSuperclass` | Base class có mapping được entity con kế thừa |
| `@PrePersist` | Callback trước khi insert entity |

Với `TenantAwareEntity`, `@MappedSuperclass` phù hợp hơn `@Entity` vì base class này không cần bảng riêng. Nó chỉ cung cấp field chung như `tenantId` cho entity con.

## Mapping `tenant_id`

Trong shared-table multi-tenant, mọi bảng nghiệp vụ cần cột `tenant_id`.

Quy tắc cho lab:

- `tenant_id` phải map thành field rõ ràng, ví dụ `tenantId`;
- column nên là `nullable = false` vì dữ liệu nghiệp vụ không được mồ côi tenant;
- khi update, không nên cho đổi tenant tùy tiện;
- khi insert, tenant nên lấy từ trusted backend context, không lấy từ request body tùy ý.

Gợi ý thiết kế học tập: `TenantAwareEntity` có thể set `tenantId` trước khi persist bằng `@PrePersist`, dựa trên `TenantContext`. Nếu không có tenant hiện tại, nên fail rõ ràng thay vì insert dữ liệu không thuộc tenant nào.

## Entity mapping phải khớp Flyway schema

Trong repo này, Flyway tạo schema bằng:

- `V1__create_tenants.sql`
- `V2__create_master_data.sql`
- `V3__create_indexes.sql`

Vì `application.yml` đang dùng `spring.jpa.hibernate.ddl-auto: validate`, Hibernate sẽ kiểm tra entity mapping có khớp schema thật hay không. Nếu tên column, kiểu dữ liệu hoặc constraint mapping lệch, app có thể fail khi start.

Điểm cần tự đối chiếu:

| Flyway schema | Entity mapping |
|---|---|
| `master_data` | `@Table(name = "master_data")` |
| `tenant_id` | field `tenantId`, `@Column(name = "tenant_id")` |
| `is_active` | field `isActive`, `@Column(name = "is_active")` |
| `created_at` | field `createdAt`, `@Column(name = "created_at")` |
| `UNIQUE (tenant_id, code)` | nên được hiểu và có thể mô tả lại bằng `@Table(uniqueConstraints = ...)` |

Trong lab này, Flyway vẫn là source chính tạo schema. Annotation unique trong entity giúp code đọc dễ hiểu hơn, nhưng không thay thế migration SQL đã chạy trong database.

## Repository làm gì?

Repository là tầng truy cập dữ liệu. Với Spring Data JPA, mình thường viết interface, còn Spring Data tạo implementation lúc runtime.

Ví dụ về ý tưởng:

```text
MasterDataService
  -> MasterDataRepository
      -> Spring Data JPA
          -> Hibernate
              -> SQL tới PostgreSQL
```

Repository không nên chứa business workflow dài. Nó nên tập trung vào query dữ liệu.

## Derived query methods và `@Query`

Spring Data JPA có hai cách cơ bản để viết query ở mức beginner:

| Cách | Ý nghĩa | Khi nào dùng |
|---|---|---|
| Derived query method | Tên method được parse thành query | Query đơn giản, field rõ ràng |
| `@Query` | Tự viết JPQL/SQL trong annotation | Query cần rõ hơn hoặc derived name quá dài |

Ví dụ tư duy method tenant-aware:

```text
findByTenantIdAndCode(...)
findByTenantIdAndCategory(...)
findByTenantIdAndIsActiveTrue(...)
```

Không nên bắt đầu bằng method thiếu tenant như:

```text
findByCode(...)
findByCategory(...)
```

Vì trong shared-table design, `code` hoặc `category` có thể tồn tại ở nhiều tenant.

## Vì sao repository phải explicit có tenantId?

Tenant isolation là correctness requirement, không phải tối ưu performance.

Nếu query thiếu `tenantId`:

- tenant A có thể nhìn thấy dữ liệu tenant B;
- endpoint có thể trả nhiều dòng cùng `code` từ nhiều tenant;
- index tốt vẫn không sửa được lỗi truy vấn sai phạm vi;
- frontend ẩn dữ liệu không bảo vệ được API.

Database constraint như `UNIQUE (tenant_id, code)` bảo vệ tính đúng khi ghi dữ liệu. Repository method tenant-aware bảo vệ phạm vi khi đọc dữ liệu. Cả hai đều cần.

## Checklist cho `TenantAwareEntity`

- [ ] Dùng `@MappedSuperclass`.
- [ ] Có field `tenantId`.
- [ ] Map đúng column `tenant_id`.
- [ ] Không để `tenantId` nullable.
- [ ] Cân nhắc không cho update `tenantId`.
- [ ] Nếu dùng `@PrePersist`, lấy tenant từ `TenantContext`.
- [ ] Nếu không có tenant context khi insert, fail rõ ràng.

## Checklist cho `MasterData`

- [ ] Dùng `@Entity`.
- [ ] Map đúng bảng `master_data`.
- [ ] Kế thừa `TenantAwareEntity`.
- [ ] Có `id`, `code`, `name`, `category`, `isActive`, `createdAt`.
- [ ] Map `is_active` và `created_at` đúng tên column.
- [ ] Đối chiếu type Java với type PostgreSQL trong Flyway migration.
- [ ] Nhớ ý nghĩa tenant-aware unique constraint `(tenant_id, code)`.

## Checklist cho `MasterDataRepository`

- [ ] Kế thừa `JpaRepository<MasterData, Long>` hoặc thiết kế đơn giản tương đương.
- [ ] Method list dữ liệu phải có `tenantId`.
- [ ] Method find by code phải có `tenantId`.
- [ ] Nếu có find by id, phải verify thêm tenant ownership, không chỉ dùng id.
- [ ] Không tạo method public dễ dùng sai như `findByCode(String code)`.
- [ ] Chưa overdo custom base repository nếu chưa cần cho demo nhỏ.

## Verify sau khi có API

Repository thường khó kiểm chứng trực tiếp bằng curl. Sau khi có service/controller, dùng API để kiểm tra:

- cùng endpoint, `X-Tenant-Id: 1` chỉ trả dữ liệu tenant 1;
- `X-Tenant-Id: 2` chỉ trả dữ liệu tenant 2;
- query theo `code` vẫn scoped theo tenant;
- không có response nào trả dữ liệu tenant khác.

Nếu app start fail khi `ddl-auto=validate`, ưu tiên kiểm tra entity mapping có lệch Flyway schema không.

## Nguồn tham khảo chuẩn

- Jakarta Persistence `@Entity`: https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/entity
- Jakarta Persistence `@Table`: https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/table
- Jakarta Persistence `@Id`: https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/id
- Jakarta Persistence `@Column`: https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/column
- Jakarta Persistence `@MappedSuperclass`: https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/mappedsuperclass
- Jakarta Persistence `@PrePersist`: https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/prepersist
- Spring Data JPA repositories: https://docs.spring.io/spring-data/jpa/reference/repositories/core-concepts.html
- Spring Data query methods: https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html
- Spring Data JPA query methods: https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html
- PostgreSQL constraints: https://www.postgresql.org/docs/current/ddl-constraints.html
