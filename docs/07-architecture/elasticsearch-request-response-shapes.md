# Elasticsearch request/response shapes cho mini-lab

## Vai trò của file này

File này giúp đọc đúng input/output khi làm mini-lab Elasticsearch cho `master_data`.

Nó không giải thích lại toàn bộ lý thuyết search engine. Đọc thêm:

- `docs/07-architecture/elasticsearch-search-service.md`
- `docs/07-architecture/elasticsearch-code-guide-spring-boot.md`

## 1. Document shape

Document đưa vào Elasticsearch là projection từ PostgreSQL entity:

```json
{
  "id": 101,
  "tenantId": 1,
  "code": "LAPTOP_DELL",
  "name": "Laptop Dell",
  "category": "DEVICE",
  "active": true
}
```

Rule:

- `id` là id gốc từ PostgreSQL để trace về source of truth.
- `tenantId` bắt buộc có trong mọi document.
- `active` giúp filter dữ liệu còn hiệu lực.
- Không đưa secret/token/request body thô vào document search.

## 2. Single index request

HTTP shape chuẩn:

```http
PUT /master_data_search/_doc/101
Content-Type: application/json

{
  "id": 101,
  "tenantId": 1,
  "code": "LAPTOP_DELL",
  "name": "Laptop Dell",
  "category": "DEVICE",
  "active": true
}
```

Java API Client shape:

```java
client.index(i -> i
    .index("master_data_search")
    .id("101")
    .document(document)
);
```

## 3. Bulk/reindex request

HTTP `_bulk` không nhận JSON array thông thường. Nó dùng NDJSON: mỗi operation metadata một dòng, document một dòng.

```http
POST /master_data_search/_bulk
Content-Type: application/x-ndjson

{ "index": { "_id": "101" } }
{ "id": 101, "tenantId": 1, "code": "LAPTOP_DELL", "name": "Laptop Dell", "category": "DEVICE", "active": true }
{ "index": { "_id": "201" } }
{ "id": 201, "tenantId": 2, "code": "LAPTOP_HP", "name": "Laptop HP", "category": "DEVICE", "active": true }
```

Java API Client giúp tránh tự ghép NDJSON:

```java
client.bulk(b -> b.operations(op -> op.index(idx -> idx
    .index("master_data_search")
    .id(String.valueOf(document.id()))
    .document(document)
)));
```

## 4. Search request

Search tenant-aware cần bool query:

```http
POST /master_data_search/_search
Content-Type: application/json

{
  "query": {
    "bool": {
      "must": [
        {
          "multi_match": {
            "query": "Laptop",
            "fields": ["code", "name", "category"]
          }
        }
      ],
      "filter": [
        { "term": { "tenantId": 1 } },
        { "term": { "active": true } }
      ]
    }
  }
}
```

Điểm quan trọng:

- `must` là phần keyword search.
- `filter` là phần bắt buộc không scoring, dùng cho `tenantId` và `active`.
- `tenantId` phải lấy từ `TenantContext`, không lấy từ request param/body.

## 5. Search response/hits shape

Elasticsearch response có nhiều metadata. API của mình không nên trả raw response trực tiếp cho client.

Shape rút gọn:

```json
{
  "hits": {
    "hits": [
      {
        "_index": "master_data_search",
        "_id": "101",
        "_score": 1.2,
        "_source": {
          "id": 101,
          "tenantId": 1,
          "code": "LAPTOP_DELL",
          "name": "Laptop Dell",
          "category": "DEVICE",
          "active": true
        }
      }
    ]
  }
}
```

API response trong lab chỉ nên trả safe fields:

```json
[
  {
    "id": 101,
    "tenantId": 1,
    "code": "LAPTOP_DELL",
    "name": "Laptop Dell",
    "category": "DEVICE",
    "active": true
  }
]
```

## 6. Common mistakes khi tự viết JSON DSL

- Bulk request viết thành JSON array thay vì NDJSON.
- Search query viết `"keyword": "Laptop"` nhưng Elasticsearch cần query type như `match`, `multi_match`, `term`, `bool`.
- Đặt `tenantId` trong `must` text search thay vì `filter`.
- Query thiếu `tenantId` filter.
- Trả raw Elasticsearch response ra API client.
- Nghĩ `_score` là authorization. Score chỉ là relevance, không phải quyền truy cập.
- Quên refresh/reindex trong local lab rồi tưởng Elasticsearch không lưu data.

## 7. URI hay dùng

| URI | Mục đích | Java API Client method |
|---|---|---|
| `PUT /{index}` | tạo index | `client.indices().create(...)` |
| `GET /{index}/_mapping` | xem mapping | `client.indices().getMapping(...)` |
| `PUT /{index}/_doc/{id}` | index/upsert một document | `client.index(...)` |
| `POST /{index}/_bulk` | bulk index nhiều document | `client.bulk(...)` |
| `POST /{index}/_search` | search document | `client.search(...)` |
| `POST /{index}/_refresh` | refresh để local lab thấy kết quả ngay | `client.indices().refresh(...)` |
| `DELETE /{index}` | xóa index local lab | `client.indices().delete(...)` |

## Nguồn tham khảo chuẩn

- [Elastic Java API Client - Getting started](https://www.elastic.co/docs/reference/elasticsearch/clients/java/getting-started)
- [Elastic Java API Client - Bulk indexing](https://www.elastic.co/docs/reference/elasticsearch/clients/java/usage/indexing-bulk)
- [Elastic Java API Client - Searching documents](https://www.elastic.co/docs/reference/elasticsearch/clients/java/usage/searching)
- [Elastic Docs - Query DSL bool query](https://www.elastic.co/docs/reference/query-languages/query-dsl/query-dsl-bool-query)
