# Spring Boot + PostgreSQL database stack

## Mục tiêu

Tài liệu này giải thích các lớp công nghệ nằm giữa một HTTP request và PostgreSQL trong `tenant-demo`. Mục tiêu là hiểu vai trò từng phần trước khi tự cấu hình `application.yml`, viết Flyway migration, entity, repository, service và controller.

Đây là ghi chú nền tảng cho Sprint 2, không phải hướng dẫn Hibernate/JPA nâng cao.

## Bức tranh tổng thể

Luồng xử lý dữ liệu trong app backend thường đi qua các lớp sau:

```text
HTTP request
  -> Controller
  -> Service
  -> Repository
  -> Spring Data JPA
  -> JPA
  -> Hibernate
  -> DataSource / HikariCP
  -> PostgreSQL JDBC Driver
  -> PostgreSQL database
```

Ý nghĩa ngắn:

| Lớp | Vai trò |
|---|---|
| Controller | Nhận request, trả response |
| Service | Xử lý nghiệp vụ và quyết định gọi repository thế nào |
| Repository | Truy cập dữ liệu theo interface/method |
| Spring Data JPA | Tạo implementation repository dựa trên JPA |
| JPA | Chuẩn/spec mapping object với database |
| Hibernate | Implementation phổ biến của JPA, sinh/chạy SQL |
| DataSource / HikariCP | Quản lý connection tới database |
| PostgreSQL JDBC Driver | Driver cụ thể để Java nói chuyện với PostgreSQL |
| PostgreSQL | Database server lưu dữ liệu thật |

Flyway nằm ở luồng khởi động app:

```text
Spring Boot app starts
  -> đọc application.yml/env vars
  -> tạo DataSource
  -> Flyway chạy migration SQL
  -> JPA/Hibernate validate hoặc dùng schema
  -> app sẵn sàng nhận request
```

Trong lab này, Flyway nên tạo schema trước. JPA/Hibernate chỉ mapping và validate/use schema, không tự tạo schema thay Flyway.

## PostgreSQL

PostgreSQL là database server. Nó lưu:

- bảng;
- dòng dữ liệu;
- constraint;
- index;
- transaction;
- dữ liệu thật của hệ thống.

Trong repo này, PostgreSQL là source of truth cho dữ liệu tenant. Các bảng như `tenants` và `master_data` phải được tạo rõ ràng bằng SQL migration. Constraint như `UNIQUE (tenant_id, code)` phải được PostgreSQL enforce, không chỉ ghi nhớ trong Java code.

## JDBC

JDBC là chuẩn API của Java để nói chuyện với relational database. Nó là lớp thấp hơn JPA.

Nếu dùng JDBC trực tiếp, developer thường phải tự viết SQL, tự bind parameter, tự đọc `ResultSet`, tự map row thành object. Điều này rõ ràng nhưng khá cơ khí.

Trong `tenant-demo`, mình không bắt đầu bằng JDBC trực tiếp. Spring Data JPA/Hibernate sẽ dùng JDBC ở bên dưới.

## PostgreSQL JDBC Driver

PostgreSQL JDBC Driver là driver cụ thể giúp Java/JDBC kết nối được với PostgreSQL.

Nó xuất hiện trong [pom.xml](/Users/nqd2005/Projects/viettel/lab-code/tenant-demo/pom.xml) vì Maven cần tải đúng thư viện driver. Nếu thiếu driver, app có thể biết có datasource config nhưng không biết cách mở connection tới PostgreSQL.

Trong `pom.xml`, dependency này là:

```xml
<groupId>org.postgresql</groupId>
<artifactId>postgresql</artifactId>
```

## DataSource

`DataSource` là object mà Spring dùng để lấy database connection.

Spring Boot tạo `DataSource` từ cấu hình trong `application.yml` hoặc environment variables, ví dụ:

- database host;
- port;
- database name;
- username;
- password.

Trong lab này, các thông tin đó khớp với [docker-compose.yml](/Users/nqd2005/Projects/viettel/lab-code/docker/docker-compose.yml):

| Thông tin | Giá trị local hiện tại |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Database | `erpdb` |
| User | `erpuser` |
| Password | `erpuser` |

## HikariCP

HikariCP là connection pool mặc định mà Spring Boot thường dùng.

Ý tưởng cơ bản:

- Mở một physical database connection tốn chi phí.
- App backend không nên mở connection mới từ đầu cho mỗi query.
- Connection pool giữ sẵn một số connection để tái sử dụng.

Ở giai đoạn này, chỉ cần hiểu HikariCP giúp app quản lý connection hiệu quả hơn. Chưa cần tuning pool size, timeout hay metric.

## JPA

JPA là Java Persistence API. Đây là chuẩn/spec, không phải một thư viện cụ thể duy nhất.

JPA định nghĩa cách:

- mapping class Java thành table;
- mapping field thành column;
- quản lý entity;
- query dữ liệu qua repository/entity manager.

Ví dụ sau này `MasterData` sẽ map tới bảng `master_data`. Nhưng JPA chỉ là chuẩn; cần một implementation cụ thể để chạy thật.

## Hibernate

Hibernate là implementation phổ biến của JPA.

Hibernate có thể:

- đọc entity class;
- map entity sang table;
- chuyển repository/entity operations thành SQL;
- quản lý session/persistence context;
- validate schema nếu cấu hình `ddl-auto=validate`.

Hibernate cũng có khả năng tự tạo/cập nhật schema bằng `ddl-auto=create`, `update`, `create-drop`. Nhưng trong lab này không nên dùng Hibernate để tạo schema, vì mục tiêu là học migration có kiểm soát bằng Flyway.

Quy tắc cho repo này:

- Flyway tạo schema.
- PostgreSQL enforce constraint.
- Hibernate/JPA mapping entity và query dữ liệu.
- `ddl-auto` nên là `validate` hoặc `none`, không dùng `create/drop/update` cho schema thật của lab.

## Spring Data JPA

Spring Data JPA là abstraction nằm trên JPA/Hibernate. Nó giúp viết repository interface thay vì tự viết nhiều code truy cập dữ liệu cơ khí.

Ví dụ sau này có thể có `MasterDataRepository` với method tìm dữ liệu theo tenant và code.

Nhưng trong multi-tenant system, Spring Data JPA không tự bảo vệ tenant isolation. Repository method vẫn phải được thiết kế cẩn thận:

- query phải có `tenantId`;
- service phải lấy tenant từ trusted context;
- không nhận `tenantId` tùy tiện từ request body;
- không viết method tìm theo `code` mà quên tenant.

## Flyway

Flyway là tool quản lý database migration.

Nó chạy các file SQL versioned theo thứ tự, ví dụ:

```text
src/main/resources/db/migration/
  V1__create_tenants.sql
  V2__create_master_data.sql
  V3__create_indexes.sql
```

Flyway ghi lại migration nào đã chạy trong database. Nhờ vậy app có thể biết schema đang ở version nào.

Trong `tenant-demo`, Flyway nên sở hữu schema creation:

- `V1`: tạo bảng `tenants`;
- `V2`: tạo bảng `master_data`;
- `V3`: tạo index/constraint bổ sung theo thiết kế lab.

So với Hibernate `ddl-auto`:

| Cách tạo schema | Phù hợp để làm gì | Rủi ro trong lab này |
|---|---|---|
| Flyway migration | Schema thay đổi có version, có lịch sử | Cần tự viết SQL cẩn thận |
| Hibernate `ddl-auto` | Demo rất nhanh hoặc prototype nhỏ | Che mất bài học migration, khó kiểm soát schema |

## `application.yml`

`application.yml` là file cấu hình Spring Boot.

Trong lab này, nó hướng dẫn cấu hình:

- `server.port`;
- `spring.datasource.url`;
- `spring.datasource.username`;
- `spring.datasource.password`;
- `spring.jpa.hibernate.ddl-auto`;
- `spring.flyway.enabled`;
- logging phục vụ học local.

File hiện tại là skeleton có TODO để bạn tự bật cấu hình. Khi gặp lỗi `make app-run`, cần phân biệt lỗi do:

- thiếu dependency trong `pom.xml`;
- sai datasource URL/user/password;
- PostgreSQL container chưa chạy;
- Flyway migration SQL chưa đúng;
- Java entrypoint chưa implement;
- entity/repository chưa khớp schema.

## Environment variables, `.env` và `.env.example`

Spring Boot đọc environment variables từ process đang chạy.

Điểm dễ nhầm:

- `.env` không tự động được Spring Boot đọc trong mọi context.
- Terminal, IDE, Docker Compose hoặc tool khác có thể load `.env` thành environment variables.
- `.env.example` là file mẫu an toàn để commit.
- `.env` thật có thể chứa secret local, không nên commit.

Trong repo này, [`.env.example`](/Users/nqd2005/Projects/viettel/lab-code/tenant-demo/.env.example) ghi các biến cần có:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USER
DB_PASSWORD
SERVER_PORT
```

Với local learning, default trong `${VAR:default}` giúp app dễ chạy hơn. Nhưng trong môi trường thật, secret/config nên được truyền từ hệ thống quản lý cấu hình phù hợp, không hardcode trong Git.

## Maven và `pom.xml`

Maven tải dependencies dựa trên `pom.xml`.

Trong `tenant-demo`, `pom.xml` hiện khai báo các dependency tối thiểu:

| Dependency | Dùng để làm gì |
|---|---|
| `spring-boot-starter-web` | Controller/HTTP API |
| `spring-boot-starter-data-jpa` | JPA, Hibernate, repository |
| `postgresql` | PostgreSQL JDBC Driver |
| `flyway-core` | Database migration |
| `spring-boot-starter-test` | Test |

Không cần học thuộc `pom.xml`. Cần hiểu dependency nào xuất hiện vì nhu cầu nào.

## Liên hệ multi-tenant với repo này

Trong `tenant-demo`, các lớp database stack sẽ gắn với multi-tenant như sau:

- Flyway tạo schema `tenants` và `master_data`.
- PostgreSQL enforce constraint như `UNIQUE (tenant_id, code)`.
- PostgreSQL index hỗ trợ query tenant-aware nhanh hơn.
- JPA/Hibernate map `MasterData` entity tới bảng `master_data`.
- Repository method phải có `tenantId` rõ ràng, ví dụ tìm theo `tenantId` và `code`.
- `TenantFilter` đọc tenant hiện tại từ request, trong Phase 1 là header `X-Tenant-Id`.
- `TenantContext` giữ tenant hiện tại cho service/repository dùng.
- Service lấy tenant từ trusted context, không lấy bừa từ request body.

Điểm quan trọng: PostgreSQL constraint và backend tenant filtering đều cần thiết.

- Constraint bảo vệ tính đúng đắn dữ liệu ở database.
- Backend filtering bảo vệ dữ liệu khi query.
- Index giúp performance.
- Index không thay thế tenant isolation.
- Repository thiếu `tenantId` vẫn có thể gây data leakage dù database có index tốt.

## Vì sao Sprint 2 chưa dùng mọi công nghệ?

Các công nghệ như Spring Security, Keycloak, Redis, Kafka, Elasticsearch, MinIO và observability stack đều quan trọng trong kiến trúc lớn hơn. Nhưng Sprint 2 chỉ tập trung dựng baseline database-backed tenant demo.

Lý do không nhồi hết vào ngay:

- Nếu app cơ bản chưa chạy, thêm nhiều công nghệ sẽ làm lỗi khó phân biệt.
- Tenant isolation ở database/backend là nền tảng trước auth/cache/message/search.
- Final demo Phase 1 chỉ cần chứng minh flow nhỏ nhưng đúng: request có tenant, schema do Flyway tạo, query tenant-aware, không lộ dữ liệu tenant khác.

Phạm vi Sprint 2:

| Có trong Sprint 2 | Chưa đưa vào Sprint 2 |
|---|---|
| Spring Boot app start | Spring Security |
| PostgreSQL datasource | Keycloak/OAuth2/OIDC |
| Flyway migration baseline | Redis cache |
| TenantContext/TenantFilter | Kafka/Debezium |
| Tenant-aware data access nền | Elasticsearch/MinIO |
| Log/curl verify tối thiểu | Prometheus/Grafana/Loki |

## Checklist trước khi chạy app

Trước khi chạy `make app-run`, kiểm tra:

- [ ] `pom.xml` có Web, Data JPA, PostgreSQL driver, Flyway, Test.
- [ ] PostgreSQL Docker Compose đang chạy.
- [ ] `.env.example` mô tả đủ biến cần có.
- [ ] `application.yml` đọc DB config từ env vars hoặc default local an toàn.
- [ ] Hibernate `ddl-auto` là `validate` hoặc `none`, không phải `create`, `create-drop`.
- [ ] Flyway được bật.
- [ ] Migration files nằm trong `src/main/resources/db/migration`.
- [ ] `TenantDemoApplication` có entrypoint Spring Boot.
- [ ] `make app-run` dùng Maven wrapper nhất quán.

## Lỗi thường gặp

- Nghĩ rằng `.env` tự động được Spring Boot đọc.
- Để Hibernate tự tạo schema trong khi cũng dùng Flyway.
- Quên PostgreSQL JDBC Driver trong `pom.xml`.
- Hardcode secret thật trong `application.yml`.
- Viết repository method thiếu `tenantId`.
- Tin rằng index có thể tự ngăn data leakage.
- Thêm Spring Security, Redis, Kafka, Elasticsearch quá sớm khi app cơ bản chưa chạy.

## Thứ tự đọc và làm tiếp

1. Đọc `docs/04-spring-boot/spring-boot-bootstrap-config.md`.
2. Đọc `docs/04-spring-boot/spring-boot-database-stack.md`.
3. Tự fill `application.yml`.
4. Tự viết Flyway `V1`, `V2`, `V3`.
5. Implement `TenantDemoApplication`.
6. Chạy `./mvnw validate`.
7. Chạy `make app-run` sau khi config, migrations và entrypoint đã sẵn sàng.
