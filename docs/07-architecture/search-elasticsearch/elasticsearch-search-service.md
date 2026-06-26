# Elasticsearch search service - nền tảng cho backend

Tài liệu này là phần nền tảng chung, không chỉ dành cho bài lab `master_data`.
Mục tiêu là giúp mình hiểu Elasticsearch đủ để thiết kế search cho nhiều domain backend khác nhau như sản phẩm, hóa đơn, khách hàng, danh mục dùng chung.

Đọc tiếp theo:

- API/request/response cụ thể: [elasticsearch-request-response-shapes.md](./elasticsearch-request-response-shapes.md)
- Pattern code Spring Boot: [elasticsearch-code-guide-spring-boot.md](./elasticsearch-code-guide-spring-boot.md)
- Thiết kế class trong repo này: [elasticsearch-design-patterns-spring-boot.md](./elasticsearch-design-patterns-spring-boot.md)

## 1. Elasticsearch là gì?

Elasticsearch là một search engine phân tán, thường dùng để tìm kiếm nhanh trên dữ liệu dạng document.

Trong backend, Elasticsearch thường xuất hiện khi:

- cần tìm kiếm text linh hoạt hơn `LIKE`;
- cần search nhiều field cùng lúc;
- cần ranking kết quả theo độ liên quan;
- cần filter + full-text search trên lượng dữ liệu lớn;
- cần tách workload search ra khỏi database chính.

Elasticsearch có thể lưu document, nhưng trong phần lớn hệ thống nghiệp vụ, database như PostgreSQL vẫn là source of truth. Elasticsearch là search projection: bản sao được tối ưu cho tìm kiếm.

## 2. Elasticsearch khác gì PostgreSQL?

| Góc nhìn | PostgreSQL | Elasticsearch |
|---|---|---|
| Mô hình chính | Bảng, row, constraint, transaction | Index, document, mapping, inverted index |
| Mạnh ở | dữ liệu chuẩn, ràng buộc, transaction, exact lookup | full-text search, relevance, search nhiều field |
| Query thường gặp | SQL | JSON Query DSL |
| Consistency | mạnh hơn, transaction rõ ràng | thường eventual consistency với DB nguồn |
| Source of truth | thường là nguồn dữ liệu chính | thường là bản chiếu phục vụ search |

Ví dụ:

- Tìm `code = 'TAX001'` theo `tenant_id`: PostgreSQL là đủ.
- Tìm tên sản phẩm chứa nhiều từ, có ranking, typo nhẹ, nhiều field: Elasticsearch hợp lý hơn.

## 3. Các khái niệm lõi

### Cluster

Cluster là một nhóm Elasticsearch node cùng phục vụ dữ liệu và truy vấn.

Trong lab local có thể chỉ có một node, nhưng mental model production thường là nhiều node.

### Node

Node là một Elasticsearch server instance trong cluster.

Một node có thể giữ shard, xử lý query, indexing, cluster metadata.

### Index

Index trong Elasticsearch là nơi chứa các document cùng loại hoặc cùng mục đích search.

Ví dụ:

- `products_search`
- `invoices_search`
- `customers_search`
- `master_data_search`

Không nên hiểu Elasticsearch index giống hoàn toàn PostgreSQL index. Trong Elasticsearch, index gần giống một “collection” document có mapping riêng.

### Shard

Shard là phần chia nhỏ của index. Elasticsearch chia index thành shard để phân tán dữ liệu và query.

Ở mức beginner:

- shard giúp scale dữ liệu/query;
- quá nhiều shard có thể làm hệ thống nặng;
- lab local không cần tuning shard sâu.

### Replica

Replica là bản sao của shard.

Replica giúp:

- tăng khả năng chịu lỗi;
- có thể hỗ trợ đọc nhiều hơn.

Trong lab local một node, replica thường không có ý nghĩa nhiều vì replica cần node khác để thật sự hữu ích.

### Document

Document là một JSON object được lưu trong index.

Ví dụ document sản phẩm:

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

Một document thường là bản chiếu từ entity/row trong database, nhưng không bắt buộc giống 100%.

### `_id`

`_id` là định danh document trong Elasticsearch index.

Với dữ liệu đồng bộ từ PostgreSQL, thường dùng id nguồn làm `_id`, ví dụ `master_data.id`.

### `_source`

`_source` là JSON gốc của document mà Elasticsearch lưu lại.

Khi search, kết quả thường trả về `_source`. Backend không nên trả raw Elasticsearch response ra API trực tiếp, mà nên map sang DTO an toàn.

### Field

Field là thuộc tính trong document, ví dụ `tenantId`, `code`, `name`, `category`, `active`.

Thiết kế field quyết định query/filter có dễ và đúng hay không.

## 4. Mapping và field type

Mapping định nghĩa cách Elasticsearch hiểu các field trong document.

Có hai kiểu tư duy:

- dynamic mapping: Elasticsearch tự đoán type khi document được index;
- explicit mapping: mình khai báo type rõ ràng.

Trong backend nghiêm túc, explicit mapping thường dễ kiểm soát hơn.

### Field type thường gặp

| Type | Dùng cho | Ví dụ |
|---|---|---|
| `keyword` | exact match, filter, sort, aggregation | `code`, `category`, `tenantId` nếu lưu dạng chuỗi |
| `text` | full-text search, qua analyzer | `name`, `description`, `invoiceNote` |
| `long`, `integer`, `double` | số | `id`, `tenantId`, `amount` |
| `boolean` | true/false | `active` |
| `date` | thời gian | `createdAt`, `invoiceDate` |

Một field có thể có multi-field. Ví dụ `name` là `text` để search, đồng thời có `name.keyword` để exact/sort.

## 5. Analyzer là gì?

Analyzer là cách Elasticsearch biến text thành token để phục vụ full-text search.

Analyzer thường gồm:

- tokenizer: cắt text thành token;
- token filter: lowercase, bỏ stop words, stemming, v.v.

Ví dụ text:

```text
Laptop Dell Latitude
```

Có thể được phân tích thành token:

```text
laptop, dell, latitude
```

Điểm cần nhớ:

- `text` thường được analyze nên phù hợp full-text search;
- `keyword` không analyze theo kiểu full-text nên phù hợp exact match/filter;
- dùng sai type dễ dẫn đến query “không ra kết quả như tưởng tượng”.

## 6. Inverted index

Inverted index là cấu trúc giúp search nhanh theo từ khóa.

Thay vì quét từng document, Elasticsearch lưu kiểu:

```text
laptop   -> document 101, 205, 300
dell     -> document 101, 310
invoice  -> document 900, 901
```

Khi search `dell laptop`, Elasticsearch có thể tìm document liên quan nhanh hơn so với quét text thô.

Đây là lý do Elasticsearch mạnh hơn PostgreSQL `LIKE '%keyword%'` cho full-text search.

## 7. Query DSL mental model

Elasticsearch dùng JSON Query DSL để mô tả truy vấn.

Một query thường gồm:

- điều kiện full-text: `match`, `multi_match`;
- điều kiện exact/filter: `term`, `terms`, `range`;
- kết hợp điều kiện: `bool`;
- phân trang: `from`, `size`;
- sort nếu cần.

Ví dụ mental model:

```json
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
  }
}
```

### Query context vs filter context

Query context hỏi: document này liên quan đến keyword tới mức nào?

- có tính `_score`;
- dùng cho full-text search.

Filter context hỏi: document này có thỏa điều kiện không?

- không quan tâm `_score`;
- thường cache tốt hơn;
- phù hợp `tenantId`, `active`, `category`, date range.

Trong multi-tenant search, `tenantId` phải là filter bắt buộc.

### `_score`

`_score` là điểm liên quan của kết quả search.

Ở mức lab:

- `_score` giúp sắp xếp kết quả full-text;
- filter như `tenantId` không nên quyết định relevance;
- không cần tuning scoring sâu trong Phase 1.

## 8. Thiết kế search cho domain backend bất kỳ

Khi thêm search cho một domain, nên trả lời các câu hỏi:

1. Source of truth là bảng/entity nào?
2. Document search cần field nào?
3. Field nào là exact filter?
4. Field nào là full-text search?
5. Có `tenantId` hoặc ownership scope không?
6. API response trả field nào cho client?
7. Khi dữ liệu DB thay đổi thì index Elasticsearch được cập nhật thế nào?

Ví dụ domain:

| Domain | Exact filter | Full-text field | Ghi chú |
|---|---|---|---|
| Product | `tenantId`, `category`, `active` | `name`, `description`, `brand` | search sản phẩm theo keyword |
| Invoice | `tenantId`, `status`, `invoiceDate` | `invoiceNo`, `customerName`, `note` | vẫn cần DB cho tính tiền/transaction |
| Customer | `tenantId`, `customerType`, `active` | `name`, `phone`, `email`, `address` | cẩn thận dữ liệu cá nhân |
| Master data | `tenantId`, `category`, `active` | `code`, `name`, `category` | phù hợp mini-lab hiện tại |

## 9. Indexing, reindex và eventual consistency

Elasticsearch cần được nạp dữ liệu từ database hoặc event stream.

Các cách phổ biến:

- reindex thủ công từ DB sang Elasticsearch;
- cập nhật index khi service ghi dữ liệu;
- dùng message/event như Kafka;
- dùng CDC như Debezium để bắt thay đổi DB.

Trong Phase 1, reindex thủ công là đủ để học.

Điểm cần nhớ:

- PostgreSQL là source of truth;
- Elasticsearch có thể trễ so với DB;
- search result chỉ là projection;
- nghiệp vụ quan trọng vẫn nên kiểm tra lại quyền/dữ liệu ở backend nếu cần.

## 10. Tenant-aware search

Trong hệ thống shared-table multi-tenant, search cũng phải tenant-aware.

Quy tắc:

- mỗi document search phải có `tenantId`;
- mọi search query phải filter theo `tenantId`;
- `tenantId` lấy từ context đã xác thực, không lấy từ request body/query param tùy ý;
- kết quả search không được lộ document của tenant khác;
- index/search không thay thế authorization.

Nếu thiếu filter `tenantId`, Elasticsearch có thể leak dữ liệu còn nhanh hơn SQL vì search nhiều field rộng hơn.

## 11. Khi nào không nên dùng Elasticsearch?

Không nên thêm Elasticsearch chỉ vì “nghe chuyên nghiệp”.

PostgreSQL thường đủ khi:

- chỉ exact lookup theo id/code;
- dữ liệu nhỏ;
- filter đơn giản;
- yêu cầu transaction/consistency là trọng tâm;
- chưa có nhu cầu full-text/ranking thực sự.

Elasticsearch thêm chi phí:

- vận hành cluster;
- mapping/index lifecycle;
- đồng bộ dữ liệu;
- xử lý stale data;
- bảo vệ tenant filter ở thêm một lớp.

## 12. Áp dụng vào repo hiện tại

Trong repo này:

- `master_data` trong PostgreSQL vẫn là nguồn dữ liệu chính;
- Elasticsearch index `master_data_search` là projection phục vụ search;
- document gồm `id`, `tenantId`, `code`, `name`, `category`, `active`;
- API search lấy tenant từ `TenantContext`;
- query Elasticsearch bắt buộc filter `tenantId`;
- response API chỉ trả DTO an toàn, không trả raw Elasticsearch response.

Lab này không nhằm xây search service production hoàn chỉnh. Mục tiêu là hiểu vì sao một search engine được đưa vào kiến trúc, và cách giữ tenant isolation khi dùng nó.

## 13. Lỗi tư duy thường gặp

- Dùng Elasticsearch cho exact lookup đơn giản mà PostgreSQL làm tốt hơn.
- Quên `tenantId` trong document hoặc query filter.
- Trả raw Elasticsearch response cho API client.
- Nghĩ Elasticsearch là source of truth.
- Không hiểu `text` khác `keyword`.
- Dùng `LIKE '%abc%'` trong PostgreSQL rồi kết luận “DB chậm” mà chưa phân biệt query pattern.
- Đồng bộ DB sang search index nhưng không có kế hoạch reindex/repair khi sai dữ liệu.

## Nguồn tham khảo chuẩn

- [Elasticsearch Guide - Basic concepts](https://www.elastic.co/docs/get-started/the-stack)
- [Elasticsearch Reference - Mapping](https://www.elastic.co/docs/manage-data/data-store/mapping)
- [Elasticsearch Reference - Text analysis](https://www.elastic.co/docs/manage-data/data-store/text-analysis)
- [Elasticsearch Reference - Query DSL](https://www.elastic.co/docs/explore-analyze/query-filter/languages/querydsl)
- [Elasticsearch Reference - Search your data](https://www.elastic.co/docs/solutions/search)
