# Search Service Split Plan

## 1. General Service Boundary Anatomy

A search service is a read-optimized projection service. It is not the source of truth.

Typical parts:

| Part | Meaning |
|---|---|
| Source service | Owns the business table and emits domain events after changes. |
| Event contract | Carries enough fields for search projection updates. |
| Search consumer | Reads events from Kafka and updates the search index. |
| Search index | Elasticsearch index optimized for keyword/search queries. |
| Search API | Tenant-aware read endpoint used by UI through the gateway. |
| Rebuild/reindex path | Optional operational path for rebuilding projection from source of truth. |

The important design rule is: PostgreSQL remains the source of truth; Elasticsearch is a projection that may lag briefly.

## 2. Repo-Specific Mapping

Current Phase 1.5 mapping:

| Concept | Repo implementation |
|---|---|
| Source service | `lab-code/tenant-demo` |
| Event | `com.viettel.demo.messaging.MasterDataChangedEvent` |
| Topic | `master-data-events` |
| Search service | `lab-code/search-service` |
| Kafka consumer group | `search-service` |
| Elasticsearch index | `master_data_search` |
| Public route | `GET /api/search/master-data?keyword=...` through Kong |
| Kong config | `lab-code/kong-gateway-lab/kong.yml` |
| Loki log file | `lab-code/logs/search-service.log` |

The old Phase 1 mini-lab put search code inside `tenant-demo` behind `APP_SEARCH_ENABLED`. That shape was useful for first learning Elasticsearch, but the current product demo uses a separated `search-service`.

## 3. Runtime Flow

```text
React Web UI
  -> Kong Gateway
  -> tenant-demo creates/updates/deactivates master data
  -> PostgreSQL remains source of truth
  -> tenant-demo publishes MasterDataChangedEvent
  -> Kafka topic master-data-events
  -> search-service consumes event
  -> search-service writes Elasticsearch document

React Web UI
  -> Kong Gateway
  -> search-service /api/search/master-data
  -> Elasticsearch query filtered by tenant_id
```

For `DEACTIVATED` events, the search document is marked inactive and normal search filters it out.

## 4. Security And Tenant Isolation

`search-service` is a Resource Server:

- validates JWT with Keycloak issuer/JWKS;
- extracts `tenant_id` from the validated token through `common-security`;
- filters Elasticsearch queries by that tenant;
- never trusts tenantId from query params, request body, or UI state.

Roles allowed to search are the same local demo roles used for read paths: `ADMIN`, `ACCOUNTANT`, and `VIEWER`.

## 5. Common Mistakes

- Treating Elasticsearch as the source of truth.
- Searching first and filtering tenant after results are already returned.
- Keeping `APP_SEARCH_ENABLED` on `tenant-demo` after search has moved out.
- Calling Elasticsearch directly from React UI.
- Expecting a newly updated record to appear instantly without Kafka/indexing delay.
- Forgetting that delete/deactivate must also update the projection.
- Adding `requestId`, `tenantId`, or `code` as Loki labels instead of text-searching logs.

## 6. Verification

Start dependencies and services:

```bash
cd lab-code
make keycloak-up
make keycloak-setup
make kafka-up
make elastic-up
make kong-up
```

Run Java services in separate terminals:

```bash
cd lab-code
APP_AUTH_MODE=keycloak \
APP_MESSAGING_ENABLED=true \
KAFKA_BOOTSTRAP_SERVERS=localhost:19092 \
make app-run-logs
```

```bash
cd lab-code
make search-run-logs
```

Then create or update master data and search through Kong:

```text
GET http://localhost:18000/api/search/master-data?keyword=...
Authorization: Bearer <local demo token>
```

Check:

- UI search returns only current tenant records;
- Kafka UI shows `master-data-events`;
- search-service log shows consume/index activity;
- Loki query `{service="search-service"}` shows service logs.

## 7. Caveats

- No outbox yet, so DB write and Kafka publish are not atomic.
- No retry/DLT yet for failed indexing.
- No schema registry yet; event DTOs are intentionally duplicated for learning.
- No full reindex admin workflow yet.
- Elasticsearch is local lab infrastructure, not a production cluster.
