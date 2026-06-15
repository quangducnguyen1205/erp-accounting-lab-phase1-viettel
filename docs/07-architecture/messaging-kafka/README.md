# Kafka / Async Messaging

## Thư mục này chứa gì?

Nhóm này giải thích async messaging, Kafka concepts và Spring Kafka mini-lab quanh `MasterDataChangedEvent`.

## Thứ tự đọc đề xuất

1. [kafka-async-messaging.md](kafka-async-messaging.md) - foundation: topic, partition, offset, producer, consumer, consumer group.
2. [kafka-configuration-deep-dive.md](kafka-configuration-deep-dive.md) - Docker Compose, listeners, advertised listeners, ports.
3. [java-async-future-completablefuture.md](java-async-future-completablefuture.md) - Java async/Future/CompletableFuture trong `KafkaTemplate.send(...)`.
4. [kafka-listener-consumer-flow.md](kafka-listener-consumer-flow.md) - `@KafkaListener`, poll, deserialization, offset.
5. [kafka-event-shapes.md](kafka-event-shapes.md) - event DTO shape, tenant context, idempotency fields.
6. [kafka-code-guide-spring-boot.md](kafka-code-guide-spring-boot.md) - Spring Kafka producer/consumer class shape.
7. [cross-service-kafka-flow.md](cross-service-kafka-flow.md) - Phase 1.5 flow từ `tenant-demo` sang `audit-log-service`.
8. [kafka-mini-lab-plan.md](kafka-mini-lab-plan.md) - checklist mini-lab.

## Trạng Thái

- Mini-lab đã đóng ở Phase 1 level.
- Producer publish event sau create/update `master_data`.
- Consumer nhận event bằng `@KafkaListener`.
- Phase 1.5 đã thêm `audit-log-service` để Kafka trở thành cross-service flow.

## Giới hạn hiện tại

Chưa có outbox, retry/DLT, idempotency storage hoặc schema evolution production. PostgreSQL vẫn là source of truth.
