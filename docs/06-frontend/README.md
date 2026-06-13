# Frontend Notes

## Folder này chứa gì?

Folder này chỉ chứa phần frontend cần thiết để demo flow kiến trúc Phase 1. Repo vẫn ưu tiên backend learning lab.

## Reading Order

1. [react-web-keycloak-gateway-demo.md](react-web-keycloak-gateway-demo.md) - React Web + Keycloak + Gateway demo.
2. [final-web-ui-design-plan.md](final-web-ui-design-plan.md) - Design-first plan for the `Master Data Portal` product UI.
3. [final-web-ui-figma-screen-handoff.md](final-web-ui-figma-screen-handoff.md) - Figma screen/frame handoff and screenshot checklist.

## Trạng Thái

- React Web UI demo đã được implement và verify ở `lab-code/web-ui-demo/`.
- UI chạy theo Docker-first workflow qua `make web-ui-up`, không yêu cầu local npm.
- UI là thin client: login Keycloak, gọi API qua Gateway, load/create `master_data`, lookup by code cho Redis demo path và hiển thị requestId để đối chiếu log.
- Product direction mới là `Master Data Portal`: một business app nhỏ để quản lý master data và xem activity log, không phải architecture console.
- React implementation đã được realign theo IA mới: Dashboard, Master Data, Activity Log, Account.
- Full screen handoff đang được tracking ở [final-web-ui-figma-screen-handoff.md](final-web-ui-figma-screen-handoff.md). Figma Starter MCP limit hiện còn chặn việc generate/export toàn bộ screen set tự động.
- Không dùng React Native hoặc Expo trong repo Viettel Phase 1 này.

## Caveat

Frontend này không phải production SPA. Backend vẫn validate JWT, map role, set tenant context và enforce tenant-aware repository query.
