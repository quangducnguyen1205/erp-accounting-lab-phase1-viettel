# Microservice Boundaries

## Folder này chứa gì?

Nhóm này chuẩn bị quyết định service split cho Phase 1.5. Mục tiêu không phải tách monolith cho vui, mà tạo thêm một service có trách nhiệm rõ để học gateway routing, centralized logs và Kafka cross-service flow.

## Reading Order

1. [phase1-service-split-options.md](phase1-service-split-options.md) - phân tích các option split và khuyến nghị `audit-log-service`.

## Trạng thái

- Planning doc đã có.
- Chưa implement service split trong commit này.
- Hướng tiếp theo sau Loki/Kafka UI/Kong: thêm `audit-log-service`.

## Caveat

Microservice không chỉ là di chuyển file. Cần nghĩ ownership, database/schema, API contract, event contract, deploy/log/test riêng.
