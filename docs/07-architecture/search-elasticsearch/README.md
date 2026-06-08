# Elasticsearch / Search

## Folder này chứa gì?

Nhóm này giải thích khi nào cần search engine thay vì chỉ dùng PostgreSQL query/index, rồi áp dụng vào tenant-aware search mini-lab cho `master_data`.

## Reading Order

1. [elasticsearch-search-service.md](elasticsearch-search-service.md) - foundation: index, document, mapping, analyzer, inverted index, Query DSL.
2. [elasticsearch-request-response-shapes.md](elasticsearch-request-response-shapes.md) - REST URI, request/response/error shape.
3. [elasticsearch-design-patterns-spring-boot.md](elasticsearch-design-patterns-spring-boot.md) - gateway/adapter, search projection, source of truth.
4. [elasticsearch-code-guide-spring-boot.md](elasticsearch-code-guide-spring-boot.md) - Spring Boot class/package/config shape.
5. [elasticsearch-mini-lab-plan.md](elasticsearch-mini-lab-plan.md) - checklist mini-lab.

## Trạng Thái

- Mini-lab đã đóng ở Phase 1 level.
- PostgreSQL vẫn là source of truth.
- Elasticsearch là search projection, disabled by default nếu không chạy lab.

## Caveat

Search query luôn phải filter `tenantId`. Không filter sau khi đã lấy raw results về app.
