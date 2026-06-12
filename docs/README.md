# Docs index

## Mục tiêu

Thư mục `docs/` là source of truth cho kiến thức Phase 1 đã được chuẩn hóa. Local notes, prompt thô, câu hỏi nháp và review nháp không nằm ở đây.

## Đọc theo thứ tự gợi ý

1. `00-gioi-thieu/tong-quan-phase-1.md` - bối cảnh Phase 1.
2. `01-saas/tong-quan-saas.md` - SaaS là gì.
3. `02-multi-tenant/` - multi-tenant và tenant isolation.
4. `03-backend-database-mo-rong/` - PostgreSQL, index, migration, transaction.
5. `04-spring-boot/` - Spring Boot, JPA, filter, repository/service/controller, test.
6. `05-security/` - JWT tạm, Spring Security, Keycloak/OIDC.
7. `06-frontend/` - React Web UI demo mỏng cho flow Keycloak -> Gateway -> backend.
8. `07-architecture/` - map từ lab hiện tại sang kiến trúc target và mini-lab công nghệ sau Keycloak.
9. `99-tong-ket/` - summary mentor-facing, milestone report và template mini-lab.

## Core vs optional

| Nhóm | Vai trò | Trạng thái |
|---|---|---|
| SaaS + multi-tenant | Nền tảng lý thuyết | Core |
| PostgreSQL/backend DB | Nền tảng thực hành SQL + migration + transaction | Core |
| Spring Boot | Demo backend runnable | Core |
| Security/JWT/Keycloak | Auth flow và tenant context từ token | Core/mini-lab |
| Frontend React Web | Thin UI demo Docker-first cho Keycloak/Gateway/backend flow | Final Phase 1 demo |
| Architecture map | Kết nối target diagram với repo | Mentor-facing |
| Elasticsearch/MinIO/Redis/Kafka/Observability/API Gateway | Mini-lab công nghệ sau Keycloak, học từng cái một | Phase 1 done |
| Loki/Kafka UI/Kong/microservice boundaries | Phase 1.5 production-like demo hardening | Loki + Kafka UI + Kong + audit-log-service local labs implemented; cross-service E2E verified |
| DDD/final reflection | Optional hoặc cuối phase, không chặn learning tech | Optional/later |

## Cấu trúc chuẩn cho công nghệ mới

Khi thêm một công nghệ sau Keycloak, ưu tiên cấu trúc:

1. Foundation/concept doc: `docs/07-architecture/<topic-folder>/<tech>.md`.
2. Request/response/config/API shape doc nếu công nghệ có protocol/API dễ nhầm: `docs/07-architecture/<topic-folder>/<tech>-request-response-shapes.md`.
3. Code guide doc: `docs/07-architecture/<topic-folder>/<tech>-code-guide-spring-boot.md`.
4. Lab README: `lab-code/<tech>-lab/README.md`.
5. Summary sau khi xong: `docs/99-tong-ket/nhung-gi-da-nam-duoc.md`.

Template chung: `99-tong-ket/technology-mini-lab-template.md`.
Chuẩn viết theory doc chi tiết: `99-tong-ket/theory-doc-writing-standard.md`.
Demo script cuối Phase 1: `99-tong-ket/phase1-final-demo-script.md`.
Plan Phase 1.5: `99-tong-ket/phase1-5-production-like-demo-plan.md`.

## Không làm trong docs public

- Không lưu token, secret, password thật.
- Không lưu prompt thô hoặc draft answer.
- Không biến mỗi topic thành textbook dài nếu chưa có mini-lab hoặc demo liên quan.
