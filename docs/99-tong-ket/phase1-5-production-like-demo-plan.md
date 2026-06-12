# Phase 1.5 - Production-like architecture demo plan

Ngày cập nhật: 11/06/2026.

Tài liệu này ghi lại hướng đi sau buổi báo cáo mentor Đạt ngày 11/06. Phase 1 core learning đã đủ nền để đọc/hiểu code do AI hỗ trợ tạo ra, review runtime flow và dùng Codex chủ động hơn cho implementation. Phase 1.5 không biến repo thành production ERP, mà làm demo gần kiến trúc target hơn.

## 1. Current architecture summary

Hiện repo đã có:

- React Web UI Docker-first login Keycloak và gọi Gateway.
- Keycloak/OIDC/RBAC local lab, đã bổ sung PostgreSQL persistence và bootstrap script để demo dễ tái tạo.
- Spring Cloud Gateway static route đến `tenant-demo`.
- `tenant-demo` Spring Boot backend: tenant-aware `master_data`, PostgreSQL/Flyway, Redis cache-aside, Kafka producer/consumer, MinIO, Elasticsearch, Actuator/logging/Micrometer.
- Prometheus/Grafana local lab cho metrics.
- Docker-first workflow qua `lab-code/Makefile`.

Flow hiện tại:

```text
React Web
-> Keycloak
-> Spring Cloud Gateway
-> tenant-demo
-> PostgreSQL / Redis / Kafka / MinIO / Elasticsearch / Actuator metrics
-> Prometheus/Grafana
```

## 2. Mentor feedback 11/06/2026

Các hướng cải thiện trước demo nghiêm túc hơn tuần sau:

1. Thêm centralized log checking bằng Loki hoặc stack tương tự.
2. Thêm Kafka UI để xem topic/message/consumer group.
3. Dùng Kong Gateway để luyện gateway platform gần target architecture.
4. Tiến gần microservice deployment model hơn bằng cách split 1-2 trách nhiệm rõ ràng.
5. Dùng Kafka giữa nhiều service, không chỉ same-app producer/consumer.
6. Chỉ polish React Web UI sau khi backend/service boundaries ổn.

## 3. Gap analysis

| Gap | Hiện trạng | Vì sao cần xử lý |
|---|---|---|
| Log nhiều service | Đọc terminal/log container thủ công | Khi có gateway + 2 backend services + Kafka consumer, tìm lỗi bằng nhiều terminal không scale. |
| Kafka visibility | Chỉ thấy producer/consumer log | Kafka UI giúp thấy topic, partition, offset, key/value, consumer group và lag. |
| Gateway platform | Đã hiểu bằng Spring Cloud Gateway static route | Target architecture dùng Kong, nên cần biết service/route/plugin/declarative config. |
| Service boundary | `tenant-demo` vẫn là một app gom nhiều lab | Chưa cảm nhận rõ service ownership, API boundary, deployment/log riêng. |
| Kafka flow | Same-app producer/consumer | Cần event từ service A sang service B để Kafka trở nên thật hơn. |
| UI demo | Baseline UI đã chạy | Chưa nên mở rộng UI khi backend route/service chưa ổn. |

## 4. Proposed target for next week

Mục tiêu demo Phase 1.5:

```text
Browser React Web UI
  -> Keycloak
  -> Kong Gateway
    -> master-data-service
    -> audit-log-service

master-data-service
  -> PostgreSQL
  -> Redis
  -> Kafka publish MasterDataChangedEvent

audit-log-service
  -> Kafka consume MasterDataChangedEvent
  -> PostgreSQL audit table hoặc log/projection nhỏ

Observability
  -> Prometheus metrics
  -> Loki logs
  -> Grafana dashboard/explore

Kafka UI
  -> inspect broker/topic/message/consumer group

Optional existing labs
  -> MinIO file storage
  -> Elasticsearch search
```

## 5. Service split options

| Option | Ý tưởng | Ưu điểm | Nhược điểm | Kết luận |
|---|---|---|---|---|
| `master-data-service` + `audit-log-service` | Service mới consume `MasterDataChangedEvent` và lưu/log audit. | Kafka thành cross-service thật, domain nhỏ, dễ demo, tận dụng event hiện có. | Audit service đơn giản. | Chọn trước. |
| Split `file-service` | Tách MinIO upload/download ra service riêng. | File ownership rõ, MinIO service-owned. | Kéo thêm auth/upload/UI/security work. | Để sau nếu còn thời gian. |
| Split `search-service` | Search projection consume event từ master data. | Rất realistic cho Elasticsearch. | Eventual consistency/reindex/search complexity dễ phình. | Later/backlog. |
| Notification/reporting service | Consumer log/notification/report. | Dễ làm. | Có thể hơi artificial nếu chưa có use case. | Không ưu tiên. |

## 6. Recommended option

Chọn `audit-log-service` làm service thứ hai.

Lý do chính:

- Không cần nghĩ domain kế toán mới.
- Có trách nhiệm rõ: ghi nhận thay đổi business entity.
- Dùng lại `MasterDataChangedEvent`.
- Làm Kafka cross-service thật.
- Tạo thêm service để Loki/Grafana log aggregation có ý nghĩa.
- Kong có nhiều route hơn sau này.

## 7. Immediate implementation order

1. **Loki/log aggregation lab**: đã có local Docker lab với Loki + Grafana + Grafana Alloy để gom Docker container logs và search bằng Grafana Explore.
2. **Kafka UI lab**: đã có local Docker lab để inspect broker/topic/message/consumer group trước khi split service.
3. **Kong Gateway lab**: bước kế tiếp, DB-less/declarative route `/api/master-data/**`, sau này `/api/audit/**`.
4. **Audit-log-service skeleton + implementation**: service nhỏ consume Kafka event, có own logs/metrics và optional DB table.
5. **Cross-service Kafka verification**: create/update master data -> event -> audit service consumed/stored/logged.
6. **Final React Web polish**: UI gọi Kong, có route xem audit nếu API thật đã có.

## 8. Non-goals

- Không Kubernetes.
- Không full production IAM/RBAC.
- Không full ERP/accounting domain.
- Không split service chỉ để có nhiều service.
- Không build UI lớn trước khi backend boundaries rõ.
- Không đưa secrets/tokens/private IP vào repo.

## 9. Suggested next Codex task

> Implement Kong Gateway local lab next, Docker-first, using DB-less/declarative config to route `/api/master-data/**` to the current backend and prepare a future `/api/audit/**` route.

Lý do: Loki/log aggregation và Kafka UI đã có nền local. Kong là gateway platform gần target architecture hơn Spring Cloud Gateway, và cần có trước khi split thêm `audit-log-service` để route nhiều backend service.
