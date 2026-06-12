# Kafka UI

## Folder này chứa gì?

Nhóm này chứa note và walkthrough cho Kafka UI mini-lab Phase 1.5. Mục tiêu là nhìn được broker/topic/message/consumer group/lag thay vì chỉ đọc log producer/consumer.

## Reading Order

1. [kafka-ui-foundation.md](kafka-ui-foundation.md) - Kafka UI giúp xem gì, dùng khi nào, kết nối với current Kafka mini-lab ra sao.
2. [kafka-ui-local-lab-config-walkthrough.md](kafka-ui-local-lab-config-walkthrough.md) - giải thích Docker Compose, Docker network, bootstrap server và cách inspect message trong lab.
3. [../../../lab-code/kafka-ui-lab/README.md](../../../lab-code/kafka-ui-lab/README.md) - lệnh chạy local.

## Trạng thái

- Foundation doc đã có.
- Runtime Docker lab đã có ở `lab-code/kafka-ui-lab/`.
- Makefile targets `kafka-ui-up/status/logs/down/info` đã có.
- Kafka UI kết nối Kafka broker qua Docker network `viettel-kafka-net`.

## Caveat

Kafka UI là công cụ inspect/debug local. Nó không thay thế việc hiểu producer/consumer code, idempotency, retry, DLT hoặc schema/versioning. Không expose Kafka UI public nếu chưa có auth/network control.
