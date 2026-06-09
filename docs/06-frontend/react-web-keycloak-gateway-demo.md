# React Web + Keycloak + Gateway demo

Tài liệu này mô tả UI web nhỏ cho Phase 1. Mục tiêu là nhìn được flow kiến trúc end-to-end, không xây full frontend product.

```text
React Web UI
-> Keycloak login
-> API Gateway
-> tenant-demo backend
-> PostgreSQL / Redis / Kafka / Observability
```

## Vì sao React Web, không phải React Native/Expo?

Repo này là backend learning lab cho kiến trúc SaaS/ERP Phase 1. UI cuối nếu làm chỉ cần một SPA chạy trên browser để demo login, gọi API và quan sát request flow.

React Native/Expo thuộc hướng mobile app và là scope khác. Trong repo này, frontend đúng hướng là:

- React Web + Vite.
- `keycloak-js` cho OIDC login local.
- `fetch` gọi API qua Gateway.
- CSS đơn giản.

## Mental model của SPA login với Keycloak

1. User mở React Web UI.
2. UI redirect user sang Keycloak login.
3. Keycloak xác thực user trong realm `viettel-lab`.
4. Browser nhận access token cho public client `tenant-demo-web`.
5. UI gọi Gateway với:
   - `Authorization: Bearer <access-token>`
   - `X-Request-Id: <generated-id>`
6. Gateway route request sang `tenant-demo`.
7. `tenant-demo` validate JWT bằng issuer/JWKS, đọc `tenant_id`, map role, rồi chạy service/repository tenant-aware.

UI có thể đọc claim để hiển thị username/tenant, nhưng backend không được tin frontend. Tenant context đáng tin cậy chỉ đến từ JWT đã validate ở backend.

## Vì sao API request đi qua Gateway?

Trong target architecture, client không gọi từng backend service trực tiếp. Gateway là entry point để:

- route path tới backend phù hợp;
- giữ/cấp `X-Request-Id`;
- áp dụng CORS/rate limit/auth pre-check nếu production cần;
- che bớt topology nội bộ.

Trong mini-lab này Gateway chỉ static route:

```text
http://localhost:8081/api/**
-> http://localhost:8080/api/**
```

Gateway không chứa business logic và không thay thế backend security.

## RequestId và observability

UI sinh `X-Request-Id` cho mỗi API call. Gateway forward header này sang backend.

`tenant-demo` request logging filter ghi log với cùng requestId:

```text
method=GET path=/api/master-data status=200 requestId=web-demo-...
```

Nhờ đó khi demo có thể:

- nhìn requestId trên UI;
- tìm log backend cùng requestId;
- nếu Prometheus/Grafana đang chạy, quan sát metric backend tăng sau API call.

UI không gọi trực tiếp Redis, Kafka, PostgreSQL, MinIO, Prometheus hoặc Grafana trong business flow. Các thành phần này được quan sát gián tiếp qua backend.

## Keycloak setup tối thiểu

Trong realm `viettel-lab`, tạo public client:

| Setting | Giá trị gợi ý |
|---|---|
| Client ID | `tenant-demo-web` |
| Client authentication | Off / public client |
| Standard flow | On |
| Direct access grants | Off cho browser login |
| Valid redirect URIs | `http://localhost:5173/*` |
| Web origins | `http://localhost:5173` |
| PKCE | S256 nếu Keycloak UI yêu cầu/chọn được |

Token cần có:

- `iss`, `sub`, `exp` chuẩn JWT/OIDC;
- `tenant_id` claim;
- role claim nếu backend RBAC đang dùng `ADMIN`, `ACCOUNTANT`, `VIEWER`.

Nếu thiếu `tenant_id`, backend nên fail rõ ở tenant context flow.

## Gateway CORS local

Browser ở `http://localhost:5173` gọi Gateway ở `http://localhost:8081`, nên Gateway cần CORS local cho origin này.

Trong `lab-code/gateway-demo`, cấu hình chỉ cho phép origin local mặc định:

```text
WEB_UI_ORIGIN=http://localhost:5173
```

Production không nên mở CORS bừa bãi. Cần giới hạn origin thật, auth policy, TLS và network boundary.

## Demo script ngắn

1. Start infra:

   ```bash
   cd lab-code
   make infra-up
   ```

2. Start backend:

   ```bash
   cd lab-code
   make app-run
   ```

3. Start Gateway:

   ```bash
   cd lab-code
   make gateway-run
   ```

4. Start UI bằng Docker:

   ```bash
   cd lab-code
   make web-ui-up
   ```

5. Mở `http://localhost:5173`, login Keycloak, load/create `master_data`.

6. Đối chiếu:

   - UI status và requestId.
   - `tenant-demo` log có requestId.
   - Kafka publish/consume log nếu messaging enabled.
   - Redis cache hit/miss qua HTTP cache lab nếu cache enabled.
   - Prometheus/Grafana nếu observability lab đang chạy.

## Caveats

- Đây là local learning UI, không phải production SPA.
- Không display full access token trong UI.
- Không lưu token vào localStorage thủ công trong lab này.
- Backend vẫn là nơi quyết định authorization và tenant data scope.
- CORS, redirect URI, PKCE, logout/session, refresh token policy cần học sâu hơn nếu làm frontend production thật.
