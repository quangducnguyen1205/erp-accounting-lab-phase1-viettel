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
```

## Adoption map theo công nghệ

| Topic | Vai trò trong kiến trúc | Khi repo nên học/dùng | Mini-lab trigger | Trạng thái hiện tại | Rủi ro overdo |
|---|---|---|---|---|---|
| React frontend | UI cho user thao tác, gọi API bằng token. | Sau khi backend demo script ổn. | Cần minh họa tenant 1/2 bằng UI. | Chưa làm. | Dễ sa vào UI/state/CSS, lệch mục tiêu backend. |
| API Gateway / service discovery / load balancing | Entry point, routing, auth pre-check, rate limit, service discovery. | Sau khi có nhiều hơn một service hoặc cần giải thích target architecture. | Tạo note/diagram request flow qua gateway. | Awareness trong roadmap. | Chạy gateway thật quá sớm sẽ tốn setup, ít giá trị Phase 1. |
| Keycloak/OIDC/RBAC | Identity, login, token issuer, role/scope/claim source. | Đã chạm auth nên đã làm mini-lab. RBAC sâu để sau. | User/tenant/role flow, token claim, Resource Server validation. | Keycloak mini-lab verified. | Overclaim production IAM, role matrix quá sớm. |
| Spring Boot backend services | Business APIs, Resource Server, tenant-aware service/repository. | Core demo hiện tại. | Thêm service khi có domain slice mới. | `tenant-demo` đã có API nhỏ. | Biến demo thành ERP thật quá sớm. |
| PostgreSQL shared-table | Lưu business data nhiều tenant trong shared table có `tenant_id`. | Core nền tảng. | Query plan, leakage, transaction, migration. | Đã học SQL/Flyway/ACID/index pattern. | Chỉ thêm index mà không hiểu query pattern/locking. |
| PostgreSQL service databases | Mỗi service có DB/schema riêng trong target. | Khi học service boundary hoặc DDD. | So sánh shared table demo với service DB concept. | Awareness. | Tách DB thật khi chưa có nhiều service sẽ phức tạp. |
| Redis cache | Cache dữ liệu/config/feature flags, giảm load DB. | Khi có query lặp lại hoặc tenant config/feature flag. | Tenant-safe cache key: `tenant:{id}:...`. | Chưa làm. | Cache leakage nếu key thiếu tenant; cache trước khi có bottleneck. |
| Kafka async messaging | Event/message giữa services, decouple async workflow. | Khi có nghiệp vụ bất đồng bộ thật. | Publish event đơn giản, consumer log. | Awareness. | Chạy Kafka chỉ để “có Kafka” rất nặng. |
| Debezium CDC | Đồng bộ thay đổi DB sang Kafka/search/reporting. | Khi học data sync/search/reporting pipeline. | Awareness diagram hoặc read-only CDC note. | Chưa làm. | Setup CDC phức tạp, dễ lệch Phase 1. |
| MinIO object storage | Lưu file qua S3 API: hóa đơn, chứng từ, attachment. | Khi có file upload/download feature. | Upload file local, store metadata tenant-aware. | Chưa làm. | File security/ACL/presigned URL phức tạp nếu làm sâu. |
| Elasticsearch/search | Search text, indexing, query search service. | Khi PostgreSQL `LIKE` không đủ hoặc cần search service. | Index vài master data docs, search keyword. | Đang bắt đầu mini-lab kế tiếp, nối từ PostgreSQL pattern. | Làm Elastic thành production search platform quá sớm. |
| gRPC internal communication | Giao tiếp service-to-service typed/efficient. | Khi so sánh REST vs internal RPC. | Chỉ note/diagram, chưa cần code. | Awareness. | Thêm IDL/protobuf khi chỉ có một service. |
| Realtime: SignalR/WebSocket/SSE/long polling | Notification/live updates tới frontend. | Khi có notification/progress update feature. | Compare SSE vs WebSocket ở mức note. | Chưa làm. | Realtime infra dễ phình scope. |
| Observability: Prometheus/Grafana/Loki | Metrics/logging/dashboard để vận hành. | Khi demo cần giải thích production readiness. | Add log/metric awareness note, không cần full stack. | Awareness. | Chạy full monitoring stack quá sớm. |
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
| `lab-code/keycloak-lab` | Authorization Server/OIDC mini-lab | Mini-labbed | Keycloak issue token, `tenant_id` claim, issuer/JWKS. |
| `APP_AUTH_MODE=keycloak` | Resource Server validate Keycloak token | Verified | Keycloak token gọi API tenant-aware thành công. |
| `presentation-notes/demo-script-keycloak-tenant-flow.md` | Mentor-facing demo path | Prepared | Script start DB/Keycloak/app, verify tenant 1/2, cross-tenant id. |
| SQL playground `01-09` | PostgreSQL learning lab | Implemented | Schema, EXPLAIN, index pattern, migration, ACID/isolation. |
| `docs/07-architecture/keycloak-in-target-architecture.md` | Security architecture mapping | Done | Map Keycloak/OIDC vào target architecture. |

## Core vs optional trong Phase 1

### Core đã đủ để demo

- Spring Boot backend service.
- PostgreSQL + Flyway.
- Shared-table tenant isolation.
- Tenant-aware repository/service.
- JWT tạm + Keycloak mini-lab.
- Regression test chống leakage.
- Demo script mentor-facing.

### Mini-lab nên làm khi có trigger

- Redis: khi có cache/feature flag.
- MinIO: khi có file upload.
- Elasticsearch: đang là mini-lab kế tiếp vì đã học PostgreSQL `LIKE`/index/query pattern.
- Kafka/Debezium: khi cần async event/data sync.
- Observability: khi cần giải thích vận hành hoặc có log/metric cụ thể.

### Awareness là đủ cho Phase 1

- API Gateway/service discovery/load balancing.
- gRPC internal communication.
- Realtime protocols.
- External integrations.
- LLM provider integration.
- DDD/domain boundaries.

### Out of scope Phase 1

- Full production microservices.
- Full ERP/accounting domain.
- Full Keycloak/RBAC platform.
- Kafka/Debezium/Elastic/Grafana stack chạy đồng thời.
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

Hướng tốt nhất sau tài liệu này:

1. Dry-run backend demo script Keycloak tenant flow.
2. Cập nhật presentation note/report để nói rõ implemented vs mini-lab vs awareness.
3. Nhánh mini-lab kế tiếp đã chọn: Elasticsearch/search service cho `master_data`.
4. Sau Elasticsearch, chỉ chọn thêm Redis/MinIO/Kafka nếu có trigger rõ.

Không nên mở nhiều mini-lab cùng lúc.
