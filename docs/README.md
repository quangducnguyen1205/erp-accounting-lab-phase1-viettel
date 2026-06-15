# Mục lục tài liệu

## Mục tiêu

Thư mục `docs/` là source of truth cho kiến thức Phase 1 và Phase 1.5 đã được chuẩn hóa. Local notes, prompt thô, câu hỏi nháp và review nháp không nằm ở đây.

Nếu chỉ có thời gian đọc một đường duy nhất, bắt đầu từ:

- [00-gioi-thieu/lo-trinh-doc-cuoi-phase-1.md](00-gioi-thieu/lo-trinh-doc-cuoi-phase-1.md)

## Thứ tự đọc đề xuất

1. [00-gioi-thieu/](00-gioi-thieu/) - bối cảnh Phase 1, phạm vi học và lộ trình đọc cuối.
2. [01-saas/](01-saas/) - SaaS, cloud, subscription, on-premise.
3. [02-multi-tenant/](02-multi-tenant/) - multi-tenant, tenant isolation và trade-off.
4. [03-backend-database-mo-rong/](03-backend-database-mo-rong/) - PostgreSQL, index, migration, transaction.
5. [04-spring-boot/](04-spring-boot/) - Spring Boot, JPA, filter, repository/service/controller, test.
6. [05-security/](05-security/) - JWT, Spring Security Resource Server, Keycloak/OIDC/RBAC.
7. [07-architecture/](07-architecture/) - gateway, service boundary, Kafka, MinIO, Elasticsearch, Redis, observability.
8. [06-frontend/](06-frontend/) - React Web UI demo `Master Data Portal`.
9. [99-tong-ket/](99-tong-ket/) - tổng kết, demo script cuối và giới hạn production-like.

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
5. Summary sau khi xong: `docs/99-tong-ket/nhung-gi-da-nam-duoc.md`.

Template chung: [99-tong-ket/technology-mini-lab-template.md](99-tong-ket/technology-mini-lab-template.md).

Chuẩn viết theory doc chi tiết:
[99-tong-ket/theory-doc-writing-standard.md](99-tong-ket/theory-doc-writing-standard.md).

Demo script cuối Phase 1.5:
[99-tong-ket/final-production-like-demo-script.md](99-tong-ket/final-production-like-demo-script.md).

Tài liệu rehearsal và Q&A:

- [99-tong-ket/final-demo-evidence-checklist.md](99-tong-ket/final-demo-evidence-checklist.md)
- [99-tong-ket/final-demo-operational-qna.md](99-tong-ket/final-demo-operational-qna.md)
- [99-tong-ket/final-demo-rehearsal-transcript.md](99-tong-ket/final-demo-rehearsal-transcript.md)

Plan Phase 1.5:
[99-tong-ket/phase1-5-production-like-demo-plan.md](99-tong-ket/phase1-5-production-like-demo-plan.md).

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
