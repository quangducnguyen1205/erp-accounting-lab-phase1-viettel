# Elasticsearch request / response / error shapes

Tài liệu này là phần tra cứu hình dạng API của Elasticsearch.
Nó giúp mình hiểu Elasticsearch nhận request gì, trả response gì, và vì sao code Java phải parse/search hits như hiện tại.

Đọc trước phần foundation: [elasticsearch-search-service.md](./elasticsearch-search-service.md)

## 1. Quy ước ví dụ

Các ví dụ dùng index giả định:

```text
products_search
```

Trong repo hiện tại, index tương ứng là:

```text
master_data_search
```

Các field ví dụ:

```json
{
  "id": 101,
  "tenantId": 1,
  "code": "PRD-001",
  "name": "Laptop Dell Latitude",
  "category": "ELECTRONICS",
  "active": true
}
```

## 2. URI thường gặp

| Mục đích | HTTP API | Ý nghĩa |
|---|---|---|
| Kiểm tra cluster | `GET /` | xem Elasticsearch có chạy không |
| Liệt kê index | `GET /_cat/indices?v` | xem index nào đang tồn tại |
| Tạo index | `PUT /{index}` | tạo index và mapping/settings |
| Xem mapping | `GET /{index}/_mapping` | xem field type |
| Index một document | `PUT /{index}/_doc/{id}` | tạo/cập nhật một document |
| Bulk index | `POST /{index}/_bulk` | nạp nhiều document bằng NDJSON |
| Search | `POST /{index}/_search` | query document |
| Xóa index | `DELETE /{index}` | xóa toàn bộ index |

Trong Java API Client, mình không tự ghép URI trực tiếp ở code chính, nhưng vẫn cần hiểu các URI này để debug/curl.

## 3. `GET /` - kiểm tra Elasticsearch đang chạy

Request:

```bash
curl http://localhost:9200
```

Response thường có dạng:

```json
{
  "name": "local-es",
  "cluster_name": "docker-cluster",
  "cluster_uuid": "...",
  "version": {
    "number": "8.x.x"
  },
  "tagline": "You Know, for Search"
}
```

Cần nhìn:

- có response JSON hay không;
- version;
- nếu connection refused thì Elasticsearch chưa chạy hoặc sai port.

## 4. `GET /_cat/indices?v` - xem index

Request:

```bash
curl "http://localhost:9200/_cat/indices?v"
```

Response là text dạng bảng, không phải JSON:

```text
health status index           uuid pri rep docs.count docs.deleted store.size pri.store.size
yellow open   products_search ...  1   1   120        0            50kb       50kb
```

Cần nhìn:

- index có tồn tại không;
- `docs.count` có dữ liệu chưa;
- `health` local một node có thể `yellow` nếu có replica nhưng không có node khác.

## 5. `PUT /{index}` - tạo index và mapping

Request ví dụ:

```json
PUT /products_search
{
  "mappings": {
    "properties": {
      "id": { "type": "long" },
      "tenantId": { "type": "long" },
      "code": { "type": "keyword" },
      "name": { "type": "text" },
      "category": { "type": "keyword" },
      "active": { "type": "boolean" }
    }
  }
}
```

Response tạo index thường có:

```json
{
  "acknowledged": true,
  "shards_acknowledged": true,
  "index": "products_search"
}
```

Ý nghĩa:

- `acknowledged`: cluster nhận thao tác tạo index;
- `shards_acknowledged`: shard được tạo/acknowledge;
- `index`: tên index vừa tạo.

Trong lab, nếu index đã tồn tại, tạo lại có thể lỗi `resource_already_exists_exception`.

## 6. `GET /{index}/_mapping` - xem mapping

Request:

```bash
curl http://localhost:9200/products_search/_mapping
```

Response rút gọn:

```json
{
  "products_search": {
    "mappings": {
      "properties": {
        "code": { "type": "keyword" },
        "name": { "type": "text" },
        "tenantId": { "type": "long" }
      }
    }
  }
}
```

Cần nhìn:

- field dùng để filter exact có phải `keyword`/numeric không;
- field dùng full-text có phải `text` không;
- type có bị dynamic mapping đoán sai không.

## 7. `PUT /{index}/_doc/{id}` - index một document

Request:

```json
PUT /products_search/_doc/101
{
  "id": 101,
  "tenantId": 1,
  "code": "PRD-001",
  "name": "Laptop Dell Latitude",
  "category": "ELECTRONICS",
  "active": true
}
```

Response thường có:

```json
{
  "_index": "products_search",
  "_id": "101",
  "_version": 1,
  "result": "created",
  "_shards": {
    "total": 2,
    "successful": 1,
    "failed": 0
  },
  "_seq_no": 0,
  "_primary_term": 1
}
```

Ý nghĩa beginner-level:

- `_index`: index chứa document;
- `_id`: id document trong Elasticsearch;
- `_version`: version nội bộ của document;
- `result`: `created`, `updated`, hoặc `deleted`;
- `_shards`: thao tác ghi thành công/thất bại trên shard;
- `_seq_no`, `_primary_term`: metadata nội bộ để kiểm soát concurrency, thường chưa cần đào sâu ở Phase 1.

## 8. `POST /{index}/_bulk` - bulk indexing

Bulk API dùng NDJSON, mỗi dòng là một JSON, và phải có newline cuối.

Request dạng thô:

```text
POST /products_search/_bulk
{ "index": { "_id": "101" } }
{ "id": 101, "tenantId": 1, "code": "PRD-001", "name": "Laptop Dell Latitude", "category": "ELECTRONICS", "active": true }
{ "index": { "_id": "102" } }
{ "id": 102, "tenantId": 1, "code": "PRD-002", "name": "Wireless Mouse", "category": "ELECTRONICS", "active": true }
```

Điểm dễ sai:

- Bulk không phải JSON array.
- Mỗi action line đi kèm một source line.
- Thiếu newline cuối có thể gây lỗi.
- Raw HTTP bulk dễ sai format hơn Java API Client.

Response rút gọn:

```json
{
  "took": 12,
  "errors": false,
  "items": [
    {
      "index": {
        "_index": "products_search",
        "_id": "101",
        "status": 201,
        "result": "created"
      }
    }
  ]
}
```

Cần nhìn:

- `errors` có `false` không;
- từng item có `status` thành công không;
- nếu `errors: true`, phải xem item nào lỗi.

## 9. `POST /{index}/_search` - search

Request tenant-aware ví dụ:

```json
POST /products_search/_search
{
  "query": {
    "bool": {
      "filter": [
        { "term": { "tenantId": 1 } },
        { "term": { "active": true } }
      ],
      "must": [
        {
          "multi_match": {
            "query": "laptop",
            "fields": ["code", "name", "category"]
          }
        }
      ]
    }
  },
  "size": 20
}
```

Response quan trọng:

```json
{
  "took": 5,
  "timed_out": false,
  "_shards": {
    "total": 1,
    "successful": 1,
    "skipped": 0,
    "failed": 0
  },
  "hits": {
    "total": {
      "value": 1,
      "relation": "eq"
    },
    "max_score": 1.23,
    "hits": [
      {
        "_index": "products_search",
        "_id": "101",
        "_score": 1.23,
        "_source": {
          "id": 101,
          "tenantId": 1,
          "code": "PRD-001",
          "name": "Laptop Dell Latitude",
          "category": "ELECTRONICS",
          "active": true
        }
      }
    ]
  }
}
```

Ý nghĩa:

- `took`: thời gian Elasticsearch xử lý query, tính bằng millisecond;
- `timed_out`: query có timeout không;
- `_shards`: số shard tham gia query và trạng thái;
- `hits.total.value`: tổng số kết quả match;
- `hits.max_score`: điểm cao nhất;
- `hits.hits[]`: danh sách kết quả;
- `_score`: điểm liên quan của từng hit;
- `_source`: document gốc.

Backend nên parse `hits.hits[]` rồi map `_source` sang response DTO, không trả nguyên response này cho client.

## 10. `DELETE /{index}` - xóa index

Request:

```bash
curl -X DELETE http://localhost:9200/products_search
```

Response:

```json
{
  "acknowledged": true
}
```

Trong lab, xóa index có thể dùng để reset search projection.

Trong production, xóa index cần rất cẩn thận và thường đi qua quy trình migration/reindex rõ ràng.

## 11. Error response format

Lỗi Elasticsearch thường có dạng:

```json
{
  "error": {
    "root_cause": [
      {
        "type": "index_not_found_exception",
        "reason": "no such index [products_search]"
      }
    ],
    "type": "index_not_found_exception",
    "reason": "no such index [products_search]"
  },
  "status": 404
}
```

Field cần đọc:

- `error.type`: loại lỗi chính;
- `error.reason`: lý do dễ đọc;
- `error.root_cause[]`: nguyên nhân gốc, có thể nhiều item;
- `status`: HTTP status.

## 12. Lỗi thường gặp

### Index not found

Dạng lỗi:

```json
{
  "error": {
    "type": "index_not_found_exception",
    "reason": "no such index [products_search]"
  },
  "status": 404
}
```

Nguyên nhân:

- chưa tạo index;
- app đang trỏ sai index name;
- Elasticsearch volume vừa reset.

Backend nên:

- log index name và operation;
- trả lỗi an toàn kiểu search chưa sẵn sàng;
- không expose toàn bộ stack trace cho API client.

### Mapping conflict

Ví dụ: mapping đã định nghĩa `tenantId` là `long`, nhưng document gửi `"tenantId": "abc"`.

Dạng lỗi có thể là `mapper_parsing_exception` hoặc `illegal_argument_exception`.

Nguyên nhân:

- dynamic mapping đoán sai từ dữ liệu đầu tiên;
- document shape không ổn định;
- code mapper gửi sai type.

Backend nên:

- log document id và field lỗi;
- fix mapper/mapping;
- reindex nếu cần.

### Invalid Query DSL

Lỗi thường gặp:

- sai tên query;
- JSON sai cấu trúc;
- dùng `term`/`match` sai field type;
- thiếu object wrapper.

Dạng lỗi có thể là `parsing_exception`, `x_content_parse_exception`, hoặc `query_shard_exception`.

Backend nên:

- log query ở mức đủ debug, tránh log dữ liệu nhạy cảm;
- với code đã compile bằng Java API Client, lỗi DSL thô thường ít hơn raw JSON.

### Elasticsearch down / connection refused

Trường hợp này thường không phải JSON error từ Elasticsearch.

Triệu chứng:

- `curl http://localhost:9200` không kết nối được;
- app báo connection refused/timeout.

Backend nên:

- fail fast khi feature search bật mà Elasticsearch không truy cập được;
- nếu search là optional, có thể tắt bằng feature flag;
- không làm hỏng app-test khi search disabled.

### Security/auth mismatch

Nếu Elasticsearch bật security nhưng client không cấu hình auth, có thể gặp 401/403.

Trong lab hiện tại thường tắt security để đơn giản hóa local learning.

Production cần:

- HTTPS;
- auth rõ ràng;
- secret management;
- phân quyền index nếu cần.

## 13. Map API shape sang Java API Client

| HTTP API | Java API Client mental model |
|---|---|
| `PUT /{index}` | `client.indices().create(...)` |
| `GET /{index}/_mapping` | `client.indices().getMapping(...)` |
| `PUT /{index}/_doc/{id}` | `client.index(...)` |
| `POST /{index}/_bulk` | `client.bulk(...)` |
| `POST /{index}/_search` | `client.search(...)` |
| `DELETE /{index}` | `client.indices().delete(...)` |

Lý do repo này dùng Java API Client:

- request/response typed hơn raw JSON;
- vẫn gần với khái niệm Elasticsearch thật;
- giảm lỗi NDJSON/bool query/parse response;
- không cần thêm abstraction Spring Data Elasticsearch quá sớm.

## 14. Quy tắc trả response từ backend API

Không nên trả raw Elasticsearch response ra ngoài vì:

- response quá nhiều metadata;
- dễ lộ chi tiết index/internal field;
- API client bị phụ thuộc vào Elasticsearch;
- khó giữ contract ổn định.

Nên trả DTO đơn giản:

```json
[
  {
    "id": 101,
    "tenantId": 1,
    "code": "PRD-001",
    "name": "Laptop Dell Latitude",
    "category": "ELECTRONICS",
    "active": true
  }
]
```

Trong repo hiện tại, controller/search service chỉ nên trả field an toàn và vẫn giữ tenant isolation.

## 15. Common mistakes

- Nghĩ bulk body là JSON array.
- Quên newline cuối ở bulk NDJSON.
- Query thiếu `tenantId` filter.
- Dùng `match` cho field cần exact filter.
- Dùng `term` trên field `text` đã analyze rồi ngạc nhiên vì không match.
- Không xem `hits.total` mà chỉ nhìn response body dài.
- Log toàn bộ raw response/error chứa dữ liệu nhạy cảm.
- Trả raw Elasticsearch response cho frontend.

## Nguồn tham khảo chuẩn

- [Elasticsearch REST APIs](https://www.elastic.co/docs/api/doc/elasticsearch)
- [Elasticsearch Search API](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-search)
- [Elasticsearch Bulk API](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-bulk)
- [Elasticsearch Mapping APIs](https://www.elastic.co/docs/api/doc/elasticsearch/group/endpoint-indices)
- [Elasticsearch Java API Client](https://www.elastic.co/docs/reference/elasticsearch/clients/java)
