# Microservice Boundaries

## Folder này chứa gì?

Nhóm này chuẩn bị quyết định service split cho Phase 1.5. Mục tiêu không phải tách monolith cho vui, mà tạo thêm một service có trách nhiệm rõ để học gateway routing, centralized logs và Kafka cross-service flow.

## Reading Order

1. [phase1-service-split-options.md](phase1-service-split-options.md) - phân tích các option split và khuyến nghị `audit-log-service`.
2. [audit-log-service-split-plan.md](audit-log-service-split-plan.md) - boundary, ownership, tenant isolation và done criteria cho service split đầu tiên.
3. [audit-log-service-code-walkthrough.md](audit-log-service-code-walkthrough.md) - giải thích code/config/runtime flow của `lab-code/audit-log-service`.

## Trạng thái

- `audit-log-service` đã được thêm làm service split đầu tiên.
- Service consume `MasterDataChangedEvent`, lưu audit vào schema riêng và expose read-only API tenant-aware.
- Hướng tiếp theo: verify cross-service Kafka flow end-to-end rồi polish React Web UI.

## Caveat

Microservice không chỉ là di chuyển file. Cần nghĩ ownership, database/schema, API contract, event contract, deploy/log/test riêng.
