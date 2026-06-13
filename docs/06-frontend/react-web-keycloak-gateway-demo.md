# React Web + Keycloak + Gateway demo

Tài liệu này mô tả UI web nhỏ cho Phase 1. Mục tiêu là nhìn được flow kiến trúc end-to-end, không xây full frontend product.

```text
React Web UI
-> Keycloak login
-> Kong Gateway
-> tenant-demo backend / audit-log-service
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
8. Với audit view, Kong route `GET /api/audit-events` sang `audit-log-service`; service này validate JWT riêng và filter audit events theo `tenant_id`.

UI có thể đọc claim để hiển thị username/tenant, nhưng backend không được tin frontend. Tenant context đáng tin cậy chỉ đến từ JWT đã validate ở backend.

## Vì sao API request đi qua Gateway?

Trong target architecture, client không gọi từng backend service trực tiếp. Gateway là entry point để:

- route path tới backend phù hợp;
- giữ/cấp `X-Request-Id`;
- áp dụng CORS/rate limit/auth pre-check nếu production cần;
- che bớt topology nội bộ.

Trong Phase 1.5, main UI path dùng Kong Gateway:

```text
http://localhost:18000/api/master-data...
-> http://localhost:8080/api/master-data...

http://localhost:18000/api/audit-events...
-> http://localhost:8082/api/audit-events...
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

Mặc định UI demo hiện dùng Kong Gateway ở `http://localhost:18000`. Spring Cloud Gateway lab cũ vẫn tồn tại để so sánh route/filter concept; UI có quick preset để đổi về `http://localhost:8081` khi cần.

## Keycloak setup tối thiểu

Đường khuyến nghị hiện tại là chạy bootstrap local:

```bash
cd lab-code
make keycloak-up
make keycloak-setup
```

Bootstrap tạo realm `viettel-lab`, API client `tenant-demo-api-client`, Web public client `tenant-demo-web`, users/roles và mapper `tenant_id`. Nếu cần debug bằng Admin Console, cấu hình tối thiểu cho Web client là:

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

Nếu reset volume Keycloak, chạy lại:

```bash
cd lab-code
make keycloak-up
make keycloak-setup
```

Nếu làm thủ công, checklist ngắn:

1. Realm: `viettel-lab`.
2. Client: `tenant-demo-web`.
3. Client authentication: off/public client.
4. Standard flow: on.
5. Valid redirect URIs: `http://localhost:5173/*`.
6. Web origins: `http://localhost:5173`.
7. User `tenant1-user`: password local `password`, `tenant_id=1`, role `ACCOUNTANT`.
8. User `tenant2-user`: password local `password`, `tenant_id=2`, role `VIEWER`.

Lưu ý quan trọng về role:

- UI client là `tenant-demo-web`.
- Backend đang đọc role từ `realm_access.roles` hoặc `resource_access.<KEYCLOAK_CLIENT_ID>.roles`.
- Nếu `KEYCLOAK_CLIENT_ID=tenant-demo-api-client`, role chỉ nằm dưới `resource_access.tenant-demo-web.roles` thì backend vẫn trả `403`.
- Cách demo đơn giản: gán realm role `ACCOUNTANT`/`VIEWER`, hoặc cấu hình role dưới đúng API client mà backend đọc.

User demo kỳ vọng:

| User | tenant_id | Role demo |
|---|---:|---|
| `tenant1-user` | `1` | `ACCOUNTANT` |
| `tenant2-user` | `2` | `VIEWER` |

Nếu ghi nhầm user thứ hai là `tenant1-user` thì coi đó là typo. Case tenant 2 nên dùng `tenant2-user`.

## Gateway CORS local

Browser ở `http://localhost:5173` gọi Gateway ở `http://localhost:18000`, nên Kong cần CORS local cho origin này. Spring Cloud Gateway legacy lab cũng cần CORS nếu đổi API base về `http://localhost:8081`.

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
   make app-run-logs
   ```

3. Start audit service và Kong. Audit service là Java service Maven/host-run:

   ```bash
   cd lab-code
   make audit-log-run-logs
   ```

   Mở terminal khác cho Kong:

   ```bash
   cd lab-code
   make kong-up
   ```

4. Start UI bằng Docker:

   ```bash
   cd lab-code
   make web-ui-up
   ```

5. Mở `http://localhost:5173`, login Keycloak, load/create `master_data`.
6. Dùng `Load by code` để gọi `GET /api/master-data/code/{code}` qua Gateway. Nếu Redis enabled, gọi cùng code hai lần rồi kiểm log/metric backend để thấy miss/hit; UI không tự kết luận cache status.
7. Bấm `Load audit events` để gọi `GET /api/audit-events` qua Kong tới `audit-log-service`.

6. Đối chiếu:

   - UI status và requestId.
   - `tenant-demo` log có requestId.
   - Kafka publish/consume log nếu messaging enabled.
   - Audit event xuất hiện trong UI sau khi audit service consume/store.
   - Redis cache hit/miss qua `Load by code` hoặc HTTP cache lab nếu cache enabled.
   - Prometheus/Grafana nếu observability lab đang chạy.

## Khi redirect về UI nhưng vẫn thấy Guest

Checklist debug:

1. Nhìn panel `Keycloak login`:
   - `authenticated=true/false`
   - `access token=available/missing`
   - `tenant_id`
   - roles
2. Mở browser console/network:
   - lỗi `invalid_client` thường là sai client id;
   - lỗi `invalid_client_credentials` sau login thường là client SPA còn bật client authentication/confidential;
   - lỗi `redirect_uri` thường là thiếu `http://localhost:5173/*`;
   - lỗi CORS/web origin thường là thiếu `http://localhost:5173`;
   - lỗi `A 'Keycloak' instance can only be initialized once` nghĩa là frontend init Keycloak adapter lặp trong React dev mode.
3. Nếu UI authenticated nhưng API trả `403`, kiểm role nằm ở `realm_access.roles` hay đúng `resource_access.<KEYCLOAK_CLIENT_ID>.roles`.
4. Không debug bằng cách paste full access token vào docs/log. Chỉ decode local rồi kiểm claim cần thiết.

Trong UI demo, `keycloak.init(...)` được gọi qua helper idempotent để React Strict Mode không làm adapter bị init hai lần.

## Caveats

- Đây là local learning UI, không phải production SPA.
- Không display full access token trong UI.
- Không lưu token vào localStorage thủ công trong lab này.
- Backend vẫn là nơi quyết định authorization và tenant data scope.
- CORS, redirect URI, PKCE, logout/session, refresh token policy cần học sâu hơn nếu làm frontend production thật.
