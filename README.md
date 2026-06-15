# Phase 1 - Kho kiến thức SaaS, Multi-tenant và Backend Database

Repository này là nơi lưu trữ kiến thức đã được tổng hợp trong Phase 1 của quá trình học backend cho bài toán ERP/kế toán SaaS đa tenant.

Mục tiêu của repo không phải là lưu mọi nháp học tập, mà là làm nguồn sự thật công khai cho các phần kiến thức đã được học, rà soát và chuẩn hóa. Các câu hỏi tự học, câu trả lời nháp, review, ghi chú mentor, prompt và scratch notes được giữ ở local để không làm nhiễu lịch sử GitHub.

## Trạng thái học hiện tại

- Đã hoàn thành nền tảng SaaS, multi-tenant, SQL playground, migration/locking, ACID/isolation và tenant-aware backend API.
- Đã có demo Spring Boot nhỏ trong `lab-code/tenant-demo` với PostgreSQL/Flyway, tenant-aware API, JWT tạm fallback, Keycloak AuthN/AuthZ mode và các mini-lab architecture.
- Phase 1 hiện đã demo được flow end-to-end bằng React Web UI `Master Data Portal` -> Keycloak -> Kong Gateway -> `tenant-demo` / `audit-log-service` / `file-service` / `search-service` -> PostgreSQL/Redis/Kafka/MinIO/Elasticsearch/Observability. UI này là demo web, không phải React Native/Expo và không phải frontend production.
- Sau buổi báo cáo mentor ngày 11/06/2026, Phase 1.5 đã bổ sung runtime tooling Loki log aggregation, Kafka UI, Kong Gateway và các service split có boundary rõ: audit log, file upload/download và search projection.

## Cấu trúc thư mục

```text
.
├── docs/
│   ├── 00-gioi-thieu/
│   ├── 01-saas/
│   ├── 02-multi-tenant/
│   ├── 03-backend-database-mo-rong/
│   ├── 04-spring-boot/
│   ├── 05-security/
│   ├── 06-frontend/
│   ├── 07-architecture/
│   └── 99-tong-ket/
├── lab-code/
│   ├── sql-playground/
│   ├── common-security/
│   ├── tenant-demo/
│   ├── audit-log-service/
│   ├── file-service/
│   ├── search-service/
│   ├── keycloak-lab/
│   ├── elasticsearch-lab/
│   ├── minio-lab/
│   ├── redis-lab/
│   ├── kafka-lab/
│   ├── observability-lab/
│   ├── loki-lab/
│   ├── kafka-ui-lab/
│   ├── kong-gateway-lab/
│   ├── gateway-demo/
│   └── web-ui-demo/
├── reports/
│   └── latex/
├── presentation-notes/
├── local/
├── .gitignore
├── LICENSE
└── README.md
```

## Nội dung được commit

- Ghi chú lý thuyết markdown đã được tổng hợp.
- Báo cáo LaTeX dạng nguồn `.tex`.
- Tài liệu thuyết trình dạng markdown có sơ đồ, bảng và ghi chú nói ngắn.
- Lab code nhỏ phục vụ việc học Phase 1, bao gồm SQL playground, Spring Boot tenant demo và Keycloak mini-lab.
- Các README giải thích cách sử dụng repo.

## Nội dung local-only

Các nội dung sau không được commit:

- Câu hỏi tự học.
- Câu trả lời nháp.
- Review câu trả lời.
- Ghi chú mentor/reviewer.
- Prompt thô.
- Scratch notes, bản nháp tạm, file backup.
- File build từ LaTeX, bao gồm PDF mặc định.

Thư mục `local/` chỉ giữ một file README làm placeholder. Mọi nội dung thực tế trong `local/` được ignore.

## Báo cáo

Báo cáo tiến độ nằm tại:

```text
reports/latex/bao-cao-saas-multi-tenant.tex
```

Báo cáo được viết bằng LaTeX để có thể compile sang PDF khi cần:

```bash
cd reports/latex
xelatex bao-cao-saas-multi-tenant.tex
```

PDF không được commit mặc định vì là file build. Nếu sau này cần publish PDF, có thể chủ động force-add file PDF.

## Tài liệu thuyết trình

Tài liệu thuyết trình nằm tại:

```text
presentation-notes/thuyet-trinh-saas-multi-tenant.md
```

Đây không phải slide thiết kế. File này là talk-through document: có sơ đồ ASCII, bảng so sánh, ý chính và ghi chú khi trình bày để có thể mở ra và trao đổi nhanh với leader.

## Quyết định về code demo

Repository này vẫn được ưu tiên làm kho kiến thức Phase 1, nhưng hiện đã có lab code nhỏ để chứng minh lý thuyết:

- `lab-code/sql-playground/`: thực hành PostgreSQL multi-tenant.
- `lab-code/tenant-demo/`: Spring Boot + PostgreSQL/Flyway + tenant-aware API.
- `lab-code/keycloak-lab/`: Keycloak/OIDC mini-lab local.
- `lab-code/gateway-demo/`: Spring Cloud Gateway static route tới `tenant-demo`.
- `lab-code/audit-log-service/`: service split đầu tiên, consume `MasterDataChangedEvent` và expose audit API tenant-aware.
- `lab-code/file-service/`: service split cho upload/download file tenant-aware qua MinIO.
- `lab-code/search-service/`: service split cho Elasticsearch projection/search tenant-aware qua Kafka event.
- `lab-code/web-ui-demo/`: React Web/Vite thin client chạy Docker-first, gọi Gateway sau khi login Keycloak. Đây là demo trực quan cuối Phase 1, không phải React Native/Expo.

Java backend services hiện ưu tiên chạy Maven/IntelliJ trên host (`tenant-demo`, `audit-log-service`, `file-service`, `search-service`). Docker vẫn dùng cho infra/tooling như PostgreSQL, Keycloak, Kafka, MinIO, Elasticsearch, Kong, Loki/Grafana/Alloy và Web UI.

Nếu demo phát triển thành một project thật sự lớn, nên tách sang repository riêng để repo kiến thức này vẫn gọn, rõ và dễ đọc. Trong Phase 1, lab code vẫn được giữ nhỏ và bám sát mục tiêu học.

Điểm điều hướng chính:

- `docs/README.md`: index tài liệu.
- `ROADMAP.md`: tiến độ và kế hoạch học.
- `docs/99-tong-ket/phase1-final-demo-script.md`: demo script cuối Phase 1 cho UI/Gateway/backend/integrations.
- `docs/99-tong-ket/phase1-5-production-like-demo-plan.md`: kế hoạch Phase 1.5 sau feedback mentor 11/06.
- `presentation-notes/demo-script-keycloak-tenant-flow.md`: demo script backend Keycloak cũ, vẫn hữu ích nếu chỉ muốn trình bày auth flow.

## Chạy final demo local

Luồng demo hiện tại được gom về một workflow ngắn trong `lab-code/Makefile`:

```bash
cd lab-code
make help
make up
make status
make down
make clean-logs   # optional, chỉ khi muốn xóa generated logs/*.log
```

`make up` bật Docker infra/tooling/web UI và chạy bốn Java service chính bằng Maven ở background: `tenant-demo`, `audit-log-service`, `file-service`, `search-service`. Các target mini-lab lịch sử vẫn còn trong `lab-code/Makefile.legacy` để học từng công nghệ riêng, ví dụ `make -f Makefile.legacy kafka-up`.

Workflow học code dự kiến:

1. Tự implement task code được giao trước.
2. Sau đó mới nhờ Agent review, chỉ ra lỗi, đề xuất sửa và giải thích.
3. Mục tiêu là học bằng cách tự viết code, không phụ thuộc vào tự động hóa ngay từ đầu.

## Nguyên tắc quản lý repo

1. Học đến đâu, chuẩn hóa kiến thức đến đó.
2. GitHub chỉ lưu kiến thức đã được tổng hợp, không lưu toàn bộ nháp học tập.
3. Câu hỏi, câu trả lời nháp, review, prompt và scratch notes là local-only.
4. Báo cáo được viết bằng LaTeX để có thể compile sang PDF.
5. Tài liệu thuyết trình là markdown dạng sơ đồ/tóm tắt, không phải slide thiết kế.
6. Code demo chỉ giữ ở mức Phase 1 learning lab; không overclaim production readiness.

## Chủ đề chính đã tổng hợp

- SaaS là mô hình phân phối phần mềm.
- Phân biệt SaaS, cloud, subscription, on-premise và multi-tenant.
- Multi-tenant là pattern kiến trúc, thường đi cùng SaaS nhưng không đồng nghĩa với SaaS.
- Các mô hình tenant isolation: shared table với `tenant_id`, schema per tenant, database per tenant.
- Trade-off giữa chi phí, isolation, migration complexity, blast radius, operational complexity và customization.
- Feature flags, zero-downtime deployment, rolling deployment, blue-green deployment và backward-compatible migration.
- Noisy neighbor, index tenant-aware, locking, rollback, partitioning, vacuum, read replica và vai trò của PostgreSQL.
- Spring Boot Resource Server, JWT tạm, Keycloak/OIDC mini-lab và tenant-aware API.
- Target architecture adoption map: React, API Gateway, Keycloak, PostgreSQL, Redis, Kafka, Debezium, MinIO, Elasticsearch, observability, LLM providers và external integrations ở mức phù hợp Phase 1.
