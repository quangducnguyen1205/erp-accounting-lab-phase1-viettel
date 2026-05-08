# Spring Boot bootstrap và cấu hình local

## Mục tiêu

Ghi chú này giúp hiểu phần bootstrap cơ khí trước khi tự implement `TenantContext`, `TenantFilter` và flow repository/service/controller. Đây không phải tài liệu học Spring Boot đầy đủ, chỉ là nền để app PoC có thể chạy đúng cách.

Sau khi đọc file này, đọc tiếp `docs/04-spring-boot/spring-boot-database-stack.md` để hiểu rõ các lớp PostgreSQL, JDBC, DataSource, HikariCP, JPA, Hibernate, Spring Data JPA và Flyway phối hợp với nhau như thế nào.

## Maven là gì?

Maven là công cụ build cho Java. Nó giúp tải dependency, compile code, chạy test, đóng gói app và chạy plugin.

Trong repo này, Maven được dùng để:

- khai báo Spring Boot dependencies;
- chạy app bằng `spring-boot:run`;
- chạy test;
- quản lý plugin như Spring Boot Maven plugin.

## `pom.xml` là gì?

`pom.xml` là file cấu hình project Maven. Nó mô tả:

- project tên gì, group/artifact/version là gì;
- dùng Java version nào;
- cần dependency nào;
- dùng plugin build nào.

Thực tế không cần học thuộc toàn bộ cấu trúc `pom.xml`. Với Spring Boot, file này thường được sinh từ Spring Initializr hoặc tooling, sau đó developer đọc và chỉnh có kiểm soát. Điều cần hiểu là dependency nào được thêm vào và vì sao.

## Maven wrapper là gì?

Maven wrapper gồm `mvnw`, `mvnw.cmd` và thư mục `.mvn/wrapper/`. Nó giúp mọi người trong team chạy cùng một Maven version mà không cần cài Maven global giống nhau.

Trong repo học này:

- `./mvnw validate` kiểm tra project Maven ở mức nhẹ;
- `./mvnw spring-boot:run` sẽ dùng để chạy app khi cấu hình và code đã đủ;
- Makefile đang gọi `./mvnw`, nên wrapper giúp lệnh `make app-run` ổn định hơn.

## `application.yml` là gì?

`application.yml` là file cấu hình của Spring Boot. Nó có thể chứa cấu hình server port, datasource, JPA, Flyway và logging.

Ví dụ các nhóm cấu hình thường gặp:

- `server.port`: app chạy ở port nào.
- `spring.datasource.*`: kết nối PostgreSQL.
- `spring.jpa.*`: cấu hình Hibernate/JPA.
- `spring.flyway.*`: cấu hình migration.
- `logging.level.*`: log phục vụ debug local.

## `application.yml`, environment variables, `.env`, `.env.example`

Các khái niệm này dễ bị lẫn:

| Thành phần | Vai trò |
|---|---|
| `application.yml` | File cấu hình Spring Boot đọc trực tiếp từ classpath |
| Environment variables | Biến môi trường được truyền vào process khi chạy app |
| `.env` | File local thường dùng để lưu biến môi trường cho máy cá nhân |
| `.env.example` | File mẫu được commit để người khác biết cần biến nào |

Spring Boot không tự đọc `.env` chỉ vì file đó tồn tại. `.env` cần được terminal, IDE, Docker Compose hoặc tool khác load thành environment variables trước khi chạy app.

`.env` không nên commit vì có thể chứa password, token hoặc cấu hình riêng của máy local. Repo chỉ commit `.env.example`.

## Flyway làm gì?

Flyway quản lý database migration theo file SQL có version, ví dụ:

- `V1__create_tenants.sql`
- `V2__create_master_data.sql`
- `V3__create_indexes.sql`

Flyway ghi lại migration nào đã chạy, migration nào chưa chạy, và chạy chúng theo thứ tự version.

Trong lab này, Flyway nên sở hữu việc tạo schema vì mục tiêu là học migration có kiểm soát. Hibernate `ddl-auto` không nên tự tạo bảng.

Gợi ý:

- Dùng `spring.jpa.hibernate.ddl-auto=validate` nếu muốn Hibernate kiểm tra entity khớp schema.
- Dùng `none` nếu chưa muốn Hibernate validate.
- Tránh `update`, `create`, `create-drop` cho PoC này vì chúng che mất bài học về migration.

## Cần hiểu trước khi sang TenantContext/TenantFilter

Trước khi implement tenant flow, cần tự trả lời được:

- App kết nối PostgreSQL bằng config nào?
- Schema do Flyway tạo hay Hibernate tự tạo?
- Vì sao migration SQL phải dựa trên SQL playground đã hoàn thành?
- Vì sao secret thật không nằm trong Git?
- Khi `make app-run` lỗi, lỗi nằm ở Maven dependency, config datasource, Flyway migration hay Java code?

Khi trả lời được các câu này, bước tiếp theo mới nên là tự code `TenantContext`, `TenantFilter` và endpoint tenant-aware.
