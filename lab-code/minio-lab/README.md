# MinIO local mini-lab

## Mục tiêu

Folder này chạy MinIO local để học object storage/S3-compatible API. Đây là lab cho Phase 1, không phải production deployment.

Đọc trước:

- `../../docs/07-architecture/object-storage-minio/minio-object-storage.md`
- `../../docs/07-architecture/object-storage-minio/minio-s3-api-shapes.md`
- `../../docs/07-architecture/object-storage-minio/minio-code-guide-spring-boot.md`
- `../../docs/07-architecture/object-storage-minio/minio-admin-console-guide.md`

## Local services

| Service | URL |
|---|---|
| S3 API endpoint | `http://localhost:19000` |
| MinIO Console | `http://localhost:19001` |

Dev credentials:

```text
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin
```

Đây là dev default của local lab. Không dùng cho production và không commit secret thật.

## Start/stop

Từ `lab-code/`:

```bash
make -f Makefile.legacy minio-up
make minio-status
make -f Makefile.legacy minio-down
```

Hoặc chạy trực tiếp trong folder này:

```bash
docker compose up -d
docker compose down
```

## Kiểm tra MinIO sống

```bash
curl http://localhost:19000/minio/health/ready
```

Expected:

```text
200 OK
```

## Tạo bucket bằng Console

1. Mở `http://localhost:19001`.
2. Login bằng dev credentials.
3. Tạo bucket:

```text
tenant-demo-files
```

Giữ bucket private. Không bật public access cho chứng từ/file tenant.

## Config dự kiến cho file-service

Trong `.env` local của `lab-code/file-service` nếu muốn override default:

```env
MINIO_ENDPOINT=http://localhost:19000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=tenant-demo-files
MINIO_REGION=us-east-1
```

`tenant-demo` không còn sở hữu `/api/files`; upload/download runtime đã tách sang `file-service`.

## Mini-lab flow dự kiến

1. Start PostgreSQL + Keycloak + MinIO.
2. Start `file-service` bằng `make -f Makefile.legacy file-run-logs`.
3. Upload file bằng API backend qua Kong: `POST /api/files`.
4. `file-service` tạo metadata tenant-aware trong PostgreSQL.
5. `file-service` upload binary object vào MinIO.
6. Download bằng `fileId`, không bằng raw object key.
7. Tenant khác không download được file này.

## Cleanup

```bash
make -f Makefile.legacy minio-down
```

Lệnh này dừng container compose. Volume `minio_data` vẫn giữ dữ liệu nếu chưa xóa volume.

Nếu cần reset lab thật sự, dùng Docker Desktop hoặc lệnh Docker volume thủ công sau khi chắc chắn không cần dữ liệu. Không reset volume khi đang chuẩn bị demo nếu chưa kiểm tra.

## Out of scope hiện tại

- production MinIO cluster;
- bucket policy phức tạp;
- lifecycle/retention/object lock;
- presigned URL production flow;
- virus scanning;
- encryption/key management;
- distributed transaction giữa DB và MinIO.
