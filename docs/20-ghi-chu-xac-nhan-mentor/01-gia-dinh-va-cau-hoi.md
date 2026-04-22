# 📋 Ghi chú xác nhận với Mentor

> Tài liệu này ghi lại các **giả định**, **câu hỏi mở**, và **điểm cần verify** với mentor.
> Cập nhật liên tục trong suốt Phase 1.

---

## Giả định đã đưa ra trong tài liệu

### Về kiến trúc

| # | Giả định | Cơ sở | Cần confirm? |
|---|---------|-------|:------------:|
| A1 | Phase 1 demo nên dùng modular monolith, chưa tách microservices | Team nhỏ, 1 developer, demo đơn giản | ✅ |
| A2 | Tenant isolation dùng MH1 (tenant_id column) cho demo | Mentor nói "bắt đầu với tenant là một bản ghi" | ✅ |
| A3 | Auth tự implement JWT, chưa dùng Keycloak | Để hiểu sâu auth flow trước | ✅ |
| A4 | Chưa cần Kong API Gateway cho demo | Chỉ có 1 backend service | ✅ |
| A5 | Chưa cần Kafka, Redis, MinIO trong Phase 1 | Modular monolith, chưa có inter-service comm | ✅ |
| A6 | Frontend React dùng Vite | Nhẹ, nhanh, phù hợp demo | ⚠️ |

### Về công nghệ

| # | Giả định | Cơ sở | Cần confirm? |
|---|---------|-------|:------------:|
| T1 | Backend dùng Spring Boot 3 + Java 17+ | Mentor nói "BE: Spring Boot" | ✅ |
| T2 | Database dùng PostgreSQL 15 | Mentor nói "DB: PostgreSQL" | ✅ |
| T3 | Build tool: Maven hoặc Gradle | Standard Java | ⚠️ |
| T4 | DB migration dùng Flyway | Phổ biến với Spring Boot | ⚠️ |

### Về scope demo

| # | Giả định | Cơ sở | Cần confirm? |
|---|---------|-------|:------------:|
| S1 | CRUD module: "Master Data" (danh mục chung) | Mentor nói "một module CRUD master-data" | ✅ |
| S2 | 10 tenants mock data | Mentor nói "mock data khoảng 10 khách hàng" | ✅ |
| S3 | Chưa cần nghiệp vụ kế toán trong demo | Mentor nói "Phase 1 chưa tập trung kế toán" | ✅ |
| S4 | RBAC đơn giản: ADMIN, ACCOUNTANT, VIEWER | Suy luận từ demo scope | ✅ |

---

## Câu hỏi mở cho Mentor

### Ưu tiên cao

1. **Master Data module cụ thể là gì?** Danh mục vật tư? Danh mục nhà cung cấp? Danh mục chung? Hay tự chọn?

2. **Auth: tự implement hay dùng Keycloak?** Em đề xuất tự implement JWT trước để học, sau đó migrate sang Keycloak. Anh đồng ý không?

3. **Demo deploy ở đâu?** Chạy local Docker Compose? Hay cần deploy lên server/cloud?

4. **Code review:** Anh muốn review code theo milestone hay cuối Phase 1?

### Ưu tiên trung bình

5. **Tech stack cụ thể:** Spring Boot version? Java version? Build tool (Maven vs Gradle)?

6. **Repo structure:** Demo code và tài liệu chung 1 repo hay tách?

7. **Testing:** Có yêu cầu viết unit test / integration test không? Mức coverage?

8. **Frontend detail:** Có yêu cầu UI design cụ thể không? Hay UI cơ bản là đủ?

### Ưu tiên thấp (để sau)

9. **Phase 2 scope preview:** Phase 2 sẽ tập trung gì? Để em thiết kế Phase 1 cho dễ mở rộng.

10. **Integration với hệ thống chính:** Sau này demo code có integrate vào hệ thống chính không? Hay chỉ để học?

---

## Điểm cần verify sau khi học

| # | Nội dung | Trạng thái | Ghi chú |
|---|---------|:----------:|---------|
| V1 | Hiểu đúng multi-tenant theo ý mentor | ⏳ | Trình bày lại cho mentor |
| V2 | Kiến trúc demo phù hợp kỳ vọng | ⏳ | Vẽ diagram, hỏi mentor |
| V3 | RBAC model đủ cho demo | ⏳ | Confirm role/permission list |
| V4 | Data model master-data đúng hướng | ⏳ | Show ER diagram cho mentor |

---

## Cầu nối Phase 2

Những thứ Phase 1 chưa làm nhưng cần chuẩn bị tư duy:

1. **Nghiệp vụ kế toán** — chưa học sâu nhưng biết sơ đồ module (Purchase, Sales, GL...)
2. **Keycloak integration** — hiểu OAuth2/OIDC concept, migrate từ JWT tự build
3. **Microservices tách** — module đã tách rõ ràng, chỉ cần tách deployment
4. **Kafka / event-driven** — hiểu concept, implement khi tách service
5. **Debezium CDC** — hiểu concept, implement khi cần data sync

---

*Cập nhật file này mỗi khi có giả định mới hoặc sau mỗi buổi họp mentor.*
