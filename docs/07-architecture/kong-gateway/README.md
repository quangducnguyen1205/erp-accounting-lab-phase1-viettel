# Kong Gateway

## Folder này chứa gì?

Nhóm này chuẩn bị mini-lab Kong Gateway cho Phase 1.5. Spring Cloud Gateway lab vẫn được giữ để học gateway concept trong Java/Spring; Kong là bước tiếp theo để làm quen platform gateway gần target architecture hơn.

## Reading Order

1. [kong-gateway-foundation.md](kong-gateway-foundation.md) - Kong vs Spring Cloud Gateway, service/route/plugin, DB-less config, lab direction.
2. [kong-local-lab-config-walkthrough.md](kong-local-lab-config-walkthrough.md) - anatomy config Kong DB-less trước, rồi map vào `lab-code/kong-gateway-lab/`.
3. [../../../lab-code/kong-gateway-lab/README.md](../../../lab-code/kong-gateway-lab/README.md) - lệnh chạy local.

## Trạng thái

- Foundation doc đã có.
- Runtime Docker lab đã có ở `lab-code/kong-gateway-lab/`.
- Makefile targets `kong-up/status/logs/down/info/config` đã có.
- Kong DB-less route trực tiếp tới `tenant-demo` host app ở port `8080`.
- Hướng tiếp theo: sau khi có `audit-log-service`, thêm route `/api/audit/**`.

## Caveat

Không xóa Spring Cloud Gateway lab. Nó vẫn hữu ích để hiểu gateway trong hệ sinh thái Spring. Kong lab là bước platform-oriented tiếp theo, không phải replacement bắt buộc cho mọi app.
