# Kafka UI lab

Mini-lab này chạy Kafka UI local để inspect broker/topic/message/consumer group của Kafka mini-lab.

Kafka UI là tool xem trạng thái Kafka, không thay thế producer/consumer code trong Spring Boot.

## Chạy nhanh

Từ `lab-code/`:

```bash
make kafka-ui-up
make kafka-ui-status
```

Mở:

```text
http://localhost:18082
```

Dừng Kafka UI:

```bash
make kafka-ui-down
```

`make kafka-ui-up` sẽ chạy `make kafka-up` trước để bảo đảm Kafka broker và Docker network `viettel-kafka-net` đã tồn tại.

## Kafka UI kết nối Kafka như thế nào?

Kafka broker chạy ở `lab-code/kafka-lab/`. Kafka UI chạy ở compose project riêng nhưng join chung Docker network `viettel-kafka-net`.

Trong container Kafka UI, bootstrap server đúng là:

```text
kafka:9092
```

Không dùng `localhost:19092` trong container Kafka UI, vì `localhost` lúc đó là chính container UI.

## Inspect gì?

- Cluster/broker có online không.
- Topic chứa `MasterDataChangedEvent`.
- Partition và offset.
- Message key/value.
- Consumer group và lag.

Với flow hiện tại:

```text
create/update master_data
  -> tenant-demo publish MasterDataChangedEvent
  -> Kafka topic
  -> Kafka UI inspect message
```

Sau này khi có `audit-log-service`, Kafka UI sẽ dùng để xem consumer group riêng của service đó.

## Docs nên đọc

- `docs/07-architecture/kafka-ui/kafka-ui-foundation.md`
- `docs/07-architecture/kafka-ui/kafka-ui-local-lab-config-walkthrough.md`

## Caveats

- Kafka UI là local/dev tool.
- Không expose public nếu chưa có auth/network control.
- Message payload có thể chứa dữ liệu nhạy cảm, nên không đưa secret/token vào Kafka event.
- Topic có thể không có message nếu chưa trigger API create/update.
