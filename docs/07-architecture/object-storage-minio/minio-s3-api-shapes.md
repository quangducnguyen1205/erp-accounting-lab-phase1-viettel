# MinIO / S3 API shapes

## Vai trò tài liệu

Tài liệu này tập trung vào request/response/error shape của các operation S3/MinIO thường gặp. Mục tiêu là biết backend cần input gì, output gì, lỗi thường trông ra sao, rồi mới đọc code Spring Boot.

Đọc trước concept tổng quan ở `minio-object-storage.md`.

---

## 1. Mental model chung

Hầu hết operation S3 xoay quanh:

```text
bucket
object key
binary stream/file
metadata
permissions/credentials
```

Với Java SDK, các field này thường được đưa vào builder object như `PutObjectArgs`, `GetObjectArgs`, `StatObjectArgs`. Với raw HTTP, chúng xuất hiện trong URL, headers và body.

---

## 2. Create bucket

Mục tiêu: tạo bucket nếu chưa có.

Input chính:

| Input | Ý nghĩa |
|---|---|
| bucket | Tên bucket, ví dụ `tenant-demo-files`. |
| region | Optional trong MinIO local, quan trọng hơn với AWS/multi-region. |

Output thường gặp:

- success/acknowledged ở SDK level;
- HTTP status thành công nếu gọi API raw;
- lỗi nếu bucket đã tồn tại hoặc thiếu quyền.

Trong mini-lab, có thể tạo bucket bằng Console để học UI trước, hoặc để backend tự kiểm tra/tạo khi startup nếu muốn.

---

## 3. Put object

Mục tiêu: upload object.

HTTP mental model:

```http
PUT /{bucket}/{objectKey}
Content-Type: application/pdf
x-amz-meta-tenant-id: 1

<binary body>
```

Input chính:

| Input | Ý nghĩa |
|---|---|
| bucket | Bucket đích. |
| object key | Key backend sinh ra, ví dụ `tenant/1/documents/2026/05/<uuid>.pdf`. |
| stream/file | Nội dung file. |
| size | Kích thước object nếu biết. |
| content type | `application/pdf`, `image/png`, ... |
| metadata | Custom metadata nếu cần, ví dụ `tenant-id`, `uploaded-by`. |

Output thường gặp:

| Field | Ý nghĩa |
|---|---|
| ETag | Identifier/checksum-like value của object version; không nên luôn giả định là MD5 trong mọi mode. |
| version id | Có nếu bucket bật versioning. |
| bucket/object key | SDK thường trả hoặc app tự biết từ request. |

Map sang MinIO Java SDK:

```text
MinioClient.putObject(PutObjectArgs)
```

---

## 4. Get object

Mục tiêu: download object.

HTTP mental model:

```http
GET /{bucket}/{objectKey}
```

Input chính:

| Input | Ý nghĩa |
|---|---|
| bucket | Bucket chứa object. |
| object key | Key đã lưu trong DB metadata. |
| range | Optional nếu download một phần file. |

Output thường gặp:

- binary stream;
- headers như `Content-Type`, `Content-Length`, `ETag`;
- lỗi `NoSuchKey`/not found nếu object không tồn tại.

Map sang MinIO Java SDK:

```text
MinioClient.getObject(GetObjectArgs)
```

Backend không nên trả raw SDK response. Controller nên stream file với headers cần thiết.

---

## 5. Stat object / Head object

Mục tiêu: lấy metadata mà không download body.

HTTP mental model:

```http
HEAD /{bucket}/{objectKey}
```

Input:

- bucket;
- object key.

Output:

| Field | Ý nghĩa |
|---|---|
| size/content length | Kích thước object. |
| content type | MIME type. |
| ETag | Object tag/check value. |
| last modified | Thời điểm object thay đổi. |
| user metadata | Custom metadata nếu có. |

Map sang MinIO Java SDK:

```text
MinioClient.statObject(StatObjectArgs)
```

Use case:

- kiểm tra object tồn tại;
- đối chiếu DB metadata với object metadata;
- debug upload.

---

## 6. Remove object

Mục tiêu: xóa object khỏi bucket.

HTTP mental model:

```http
DELETE /{bucket}/{objectKey}
```

Input:

- bucket;
- object key;
- version id nếu dùng versioning.

Output:

- thường không cần body;
- lỗi nếu thiếu quyền hoặc object/bucket không tồn tại tùy behavior.

Map sang MinIO Java SDK:

```text
MinioClient.removeObject(RemoveObjectArgs)
```

Trong backend nghiệp vụ, delete nên đi qua metadata query theo tenant trước.

---

## 7. List objects

Mục tiêu: liệt kê object theo prefix.

Input:

| Input | Ý nghĩa |
|---|---|
| bucket | Bucket cần list. |
| prefix | Ví dụ `tenant/1/documents/`. |
| recursive | Có list sâu theo prefix hay không. |
| continuation token | Dùng khi phân trang. |

Output:

- danh sách object summaries;
- key, size, ETag, last modified;
- pagination token nếu còn nhiều object.

Map sang MinIO Java SDK:

```text
MinioClient.listObjects(ListObjectsArgs)
```

Cảnh báo: list prefix không thay thế DB query. Với app nghiệp vụ, metadata list nên lấy từ PostgreSQL để có filter/search/audit tốt hơn.

---

## 8. Presigned GET/PUT URL

Mục tiêu: tạo URL tạm để client upload/download trực tiếp tới object storage.

Input:

| Input | Ý nghĩa |
|---|---|
| method | GET hoặc PUT. |
| bucket/object key | Object cần truy cập. |
| expiry | Thời hạn URL. |
| headers/conditions | Optional, ví dụ content type. |

Output:

- URL dài có query signature và expiry.

Map sang MinIO Java SDK:

```text
MinioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs)
```

Rule:

- Backend phải authorize trước khi cấp URL.
- Không log/paste full presigned URL vào report.
- URL hết hạn nhưng trong thời hạn thì ai có URL thường có thể dùng được theo permission đã ký.

Phase 1: chỉ awareness, chưa cần implement ngay.

---

## 9. Object key design examples

Backend nên tự sinh object key.

Ví dụ attachment chung:

```text
tenant/{tenantId}/attachments/{yyyy}/{mm}/{uuid}-{safeFilename}
```

Ví dụ chứng từ kế toán:

```text
tenant/{tenantId}/documents/{yyyy}/{mm}/{documentId}/{uuid}.pdf
```

Ví dụ export report:

```text
tenant/{tenantId}/exports/{yyyy}/{mm}/{jobId}.xlsx
```

Không nên dùng trực tiếp filename client gửi:

```text
invoice.pdf
../../other-tenant/file.pdf
tenant/2/private.pdf
```

Tên gốc nên lưu ở DB metadata để hiển thị, còn object key nên là key an toàn do backend sinh.

---

## 10. Standard error shape ở mức học tập

S3/MinIO error thường có các thông tin như:

| Field | Ý nghĩa |
|---|---|
| code | Mã lỗi như `NoSuchBucket`, `NoSuchKey`, `AccessDenied`. |
| message | Mô tả lỗi. |
| bucket/object key | Có thể xuất hiện trong lỗi. |
| request id/host id | Dùng debug server-side. |
| HTTP status | `404`, `403`, `400`, `500`, ... |

Common cases:

| Case | Ý nghĩa backend |
|---|---|
| Bucket not found | Lab chưa tạo bucket hoặc config sai bucket. |
| Object not found | DB metadata trỏ tới object không tồn tại, hoặc key sai. |
| Access denied | Credential/policy/bucket private không cho operation. |
| Invalid object key | Key rỗng, quá lạ, hoặc backend sinh sai. |
| Connection refused | MinIO chưa chạy hoặc endpoint/port sai. |
| Content type/size mismatch | Backend validate file hoặc client gửi sai header/body. |

Backend nên log đủ để debug, nhưng response API cho client nên gọn:

```text
404 File not found
403 Forbidden
400 Invalid file
503 Storage unavailable
```

Không expose access key, secret, internal bucket policy hoặc raw stack trace.

---

## 11. Map API shape sang mini-lab

| Use case | S3 operation | Backend method dự kiến |
|---|---|---|
| Upload file | `putObject` | `FileStorageGateway.putObject(...)` |
| Download file | `getObject` | `FileStorageGateway.getObject(...)` |
| Check object | `statObject` | `FileStorageGateway.statObject(...)` |
| Delete file | `removeObject` | `FileStorageGateway.removeObject(...)` |
| Debug console/list | `listObjects` | Chỉ dùng lab/admin, không thay DB list |
| Future direct upload/download | presigned URL | Optional later |

---

## Nguồn tham khảo chuẩn

- [AWS S3 API Reference](https://docs.aws.amazon.com/AmazonS3/latest/API/Type_API_Reference.html)
- [AWS S3 PutObject](https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html)
- [AWS S3 ListObjects](https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjects.html)
- [AWS S3 object metadata](https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingMetadata.html)
- [MinIO Java API - MinioClient](https://minio-java.min.io/io/minio/MinioClient.html)
- [MinIO Java API - PutObjectArgs](https://minio-java.min.io/io/minio/PutObjectArgs.html)
- [MinIO Java API - GetObjectArgs](https://minio-java.min.io/io/minio/GetObjectArgs.html)
