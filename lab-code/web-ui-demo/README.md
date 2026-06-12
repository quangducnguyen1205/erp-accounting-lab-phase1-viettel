# React Web UI demo

Mini-lab này là UI web rất nhỏ để nhìn được flow Phase 1:

```text
React Web UI
-> Keycloak login
-> Spring Cloud Gateway :8081
-> tenant-demo :8080
-> PostgreSQL / Redis / Kafka / Observability
```

Đây không phải frontend production. UI chỉ là thin client để demo kiến trúc backend.

## Stack

- React Web + Vite.
- `keycloak-js` cho login OIDC.
- `fetch` gọi API qua Gateway.
- CSS thuần, không dùng UI framework.

Không dùng React Native hoặc Expo trong repo này.

## Cấu hình local

Copy `.env.example` thành `.env` nếu cần đổi cấu hình:

```bash
cp .env.example .env
```

Giá trị mặc định:

```text
VITE_API_BASE_URL=http://localhost:8081
VITE_KEYCLOAK_URL=http://localhost:18080
VITE_KEYCLOAK_REALM=viettel-lab
VITE_KEYCLOAK_CLIENT_ID=tenant-demo-web
VITE_REQUEST_ID_PREFIX=web-demo
```

`VITE_API_BASE_URL` trỏ tới Gateway, không trỏ trực tiếp tới `tenant-demo`.

## Keycloak client cần chuẩn bị

Đường khuyến nghị hiện tại là bootstrap tự động:

```bash
cd lab-code
make keycloak-up
make keycloak-setup
```

Script này tạo realm, API client, Web client, role, user và `tenant_id` mapper cho local demo. Nếu muốn hiểu Admin Console hoặc debug thủ công, checklist bên dưới là cấu hình tối thiểu cần có.

Trong realm `viettel-lab`, tạo public client cho SPA:

| Setting | Giá trị gợi ý |
|---|---|
| Client ID | `tenant-demo-web` |
| Client type | OpenID Connect |
| Client authentication | Off / public client |
| Standard flow | On |
| Direct access grants | Off nếu chỉ dùng browser login |
| Valid redirect URIs | `http://localhost:5173/*` |
| Web origins | `http://localhost:5173` |
| PKCE | Bật hoặc yêu cầu `S256` nếu UI Keycloak hiện setting này |

Claim cần có trong access token:

- `tenant_id`
- role claim nếu backend đang kiểm tra RBAC, ví dụ `ADMIN`, `ACCOUNTANT`, `VIEWER`.

Backend vẫn validate token qua issuer/JWKS và tự enforce tenant isolation. UI không được tin là nguồn tenant đáng tin cậy.

Nếu reset volume Keycloak, ưu tiên chạy lại:

```bash
cd lab-code
make keycloak-up
make keycloak-setup
```

Nếu cấu hình tay trong Admin Console, checklist tối thiểu:

1. Vào realm `viettel-lab`.
2. Tạo hoặc kiểm tra client `tenant-demo-web`.
3. Đặt `Client authentication = Off` để client là public SPA.
4. Bật `Standard flow`.
5. Đặt `Valid redirect URIs = http://localhost:5173/*`.
6. Đặt `Web origins = http://localhost:5173`.
7. Gán `tenant1-user` attribute `tenant_id=1` và role `ACCOUNTANT`.
8. Gán `tenant2-user` attribute `tenant_id=2` và role `VIEWER`.
9. Login lại hoặc bấm `Refresh token` để token mới chứa claim/role mới.

Hai lỗi setup dễ gặp:

- Login được nhưng redirect về UI báo `Server responded with an invalid status`: kiểm tra `tenant-demo-web` có đang là public client không. Nếu client còn confidential/client authentication on, browser token exchange sẽ bị Keycloak log `invalid_client_credentials`.
- Login được nhưng gọi API trả `403`: token hợp lệ nhưng backend không thấy role đúng. Với repo này, role phải nằm ở `realm_access.roles` hoặc `resource_access.<KEYCLOAK_CLIENT_ID>.roles`. Nếu chỉ gán role dưới client `tenant-demo-web` trong khi backend đang đọc `tenant-demo-api-client`, API sẽ bị `403`.

User demo kỳ vọng trong lab hiện tại:

| User | Password local | tenant_id | Role demo |
|---|---|---:|---|
| `tenant1-user` | `password` | `1` | `ACCOUNTANT` |
| `tenant2-user` | `password` | `2` | `VIEWER` |

Nếu thấy ghi nhầm `tenant1-user` cho cả hai tenant thì đó là typo; user tenant 2 đúng là `tenant2-user`.

## Chạy UI bằng Docker

Repo này dùng workflow Docker-first, không yêu cầu local `npm`.

Từ thư mục `lab-code/`:

```bash
make web-ui-info
make web-ui-up
```

Mở:

```text
http://localhost:5173
```

Nếu muốn chạy foreground để nhìn log trực tiếp:

```bash
make web-ui-run
```

Build production artifact trong Docker:

```bash
make web-ui-build
```

Build output `dist/` chỉ nằm trong Docker image/layer; không commit `dist/` vào repo.

## Demo route

1. Start infra cần thiết:

   ```bash
   cd lab-code
   make infra-up
   ```

2. Start `tenant-demo` ở Keycloak mode:

   ```bash
   cd lab-code
   make app-run
   ```

   File `tenant-demo/.env` nên có `APP_AUTH_MODE=keycloak` và issuer URI đúng với Keycloak local.

3. Start Gateway:

   ```bash
   cd lab-code
   make gateway-run
   ```

4. Start UI:

   ```bash
   cd lab-code
   make web-ui-up
   ```

5. Trên UI:

   - Login bằng Keycloak.
   - Bấm `Load master data`.
   - Bấm `Load by code` với một code có thật như `LAPTOP-01`.
   - Tạo record với code `UI-DEMO-*`.
   - Xem `requestId` sau request.
   - Đối chiếu log `tenant-demo` bằng requestId đó.

## Quan sát backend integrations

- PostgreSQL: record được lưu qua `tenant-demo`.
- Redis: nếu cache enabled, dùng `Load by code` trên UI hoặc HTTP file cache để gọi cùng code hai lần và quan sát hit/miss bằng log/metric backend. UI không tự đoán cache status.
- Kafka: create/update `master_data` phát `MasterDataChangedEvent` nếu messaging enabled.
- Observability: Prometheus/Grafana quan sát metric từ `tenant-demo`, không phải UI gọi trực tiếp Prometheus/Grafana.

## Debug login state

Sau khi redirect từ Keycloak về UI, panel `Keycloak login` hiển thị vài field an toàn:

- `authenticated`: adapter Keycloak đã xác thực chưa.
- `access token`: token có sẵn trong memory chưa, nhưng không hiển thị token.
- `tenant_id`, roles và thời điểm token hết hạn nếu login thành công.

Nếu login xong nhưng nút vẫn disabled:

1. Mở browser console.
2. Tìm lỗi Keycloak/CORS/redirect, ví dụ `invalid_client`, `redirect_uri`, hoặc `A 'Keycloak' instance can only be initialized once`.
3. Kiểm tra client `tenant-demo-web` có `Valid redirect URIs = http://localhost:5173/*` và `Web origins = http://localhost:5173`.
4. Kiểm tra `tenant-demo-web` là public client: client authentication off.
5. Lấy token mới rồi kiểm tra claim `tenant_id`/role bằng công cụ decode local, không commit token vào repo.
6. Nếu vừa đổi role/mapper trong Keycloak, bấm `Refresh token`; UI demo force refresh access token để lấy claim mới.

Code UI đã làm `keycloak.init(...)` idempotent để React Strict Mode trong Vite dev không init cùng một Keycloak adapter hai lần.

## Caveats

- Browser token handling trong lab này chỉ để học local. Production SPA cần kiểm soát redirect URI, PKCE, CORS, token lifetime và logout kỹ hơn.
- Gateway đang dùng static route tới `tenant-demo`; service discovery/load balancing chỉ ở mức awareness.
- UI không làm authorization decision thật. Backend vẫn là security boundary.
- Không đưa token, password, secret hoặc private file vào repo.
