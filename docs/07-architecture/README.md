# Architecture notes index

## Thư mục này chứa gì?

Thư mục này kết nối các lab nhỏ trong repo với kiến trúc target rộng hơn. Mục tiêu là biết phần nào đã implement, phần nào đã mini-lab, phần nào chỉ awareness và phần nào out of scope Phase 1.

## Reading order

1. `target-architecture-adoption-map.md` - adoption map cho React, Gateway, Keycloak, PostgreSQL, Redis, Kafka, Debezium, MinIO, Elastic, observability, LLM, integrations.
2. `keycloak-in-target-architecture.md` - Keycloak/OIDC trong kiến trúc target.
3. `elasticsearch-search-service.md` - foundation: search engine, index, document, mapping, analyzer, Query DSL, tenant-aware search.
4. `elasticsearch-request-response-shapes.md` - REST API, request/response/error shape và cách đọc lỗi.
5. `elasticsearch-design-patterns-spring-boot.md` - vì sao tách Controller/Service/Gateway/Document.
6. `elasticsearch-code-guide-spring-boot.md` - code shape Spring Boot cho search mini-lab.
7. `elasticsearch-mini-lab-plan.md` - checklist mini-lab search nhỏ trên `master_data`.
8. `minio-object-storage.md` - foundation: object storage, bucket, object key, metadata, tenant-aware file storage.
9. `minio-s3-api-shapes.md` - S3/MinIO operations, request/response/error shape.
10. `minio-code-guide-spring-boot.md` - Spring Boot integration pattern cho file upload/download.
11. `minio-admin-console-guide.md` - thao tác MinIO Console local.
12. `minio-advanced-object-management.md` - backlog optional: presigned URL expiry, lifecycle, versioning, object lock/retention.
13. `redis-cache-strategy.md` - foundation: Redis, cache-aside, TTL, invalidation, tenant-safe key.
14. `redis-code-guide-spring-boot.md` - Spring Boot integration pattern cho Redis cache mini-lab.
15. `redis-mini-lab-plan.md` - checklist mini-lab cache nhỏ, hit/miss và Redis CLI.
16. `kafka-async-messaging.md` - foundation: Kafka, topic/partition/offset, producer/consumer, async messaging.
17. `kafka-event-shapes.md` - record/event shape, tenant context, key/header và duplicate/idempotency fields.
18. `kafka-code-guide-spring-boot.md` - Spring Boot integration pattern cho producer/consumer mini-lab.
19. `kafka-mini-lab-plan.md` - checklist tự code Kafka event flow nhỏ.

## Core

- Mapping `tenant-demo` sang backend service/resource server.
- Mapping PostgreSQL/Flyway sang service DB + migration.
- Mapping Keycloak mini-lab sang Authorization Server/OIDC.
- Mapping Elasticsearch/search service sang search projection, không thay thế PostgreSQL source of truth.
- Phân biệt implemented / mini-lab / awareness / out of scope.

## Optional / later

- API Gateway/service discovery/load balancing chi tiết.
- Redis mini-lab đã đóng: học tenant-safe cache key, TTL và cache-aside.
- MinIO advanced object management chỉ làm sau core demo/UI hoặc sau các milestone công nghệ chính, không chặn Redis.
- Kafka mini-lab là task hiện tại: chuẩn bị event producer/consumer nhỏ, không làm Debezium/full event platform.
- Elasticsearch mini-lab đã đóng vì nối tự nhiên từ PostgreSQL `LIKE`/index/query-pattern.
- Debezium/observability awareness hoặc mini-lab nhỏ sau Kafka.
- DDD/domain boundaries sau khi demo backend đã đóng gói.

## Source-of-truth pattern cho mỗi công nghệ

- Concept/theory doc: giải thích "là gì, vì sao dùng, khi nào không dùng".
- Request/response/config/API shape doc: giải thích dữ liệu, protocol, lỗi và cách debug nếu công nghệ đó có API riêng.
- Code guide doc: giải thích Spring Boot/package/config/service/test shape.
- Lab README: chỉ giữ lệnh chạy local và cleanup.
- Summary/report: ghi lại kết quả sau khi đã tự code/verify.

Chuẩn chi tiết: `../99-tong-ket/theory-doc-writing-standard.md`.
