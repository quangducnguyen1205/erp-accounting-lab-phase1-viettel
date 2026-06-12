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
7. [api-gateway-service-discovery/](api-gateway-service-discovery/) - API Gateway static route mini-lab + service discovery/load balancing awareness.
8. [log-aggregation-loki/](log-aggregation-loki/) - Phase 1.5 centralized logs bằng Loki/Grafana.
9. [kafka-ui/](kafka-ui/) - Phase 1.5 Kafka topic/message/consumer inspection.
10. [kong-gateway/](kong-gateway/) - Phase 1.5 Kong Gateway platform lab.
11. [microservice-boundaries/](microservice-boundaries/) - Phase 1.5 service split decision.
12. [../06-frontend/](../06-frontend/) - React Web UI demo mỏng để trình bày end-to-end.
13. [awareness/](awareness/) - các chủ đề target architecture chưa implement trong Phase 1.

Demo script cuối Phase 1: [../99-tong-ket/phase1-final-demo-script.md](../99-tong-ket/phase1-final-demo-script.md).

## Trạng Thái Phase 1

| Nhóm | Trạng thái | Doc bắt đầu |
|---|---|---|
| Overview | Done | [overview/target-architecture-adoption-map.md](overview/target-architecture-adoption-map.md) |
| Elasticsearch/search | Done | [search-elasticsearch/elasticsearch-search-service.md](search-elasticsearch/elasticsearch-search-service.md) |
| MinIO/object storage | Done, advanced optional later | [object-storage-minio/minio-object-storage.md](object-storage-minio/minio-object-storage.md) |
| Redis/cache | Done | [cache-redis/redis-cache-strategy.md](cache-redis/redis-cache-strategy.md) |
| Kafka/messaging | Done | [messaging-kafka/kafka-async-messaging.md](messaging-kafka/kafka-async-messaging.md) |
| Observability | Done at Phase 1 level | [observability/observability-foundation.md](observability/observability-foundation.md) |
| API Gateway/service discovery | Done at Phase 1 level: static route verified, service discovery awareness only | [api-gateway-service-discovery/api-gateway-foundation.md](api-gateway-service-discovery/api-gateway-foundation.md) |
| React Web UI | Done as thin final demo: Docker-first Keycloak/Gateway UI | [../06-frontend/react-web-keycloak-gateway-demo.md](../06-frontend/react-web-keycloak-gateway-demo.md) |
| Loki/log aggregation | Phase 1.5 local lab implemented | [log-aggregation-loki/loki-foundation.md](log-aggregation-loki/loki-foundation.md), [log-aggregation-loki/loki-local-lab-config-walkthrough.md](log-aggregation-loki/loki-local-lab-config-walkthrough.md) |
| Kafka UI | Phase 1.5 local lab implemented | [kafka-ui/kafka-ui-foundation.md](kafka-ui/kafka-ui-foundation.md), [kafka-ui/kafka-ui-local-lab-config-walkthrough.md](kafka-ui/kafka-ui-local-lab-config-walkthrough.md) |
| Kong Gateway | Phase 1.5 planned | [kong-gateway/kong-gateway-foundation.md](kong-gateway/kong-gateway-foundation.md) |
| Microservice boundaries | Phase 1.5 planned | [microservice-boundaries/phase1-service-split-options.md](microservice-boundaries/phase1-service-split-options.md) |
| Awareness/later | Planned | [awareness/README.md](awareness/README.md) |

## Source-of-truth Pattern Cho Mỗi Công Nghệ

- Concept/foundation doc: giải thích "là gì, vì sao dùng, khi nào không dùng".
- Shape/config/API doc: giải thích request/response/config/error shape nếu công nghệ có protocol/API riêng.
- Code guide doc: giải thích Spring Boot/package/config/service/test shape.
- Config/code walkthrough doc: giải thích từng file config/code mới và runtime flow thực tế.
- Lab README: chỉ giữ lệnh chạy local và cleanup.
- Summary/report: ghi lại kết quả sau khi đã tự code/verify.

Chuẩn chi tiết: [../99-tong-ket/theory-doc-writing-standard.md](../99-tong-ket/theory-doc-writing-standard.md).
