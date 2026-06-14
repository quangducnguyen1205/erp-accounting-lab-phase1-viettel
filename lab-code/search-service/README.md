# Search Service

`search-service` owns the Phase 1.5 Elasticsearch projection for `Master Data Portal`.

It is intentionally Maven/IntelliJ-first like the other Java backend services:

```text
tenant-demo
  -> Kafka MasterDataChangedEvent
    -> search-service :8084
      -> Elasticsearch index master_data_search

React Web UI
  -> Kong Gateway /api/search/master-data
    -> search-service :8084
```

## Run

Start Docker infra first:

```bash
cd lab-code
make keycloak-up
make keycloak-setup
make kafka-up
make elastic-up
make kong-up
```

Start `tenant-demo` with Kafka enabled in another terminal so it can publish `MasterDataChangedEvent`:

```bash
cd lab-code
APP_AUTH_MODE=keycloak \
APP_MESSAGING_ENABLED=true \
KAFKA_BOOTSTRAP_SERVERS=localhost:19092 \
make app-run-logs
```

Run the service:

```bash
cd lab-code
make search-run
```

Run with Loki-friendly file logging:

```bash
cd lab-code
make search-run-logs
```

The log file is:

```text
lab-code/logs/search-service.log
```

## API

All endpoints validate JWT through Spring Security Resource Server and derive `tenant_id` from the token.

```text
GET /api/search/master-data?keyword=...
```

Search results are filtered by tenant and `active=true`.

## Logs

When running with:

```bash
make search-run-logs
```

Spring Boot also writes a local file log:

```text
lab-code/logs/search-service.log
```

Grafana Alloy tails that file into Loki with:

```text
service="search-service"
source="file"
environment="local"
```

Search request text such as `requestId`, code, or keyword should be searched with LogQL `|=`, not added as Loki labels.

## Caveat

This service is an eventually consistent projection. A newly created or updated master-data record appears after Kafka delivery and indexing complete.

There is no outbox, retry/DLT, schema registry, or full reindex workflow yet. Those are later production-hardening topics.
