# Kong Gateway

## Folder này chứa gì?

Nhóm này chuẩn bị mini-lab Kong Gateway cho Phase 1.5. Spring Cloud Gateway lab vẫn được giữ để học gateway concept trong Java/Spring; Kong là bước tiếp theo để làm quen platform gateway gần target architecture hơn.

## Reading Order

1. [kong-gateway-foundation.md](kong-gateway-foundation.md) - Kong vs Spring Cloud Gateway, service/route/plugin, DB-less config, lab direction.

## Trạng thái

- Planning/foundation doc đã có.
- Runtime Docker lab chưa implement trong commit này.
- Hướng tiếp theo: tạo `lab-code/kong-gateway-lab/` DB-less/declarative config route tới `tenant-demo`, sau này `audit-log-service`.

## Caveat

Không xóa Spring Cloud Gateway lab. Nó vẫn hữu ích để hiểu gateway trong hệ sinh thái Spring. Kong lab là bước platform-oriented tiếp theo, không phải replacement bắt buộc cho mọi app.
