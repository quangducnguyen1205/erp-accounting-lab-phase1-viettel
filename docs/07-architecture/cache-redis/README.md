# Redis / Cache

## Thư mục này chứa gì?

Nhóm này giải thích Redis và cache-aside pattern, rồi áp dụng vào read-by-code path của `master_data`.

## Thứ tự đọc đề xuất

1. [redis-cache-strategy.md](redis-cache-strategy.md) - foundation: Redis, in-memory cache, TTL, invalidation, tenant-safe key.
2. [redis-code-guide-spring-boot.md](redis-code-guide-spring-boot.md) - RedisTemplate + manual cache-aside design.

## Trạng Thái

- Mini-lab đã đóng.
- Cache key có tenant scope.
- Cache disabled mode giữ app/test không phụ thuộc Redis.

## Giới hạn hiện tại

Redis là cache, không phải source of truth. Update/delete eviction chưa phải production-grade, stale cache có thể tồn tại tới TTL.
