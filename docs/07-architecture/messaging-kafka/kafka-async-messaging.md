# Kafka / async messaging foundation

> [!NOTE]
> **Phạm vi tài liệu:** Tính năng nhắn tin không đồng bộ với Kafka hiện ở mức cơ bản để chứng minh luồng luân chuyển sự kiện. Các kỹ thuật hardening nâng cao (như Outbox, Dead Letter Queue - DLT, Idempotency Framework) thuộc phạm vi mở rộng trong tương lai.

## Vai trò tài liệu

Tài liệu này là nền tảng để hiểu Kafka trong backend architecture. Mục tiêu chưa phải xây full event platform, mà là biết khi nào dùng async messaging, Kafka nằm ở đâu trong hệ thống SaaS/ERP và những lỗi tư duy phổ biến cần tránh.

Đọc kèm:

- `kafka-event-shapes.md` - event/message shape cụ thể.
- `kafka-code-guide-spring-boot.md` - shape code Spring Boot.

---

## 1. Kafka là gì?

Apache Kafka là một distributed event streaming platform. Ở mức backend beginner, có thể hiểu Kafka là hệ thống giúp service publish message/event vào topic, rồi service khác consume message đó theo cách bất đồng bộ.

Ví dụ:

```text
MasterDataService
-> publish MasterDataChangedEvent
-> Kafka topic master-data-events
-> Consumer xử lý log/projection/notification sau đó
```

Kafka không phải database chính. Kafka giữ event log để consumer đọc, nhưng dữ liệu nghiệp vụ source of truth vẫn nên nằm trong PostgreSQL hoặc database nghiệp vụ.

---

## 2. HTTP sync vs async messaging

### Synchronous HTTP call

```text
Client/Service A -> gọi HTTP -> Service B -> trả response ngay
```

Phù hợp khi caller cần kết quả ngay:

- lấy danh sách dữ liệu;
- tạo record và cần response;
- validate nghiệp vụ trực tiếp.

Rủi ro:

- Service A phụ thuộc Service B đang sống;
- timeout làm request chậm;
- chain nhiều service dễ khó debug.

### Asynchronous messaging

```text
Service A -> publish event -> Kafka -> Service B consume sau
```

Phù hợp khi việc sau không cần hoàn tất ngay trong request hiện tại:

- gửi notification;
- cập nhật search projection;
- audit/log event;
- xử lý background workflow;
- đồng bộ sang hệ thống ngoài.

Trade-off:

- không có response nghiệp vụ ngay từ consumer;
- data có thể eventual consistency;
- phải xử lý duplicate/retry/idempotency.

---

## 3. Core concepts

| Khái niệm | Ý nghĩa ngắn |
|---|---|
| Broker | Server Kafka lưu topic/partition và phục vụ producer/consumer. |
| Topic | Kênh chứa message cùng loại, ví dụ `master-data-events`. |
| Partition | Topic được chia thành nhiều partition để scale và giữ order trong từng partition. |
| Offset | Vị trí của message trong partition. Consumer đọc theo offset. |
| Producer | App/service publish message vào topic. |
| Consumer | App/service đọc message từ topic. |
| Consumer group | Nhóm consumer cùng xử lý một topic; mỗi partition thường được một consumer trong group xử lý tại một thời điểm. |
| Key | Key giúp Kafka chọn partition; dùng key ổn định như `tenantId:masterDataId` nếu cần order theo entity. |

Beginner mental model:

```text
topic master-data-events
├── partition 0: offset 0, 1, 2...
└── partition 1: offset 0, 1, 2...
```

Kafka có thể giữ order trong cùng partition, nhưng không nên giả định global order trên toàn topic.

---

## 4. Event vs command

### Event

Event nói rằng điều gì đã xảy ra.

```text
MasterDataChangedEvent
```

Ví dụ ý nghĩa: master data đã được tạo/cập nhật trong database.

### Command

Command yêu cầu hệ thống khác làm gì.

```text
SendNotificationCommand
```

Ví dụ ý nghĩa: hãy gửi email/thông báo.

Mini-lab này ưu tiên event vì dễ học và phù hợp với flow: backend đã xử lý DB xong rồi publish thông tin thay đổi.

---

## 5. At-least-once, duplicate và idempotency

Kafka consumer thường được thiết kế theo tư duy **at-least-once delivery**: một message có thể được xử lý ít nhất một lần, nhưng trong một số tình huống retry/rebalance, consumer có thể thấy lại message cũ.

Vì vậy consumer phải nghĩ tới duplicate:

- dùng `eventId` để phát hiện đã xử lý chưa;
- dùng operation idempotent;
- không cộng tiền/gửi email/lưu audit nhiều lần một cách mù quáng;
- log rõ retry/failure.

Trong Phase 1, chưa cần làm idempotency framework. Nhưng event shape nên có `eventId` và docs phải ghi caveat này.

---

## 6. Kafka trong kiến trúc ERP/accounting SaaS

Trong target architecture, Kafka nằm ở lớp internal communication/event integration:

```text
Backend services
-> Kafka
-> notification/search/reporting/integration consumers
```

Ví dụ hợp lý:

- `InvoiceCreatedEvent` -> notification service gửi thông báo.
- `FileUploadedEvent` -> virus scan/metadata processing.
- `MasterDataChangedEvent` -> update search projection hoặc audit log.
- `PaymentStatusChangedEvent` -> update reporting.

Không nên dùng Kafka chỉ để thay một API query đơn giản. Nếu caller cần dữ liệu ngay, HTTP/API hoặc database query vẫn phù hợp hơn.

---

## 7. Tenant safety trong event

Với shared-table multi-tenant, event phải mang tenant context:

```json
{
  "eventId": "...",
  "tenantId": 1,
  "aggregateType": "MASTER_DATA",
  "aggregateId": 101,
  "eventType": "MASTER_DATA_CHANGED"
}
```

Rule:

- Producer lấy `tenantId` từ trusted context/entity, không lấy từ request body tùy tiện.
- Consumer không xử lý event như global data nếu event thuộc tenant cụ thể.
- Nếu consumer ghi DB/projection, write cũng phải tenant-aware.
- Không đưa secret/token/raw binary payload lớn vào event.

---

## 8. Common mistakes

- Dùng Kafka như request/response synchronous API.
- Dùng Kafka thay PostgreSQL source of truth.
- Quên duplicate handling/idempotency.
- Tin rằng Kafka đảm bảo global ordering cho mọi event.
- Đưa password, access token, secret hoặc file binary lớn vào message.
- Quên tenant context trong payload/header.
- Trộn lẫn domain event, integration event và audit log mà không đặt tên rõ.
- Publish event trước khi transaction DB thật sự thành công mà không hiểu consistency caveat.

---

## 9. Áp dụng vào repo này

Mini-lab đề xuất:

```text
MasterData create/update
-> publish MasterDataChangedEvent
-> Kafka topic master-data-events
-> consumer log/projection nhỏ
```

Giữ scope:

- PostgreSQL vẫn là source of truth.
- Kafka disabled by default.
- Không Debezium ở bước này.
- Không full microservices.
- Không full accounting workflow.

---

## Nguồn tham khảo chuẩn

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring for Apache Kafka Reference](https://docs.spring.io/spring-kafka/reference/)
- [Confluent - Kafka Concepts](https://docs.confluent.io/kafka/introduction.html)
