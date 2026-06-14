# Elasticsearch / Search

## Folder này chứa gì?

Nhóm này giải thích khi nào cần search engine thay vì chỉ dùng PostgreSQL query/index, rồi áp dụng vào tenant-aware search cho `Master Data Portal`.

Phase 1 có embedded mini-lab trong `tenant-demo` để học Elasticsearch nhanh. Phase 1.5 đã chuyển runtime chính sang service riêng: `lab-code/search-service`.

## Reading Order

1. [elasticsearch-search-service.md](elasticsearch-search-service.md) - foundation: index, document, mapping, analyzer, inverted index, Query DSL.
2. [elasticsearch-request-response-shapes.md](elasticsearch-request-response-shapes.md) - REST URI, request/response/error shape.
3. [elasticsearch-design-patterns-spring-boot.md](elasticsearch-design-patterns-spring-boot.md) - gateway/adapter, search projection, source of truth.
4. [elasticsearch-code-guide-spring-boot.md](elasticsearch-code-guide-spring-boot.md) - Spring Boot class/package/config shape.
5. [elasticsearch-mini-lab-plan.md](elasticsearch-mini-lab-plan.md) - checklist mini-lab.
6. [search-service-split-plan.md](search-service-split-plan.md) - Phase 1.5 service boundary: Kafka event -> search-service -> Elasticsearch.
7. [cross-service-search-projection.md](cross-service-search-projection.md) - code walkthrough and runtime flow for the event-driven projection.

## Trạng Thái

- Mini-lab đã đóng ở Phase 1 level.
- Phase 1.5 runtime search service đã được tách ra ở `lab-code/search-service`.
- PostgreSQL vẫn là source of truth.
- Elasticsearch là search projection, chạy khi bật `search-service` và Elasticsearch local.
- React UI gọi `GET /api/search/master-data?keyword=...` qua Kong; UI không gọi Elasticsearch trực tiếp.

## Caveat

Search query luôn phải filter `tenantId`. Không filter sau khi đã lấy raw results về app.

Search projection là eventually consistent. Sau khi create/update/deactivate master data, cần Kafka deliver event và `search-service` index xong thì kết quả mới xuất hiện.
