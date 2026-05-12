# Abstract class, interface và repository trong JPA

## Mục tiêu

Ghi chú này giúp mình chọn đúng abstraction trước khi code repository cho `tenant-demo`.

Trọng tâm:

- entity base dùng `abstract class` khi có field mapping chung;
- Spring Data JPA repository thường là `interface`;
- chưa over-design generic base repository trong giai đoạn học.

## Abstract class vs interface

| Khái niệm | Dùng để làm gì | Ví dụ dễ hiểu |
|---|---|---|
| `abstract class` | Chia sẻ state/field và logic chung cho class con | Base entity có `tenantId` |
| `interface` | Mô tả contract/hành vi mà class khác implement | Repository khai báo method query |

Nói thực dụng:

- Nếu cần có field chung thật sự: nghĩ tới abstract class.
- Nếu chỉ cần khai báo "có những method nào": nghĩ tới interface.

## Khi nào dùng abstract class?

Dùng abstract class khi:

- nhiều class con có field chung;
- muốn chia sẻ logic chung;
- không muốn tạo object trực tiếp từ base class;
- base class là một phần của mô hình domain/mapping.

Trong JPA, base entity thường dùng abstract class vì nó có persistent fields.

Ví dụ trong repo:

```text
TenantAwareEntity
  -> có field tenantId
  -> có @PrePersist auto set tenant
  -> MasterData kế thừa
```

Vì `TenantAwareEntity` sở hữu field mapping `tenant_id`, dùng `abstract class` + `@MappedSuperclass` là lựa chọn hợp lý.

## Khi nào dùng interface?

Dùng interface khi:

- muốn mô tả contract;
- không cần giữ state/persistent field;
- framework sẽ tạo implementation;
- muốn code phụ thuộc vào hành vi hơn là class cụ thể.

Spring Data JPA repository thường là interface vì mình chỉ khai báo method, còn Spring Data tạo implementation lúc runtime.

Ví dụ hướng thiết kế:

```text
MasterDataRepository extends JpaRepository<MasterData, Long>
  -> khai báo findByTenantIdAndCode(...)
  -> Spring Data JPA tạo implementation
```

## Vì sao shared JPA base entity dùng abstract class + `@MappedSuperclass`?

`@MappedSuperclass` cho JPA biết:

- class này không có table riêng;
- field của nó được mapping vào table của entity con;
- entity con kế thừa field và mapping đó.

Trong shared-table multi-tenant, các bảng nghiệp vụ đều cần `tenant_id`, nên base entity có thể gom logic chung:

- field `tenantId`;
- mapping `@Column(name = "tenant_id")`;
- logic `@PrePersist` để set tenant trước khi insert.

Interface không phù hợp để chứa persistent field như `tenantId`, vì interface không phải nơi JPA mapping column theo cách này.

## Vì sao repository thường là interface?

Spring Data JPA được thiết kế để repository là interface:

- mình khai báo method;
- Spring Data parse method name hoặc đọc `@Query`;
- Spring tạo bean repository thật lúc runtime;
- service inject repository interface đó.

Vì vậy ở giai đoạn này, `MasterDataRepository` nên là interface, không phải abstract class.

## `@MappedSuperclass` không phải repository domain entity

Một lỗi dễ gặp là để repository trỏ vào base class có `@MappedSuperclass`.

Trong JPA:

- `@Entity` là class có entity identity và được map tới table thật;
- `@MappedSuperclass` chỉ cung cấp field/mapping cho entity con;
- `@MappedSuperclass` không phải một domain entity độc lập để Spring Data tạo repository trực tiếp.

Vì vậy cách sai là:

```text
JpaRepository<TenantAwareEntity, Long>
```

`TenantAwareEntity` không phải bảng thật, không có `@Id` riêng và không nên là domain type của repository.

Cách đúng trong repo này là:

```text
JpaRepository<MasterData, Long>
```

`MasterData` là real `@Entity`, map tới bảng `master_data`, có `id` và kế thừa field `tenantId`.

## Vì sao chưa nên over-design base repository?

Generic base repository tenant-aware nghe hấp dẫn, nhưng dễ làm bài học phức tạp quá sớm:

- phải hiểu custom repository base class;
- phải override behavior mặc định của Spring Data;
- dễ tưởng là đã tự động chống leakage trong mọi case;
- khó debug nếu query sinh ra không như mong muốn.

Trong Phase 1, cách học tốt hơn là explicit:

```text
findByTenantIdAndCode(...)
findByTenantIdAndCategory(...)
findByTenantIdAndIsActiveTrue(...)
```

Nhìn method là thấy ngay query có tenant scope hay không.

## Nếu sau này dùng generic base repository thì sao?

Spring Data có pattern nâng cao cho base repository. Khi tạo một generic repository interface không muốn Spring tự tạo bean trực tiếp, thường cần đánh dấu bằng `@NoRepositoryBean`.

Ý tưởng:

```text
@NoRepositoryBean
interface TenantAwareRepository<T extends TenantAwareEntity, ID>
```

Nhưng đây là bước sau. Nếu dùng sớm, mình dễ bị rối ở các câu hỏi:

- domain type thật là gì;
- method nào Spring Data parse được;
- method nào cần custom implementation;
- query nào thực sự tenant-aware;
- Spring có đang tạo bean repository ngoài ý muốn không.

Vì vậy trong Phase 1, pattern khuyến nghị vẫn là explicit repository cho từng entity chính.

## `Optional` là gì và dùng khi nào?

`Optional<T>` diễn đạt rằng kết quả có thể có hoặc không.

Với query zero-or-one như:

```text
findByTenantIdAndCode(...)
findByTenantIdAndId(...)
```

`Optional<MasterData>` tốt hơn trả trực tiếp `MasterData`, vì:

- code nói rõ record có thể không tồn tại;
- service buộc phải xử lý case not found;
- tránh nhầm `null` với dữ liệu hợp lệ;
- phù hợp khi unique constraint đảm bảo tối đa một record cho `(tenant_id, code)`.

Với query có thể trả nhiều dòng như category hoặc active list, dùng `List<MasterData>`.

## Khuyến nghị cho repo này

| Thành phần | Nên dùng | Lý do |
|---|---|---|
| `TenantAwareEntity` | `abstract class` + `@MappedSuperclass` | Có field mapping chung `tenantId` |
| `MasterData` | concrete `@Entity` class | Map bảng thật `master_data` |
| `MasterDataRepository` | interface extends `JpaRepository<MasterData, Long>` | Spring Data tạo implementation cho real entity |
| `TenantAwareRepository` | de-scope hoặc `@NoRepositoryBean` nếu giữ để học | Chưa dùng làm base chính trong Phase 1 |

## Common mistakes

| Lỗi | Vì sao nguy hiểm |
|---|---|
| Đặt persistent fields trong interface | Interface không phải nơi phù hợp để JPA map column |
| Đặt `@Id` ở base entity khi không thật sự chung | Có thể làm entity con khó khớp schema |
| Repository là abstract class khi chưa cần custom implementation | Tăng độ khó và lệch pattern Spring Data |
| `JpaRepository<TenantAwareEntity, Long>` | Trỏ repository vào mapped superclass thay vì real entity |
| Quên `@NoRepositoryBean` ở generic base repository | Spring Data có thể cố tạo bean repository không mong muốn |
| Method repository thiếu `tenantId` | Dễ gây data leakage |
| Entity mapping lệch Flyway schema | App fail khi `ddl-auto=validate` hoặc query sai |
| Entity tự đọc request/security trực tiếp | Trộn trách nhiệm mapping với web/security |
| Tin `tenant_id` từ request body | Client có thể tự đổi tenant nếu backend không kiểm soát |

## Checklist trước khi code repository

Method nên có:

- `findByTenantIdAndIsActiveTrue(Long tenantId)`
- `findByTenantIdAndCategory(Long tenantId, String category)`
- `Optional<MasterData> findByTenantIdAndCode(Long tenantId, String code)`
- `Optional<MasterData> findByTenantIdAndId(Long tenantId, Long id)`

Method nguy hiểm nên tránh:

- `findByCode(String code)`
- `findByCategory(String category)`
- `findAll()`, nếu service dùng trực tiếp mà không filter tenant;
- `findById(Long id)`, nếu service không verify tenant ownership.

Sau khi thêm repository, verify:

- `./mvnw validate` compile pass;
- app start được với PostgreSQL/Flyway;
- log Spring Data JPA phát hiện repository interface;
- sau khi có service/controller, curl tenant 1 và tenant 2 trả data tách biệt.

## Nguồn tham khảo chuẩn

- Java Language Specification - Classes: https://docs.oracle.com/javase/specs/jls/se17/html/jls-8.html
- Java Language Specification - Interfaces: https://docs.oracle.com/javase/specs/jls/se17/html/jls-9.html
- Jakarta Persistence `@MappedSuperclass`: https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/mappedsuperclass
- Jakarta Persistence `@Entity`: https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/entity
- Spring Data repositories core concepts: https://docs.spring.io/spring-data/jpa/reference/repositories/core-concepts.html
- Spring Data query methods: https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html
