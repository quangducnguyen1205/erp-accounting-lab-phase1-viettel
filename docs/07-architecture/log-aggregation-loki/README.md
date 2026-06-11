# Log Aggregation / Loki

## Folder này chứa gì?

Nhóm này chuẩn bị mini-lab centralized logging cho Phase 1.5. Mục tiêu là không còn phải đọc log thủ công ở nhiều terminal khi repo có Gateway, backend service và Kafka consumer service.

## Reading Order

1. [loki-foundation.md](loki-foundation.md) - Loki là gì, collector/agent, Grafana Explore, labels, Promtail/Alloy caveat.

## Trạng thái

- Planning/foundation doc đã có.
- Runtime Docker lab chưa implement trong commit này.
- Hướng tiếp theo: tạo `lab-code/loki-lab/` với Loki + collector + Grafana datasource, giữ Docker-first workflow.

## Caveat

Promtail đã EOL theo Grafana docs. Nếu dùng Promtail trong local lab chỉ vì đơn giản/quen thuộc thì phải ghi rõ là lựa chọn học local, không phải hướng mới cho production. Hướng hiện đại hơn là Grafana Alloy hoặc collector tương thích khác.
