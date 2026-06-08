# Architecture Notes Index

## Thư mục này chứa gì?

Thư mục này nối các mini-lab trong repo với target architecture rộng hơn. Mỗi nhóm công nghệ có folder riêng để tránh việc `docs/07-architecture` bị phẳng quá nhiều file ngang hàng.

## Reading Order Tổng Quan

1. [overview/](overview/) - bản đồ target architecture và cách Keycloak/OIDC map vào kiến trúc.
2. [search-elasticsearch/](search-elasticsearch/) - Elasticsearch/search projection mini-lab.
3. [object-storage-minio/](object-storage-minio/) - MinIO/S3 object storage mini-lab.
4. [cache-redis/](cache-redis/) - Redis tenant-safe cache-aside mini-lab.
5. [messaging-kafka/](messaging-kafka/) - Kafka/async messaging mini-lab.
6. [observability/](observability/) - Actuator, request logging, Micrometer metrics, Prometheus/Grafana local lab.
7. [awareness/](awareness/) - các chủ đề target architecture chưa implement trong Phase 1.

## Trạng Thái Phase 1

| Nhóm | Trạng thái | Doc bắt đầu |
|---|---|---|
| Overview | Done | [overview/target-architecture-adoption-map.md](overview/target-architecture-adoption-map.md) |
| Elasticsearch/search | Done | [search-elasticsearch/elasticsearch-search-service.md](search-elasticsearch/elasticsearch-search-service.md) |
| MinIO/object storage | Done, advanced optional later | [object-storage-minio/minio-object-storage.md](object-storage-minio/minio-object-storage.md) |
| Redis/cache | Done | [cache-redis/redis-cache-strategy.md](cache-redis/redis-cache-strategy.md) |
| Kafka/messaging | Done | [messaging-kafka/kafka-async-messaging.md](messaging-kafka/kafka-async-messaging.md) |
| Observability | Done at Phase 1 level | [observability/observability-foundation.md](observability/observability-foundation.md) |
| Awareness/later | Planned | [awareness/README.md](awareness/README.md) |

## Source-of-truth Pattern Cho Mỗi Công Nghệ

- Concept/foundation doc: giải thích "là gì, vì sao dùng, khi nào không dùng".
- Shape/config/API doc: giải thích request/response/config/error shape nếu công nghệ có protocol/API riêng.
- Code guide doc: giải thích Spring Boot/package/config/service/test shape.
- Lab README: chỉ giữ lệnh chạy local và cleanup.
- Summary/report: ghi lại kết quả sau khi đã tự code/verify.

Chuẩn chi tiết: [../99-tong-ket/theory-doc-writing-standard.md](../99-tong-ket/theory-doc-writing-standard.md).
