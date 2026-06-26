# Mục lục thiết kế domain

## Thư mục này chứa gì?

Thư mục `08-design/` lưu các tài liệu tham chiếu về thiết kế domain và ranh giới module/service. Nội dung ở đây là reference để đọc lại sau khi đã hiểu luồng runtime của repo, không phải kế hoạch refactor.

## Tài liệu hiện có

1. [ddd-awareness.md](ddd-awareness.md) - nhận biết DDD, domain boundary, aggregate, service/module ownership và cách áp dụng đúng mức cho Phase 1 / Phase 1.5.

## Ranh giới với các thư mục khác

- `docs/04-spring-boot/` giải thích entity/repository/service/controller ở mức Spring Boot implementation.
- `docs/07-architecture/microservice-boundaries/` giải thích service split đã có trong repo.
- `docs/08-design/` giải thích cách suy nghĩ về domain boundary trước khi quyết định refactor hoặc tách service lớn hơn.

## Trạng thái hiện tại

Repo hiện đã có các boundary runtime rõ ràng cho `tenant-demo`, `audit-log-service`, `file-service` và `search-service`. DDD trong Phase 1 / Phase 1.5 chỉ dùng để đọc hiểu và đánh giá boundary, không phải yêu cầu viết lại code.
