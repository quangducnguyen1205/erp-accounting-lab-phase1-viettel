# Mục lục frontend

## Thư mục này chứa gì?

Thư mục này chỉ chứa phần frontend cần thiết để demo flow kiến trúc Phase 1. Repo vẫn ưu tiên backend learning lab.

## Thứ tự đọc đề xuất

1. [react-web-keycloak-gateway-demo.md](react-web-keycloak-gateway-demo.md) - React Web + Keycloak + Gateway demo.

## Trạng thái

- React Web UI demo đã được implement và verify ở `lab-code/web-ui-demo/`.
- UI chạy theo Docker-first workflow qua `make -f Makefile.legacy web-ui-up`, không yêu cầu local npm.
- UI là thin client: login Keycloak, gọi API qua Gateway, load/create `master_data`, lookup by code cho Redis demo path và hiển thị requestId để đối chiếu log.
- Product direction mới là `Master Data Portal`: một business app nhỏ để quản lý master data và xem activity log, không phải architecture console.
- React implementation đã được realign theo IA mới: Tổng quan, Danh mục tham chiếu, Tra cứu, Tài liệu đính kèm, Nhật ký hoạt động, Demo & kỹ thuật, Tài khoản.
- Không dùng React Native hoặc Expo trong repo Viettel Phase 1 này.

## Giới hạn hiện tại

Frontend này không phải production SPA. Backend vẫn validate JWT, map role, set tenant context và enforce tenant-aware repository query.
