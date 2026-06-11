# Chuẩn viết theory doc cho công nghệ backend

## Mục đích

Từ sau mini-lab Elasticsearch, theory doc trong repo nên dạy kiến thức nền có thể dùng lại cho nhiều bài toán backend, rồi mới nối về mini-lab hiện tại.

Tránh kiểu tài liệu chỉ nói “trong lab này làm X” nhưng người đọc vẫn không hiểu công nghệ đó là gì, request/response ra sao, khi nào dùng, khi nào không dùng.

## Nguyên tắc chính

1. Foundation trước, lab sau.
2. Dạy khái niệm có thể chuyển sang domain khác.
3. Có ví dụ cụ thể nhưng không khóa chặt vào một entity duy nhất.
4. Phân biệt rõ source of truth, projection, cache, message, object storage, security boundary.
5. Có phần request/response/config/error shape nếu công nghệ đó có API/protocol rõ ràng.
6. Có code guide riêng nếu cần tích hợp Spring Boot.
7. Có config/code walkthrough nếu Codex sinh Docker Compose, config hoặc code pattern mới.
8. Lab README chỉ nên chứa command/checklist thực hành, không lặp lại toàn bộ theory.
9. Summary sau milestone phải mentor-facing, ngắn và nói được đã học gì, verify gì.

## Quy tắc bắt buộc cho Codex implementation

Khi dùng Codex để tạo implementation cho công nghệ mới, runnable code/config chưa đủ. Người học phải đọc lại được file sinh ra và giải thích runtime flow.

Vì vậy:

- Nếu Codex thêm Docker Compose/config files cho công nghệ mới, Codex phải thêm walkthrough giải thích từng file, từng service chính và các field quan trọng.
- Nếu Codex thêm code cho pattern mới, Codex phải thêm code-flow walkthrough giải thích class nào làm gì và request/event đi qua đâu.
- Nếu config/API/protocol có shape dễ nhầm, Codex phải thêm shape doc hoặc section giải thích input/output/error/config.
- Không được chỉ tạo file chạy được rồi để người học tự đoán.
- Walkthrough phải repo-grounded: dùng đúng tên file, class, endpoint, env var, Docker service trong repo.
- Walkthrough không chứa secret/token/log thật.
- Mỗi walkthrough phải có cách verify và common mistakes.

Ví dụ:

| Codex thêm gì? | Doc bắt buộc đi kèm |
|---|---|
| `docker-compose.yml` cho Loki/Kafka UI/Kong | Config walkthrough giải thích từng service, port, volume, network, datasource/route. |
| Spring Boot Kafka producer/consumer | Code-flow walkthrough: service save DB -> event DTO -> producer -> broker -> listener -> handler. |
| Redis cache-aside code | Code-flow walkthrough: key factory -> Redis get -> DB fallback -> set TTL -> tenant safety. |
| Gateway config | Route walkthrough: path predicate -> filter/rewrite -> backend URI -> header propagation. |

Mục tiêu cuối: sinh viên có thể mở file generated config/code và tự thuyết trình được "vì sao có dòng này".

## Cấu trúc khuyến nghị cho mỗi công nghệ

Không phải công nghệ nào cũng cần đủ 5 file, nhưng nếu topic đủ lớn thì nên tách như sau.

### 1. Concept/foundation doc

Ví dụ:

```text
docs/07-architecture/search-elasticsearch/elasticsearch-search-service.md
```

Nên có:

- công nghệ là gì;
- vì sao tồn tại;
- nằm ở đâu trong backend architecture;
- khái niệm lõi;
- khi nào dùng;
- khi nào không nên dùng;
- data/security/tenant concern;
- production caveat ở mức vừa đủ;
- phần áp dụng vào repo hiện tại.

Không nên:

- chỉ kể lại code mini-lab;
- nhảy thẳng vào dependency/config;
- biến thành textbook dài không có liên hệ thực hành.

### 2. Request/response/config/API shape doc

Ví dụ:

```text
docs/07-architecture/search-elasticsearch/elasticsearch-request-response-shapes.md
```

Dùng khi công nghệ có API/protocol/config shape dễ gây nhầm.

Nên có:

- URI hoặc protocol operation thường gặp;
- request shape;
- response shape;
- error shape;
- field quan trọng cần đọc;
- common mistakes;
- cách map sang client/library trong code;
- phần “backend nên log/trả lỗi thế nào”.

Ví dụ công nghệ cần dạng doc này:

- Elasticsearch request/response/error;
- MinIO/S3 API object/bucket/presigned URL;
- Kafka topic/message/consumer group/error retry;
- Redis key/value/TTL/cache miss;
- OAuth2/OIDC token/metadata/JWKS;
- Prometheus metrics format nếu học observability.

### 3. Code guide / integration pattern doc

Ví dụ:

```text
docs/07-architecture/search-elasticsearch/elasticsearch-code-guide-spring-boot.md
```

Nên có:

- dependency/library nên dùng;
- package/class shape;
- config properties/env;
- service/client/gateway pattern;
- controller/API shape nếu có;
- test/verification style;
- feature flag/profile nếu công nghệ optional;
- common mistakes trong code;
- done criteria.

Không nên lặp toàn bộ foundation/API shape. Link về doc nền.

### 4. Config/code walkthrough doc

Ví dụ:

```text
docs/07-architecture/log-aggregation-loki/loki-local-lab-config-walkthrough.md
```

Dùng khi Codex thêm config/code mới đáng kể.

Nên có:

- big picture runtime flow;
- file map;
- từng file mới dùng để làm gì;
- từng service/class/block chính có trách nhiệm gì;
- các field/env/property quan trọng;
- data/request/log/event đi qua các bước nào;
- local vs production URL/network khác nhau thế nào;
- common mistakes;
- production caveats;
- verification commands.

Không nên:

- lặp lại toàn bộ foundation doc;
- viết chung chung không trỏ tới file thật;
- giấu logic quan trọng sau câu "xem config".

### 5. Lab README

Ví dụ:

```text
lab-code/elasticsearch-lab/README.md
```

Nên có:

- cách start local dependency;
- env/config cần set;
- command kiểm tra;
- manual verification;
- cleanup/reset;
- caveat local-only.

Không nên:

- giải thích lại dài dòng lý thuyết;
- chứa secret/token thật;
- phụ thuộc vào local note riêng.

### 6. Milestone summary

Ví dụ:

```text
docs/99-tong-ket/nhung-gi-da-nam-duoc.md
```

Nên có:

- đã học concept gì;
- đã implement/verify gì;
- command hoặc pattern đã kiểm tra;
- liên hệ với target architecture;
- giới hạn hiện tại;
- next step.

## Outline chuẩn cho theory doc

Khi viết doc mới, có thể dùng outline này:

```markdown
# <Technology> foundation

## Vai trò của tài liệu

## Công nghệ này là gì?

## Vì sao backend cần nó?

## Nó nằm ở đâu trong target architecture?

## Khái niệm lõi

## Standard data/request/response/error shape

## Pattern tích hợp Spring Boot

## Data/security/tenant concern

## Common mistakes

## Production caveats

## Áp dụng vào repo hiện tại

## Nguồn tham khảo chuẩn
```

Nếu doc quá dài, tách `Standard data/request/response/error shape` và `Pattern tích hợp Spring Boot` sang file riêng.

## Nguồn tham khảo

Ưu tiên:

- official docs của công nghệ;
- Spring/Spring Boot official docs;
- RFC/standard nếu là giao thức;
- tài liệu vendor chính thức.

Không dùng blog ngẫu nhiên làm nguồn chính. Nếu dùng để tham khảo cách giải thích, vẫn phải kiểm tra lại bằng nguồn chuẩn.

## Checklist review trước khi merge doc

- [ ] Người mới có hiểu công nghệ này là gì không?
- [ ] Có giải thích vì sao/khi nào dùng không?
- [ ] Có nói khi nào không nên dùng không?
- [ ] Có khái niệm lõi đủ để đọc code không?
- [ ] Có request/response/config/error shape nếu cần không?
- [ ] Nếu có Docker Compose/config mới, có config walkthrough chưa?
- [ ] Nếu có code pattern mới, có code-flow walkthrough chưa?
- [ ] Walkthrough có dùng đúng file/class/service/env trong repo không?
- [ ] Có liên hệ với multi-tenant/security không?
- [ ] Có phần áp dụng vào repo hiện tại không?
- [ ] Có link tới code guide/lab README/summary liên quan không?
- [ ] Có tránh lặp nội dung giữa nhiều file không?
- [ ] Không chứa token, secret, password thật.

## Ví dụ tốt và chưa tốt

Chưa tốt:

```text
Trong lab này tạo endpoint /api/search/master-data và gọi Elasticsearch.
```

Vì câu này chỉ nói việc cần làm, không dạy Elasticsearch là gì.

Tốt hơn:

```text
Elasticsearch là search projection cho dữ liệu từ PostgreSQL. PostgreSQL vẫn là source of truth, còn Elasticsearch tối ưu cho full-text search. Với multi-tenant, document và query đều phải có tenantId để tránh leakage.
```

Vì câu này dạy được vai trò chung và rule áp dụng lại cho domain khác.

## Áp dụng cho các topic sắp tới

| Topic | Doc foundation | Shape/API doc | Code guide | Walkthrough | Lab README |
|---|---|---|---|---|---|
| Elasticsearch | đã có | đã có | đã có | nên có nếu đổi code/config lớn | đã có |
| MinIO/S3 | đã có | đã có object/bucket/presigned URL shape | đã có | nên có nếu thêm presigned/lifecycle/versioning runtime | đã có |
| Redis cache | đã có | key/TTL/cache miss shape trong plan/code guide | đã có | nên có nếu đổi cache implementation lớn | đã có |
| Kafka | đã có | đã có topic/message/consumer group shape | đã có | nên bổ sung code-flow nếu mở cross-service flow | đã có |
| Loki/log aggregation | đã có | config walkthrough là source chính cho runtime config | không cần Spring code guide | đã có | đã có |
| Kafka UI/Kong | cần tạo khi tới task | cần config/API walkthrough | nếu có code/config Spring hoặc declarative config | bắt buộc nếu thêm compose/config | nên có |

## AI/Codex workflow note

Codex có thể hỗ trợ implement nhanh, nhưng với repo học tập này thì tiêu chuẩn bàn giao là:

```text
runnable artifact
+ explanation artifact
+ verification path
```

Nếu thiếu explanation artifact, milestone chưa thật sự tốt cho học tập. Sinh viên cần đọc được generated config/code, hiểu luồng runtime và biết điểm nào là local-only, điểm nào là production caveat.
