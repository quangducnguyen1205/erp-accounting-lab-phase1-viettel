# Mục lục tài liệu

## Mục tiêu

Thư mục `docs/` là source of truth cho kiến thức Phase 1 và Phase 1.5 đã được chuẩn hóa. Bản đồ tài liệu này (reading map) được dùng để tra cứu các chủ đề kỹ thuật và kiến trúc của repository.

## Thứ tự đọc đề xuất

Để có lộ trình đọc lại tĩnh (static rereading order) kết hợp giữa tài liệu lý thuyết và cấu trúc mã nguồn một cách hệ thống, vui lòng tham khảo [../ROADMAP.md](../ROADMAP.md).
Nếu bạn chỉ muốn tra cứu theo từng chủ đề riêng lẻ:

1. [01-saas/](01-saas/) - SaaS, cloud, subscription, on-premise.
2. [02-multi-tenant/](02-multi-tenant/) - multi-tenant, tenant isolation và trade-off.
3. [03-backend-database-mo-rong/](03-backend-database-mo-rong/) - PostgreSQL, index, migration, transaction.
4. [04-spring-boot/](04-spring-boot/) - Spring Boot, JPA, filter, repository/service/controller, test.
5. [05-security/](05-security/) - JWT, Spring Security Resource Server, Keycloak/OIDC/RBAC.
6. [07-architecture/](07-architecture/) - gateway, service boundary, Kafka, MinIO, Elasticsearch, Redis, observability.
7. [06-frontend/](06-frontend/) - React Web UI demo `Master Data Portal`.

## Nội dung cốt lõi và nội dung mở rộng

| Nhóm | Vai trò | Trạng thái |
|---|---|---|
| SaaS + multi-tenant | Nền tảng lý thuyết | Cốt lõi |
| PostgreSQL/backend DB | Nền tảng thực hành SQL, migration, transaction | Cốt lõi |
| Spring Boot | Backend tenant-aware runnable | Cốt lõi |
| Security/JWT/Keycloak | Auth flow và tenant context từ token | Cốt lõi + mini-lab |
| React Web UI | Demo business flow qua Keycloak/Kong/backend | Final Phase 1.5 demo |
| Kafka, MinIO, Elasticsearch, Redis | Các capability backend được tách thành service/lab nhỏ | Đã có demo local |
| Loki/Grafana/Alloy, Kafka UI, Kong | Tooling production-like để quan sát hệ thống local | Phase 1.5 |
| DDD, outbox, retry/DLT, schema registry, Kubernetes | Mở rộng sau | Chưa làm |

## Cấu trúc chuẩn cho công nghệ mới

Khi thêm một công nghệ mới, ưu tiên cấu trúc tài liệu:

1. Foundation/concept doc: `docs/07-architecture/<topic-folder>/<tech>.md`.
2. Request/response/config/API shape doc nếu công nghệ có protocol/API dễ nhầm:
   `docs/07-architecture/<topic-folder>/<tech>-request-response-shapes.md`.
3. Code guide doc: `docs/07-architecture/<topic-folder>/<tech>-code-guide-spring-boot.md`.
4. Lab README: `lab-code/<tech>-lab/README.md` hoặc README của service tương ứng.
## Nguồn tham khảo chuẩn cần đối chiếu

Các theory doc nên ưu tiên nguồn chính thức hoặc authoritative:

- PostgreSQL official docs cho transaction, index, EXPLAIN, isolation, VACUUM.
- Spring Boot và Spring Security official docs cho configuration, Actuator, Resource Server JWT.
- Keycloak official docs cho realm, client, role, user attribute, token claim.
- OpenID Connect Core, OAuth2 RFC và JWT RFC 7519 cho protocol/claim.
- Apache Kafka official docs cho topic, partition, offset, consumer group, producer/consumer.
- Elasticsearch official docs cho index, document, mapping, Query DSL, bulk indexing.
- MinIO docs và AWS S3 API docs cho bucket, object, object key, metadata, presigned URL.
- Redis official docs cho key/value, TTL và command behavior.
- Grafana Loki/Grafana Alloy docs cho labels, log collection, Explore và collector pipeline.
- Kong Gateway docs cho service, route, plugin và DB-less declarative config.
- OWASP chỉ dùng cho security best-practice context, không thay thế protocol docs.

## Không làm trong docs public

- Không lưu token, secret, password thật.
- Không lưu prompt thô hoặc draft answer.
- Không biến mỗi topic thành textbook dài nếu chưa có mini-lab hoặc demo liên quan.
- Không overclaim đây là production system. Repo là production-like local lab để học backend architecture.
