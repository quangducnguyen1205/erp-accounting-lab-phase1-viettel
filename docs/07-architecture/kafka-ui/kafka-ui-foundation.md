# Kafka UI foundation

## 1. Vì sao cần Kafka UI?

Kafka mini-lab hiện đã publish và consume được `MasterDataChangedEvent`, nhưng bằng chứng chính là log:

```text
Published Kafka event
Consumed Kafka event
```

Kafka UI giúp nhìn trực tiếp:

- broker đang chạy;
- topic có tồn tại không;
- partition và offset;
- message key/value;
- consumer group;
- lag của consumer.

Khi chuyển sang cross-service flow, Kafka UI giúp xác nhận event thật sự nằm trong Kafka trước khi debug consumer service.

## 2. Những thứ cần nhìn trong Kafka UI

| Khái niệm | Cần quan sát gì? | Liên hệ repo |
|---|---|---|
| Broker | Kafka node có up không | Local Kafka container. |
| Topic | Topic event tồn tại không | `master-data-events`. |
| Partition | Message nằm partition nào | Key tenant-aware ảnh hưởng partitioning. |
| Offset | Message thứ mấy trong partition | Chứng minh event đã append. |
| Message key | Key dùng để partition/order trong phạm vi key | `tenant:{tenantId}:master-data:{id}`. |
| Message value | JSON event payload | `MasterDataChangedEvent`. |
| Consumer group | Consumer nào đang đọc topic | Current app hoặc future `audit-log-service`. |
| Lag | Consumer còn chậm bao nhiêu message | Khi consumer down hoặc lỗi. |

## 3. Kafka UI không thay thế gì?

Kafka UI không thay:

- producer/consumer code;
- idempotency;
- retry/DLT strategy;
- schema evolution;
- alerting/monitoring production;
- authorization/security config.

Nó là debugging/learning tool để thấy Kafka không phải "hộp đen".

## 4. Áp dụng vào current mini-lab

Hiện tại:

```text
tenant-demo
-> KafkaTemplate publish MasterDataChangedEvent
-> Kafka topic master-data-events
-> @KafkaListener trong cùng app consume
```

Phase 1.5:

```text
master-data-service
-> Kafka topic master-data-events
-> audit-log-service consume
```

Kafka UI cần chứng minh:

- event có `tenantId`;
- event key có tenant scope;
- topic có message sau create/update;
- consumer group của audit-log-service đọc được message;
- lag bằng 0 khi consumer xử lý kịp.

## 5. Common mistakes

- Chỉ nhìn UI thấy message rồi nghĩ business đã đúng. Consumer vẫn có thể parse sai hoặc xử lý duplicate.
- Dựa vào global ordering. Kafka chỉ đảm bảo order trong từng partition.
- Không để ý consumer group nên tưởng consumer không chạy.
- Publish event thiếu tenant context.
- Đưa token/secret/file binary lớn vào event payload.

## 6. Local lab direction

Mini-lab nên Docker-first:

- Kafka vẫn chạy từ `lab-code/kafka-lab/`.
- Kafka UI container kết nối tới broker local.
- Makefile targets: `kafka-ui-up`, `kafka-ui-status`, `kafka-ui-down`.
- Không bật Kafka UI mặc định trong `infra-up` nếu muốn giữ máy nhẹ; có thể document là optional debug tool.
