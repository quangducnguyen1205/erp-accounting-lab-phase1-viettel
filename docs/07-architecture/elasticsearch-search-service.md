# Elasticsearch / search service trong kiến trúc SaaS multi-tenant

## Mục tiêu

Tài liệu này nối từ bài PostgreSQL query pattern sang câu hỏi: khi nào cần search engine như Elasticsearch?

Phạm vi hiện tại:

- chỉ tìm kiếm trên lát cắt `master_data`;
- giữ tenant isolation;
- chưa làm production search service;
- chưa học sâu analyzer/shard/cluster tuning.

## Elasticsearch là gì?

Elasticsearch là search engine/document store phục vụ tìm kiếm và phân tích dữ liệu theo document. Nó thường được dùng khi cần:

- full-text search;
- search nhiều field;
- ranking/relevance;
- fuzzy/contains search;
- filter + search phức tạp;
- search service tách khỏi database nghiệp vụ.

Trong kiến trúc target, Elasticsearch/Search service thường không phải source of truth. Source of truth vẫn là database nghiệp vụ như PostgreSQL. Elasticsearch là index phục vụ tìm kiếm nhanh và linh hoạt hơn.

## Khác gì PostgreSQL query/index?

| Câu hỏi | PostgreSQL | Elasticsearch |
|---|---|---|
| Source of truth | Có, lưu transaction/business data chính. | Thường không; là search index/copy phục vụ tìm kiếm. |
| Dữ liệu chính | Row/table/constraint/transaction. | Document/index/query DSL. |
| Tối ưu mạnh | Exact lookup, relational query, constraint, transaction. | Full-text search, relevance, fuzzy/multi-field search. |
| Consistency | Mạnh hơn trong transaction database. | Near real-time, có độ trễ index/search. |
| Multi-tenant risk | Query thiếu `tenant_id` có thể leak data. | Search query thiếu tenant filter cũng leak data. |

## Khi nào PostgreSQL là đủ?

PostgreSQL thường đủ nếu:

- tìm chính xác theo `tenant_id + code`;
- lọc theo category/status đơn giản;
- prefix search nhỏ như `name LIKE 'Lap%'` đã có index phù hợp;
- dữ liệu ít;
- cần transaction/constraint mạnh;
- search không phải feature chính.

Ví dụ trong repo:

```sql
WHERE tenant_id = ? AND code = ?
WHERE tenant_id = ? AND category = ?
WHERE tenant_id = ? AND name LIKE 'Laptop%'
```

Các pattern này đã được học ở `docs/03-backend-database-mo-rong/index-query-patterns-postgresql.md`.

## Khi nào nên cân nhắc Elasticsearch?

Cân nhắc Elasticsearch khi:

- user cần tìm chứa từ khóa tự do, không chỉ prefix;
- search trên nhiều field: `code`, `name`, `category`, mô tả, metadata;
- cần relevance/ranking;
- cần fuzzy search hoặc analyzer theo ngôn ngữ;
- PostgreSQL `LIKE '%keyword%'`/trigram bắt đầu không đủ hoặc khó scale;
- muốn tách read/search workload khỏi database giao dịch.

Không nên dùng Elasticsearch cho exact lookup đơn giản như `tenant_id + id` hoặc `tenant_id + code`. Exact lookup đó nên đi PostgreSQL.

## Inverted index ở mức beginner

Database B-tree index thường giống mục lục theo thứ tự giá trị cột. Search engine thường dùng inverted index: ánh xạ từ term/từ khóa về danh sách document chứa term đó.

Ví dụ:

```text
Document 101: "Laptop Dell"
Document 201: "Laptop HP"

Inverted index:
laptop -> [101, 201]
dell   -> [101]
hp     -> [201]
```

Khi search `laptop`, Elasticsearch không scan từng document từ đầu. Nó tra inverted index để tìm candidate documents, sau đó tính score/relevance nếu cần.

## Document vs row

Trong PostgreSQL, `master_data` là row trong table:

```text
id | tenant_id | code | name | category | is_active
```

Trong Elasticsearch, dữ liệu thường được index thành JSON document:

```json
{
  "id": 101,
  "tenantId": 1,
  "code": "LAPTOP-01",
  "name": "Laptop Dell",
  "category": "ASSET",
  "active": true
}
```

Document này là bản sao phục vụ search. Khi PostgreSQL thay đổi, search index cần được update/reindex theo một cơ chế nào đó.

## Indexing data từ DB sang Elasticsearch

Flow đơn giản nhất cho mini-lab:

```text
PostgreSQL master_data
-> Spring Boot đọc rows tenant-aware hoặc admin job đọc toàn bộ
-> chuyển thành MasterDataSearchDocument
-> index vào Elasticsearch
-> search API query Elasticsearch với tenant filter
```

Trong production, data sync có thể dùng:

- application event sau khi create/update/delete;
- scheduled reindex job;
- outbox pattern;
- Debezium CDC + Kafka;
- search service riêng consume event.

Phase 1 chỉ cần hiểu: DB là source of truth, Elasticsearch là search projection.

## Search query vs database query

Database query thường hỏi: “row nào thỏa điều kiện chính xác?”

Search query thường hỏi: “document nào liên quan nhất với keyword này, trong phạm vi filter này?”

Ví dụ search tenant-aware:

```json
{
  "query": {
    "bool": {
      "must": [
        { "multi_match": { "query": "laptop", "fields": ["code", "name", "category"] } }
      ],
      "filter": [
        { "term": { "tenantId": 1 } },
        { "term": { "active": true } }
      ]
    }
  }
}
```

Trong query này:

- `must` là phần search keyword/relevance;
- `filter` là phần bắt buộc, không dùng để score;
- `tenantId` phải luôn nằm trong filter.

## Eventual consistency

Elasticsearch là near real-time search. Sau khi index document, document có thể chưa searchable ngay lập tức trong một khoảng ngắn do refresh interval.

Điều này dẫn tới eventual consistency:

```text
PostgreSQL đã update
-> Elasticsearch chưa kịp update hoặc refresh
-> search result có thể hơi trễ
```

Vì vậy:

- các action cần dữ liệu chính xác tức thì nên đọc PostgreSQL;
- search result có thể chấp nhận độ trễ nhỏ;
- cần có cơ chế reindex/retry/monitor khi sync lỗi.

## Tenant-aware search

Rule quan trọng nhất:

1. Mỗi search document phải có `tenantId`.
2. Mỗi search query phải filter theo `tenantId`.
3. Không lấy `tenantId` từ request body tùy ý; lấy từ `TenantContext` sau auth.
4. Search result chỉ là candidate; với dữ liệu nhạy cảm, backend vẫn có thể re-check PostgreSQL khi cần.

Sai nguy hiểm:

```json
{
  "query": {
    "multi_match": {
      "query": "Laptop",
      "fields": ["code", "name"]
    }
  }
}
```

Query này thiếu tenant filter nên có thể trả dữ liệu nhiều tenant.

Đúng hơn:

```json
{
  "query": {
    "bool": {
      "must": [{ "multi_match": { "query": "Laptop", "fields": ["code", "name"] } }],
      "filter": [{ "term": { "tenantId": 1 } }]
    }
  }
}
```

## Common risks

| Risk | Vì sao nguy hiểm | Rule học được |
|---|---|---|
| Search query thiếu tenant filter | Lộ kết quả tenant khác. | Luôn filter `tenantId` từ trusted context. |
| DB và search index lệch nhau | User thấy data cũ/mất data trên search. | Cần reindex/sync strategy. |
| Dùng Elasticsearch cho exact lookup | Tăng phức tạp không cần thiết. | `id`, `code` exact vẫn nên ưu tiên DB. |
| Tin search result như authorization | Search index có thể stale. | Backend authorization/repository vẫn cần tenant-aware. |
| Bật search endpoint khi Elastic chưa sẵn sàng | App/demo dễ fail. | Feature flag `APP_SEARCH_ENABLED=false` mặc định. |

## Điều cần nhớ

- Elasticsearch là search projection, không phải replacement cho PostgreSQL.
- Inverted index giúp full-text search tốt hơn B-tree cho nhiều use case.
- Multi-tenant search vẫn cần tenant filter ở mọi query.
- Search index có eventual consistency với DB.
- Mini-lab chỉ nên index/search `master_data`, không mở rộng thành search platform.

## Nguồn tham khảo chuẩn

- [Elastic Docs - Index and search basics](https://www.elastic.co/docs/solutions/search/get-started/index-basics)
- [Elastic Docs - Query DSL](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-filters.html)
- [Elastic Docs - Boolean query](https://www.elastic.co/docs/reference/query-languages/query-dsl/query-dsl-bool-query)
- [Elastic Docs - Near real-time search](https://www.elastic.co/docs/manage-data/data-store/near-real-time-search)
- [Elastic Docs - Security](https://www.elastic.co/docs/deploy-manage/security)
