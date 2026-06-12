# Target architecture adoption map

## Mục tiêu

Tài liệu này map sơ đồ kiến trúc target vào repo Phase 1: phần nào đã làm, phần nào đã mini-lab, phần nào chỉ cần awareness, và phần nào nên học just-in-time sau.

Nguyên tắc:

- Không biến Phase 1 thành full production microservices stack.
- Công nghệ nào có feature thật mới học sâu hơn.
- Với mỗi công nghệ, phải biết vai trò kiến trúc, trigger để học tiếp, rủi ro nếu làm quá sớm.

## Bức tranh target

Sơ đồ target có các nhóm chính:

- Frontend: React app.
- Entry/security: API Gateway, service discovery, load balancing, Keycloak/OIDC.
- Backend services: tenant, account, authorization/RBAC, notification, file, search, business services.
- Database/storage: PostgreSQL cluster/service DBs, MinIO.
- Integration/event: Kafka, Debezium CDC, external systems.
- Search/cache/realtime: Elasticsearch, Redis, SignalR/WebSocket/SSE/long polling.
- Observability: Prometheus, Grafana, Loki.
- AI/LLM providers: OpenAI, OpenRouter, provider khác.

Repo hiện tại mới implement một lát cắt nhỏ nhưng đúng hướng:

```text
Spring Boot tenant-demo
-> PostgreSQL/Flyway
-> tenant-aware API
-> DataLeakageTest
-> JWT tạm
-> Keycloak mini-lab / APP_AUTH_MODE=keycloak
-> Keycloak Authorization/RBAC mini-lab
-> Elasticsearch, MinIO, Redis, Kafka, Observability local mini-labs
-> Spring Cloud Gateway static route
-> React Web UI demo Docker-first
```

Sau feedback mentor ngày 11/06/2026, Phase 1.5 sẽ đưa demo gần target hơn bằng Loki log aggregation, Kafka UI, Kong Gateway và một service split nhỏ (`audit-log-service`).

## Adoption map theo công nghệ

| Topic | Vai trò trong kiến trúc | Khi repo nên học/dùng | Mini-lab trigger | Trạng thái hiện tại | Rủi ro overdo |
|---|---|---|---|---|---|
| React Web frontend | UI web mỏng cho user thao tác, login Keycloak và gọi API qua Gateway. | Đã dùng làm final demo trực quan sau khi backend/gateway ổn. | Login Keycloak, ACCOUNTANT load/create `master_data`, VIEWER load nhưng create `403`, hiển thị requestId để đối chiếu log. | Verified Docker-first ở `lab-code/web-ui-demo`. | Dễ sa vào UI/state/CSS; không dùng React Native/Expo trong repo này. |
| API Gateway / service discovery / load balancing | Entry point, routing, auth pre-check, rate limit, service discovery. | Đã thêm mini-lab static route để hiểu gateway flow; discovery/load balancing vẫn awareness. | Route `/api/**` qua gateway đến `tenant-demo`, forward `Authorization` và `X-Request-Id`. | Static route mini-lab verified. | Chạy gateway production/service discovery thật quá sớm sẽ tốn setup, ít giá trị Phase 1. |
| Keycloak/OIDC | Authentication, login/token issuer, issuer/JWKS, user identity claims. | Đã chạm auth nên đã làm mini-lab. | User/token/tenant claim flow, Resource Server validation. | Keycloak mini-lab verified. | Overclaim production IAM quá sớm. |
| Keycloak Authorization/RBAC/tenant-scope | Authorization: user được phép làm gì, role/scope/authority, service/business permission. | Đã làm sau feedback mentor Đạt ngày 25/05. | Realm roles vs client roles, role claim, Spring Security authorities, 401 vs 403, tenant-scope. | Mini-lab verified. | Làm full permission matrix hoặc Keycloak Authorization Services/UMA quá sớm. |
| Spring Boot backend services | Business APIs, Resource Server, tenant-aware service/repository. | Core demo hiện tại. | Thêm service khi có domain slice mới. | `tenant-demo` đã có API nhỏ. | Biến demo thành ERP thật quá sớm. |
| PostgreSQL shared-table | Lưu business data nhiều tenant trong shared table có `tenant_id`. | Core nền tảng. | Query plan, leakage, transaction, migration. | Đã học SQL/Flyway/ACID/index pattern. | Chỉ thêm index mà không hiểu query pattern/locking. |
| PostgreSQL service databases | Mỗi service có DB/schema riêng trong target. | Khi học service boundary hoặc DDD. | So sánh shared table demo với service DB concept. | Awareness. | Tách DB thật khi chưa có nhiều service sẽ phức tạp. |
| Elasticsearch/search | Search text, indexing, query search service. | Đã học sau PostgreSQL query-pattern. | Index `master_data`, search keyword, tenant filter trong query. | Mini-lab verified. | Làm Elastic thành production search platform quá sớm. |
| MinIO object storage | Lưu file qua S3 API: hóa đơn, chứng từ, attachment. | Đã học storage slice sau search. | Upload/download file, store metadata tenant-aware. | Mini-lab verified; advanced object management optional later. | File security/ACL/presigned URL phức tạp nếu làm sâu. |
| Redis cache | Cache dữ liệu/config/feature flags, giảm load DB. | Đã học sau MinIO. | Tenant-safe cache key: `tenant:{id}:...`, cache-aside by code. | Mini-lab verified. | Cache leakage nếu key thiếu tenant; cache trước khi có bottleneck. |
| Kafka async messaging | Event/message giữa services, decouple async workflow. | Đã học sau cache/storage. | Publish `MasterDataChangedEvent`, consumer log. | Mini-lab verified. | Chạy Kafka chỉ để “có Kafka” rất nặng. |
| Kafka UI | Inspect topic/message/consumer group/lag. | Phase 1.5, trước khi tách service để debug Kafka rõ hơn. | Mở topic `master-data-events`, xem key/value và consumer group. | Local lab implemented. | Nhầm Kafka UI với monitoring/alerting production. |
| Loki/log aggregation | Centralized logs cho nhiều service. | Phase 1.5, trước khi có nhiều service/log terminal. | Tìm log theo service/container/requestId text trong Grafana Explore. | Local lab implemented với Loki + Grafana + Alloy. | Dùng high-cardinality labels hoặc log token/body. |
| Kong Gateway | Gateway platform gần target architecture hơn Spring Cloud Gateway lab. | Phase 1.5 sau khi đã hiểu gateway concept. | DB-less route `/api/master-data/**`, sau này `/api/audit/**`. | Local lab implemented. | Đưa business logic hoặc expose Admin API public. |
| Microservice boundary split | Tách responsibility/service ownership rõ ràng. | Phase 1.5 khi cần Kafka cross-service và nhiều service logs. | Thêm `audit-log-service` consume `MasterDataChangedEvent`. | Planned. | Split artificial hoặc tạo domain mới quá phức tạp. |
| Debezium CDC | Đồng bộ thay đổi DB sang Kafka/search/reporting. | Sau khi hiểu Kafka và search. | Awareness diagram hoặc read-only CDC note. | Later awareness. | Setup CDC phức tạp, dễ lệch Phase 1. |
| gRPC internal communication | Giao tiếp service-to-service typed/efficient. | Khi so sánh REST vs internal RPC. | Chỉ note/diagram, chưa cần code. | Awareness. | Thêm IDL/protobuf khi chỉ có một service. |
| Realtime: SignalR/WebSocket/SSE/long polling | Notification/live updates tới frontend. | Khi có notification/progress update feature. | Compare SSE vs WebSocket ở mức note. | Chưa làm. | Realtime infra dễ phình scope. |
| Observability: Actuator/Micrometer/Prometheus/Grafana | Health, logs, metrics, local dashboard để hiểu vận hành backend. | Đã học sau Kafka. | Actuator endpoint, request logging, custom metrics, Prometheus scrape, Grafana datasource/dashboard. | Mini-lab verified ở Phase 1 level. | Overclaim production monitoring; chưa có tracing/log aggregation/alerting. |
| LLM providers | AI agents/integration với OpenAI/OpenRouter. | Khi có use case AI rõ trong nghiệp vụ. | Architecture note về boundary/secrets/logging. | Chưa làm. | Gọi API thật khi chưa có requirement, rủi ro secret/cost. |
| External integrations | e-contract, eCommerce, CRM, HR, documents, digital signing. | Khi cần hiểu boundary ERP/SME ecosystem. | Draw integration boundary + failure/retry awareness. | Awareness từ sơ đồ target. | Mock quá nhiều hệ ngoài, mất trọng tâm. |
| DDD/domain boundaries | Thiết kế domain/service boundary, aggregate, module. | Sau khi demo backend chạy ổn và cần review design. | Awareness note + refactor discussion, không bắt buộc code. | Later. | Refactor DDD khi domain demo còn quá nhỏ. |

## Current lab mapping

| Current artifact | Architecture concept | Status | Evidence |
|---|---|---|---|
| `lab-code/tenant-demo` | Spring Boot backend service / Resource Server | Implemented | App có API `master_data`, SecurityConfig, Keycloak mode. |
| Flyway `V1-V3` | Service DB migration baseline | Implemented | App startup/Flyway validation, PostgreSQL schema. |
| `TenantContext` + `JwtTenantContextFilter` | Request-scoped tenant context | Implemented | Token/header context được đưa vào service layer. |
| `MasterDataRepository` | Tenant-aware data access | Implemented | Method explicit có `tenantId`, không dùng query thiếu tenant. |
| `DataLeakageTest` | Regression guard chống leakage | Implemented | `make app-test` pass với tenant isolation cases. |
| `lab-code/keycloak-lab` | Authorization Server/OIDC mini-lab | Mini-labbed + persistent local setup | Keycloak issue token, `tenant_id` claim, issuer/JWKS; PostgreSQL volume + bootstrap script for demo reproducibility. |
| `APP_AUTH_MODE=keycloak` | Resource Server validate Keycloak token | Verified | Keycloak token gọi API tenant-aware thành công. |
| `com.viettel.demo.search` | Elasticsearch search projection | Verified | Reindex/search `master_data`, query luôn filter tenantId. |
| `com.viettel.demo.storage` | Object storage + metadata source of truth | Verified | Upload/download qua backend, metadata PostgreSQL tenant-aware, object trong MinIO. |
| `com.viettel.demo.cache` | Redis cache-aside | Verified | Read-by-code miss -> DB -> TTL -> hit, key có tenantId. |
| `com.viettel.demo.messaging` | Kafka async event propagation | Verified | Publish/consume `MasterDataChangedEvent`, event có tenantId và tenant-aware key. |
| `com.viettel.demo.observability` + `lab-code/observability-lab` | Logging/metrics/local monitoring | Verified | RequestId/MDC, custom Micrometer metrics, Prometheus target UP, Grafana datasource/dashboard. |
| `lab-code/gateway-demo` | API Gateway/static routing | Verified | Spring Cloud Gateway route `/api/**` đến `tenant-demo`, service discovery để awareness. |
| `lab-code/web-ui-demo` | React Web thin client | Verified | Docker-first Vite app; Keycloak login bằng public client, gọi Gateway `/api/master-data`, lookup by code, create data và hiển thị requestId. |
| `lab-code/loki-lab` | Loki log aggregation lab | Local lab implemented | Docker Compose Loki + Grafana + Alloy, Makefile targets `loki-*`. |
| `lab-code/kafka-ui-lab` | Kafka UI inspection lab | Local lab implemented | Docker Compose + Makefile targets, connects to Kafka via `viettel-kafka-net`. |
| `lab-code/kong-gateway-lab` | Kong Gateway lab | Local lab implemented | DB-less Kong, route `/api/master-data` và health route đến `tenant-demo`, Makefile targets `kong-*`. |
| Future `audit-log-service` | Second backend service / Kafka consumer | Planned | Recommended split để Kafka thành cross-service flow thật. |
| Keycloak Authorization/RBAC task | Authorization layer sau AuthN | Verified | Role/authority check nhỏ, phân biệt `401`/`403`, vẫn giữ tenant-aware query. |
| `presentation-notes/demo-script-keycloak-tenant-flow.md` | Mentor-facing demo path | Prepared | Script start DB/Keycloak/app, verify tenant 1/2, cross-tenant id. |
| SQL playground `01-09` | PostgreSQL learning lab | Implemented | Schema, EXPLAIN, index pattern, migration, ACID/isolation. |
| `docs/07-architecture/overview/keycloak-in-target-architecture.md` | Security architecture mapping | Done | Map Keycloak/OIDC vào target architecture. |

## Core vs optional trong Phase 1

### Core đã đủ để demo

- Spring Boot backend service.
- PostgreSQL + Flyway.
- Shared-table tenant isolation.
- Tenant-aware repository/service.
- JWT tạm + Keycloak/OIDC mini-lab.
- Keycloak Authorization/RBAC/tenant-scope để phân biệt AuthN, AuthZ và data isolation.
- Regression test chống leakage.
- Demo script mentor-facing.
- Elasticsearch, MinIO, Redis, Kafka và Observability mini-labs đã đủ ở mức Phase 1 để giải thích vai trò target architecture.

### Mini-lab nên làm khi có trigger

- Elasticsearch: đã hoàn thành mini-lab vì nối tự nhiên từ PostgreSQL `LIKE`/index/query pattern.
- MinIO: đã hoàn thành mini-lab upload/download tenant-aware; advanced object management để optional later.
- Redis: đã hoàn thành cache-aside mini-lab; eviction/update-delete invalidation là caveat.
- Kafka: đã hoàn thành event producer/consumer mini-lab; Debezium CDC vẫn để awareness/later.
- Observability: đã hoàn thành Actuator/logging/custom metrics/Prometheus/Grafana local lab.

### Awareness là đủ cho Phase 1

- Service discovery/load balancing production-grade.
- gRPC internal communication.
- Realtime protocols.
- External integrations.
- LLM provider integration.
- DDD/domain boundaries.

### Out of scope Phase 1

- Full production microservices.
- Full ERP/accounting domain.
- Full Keycloak/RBAC platform.
- Production-grade Kafka/Debezium/Elastic/Grafana stack chạy đồng thời như hệ vận hành thật.
- Production deployment/HA/cluster/security hardening đầy đủ.

## Quy tắc học just-in-time

Trước khi thêm công nghệ mới, hỏi:

1. Công nghệ này giải quyết feature hoặc rủi ro nào trong demo?
2. Có thể chứng minh bằng mini-lab nhỏ không?
3. Có cần chạy thật, hay awareness note là đủ?
4. Nếu chạy thật, verification command là gì?
5. Rủi ro data leakage/secret leakage/over-engineering là gì?

Nếu chưa trả lời được 5 câu này, chưa nên implement công nghệ đó.

## Đề xuất hướng tiếp theo

Hướng tốt nhất sau tài liệu này là đi theo Phase 1.5:

1. `audit-log-service` split.
2. Kafka cross-service verification.
3. Final React Web demo polish sau khi backend boundaries ổn.

Loki/Grafana log aggregation, Kafka UI và Kong Gateway đã có local lab. Không nên mở UI lớn hoặc domain kế toán mới trước khi gateway/service boundary rõ.
