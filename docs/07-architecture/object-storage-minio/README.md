# MinIO / S3 Object Storage

## Folder này chứa gì?

Nhóm này giải thích object storage, S3-compatible API, MinIO local lab và cách backend lưu binary object riêng với metadata tenant-aware trong PostgreSQL.

## Reading Order

1. [minio-object-storage.md](minio-object-storage.md) - foundation: bucket, object, key, metadata, private bucket.
2. [minio-s3-api-shapes.md](minio-s3-api-shapes.md) - put/get/stat/delete/list/presigned request shapes.
3. [minio-code-guide-spring-boot.md](minio-code-guide-spring-boot.md) - gateway/adapter + metadata repository + service/controller flow.
4. [minio-admin-console-guide.md](minio-admin-console-guide.md) - local MinIO Console walkthrough.
5. [minio-advanced-object-management.md](minio-advanced-object-management.md) - optional backlog: presigned URL expiry, lifecycle, versioning, object lock/retention.

## Trạng Thái

- Basic upload/download mini-lab đã đóng.
- Advanced object management là optional/later, không chặn Redis/Kafka/Observability.

## Caveat

Client không được tự quyết object key. Backend phải authorize upload/download và metadata lookup phải tenant-aware.
