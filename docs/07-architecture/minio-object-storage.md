# MinIO / S3 object storage foundation

## Vai trò tài liệu

Tài liệu này giải thích kiến thức nền về object storage để có thể dùng lại cho nhiều backend domain: chứng từ kế toán, hóa đơn, hợp đồng, avatar, file đính kèm, export report. Phần cuối mới nối về mini-lab hiện tại trong `tenant-demo`.

Đọc tiếp theo:

- `minio-s3-api-shapes.md` - request/response/error shape của S3/MinIO.
- `minio-code-guide-spring-boot.md` - cách tích hợp vào Spring Boot.
- `lab-code/minio-lab/README.md` - lệnh chạy local.

---

## 1. Object storage là gì?

Object storage là kiểu lưu trữ dữ liệu dưới dạng object. Mỗi object thường gồm:

- dữ liệu nhị phân: file PDF, ảnh, file scan, CSV, report;
- object key: tên định danh object trong bucket;
- metadata: content type, kích thước, ETag, custom metadata;
- bucket: không gian chứa object.

Khác với file system truyền thống, object storage không dùng thư mục thật kiểu POSIX. Các chuỗi như `tenant/1/documents/a.pdf` chỉ là object key có dấu `/`, còn “folder” trong console thường là cách UI nhóm object theo prefix.

---

## 2. MinIO là gì?

MinIO là object storage server tương thích S3 API. Nghĩa là backend có thể dùng các thao tác quen thuộc của S3 như put object, get object, list object, presigned URL, nhưng chạy local hoặc private infrastructure.

Trong Phase 1, MinIO phù hợp để học:

- cách backend lưu file ngoài database;
- cách thiết kế object key tenant-aware;
- cách tách file binary khỏi business metadata;
- cách dùng S3-compatible API mà chưa cần AWS thật.

---

## 3. S3-compatible API là gì?

S3-compatible nghĩa là server hỗ trợ các API/behavior quan trọng giống Amazon S3 ở mức đủ để SDK/client S3 làm việc được.

Ví dụ operation:

```text
PUT object     -> upload object
GET object     -> download object
HEAD object    -> đọc metadata/stat
DELETE object  -> xóa object
LIST objects   -> liệt kê object theo prefix
Presigned URL  -> URL tạm thời để upload/download
```

MinIO không phải AWS S3, nhưng cố gắng tương thích S3 API để app có thể đổi target storage dễ hơn.

---

## 4. Object storage khác database và file system thế nào?

| Tiêu chí | Database | File system | Object storage |
|---|---|---|---|
| Phù hợp | dữ liệu có quan hệ, query, transaction | file local của một máy | file/blob lớn, phân tán, truy cập qua API |
| Ví dụ | invoice row, tenant, permission | temp file local | PDF hóa đơn, ảnh, hợp đồng |
| Query | SQL mạnh | path/file API | key/prefix/list, không phải query business |
| Transaction với DB | trực tiếp trong DB | không tự đồng bộ | phải tự thiết kế consistency |
| Scale file lớn | không nên lạm dụng | phụ thuộc máy | phù hợp hơn |

Rule học tập:

```text
PostgreSQL lưu metadata nghiệp vụ.
MinIO lưu binary object.
Backend nối hai phần bằng fileId/objectKey.
```

---

## 5. Core concepts

### Bucket

Bucket là container cấp cao chứa object. Ví dụ local mini-lab có thể dùng một bucket:

```text
tenant-demo-files
```

Không nên tạo bucket theo từng file. Với SaaS nhiều tenant, có thể chọn:

- một bucket chung, object key có tenant prefix;
- bucket theo environment/domain;
- bucket theo tenant chỉ khi có lý do vận hành rõ ràng.

Phase 1 nên dùng một private bucket chung để đơn giản.

### Object

Object là dữ liệu được lưu trong bucket. Object có key duy nhất trong bucket.

Ví dụ:

```text
tenant/1/documents/2026/05/8f4d...pdf
```

### Object key

Object key là định danh của object. Backend nên sinh key, không nhận nguyên key từ client.

Ví dụ tốt:

```text
tenant/{tenantId}/documents/{yyyy}/{mm}/{uuid}-{safeFilename}
```

Ví dụ rủi ro:

```text
../../secret.txt
tenant/2/invoice.pdf     # client tenant 1 tự gửi key tenant 2
```

### Prefix

Prefix là phần đầu của object key, dùng để nhóm/list object:

```text
tenant/1/
tenant/1/documents/
tenant/2/documents/2026/
```

Prefix không thay thế authorization. Backend vẫn phải check tenant bằng DB/query.

### Metadata

Metadata gồm thông tin hệ thống và custom metadata:

- content type: `application/pdf`, `image/png`;
- size;
- ETag;
- custom metadata như `tenant-id`, `uploaded-by` nếu cần.

Metadata trên object hữu ích, nhưng metadata nghiệp vụ vẫn nên nằm trong PostgreSQL để query/audit tốt hơn.

### Content type và object size

Backend nên biết content type và size khi upload:

- để trả response download đúng;
- để validate file quá lớn;
- để tránh nhầm file binary với text;
- để logging/audit tốt hơn.

---

## 6. Vì sao object storage hợp với invoice/document/attachment?

Các file như PDF hóa đơn, ảnh chứng từ, hợp đồng scan thường:

- có kích thước lớn hơn row nghiệp vụ;
- không cần query bằng SQL bên trong file;
- cần upload/download/stream;
- cần lưu lâu dài;
- có metadata nghiệp vụ riêng: tenant, người upload, loại chứng từ, trạng thái.

Thiết kế hợp lý:

```text
PostgreSQL
  file_id, tenant_id, original_filename, object_key, content_type, size, created_at

MinIO
  bucket: tenant-demo-files
  object key: tenant/1/documents/2026/05/<uuid>.pdf
  binary content: file PDF
```

---

## 7. Source of truth principle

Trong backend nghiệp vụ, PostgreSQL thường là source of truth cho metadata:

- file này thuộc tenant nào;
- file gắn với invoice/document nào;
- user nào upload;
- trạng thái file còn active hay đã deleted;
- quyền nào được download.

MinIO là nơi giữ binary object. Object storage không nên bị dùng như database nghiệp vụ.

Consistency cần tự suy nghĩ:

- DB insert thành công nhưng upload object fail thì xử lý sao?
- upload object thành công nhưng DB insert fail thì có orphan object không?
- delete DB metadata trước hay delete object trước?

Phase 1 chỉ cần hiểu mindset, chưa cần distributed transaction.

---

## 8. Tenant-aware storage

Tenant-aware file storage cần ít nhất hai lớp:

1. Metadata trong DB có `tenant_id`.
2. Object key hoặc metadata object có tenant scope để dễ audit/debug.

Request flow an toàn:

```text
JWT/Keycloak token đã validate
-> TenantContext có tenantId
-> Service tạo file metadata với tenantId
-> Service sinh objectKey có tenant prefix
-> Gateway upload object lên MinIO
-> API download chỉ tìm file metadata bằng tenantId + fileId
```

Rule quan trọng:

- không nhận `tenantId` từ request body;
- không nhận object key raw từ client để download;
- không để client tự đoán object key rồi gọi MinIO trực tiếp;
- backend phải authorize trước khi trả file hoặc presigned URL.

---

## 9. Public bucket, private bucket và presigned URL

### Private bucket

Private bucket là lựa chọn mặc định nên dùng cho backend nghiệp vụ. Client không đọc object trực tiếp. Backend kiểm tra auth/tenant rồi stream file hoặc cấp presigned URL.

### Public bucket

Public bucket phù hợp cho asset công khai như logo public, file marketing, ảnh public. Không phù hợp cho chứng từ/hóa đơn tenant.

### Presigned URL

Presigned URL là URL tạm thời được ký bằng credential storage. Người có URL có thể upload/download trong thời hạn ngắn mà không cần biết secret.

Presigned URL hữu ích khi:

- file lớn, muốn client upload/download trực tiếp tới object storage;
- backend không muốn stream toàn bộ file qua app server;
- vẫn muốn kiểm soát thời hạn/quyền trước khi cấp URL.

Phase 1 nên bắt đầu với backend-mediated upload/download cho dễ hiểu. Presigned URL để optional sau.

---

## 10. Upload/download/delete flow

### Upload qua backend

```text
Client
-> POST /api/files multipart/form-data
-> Spring Controller nhận MultipartFile
-> Service lấy tenantId từ TenantContext
-> Service validate size/content type
-> Service sinh objectKey
-> Gateway putObject vào MinIO
-> Service lưu metadata vào PostgreSQL
-> Response trả fileId + metadata an toàn
```

### Download qua backend

```text
Client
-> GET /api/files/{fileId}
-> Service tìm metadata bằng tenantId + fileId
-> Nếu không có: 404
-> Gateway getObject từ MinIO bằng objectKey đã lưu
-> Controller stream file về client
```

### Delete

```text
Client
-> DELETE /api/files/{fileId}
-> Service tìm metadata bằng tenantId + fileId
-> Xóa object hoặc soft-delete metadata tùy mục tiêu
```

Trong lab, soft delete metadata hoặc remove object trực tiếp đều được, miễn ghi rõ trade-off.

---

## 11. Common risks

- Cross-tenant file access: API download tìm file bằng `fileId` nhưng quên `tenantId`.
- Public bucket nhầm: chứng từ tenant bị public.
- Commit access key/secret thật vào repo.
- Dùng object storage như database: list prefix rồi coi đó là source of truth nghiệp vụ.
- DB/object lệch nhau: object có nhưng DB không có, hoặc DB có nhưng object mất.
- Nhận raw object key từ client: dễ path traversal/tenant guessing.
- Log full presigned URL quá dài hoặc chứa signature nhạy cảm.
- Load file lớn hết vào memory thay vì stream.

---

## 12. Áp dụng vào repo hiện tại

Mini-lab nên giữ scope nhỏ:

- object storage: MinIO local;
- bucket: private bucket `tenant-demo-files`;
- domain slice: file attachment/chứng từ nhỏ;
- metadata: thiết kế tenant-aware, có thể bắt đầu bằng skeleton trước;
- upload/download: backend-mediated;
- auth: dùng Keycloak/JWT flow hiện tại;
- tenant isolation: download phải query metadata theo `tenantId`.

Không làm ngay:

- full file service production;
- virus scanning;
- lifecycle/retention policy;
- presigned URL production flow;
- distributed transaction giữa DB và MinIO;
- multi-bucket/multi-region design.

---

## Nguồn tham khảo chuẩn

- [MinIO Object Storage for Linux](https://min.io/docs/minio/linux/index.html)
- [MinIO S3 API compatibility](https://minio.community/community/minio-object-store/reference/s3-api-compatibility.html)
- [AWS S3 object overview](https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingObjects.html)
- [AWS S3 object metadata](https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingMetadata.html)
