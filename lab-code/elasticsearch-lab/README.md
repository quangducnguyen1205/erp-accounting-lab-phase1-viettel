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

## Liên hệ với tenant-demo

`tenant-demo` mặc định không phụ thuộc Elasticsearch:

```text
APP_SEARCH_ENABLED=false
```

Khi tự code mini-lab, bật:

```text
APP_SEARCH_ENABLED=true
ELASTICSEARCH_URIS=http://localhost:9200
ELASTICSEARCH_MASTER_DATA_INDEX=master_data_search
```

Search endpoint phải luôn lấy `tenantId` từ `TenantContext`, không lấy từ request body.

