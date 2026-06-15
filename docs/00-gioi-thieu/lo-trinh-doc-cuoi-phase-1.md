# Lộ trình đọc cuối Phase 1 / Phase 1.5

## Mục tiêu

File này là tuyến đọc cuối trước khi review hoặc demo repo. Nó giúp đọc từ nền tảng SaaS/multi-tenant đến hệ thống local production-like hiện tại mà không bị lạc giữa các mini-lab lịch sử.

Repo này là learning lab, không phải production platform. Khi đọc, luôn tách ba lớp:

- lý thuyết backend có thể dùng lại;
- implementation local trong repo;
- caveat production chưa làm.

## Đọc gì trước?

Nếu chưa biết repo, đọc theo thứ tự:

1. [tong-quan-phase-1.md](tong-quan-phase-1.md)
2. [../README.md](../README.md)
3. [../../README.md](../../README.md)
4. [../99-tong-ket/phase1-final-demo-script.md](../99-tong-ket/phase1-final-demo-script.md)

Sau đó mới đi vào từng nhóm công nghệ bên dưới.

## Tuyến đọc cốt lõi

### 1. SaaS và multi-tenant

- [../01-saas/tong-quan-saas.md](../01-saas/tong-quan-saas.md)
- [../02-multi-tenant/tong-quan-multi-tenant.md](../02-multi-tenant/tong-quan-multi-tenant.md)
- [../02-multi-tenant/cac-mo-hinh-tenant-isolation.md](../02-multi-tenant/cac-mo-hinh-tenant-isolation.md)
- [../02-multi-tenant/tinh-huong-va-trade-off.md](../02-multi-tenant/tinh-huong-va-trade-off.md)

Điểm cần nắm: SaaS là delivery model; multi-tenant là kiến trúc phục vụ nhiều tenant trên một nền tảng; tenant isolation phải được enforce ở database/backend/auth/cache/log/search/file boundary.

### 2. PostgreSQL và database backend

- [../03-backend-database-mo-rong/README.md](../03-backend-database-mo-rong/README.md)
- [../03-backend-database-mo-rong/postgres-va-bai-toan-multi-tenant.md](../03-backend-database-mo-rong/postgres-va-bai-toan-multi-tenant.md)
- [../03-backend-database-mo-rong/index-va-query-tenant-aware.md](../03-backend-database-mo-rong/index-va-query-tenant-aware.md)
- [../03-backend-database-mo-rong/index-query-patterns-postgresql.md](../03-backend-database-mo-rong/index-query-patterns-postgresql.md)
- [../03-backend-database-mo-rong/migration-lock-rollback.md](../03-backend-database-mo-rong/migration-lock-rollback.md)
- [../03-backend-database-mo-rong/flyway-rollback-failure-handling.md](../03-backend-database-mo-rong/flyway-rollback-failure-handling.md)
- [../03-backend-database-mo-rong/acid-isolation-levels-postgresql.md](../03-backend-database-mo-rong/acid-isolation-levels-postgresql.md)

Điểm cần nắm: PostgreSQL là source of truth cho dữ liệu nghiệp vụ; query phải tenant-aware; index/migration/transaction ảnh hưởng trực tiếp đến an toàn multi-tenant.

### 3. Spring Boot tenant-aware API

- [../04-spring-boot/spring-boot-bootstrap-config.md](../04-spring-boot/spring-boot-bootstrap-config.md)
- [../04-spring-boot/jpa-entity-repository-tenant-aware.md](../04-spring-boot/jpa-entity-repository-tenant-aware.md)
- [../04-spring-boot/request-filter-threadlocal.md](../04-spring-boot/request-filter-threadlocal.md)
- [../04-spring-boot/service-controller-curl-flow.md](../04-spring-boot/service-controller-curl-flow.md)
- [../04-spring-boot/testing-tenant-isolation.md](../04-spring-boot/testing-tenant-isolation.md)

Điểm cần nắm: Resource Server validate token, filter set tenant context, service/repository dùng tenant context để query source of truth.

### 4. Keycloak, OIDC, JWT, RBAC

- [../05-security/README.md](../05-security/README.md)
- [../05-security/oauth2-jwt-resource-server-concepts.md](../05-security/oauth2-jwt-resource-server-concepts.md)
- [../05-security/spring-security-core-concepts.md](../05-security/spring-security-core-concepts.md)
- [../05-security/keycloak-oidc-mental-model.md](../05-security/keycloak-oidc-mental-model.md)
- [../05-security/keycloak-authorization-rbac-tenant-scope.md](../05-security/keycloak-authorization-rbac-tenant-scope.md)
- [../07-architecture/security/keycloak-vs-auth-service.md](../07-architecture/security/keycloak-vs-auth-service.md)
- [../07-architecture/security/common-security-code-walkthrough.md](../07-architecture/security/common-security-code-walkthrough.md)

Điểm cần nắm: Keycloak là Auth Service/Identity Provider; backend services là Resource Server; không tạo runtime auth-service tự viết; `common-security` chỉ là shared library để tránh duplicate security plumbing.

### 5. Gateway boundary

- [../07-architecture/kong-gateway/kong-gateway-foundation.md](../07-architecture/kong-gateway/kong-gateway-foundation.md)
- [../07-architecture/kong-gateway/kong-local-lab-config-walkthrough.md](../07-architecture/kong-gateway/kong-local-lab-config-walkthrough.md)
- [../07-architecture/api-gateway-service-discovery/api-gateway-foundation.md](../07-architecture/api-gateway-service-discovery/api-gateway-foundation.md)

Điểm cần nắm: final demo dùng Kong Gateway; Spring Cloud Gateway được giữ như legacy concept lab. Gateway route request, backend vẫn validate JWT và enforce tenant/role.

### 6. Service boundaries

- [../07-architecture/microservice-boundaries/README.md](../07-architecture/microservice-boundaries/README.md)
- [../07-architecture/microservice-boundaries/audit-log-service-split-plan.md](../07-architecture/microservice-boundaries/audit-log-service-split-plan.md)
- [../07-architecture/object-storage-minio/file-service-split-plan.md](../07-architecture/object-storage-minio/file-service-split-plan.md)
- [../07-architecture/search-elasticsearch/search-service-split-plan.md](../07-architecture/search-elasticsearch/search-service-split-plan.md)

Điểm cần nắm: `tenant-demo`, `audit-log-service`, `file-service`, `search-service` có DB/API/security boundary riêng; không share JPA entity/repository giữa services.

### 7. Kafka event flow

- [../07-architecture/messaging-kafka/README.md](../07-architecture/messaging-kafka/README.md)
- [../07-architecture/messaging-kafka/kafka-async-messaging.md](../07-architecture/messaging-kafka/kafka-async-messaging.md)
- [../07-architecture/messaging-kafka/kafka-event-shapes.md](../07-architecture/messaging-kafka/kafka-event-shapes.md)
- [../07-architecture/messaging-kafka/cross-service-kafka-flow.md](../07-architecture/messaging-kafka/cross-service-kafka-flow.md)

Điểm cần nắm: `tenant-demo` publish `MasterDataChangedEvent`; `audit-log-service` và `search-service` consume. Chưa có outbox/retry/DLT/schema registry.

### 8. Audit log, file storage, search projection

- [../07-architecture/microservice-boundaries/audit-log-service-code-walkthrough.md](../07-architecture/microservice-boundaries/audit-log-service-code-walkthrough.md)
- [../07-architecture/object-storage-minio/minio-object-storage.md](../07-architecture/object-storage-minio/minio-object-storage.md)
- [../07-architecture/object-storage-minio/file-service-code-walkthrough.md](../07-architecture/object-storage-minio/file-service-code-walkthrough.md)
- [../07-architecture/search-elasticsearch/elasticsearch-search-service.md](../07-architecture/search-elasticsearch/elasticsearch-search-service.md)
- [../07-architecture/search-elasticsearch/cross-service-search-projection.md](../07-architecture/search-elasticsearch/cross-service-search-projection.md)

Điểm cần nắm:

- Audit log là tenant-aware read model cho activity.
- File metadata nằm ở PostgreSQL, binary object nằm ở MinIO.
- Elasticsearch là projection, không phải source of truth.
- Reindex admin endpoint rebuild projection cho tenant hiện tại, không expose trên React UI.

### 9. Redis cache-aside

- [../07-architecture/cache-redis/redis-cache-strategy.md](../07-architecture/cache-redis/redis-cache-strategy.md)
- [../07-architecture/cache-redis/redis-code-guide-spring-boot.md](../07-architecture/cache-redis/redis-code-guide-spring-boot.md)

Điểm cần nắm: cache là bản sao tạm; source of truth vẫn là PostgreSQL; key phải tenant-aware; TTL không thay thế invalidation.

### 10. Observability và log aggregation

- [../07-architecture/observability/observability-foundation.md](../07-architecture/observability/observability-foundation.md)
- [../07-architecture/log-aggregation-loki/loki-foundation.md](../07-architecture/log-aggregation-loki/loki-foundation.md)
- [../07-architecture/log-aggregation-loki/how-to-read-logs-in-grafana.md](../07-architecture/log-aggregation-loki/how-to-read-logs-in-grafana.md)
- [../07-architecture/log-aggregation-loki/grafana-loki-ui-screenshot-guide.md](../07-architecture/log-aggregation-loki/grafana-loki-ui-screenshot-guide.md)
- [../07-architecture/kafka-ui/kafka-ui-local-lab-config-walkthrough.md](../07-architecture/kafka-ui/kafka-ui-local-lab-config-walkthrough.md)

Điểm cần nắm: metrics, logs và tracing là observability signals khác nhau. Final demo dùng Alloy tail Java file logs + Docker stdout logs vào Loki. Kafka UI dùng để inspect topic/consumer group, không thay Loki.

### 11. React Web UI demo

- [../06-frontend/react-web-keycloak-gateway-demo.md](../06-frontend/react-web-keycloak-gateway-demo.md)

Điểm cần nắm: UI là `Master Data Portal`, một business UI nhỏ. UI không gọi trực tiếp PostgreSQL/Redis/Kafka/MinIO/Elasticsearch/Loki/Grafana.

## Đọc gì trước khi chạy demo?

1. File này.
2. [../99-tong-ket/phase1-final-demo-script.md](../99-tong-ket/phase1-final-demo-script.md)
3. [../../lab-code/README.md](../../lab-code/README.md)
4. [../../lab-code/README-http-client.md](../../lab-code/README-http-client.md)
5. [../07-architecture/log-aggregation-loki/how-to-read-logs-in-grafana.md](../07-architecture/log-aggregation-loki/how-to-read-logs-in-grafana.md)

## Legacy learning labs

Các phần sau không phải main final demo path, nhưng vẫn hữu ích để học từng công nghệ:

- `lab-code/sql-playground/`
- `lab-code/flyway-failure-lab/`
- `lab-code/gateway-demo/`
- `lab-code/observability-lab/`
- `lab-code/Makefile.legacy`
- `presentation-notes/`
- `reports/`

## Giới hạn cần nói rõ

- Chưa có outbox.
- Chưa có retry/DLT hoàn chỉnh.
- Chưa có schema registry.
- Chưa có Kubernetes/service discovery production.
- Chưa có production HA/secrets/TLS.
- Chưa có production file lifecycle/versioning.
- Demo dùng local credential và local ports; không dùng cho môi trường thật.
