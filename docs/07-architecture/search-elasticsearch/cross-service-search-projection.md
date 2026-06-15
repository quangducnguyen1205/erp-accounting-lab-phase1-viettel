# Cross-Service Search Projection

## 1. General Anatomy

An event-driven search projection usually has these pieces:

| Piece | Responsibility |
|---|---|
| Domain write API | Validates request and writes source-of-truth data. |
| Domain event | Describes what changed. |
| Kafka topic | Delivers changes to independent consumers. |
| Projection consumer | Converts domain event into search document changes. |
| Search index | Stores denormalized documents for query speed. |
| Search read API | Applies tenant/security filters and returns results. |

This pattern is useful when search needs a different query model than the relational database.

## 2. Repo-Specific Mapping

| General concept | Repo file/class |
|---|---|
| Domain write API | `tenant-demo/src/main/java/com/viettel/demo/controller/MasterDataController.java` |
| Source-of-truth table | `tenant-demo` Flyway `master_data` table |
| Event DTO | `tenant-demo/.../messaging/MasterDataChangedEvent.java` |
| Search consumer DTO copy | `search-service/.../event/MasterDataChangedEvent.java` |
| Kafka listener | `search-service/.../event/MasterDataChangedEventConsumer.java` |
| Projection service | `search-service/.../search/MasterDataSearchProjectionService.java` |
| Elasticsearch adapter | `search-service/.../search/MasterDataSearchGateway.java` |
| Search API | `search-service/.../search/MasterDataSearchController.java` |
| Admin reindex API | `POST /api/search/master-data/reindex`, role `ADMIN` only |
| Security | `search-service/.../security/SecurityConfig.java` + `common-security` |

The DTO copy is deliberate. It keeps `search-service` from importing `tenant-demo` code. In a larger organization, this contract would usually become a versioned shared contract package or schema registry subject.

## 3. Event Shape

`MasterDataChangedEvent` now carries projection fields:

| Field | Why search-service needs it |
|---|---|
| `eventId` | Idempotency/debug correlation candidate. |
| `tenantId` | Tenant filter and Kafka key ownership. |
| `aggregateId` | Elasticsearch document id. |
| `code` | Searchable business code. |
| `name` | Searchable name. |
| `category` | Searchable type/category. |
| `active` | Search filter for deactivated records. |
| `changeType` | Decides create/update/deactivate behavior. |
| `occurredAt` | Projection timestamp/debug signal. |

## 4. Runtime Flow

```text
ACCOUNTANT creates/updates/deactivates master data
  -> tenant-demo validates JWT and tenant
  -> tenant-demo writes PostgreSQL
  -> tenant-demo publishes MasterDataChangedEvent
  -> Kafka topic master-data-events
  -> audit-log-service consumes and stores audit history
  -> search-service consumes and updates Elasticsearch
  -> React UI searches through Kong
  -> search-service filters by tenant_id from JWT
```

Both `audit-log-service` and `search-service` consume the same topic with different consumer groups. Kafka keeps those consumers independent.

## 5. How Deletes Work In This Lab

`tenant-demo` uses soft delete/deactivate for master data. The event uses `changeType=DEACTIVATED`.

The current search projection marks the document inactive. Search API filters `active=true`, so deactivated records disappear from normal search results while PostgreSQL keeps the historical row.

## 6. Tenant-Scoped Reindex

Reindex is an operational/manual endpoint, not a product feature:

```text
POST /api/search/master-data/reindex
Authorization: Bearer <platform-admin token>
```

Runtime flow:

```text
platform-admin token
  -> Kong
  -> search-service validates ADMIN role and tenant_id
  -> search-service calls tenant-demo GET /api/master-data with same token
  -> tenant-demo returns active source records for that tenant
  -> search-service deletes old tenant docs from Elasticsearch
  -> search-service bulk indexes current tenant records
```

Current scope is tenant-specific because it uses `tenant_id` from the admin token. No all-tenant/global reindex workflow is implemented in this lab.

## 7. Common Mistakes

- Emitting only `aggregateId` and then expecting search-service to build a full projection without another call.
- Calling `tenant-demo` from search-service for every event unless that tradeoff is intentional.
- Forgetting that audit and search are separate consumers with separate consumer groups.
- Assuming Kafka delivery means Elasticsearch index is instantly updated.
- Returning cross-tenant search results because tenant filter was missing.
- Putting the reindex endpoint into the React UI.

## 8. Verification

Compile/config checks:

```bash
mvn -f lab-code/search-service/pom.xml validate
mvn -f lab-code/search-service/pom.xml compile
docker compose -f lab-code/kong-gateway-lab/docker-compose.yml config
docker compose -f lab-code/loki-lab/docker-compose.yml config
```

Runtime smoke:

```bash
cd lab-code
make -f Makefile.legacy elastic-up
make -f Makefile.legacy kafka-up
make -f Makefile.legacy search-run-logs
```

Then create a master-data record and search it:

```text
GET http://localhost:18000/api/search/master-data?keyword=<code-or-name>
Authorization: Bearer <tenant token>
```

Expected:

- tenant 1 finds tenant 1 records;
- tenant 2 does not see tenant 1 records;
- deactivated records disappear from active search;
- `platform-admin` can reindex the current tenant;
- `tenant1-user` and `tenant2-user` get `403` on reindex;
- `{service="search-service"}` in Loki shows consume/index logs.
