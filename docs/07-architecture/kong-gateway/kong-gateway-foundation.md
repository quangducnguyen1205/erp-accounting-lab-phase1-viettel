# Kong Gateway foundation

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

## 3. Service, Route, Plugin

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

Route /api/audit/**
-> Service audit-log-service http://host.docker.internal:8082
```

Plugin có thể thử local:

- request-id plugin để đảm bảo request có correlation id;
- cors plugin nếu cần cho React Web;
- logging plugin awareness only nếu đã dùng Loki collector.

## 4. DB-less/declarative config

Kong có thể chạy DB-less: không cần database cho gateway config, config entity nằm trong file YAML/JSON declarative. Kong docs nói DB-less mode dùng in-memory storage và declarative configuration file; lợi ích là ít dependency và config có thể nằm trong Git.

Nguồn: [Kong DB-less mode docs](https://developer.konghq.com/gateway/db-less-mode/).

Với repo học local, DB-less là hướng tốt:

- Docker-first;
- không thêm Postgres riêng cho Kong;
- dễ review config;
- dễ reset.

## 5. Lab direction cho repo

Kong mini-lab nên làm:

```text
React Web
-> Kong :8000
-> tenant-demo/master-data-service :8080
```

Route ban đầu:

- `/api/master-data/**` -> `tenant-demo`.
- `/actuator/health` có thể route riêng nếu cần, nhưng tránh expose nhầm sensitive endpoints.

Route sau khi split service:

- `/api/audit/**` -> `audit-log-service`.

Yêu cầu:

- preserve `Authorization`;
- preserve hoặc generate `X-Request-Id`;
- không bypass backend security;
- Admin API không expose public trong production; local lab chỉ bind cẩn thận.

## 6. Production caveats

- Kong Admin API không được public bừa bãi.
- Production cần TLS, rate limit, access control, network boundary.
- Nếu validate JWT ở gateway, backend vẫn nên validate hoặc chỉ trust trong internal boundary rất chặt.
- Không assume enterprise plugins.
- Không thêm Kubernetes trong Phase 1.5.

## 7. Common mistakes

- Đưa business logic vào gateway.
- Nghĩ gateway auth thay thế service auth.
- Route quá rộng làm lộ actuator/private endpoints.
- Log Authorization header/token ở gateway.
- Mở CORS wildcard cho demo rồi quên caveat.
