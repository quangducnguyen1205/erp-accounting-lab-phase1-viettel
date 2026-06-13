# File Service

`file-service` is the Phase 1.5 tenant-aware file boundary for `Master Data Portal`.

It is intentionally Maven/IntelliJ-first like the other Java backend services:

```text
React Web UI
  -> Kong Gateway /api/files
    -> file-service :8083
      -> PostgreSQL schema file_service
      -> MinIO bucket tenant-demo-files
```

## Run

Start Docker infra first:

```bash
cd lab-code
make db-up
make keycloak-up
make keycloak-setup
make minio-up
make kong-up
```

Run the service:

```bash
cd lab-code
make file-run
```

Run with Loki-friendly file logging:

```bash
cd lab-code
make file-run-logs
```

The log file is:

```text
lab-code/logs/file-service.log
```

## API

All endpoints validate JWT through Spring Security Resource Server and derive `tenant_id` from the token.

```text
GET    /api/files
POST   /api/files
GET    /api/files/{fileId}
DELETE /api/files/{fileId}
```

`GET` is allowed for `ADMIN`, `ACCOUNTANT`, and `VIEWER`.
`POST` and `DELETE` are allowed for `ADMIN` and `ACCOUNTANT`.

Cross-tenant file access returns `404` so the API does not reveal whether another tenant owns that file.

## Local credentials

The default MinIO credentials are local lab values only:

```text
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
```

Do not reuse these values outside the local demo.
