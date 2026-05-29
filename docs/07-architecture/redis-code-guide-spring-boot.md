# Redis code guide cho Spring Boot

## Vai trò tài liệu

Tài liệu này hướng dẫn shape code Spring Boot cho Redis/cache mini-lab. Concept nền nằm ở `redis-cache-strategy.md`, còn checklist thực hành nằm ở `redis-mini-lab-plan.md`.

Mục tiêu code:

```text
Controller/Service hiện có
-> Service/use-case lấy tenantId từ TenantContext
-> Cache service/gateway build tenant-safe key
-> Redis giữ bản sao tạm có TTL
-> PostgreSQL vẫn là source of truth
```

---

## 1. Chọn integration approach

### Option 1: Spring Cache abstraction

Ý tưởng:

- dùng annotation như `@Cacheable`, `@CacheEvict`;
- Spring Boot auto-config `RedisCacheManager` nếu có Redis và dependency phù hợp.

Ưu điểm:

- ít code;
- hợp với method-level caching đơn giản;
- TTL có thể config qua Spring Cache/Redis cache manager.

Nhược điểm:

- tenant-safe key bằng SpEL/custom key generator có thể khó nhìn với beginner;
- dễ quên key phải chứa tenantId;
- cache behavior nằm trong annotation, đôi khi khó debug hit/miss lúc mới học.

### Option 2: RedisTemplate + cache-aside thủ công

Ý tưởng:

- code tự build key;
- tự `get`, miss thì query DB, rồi `set` với TTL;
- tự evict khi cần.

Ưu điểm:

- thấy rõ key, TTL, hit/miss;
- rất hợp để học tenant-safe cache;
- dễ log và demo bằng Redis CLI.

Nhược điểm:

- nhiều code hơn annotation;
- phải tự giữ logic cache đủ gọn;
- dễ lặp code nếu mở rộng nhiều use case.

### Option 3: Lettuce client trực tiếp

Lettuce là Redis client Java phổ biến. Spring Data Redis mặc định thường dùng Lettuce bên dưới.

Ưu điểm:

- gần client layer;
- nhiều capability nâng cao.

Nhược điểm:

- không cần thiết cho mini-lab Spring Boot beginner;
- dễ bỏ qua abstraction đã có của Spring.

### Option 4: Redisson

Redisson cung cấp nhiều abstraction nâng cao như distributed lock, map/cache object style.

Phù hợp sau này nếu cần lock, advanced distributed structures. Không cần cho mini-lab cache hiện tại.

### Khuyến nghị cho repo này

Dùng **RedisTemplate + cache-aside thủ công** cho mini-lab đầu tiên.

Lý do:

- mục tiêu chính là học key design và tenant safety;
- dễ chứng minh hit/miss;
- không giấu logic tenant key trong annotation;
- gần với pattern Gateway/Adapter đã dùng ở Elasticsearch/MinIO.

Sau khi hiểu rồi, có thể học Spring Cache annotation như hướng rút gọn.

---

## 2. Dependency/config dự kiến

Khi bắt đầu code thật, dependency tối thiểu thường là:

```xml
spring-boot-starter-data-redis
```

Config gợi ý:

```yaml
app:
  cache:
    enabled: ${APP_CACHE_ENABLED:false}
    master-data-ttl-seconds: ${APP_CACHE_MASTER_DATA_TTL_SECONDS:300}

spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:16379}
```

Rule:

- `APP_CACHE_ENABLED=false` mặc định để baseline test không phụ thuộc Redis.
- Redis local chạy ở `localhost:16379` để tránh đụng Redis system port `6379`.
- Không cache secret/token.

---

## 3. Package/class shape đề xuất

```text
com.viettel.demo.cache
├── CacheProperties
├── RedisConfig
├── MasterDataCacheKeyFactory
├── MasterDataCacheGateway
└── CachedMasterData
```

Hoặc giữ nhỏ hơn nếu chỉ làm một use case:

```text
com.viettel.demo.cache
├── CacheProperties
└── MasterDataCacheGateway
```

Không cần tạo framework cache chung quá sớm.

---

## 4. Responsibility của từng class

| Class | Trách nhiệm | Không nên làm |
|---|---|---|
| `CacheProperties` | Bind feature flag, TTL, Redis/cache settings riêng của app. | Không chứa business logic. |
| `RedisConfig` | Cấu hình `RedisTemplate`/serializer nếu cần. | Không gọi service/repository. |
| `MasterDataCacheKeyFactory` | Sinh key có tenant scope. | Không query DB. |
| `MasterDataCacheGateway` | `get/set/evict` Redis cho master data cache. | Không tự lấy tenant từ request body. |
| `MasterDataService` | Quyết định cache-aside flow và DB fallback. | Không expose Redis details ra controller. |

---

## 5. Tenant-safe key pattern

Ví dụ key:

```text
tenant:{tenantId}:master-data:id:{id}
tenant:{tenantId}:master-data:code:{code}
tenant:{tenantId}:master-data:list:active
```

Nếu response phụ thuộc role/permission ngoài tenant, key cần thêm context đó hoặc không cache response đó.

Không dùng:

```text
master-data:{id}
master-data:code:{code}
```

Vì cùng `id/code` có thể xuất hiện ở nhiều tenant.

---

## 6. Cache-aside trong service

Flow học tập:

```text
getByCode(code):
  tenantId = TenantContext.getCurrentTenant()
  key = tenant:{tenantId}:master-data:code:{code}

  cached = cacheGateway.get(key)
  if cached exists:
    log "cache hit"
    return cached

  log "cache miss"
  data = repository.findByTenantIdAndCode(tenantId, code)
  cacheGateway.set(key, data, ttl)
  return data
```

Chú ý:

- DB query vẫn phải có `tenantId`;
- cache hit chỉ hợp lệ nếu key đã có tenantId;
- controller không biết Redis tồn tại;
- nếu Redis disabled, service có thể đi thẳng DB.

---

## 7. TTL đặt ở đâu?

TTL nên là config:

```text
APP_CACHE_MASTER_DATA_TTL_SECONDS=300
```

Không hardcode TTL rải rác trong service.

Gợi ý Phase 1:

- `master_data` read cache: 300 giây;
- data thay đổi thường xuyên: TTL ngắn hoặc không cache;
- data nhạy cảm/permission phức tạp: không cache vội.

---

## 8. Serialization/value shape

Redis value nên là DTO/cache object an toàn, không phải raw JPA entity nếu muốn tránh lazy-loading/internal fields.

Ví dụ:

```text
CachedMasterData(id, tenantId, code, name, category, active)
```

Giữ value nhỏ và rõ. Không cache raw security context, token, password hoặc object quá lớn.

---

## 9. Hit/miss verification

Cách verify thủ công:

1. Start Redis.
2. Call API lần 1 -> log `cache miss`, Redis có key mới.
3. Call API lần 2 cùng tenant/code -> log `cache hit`.
4. Call tenant khác cùng code -> key khác, không dùng cache tenant 1.
5. Dùng Redis CLI:

```bash
redis-cli -p 16379 keys 'tenant:*:master-data:*'
redis-cli -p 16379 ttl 'tenant:1:master-data:code:LAPTOP-01'
```

Với production, không dùng `KEYS` trên dataset lớn; lab nhỏ dùng được để học.

---

## 10. Existing tests

`DataLeakageTest` hiện nên tiếp tục chạy khi Redis disabled.

Sau khi tự code cache, nên thêm test riêng hoặc manual verification cho:

- tenant 1 và tenant 2 có key khác nhau;
- cache miss vẫn query repository tenant-aware;
- thiếu token/invalid token vẫn bị chặn trước service;
- stale/invalidation caveat được ghi rõ nếu chưa implement write invalidation.

---

## 11. Common mistakes

- Dùng key thiếu `tenantId`.
- Cache response sau khi đã filter ở frontend.
- Cache raw JPA entity lớn/phức tạp.
- Cache dữ liệu nhạy cảm mà không có policy.
- Đặt TTL quá dài rồi quên invalidation.
- Bật Redis bắt buộc làm `make app-test` fail khi Redis chưa chạy.
- Dùng Redis như source of truth.
- Over-engineer cache framework trước khi có một use case rõ.

---

## Nguồn tham khảo chuẩn

- [Spring Boot caching](https://docs.spring.io/spring-boot/reference/io/caching.html)
- [Spring Framework cache abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)
- [Spring Data Redis getting started](https://docs.spring.io/spring-data/redis/reference/4.0/redis/getting-started.html)
- [Redis key eviction](https://redis.io/docs/latest/develop/reference/eviction/)
