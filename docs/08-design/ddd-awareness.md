# DDD awareness cho Master Data Portal

## Vai trò của tài liệu

Tài liệu này đóng vai trò tham chiếu cuối cho chủ đề DDD/domain boundary trong Phase 1 / Phase 1.5. Mục tiêu là giúp đọc lại repo và trả lời:

- domain boundary là gì;
- entity/service/module khác nhau thế nào;
- khi nào nên nghĩ tới DDD;
- repo hiện tại đã có boundary nào;
- phần nào chỉ là reference tương lai, không phải implementation hiện tại.

Đây không phải kế hoạch refactor. Repo hiện là production-like local lab, không phải hệ ERP production đầy đủ.

## DDD là gì?

DDD, hay Domain-Driven Design, là cách thiết kế phần mềm bắt đầu từ domain nghiệp vụ thay vì bắt đầu từ framework, database table hoặc folder structure.

Trong DDD, hệ thống được chia quanh các khái niệm nghiệp vụ có ý nghĩa với người dùng và người vận hành, ví dụ:

- tenant;
- master data;
- file attachment;
- audit/activity history;
- search projection;
- account/role/permission.

Điểm quan trọng: DDD không bắt buộc dùng microservices. Một monolith có module rõ ràng vẫn có thể theo tinh thần DDD. Ngược lại, nhiều service nhỏ nhưng boundary sai vẫn không phải thiết kế tốt.

## Vì sao DDD quan trọng?

Khi hệ thống lớn dần, lỗi thường không đến từ thiếu framework mà đến từ boundary mơ hồ:

- service này đọc hoặc ghi dữ liệu thuộc service khác;
- controller xử lý quá nhiều business rule;
- event chứa raw entity quá lớn;
- search/cache/projection bị hiểu nhầm là source of truth;
- file object storage bị dùng như database;
- frontend hoặc gateway bắt đầu quyết định business authorization thay backend.

DDD giúp đặt câu hỏi đúng:

- dữ liệu này thuộc domain nào;
- rule này nên nằm ở service nào;
- transaction boundary ở đâu;
- API/event nào là contract;
- phần nào là source of truth, projection, cache hoặc read model.

## Mental model ngắn gọn

Một cách nhớ phù hợp với repo này:

```text
Domain
  -> có ngôn ngữ nghiệp vụ riêng
  -> có dữ liệu/rule thuộc quyền sở hữu của nó
  -> có API/event để domain khác tương tác
  -> không để domain khác ghi thẳng vào database của mình
```

Ví dụ:

```text
master data domain
  source of truth: PostgreSQL master_data
  owner: tenant-demo
  event: MasterDataChangedEvent

audit domain
  read model: audit_log.audit_events
  owner: audit-log-service
  input: MasterDataChangedEvent

search domain/projection
  projection: Elasticsearch master_data_search
  owner: search-service
  input: MasterDataChangedEvent hoặc admin reindex

file domain
  metadata source of truth: PostgreSQL file metadata
  binary object: MinIO
  owner: file-service
```

## Thuật ngữ cốt lõi

| Thuật ngữ | Ý nghĩa | Cách đọc trong repo |
|---|---|---|
| Domain | Khu vực nghiệp vụ có dữ liệu, rule và ngôn ngữ riêng. | Master data, file, audit, search. |
| Subdomain | Một phần nhỏ hơn của domain tổng thể. | File attachment là subdomain hỗ trợ business data. |
| Bounded Context | Ranh giới nơi một model có ý nghĩa nhất quán. | `audit-log-service` hiểu `AuditEvent`; `tenant-demo` hiểu `MasterData`. |
| Entity | Object có identity ổn định theo thời gian. | `MasterData` có `id`, `tenantId`, `code`. |
| Value Object | Object được so sánh bằng giá trị, không cần identity riêng. | Có thể là money, address, period trong ERP thật; repo hiện chưa cần model riêng. |
| Aggregate | Cụm entity/value object được thay đổi qua một root để giữ invariant. | Repo hiện chưa model aggregate phức tạp; `MasterData` gần nhất với aggregate root nhỏ. |
| Repository | Port/pattern để đọc/ghi aggregate/entity từ source of truth. | Spring Data repositories trong từng service. |
| Application Service | Điều phối use case: validate input, gọi domain/repository/event. | `MasterDataService`, file service layer, search reindex service. |
| Domain Event | Sự kiện nói một việc nghiệp vụ đã xảy ra. | `MasterDataChangedEvent`. |
| Integration Event | Event dùng để service khác đồng bộ/read model/projection. | Trong repo, `MasterDataChangedEvent` đang đóng vai trò integration event. |
| Read Model | Model tối ưu cho đọc/hiển thị, không nhất thiết là source of truth. | Audit table, Elasticsearch index. |
| Anti-corruption Layer | Lớp chuyển đổi giữa model bên ngoài và model nội bộ. | DTO/event mapper, không import JPA entity từ service khác. |

## Mapping vào kiến trúc hiện tại

### CURRENT - hiện đang đúng trong repo

| Boundary | Owner | Source of truth / projection | Contract chính |
|---|---|---|---|
| Master data | `tenant-demo` | PostgreSQL `master_data` | REST `/api/master-data`, Kafka `MasterDataChangedEvent` |
| Audit/activity | `audit-log-service` | PostgreSQL schema/table audit | REST `/api/audit-events`, consume `MasterDataChangedEvent` |
| File attachment | `file-service` | PostgreSQL metadata + MinIO object | REST `/api/files` |
| Search | `search-service` | Elasticsearch projection | REST `/api/search/master-data`, consume event, admin reindex |
| Security plumbing | `common-security` | Không sở hữu business data | Shared library cho TenantContext/filter/converter |
| Identity/AuthN | Keycloak | Realm/client/user/role/token | OIDC login/token/JWKS |
| Gateway | Kong | Không sở hữu domain data | Route/proxy entry point |

Các service Spring Boot là Resource Server. Chúng tự validate JWT và tự enforce tenant/role rule. Kong route request, nhưng không nhận ownership business authorization.

### HISTORICAL - giữ để học, không phải final runtime boundary

| Artifact | Vai trò lịch sử |
|---|---|
| `lab-code/gateway-demo` | Spring Cloud Gateway concept lab trước khi final demo chuyển sang Kong. |
| Một số mini-lab riêng lẻ | Học từng công nghệ trước khi service boundary Phase 1.5 ổn định. |
| `tenant-demo` consumer học Kafka cũ | Không còn là consumer chính; final flow dùng `audit-log-service` và `search-service`. |

### FUTURE / REFERENCE - không coi là đã implement

| Chủ đề | Trạng thái |
|---|---|
| Full DDD aggregate model cho ERP/accounting | Chưa làm; domain demo còn nhỏ. |
| Outbox pattern | Chưa làm; hiện DB write và Kafka publish chưa atomic kiểu production. |
| Retry/DLT/schema registry | Chưa làm; chỉ là production caveat/reference. |
| Service discovery/Kubernetes/HA/TLS/secrets hardening | Chưa làm trong repo. |
| Full permission matrix/authorization service | Chưa làm; Keycloak + backend role checks đủ cho demo. |

## Boundary và ownership

### `tenant-demo`

`tenant-demo` là owner của master data write path:

- nhận request create/update/deactivate;
- lấy tenant từ JWT/TenantContext;
- ghi PostgreSQL;
- publish `MasterDataChangedEvent`.

Các service khác không nên import JPA entity/repository của `tenant-demo`. Nếu cần biết master data thay đổi, chúng dùng event hoặc API.

### `audit-log-service`

`audit-log-service` không quyết định business rule của master data. Nó sở hữu audit/read model:

- consume event;
- lưu event theo tenant;
- expose read-only API tenant-aware.

Audit data là evidence/read model, không phải nơi sửa master data.

### `file-service`

`file-service` sở hữu lifecycle file:

- metadata và tenant ownership nằm trong PostgreSQL;
- binary payload nằm trong MinIO;
- backend kiểm tra tenant/role trước khi stream/download/delete object.

Client không được tự quyết định object key hoặc gọi MinIO trực tiếp như source of truth.

### `search-service`

`search-service` sở hữu projection vào Elasticsearch:

- consume master data event;
- cập nhật index;
- expose search API tenant-aware;
- admin reindex rebuild projection từ source of truth hiện tại.

Elasticsearch không phải database chính. Nếu search index lệch, phải rebuild từ PostgreSQL/master data source path, không sửa tay index như nghiệp vụ thật.

### `common-security`

`common-security` là shared library, không phải bounded context nghiệp vụ:

- reusable TenantContext;
- JWT tenant extraction/filter;
- Keycloak role/authority converter.

Không đưa business rule, controller, JPA entity, repository hoặc event contract vào module này.

## Entity, DTO, event, projection khác nhau thế nào?

| Loại object | Mục đích | Ví dụ trong repo | Lỗi dễ gặp |
|---|---|---|---|
| JPA entity | Map table source of truth trong service sở hữu. | `MasterData`, file metadata entity, audit entity. | Dùng entity của service này trong service khác. |
| Request/response DTO | Contract HTTP với client/service khác. | Master data create/update response, file metadata response. | Trả raw entity làm lộ field nội bộ khi domain lớn dần. |
| Event DTO | Contract async giữa producer và consumer. | `MasterDataChangedEvent`. | Nhét toàn bộ entity/raw payload lớn vào event. |
| Search document | Projection tối ưu cho query search. | `MasterDataSearchDocument`. | Coi search document là source of truth. |
| Cache value | Bản sao tạm để tăng tốc đọc. | Cached master data by code. | Cache thiếu tenantId hoặc không invalidation đúng. |

## Trade-off trong repo hiện tại

### Vì sao không refactor DDD sâu?

Repo này dùng `master_data` làm một lát cắt học kiến trúc, không phải domain kế toán thật. Nếu áp dụng DDD quá sâu lúc domain còn nhỏ, chi phí tăng nhanh:

- nhiều package/layer nhưng ít rule nghiệp vụ;
- mất trọng tâm vào multi-tenant/security/Kafka/file/search;
- dễ tạo abstraction giả không có business invariant thật.

Vì vậy Phase 1 / Phase 1.5 chọn hướng thực dụng:

- service boundary rõ ở runtime;
- repository query tenant-aware;
- event contract đủ cho audit/search;
- source of truth/projection/cache/object storage được phân biệt rõ;
- DDD giữ ở mức awareness/reference.

### Khi nào DDD bắt đầu đáng giá?

DDD đáng revisit khi xuất hiện rule nghiệp vụ đủ phức tạp, ví dụ:

- accounting document có trạng thái draft/posted/cancelled với invariant rõ;
- approval workflow có role và điều kiện chuyển trạng thái;
- journal entry cần cân bằng debit/credit;
- invoice/payment reconciliation cần aggregate boundary rõ;
- nhiều team cùng sửa chung một model và dùng từ vựng khác nhau;
- service này cần dữ liệu service khác nhưng không rõ contract nên bắt đầu query chéo database.

## Common misconceptions

| Nhầm lẫn | Cách hiểu đúng |
|---|---|
| DDD là phải tạo nhiều microservices. | DDD là tư duy domain boundary; microservice chỉ là một cách deploy boundary. |
| Entity trong JPA chính là domain model hoàn chỉnh. | JPA entity có thể gần domain model, nhưng thường bị ràng buộc bởi persistence concern. |
| Mỗi bảng nên thành một service. | Service boundary nên theo ownership/use case, không theo số bảng. |
| Gateway hoặc frontend có thể quyết định tenant/business rule. | Backend service sở hữu business rule và tenant isolation. |
| Event nên chứa toàn bộ database row. | Event nên là contract ổn định, đủ nghĩa, không làm lộ model nội bộ quá mức. |
| Elasticsearch/cache có thể thay PostgreSQL. | Search/cache là derived data; PostgreSQL vẫn là source of truth. |
| Shared library là một bounded context. | Shared library chỉ gom code dùng chung; nó không sở hữu business capability. |

## Cách đọc repo bằng lăng kính DDD

Khi mở một service, đọc theo thứ tự ownership:

1. Service này sở hữu capability nào?
2. Source of truth của capability đó là gì?
3. API/event nào là contract ra bên ngoài?
4. Tenant/role được enforce ở đâu?
5. Service có import entity/repository của service khác không?
6. Projection/cache/read model có được ghi rõ là derived data không?

Với repo hiện tại:

- `tenant-demo` là write owner cho master data;
- `audit-log-service` là read-model owner cho activity;
- `file-service` là owner file metadata/lifecycle;
- `search-service` là owner search projection;
- Keycloak là identity provider;
- Kong là gateway boundary;
- `common-security` là code reuse, không phải service domain.

## Khi nào đọc lại tài liệu này?

Đọc lại tài liệu này khi:

- muốn giải thích vì sao repo tách `audit-log-service`, `file-service`, `search-service`;
- cân nhắc thêm service mới;
- muốn thêm nghiệp vụ kế toán thật thay vì chỉ `master_data`;
- thấy service bắt đầu đọc/ghi database của nhau;
- thấy event/cache/search document bị nhầm với source of truth;
- muốn trả lời mentor vì sao chưa refactor toàn bộ theo DDD.

## Nguồn tham khảo chuẩn

- Eric Evans, *Domain-Driven Design: Tackling Complexity in the Heart of Software*.
- Vaughn Vernon, *Implementing Domain-Driven Design*.
- Martin Fowler, bài viết về bounded context, domain model và microservice trade-off.
- Microsoft architecture guidance về DDD, bounded contexts và microservices.

Các nguồn trên dùng để hiểu khái niệm thiết kế. Source of truth cho implementation cụ thể vẫn là code và docs hiện tại của repo.
