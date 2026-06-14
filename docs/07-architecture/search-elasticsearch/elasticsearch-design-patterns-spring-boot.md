# Elasticsearch design patterns trong Spring Boot mini-lab

> Ghi chú Phase 1.5: các pattern này được học ban đầu trong `tenant-demo`. Runtime hiện tại đã tách projection sang `lab-code/search-service`; xem [search-service-split-plan.md](./search-service-split-plan.md) cho service boundary đang dùng.

## Vai trò của file này

File này giải thích **vì sao code search được tách thành nhiều class** thay vì gọi Elasticsearch trực tiếp từ controller/service.

Đọc kèm:

- `elasticsearch-search-service.md` - Elasticsearch là gì và khi nào cần.
- `elasticsearch-code-guide-spring-boot.md` - code guide và cách tích hợp client.
- `elasticsearch-request-response-shapes.md` - input/output shape của Elasticsearch.

## Pattern đang dùng

Code hiện tại dùng pattern gần với:

- **Gateway / Adapter**: `MasterDataSearchGateway` là cổng ra ngoài để gọi Elasticsearch.
- **Infrastructure adapter**: chi tiết Elasticsearch Java API Client nằm ở lớp hạ tầng, không lộ vào service/controller.
- **Anti-corruption layer nhẹ**: app không trả raw Elasticsearch response và không để Query DSL lan vào business flow.

Nói ngắn gọn:

```text
Controller -> Service/use case -> Gateway/Adapter -> Elasticsearch Java API Client
```

`MasterDataSearchGateway` **không phải Repository thay thế PostgreSQL**. Repository JPA vẫn là nơi đọc/ghi source of truth từ PostgreSQL.

## Class shape khuyến nghị trong repo

| Class | Trách nhiệm |
|---|---|
| `MasterDataSearchDocument` | DTO/projection đưa vào Elasticsearch. Tách khỏi JPA entity. |
| `MasterDataSearchGateway` | Adapter gọi Elasticsearch: create index, bulk index, search, parse hits. |
| `MasterDataSearchIndexer` | Use case reindex: đọc PostgreSQL, map entity sang document, gọi gateway. |
| `MasterDataSearchService` | Use case search tenant-aware: lấy tenant từ `TenantContext`, validate keyword, gọi gateway. |
| `MasterDataSearchController` | HTTP endpoint mỏng: nhận request, gọi service/indexer, trả response. |
| `MasterDataSearchReindexResponse` | Response nhỏ cho endpoint reindex, không trả raw bulk response. |
| `SearchProperties` | Gom config search: enabled, URI, index name. |

Shape này đủ nhỏ cho mini-lab nhưng vẫn gần style backend thật: controller không chứa logic, service không tự ghép HTTP/JSON Elasticsearch, gateway không biết chi tiết HTTP request của client.

## Pattern này generalize sang domain khác thế nào?

Khi chuyển từ `master_data` sang domain khác, giữ nguyên vai trò lớp, chỉ đổi tên và field:

| `master_data` lab | Domain khác |
|---|---|
| `MasterDataSearchDocument` | `ProductSearchDocument`, `InvoiceSearchDocument`, `CustomerSearchDocument` |
| `MasterDataSearchGateway` | `ProductSearchGateway` hoặc gateway search riêng cho domain đó |
| `MasterDataSearchIndexer` | job/use case reindex sản phẩm, hóa đơn, khách hàng |
| `MasterDataSearchService` | use case search theo tenant và keyword của domain |
| `MasterDataSearchController` | API search mỏng cho domain |

Không cần tạo generic search framework ngay. Chỉ nên trừu tượng hóa thêm khi nhiều domain có duplication thật sự và team đã hiểu rõ convention.

## Data flow

### Reindex flow

```text
POST /api/search/master-data/reindex
-> MasterDataSearchController
-> MasterDataSearchIndexer
-> MasterDataRepository.findAll()
-> MasterDataSearchDocument.fromEntity(...)
-> MasterDataSearchGateway.bulkIndex(...)
-> Elasticsearch master_data_search
```

Điểm cần nhớ:

- PostgreSQL là source of truth.
- Reindex đọc lại từ DB rồi ghi projection sang Elasticsearch.
- Endpoint reindex chỉ dành cho local/admin lab, không public production tùy tiện.

### Search flow

```text
Authorization: Bearer <token>
-> Spring Security validate token
-> JwtTenantContextFilter set TenantContext
-> MasterDataSearchController
-> MasterDataSearchService
-> MasterDataSearchGateway.search(tenantId, keyword)
-> Elasticsearch bool query
-> safe response fields
```

Tenant isolation nằm ở service/gateway flow:

- `tenantId` lấy từ `TenantContext`, không lấy từ query param/body.
- Elasticsearch bool query luôn có `tenantId` filter.
- Response không được lộ raw Elasticsearch metadata không cần thiết.

## Vì sao Controller phải mỏng?

Controller nên chỉ xử lý HTTP boundary:

- nhận `keyword`;
- gọi service;
- trả response.

Controller không nên:

- tự đọc `TenantContext`;
- tự tạo Query DSL;
- tự parse Elasticsearch hits;
- gọi JPA repository hoặc Elasticsearch client trực tiếp.

Giữ controller mỏng giúp API dễ đọc và ít lẫn business/infrastructure logic.

## Vì sao Service giữ intent tenant-aware?

`MasterDataSearchService` trả lời câu hỏi nghiệp vụ: “tenant hiện tại muốn search `master_data` theo keyword”.

Service nên biết:

- keyword có hợp lệ không;
- tenant hiện tại là ai;
- search phải scoped theo tenant.

Service không cần biết:

- URI Elasticsearch là gì;
- request JSON shape cụ thể ra sao;
- `_bulk` là NDJSON;
- hits response có `_source` nằm ở đâu.

## Vì sao Gateway sở hữu Elasticsearch details?

Elasticsearch là external system. Gateway/Adapter giúp gom chi tiết này vào một nơi:

- API client nào được dùng;
- index name;
- create index;
- bulk index;
- bool query;
- parse search hits;
- translate lỗi Elasticsearch thành lỗi backend dễ hiểu hơn.

Nếu sau này đổi từ official Java API Client sang Spring Data Elasticsearch, phần thay đổi chính nên nằm ở Gateway/code guide, không lan khắp controller/service.

## Vì sao Document tách khỏi JPA Entity?

`MasterData` là JPA entity:

- mapping với bảng PostgreSQL;
- chịu constraint/schema do Flyway quản lý;
- phục vụ transaction và source of truth.

`MasterDataSearchDocument` là search projection:

- có thể denormalize;
- có thể stale;
- phục vụ search;
- luôn phải có `tenantId`.

Tách hai class giúp không nhầm “row nghiệp vụ chính” với “document index phục vụ tìm kiếm”.

## Client integration approach

Repo hiện tại dùng **official Elasticsearch Java API Client**.

So sánh đầy đủ giữa raw HTTP/Spring `RestClient`, Java API Client và Spring Data Elasticsearch nằm ở [elasticsearch-code-guide-spring-boot.md](./elasticsearch-code-guide-spring-boot.md). Ở tài liệu pattern này chỉ cần nhớ một rule: **không mix nhiều cách gọi Elasticsearch trong cùng implementation**, vì service/gateway sẽ khó đọc và khó debug.

## Request/response shape cần nhớ

Search request logic:

```text
bool query
├── must: multi_match(keyword) trên code/name/category
└── filter: tenantId = current tenant, active = true
```

Response của API chỉ trả:

- `id`
- `tenantId`
- `code`
- `name`
- `category`
- `active`

Không trả raw Elasticsearch response vì raw response có nhiều metadata (`_index`, `_score`, shard info) không cần cho API client trong mini-lab.

## Common mistakes

- Gọi Elasticsearch trực tiếp từ controller.
- Để service tự ghép JSON DSL bằng `Map<String, Object>` rối và dễ sai.
- Dùng Elasticsearch như source of truth.
- Quên `tenantId` trong document hoặc query filter.
- Nhận `tenantId` từ request param/body.
- Trả raw Elasticsearch response ra ngoài.
- Dùng `MasterData` JPA entity làm luôn search document.
- Bật search mặc định làm test fail khi Elasticsearch chưa chạy.

## Vì sao thiết kế này phù hợp Phase 1?

Thiết kế hiện tại không quá production-heavy, nhưng vẫn giữ các ranh giới quan trọng:

- business data vẫn ở PostgreSQL;
- search index là projection;
- tenant isolation vẫn enforced ở backend;
- Elasticsearch details không lan vào controller/service;
- app/test baseline không phụ thuộc Elasticsearch nhờ `APP_SEARCH_ENABLED=false`.

Đây là mức vừa đủ để demo với mentor: có công nghệ thật, có flow thật, nhưng không biến Phase 1 thành search platform production.

## Nguồn tham khảo chuẩn

- [Elastic Java API Client - Getting started](https://www.elastic.co/docs/reference/elasticsearch/clients/java/getting-started)
- [Elastic Java API Client - Searching documents](https://www.elastic.co/docs/reference/elasticsearch/clients/java/usage/searching)
- [Spring Boot - Elasticsearch support](https://docs.spring.io/spring-boot/reference/data/nosql.html#data.nosql.elasticsearch)
- [Spring Data Elasticsearch Reference](https://docs.spring.io/spring-data/elasticsearch/reference/)
