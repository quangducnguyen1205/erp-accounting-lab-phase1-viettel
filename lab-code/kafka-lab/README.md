# Kafka local mini-lab

Thư mục này cung cấp Kafka local cho Phase 1 async messaging mini-lab.

Mục tiêu hiện tại:

- hiểu topic/producer/consumer/consumer group ở mức thực hành;
- chạy reference implementation nhỏ quanh `MasterDataChangedEvent`;
- không biến repo thành full event platform.

## Start/stop

Chạy từ `lab-code/`:

```bash
make -f Makefile.legacy kafka-up
make kafka-status
make -f Makefile.legacy kafka-down
```

Kafka local expose bootstrap server:

```text
localhost:19092
```

Kafka UI local dùng Docker network chung để kết nối broker bằng địa chỉ nội bộ:

```text
kafka:9092
```

Chạy UI từ `lab-code/`:

```bash
make -f Makefile.legacy kafka-ui-up
```

Mở `http://localhost:18082` để inspect topic/message/consumer group.

## Giải thích `docker-compose.yml`

File `docker-compose.yml` trong thư mục này chỉ dùng để dựng **Kafka local 1 node** cho mini-lab. Ý chính của file:

### 1. Chạy Kafka ở chế độ KRaft

Kafka trong lab này chạy theo kiểu **controller + broker chung một container**, nên **không cần ZooKeeper**.

- `KAFKA_NODE_ID=1`: định danh node Kafka.
- `KAFKA_PROCESS_ROLES=controller,broker`: một node vừa làm controller vừa làm broker.
- `KAFKA_CONTROLLER_QUORUM_VOTERS=1@kafka:9093`: khai báo controller voter của cluster KRaft.

### 2. Các listener của Kafka

Kafka mở 3 listener:

- `PLAINTEXT://:9092`
  - listener nội bộ cho container khác trong Docker network.
- `CONTROLLER://:9093`
  - kênh riêng cho controller của KRaft.
- `EXTERNAL://:19092`
  - listener dành cho app chạy trên máy host, ví dụ Spring Boot chạy ở máy bạn.

### 3. Vì sao có `advertised.listeners`

Kafka không chỉ cần “nghe” ở một port, mà còn phải **nói cho client biết địa chỉ nào để quay lại kết nối**.

- `KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092,EXTERNAL://localhost:19092`

Ý nghĩa:

- client ở trong Docker dùng `kafka:9092`
- client chạy trên máy host dùng `localhost:19092`

Nếu cấu hình phần này sai, client thường kết nối được lần đầu nhưng sẽ lỗi khi Kafka trả về metadata và bảo client reconnect sang địa chỉ khác.

### 4. Vì sao để `PLAINTEXT`

Toàn bộ lab này là môi trường local nên dùng `PLAINTEXT` cho đơn giản. Nếu lên môi trường thật, thường sẽ chuyển sang TLS/SASL và cấu hình auth rõ hơn.

### 5. Các setting dev-only

- `KAFKA_AUTO_CREATE_TOPICS_ENABLE=true`
  - topic có thể tự tạo khi producer hoặc CLI ghi lần đầu.
  - tiện cho học/lab, nhưng production thường không để vậy.

- `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1`
- `KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1`
- `KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1`
  - các giá trị này đều bằng 1 vì đây là **single-node lab**.
  - nếu để >1 thì Kafka sẽ đòi nhiều broker/controller hơn và không chạy được trong lab này.

- `KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0`
  - giảm độ trễ rebalance của consumer group để test nhanh hơn.

### 6. Healthcheck làm gì

Healthcheck trong compose dùng `kafka-topics.sh --list` để kiểm tra broker đã sẵn sàng chưa.

Nói ngắn gọn: nếu healthcheck pass, Kafka đã đủ ổn để app/CLI kết nối.

### 7. Khi đọc log và code, cần nhớ gì

Trong project này:

- Spring Boot app chạy trên host sẽ connect qua `localhost:19092`.
- Docker internal tools/container khác sẽ dùng `kafka:9092`.
- Kafka UI join network `viettel-kafka-net` nên cũng dùng `kafka:9092`.
- File compose chỉ lo **hạ tầng Kafka local**; phần logic publish/consume nằm ở code Spring Boot.

## Tạo/list topic bằng CLI

Sau khi container chạy:

```bash
docker exec -it viettel-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --if-not-exists \
  --topic master-data-events \
  --partitions 1 \
  --replication-factor 1
```

List topic:

```bash
docker exec -it viettel-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --list
```

## Quan sát message thủ công

Consumer CLI:

```bash
docker exec -it viettel-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic master-data-events \
  --from-beginning
```

Producer CLI test nhanh:

```bash
docker exec -it viettel-kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic master-data-events
```

Sau đó paste JSON thử, ví dụ:

```json
{"eventId":"local-test","eventType":"MASTER_DATA_CHANGED","tenantId":1,"aggregateId":101,"changeType":"UPDATED"}
```

Không dùng CLI test này làm bằng chứng code Spring Boot đã publish. Reference implementation hiện tại cần verify bằng app log sau khi gọi API create/update.

## Cấu hình app Spring Boot

`.env.example` đã có:

```text
APP_MESSAGING_ENABLED=false
KAFKA_BOOTSTRAP_SERVERS=localhost:19092
KAFKA_MASTER_DATA_TOPIC=master-data-events
```

Giữ `APP_MESSAGING_ENABLED=false` mặc định để `make app-test` không phụ thuộc Kafka.

Trong final demo, `tenant-demo` chỉ publish event. Consumer thật là:

```text
audit-log-service
search-service
```

Không còn kỳ vọng consumer group `tenant-demo-master-data` trong Kafka UI.

Khi chỉ muốn chạy Kafka mini-lab, nên tắt các lab khác để app không phụ thuộc Elasticsearch/MinIO/Redis:

```bash
cd lab-code/tenant-demo
set -a; . ./.env; set +a
APP_MESSAGING_ENABLED=true \
APP_CACHE_ENABLED=false \
./mvnw spring-boot:run
```

## Cleanup

```bash
make -f Makefile.legacy kafka-down
```

Nếu cần reset lab data Kafka local, có thể dùng Docker Compose volume cleanup thủ công. Không làm reset destructive trong Makefile mặc định.
