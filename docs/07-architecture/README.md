# Architecture notes index

## Thư mục này chứa gì?

Thư mục này kết nối các lab nhỏ trong repo với kiến trúc target rộng hơn. Mục tiêu là biết phần nào đã implement, phần nào đã mini-lab, phần nào chỉ awareness và phần nào out of scope Phase 1.

## Reading order

1. `keycloak-in-target-architecture.md` - Keycloak/OIDC trong kiến trúc target.
2. `target-architecture-adoption-map.md` - adoption map cho React, Gateway, Keycloak, PostgreSQL, Redis, Kafka, Debezium, MinIO, Elastic, observability, LLM, integrations.
3. `elasticsearch-search-service.md` - vì sao/khi nào đưa search engine vào sau PostgreSQL query pattern.
4. `elasticsearch-mini-lab-plan.md` - kế hoạch mini-lab search nhỏ trên `master_data`.

## Core

- Mapping `tenant-demo` sang backend service/resource server.
- Mapping PostgreSQL/Flyway sang service DB + migration.
- Mapping Keycloak mini-lab sang Authorization Server/OIDC.
- Mapping Elasticsearch/search service sang search projection, không thay thế PostgreSQL source of truth.
- Phân biệt implemented / mini-lab / awareness / out of scope.

## Optional / later

- API Gateway/service discovery/load balancing chi tiết.
- Redis/MinIO mini-lab khi có feature trigger.
- Elasticsearch mini-lab hiện là nhánh kế tiếp vì nối tự nhiên từ PostgreSQL `LIKE`/index/query-pattern.
- Kafka/Debezium/observability awareness hoặc mini-lab nhỏ nếu mentor yêu cầu.
- DDD/domain boundaries sau khi demo backend đã đóng gói.
