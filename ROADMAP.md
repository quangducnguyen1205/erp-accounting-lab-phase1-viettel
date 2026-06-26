# Lộ trình đọc lại kiến thức của Master Data Portal

Tài liệu này là một bản đồ tham chiếu tĩnh (closed technical reference map) dành cho hệ thống Master Data Portal. Đây **không phải** là một kế hoạch dự án đang hoạt động (not an active project plan). Nó hoàn toàn không chứa danh sách công việc cần triển khai, không có quy trình báo cáo kết quả, và không có nhật ký theo dõi tiến độ. 

Thứ tự gợi ý dưới đây được thiết kế nhằm giúp người đọc trong tương lai có thể hiểu repository này một cách có hệ thống, từ các định nghĩa khái niệm nền tảng ban đầu cho đến cấu trúc kiến trúc runtime thực tế. Việc đọc theo trình tự này sẽ tái tạo lại quá trình thiết kế hệ thống mà không vướng phải những thông tin thay đổi trong quá trình thử nghiệm.

## Tóm tắt kiến trúc hiện tại

Để có bối cảnh trước khi đi sâu vào chi tiết, xin lưu ý cấu trúc hệ thống đã được chốt và đóng lại:

- Keycloak đóng vai trò là local OIDC provider.
- Các Spring Boot services đóng vai trò là Resource Servers độc lập.
- Kong là current gateway xử lý định tuyến (routing).
- PostgreSQL là source of truth lưu trữ dữ liệu chính.
- Redis chỉ được sử dụng với mô hình cache-aside.
- `tenant-demo` độc quyền sở hữu quyền ghi đối với master-data.
- `audit-log-service` độc quyền sở hữu hành vi đọc và lưu trữ audit log.
- `file-service` sở hữu dữ liệu metadata của file và vòng đời file, trong khi MinIO trực tiếp lưu trữ các binary payloads.
- `search-service` sở hữu hành vi tìm kiếm và quản lý Elasticsearch projection.
- Kafka hỗ trợ luồng xử lý dữ liệu bất đồng bộ (asynchronous flow) giữa các service.
- Prometheus, Grafana, Loki và Alloy kết hợp lại tạo thành baseline cho observability ở môi trường local.
- Thiết kế hướng miền (DDD) chỉ nằm ở mức độ nhận thức và tham chiếu (awareness/reference); repository này không phải là một đợt tái cấu trúc (refactor) toàn diện của hệ thống ERP theo chuẩn DDD.

---

## 1. Bức tranh sản phẩm và SaaS

Mục tiêu của phần này là nắm vững ngữ cảnh nghiệp vụ của bài toán: vì sao phần mềm được phân phối dưới dạng dịch vụ (SaaS) và sự khác biệt cốt lõi giữa nó với các giải pháp phần mềm on-premise truyền thống.

- **Đọc:** [docs/01-saas/README.md](docs/01-saas/README.md)
- **Mục đích:** Hiểu rõ vì sao hệ thống ERP/kế toán lại đòi hỏi phải giải quyết bài toán multi-tenancy một cách triệt để ngay từ kiến trúc ban đầu.

## 2. Multi-tenancy và tenant isolation

Trọng tâm là nắm bắt các mô hình cách ly dữ liệu giữa nhiều khách hàng (tenant) trong cùng một cơ sở hạ tầng, cũng như các đánh đổi (trade-off) kiến trúc tương ứng với mỗi lựa chọn.

- **Đọc:** [docs/02-multi-tenant/README.md](docs/02-multi-tenant/README.md)
- **Mục đích:** Hiểu vì sao dự án quyết định chọn giải pháp cách ly dữ liệu bằng mô hình shared-table và phương pháp phòng tránh các rủi ro vận hành (như rò rỉ dữ liệu leakage, hay vấn đề noisy neighbor).
- **Khu vực code liên quan:** Tập hợp các đoạn mã truy vấn SQL thuần trong thư mục `lab-code/sql-playground/`.

## 3. Backend, PostgreSQL và Spring Boot

Phần này chuyển các khái niệm lý thuyết về cơ sở dữ liệu thành việc áp dụng trong mã nguồn backend thực tế, từ đó xử lý an toàn các transaction và kịch bản di trú (migration) cơ sở dữ liệu.

- **Đọc:** 
  - [docs/03-backend-database-mo-rong/README.md](docs/03-backend-database-mo-rong/README.md)
  - [docs/04-spring-boot/README.md](docs/04-spring-boot/README.md)
- **Mục đích:** Hiểu cách cơ sở dữ liệu PostgreSQL thực hiện việc cô lập (isolation) và khóa (locking) ở mức độ thấp, cùng với việc Spring Boot và JPA truy xuất dữ liệu có ý thức về ngữ cảnh của tenant (tenant-aware).
- **Khu vực code liên quan:** Core logic tương tác cơ sở dữ liệu nằm trong module `lab-code/tenant-demo/`. Lưu ý môi trường thử nghiệm flyway nằm ở thư mục lịch sử `lab-code/flyway-failure-lab/` với mục đích tham khảo.

## 4. Identity, Keycloak, JWT và RBAC

Phần này giải thích chi tiết cơ chế xác thực (AuthN) và phân quyền (AuthZ) được tách rời hoàn toàn khỏi core business logic thông qua một identity provider chuyên dụng.

- **Đọc:** [docs/05-security/README.md](docs/05-security/README.md)
- **Mục đích:** Phân tích luồng phát hành token bảo mật của Keycloak, cơ chế các backend (như một Resource Server) xác thực cấu trúc JWT, và cách áp dụng mô hình Role-Based Access Control (RBAC).
- **Khu vực code liên quan:** Các filter bảo mật nằm tại module dùng chung `lab-code/common-security/` và môi trường định nghĩa thiết lập tĩnh tại `lab-code/keycloak-lab/`.

## 5. Frontend, browser flow và Kong gateway

Phần tài liệu này giúp kết nối các mảnh ghép độc lập lại thành một luồng xử lý end-to-end hoàn chỉnh, đi từ thao tác trên trình duyệt của người dùng cho tới khi request đi qua lớp gateway bảo vệ.

- **Đọc:** 
  - [docs/06-frontend/README.md](docs/06-frontend/README.md)
  - [docs/07-architecture/kong-gateway/README.md](docs/07-architecture/kong-gateway/README.md)
- **Mục đích:** Nắm bắt luồng cấu hình định tuyến tĩnh (static routing) thông qua Kong Gateway. Xin lưu ý: Môi trường `gateway-demo` (dựa trên Spring Cloud Gateway) chỉ đóng vai trò tài liệu tham chiếu lịch sử nhằm mục đích so sánh các mô hình kiến trúc gateway.
- **Khu vực code liên quan:** Web interface mỏng nằm tại `lab-code/web-ui-demo/` và thiết lập Kong cố định nằm tại `lab-code/kong-gateway-lab/`.

## 6. Runtime ownership và service boundaries

Phân tích cách một khối kiến trúc lớn được chia cắt thành các service độc lập, mỗi service có quyền sở hữu dữ liệu và vòng đời hệ thống riêng biệt.

- **Đọc:** [docs/07-architecture/microservice-boundaries/README.md](docs/07-architecture/microservice-boundaries/README.md)
- **Mục đích:** Hiểu rõ giới hạn về trách nhiệm của từng service trong Phase 1.5 và làm thế nào dữ liệu không bị chồng chéo giữa các miền.
- **Khu vực code liên quan:** Các module thực thi tách biệt bao gồm `lab-code/tenant-demo/`, `lab-code/audit-log-service/`, `lab-code/file-service/`, và `lab-code/search-service/`.

## 7. Redis, Kafka, Elasticsearch, MinIO và observability

Khám phá các thành phần mở rộng trong backend, nơi hệ thống sử dụng cache, event-driven pattern, tìm kiếm văn bản và giám sát hệ thống.

- **Đọc:** 
  - [docs/07-architecture/cache-redis/README.md](docs/07-architecture/cache-redis/README.md)
  - [docs/07-architecture/messaging-kafka/README.md](docs/07-architecture/messaging-kafka/README.md)
  - [docs/07-architecture/search-elasticsearch/README.md](docs/07-architecture/search-elasticsearch/README.md)
  - [docs/07-architecture/object-storage-minio/README.md](docs/07-architecture/object-storage-minio/README.md)
  - [docs/07-architecture/observability/README.md](docs/07-architecture/observability/README.md)
  - [docs/07-architecture/log-aggregation-loki/README.md](docs/07-architecture/log-aggregation-loki/README.md)
- **Mục đích:** Nhận diện được vị trí, trách nhiệm cụ thể và lý do sử dụng của mỗi công cụ trong bức tranh kiến trúc nền tảng.
- **Khu vực code liên quan:** Các cấu trúc Docker và kịch bản khởi chạy tại `lab-code/redis-lab/`, `lab-code/kafka-lab/`, `lab-code/elasticsearch-lab/`, `lab-code/minio-lab/`, và `lab-code/loki-lab/`.

## 8. DDD awareness và cách đọc code theo ownership

Một góc nhìn định hướng về Domain-Driven Design (DDD) nhưng được thiết lập giới hạn cẩn thận để không làm phức tạp hóa môi trường thực hành.

- **Đọc:** [docs/08-design/README.md](docs/08-design/README.md)
- **Mục đích:** Trang bị nhận thức về sự khác biệt giữa repository này so với một hệ thống ERP thực tế đầy đủ tính năng, từ đó giúp định hình tư duy đọc source code một cách đúng đắn dựa trên các ranh giới ngữ cảnh (bounded context).

## 9. Gợi ý thứ tự mở code sau khi đọc docs

Để việc duyệt mã nguồn có hệ thống và đối chiếu dễ dàng với phần lý thuyết:

1. Bắt đầu bằng việc mở `lab-code/common-security/` để hiểu cơ chế filter JWT tenant-aware được áp dụng chung cho mọi request.
2. Mở `lab-code/tenant-demo/` và đi qua các Controller đến Repository nhằm theo dõi luồng ghi dữ liệu trực tiếp vào Master Data.
3. Chuyển hướng sang `lab-code/audit-log-service/` để quan sát cơ chế hệ thống consume Kafka event một cách độc lập.
4. Kiểm tra cấu trúc của `lab-code/search-service/` để nhận ra quá trình đồng bộ hóa dữ liệu từ hệ thống chính sang Elasticsearch projection.
5. Xem xét `lab-code/file-service/` để nắm rõ sự phân chia giữa việc lưu siêu dữ liệu (metadata) trên PostgreSQL so với dữ liệu nhị phân (binary payload) lưu tại MinIO.
6. Cuối cùng, xem qua `lab-code/web-ui-demo/` và cách cấu hình gateway `lab-code/kong-gateway-lab/` để nhìn nhận toàn diện cách các service rời rạc được tập hợp phục vụ cho một client thống nhất.
