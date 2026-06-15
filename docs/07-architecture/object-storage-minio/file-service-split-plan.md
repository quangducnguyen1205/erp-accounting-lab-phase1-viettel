# File Service Split Plan

## 1. Why Split File Storage

`tenant-demo` is the master-data service. It owns master data business rules and publishes master data events.

File upload/download is a different responsibility:

- accept multipart upload;
- store binary object in MinIO;
- store tenant-aware file metadata in PostgreSQL;
- return/download files only for the authenticated tenant.

So Phase 1.5 moves file runtime behavior into a separate Java service:

```text
React Web UI
  -> Kong Gateway
    -> file-service

file-service
  -> PostgreSQL schema file_service
  -> MinIO bucket tenant-demo-files
  -> common-security for JWT/tenant context
```

The old `tenant-demo` MinIO runtime code was removed so `/api/files` has one clear service owner.

## 2. Service Responsibility

`file-service` owns:

- file upload API;
- file download API;
- file metadata list;
- file delete;
- tenant-aware metadata queries;
- object key generation;
- MinIO adapter/gateway.

`file-service` must not own:

- master data business rules;
- audit-log storage;
- Keycloak user/role management;
- Kong routing decisions;
- frontend token handling.

## 3. Data Ownership

Binary object:

```text
MinIO bucket tenant-demo-files
```

Metadata:

```text
PostgreSQL schema file_service
table file_metadata
```

The metadata table stores `tenant_id`, `file_id`, original filename, content type, size, object key, and created time.

Tenant ID always comes from validated JWT through `common-security`. The client never sends `tenantId` in upload/download requests.

## 4. API Boundary

API hiện tại:

```text
GET    /api/files
POST   /api/files
GET    /api/files/{fileId}
DELETE /api/files/{fileId}
```

Role behavior:

- `ADMIN`, `ACCOUNTANT`, `VIEWER` can list/download;
- `ADMIN`, `ACCOUNTANT` can upload/delete;
- `VIEWER` receives `403` for upload/delete.

Cross-tenant file access returns `404`, not `403`, so one tenant cannot infer whether another tenant owns a `fileId`.

## 5. Kong Route

Kong route:

```text
http://localhost:18000/api/files...
  -> http://host.docker.internal:8083/api/files...
```

This keeps the browser path consistent:

```text
React Web UI -> Kong -> backend service
```

The UI does not call MinIO directly.

## 6. Giới hạn hiện tại

- No file versioning yet.
- No presigned URL flow yet.
- No antivirus/content scanning.
- No production object lifecycle/retention.
- No distributed transaction between PostgreSQL metadata and MinIO object.
- Delete is physical delete in this lab, not retention-safe production delete.

These are acceptable for the Phase 1.5 learning demo.
