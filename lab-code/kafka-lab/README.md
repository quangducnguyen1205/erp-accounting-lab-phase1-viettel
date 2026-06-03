# Kafka local mini-lab

Thư mục này cung cấp Kafka local cho Phase 1 async messaging mini-lab.

Mục tiêu hiện tại:

- hiểu topic/producer/consumer/consumer group ở mức thực hành;
- chuẩn bị môi trường để tự code `MasterDataChangedEvent`;
- không biến repo thành full event platform.

## Start/stop

Chạy từ `lab-code/`:

```bash
make kafka-up
make kafka-status
make kafka-down
```

Kafka local expose bootstrap server:

```text
localhost:19092
```

## Tạo/list topic bằng CLI

Sau khi container chạy:

```bash
docker exec -it viettel-kafka kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --if-not-exists \
  --topic master-data-events \
  --partitions 1 \
  --replication-factor 1
```

List topic:

```bash
docker exec -it viettel-kafka kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --list
```

## Quan sát message thủ công

Consumer CLI:

```bash
docker exec -it viettel-kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic master-data-events \
  --from-beginning
```

Producer CLI test nhanh:

```bash
docker exec -it viettel-kafka kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic master-data-events
```

Sau đó paste JSON thử, ví dụ:

```json
{"eventId":"local-test","eventType":"MASTER_DATA_CHANGED","tenantId":1,"aggregateId":101,"changeType":"UPDATED"}
```

Không dùng CLI test này làm bằng chứng code Spring Boot đã publish. Khi tự code producer, cần verify bằng app log hoặc consumer log sau khi gọi API create/update.

## Cấu hình app Spring Boot

`.env.example` đã có:

```text
APP_MESSAGING_ENABLED=false
KAFKA_BOOTSTRAP_SERVERS=localhost:19092
KAFKA_MASTER_DATA_TOPIC=master-data-events
KAFKA_CONSUMER_GROUP_ID=tenant-demo-master-data
```

Giữ `APP_MESSAGING_ENABLED=false` mặc định để `make app-test` không phụ thuộc Kafka.

## Cleanup

```bash
make kafka-down
```

Nếu cần reset lab data Kafka local, có thể dùng Docker Compose volume cleanup thủ công. Không làm reset destructive trong Makefile mặc định.
