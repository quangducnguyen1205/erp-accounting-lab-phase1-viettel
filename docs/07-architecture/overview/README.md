# Overview

## Thư mục này chứa gì?

Nhóm này là bản đồ tổng quan: target architecture có những thành phần nào, phần nào repo đã implement, phần nào mới mini-lab, phần nào chỉ awareness.

## Thứ tự đọc đề xuất

1. [target-architecture-adoption-map.md](target-architecture-adoption-map.md) - map target architecture vào implemented / mini-lab / awareness / out of scope.
2. [keycloak-in-target-architecture.md](keycloak-in-target-architecture.md) - vị trí Keycloak/OIDC/RBAC trong kiến trúc target.
3. [keycloak-local-persistence-and-bootstrap.md](keycloak-local-persistence-and-bootstrap.md) - vì sao Keycloak local cần DB bền và script bootstrap cho Phase 1.5 demo.
4. [keycloak-lab-config-walkthrough.md](keycloak-lab-config-walkthrough.md) - walkthrough `keycloak-lab/docker-compose.yml`, setup script và Makefile targets.

## Trạng Thái

- Target architecture adoption map: done.
- Keycloak/OIDC mapping: done.
- Keycloak local persistence/bootstrap: implemented for Phase 1.5 demo.
- API Gateway/service discovery, Debezium, gRPC, realtime, LLM/external integrations: để trong [../awareness/](../awareness/) hoặc milestone sau.

## Giới hạn hiện tại

Các sơ đồ target là định hướng học và demo, không có nghĩa Phase 1 đã triển khai full microservice architecture.
