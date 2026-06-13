# File Service Code Walkthrough

## 1. General Anatomy

A simple object-storage service usually has these parts:

- **Controller**: HTTP upload/download/list/delete endpoints.
- **Service/use case**: validates input, resolves tenant, decides object key, writes metadata.
- **Storage gateway/adapter**: wraps MinIO/S3 SDK calls.
- **Metadata entity/repository**: stores searchable business metadata in PostgreSQL.
- **Security config**: validates JWT and maps roles.
- **Migration**: creates service-owned table/schema.
- **Runtime config**: DB, MinIO endpoint/bucket, Keycloak issuer/JWKS.

MinIO stores binary object bytes. PostgreSQL stores metadata and tenant ownership. The backend connects the two using an internal `object_key`.

## 2. Repo-Specific Mapping

| General concept | Repo file |
|---|---|
| Spring Boot app | `lab-code/file-service/src/main/java/com/viettel/files/FileServiceApplication.java` |
| Security config | `lab-code/file-service/src/main/java/com/viettel/files/security/SecurityConfig.java` |
| Request logging | `lab-code/file-service/src/main/java/com/viettel/files/observability/RequestLoggingFilter.java` |
| Controller | `lab-code/file-service/src/main/java/com/viettel/files/file/FileController.java` |
| Use case service | `lab-code/file-service/src/main/java/com/viettel/files/file/FileStorageService.java` |
| MinIO adapter | `lab-code/file-service/src/main/java/com/viettel/files/file/MinioFileStorageGateway.java` |
| Metadata entity/repository | `FileMetadata.java`, `FileMetadataRepository.java` |
| Migration | `src/main/resources/db/migration/V1__create_file_metadata.sql` |
| Config | `src/main/resources/application.yml` |
| Makefile targets | `file-validate`, `file-run`, `file-run-logs`, `file-status` |

`tenant-demo` no longer owns `/api/files`. It remains focused on master data.

## 3. Runtime Flow

Upload:

```text
Browser selects file
  -> React Web UI POST /api/files through Kong
  -> file-service validates JWT
  -> common-security extracts tenant_id
  -> FileStorageService generates fileId/objectKey
  -> MinioFileStorageGateway uploads object
  -> FileMetadataRepository stores metadata
  -> UI receives fileId
```

Download:

```text
React Web UI GET /api/files/{fileId} through Kong
  -> file-service validates JWT
  -> lookup by tenant_id + fileId
  -> if missing, 404
  -> get object from MinIO by stored objectKey
  -> stream file to browser
```

List:

```text
React Web UI GET /api/files through Kong
  -> file-service validates JWT
  -> query metadata by tenant_id
  -> return metadata list
```

Delete:

```text
React Web UI DELETE /api/files/{fileId}
  -> file-service validates role
  -> lookup by tenant_id + fileId
  -> remove object from MinIO
  -> delete metadata row
```

## 4. Security/Tenant Rules

The service never trusts tenant ID from request params, body, form fields, or file metadata.

Tenant ID comes from:

```text
Keycloak token -> Spring Resource Server -> JwtTenantContextFilter -> TenantContext
```

Roles:

- `VIEWER` can list/download.
- `ACCOUNTANT` can upload/delete.
- `ADMIN` can do both.

## 5. Common Mistakes

- Calling MinIO directly from React with access keys.
- Letting the client choose `object_key`.
- Trusting `tenantId` from multipart form fields.
- Returning `403` for cross-tenant file IDs and leaking that the file exists.
- Storing binary data in PostgreSQL instead of metadata only.
- Forgetting MinIO and PostgreSQL are not in one atomic transaction.
- Logging file contents or access tokens.

## 6. Verification

Start dependencies:

```bash
cd lab-code
make db-up
make keycloak-up
make keycloak-setup
make minio-up
make kong-up
```

Run service:

```bash
cd lab-code
make file-run-logs
```

Validate:

```bash
mvn -f lab-code/file-service/pom.xml validate
curl -i http://localhost:8083/actuator/health
curl -i http://localhost:18000/api/files
```

Missing token should return `401` through Kong. With a tenant token:

- upload as `tenant1-user`;
- list as `tenant1-user`;
- download as `tenant1-user`;
- login as `tenant2-user` and confirm tenant 1 file does not appear;
- upload/delete as `tenant2-user` should return `403` if role is `VIEWER`.

For Loki:

```logql
{service="file-service"}
{service="file-service"} |= "requestId="
```
