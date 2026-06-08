# Redis cache strategy foundation

## Vai trò tài liệu

Tài liệu này giải thích kiến thức nền về Redis/cache cho backend. Mục tiêu là hiểu cache như một lớp tối ưu đọc, không biến Redis thành database chính và không làm lộ dữ liệu giữa tenant.

Đọc tiếp theo:

- `redis-code-guide-spring-boot.md` - cách tích hợp Redis vào Spring Boot.
- `redis-mini-lab-plan.md` - checklist mini-lab cache nhỏ trong repo.
- `lab-code/redis-lab/README.md` - lệnh chạy Redis local.

---

## 1. Redis là gì?

Redis là in-memory data store: dữ liệu chủ yếu nằm trong RAM để đọc/ghi nhanh. Redis thường được dùng cho:

- cache;
- session/token state nếu cần;
- rate limit;
- queue/stream nhỏ;
- distributed lock ở một số use case;
- pub/sub hoặc realtime signal nhẹ.

Trong mini-lab này chỉ học Redis như **cache**.

---

## 2. In-memory data store khác database thế nào?

| Tiêu chí | PostgreSQL | Redis cache |
|---|---|---|
| Vai trò chính | Source of truth, transaction, query quan hệ | Tăng tốc đọc, giữ bản sao tạm |
| Storage | Disk + buffer/cache | RAM là chính |
| Query | SQL, join, constraint | Key/value và data structures |
| Consistency | Mạnh hơn, có transaction DB | Có thể stale so với DB |
| Khi mất dữ liệu | Nghiêm trọng nếu là DB chính | Cache miss rồi rebuild từ DB |

Rule cho repo này:

```text
PostgreSQL vẫn là source of truth.
Redis chỉ giữ bản sao tạm để đọc nhanh hơn.
```

---

## 3. Cache là gì và vì sao backend dùng cache?

Cache lưu tạm kết quả đọc/tính toán để request sau không phải query DB hoặc gọi external service lại.

Hợp lý khi:

- data đọc nhiều hơn ghi;
- query tốn chi phí;
- data không cần realtime tuyệt đối;
- có key rõ ràng;
- có cách expire/invalidate.

Không nên cache khi:

- data thay đổi liên tục;
- dữ liệu quá nhạy cảm mà chưa có policy rõ;
- query DB đã đủ nhanh và đơn giản;
- chưa biết key/TTL/invalidation;
- cache có thể gây cross-tenant leak.

---

## 4. Cache-aside pattern

Cache-aside là pattern dễ hiểu nhất cho mini-lab:

```text
Read request
-> check Redis by key
-> hit: return cached value
-> miss: query PostgreSQL
-> store result into Redis with TTL
-> return result
```

Khi write/update/delete:

```text
update PostgreSQL
-> evict related Redis key
```

Ưu điểm:

- dễ hiểu;
- DB vẫn là source of truth;
- nếu Redis down hoặc key hết hạn, backend vẫn có thể đọc DB.

Nhược điểm:

- request đầu tiên vẫn hit DB;
- có thể stale nếu quên invalidate;
- nhiều request đồng thời cùng miss có thể gây cache stampede.

---

## 5. Read-through / write-through / write-behind awareness

Chỉ cần biết ở mức awareness:

| Pattern | Ý tưởng | Có dùng ngay không? |
|---|---|---|
| Cache-aside | App tự đọc cache, miss thì đọc DB và set cache | Có, phù hợp mini-lab |
| Read-through | Cache layer tự load data từ source khi miss | Chưa cần |
| Write-through | Write đi qua cache rồi ghi đồng bộ xuống DB/source | Chưa cần |
| Write-behind | Write vào cache trước, ghi DB async sau | Không phù hợp Phase 1 vì dễ mất consistency |

---

## 6. TTL

TTL là thời gian sống của key. Hết TTL thì key tự expire.

Ví dụ:

```text
tenant:1:master-data:code:LAPTOP-01 -> TTL 5 minutes
```

TTL giúp:

- tránh cache giữ dữ liệu cũ mãi;
- tự cleanup key;
- giảm rủi ro stale lâu.

TTL không thay thế invalidation. Nếu update data quan trọng, nên evict key liên quan thay vì chỉ chờ TTL.

---

## 7. Cache invalidation

Invalidation là xóa/cập nhật cache khi source of truth thay đổi.

Ví dụ:

- update `master_data` code/name/category;
- xóa record;
- deactivate record;
- reimport data.

Các cách cơ bản:

- evict key cụ thể;
- evict cache theo prefix/cache name;
- TTL ngắn nếu data ít critical;
- event-driven invalidation sau này nếu có Kafka/CDC.

Trong mini-lab, chỉ cần awareness vì `master_data` hiện chủ yếu read-only.

---

## 8. Eviction policy

Eviction là Redis tự xóa key khi vượt giới hạn memory.

Ở mức beginner:

- TTL expiration: key hết hạn nên biến mất.
- Memory eviction: Redis thiếu memory nên xóa key theo policy như LRU/LFU/TTL.

Với cache, việc key bị evict thường không nghiêm trọng vì app có thể query DB và set lại cache. Nếu mất Redis key mà app mất dữ liệu nghiệp vụ, nghĩa là bạn đang dùng Redis như source of truth sai cách.

---

## 9. Key design

Key Redis nên:

- rõ domain;
- có tenant scope nếu data tenant-aware;
- có version nếu format value thay đổi;
- không chứa secret/token thô;
- không quá dài hoặc tùy tiện.

Ví dụ tốt:

```text
tenant:{tenantId}:master-data:id:{id}
tenant:{tenantId}:master-data:code:{code}
tenant:{tenantId}:master-data:list:active
```

Ví dụ nguy hiểm:

```text
master-data:id:10
code:LAPTOP-01
```

Hai key trên thiếu tenant scope, dễ trả nhầm dữ liệu tenant khác.

---

## 10. Tenant-safe cache key

Với shared-table multi-tenant, cache key phải coi `tenantId` là một phần của identity dữ liệu.

Luồng đúng:

```text
TenantContext -> tenantId
-> build key with tenantId
-> cache lookup
-> DB query vẫn có tenantId nếu cache miss
```

Không được:

- lấy `tenantId` từ request body;
- cache bằng global key nếu response phụ thuộc tenant;
- cache trước rồi mới filter tenant sau;
- tin frontend tự giấu dữ liệu tenant khác.

---

## 11. Data nên/không nên cache

Nên cân nhắc cache:

- lookup read-heavy;
- reference/master data ít đổi;
- config public/tenant-scoped;
- kết quả query đắt nhưng ít thay đổi.

Không nên cache vội:

- dữ liệu thay đổi liên tục;
- dữ liệu tài chính cần consistency chặt;
- dữ liệu nhạy cảm nếu chưa có mã hóa/masking/policy;
- response phụ thuộc permission phức tạp nếu key chưa encode đủ context;
- dữ liệu lớn vượt lợi ích cache.

---

## 12. Common risks

- Stale data: DB đổi nhưng cache chưa evict.
- Cross-tenant leakage: key thiếu `tenantId`.
- Cache sensitive data: lưu quá nhiều PII/token/raw authorization data.
- Cache stampede/thundering herd: nhiều request cùng miss và cùng đánh vào DB.
- Redis as source of truth: app phụ thuộc Redis để không mất dữ liệu nghiệp vụ.
- Over-caching: thêm Redis trước khi đo/hiểu bottleneck.

---

## 13. Áp dụng vào repo hiện tại

Mini-lab nên giữ scope nhỏ:

- cache một read path của `master_data`, ví dụ lookup by code hoặc active list;
- key luôn có `tenantId`;
- DB query vẫn tenant-aware khi cache miss;
- TTL ngắn, ví dụ 5 phút;
- log hoặc Redis CLI chứng minh hit/miss;
- Redis disabled by default để `make app-test` không phụ thuộc Redis.

Không làm ngay:

- distributed lock;
- Redis Streams;
- session store;
- pub/sub;
- cluster/sentinel;
- cache invalidation phức tạp bằng Kafka/CDC.

---

## Nguồn tham khảo chuẩn

- [Redis documentation](https://redis.io/docs/latest/)
- [Redis key eviction](https://redis.io/docs/latest/develop/reference/eviction/)
- [Spring Boot caching](https://docs.spring.io/spring-boot/reference/io/caching.html)
- [Spring Framework cache abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)
