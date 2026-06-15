# Kafka UI Local Lab Config Walkthrough

Doc này giải thích Kafka UI lab trong repo: vì sao cần, config kết nối Kafka như thế nào, và đọc UI ra sao.

## Big Picture

```text
Kafka broker container
  -> exposes internal listener kafka:9092 on Docker network
  -> exposes external listener localhost:19092 for host apps

Kafka UI container
  -> joins the same Docker network
  -> connects to kafka:9092

Browser
  -> http://localhost:18082
  -> Kafka UI
```

Kafka UI không thay thế producer/consumer code. Nó chỉ giúp nhìn broker/topic/message/consumer group để việc học Kafka bớt "chỉ đọc log".

## File Map

| File | Vai trò | Khi nào sửa? | Không nên đặt gì vào đây? |
|---|---|---|---|
| `lab-code/kafka-lab/docker-compose.yml` | Chạy Kafka broker và tạo shared Docker network | Đổi Kafka version/listener/network | Secret production |
| `lab-code/kafka-ui-lab/docker-compose.yml` | Chạy Kafka UI local | Đổi UI port/bootstrap server | Credential thật |
| `lab-code/kafka-ui-lab/README.md` | Lệnh chạy nhanh và cách inspect | Khi workflow đổi | Message payload nhạy cảm |
| `lab-code/Makefile` | Target `kafka-ui-*` Docker-first | Khi thêm/sửa target | Lệnh phụ thuộc local npm/tool khác |

## Kafka Docker Network

`lab-code/kafka-lab/docker-compose.yml` khai báo network:

```yaml
networks:
  kafka-net:
    name: viettel-kafka-net
```

Kafka UI chạy ở compose project khác, nên nó cần join cùng external network này:

```yaml
networks:
  kafka-net:
    external: true
    name: viettel-kafka-net
```

Vì vậy target `make -f Makefile.legacy kafka-ui-up` chạy `kafka-up` trước để broker và network tồn tại.

## Bootstrap Server: Host vs Container

Kafka local có hai kiểu địa chỉ:

| Người gọi | Địa chỉ đúng | Vì sao |
|---|---|---|
| Spring Boot app chạy trên host | `localhost:19092` | Host gọi port publish ra ngoài Docker |
| Kafka UI container | `kafka:9092` | Container gọi service name trong Docker network |

Sai lầm phổ biến là cấu hình Kafka UI container dùng `localhost:19092`. Bên trong container, `localhost` là chính container Kafka UI, không phải máy host và không phải Kafka broker.

## `kafka-ui-lab/docker-compose.yml`

Service chính:

```yaml
kafka-ui:
  image: provectuslabs/kafka-ui:v0.7.2
  container_name: viettel-kafka-ui
  ports:
    - "18082:8080"
  environment:
    KAFKA_CLUSTERS_0_NAME: viettel-local
    KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
```

Ý nghĩa:

- `image`: Kafka UI web app local.
- `ports 18082:8080`: browser mở `http://localhost:18082`, container app nghe port `8080`.
- `KAFKA_CLUSTERS_0_NAME`: tên cluster hiển thị trong UI.
- `KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS`: địa chỉ broker từ góc nhìn container Kafka UI.
- `READONLY=false`: local lab cho phép inspect nhiều thao tác. Production/dev shared environment nên cân nhắc read-only và auth.

## Makefile Targets

Từ `lab-code/`:

```bash
make -f Makefile.legacy kafka-ui-up       # start Kafka broker nếu cần, rồi start Kafka UI
make -f Makefile.legacy kafka-ui-status   # xem container Kafka UI
make kafka-ui-logs     # xem log Kafka UI
make -f Makefile.legacy kafka-ui-down     # dừng Kafka UI, không dừng Kafka broker
make -f Makefile.legacy kafka-ui-info     # in URL và ghi chú nhanh
```

Kafka broker vẫn có target riêng:

```bash
make -f Makefile.legacy kafka-up
make -f Makefile.legacy kafka-status
make -f Makefile.legacy kafka-down
```

## Inspect gì trong Kafka UI?

Khi backend publish `MasterDataChangedEvent`, mở:

```text
http://localhost:18082
```

Các thứ nên quan sát:

- **Cluster**: Kafka broker có kết nối được không.
- **Topics**: topic `master-data-events` hoặc topic được cấu hình trong app.
- **Partitions**: message nằm ở partition nào.
- **Offset**: thứ tự message trong partition.
- **Key**: key tenant-aware, giúp routing/order theo aggregate/tenant tốt hơn.
- **Value**: JSON event, ví dụ `eventId`, `tenantId`, `aggregateId`, `changeType`.
- **Consumer groups**: group id của consumer, offset đã consume, lag.
- **Lag**: số message consumer group chưa xử lý.

## Query Flow Với Repo Hiện Tại

```text
POST/PUT master_data
  -> tenant-demo saves PostgreSQL first
  -> tenant-demo publishes MasterDataChangedEvent
  -> Kafka broker stores message in topic
  -> tenant-demo consumer currently consumes/logs it
  -> Kafka UI shows topic/message/group state
```

Sau này khi có `audit-log-service`, Kafka UI sẽ càng hữu ích vì có thể thấy consumer group riêng của service mới.

## Common Mistakes

- Dùng `localhost:19092` trong container Kafka UI.
- Chưa chạy `make -f Makefile.legacy kafka-up` nên external network `viettel-kafka-net` chưa tồn tại.
- Topic chưa có message vì chưa trigger create/update `master_data`.
- Consumer đã đọc hết message nên lag bằng 0, không có nghĩa consumer không chạy.
- Message cũ biến mất do retention.
- Nhầm Kafka UI với Kafka broker. UI chỉ là công cụ xem/quản trị local.
- Đưa dữ liệu nhạy cảm vào event rồi quên rằng Kafka UI có thể xem payload.

## Giới hạn production

- Kafka UI chỉ là local/dev inspection tool trong repo này.
- Không expose Kafka UI public nếu chưa có auth/network control.
- Message payload có thể chứa dữ liệu nhạy cảm, cần quy tắc schema và masking.
- Production Kafka cần ACL, TLS/SASL, retention policy, schema compatibility, retry/DLT strategy.
- UI không thay thế monitoring/alerting Kafka thật.
