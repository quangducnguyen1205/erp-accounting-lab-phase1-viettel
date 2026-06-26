# Mục lục ghi chú kiến trúc

## Thư mục này chứa gì?

Thư mục này nối các mini-lab trong repo với target architecture rộng hơn. Mỗi nhóm công nghệ có folder riêng để người đọc thấy rõ boundary: gateway, security, service split, messaging, object storage, search, cache và observability.

## Thứ tự đọc tổng quan

1. [overview/](overview/) - bản đồ target architecture và cách các phần trong repo map vào kiến trúc.
2. [security/](security/) - Keycloak là Auth Service/Identity Provider; `common-security` là shared code module.
3. [kong-gateway/](kong-gateway/) - Kong Gateway DB-less trong final demo.
4. [api-gateway-service-discovery/](api-gateway-service-discovery/) - Spring Cloud Gateway và service discovery là legacy/awareness lab.
5. [microservice-boundaries/](microservice-boundaries/) - quyết định tách service trong Phase 1.5.
6. [messaging-kafka/](messaging-kafka/) - Kafka event flow: `tenant-demo` produce, `audit-log-service` và `search-service` consume.
7. [object-storage-minio/](object-storage-minio/) - `file-service`, MinIO object storage và PostgreSQL metadata.
8. [search-elasticsearch/](search-elasticsearch/) - `search-service`, Elasticsearch projection và admin reindex.
9. [cache-redis/](cache-redis/) - Redis cache-aside tenant-safe.
10. [observability/](observability/) - Actuator, metrics, Prometheus/Grafana metrics lab.
11. [log-aggregation-loki/](log-aggregation-loki/) - Loki/Grafana/Alloy centralized logs.
12. [kafka-ui/](kafka-ui/) - inspect topic/message/consumer group.
13. [../06-frontend/](../06-frontend/) - React Web UI `Master Data Portal`.
14. [awareness/](awareness/) - các chủ đề target architecture chưa implement trong Phase 1.

## Trạng thái Phase 1.5

| Nhóm | Trạng thái | Doc bắt đầu |
|---|---|---|
| Overview | Đã có bản đồ adoption | [overview/target-architecture-adoption-map.md](overview/target-architecture-adoption-map.md) |
| Security shared module | Đã tách `common-security`; không tạo runtime auth-service tự viết | [security/keycloak-vs-auth-service.md](security/keycloak-vs-auth-service.md) |
| Kong Gateway | Final demo dùng Kong DB-less | [kong-gateway/kong-gateway-foundation.md](kong-gateway/kong-gateway-foundation.md) |
| Spring Cloud Gateway | Giữ làm legacy learning lab | [api-gateway-service-discovery/api-gateway-foundation.md](api-gateway-service-discovery/api-gateway-foundation.md) |
| Microservice boundaries | Đã tách `audit-log-service`, `file-service`, `search-service` | [microservice-boundaries/README.md](microservice-boundaries/README.md) |
| Kafka/messaging | `tenant-demo` là producer; audit/search là consumers | [messaging-kafka/cross-service-kafka-flow.md](messaging-kafka/cross-service-kafka-flow.md) |
| MinIO/object storage | `file-service` upload/download tenant-aware | [object-storage-minio/file-service-code-walkthrough.md](object-storage-minio/file-service-code-walkthrough.md) |
| Elasticsearch/search | `search-service` projection + admin reindex tenant-scoped | [search-elasticsearch/cross-service-search-projection.md](search-elasticsearch/cross-service-search-projection.md) |
| Redis/cache | Cache-aside tenant-safe cho read path | [cache-redis/redis-cache-strategy.md](cache-redis/redis-cache-strategy.md) |
| Observability metrics | Local metrics lab, không phải production monitoring | [observability/observability-foundation.md](observability/observability-foundation.md) |
| Loki log aggregation | Alloy collect Docker stdout + host Java file logs | [log-aggregation-loki/loki-local-lab-config-walkthrough.md](log-aggregation-loki/loki-local-lab-config-walkthrough.md) |
| Kafka UI | Inspect `master-data-events`, `audit-log-service`, `search-service` lag | [kafka-ui/kafka-ui-local-lab-config-walkthrough.md](kafka-ui/kafka-ui-local-lab-config-walkthrough.md) |
| React Web UI | Business UI `Master Data Portal`, không phải architecture console | [../06-frontend/react-web-keycloak-gateway-demo.md](../06-frontend/react-web-keycloak-gateway-demo.md) |
| Awareness/later | Debezium, gRPC, realtime, LLM/external integrations | [awareness/README.md](awareness/README.md) |

## Mẫu source of truth cho mỗi công nghệ

- Concept/foundation doc: giải thích "là gì, vì sao dùng, khi nào không dùng".
- Shape/config/API doc: giải thích request/response/config/error shape nếu công nghệ có protocol/API riêng.
- Code guide doc: giải thích Spring Boot package/config/service/test shape.
- Config/code walkthrough doc: giải thích từng file config/code mới và runtime flow thực tế.
- Lab README: chỉ giữ lệnh chạy local và cleanup.
- Summary/report: ghi lại kết quả sau khi đã tự code/verify.

## Giới hạn hiện tại

Repo này là production-like local lab, chưa phải production platform:

- chưa có outbox;
- chưa có retry/DLT hoàn chỉnh;
- chưa có schema registry;
- chưa có Kubernetes/service discovery production;
- chưa có HA/secrets/TLS production;
- chưa có production file lifecycle/versioning.
