# Elasticsearch code guide cho Spring Boot tenant-demo

## Vai trò của file này

File này là **code guide** cho mini-lab Elasticsearch. Nó không giải thích lại toàn bộ lý thuyết search engine. Nếu cần phần "vì sao dùng Elasticsearch", đọc:

- `docs/07-architecture/elasticsearch-search-service.md`

Nếu cần lệnh chạy Docker/checklist lab, đọc:

- `docs/07-architecture/elasticsearch-mini-lab-plan.md`
- `lab-code/elasticsearch-lab/README.md`

Nếu bị rối input/output của Elasticsearch, đọc:

- `docs/07-architecture/elasticsearch-request-response-shapes.md`

Mục tiêu ở đây là biết nên code phần search trong Spring Boot theo hình dạng nào.

## Hướng tích hợp chuẩn ở mức Phase 1

Có hai hướng phổ biến:

| Hướng | Ý nghĩa | Khi nào dùng |
|---|---|---|
| Spring Data Elasticsearch | Dùng abstraction kiểu Spring: document mapping, `ElasticsearchOperations`, repository nếu cần. | Hợp với Spring Boot app, dễ giữ code theo style Spring. |
| Official Elasticsearch Java API Client | Dùng client chính thức của Elastic, request/response sát API Elasticsearch hơn. | Hợp khi muốn kiểm soát query/indexing rõ hoặc học trực tiếp Elasticsearch API. |

Trong repo này, hướng học hợp lý hiện tại là:

1. Dùng **official Elasticsearch Java API Client** cho phần gọi Elasticsearch.
2. Không tự ghép JSON bằng `RestClient` raw trong service.
3. Không trộn official client với Spring Data Elasticsearch trong cùng mini-lab.
4. Không dùng Elasticsearch repository generic để thay thế JPA repository của PostgreSQL.

Lý do chọn official Java API Client: request/response typed hơn raw HTTP, nhưng vẫn cho thấy rõ các API Elasticsearch như `index`, `bulk`, `search`, `indices`.

## Package shape trong repo

Skeleton hiện nằm ở:

```text
lab-code/tenant-demo/src/main/java/com/viettel/demo/search/
├── SearchProperties.java
├── MasterDataSearchDocument.java
├── MasterDataSearchGateway.java
├── MasterDataSearchIndexer.java
├── MasterDataSearchService.java
├── MasterDataSearchController.java
└── MasterDataSearchReindexResponse.java
```

Ý nghĩa:

| Class | Vai trò |
|---|---|
| `SearchProperties` | Đọc config `app.search.*`: bật/tắt search, Elasticsearch URI, index name. |
| `MasterDataSearchDocument` | Projection/document đưa sang Elasticsearch, khác với JPA entity. |
| `MasterDataSearchGateway` | Adapter duy nhất gọi Elasticsearch Java API Client. |
| `MasterDataSearchIndexer` | Chuyển dữ liệu từ PostgreSQL sang Elasticsearch. |
| `MasterDataSearchService` | Tạo search query tenant-aware. |
| `MasterDataSearchController` | Endpoint lab để gọi search, chỉ active khi `APP_SEARCH_ENABLED=true`. |
| `MasterDataSearchReindexResponse` | Response nhỏ cho endpoint reindex, không trả raw bulk response. |

## Vì sao document không nên dùng trực tiếp JPA entity?

`MasterData` là entity của PostgreSQL/JPA:

- phục vụ transaction database;
- mapping theo Flyway schema;
- dùng trong repository/service nghiệp vụ.

`MasterDataSearchDocument` là document cho search:

- có field phục vụ tìm kiếm;
- có thể denormalize dữ liệu;
- có thể stale so với DB trong một khoảng ngắn;
- bắt buộc có `tenantId`.

Không nên ép một class vừa làm JPA entity vừa làm Elasticsearch document trong lab này, vì nó dễ làm lẫn trách nhiệm.

## Config/env

Config hiện tại:

```yaml
app:
  search:
    enabled: ${APP_SEARCH_ENABLED:false}
    elasticsearch-uris: ${ELASTICSEARCH_URIS:http://localhost:9200}
    master-data-index: ${ELASTICSEARCH_MASTER_DATA_INDEX:master_data_search}
```

Rule:

- `APP_SEARCH_ENABLED=false` mặc định để app/test không phụ thuộc Elasticsearch.
- Chỉ bật `true` khi đang chạy mini-lab.
- Không để search endpoint active nếu Elasticsearch chưa sẵn sàng.

## Indexing flow

Phase 1 nên dùng flow đơn giản:

```text
MasterDataRepository
-> lấy dữ liệu từ PostgreSQL source of truth
-> MasterDataSearchDocument.fromEntity(...)
-> MasterDataSearchIndexer index vào Elasticsearch
```

Gợi ý cách tự code:

1. Thêm dependency/client đã chọn.
2. Tạo index nếu chưa tồn tại.
3. Index từng document hoặc bulk index.
4. Log số document đã index, không log token/secret.
5. Nếu update/delete `MasterData`, ghi chú rằng search index cần update/delete tương ứng.

Không overdo:

- chưa cần Kafka/Debezium sync;
- chưa cần scheduled job phức tạp;
- chưa cần analyzer tiếng Việt;
- chưa cần production index lifecycle.

## Standard Elasticsearch URI/API map

| HTTP API | Dùng để | Java API Client tương ứng |
|---|---|---|
| `PUT /{index}` | tạo index nếu chưa có | `client.indices().create(...)` |
| `GET /{index}/_mapping` | xem mapping hiện tại | `client.indices().getMapping(...)` |
| `PUT /{index}/_doc/{id}` | index/upsert một document | `client.index(...)` |
| `POST /{index}/_bulk` | reindex/bulk index nhiều document | `client.bulk(...)` |
| `POST /{index}/_search` | search document | `client.search(...)` |
| `POST /{index}/_refresh` | refresh local lab để thấy document ngay | `client.indices().refresh(...)` |
| `DELETE /{index}` | xóa index local lab | `client.indices().delete(...)` |

Trong code hiện tại, các API này được gom vào `MasterDataSearchGateway`.

## Request/response shape chuẩn

Document:

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

Search query shape:

```text
bool query
├── must: multi_match(keyword) trên code/name/category
└── filter: tenantId = current tenant, active = true
```

API response của `tenant-demo` chỉ trả safe fields từ `MasterDataSearchDocument`, không trả raw Elasticsearch response có `_index`, `_score`, shard metadata.

Chi tiết hơn: `docs/07-architecture/elasticsearch-request-response-shapes.md`.

## Search flow tenant-aware

Search service phải đi theo flow:

```text
JwtTenantContextFilter
-> TenantContext có tenantId đã validate
-> MasterDataSearchController
-> MasterDataSearchService.search(keyword)
-> Elasticsearch query có filter tenantId
```

Query shape cần nhớ:

```text
bool query
├── must/should: keyword search trên code/name/category
└── filter: tenantId = current tenant, active = true
```

Rule quan trọng:

- Không nhận `tenantId` từ query param/body.
- Không search toàn index rồi filter sau trong Java.
- Không dùng search result như bằng chứng authorization cuối cùng cho write/update/delete.

## Controller/API shape

Endpoint học tập đủ nhỏ:

```text
GET /api/search/master-data?keyword=Laptop
Authorization: Bearer <token>
```

Response có thể là list `MasterDataSearchDocument` hoặc DTO riêng. Với Phase 1, document DTO là đủ nếu không lộ field nhạy cảm.

Optional local-only endpoint:

```text
POST /api/search/master-data/reindex
```

Nếu tạo endpoint reindex:

- chỉ active khi `APP_SEARCH_ENABLED=true`;
- ghi rõ local/admin lab only;
- không public production;
- không cần phân quyền phức tạp trong Phase 1.

## Test/verification style

Tối thiểu manual verification:

1. `make db-up`
2. `make elastic-up`
3. Chạy app với `APP_SEARCH_ENABLED=true`.
4. Reindex `master_data`.
5. Search bằng token tenant 1.
6. Search cùng keyword bằng token tenant 2.
7. Verify không có document cross-tenant.

Automated test có thể để sau vì Elasticsearch container làm test nặng hơn. Nếu viết test sau này, nên tách profile hoặc dùng Testcontainers, không làm `DataLeakageTest` hiện tại phụ thuộc Elasticsearch.

## Common mistakes

- Bật search mặc định làm app-test fail khi Elasticsearch chưa chạy.
- Dùng Elasticsearch làm source of truth cho data nghiệp vụ.
- Quên field `tenantId` trong document.
- Quên filter `tenantId` trong query.
- Dùng `tenantId` từ request param.
- Tự viết bulk body như JSON thường; `_bulk` dùng NDJSON nếu đi raw HTTP.
- Tự viết query JSON sai DSL, ví dụ đặt `"keyword": "Laptop"` trực tiếp thay vì `multi_match`.
- Reindex endpoint public quá rộng.
- Đánh giá search consistency như PostgreSQL transaction.
- Thêm Kafka/Debezium/analyzer phức tạp trước khi search flow cơ bản chạy.

## Done criteria cho code mini-lab

- App vẫn chạy/test bình thường khi `APP_SEARCH_ENABLED=false`.
- Elasticsearch local chạy được bằng `make elastic-up`.
- Có cách reindex dữ liệu `master_data` vào `master_data_search`.
- Search keyword trả kết quả tenant-aware.
- Tenant 1 không thấy document tenant 2 và ngược lại.
- Ghi được caveat: PostgreSQL là source of truth, Elasticsearch là search projection eventual consistency.

## Nguồn tham khảo chuẩn

- [Spring Boot - Elasticsearch support](https://docs.spring.io/spring-boot/reference/data/nosql.html#data.nosql.elasticsearch)
- [Spring Data Elasticsearch Reference](https://docs.spring.io/spring-data/elasticsearch/reference/)
- [Elastic Java API Client - Usage](https://www.elastic.co/docs/reference/elasticsearch/clients/java/usage)
- [Elastic Java API Client - Indexing documents](https://www.elastic.co/docs/reference/elasticsearch/clients/java/usage/indexing)
- [Elastic Java API Client - Bulk indexing](https://www.elastic.co/docs/reference/elasticsearch/clients/java/usage/indexing-bulk)
- [Elastic Java API Client - Searching documents](https://www.elastic.co/docs/reference/elasticsearch/clients/java/usage/searching)
