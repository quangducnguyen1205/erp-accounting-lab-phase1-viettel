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
   - Tạo record với code `UI-DEMO-*`.
   - Xem `requestId` sau request.
   - Đối chiếu log `tenant-demo` bằng requestId đó.

## Quan sát backend integrations

- PostgreSQL: record được lưu qua `tenant-demo`.
- Redis: nếu cache enabled, dùng HTTP file cache để quan sát hit/miss by code.
- Kafka: create/update `master_data` phát `MasterDataChangedEvent` nếu messaging enabled.
- Observability: Prometheus/Grafana quan sát metric từ `tenant-demo`, không phải UI gọi trực tiếp Prometheus/Grafana.

## Caveats

- Browser token handling trong lab này chỉ để học local. Production SPA cần kiểm soát redirect URI, PKCE, CORS, token lifetime và logout kỹ hơn.
- Gateway đang dùng static route tới `tenant-demo`; service discovery/load balancing chỉ ở mức awareness.
- UI không làm authorization decision thật. Backend vẫn là security boundary.
- Không đưa token, password, secret hoặc private file vào repo.
