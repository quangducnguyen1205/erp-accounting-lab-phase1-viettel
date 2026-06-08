# Kafka event shapes

## Vai trò tài liệu

Tài liệu này giải thích message/event shape cho Kafka mini-lab. Kafka không có "request/response body" giống HTTP API; producer gửi record vào topic, consumer đọc record đó theo topic/partition/offset.

Đọc trước:

- `kafka-async-messaging.md`

---

## 1. Kafka record shape ở mức beginner

Một Kafka record thường có:

| Thành phần | Vai trò |
|---|---|
| Topic | Nơi message được gửi vào, ví dụ `master-data-events`. |
| Key | Key để chọn partition và giữ order tương đối cho cùng entity. |
| Value | Payload chính, thường là JSON trong mini-lab này. |
| Headers | Metadata phụ, ví dụ tenant/correlation id nếu cần. |
| Partition | Partition Kafka ghi record vào. |
| Offset | Vị trí record trong partition. |
| Timestamp | Thời điểm Kafka nhận record hoặc producer set. |

Spring Kafka thường giúp mình làm việc với key/value/header thay vì tự xử lý partition/offset ngay từ đầu.

---

## 2. Event shape đề xuất cho repo này

Ví dụ `MasterDataChangedEvent`:

```json
{
  "eventId": "6a5b4a60-9e56-4bb1-b5d2-2c8d0a9b5a44",
  "eventType": "MASTER_DATA_CHANGED",
  "occurredAt": "2026-06-02T10:00:00Z",
  "tenantId": 1,
  "aggregateType": "MASTER_DATA",
  "aggregateId": 101,
  "code": "LAPTOP-01",
  "changeType": "UPDATED",
  "source": "tenant-demo"
}
```

Field gợi ý:

| Field | Vì sao cần |
|---|---|
| `eventId` | Dùng cho log/idempotency, giúp consumer phát hiện duplicate sau này. |
| `eventType` | Tên loại event rõ ràng. |
| `occurredAt` | Thời điểm event xảy ra ở producer. |
| `tenantId` | Tenant context bắt buộc cho shared-table SaaS. |
| `aggregateType` | Entity/domain object liên quan. |
| `aggregateId` | ID nghiệp vụ của object. |
| `code` | Field nhỏ giúp log/demo dễ đọc. |
| `changeType` | `CREATED`, `UPDATED`, `DELETED` nếu cần. |
| `source` | App/service phát event. |

Không cần nhét toàn bộ entity vào event. Event nên đủ cho consumer biết chuyện gì xảy ra và có thể query source of truth nếu cần thêm chi tiết.

---

## 3. Kafka key đề xuất

Key có thể là:

```text
tenant:{tenantId}:master-data:{aggregateId}
```

Ví dụ:

```text
tenant:1:master-data:101
```

Lợi ích:

- các event của cùng master data có xu hướng vào cùng partition;
- giữ order tương đối cho cùng entity;
- nhìn log/CLI dễ hiểu hơn.

Không dùng key chỉ có `aggregateId` nếu ID có thể trùng hoặc nếu muốn nhấn mạnh tenant boundary trong event stream.

---

## 4. Header nên dùng khi nào?

Payload phải đủ tự hiểu. Header có thể thêm metadata phụ:

```text
tenant-id: 1
correlation-id: ...
event-type: MASTER_DATA_CHANGED
```

Trong mini-lab, có thể để `tenantId` trong payload là đủ. Nếu thêm header, không được để payload thiếu tenant vì consumer có thể chỉ log/lưu payload.

---

## 5. Command vs event shape

### Event

```json
{
  "eventType": "MASTER_DATA_CHANGED",
  "changeType": "UPDATED"
}
```

Ý nghĩa: việc đã xảy ra.

### Command

```json
{
  "commandType": "SEND_NOTIFICATION",
  "targetUserId": "u-123"
}
```

Ý nghĩa: yêu cầu một handler làm gì đó.

Mini-lab hiện dùng event, không dùng command.

---

## 6. Error/retry/idempotency shape

Chưa cần dead-letter topic đầy đủ trong Phase 1, nhưng event nên có field để debug:

- `eventId`;
- `occurredAt`;
- `source`;
- `tenantId`;
- `aggregateId`.

Nếu sau này có DLQ/dead-letter topic, message lỗi nên giữ lại:

- original topic/key/value;
- error message;
- retry count;
- failedAt;
- consumer group.

---

## 7. Common mistakes khi thiết kế event

- Payload quá to, nhét cả file/binary/object graph.
- Thiếu `tenantId`.
- Thiếu `eventId`, khó xử lý duplicate.
- Tên event mơ hồ như `DataEvent`.
- Dùng event để thay query DB.
- Dùng event chứa secret/token/password.
- Consumer tin event mà không verify tenant/authorization boundary khi ghi dữ liệu.

---

## 8. Áp dụng vào mini-lab

Topic dự kiến:

```text
master-data-events
```

Event dự kiến:

```text
MasterDataChangedEvent
```

Producer dự kiến:

```text
MasterDataService create/update
-> MasterDataEventPublisher
-> Kafka topic
```

Consumer dự kiến:

```text
Kafka listener
-> log event hoặc lưu projection/audit-style record nhỏ
```

Rule vẫn giữ: PostgreSQL là source of truth; event chỉ báo rằng dữ liệu đã thay đổi.
