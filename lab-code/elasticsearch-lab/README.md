# Elasticsearch mini-lab

## Mục tiêu

Folder này chạy Elasticsearch local để học search service ở mức nhỏ, nối tiếp bài PostgreSQL index/query-pattern.

Không dùng setup này cho production.

## Chạy Elasticsearch

```bash
cd lab-code/elasticsearch-lab
docker compose up -d
```

Kiểm tra:

```bash
curl http://localhost:9200
```

Kỳ vọng: Elasticsearch trả JSON thông tin cluster/node.

## Dừng lab

```bash
docker compose down
```

Nếu muốn xóa volume local:

```bash
docker compose down -v
```

Chỉ xóa volume khi chắc chắn không cần dữ liệu lab nữa.

## Lưu ý bảo mật

`xpack.security.enabled=false` chỉ để giảm độ phức tạp local learning. Production phải có authentication, authorization, TLS/network boundary và quản lý user/role rõ ràng.

## Liên hệ với Phase 1.5 search-service

Phase 1 mini-lab từng đặt search code trong `tenant-demo` sau một feature flag. Runtime demo hiện tại đã tách search sang service riêng:

```text
lab-code/search-service
```

Chạy Elasticsearch local rồi chạy search-service:

```bash
cd lab-code
make -f Makefile.legacy elastic-up
make -f Makefile.legacy search-run-logs
```

Search endpoint vẫn phải luôn lấy `tenantId` từ JWT/TenantContext, không lấy từ request body. `tenant-demo` chỉ publish `MasterDataChangedEvent`; `search-service` consume event và update Elasticsearch projection.
