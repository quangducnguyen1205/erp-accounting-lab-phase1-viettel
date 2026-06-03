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

Hiện tại repo chỉ chuẩn bị skeleton/TODO. Chưa implement producer/consumer thật.

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

Dùng **publisher component nhỏ quanh KafkaTemplate** khi bắt đầu implement thật.

Ở skeleton hiện tại:

- chưa thêm `spring-kafka`;
- chưa inject `KafkaTemplate`;
- có DTO/event shape và publisher TODO để bạn tự nối sau.

---

## 2. Dependency/config khi bắt đầu code thật

Khi tự implement Kafka producer/consumer, thêm dependency:

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
    consumer-group-id: ${KAFKA_CONSUMER_GROUP_ID:tenant-demo-master-data}
```

Rule:

- `APP_MESSAGING_ENABLED=false` mặc định để `make app-test` không cần Kafka.
- Không bật Kafka trong test hiện tại nếu chưa có test profile riêng.
- Không hardcode secret/token trong event hoặc config.

---

## 3. Package/class shape đề xuất

```text
com.viettel.demo.messaging
├── MessagingProperties
├── MasterDataChangedEvent
├── MasterDataEventPublisher
└── MasterDataEventConsumer      (tạo sau khi tự code consumer)
```

Nếu thêm Kafka thật:

```text
KafkaConfig
KafkaMasterDataEventPublisher
MasterDataEventConsumer
```

Không cần tạo generic event framework trong Phase 1.

---

## 4. Responsibility của từng class

| Class | Trách nhiệm | Không nên làm |
|---|---|---|
| `MessagingProperties` | Bind feature flag, bootstrap servers, topic/group config. | Không chứa business logic. |
| `MasterDataChangedEvent` | Event DTO nhỏ, có `tenantId`, `eventId`, `aggregateId`. | Không chứa JPA entity/raw payload lớn. |
| `MasterDataEventPublisher` | Boundary để service gọi publish event. | Không query DB, không tự lấy tenant từ request body. |
| `KafkaMasterDataEventPublisher` | Khi thêm Kafka thật: build key, gọi `KafkaTemplate.send(...)`. | Không thay PostgreSQL source of truth. |
| `MasterDataEventConsumer` | Khi thêm consumer: log hoặc xử lý projection nhỏ. | Không giả định event chỉ xử lý đúng một lần. |

---

## 5. Producer flow đề xuất

Sau khi DB write thành công:

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

---

## 6. Consumer flow đề xuất

Consumer ban đầu chỉ nên làm việc nhỏ:

```text
@KafkaListener(...)
-> nhận MasterDataChangedEvent
-> log tenantId/eventId/aggregateId/changeType
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
make kafka-up
make kafka-status
```

Sau đó:

- tạo/update `master_data`;
- kiểm tra producer log;
- dùng Kafka CLI hoặc consumer log để thấy event;
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

## 10. Skeleton hiện tại

Skeleton hiện chỉ để sẵn:

- config placeholder `app.messaging.*`;
- `MessagingProperties`;
- `MasterDataChangedEvent`;
- `MasterDataEventPublisher` TODO.

Bạn sẽ tự thêm `spring-kafka`, `KafkaTemplate`, producer/consumer thật khi bắt đầu phần thực hành.

---

## Nguồn tham khảo chuẩn

- [Spring for Apache Kafka Reference](https://docs.spring.io/spring-kafka/reference/)
- [Spring Boot Kafka support](https://docs.spring.io/spring-boot/reference/messaging/kafka.html)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
