# MinIO code guide cho Spring Boot

## Vai trò tài liệu

Tài liệu này hướng dẫn hình dạng code Spring Boot cho mini-lab MinIO/file storage. Nó không thay thế concept doc:

- `minio-object-storage.md`
- `minio-s3-api-shapes.md`
- `lab-code/minio-lab/README.md`

Mục tiêu code:

```text
Controller mỏng
-> Service/use-case lấy tenantId, validate, tạo metadata/objectKey
-> Gateway/Adapter gọi MinIO
-> PostgreSQL giữ metadata nghiệp vụ
-> MinIO giữ binary object
```

---

## 1. Chọn integration approach

### Option 1: Gọi MinIO Java SDK trực tiếp trong service

Ưu điểm:

- ít class;
- nhanh cho demo cực nhỏ;
- dễ nhìn thấy SDK method.

Nhược điểm:

- service bị trộn business logic với object storage API;
- khó đổi storage hoặc mock test;
- dễ lộ raw SDK exception/response lên controller.

### Option 2: MinIO Java SDK + Gateway/Adapter

Ưu điểm:

- service nói bằng ngôn ngữ nghiệp vụ: upload/download/delete file;
- gateway giữ chi tiết `putObject`, `getObject`, `statObject`;
- dễ test service hơn;
- giống pattern đã dùng cho Elasticsearch gateway.

Nhược điểm:

- thêm vài class;
- cần giữ adapter đủ nhỏ, không biến thành framework.

### Option 3: AWS SDK S3 client hoặc Spring Cloud AWS abstraction

Ưu điểm:

- gần AWS ecosystem;
- phù hợp nếu sau này chạy AWS S3 thật.

Nhược điểm:

- thêm khái niệm AWS credential/region/client config;
- có thể xa mục tiêu học MinIO local;
- Spring Cloud AWS là abstraction lớn hơn nhu cầu Phase 1.

### Khuyến nghị cho repo này

Dùng **MinIO Java SDK + Gateway/Adapter**.

Lý do:

- sát MinIO local mini-lab;
- đủ gần S3 operation thật;
- vẫn giữ code Spring Boot sạch;
- không cần full production file service.

Skeleton hiện tại cố ý chưa thêm dependency `io.minio:minio`. Khi tự code implementation thật, thêm dependency này vào `pom.xml`, rồi hoàn thiện `MinioClientConfig` và `FileStorageGateway` implementation.

---

## 2. Package/class shape đề xuất

```text
com.viettel.demo.storage
├── FileStorageProperties
├── MinioClientConfig
├── FileStorageGateway
├── FileStorageService
├── FileController
├── StoredObjectInfo
├── FileUploadResponse
└── FileDownloadInfo
```

Nếu tự code metadata DB:

```text
com.viettel.demo.file
├── FileMetadata
├── FileMetadataRepository
└── FileMetadataService
```

Phase 1 có thể giữ metadata model nhỏ. Không cần xây file service production.

---

## 3. Responsibility của từng class

| Class | Trách nhiệm | Không nên làm |
|---|---|---|
| `FileStorageProperties` | Bind endpoint, bucket, access key, secret, feature flag. | Không chứa business logic. |
| `MinioClientConfig` | Tạo MinIO client khi file storage enabled. | Không tự upload/download. |
| `FileStorageGateway` | Adapter gọi MinIO operation. | Không đọc `TenantContext`, không biết HTTP. |
| `FileStorageService` | Use case upload/download/delete, tenant-aware metadata, object key generation. | Không expose raw MinIO response. |
| `FileController` | HTTP boundary, nhận `MultipartFile`, trả response/stream. | Không tự sinh object key, không gọi MinIO trực tiếp. |
| DTOs | Response an toàn cho API caller. | Không chứa secret/internal policy. |

---

## 4. Config properties

`application.yml` nên có feature flag:

```yaml
app:
  file-storage:
    enabled: ${APP_FILE_STORAGE_ENABLED:false}
    endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
    access-key: ${MINIO_ACCESS_KEY:minioadmin}
    secret-key: ${MINIO_SECRET_KEY:minioadmin}
    bucket: ${MINIO_BUCKET:tenant-demo-files}
    region: ${MINIO_REGION:us-east-1}
```

Rule:

- `enabled=false` mặc định để app-test không phụ thuộc MinIO.
- `.env.example` chỉ chứa dev defaults, không chứa secret thật.
- Production phải dùng secret manager/vault hoặc env thật, không commit.

---

## 5. MinIO client config

Khi tự code thật, `MinioClientConfig` có thể tạo bean:

```text
MinioClient.builder()
    .endpoint(properties.getEndpoint())
    .credentials(properties.getAccessKey(), properties.getSecretKey())
    .build()
```

Đặt `@ConditionalOnProperty(prefix = "app.file-storage", name = "enabled", havingValue = "true")` để bean chỉ active khi bật mini-lab.

Không cần validate quá sâu cho lỗi gõ sai YAML/env ở Phase 1. Nếu endpoint sai, fail rõ khi gọi MinIO là đủ.

---

## 6. Gateway/Adapter shape

Gateway nên nói bằng operation object storage, không nói bằng HTTP/controller:

```text
putObject(objectKey, inputStream, size, contentType, metadata) -> StoredObjectInfo
getObject(objectKey) -> InputStream/stream wrapper
statObject(objectKey) -> StoredObjectInfo
removeObject(objectKey)
```

Gateway được phép biết:

- bucket name;
- MinIO client method;
- mapping SDK exception sang app exception gọn;
- object metadata shape.

Gateway không nên biết:

- user hiện tại là ai;
- tenantId đến từ đâu;
- endpoint nào gọi nó;
- business rule download file.

---

## 7. Service/use-case shape

`FileStorageService` là nơi enforce tenant-aware flow:

```text
upload(file):
  tenantId = TenantContext.getCurrentTenant()
  validate content type/size cơ bản
  fileId = UUID
  objectKey = generateObjectKey(tenantId, fileId, safeFilename)
  gateway.putObject(...)
  save metadata tenantId + fileId + objectKey + contentType + size
  return FileUploadResponse

download(fileId):
  tenantId = TenantContext.getCurrentTenant()
  metadata = repository.findByTenantIdAndFileId(tenantId, fileId)
  if missing -> 404
  object = gateway.getObject(metadata.objectKey)
  return stream + safe headers
```

Không nhận `tenantId` từ request body. Không nhận raw object key từ request.

---

## 8. Metadata model

Metadata DB nên có tối thiểu:

| Field | Lý do |
|---|---|
| `id` hoặc `file_id` | API dùng id này, không expose object key. |
| `tenant_id` | Tenant isolation. |
| `object_key` | Liên kết tới MinIO object. |
| `original_filename` | Hiển thị cho user. |
| `content_type` | Trả response download đúng. |
| `size_bytes` | Validate/audit. |
| `etag` | Debug/đối chiếu object. |
| `created_at` | Audit tối thiểu. |
| `is_deleted` | Optional soft delete. |

Nếu chưa muốn migration/entity ngay, có thể bắt đầu bằng skeleton và tự thiết kế sau.

---

## 9. MultipartFile upload

Spring MVC thường nhận upload bằng `MultipartFile`:

```text
POST /api/files
Content-Type: multipart/form-data
field name: file
```

Điểm cần nhớ:

- dùng `file.getInputStream()` để stream lên MinIO;
- dùng `file.getSize()` để biết size;
- dùng `file.getContentType()` nhưng không tin tuyệt đối;
- sanitize `file.getOriginalFilename()` nếu dùng trong display/object key;
- không load file lớn hết vào byte array nếu không cần.

---

## 10. Streaming download

Download nên tránh đọc toàn bộ file vào memory khi file lớn.

Hướng đơn giản:

- Gateway trả `InputStream`;
- Controller trả `InputStreamResource` hoặc stream response;
- set headers:
  - `Content-Type`;
  - `Content-Length` nếu biết;
  - `Content-Disposition` nếu muốn browser tải file.

Phase 1 file nhỏ nên không cần tối ưu quá sâu, nhưng nên biết nguyên tắc.

---

## 11. Authorization và tenant safety

Auth flow hiện tại vẫn dùng Keycloak/JWT:

```text
Bearer token
-> Spring Security
-> JwtTenantContextFilter
-> TenantContext
-> FileStorageService
```

Rule:

- upload/download phải cần authenticated user;
- nếu RBAC bật, có thể yêu cầu role như `ACCOUNTANT` hoặc `ADMIN`;
- tenant isolation vẫn nằm ở service/repository metadata query;
- object key có tenant prefix để dễ audit, nhưng prefix không thay thế DB tenant check.

---

## 12. Test/manual verification

Automated test tối thiểu sau khi tự code:

- `make app-test` vẫn pass khi `APP_FILE_STORAGE_ENABLED=false`.

Manual HTTP cases:

- tenant 1 upload file -> `201/200`;
- tenant 1 download own file -> `200`;
- tenant 2 download tenant 1 file id -> `404` hoặc không leak;
- missing token -> `401`;
- invalid token -> `401`;
- user thiếu role nếu endpoint bị RBAC -> `403`;
- MinIO down khi enabled -> lỗi rõ, không làm app baseline fail nếu disabled.

---

## 13. Common mistakes

- Cho client gửi `tenantId` hoặc `objectKey` raw.
- Lưu file binary trực tiếp vào PostgreSQL khi không có lý do rõ.
- Lưu metadata nghiệp vụ trong object metadata rồi query bằng list prefix.
- Public bucket nhầm cho chứng từ tenant.
- Log full presigned URL/signature.
- Không xử lý object orphan khi DB save fail.
- Không sanitize filename.
- Load toàn bộ file lớn vào memory.
- Bật file storage mặc định làm `make app-test` phụ thuộc MinIO.

---

## 14. Done criteria cho mini-lab

- Có MinIO local chạy được.
- Có bucket private cho lab.
- App config có `APP_FILE_STORAGE_ENABLED=false` default.
- Có skeleton/code guide rõ.
- Sau khi tự implement:
  - upload/download đi qua backend;
  - metadata tenant-aware;
  - tenant 2 không đọc được file tenant 1;
  - app-test baseline vẫn pass khi storage disabled.

---

## Nguồn tham khảo chuẩn

- [MinIO Object Storage for Linux](https://min.io/docs/minio/linux/index.html)
- [MinIO Java API - MinioClient](https://minio-java.min.io/io/minio/MinioClient.html)
- [MinIO Java API - PutObjectArgs](https://minio-java.min.io/io/minio/PutObjectArgs.html)
- [MinIO Java API - GetObjectArgs](https://minio-java.min.io/io/minio/GetObjectArgs.html)
- [AWS S3 API Reference](https://docs.aws.amazon.com/AmazonS3/latest/API/Type_API_Reference.html)
