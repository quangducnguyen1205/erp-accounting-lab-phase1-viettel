# MinIO Console guide cho mini-lab

## Vai trò tài liệu

Guide này giúp làm quen MinIO Console ở mức đủ dùng cho Phase 1. Đây là UI để quan sát bucket/object local, không phải nơi thiết kế production storage policy.

Điều kiện:

```bash
cd lab-code
make -f Makefile.legacy minio-up
make minio-status
```

Console local:

```text
http://localhost:19001
```

Dev credentials mặc định trong `lab-code/minio-lab/docker-compose.yml`:

```text
username: minioadmin
password: minioadmin
```

Không dùng credential này cho production.

---

## 1. Mở MinIO Console

Truy cập:

```text
http://localhost:19001
```

![MinIO Console login](../assets/minio/01-minio-login.png)

Kết quả mong đợi:

- thấy form đăng nhập MinIO Console;
- không cần tạo account mới;
- dùng dev credential local.

---

## 2. Xem bucket list

Sau khi login, vào Object Browser/Buckets.

![MinIO bucket list](../assets/minio/02-minio-bucket-list.png)

Kết quả mong đợi:

- thấy danh sách bucket;
- nếu mới chạy lần đầu có thể chưa có bucket nào;
- bucket lab dự kiến là `tenant-demo-files`.

---

## 3. Tạo bucket lab

Trong Console:

```text
Buckets/Object Browser
-> Create bucket
-> Bucket name: tenant-demo-files
-> Create
```

![MinIO create bucket](../assets/minio/03-minio-create-bucket.png)

Kết quả mong đợi:

- bucket `tenant-demo-files` xuất hiện;
- giữ bucket private cho lab;
- không bật public access cho chứng từ/file tenant.

---

## 4. Xem bucket details

Click vào bucket `tenant-demo-files`.

![MinIO bucket details](../assets/minio/04-minio-bucket-details.png)

Kết quả mong đợi:

- có thể xem object trong bucket;
- chưa cần cấu hình lifecycle/replication/policy nâng cao;
- production policy để sau.

---

## 5. Upload object thủ công để quan sát

Có thể upload một file nhỏ thủ công trong Console để hiểu object/key/metadata. File này chỉ phục vụ quan sát UI, không phải flow backend chính.

![MinIO upload object](../assets/minio/05-minio-upload-object.png)

Kết quả mong đợi:

- object xuất hiện trong bucket;
- Console hiển thị key/name, size, last modified;
- nếu sau này backend tự upload, object key nên do backend sinh.

---

## 6. Xem object details/metadata

Click vào object đã upload.

![MinIO object details](../assets/minio/06-minio-object-details.png)

Kết quả mong đợi:

- thấy thông tin object như size, content type/metadata nếu có;
- không dùng Console làm source of truth nghiệp vụ;
- metadata nghiệp vụ vẫn nên nằm trong PostgreSQL.

---

## 7. Cần nhớ gì sau khi xem Console?

- Bucket là nơi chứa object.
- Object key nhìn giống path, nhưng không phải thư mục thật.
- Console giúp debug, còn backend phải authorize upload/download.
- Private bucket là default an toàn cho chứng từ tenant.
- Access key/secret chỉ dùng cho backend/service, không đưa cho frontend.
- Presigned URL nếu dùng sau này phải được cấp bởi backend sau khi check quyền.

---

## Common mistakes

- Tạo bucket xong để public nhầm.
- Upload thủ công rồi tưởng đó là flow production.
- Copy object key từ Console rồi cho client gọi trực tiếp.
- Quên rằng object trên MinIO có thể lệch với metadata trong DB.
- Lưu credential thật vào docs hoặc screenshot.
