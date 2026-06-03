# Kafka/async messaging mini-lab plan

## Vai trò tài liệu

Tài liệu này là checklist thực hành cho Kafka mini-lab. Phần foundation đọc ở:

- `kafka-async-messaging.md`
- `kafka-event-shapes.md`
- `kafka-code-guide-spring-boot.md`

Mục tiêu hiện tại là chạy một reference implementation nhỏ để đọc/debug event-driven code thật, không xây full event-driven ERP.

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
- `com.viettel.demo.messaging.MasterDataEventPublisher`: boundary/interface cho service.
- `com.viettel.demo.messaging.NoOpMasterDataEventPublisher`: giữ app behavior cũ khi messaging disabled.
- `com.viettel.demo.messaging.KafkaMessagingConfig`: config Spring Kafka nhỏ.
- `com.viettel.demo.messaging.KafkaMasterDataEventPublisher`: producer dùng `KafkaTemplate`.
- `com.viettel.demo.messaging.MasterDataChangedEventConsumer`: consumer dùng `@KafkaListener` và log event.

---

## 4. Cách chạy implementation hiện tại

### Bước 1 - Chạy Kafka local

```bash
cd lab-code
make kafka-up
make kafka-status
```

Nếu cần kiểm tra topic bằng CLI trong container, xem `lab-code/kafka-lab/README.md`.

### Bước 2 - Verify compile/test baseline

Kafka disabled là default, nên test bình thường không cần Kafka:

```bash
cd lab-code
make app-test
```

### Bước 3 - Chạy app với messaging enabled

Ví dụ:

```bash
cd lab-code/tenant-demo
set -a; . ./.env; set +a
APP_MESSAGING_ENABLED=true \
APP_SEARCH_ENABLED=false \
APP_FILE_STORAGE_ENABLED=false \
APP_CACHE_ENABLED=false \
KAFKA_BOOTSTRAP_SERVERS=localhost:19092 \
./mvnw spring-boot:run
```

### Bước 4 - Gọi API create/update

Mở `lab-code/tenant-demo/http/kafka-api.http`.

Expected:

- API create/update trả `201`/`200`.
- Producer log có `Published Kafka event...`.
- Consumer log có `Consumed Kafka event...`.
- Log có `tenantId`, `aggregateId`, `changeType`, `key`.

---

## 5. Flow code hiện tại

```text
POST/PUT /api/master-data
-> MasterDataController
-> MasterDataService
-> repository.save(...)
-> MasterDataChangedEvent.from(saved, "CREATED"/"UPDATED")
-> MasterDataEventPublisher.publish(event)
-> KafkaMasterDataEventPublisher
-> Kafka topic master-data-events
-> MasterDataChangedEventConsumer logs event
```

`MasterDataService` không biết `KafkaTemplate`. Nó chỉ biết publish event qua boundary `MasterDataEventPublisher`.

---

## 6. Verification checklist

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

## 7. Caveat cần nói khi demo

- Chưa có outbox pattern: DB write và Kafka publish không atomic.
- Nếu DB save thành công nhưng Kafka publish fail, mini-lab sẽ fail request rõ ràng nhưng DB có thể đã ghi.
- Consumer chỉ log event, chưa có idempotency storage.
- Kafka có thể deliver duplicate; production consumer cần xử lý theo `eventId`.
- Chưa có retry topic, dead-letter topic hoặc schema versioning.
- Kafka không thay PostgreSQL source of truth.

---

## 8. Done criteria

Milestone #15 chỉ nên đóng khi có đủ:

- Docs foundation + code guide đã đọc.
- Kafka local lab chạy được hoặc có lý do rõ nếu chỉ làm awareness.
- Producer/consumer nhỏ tự code và được review.
- `make app-test` vẫn pass khi messaging disabled.
- Có evidence ngắn: command/log/HTTP flow, không paste log dài.
- `docs/99-tong-ket/nhung-gi-da-nam-duoc.md` có summary Kafka ngắn.

---

## 9. Câu hỏi tự kiểm trước khi nhờ review

- Event của mình là event hay command?
- Event có đủ tenant context không?
- Consumer có thể nhận duplicate thì có nguy hiểm không?
- Mình có đang dùng Kafka để thay database/query API không?
- Nếu publish fail sau DB save, mình đã ghi caveat chưa?
- Test hiện tại có bị phụ thuộc Kafka container không?
