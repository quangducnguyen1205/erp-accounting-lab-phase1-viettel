# Phase 1 service split options

## 1. Vì sao không split bừa?

Tách service không chỉ là copy controller/service sang module mới. Một service thật cần có:

- responsibility rõ;
- API boundary hoặc event boundary;
- data ownership;
- config/deploy/log riêng;
- test/verification riêng;
- bảo mật/token/tenant context không bị hở.

Nếu split khi chưa rõ boundary, repo sẽ phức tạp hơn nhưng không học được nhiều.

## 2. Current monolith-ish state

`tenant-demo` hiện gom nhiều mini-lab:

- master data CRUD;
- tenant-aware repository/service;
- Redis cache-aside;
- Kafka producer/consumer;
- MinIO file storage;
- Elasticsearch search;
- Actuator/logging/metrics.

Điều này tốt cho Phase 1 foundation, nhưng chưa đủ để cảm nhận:

- service-to-service boundary;
- cross-service Kafka event;
- centralized logging;
- Gateway nhiều upstream thật.

## 3. Option A - master-data-service + audit-log-service

Ý tưởng:

```text
master-data-service
-> save master_data
-> publish MasterDataChangedEvent

audit-log-service
-> consume MasterDataChangedEvent
-> write audit record hoặc log/projection nhỏ
```

Ưu điểm:

- tận dụng event đã có;
- domain nhỏ, dễ giải thích;
- Kafka trở thành cross-service thật;
- Loki có nhiều service log để search;
- Kong có thêm route `/api/audit-events/**` nếu expose query audit.

Nhược điểm:

- audit domain đơn giản, chưa phải nghiệp vụ kế toán sâu.

Kết luận: chọn trước.

## 4. Option B - file-service

Ý tưởng: tách MinIO upload/download sang `file-service`.

Ưu điểm:

- file ownership rõ;
- MinIO logic nằm đúng service;
- hợp target architecture file/document service.

Nhược điểm:

- cần nhiều endpoint/auth/RBAC/multipart test hơn;
- dễ kéo UI/file upload vào scope;
- không giải quyết Kafka cross-service ngay.

Kết luận: backlog nếu còn thời gian.

## 5. Option C - search-service

Ý tưởng: search service consume `MasterDataChangedEvent` và update Elasticsearch projection.

Ưu điểm:

- rất realistic;
- nối Kafka -> Elasticsearch rõ.

Nhược điểm:

- eventual consistency/reindex/error recovery phức tạp;
- dễ mất nhiều thời gian vào Elasticsearch thay vì service boundary.

Kết luận: later/backlog.

## 6. Option D - notification/reporting service

Ưu điểm:

- consumer đơn giản.

Nhược điểm:

- nếu chưa có use case cụ thể, dễ artificial.

Kết luận: không ưu tiên.

## 7. Recommended split

Chọn `audit-log-service` trước.

Minimal scope:

- Spring Boot service riêng, port khác.
- Kafka consumer cho `MasterDataChangedEvent`.
- Có thể lưu audit vào PostgreSQL table riêng hoặc log rõ trước.
- Nếu có DB, dùng schema/table riêng để thể hiện data ownership.
- Endpoint read audit chỉ thêm nếu cần demo qua Kong/React thật.

Không làm ngay:

- outbox pattern;
- exactly-once;
- full audit compliance;
- service discovery thật;
- Kubernetes;
- full UI audit screen trước khi backend ổn.

## 8. Target flow sau split

```text
React Web
-> Kong Gateway
-> master-data-service
-> PostgreSQL
-> Kafka topic master-data-events
-> audit-log-service
-> audit table/log

Grafana Explore
-> Loki logs from Kong + master-data-service + audit-log-service

Kafka UI
-> topic/message/consumer group/lag
```

## 9. Done criteria cho split đầu tiên

- `audit-log-service` chạy riêng.
- `master-data-service` publish event sau create/update.
- Kafka UI thấy event và consumer group.
- `audit-log-service` consume event, log/store audit record.
- Loki/Grafana tìm được log theo service và requestId/eventId nếu có.
- Backend security/tenant-aware flow không bị yếu đi.
