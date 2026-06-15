# Elasticsearch code guide cho Spring Boot tenant-demo

> Ghi chú Phase 1.5: file này mô tả mini-lab cũ nhúng search vào `tenant-demo`. Runtime hiện tại của product demo đã chuyển sang `lab-code/search-service`; đọc [search-service-split-plan.md](./search-service-split-plan.md) và [cross-service-search-projection.md](./cross-service-search-projection.md) cho đường chạy hiện hành.

## Vai trò của file này

File này là **code guide** cho mini-lab Elasticsearch. Nó không giải thích lại toàn bộ lý thuyết search engine hay toàn bộ REST API shape. Nếu cần phần foundation và input/output, đọc trước:

- `docs/07-architecture/search-elasticsearch/elasticsearch-search-service.md`
- `docs/07-architecture/search-elasticsearch/elasticsearch-request-response-shapes.md`

Nếu cần lệnh chạy Docker/checklist lab, đọc:

- `docs/07-architecture/search-elasticsearch/elasticsearch-mini-lab-plan.md`
- `lab-code/elasticsearch-lab/README.md`

Nếu muốn hiểu vì sao code tách thành Controller/Service/Gateway/Document, đọc:

- `docs/07-architecture/search-elasticsearch/elasticsearch-design-patterns-spring-boot.md`

Mục tiêu ở đây là biết nên code phần search trong Spring Boot theo hình dạng nào.

## Hướng tích hợp chuẩn ở mức Phase 1

Có ba hướng phổ biến:

| Hướng | Nó là gì | Ưu điểm | Nhược điểm | Khi nào dùng |
|---|---|---|---|---|
| Raw HTTP / Spring `RestClient` | Tự gọi REST API Elasticsearch bằng URL + JSON body. | Gần REST API nhất, tốt để học URI/Query DSL/debug bằng curl. | Dễ sai bulk NDJSON, query shape, response parsing; service dễ bị lẫn hạ tầng. | Dùng để học request shape hoặc debug, không chọn làm implementation chính trong lab này. |
| Official Elasticsearch Java API Client | Client chính thức của Elastic, request/response typed và sát API Elasticsearch. | Tránh tự ghép JSON thủ công, vẫn thấy rõ `index`, `bulk`, `search`. | Cú pháp builder/lambda hơi mới lúc đầu. | **Khuyến nghị cho repo này.** |
| Spring Data Elasticsearch / `ElasticsearchOperations` | Abstraction kiểu Spring cho document mapping/operations/repository-like flow. | Hợp app Spring lớn hơn, style quen thuộc với Spring. | Có thể che bớt chi tiết Elasticsearch; dễ nhầm với JPA repository khi mới học. | Để tham khảo hoặc học sau, chưa cần trộn vào mini-lab. |

Trong repo này, hướng học hợp lý hiện tại là:

1. Dùng **official Elasticsearch Java API Client** cho phần gọi Elasticsearch.
2. Không tự ghép JSON bằng `RestClient` raw trong service.
3. Không trộn official client với Spring Data Elasticsearch trong cùng mini-lab.
4. Không dùng Elasticsearch repository generic để thay thế JPA repository của PostgreSQL.
5. Giữ Spring Data Elasticsearch như theory/reference later, không trộn vào implementation hiện tại.

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
- Mini-lab hiện dùng một Elasticsearch URI local; chưa cần xử lý cluster nhiều node/SSL/credential.
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

## Elasticsearch API shape nằm ở đâu trong code?

Chi tiết URI/request/response/error nằm ở [elasticsearch-request-response-shapes.md](./elasticsearch-request-response-shapes.md).

Trong code Spring Boot, các API đó được gom vào `MasterDataSearchGateway`:

| Elasticsearch concept | Java API Client call | Class đang sở hữu |
|---|---|---|
| tạo/xóa index | `client.indices().create(...)`, `delete(...)` | `MasterDataSearchGateway` |
| bulk index | `client.bulk(...)` | `MasterDataSearchGateway` |
| search | `client.search(...)` | `MasterDataSearchGateway` |
| parse hits | `SearchResponse<T>.hits().hits()` | `MasterDataSearchGateway` |

Service/controller không nên biết raw URI như `POST /{index}/_search` hoặc response field như `_source`, `_score`, `_shards`.

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

1. `make -f Makefile.legacy db-up`
2. `make -f Makefile.legacy elastic-up`
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
- Elasticsearch local chạy được bằng `make -f Makefile.legacy elastic-up`.
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
