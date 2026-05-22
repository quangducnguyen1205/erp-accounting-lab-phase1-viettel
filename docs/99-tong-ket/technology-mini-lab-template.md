# Technology mini-lab template

## Mục tiêu

Template này dùng cho các mini-lab công nghệ sau Keycloak: Elasticsearch, MinIO, Redis, Kafka, Observability, Gateway hoặc công nghệ tương tự.

Mỗi mini-lab không cần thành production system. Nhưng mỗi mini-lab nên có đủ: concept, code shape, config, verification và summary để báo cáo mentor.

## 1. Concept

- Công nghệ này là gì?
- Nó giải quyết vấn đề nào trong kiến trúc target?
- Khi nào không nên dùng nó?
- Nó khác gì với phần đã học trước đó?

## 2. Vị trí trong target architecture

- Nằm ở frontend, gateway, backend service, storage, messaging, observability hay external integration?
- Service nào sẽ gọi nó?
- Data đi vào/đi ra thế nào?
- Nó có phải source of truth không?

## 3. Spring Boot integration pattern

- Dependency/library nên dùng.
- Config properties cần có.
- Package/class shape gợi ý.
- Service/client class nào chịu trách nhiệm gọi công nghệ đó.
- Controller/API shape nếu cần expose endpoint.
- Test hoặc manual verification nên đi qua lớp nào.

## 4. Config/env

- Biến `.env.example` cần thêm.
- Default local an toàn.
- Feature flag có cần không?
- Có cần Docker Compose local không?
- Secret/token/password nào không được commit?

## 5. Data/security/tenant concerns

- Có tenantId không?
- Có nguy cơ leak cross-tenant không?
- Có stale data/eventual consistency không?
- Có auth/authorization riêng không?
- Có cần sanitize/log masking không?

## 6. Local Docker setup

- Folder lab nằm ở đâu?
- Lệnh start/stop/status.
- Cách kiểm tra service sống.
- Cách cleanup an toàn.

## 7. Code skeleton

- Files/classes cần tạo.
- TODO nào để người học tự code.
- Phần nào Codex có thể scaffold cơ học.
- Phần nào nên tự implement để hiểu.

## 8. Verification

- Command compile/test.
- HTTP/curl/.http request.
- Expected behavior định tính.
- Negative cases.
- Cách chứng minh tenant isolation nếu có.

## 9. Common mistakes

- Các lỗi beginner dễ mắc.
- Các over-engineering cần tránh.
- Các production caveat cần ghi rõ.

## 10. Mentor-facing summary

- 3-5 bullet công nghệ này làm gì.
- Mini-lab đã chứng minh gì.
- File/code nào làm evidence.
- Giới hạn hiện tại.
- Next step nếu tiếp tục sau Phase 1.
