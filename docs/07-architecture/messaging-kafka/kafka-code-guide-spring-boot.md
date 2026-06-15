# Kafka code guide cho Spring Boot

## Vai trò tài liệu

Tài liệu này hướng dẫn hình dạng code Spring Boot cho Kafka/async messaging mini-lab. Nó không thay thế concept docs:

- `kafka-async-messaging.md`
- `kafka-event-shapes.md`
- `kafka-mini-lab-plan.md`

Mục tiêu code:

```text
Service/use-case xử lý DB trước
-> Publisher nhận event DTO
-> Kafka producer gửi event
-> Consumer xử lý async nhỏ
```

Hiện tại repo đã có reference implementation nhỏ để học producer/consumer thật,
nhưng vẫn giữ scope Phase 1: chưa có outbox, retry topic, DLT hoặc full
microservice architecture.

---

## 1. Chọn integration approach

### Option 1: Gọi KafkaTemplate trực tiếp trong service

Ưu điểm:

- ít class;
- nhanh cho demo rất nhỏ.

Nhược điểm:

- service bị trộn business logic với messaging details;
- khó test service;
- dễ quên feature flag/tenant/event shape.

### Option 2: Publisher component quanh KafkaTemplate

Ưu điểm:

- service nói bằng ngôn ngữ nghiệp vụ: publish `MasterDataChangedEvent`;
- publisher giữ topic/key/KafkaTemplate details;
- dễ tắt messaging bằng feature flag;
- giống Gateway/Adapter pattern đã dùng ở Elasticsearch/MinIO/Redis.

Nhược điểm:

- thêm một lớp nhỏ;
- cần tránh biến publisher thành framework.

### Option 3: Event bus/domain event framework

Phù hợp khi hệ thống lớn, nhiều domain events, nhiều handler. Không cần cho Phase 1.

### Khuyến nghị cho repo này

Dùng **publisher component nhỏ quanh KafkaTemplate**.

Repo hiện dùng:

- `MasterDataEventPublisher` interface để service không phụ thuộc chi tiết Kafka.
- `NoOpMasterDataEventPublisher` khi `APP_MESSAGING_ENABLED=false`.
- `KafkaMasterDataEventPublisher` khi `APP_MESSAGING_ENABLED=true`.
- `MasterDataChangedEventConsumer` dùng `@KafkaListener` để log event nhận được.

---

## 2. Dependency/config

Dependency hiện đã được thêm:

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

Config gợi ý:

```yaml
app:
  messaging:
    enabled: ${APP_MESSAGING_ENABLED:false}
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:19092}
    master-data-topic: ${KAFKA_MASTER_DATA_TOPIC:master-data-events}
```

Rule:

- `APP_MESSAGING_ENABLED=false` mặc định để `make app-test` không cần Kafka.
- Không bật Kafka trong test hiện tại nếu chưa có test profile riêng.
- Không hardcode secret/token trong event hoặc config.
- `tenant-demo` hiện là producer-only. Consumer group thật trong final demo nằm ở `audit-log-service` và `search-service`.

---

## 3. Package/class shape đề xuất

```text
com.viettel.demo.messaging
├── MessagingProperties
├── MasterDataChangedEvent
├── MasterDataEventPublisher
├── NoOpMasterDataEventPublisher
├── KafkaMessagingConfig
└── KafkaMasterDataEventPublisher
```

Không cần tạo generic event framework trong Phase 1.

---

## 4. Responsibility của từng class

| Class | Trách nhiệm | Không nên làm |
|---|---|---|
| `MessagingProperties` | Bind feature flag, bootstrap servers và topic config. | Không chứa business logic. |
| `MasterDataChangedEvent` | Event DTO nhỏ, có `tenantId`, `eventId`, `aggregateId`. | Không chứa JPA entity/raw payload lớn. |
| `MasterDataEventPublisher` | Boundary/interface để service gọi publish event. | Không query DB, không tự lấy tenant từ request body. |
| `NoOpMasterDataEventPublisher` | Giữ app behavior cũ khi messaging disabled. | Không giả vờ gửi event. |
| `KafkaMessagingConfig` | ProducerFactory và KafkaTemplate cho `tenant-demo`. | Không chứa business logic. |
| `KafkaMasterDataEventPublisher` | Build topic/key, gọi `KafkaTemplate.send(...)`, log metadata. | Không thay PostgreSQL source of truth. |

---

## 5. Producer flow đề xuất

Sau khi DB write thành công trong `MasterDataService.create/update`:

```text
create/update MasterData
-> repository.save(...)
-> build MasterDataChangedEvent từ saved entity
-> publisher.publish(event)
```

Lưu ý consistency:

- Nếu publish trước khi DB commit thành công, consumer có thể thấy event cho dữ liệu chưa thật sự lưu.
- Nếu DB save xong nhưng publish fail, sẽ mất event nếu chưa có outbox/retry.
- Phase 1 chấp nhận caveat này và ghi rõ; chưa làm outbox/Debezium.
- Implementation hiện tại cố tình wait kết quả send ngắn để Kafka unavailable fail rõ ràng khi `APP_MESSAGING_ENABLED=true`.

---

## 6. Consumer flow trong final demo

Trong mini-lab rất sớm, consumer có thể chỉ log event để học `@KafkaListener`. Trong final demo hiện tại, `tenant-demo` không còn tự consume event của chính nó.

Consumer thật là:

- `audit-log-service`: lưu audit/activity event tenant-aware;
- `search-service`: cập nhật Elasticsearch projection.

Flow consumer:

```text
@KafkaListener(...)
-> nhận MasterDataChangedEvent
-> kiểm tra eventId/idempotency nếu service cần
-> lưu audit hoặc update projection
```

Sau đó mới cân nhắc:

- lưu audit-style record nhỏ;
- update projection;
- retry/dead-letter handling.

Không đưa logic kế toán thật vào consumer mini-lab.

---

## 7. Tenant safety

Producer:

- lấy `tenantId` từ entity đã tenant-aware hoặc `TenantContext`;
- event payload phải có `tenantId`;
- Kafka key nên có tenant scope.

Consumer:

- không xử lý event như global data;
- nếu ghi DB/projection, write phải có tenant context rõ;
- không nhận tenant từ nguồn không trusted nếu event chưa validate/deserialize đúng.

---

## 8. Test/verification style

Baseline:

```bash
cd lab-code
make app-test
```

Phải pass khi `APP_MESSAGING_ENABLED=false`.

Manual Kafka verification sau khi tự code:

```bash
cd lab-code
make -f Makefile.legacy kafka-up
make -f Makefile.legacy kafka-status
```

Sau đó:

- tạo/update `master_data`;
- kiểm tra producer log;
- dùng consumer log để thấy event đã nhận;
- verify event có `tenantId`;
- verify duplicate/idempotency caveat được ghi lại.

---

## 9. Common mistakes trong code

- Inject `KafkaTemplate` thẳng vào controller.
- Publish event trước khi DB write.
- Quên feature flag làm test fail khi Kafka chưa chạy.
- Event thiếu `tenantId`.
- Event chứa quá nhiều field/raw entity.
- Consumer xử lý duplicate không an toàn.
- Bắt lỗi publish rồi nuốt âm thầm, làm debug khó.
- Dùng Kafka như request/response API.

---

## 10. Implementation hiện tại

Đã có:

- `spring-kafka` dependency.
- `KafkaTemplate<String, MasterDataChangedEvent>`.
- JSON serializer/deserializer cho event DTO.
- Producer publish sau create/update.
- Consumer log event.
- Feature flag `APP_MESSAGING_ENABLED=false` mặc định.

Chưa có:

- outbox pattern;
- idempotency storage;
- retry topic/DLT;
- schema versioning;
- consumer projection/audit table.

---

## Nguồn tham khảo chuẩn

- [Spring for Apache Kafka Reference](https://docs.spring.io/spring-kafka/reference/)
- [Spring Boot Kafka support](https://docs.spring.io/spring-boot/reference/messaging/kafka.html)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
