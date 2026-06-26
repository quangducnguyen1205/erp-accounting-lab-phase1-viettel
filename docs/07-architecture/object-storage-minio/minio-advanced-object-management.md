# MinIO advanced object management backlog

## Vai trò tài liệu

Tài liệu này ghi lại các tính năng MinIO/S3 nên học **sau** khi mini-lab upload/download cơ bản đã ổn. Đây là backlog/awareness, không phải blocker cho Phase 1 demo hiện tại.

Scope hiện tại vẫn là:

```text
backend-mediated upload/download
PostgreSQL metadata
MinIO binary object
tenant-aware metadata lookup
private bucket
```

---

## 1. Presigned URL expiry

Presigned URL là URL tạm thời đã được ký để client upload/download trực tiếp tới object storage trong một thời hạn nhất định.

Khi hữu ích:

- file lớn, không muốn stream toàn bộ qua Spring Boot app;
- client cần download trực tiếp từ object storage;
- backend vẫn muốn authorize trước rồi mới cấp URL.

Rủi ro:

- TTL quá dài làm URL bị forward/chia sẻ lâu hơn mong muốn;
- nếu log/paste URL vào report/chat, người khác có thể dùng trong thời hạn còn hiệu lực;
- backend vẫn phải kiểm tra tenant/permission trước khi cấp URL.

Trong MinIO Java SDK, `GetPresignedObjectUrlArgs` có `expiry(...)` để cấu hình thời hạn URL. Phase hiện tại chưa cần vì app đang download qua backend để dễ hiểu tenant check.

---

## 2. Lifecycle / expiration

Lifecycle rule cho phép object storage tự xử lý object theo thời gian, ví dụ tự xóa object sau N ngày.

Use case:

- file tạm;
- export report hết hạn;
- file import staging;
- log hoặc artifact cũ;
- attachment có chính sách lưu giữ theo nghiệp vụ.

Cần cẩn thận:

- PostgreSQL metadata phải biết object đã hết hạn/xóa chưa;
- nếu lifecycle xóa object nhưng DB vẫn còn metadata active, API download sẽ lỗi;
- lifecycle phù hợp với object policy, không thay thế business status trong DB.

Trong MinIO, lifecycle expiration thường được setup bằng MinIO Console hoặc `mc ilm`.

---

## 3. Versioning

Versioning giữ nhiều version của cùng một object key.

Khi hữu ích:

- recovery khi upload nhầm;
- audit thay đổi file;
- bảo vệ trước overwrite/delete nhầm.

Trade-off:

- tốn storage hơn;
- delete không còn đơn giản vì có current version, noncurrent version, delete marker;
- lifecycle rule cần tính cả noncurrent versions;
- backend metadata cần quyết định có lưu `versionId` hay không.

Phase 1 hiện dùng object key có UUID/fileId nên chưa cần versioning.

---

## 4. Object Lock / retention / legal hold

Object Lock/retention bảo vệ object khỏi bị xóa hoặc sửa trong một thời hạn hoặc theo legal hold.

Khi hữu ích:

- chứng từ cần lưu theo quy định;
- audit/compliance;
- tài liệu không được sửa/xóa sau khi phát hành.

Cần thiết kế kỹ:

- retention có thể làm delete endpoint không còn xóa được object ngay;
- legal hold thường là indefinite hold cho tới khi người có quyền gỡ;
- behavior này ảnh hưởng trực tiếp đến nghiệp vụ, audit và support.

Không nên bật chỉ để “thử cho vui” trên flow chính nếu chưa hiểu hậu quả với delete/lifecycle.

---

## 5. Bucket policy và private bucket

Rule mặc định cho backend nghiệp vụ:

- bucket private;
- client không gọi MinIO trực tiếp bằng access key;
- backend authorize rồi stream file hoặc cấp presigned URL;
- không public bucket chứa chứng từ/attachment tenant.

Public bucket chỉ phù hợp cho asset thật sự public như logo, ảnh marketing, file public.

---

## 6. Học sau này theo thứ tự nào?

Khi quay lại advanced object management, nên đi theo thứ tự:

1. Presigned GET URL với TTL ngắn.
2. Presigned PUT URL nếu cần direct upload.
3. Lifecycle expiration cho file tạm/export.
4. Versioning và cách metadata DB lưu `versionId`.
5. Object Lock/retention/legal hold cho compliance document.
6. Tương tác giữa lifecycle, versioning và retention.

Tiêu chí hoàn thành khi học sau:

- có mini-lab riêng, không làm hỏng flow upload/download hiện tại;
- có HTTP/curl hoặc Console evidence;
- có ghi rõ risk/caveat;
- không commit presigned URL thật, token, access key hoặc file nhạy cảm.

---

## 7. Áp dụng vào repo hiện tại

Hiện tại **không implement** các phần này. Trong lộ trình Phase 1 ban đầu, Redis/cache được ưu tiên trước.

Nếu sau này cần chọn một feature nhỏ để demo, nên chọn:

- presigned GET URL TTL 1-5 phút cho download file đã authorize; hoặc
- lifecycle expiration cho file export tạm.

Không nên bắt đầu bằng Object Lock/retention vì nó liên quan compliance và thay đổi behavior delete.

---

## Nguồn tham khảo chuẩn

- [MinIO Object Lifecycle Management](https://min.io/docs/minio/linux/administration/object-management/object-lifecycle-management.html)
- [MinIO Automatic Object Expiration](https://min.io/docs/minio/linux/administration/object-management/create-lifecycle-management-expiration-rule.html)
- [MinIO Bucket Versioning](https://min.io/docs/minio/linux/administration/object-management/object-versioning.html)
- [MinIO Object Locking / Retention](https://min.io/docs/minio/linux/administration/object-management/object-retention.html)
- [MinIO Java API - GetPresignedObjectUrlArgs](https://minio-java.min.io/io/minio/GetPresignedObjectUrlArgs.html)
