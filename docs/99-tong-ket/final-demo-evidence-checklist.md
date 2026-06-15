# Checklist bằng chứng demo

Checklist này dùng trong lúc live demo. Mục tiêu là biết nên mở gì, kiểm gì, và bằng chứng nào đủ để nói hệ thống đang chạy đúng.

## Startup

- [ ] `cd lab-code`
- [ ] `make up`
- [ ] `make status`
- [ ] Web UI reachable: `http://localhost:5173`
- [ ] Keycloak reachable: `http://localhost:18080`
- [ ] Kong reachable: `http://localhost:18000`
- [ ] Kafka UI reachable: `http://localhost:18082`
- [ ] Grafana Loki reachable: `http://localhost:13001`

## Auth

- [ ] UI login `tenant1-user/password`
- [ ] UI login `tenant2-user/password`
- [ ] Token không hiển thị full value trong UI
- [ ] No-token API trả `401`
- [ ] Viewer write action trả `403`
- [ ] `platform-admin` token có role `ADMIN` cho admin-only reindex

## Tenant isolation

- [ ] Tenant 1 tạo master data thành công
- [ ] Tenant 2 không thấy master data của tenant 1
- [ ] Tenant 1 file không visible/download được bởi tenant 2
- [ ] Tenant 1 audit/activity không visible bởi tenant 2
- [ ] Tenant 1 search result không visible bởi tenant 2

## Master Data

- [ ] List records
- [ ] Create record
- [ ] Update record
- [ ] Duplicate code trả `409`
- [ ] Deactivate/delete record
- [ ] Load by code trước khi deactivate
- [ ] UI hiển thị lỗi thân thiện, không lộ token

## Kafka

- [ ] Topic `master-data-events`
- [ ] Consumer group `audit-log-service`
- [ ] Consumer group `search-service`
- [ ] Lag `0` hoặc giải thích được nếu vừa có event mới
- [ ] Message key có tenant/master-data context

## Services

- [ ] `tenant-demo` health
- [ ] `audit-log-service` health
- [ ] `file-service` health
- [ ] `search-service` health
- [ ] Kong route health qua `http://localhost:18000/tenant-demo/actuator/health`

## Storage

- [ ] PostgreSQL là source of truth cho master data và metadata
- [ ] Redis cache optional cho get-by-code
- [ ] MinIO lưu binary object
- [ ] Elasticsearch lưu search projection
- [ ] Audit service lưu audit table/read model riêng

## Observability

- [ ] Loki có logs cho `tenant-demo`
- [ ] Loki có logs cho `audit-log-service`
- [ ] Loki có logs cho `file-service`
- [ ] Loki có logs cho `search-service`
- [ ] Loki có logs cho `kong-gateway`
- [ ] Query được `requestId`
- [ ] Query được demo code, ví dụ `DEMO-`
- [ ] Log có `durationMs` hoặc thông tin duration tương đương
- [ ] Happy path không có `ERROR`/`FATAL` lặp lại

## Admin operation

- [ ] No-token reindex -> `401`
- [ ] `tenant1-user` hoặc `tenant2-user` reindex -> `403`
- [ ] `platform-admin` reindex -> `200`
- [ ] Response có `tenantId`, `indexName`, `indexedCount`, `requestedAt`
- [ ] Search sau reindex trả dữ liệu tenant hiện tại

## Shutdown

- [ ] `make down`
- [ ] Nếu muốn dọn log: `make clean-logs`
- [ ] Không commit `logs/*.log`, `.pids/`, token, local HTTP env
