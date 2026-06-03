# Kafka/async messaging mini-lab plan

## Vai trò tài liệu

Tài liệu này là checklist thực hành cho Kafka mini-lab. Phần foundation đọc ở:

- `kafka-async-messaging.md`
- `kafka-event-shapes.md`
- `kafka-code-guide-spring-boot.md`

Mục tiêu hiện tại là chuẩn bị đường học để bạn tự code producer/consumer nhỏ, không xây full event-driven ERP.

---

## 1. Mục tiêu mini-lab

Flow nhỏ đề xuất:

```text
MasterData create/update thành công trong PostgreSQL
-> build MasterDataChangedEvent
-> publish vào topic master-data-events
-> consumer nhận event và log/projection nhỏ
```

Giữ nguyên nguyên tắc:

- PostgreSQL là source of truth.
- Kafka chỉ truyền event bất đồng bộ.
- Không dùng Kafka để thay thế API query hoặc database.
- Không làm Debezium/outbox ở bước này.
- Không tách thành nhiều microservices thật trong Phase 1.

---

## 2. Phạm vi nên làm

### Nên làm trong mini-lab

- Tạo topic local `master-data-events`.
- Tự code producer gửi `MasterDataChangedEvent`.
- Event có `eventId`, `tenantId`, `aggregateId`, `code`, `changeType`.
- Kafka key có tenant scope, ví dụ `tenant:1:master-data:101`.
- Consumer ban đầu chỉ log event hoặc lưu projection/audit-style rất nhỏ.
- Giữ `APP_MESSAGING_ENABLED=false` mặc định để `make app-test` không cần Kafka.

### Không làm ngay

- Không làm full accounting workflow.
- Không làm retry/dead-letter framework đầy đủ.
- Không làm Debezium CDC.
- Không gửi file binary, token, password hoặc secret qua event.
- Không publish event trước khi DB write thành công.

---

## 3. Artifact hiện tại

- `lab-code/kafka-lab/`: Docker Compose và lệnh local Kafka.
- `app.messaging.*`: config placeholder trong `application.yml`.
- `com.viettel.demo.messaging.MessagingProperties`: bind config.
- `com.viettel.demo.messaging.MasterDataChangedEvent`: DTO event shape.
- `com.viettel.demo.messaging.MasterDataEventPublisher`: TODO boundary cho producer.

Skeleton chưa thêm `spring-kafka` và chưa có `KafkaTemplate`. Đây là chủ ý để bạn tự code phần producer/consumer chính.

---

## 4. Các bước tự code đề xuất

### Bước 1 - Chạy Kafka local

```bash
cd lab-code
make kafka-up
make kafka-status
```

Nếu cần kiểm tra topic bằng CLI trong container, xem `lab-code/kafka-lab/README.md`.

### Bước 2 - Thêm Spring Kafka dependency

Trong `lab-code/tenant-demo/pom.xml`, thêm:

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

Sau đó chạy:

```bash
cd lab-code/tenant-demo
./mvnw validate
```

### Bước 3 - Tạo Kafka config/publisher thật

Gợi ý:

```text
KafkaConfig
KafkaMasterDataEventPublisher
```

Publisher cần:

- chỉ gửi khi `app.messaging.enabled=true`;
- dùng topic từ `MessagingProperties`;
- build key từ `tenantId + aggregateId`;
- serialize event JSON qua Spring Kafka serializer;
- log ngắn khi publish.

### Bước 4 - Nối vào service sau DB write

Sau `repository.save(...)` thành công trong create/update:

```text
saved entity
-> MasterDataChangedEvent.from(saved, "CREATED"/"UPDATED")
-> publisher.publish(event)
```

Caveat: Phase 1 có thể chấp nhận DB save xong nhưng publish fail sẽ mất event. Production thường cân nhắc outbox/retry.

### Bước 5 - Tạo consumer nhỏ

Consumer ban đầu chỉ nên:

- nhận `MasterDataChangedEvent`;
- log `eventId`, `tenantId`, `aggregateId`, `changeType`;
- không xử lý nghiệp vụ kế toán thật;
- ghi chú duplicate/idempotency caveat.

---

## 5. Verification checklist

- [ ] `make app-test` pass khi `APP_MESSAGING_ENABLED=false`.
- [ ] `make kafka-up` chạy Kafka local.
- [ ] App chạy với `APP_MESSAGING_ENABLED=true`.
- [ ] Tạo/update master data thành công.
- [ ] Producer log cho thấy event đã publish.
- [ ] Consumer log cho thấy event đã nhận.
- [ ] Event có `tenantId`.
- [ ] Kafka key có tenant scope.
- [ ] Không có token/secret/raw file trong event payload.
- [ ] Summary ghi rõ caveat duplicate/idempotency và DB/Kafka consistency.

---

## 6. Done criteria

Milestone #15 chỉ nên đóng khi có đủ:

- Docs foundation + code guide đã đọc.
- Kafka local lab chạy được hoặc có lý do rõ nếu chỉ làm awareness.
- Producer/consumer nhỏ tự code và được review.
- `make app-test` vẫn pass khi messaging disabled.
- Có evidence ngắn: command/log/HTTP flow, không paste log dài.
- `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` có summary Kafka ngắn.

---

## 7. Câu hỏi tự kiểm trước khi nhờ review

- Event của mình là event hay command?
- Event có đủ tenant context không?
- Consumer có thể nhận duplicate thì có nguy hiểm không?
- Mình có đang dùng Kafka để thay database/query API không?
- Nếu publish fail sau DB save, mình đã ghi caveat chưa?
- Test hiện tại có bị phụ thuộc Kafka container không?
