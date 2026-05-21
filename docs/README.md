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
7. `07-architecture/` - map từ lab hiện tại sang kiến trúc target.
8. `99-tong-ket/` - summary mentor-facing và milestone report.

## Core vs optional

| Nhóm | Vai trò | Trạng thái |
|---|---|---|
| SaaS + multi-tenant | Nền tảng lý thuyết | Core |
| PostgreSQL/backend DB | Nền tảng thực hành SQL + migration + transaction | Core |
| Spring Boot | Demo backend runnable | Core |
| Security/JWT/Keycloak | Auth flow và tenant context từ token | Core/mini-lab |
| Architecture map | Kết nối target diagram với repo | Mentor-facing |
| DDD/React/Redis/Kafka/MinIO/Elastic | Học just-in-time khi cần | Optional/later |

## Không làm trong docs public

- Không lưu token, secret, password thật.
- Không lưu prompt thô hoặc draft answer.
- Không biến mỗi topic thành textbook dài nếu chưa có mini-lab hoặc demo liên quan.

