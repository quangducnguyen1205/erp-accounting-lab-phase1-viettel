# Kafka UI lab

Trạng thái: planning stub cho Phase 1.5.

Mini-lab này sẽ thêm Kafka UI để inspect:

- broker;
- topic;
- partition;
- offset;
- message key/value;
- consumer group;
- consumer lag.

## Mục tiêu khi implement

- Docker-first.
- Kết nối tới Kafka local từ `lab-code/kafka-lab/`.
- Không thay thế Spring Kafka code.
- Dùng để verify `MasterDataChangedEvent` và future `audit-log-service` consumer group.

## Chưa làm trong stub này

- Chưa có `docker-compose.yml`.
- Chưa có Makefile targets.
- Chưa bật vào `infra-up`.

Doc nền: `docs/07-architecture/kafka-ui/`.
