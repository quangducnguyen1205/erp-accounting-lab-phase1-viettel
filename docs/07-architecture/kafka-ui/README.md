# Kafka UI

## Folder này chứa gì?

Nhóm này chuẩn bị mini-lab Kafka UI cho Phase 1.5. Mục tiêu là nhìn được topic/message/consumer group thay vì chỉ đọc log producer/consumer.

## Reading Order

1. [kafka-ui-foundation.md](kafka-ui-foundation.md) - Kafka UI giúp xem gì, dùng khi nào, kết nối với current Kafka mini-lab ra sao.

## Trạng thái

- Planning/foundation doc đã có.
- Runtime Docker lab chưa implement trong commit này.
- Hướng tiếp theo: thêm `lab-code/kafka-ui-lab/` hoặc tích hợp Kafka UI cạnh `lab-code/kafka-lab/`.

## Caveat

Kafka UI là công cụ inspect/debug local. Nó không thay thế việc hiểu producer/consumer code, idempotency, retry, DLT hoặc schema/versioning.
