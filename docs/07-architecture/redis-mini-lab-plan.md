# Redis mini-lab plan

## Mục tiêu

Tạo một mini-lab nhỏ để hiểu cache-aside + tenant-safe key trong backend Spring Boot. Không biến Redis thành source of truth và không làm cache framework phức tạp.

Mini-lab đề xuất:

```text
GET master_data by code/id
-> cache key có tenantId
-> miss thì query PostgreSQL tenant-aware
-> set Redis với TTL
-> request sau hit Redis
```

---

## 1. Scope

Nên làm:

- cache một read path nhỏ của `master_data`;
- key có tenant scope;
- TTL cấu hình được;
- Redis disabled by default;
- manual verification bằng HTTP + Redis CLI/logs.

Không làm ngay:

- cache mọi endpoint;
- distributed lock;
- Redis Streams/pub-sub;
- session storage;
- write-behind;
- production Redis cluster/sentinel;
- cache invalidation phức tạp bằng Kafka/CDC.

---

## 2. Files dự kiến

Docs đã có:

- `docs/07-architecture/redis-cache-strategy.md`
- `docs/07-architecture/redis-code-guide-spring-boot.md`
- `docs/07-architecture/redis-mini-lab-plan.md`

Lab setup:

- `lab-code/redis-lab/README.md`
- `lab-code/redis-lab/docker-compose.yml`

Code sau này nếu tự implement:

```text
lab-code/tenant-demo/src/main/java/com/viettel/demo/cache/
├── CacheProperties.java
├── RedisConfig.java
├── MasterDataCacheGateway.java
└── CachedMasterData.java
```

Có thể ít file hơn nếu giữ mini-lab thật nhỏ.

---

## 3. Config dự kiến

```env
APP_CACHE_ENABLED=false
APP_CACHE_MASTER_DATA_TTL_SECONDS=300
REDIS_HOST=localhost
REDIS_PORT=16379
```

Không bật mặc định để `make app-test` không phụ thuộc Redis.

---

## 4. Coding order đề xuất

1. Thêm dependency Redis khi bắt đầu code thật.
2. Thêm config properties/feature flag.
3. Tạo Redis config hoặc RedisTemplate bean nếu cần custom serializer.
4. Tạo key factory hoặc gateway để key luôn có tenantId.
5. Chọn một service method:
   - `findByCode`;
   - hoặc `findById`;
   - hoặc active list nếu muốn quan sát rõ hơn.
6. Implement cache-aside:
   - get cache;
   - miss -> query DB;
   - set cache with TTL.
7. Log ngắn `cache hit/miss` cho lab.
8. Verify tenant 1/2 không dùng chung key.

---

## 5. Verification checklist

### Infra

```bash
cd lab-code
make redis-up
make redis-status
```

### Redis CLI

```bash
docker exec -it viettel-redis redis-cli
PING
KEYS tenant:*
TTL tenant:1:master-data:code:LAPTOP-01
```

Lab nhỏ được phép dùng `KEYS`; production không nên dùng `KEYS` trên dataset lớn.

### HTTP/API behavior sau khi code

- Tenant 1 gọi lookup lần 1 -> cache miss.
- Tenant 1 gọi lookup lần 2 -> cache hit.
- Tenant 2 gọi cùng code -> miss/key khác, không dùng cache tenant 1.
- Missing token -> `401`.
- Invalid token -> `401`.
- Cross-tenant vẫn không leak vì DB query và cache key đều có tenantId.

---

## 6. Done criteria

- Redis local chạy được.
- Docs giải thích cache-aside, TTL, invalidation, eviction và tenant-safe key.
- Code nếu implement có feature flag disabled by default.
- Cache key có `tenantId`.
- Có cách chứng minh hit/miss.
- `make app-test` vẫn pass khi Redis không chạy.
- Summary ghi rõ stale data/invalidation caveat.

---

## 7. Câu hỏi review sau khi tự code

Khi nhờ Codex review, hỏi tập trung:

- Key Redis đã đủ tenant-safe chưa?
- Có chỗ nào cache data trước khi check tenant/auth không?
- Redis có đang thành source of truth không?
- TTL/invalidation caveat đã rõ chưa?
- App/test có phụ thuộc Redis khi `APP_CACHE_ENABLED=false` không?
