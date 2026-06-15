# Q&A vận hành cho final demo

Tài liệu này chuẩn bị câu trả lời cho các câu hỏi thực tế khi reviewer nhìn log, latency, Kafka lag, search delay hoặc upload/download.

## API duration / latency

**Hỏi:** Duration trong log như vậy là nhanh hay chậm? Chạy local như vậy có ổn không?

**Trả lời ngắn:**

`durationMs` trong log là quan sát local runtime, không phải benchmark chính thức. Local Docker + Maven + laptop resource contention có thể làm latency dao động.

Trong demo, mình chỉ dùng duration để kiểm:

- không có timeout lặp lại;
- không có `5xx` lặp lại;
- API phản hồi trong mức người dùng thao tác được;
- phân biệt cold start, first request, cache miss và cache hit.

Muốn kết luận performance phải có load test với:

- môi trường cố định;
- dataset size rõ;
- số concurrent users rõ;
- throughput;
- error rate;
- percentile p50/p95/p99;
- JVM/DB/Kafka/Elasticsearch metrics đi kèm.

Không nên overclaim từ một con số duration local.

## Concurrent users

**Hỏi:** Nếu nhiều người dùng cùng lúc thì sao?

**Trả lời ngắn:**

Thiết kế hiện tại có boundary đúng hướng:

- backend Resource Server stateless;
- PostgreSQL query có tenant-aware index;
- Redis cache hỗ trợ read path;
- Kafka tách async flow;
- MinIO lưu object;
- Kong làm entry point.

Nhưng local lab chưa chứng minh production concurrency.

Bottleneck cần theo dõi khi tăng tải:

- database connection pool;
- PostgreSQL query plan và index;
- Kafka consumer lag;
- Elasticsearch indexing/query latency;
- file size/network/MinIO throughput;
- JVM heap/GC;
- Kong upstream timeout/connection limit.

Để production hóa cần scale backend instances, tune pool/index, tăng partition/consumer nếu phù hợp, thêm retry/DLT/backpressure, và đo p95/p99.

## Kafka lag

**Hỏi:** Kafka lag bằng 0 có nghĩa là gì?

**Trả lời:**

Lag `0` nghĩa là consumer group đã đọc đến offset mới nhất tại thời điểm kiểm tra. Trong demo, đó là dấu hiệu `audit-log-service` và `search-service` đang bắt kịp topic `master-data-events`.

Nếu lag tăng:

- consumer có thể đang xử lý chậm;
- consumer có thể lỗi hoặc restart;
- event rate có thể cao hơn khả năng xử lý;
- partition/consumer count có thể chưa đủ.

Lag phải đọc cùng event rate, số partition, consumer count và processing time. Lag non-zero ngắn ngay sau khi tạo event chưa chắc là lỗi.

## Search eventual consistency

**Hỏi:** Tại sao vừa tạo record nhưng search chưa thấy ngay?

**Trả lời:**

PostgreSQL write là source-of-truth path. Elasticsearch là projection được cập nhật qua Kafka event, nên có thể trễ vài giây.

Luồng đúng là:

```text
tenant-demo save PostgreSQL
-> publish MasterDataChangedEvent
-> search-service consume
-> update Elasticsearch index
-> UI search thấy record
```

Nếu projection lệch, admin có thể gọi tenant-scoped reindex để rebuild từ `tenant-demo`/PostgreSQL source data.

Production vẫn cần outbox, retry/DLT, schema registry và backfill workflow tốt hơn.

## Redis cache

**Hỏi:** Redis ở đây có thay database không?

**Trả lời:**

Không. Redis chỉ là cache-aside cho read path.

- Cache hit giúp đọc nhanh hơn.
- Cache miss fallback về PostgreSQL.
- PostgreSQL vẫn là source of truth.
- Cache key phải tenant-scoped để tránh cross-tenant leak.
- TTL và invalidation phải thiết kế cẩn thận khi update/delete.

Trong demo, UI không tự kết luận cache hit/miss; mình đọc log/metrics để chứng minh.

## File upload/download

**Hỏi:** Upload/download chậm thì bottleneck ở đâu?

**Trả lời:**

Upload/download phụ thuộc:

- file size;
- network local;
- backend streaming;
- MinIO throughput;
- PostgreSQL metadata lookup;
- JVM memory/streaming config;
- Kong timeout nếu đi qua gateway.

Model hiện tại:

```text
UI -> Kong -> file-service -> PostgreSQL metadata check -> MinIO object
```

Backend kiểm tenant ownership trước khi stream object. UI không gọi MinIO trực tiếp.

Production cần thêm size limit, lifecycle, retention/versioning, antivirus scan nếu domain yêu cầu, quota và monitoring theo object size.

## Database source of truth

**Hỏi:** Tại sao cứ nhấn mạnh PostgreSQL là source of truth?

**Trả lời:**

Vì dữ liệu nghiệp vụ phải có một nơi quyết định trạng thái thật. Trong repo này:

- master data nằm ở PostgreSQL;
- file metadata/ownership nằm ở PostgreSQL;
- audit table là read model riêng của audit service;
- Elasticsearch là projection;
- Redis là cache;
- Kafka là event transport.

Tenant-aware query và index là bắt buộc. Nếu query quên `tenant_id`, hệ thống có nguy cơ leak dữ liệu.

## 401 vs 403

**Hỏi:** Khác nhau giữa `401` và `403` là gì?

**Trả lời:**

- `401`: request chưa authenticated hoặc token không hợp lệ.
- `403`: token hợp lệ, user đã authenticated, nhưng role không đủ quyền cho action.

Ví dụ:

- Không gửi token vào `/api/master-data` -> `401`.
- `tenant2-user` role `VIEWER` thử create master data -> `403`.

Tenant isolation vẫn là lớp riêng: dù có role đọc, user tenant 2 vẫn không được thấy dữ liệu tenant 1.

## Kong và security

**Hỏi:** Kong có validate token không?

**Trả lời:**

Trong local lab này, Kong chủ yếu làm gateway route và giữ đường vào API thống nhất. Backend services vẫn validate JWT bằng Spring Security Resource Server.

Đây là lựa chọn học tập tốt vì người học thấy rõ mỗi service vẫn không được tin request chỉ vì đã đi qua gateway.

Production có thể dùng thêm Kong plugin cho auth/rate limit/mTLS, nhưng không nên bỏ authorization ở backend.

## Multiple services và failure

**Hỏi:** Nếu `audit-log-service` chết thì create master data có fail không?

**Trả lời:**

Trong async model, create master data đi theo synchronous path tới `tenant-demo` và PostgreSQL.
Audit/search là async consumers.
Nếu consumer chết, Kafka giữ event theo retention và consumer group offset;
khi service chạy lại có thể bắt kịp nếu event còn trong topic.

Nhưng vì chưa có outbox/retry/DLT production-grade, vẫn có gap nếu DB write thành công nhưng publish Kafka fail, hoặc consumer fail liên tục.

## Production-like vs production-ready

**Hỏi:** Local demo này chứng minh được production chưa?

**Trả lời:**

Chưa. Nó chứng minh kiến trúc và flow đúng hướng ở mức production-like:

- auth boundary;
- gateway boundary;
- service boundary;
- tenant isolation;
- event-driven flow;
- cache/projection/object storage;
- observability.

Để production-ready cần thêm:

- HA;
- TLS/secrets;
- CI/CD deployment;
- real load test;
- alerting/SLO;
- distributed tracing;
- outbox/retry/DLT/schema registry;
- backup/restore;
- file lifecycle/versioning/quota;
- security hardening.

## Khi demo bị chậm

Nếu UI hoặc API hơi chậm, nói:

> Đây là local demo chạy nhiều container và Maven process trên cùng laptop. Em không dùng số latency này để claim performance. Em dùng nó để chứng minh request không timeout, status đúng, và log/metric đủ để debug.

Nếu search chậm vài giây:

> Đây là eventual consistency của projection. Source of truth là PostgreSQL; search-service cần consume event rồi update Elasticsearch.

Nếu Kafka lag non-zero ngắn:

> Lag non-zero ngay sau khi publish event có thể là bình thường. Mình xem nó có về 0 không và consumer có lỗi không.
