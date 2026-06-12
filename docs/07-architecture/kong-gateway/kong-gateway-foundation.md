# Kong Gateway Foundation

## 1. Kong là gì?

Kong Gateway là API Gateway platform nằm trước upstream services. Client gọi Kong, Kong route request tới service phía sau và có thể áp dụng plugin như request-id, rate limit, auth pre-check, logging hoặc transformation.

Trong Phase 1, Spring Cloud Gateway đã giúp hiểu gateway concept bằng Java/Spring. Kong giúp làm quen cách target architecture thường vận hành gateway như một platform component độc lập.

## 2. Kong vs Spring Cloud Gateway

| Tiêu chí | Spring Cloud Gateway | Kong Gateway |
|---|---|---|
| Bản chất | Java/Spring app gateway | API gateway platform |
| Config | `application.yml`/Spring beans | services/routes/plugins, DB/declarative config |
| Phù hợp học gì? | Route/predicate/filter trong Spring | Gateway platform, plugin, declarative config |
| Vai trò trong repo | Đã có mini-lab static route | Phase 1.5 platform lab |
| Có thay backend security không? | Không | Không |

Kong không làm backend hết trách nhiệm. `tenant-demo`/future services vẫn phải validate token, map role và enforce tenant-aware query.

## 3. General Kong config anatomy

Khi dùng Kong, thường gặp các nhóm config sau:

| Khái niệm | Nghĩa |
|---|---|
| Gateway / reverse proxy | Thành phần client gọi vào, sau đó forward request tới upstream service. |
| Service | Đại diện cho upstream backend mà Kong sẽ gọi. Ví dụ `tenant-demo-api`. |
| Route | Điều kiện match request vào, thường dựa trên path/method/host. |
| Upstream target | Địa chỉ backend thật phía sau Kong, ví dụ `http://host.docker.internal:8080`. |
| Plugin | Behavior gắn vào global/service/route, ví dụ CORS, request-id, rate limit, auth. |
| Consumer | Đại diện cho caller/app/user ở Kong layer. Lab này chưa dùng Consumer. |
| DB-less mode | Kong không dùng database, đọc config từ file declarative. |
| Declarative config file | File `kong.yml` mô tả services/routes/plugins. |
| Admin API | API quản trị Kong. Local lab expose ở `localhost:18001`, production không được public bừa bãi. |
| Proxy port | Port client gọi business API qua Kong. Local lab dùng `localhost:18000`. |
| Admin port | Port quản trị Kong. Local lab bind local-only `127.0.0.1:18001`. |

## 4. Service, Route, Plugin

Mental model:

```text
Route: request nào match?
Service: forward tới upstream nào?
Plugin: áp dụng behavior nào?
```

Ví dụ Phase 1.5:

```text
Route /api/master-data/**
-> Service master-data-service http://host.docker.internal:8080

Route /api/audit-events/**
-> Service audit-log-service http://host.docker.internal:8082
```

Plugin có thể thử local:

- cors plugin nếu cần cho React Web;
- request-id plugin nếu muốn Kong sinh request id sau này;
- logging plugin awareness only nếu đã dùng Loki collector.

Trong lab hiện tại, request id đã được UI/Spring Gateway/backend xử lý bằng `X-Request-Id`, nên Kong lab ưu tiên preserve header thay vì thêm plugin mới ngay.

## 5. DB-less/declarative config

Kong có thể chạy DB-less: không cần database cho gateway config, config entity nằm trong file YAML/JSON declarative. Kong docs nói DB-less mode dùng in-memory storage và declarative configuration file; lợi ích là ít dependency và config có thể nằm trong Git.

Nguồn: [Kong DB-less mode docs](https://developer.konghq.com/gateway/db-less-mode/).

Với repo học local, DB-less là hướng tốt:

- Docker-first;
- không thêm Postgres riêng cho Kong;
- dễ review config;
- dễ reset.

## 6. Lab direction cho repo

Kong mini-lab nên làm:

```text
React Web
-> Kong :8000
-> tenant-demo/master-data-service :8080
```

Route ban đầu đã implement:

- `/api/master-data/**` -> `tenant-demo`.
- `/tenant-demo/actuator/health` -> `tenant-demo /actuator/health`.

Route sau khi split service:

- `/api/audit-events/**` -> `audit-log-service`.

Yêu cầu:

- preserve `Authorization`;
- preserve `X-Request-Id`;
- không bypass backend security;
- Admin API không expose public trong production; local lab chỉ bind cẩn thận.

## 7. Production caveats

- Kong Admin API không được public bừa bãi.
- Production cần TLS, rate limit, access control, network boundary.
- Nếu validate JWT ở gateway, backend vẫn nên validate hoặc chỉ trust trong internal boundary rất chặt.
- Không assume enterprise plugins.
- Không thêm Kubernetes trong Phase 1.5.

## 8. Common mistakes

- Đưa business logic vào gateway.
- Nghĩ gateway auth thay thế service auth.
- Route quá rộng làm lộ actuator/private endpoints.
- Log Authorization header/token ở gateway.
- Mở CORS wildcard cho demo rồi quên caveat.
- Dùng `localhost:8080` làm upstream trong container Kong khi backend chạy trên host. Với Docker Desktop/local lab này, dùng `host.docker.internal:8080`.
