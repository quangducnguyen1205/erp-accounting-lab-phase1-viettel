# Architecture notes index

## Thư mục này chứa gì?

Thư mục này kết nối các lab nhỏ trong repo với kiến trúc target rộng hơn. Mục tiêu là biết phần nào đã implement, phần nào đã mini-lab, phần nào chỉ awareness và phần nào out of scope Phase 1.

## Reading order

1. `keycloak-in-target-architecture.md` - Keycloak/OIDC trong kiến trúc target.
2. `target-architecture-adoption-map.md` - adoption map cho React, Gateway, Keycloak, PostgreSQL, Redis, Kafka, Debezium, MinIO, Elastic, observability, LLM, integrations.

## Core

- Mapping `tenant-demo` sang backend service/resource server.
- Mapping PostgreSQL/Flyway sang service DB + migration.
- Mapping Keycloak mini-lab sang Authorization Server/OIDC.
- Phân biệt implemented / mini-lab / awareness / out of scope.

## Optional / later

- API Gateway/service discovery/load balancing chi tiết.
- Redis/MinIO/Elasticsearch mini-lab khi có feature trigger.
- Kafka/Debezium/observability awareness hoặc mini-lab nhỏ nếu mentor yêu cầu.
- DDD/domain boundaries sau khi demo backend đã đóng gói.

