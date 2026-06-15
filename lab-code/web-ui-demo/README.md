# React Web UI demo

Mini-lab này là React Web frontend để nhìn được flow Phase 1.5:

```text
React Web UI
-> Keycloak login
-> Kong Gateway :18000
-> tenant-demo :8080 / audit-log-service :8082 / file-service :8083 / search-service :8084
-> PostgreSQL / Redis / Kafka / MinIO / Elasticsearch / Observability
```

Đây không phải frontend production. UI là thin client gọi backend qua Gateway, nhưng product direction mới là một business app nhỏ:

```text
Master Data Portal
```

Mục tiêu UI cuối là quản lý master data, tệp tin tenant và activity log như một web product bình thường. Backend/infra vẫn được demo bằng lời nói, Loki/Kafka UI và docs, không phải bằng cách biến màn hình chính thành architecture dashboard.

Ghi chú: code hiện tại đã được đổi nhãn/IA sang `Master Data Portal`: Dashboard, Master Data, Files/Tệp tin, Activity Log, Account.

## Stack

- React Web + Vite.
- `keycloak-js` cho login OIDC.
- `fetch` gọi API qua Gateway.
- CSS thuần, không dùng UI framework.
- Single-page app nhiều màn hình bằng state nội bộ. Target IA mới: Welcome, Dashboard, Master Data, Files/Tệp tin, Activity Log, Account.

Không dùng React Native hoặc Expo trong repo này.

## Cấu hình local

Copy `.env.example` thành `.env` nếu cần đổi cấu hình:

```bash
cp .env.example .env
```

Giá trị mặc định:

```text
VITE_API_BASE_URL=http://localhost:18000
VITE_KEYCLOAK_URL=http://localhost:18080
VITE_KEYCLOAK_REALM=viettel-lab
VITE_KEYCLOAK_CLIENT_ID=tenant-demo-web
VITE_REQUEST_ID_PREFIX=web-demo
```

`VITE_API_BASE_URL` trỏ tới Gateway, không trỏ trực tiếp tới `tenant-demo`, `audit-log-service`, `file-service` hoặc `search-service`.

Mặc định Phase 1.5 dùng Kong Gateway ở `http://localhost:18000`. Spring Cloud Gateway lab cũ vẫn tồn tại trong repo để học route/filter concept, nhưng không còn là lựa chọn chính trong UI sản phẩm. Nếu cần so sánh lab cũ, có thể đổi env thủ công:

```text
VITE_API_BASE_URL=http://localhost:8081
```

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
make up
```

Mở:

```text
http://localhost:5173
```

Nếu muốn chạy foreground để nhìn log trực tiếp:

```bash
make -f Makefile.legacy web-ui-run
```

Build production artifact trong Docker:

```bash
make web-ui-build
```

Build output `dist/` chỉ nằm trong Docker image/layer; không commit `dist/` vào repo.

## Demo route

1. Start final demo stack:

   ```bash
   cd lab-code
   make up
   ```

   Target này bật Docker infra/tooling/web UI và bốn Java service chính ở background. Các target mini-lab cũ vẫn còn trong `Makefile.legacy`, ví dụ `make -f Makefile.legacy web-ui-up` hoặc `make -f Makefile.legacy app-run-logs`.

2. Trên UI:

   - Login bằng Keycloak.
   - Dashboard/Account: kiểm API base URL đang là Kong và user/tenant/role đúng.
   - Master Data: bấm `Load master data`.
   - Master Data: bấm `Load by code` với một code có thật như `LAPTOP-01`.
   - Master Data: tạo record với code `UI-DEMO-*`.
   - Master Data: sửa tên/loại/mã khi cần và thử tạm ngưng bản ghi. Tạm ngưng là soft delete/deactivate: bản ghi không còn hiện trong list/lookup thường, nhưng code cũ vẫn được giữ để tránh tái sử dụng nhầm trong cùng tenant.
   - Master Data: dùng `Tìm kiếm nâng cao` để gọi search-service qua Kong. Kết quả có thể trễ vài giây sau create/update/deactivate vì search là Elasticsearch projection từ Kafka event.
   - Tệp tin: upload file nhỏ, tải danh sách, tải xuống, thử viewer `403` nếu cần.
   - Activity Log: đợi một chút rồi bấm `Load activity`.
   - Demo docs: mở Grafana Loki/Kafka UI từ URL trong demo script nếu cần giải thích backend flow.
   - Khi cần debug, mở `Chi tiết kỹ thuật` để xem requestId.
   - Đối chiếu log `tenant-demo` và `audit-log-service` bằng requestId/event log.

Stop:

```bash
cd lab-code
make down
make clean-logs   # optional
```

## Quan sát backend integrations

- PostgreSQL: record được lưu qua `tenant-demo`.
- Redis: nếu cache enabled, dùng `Load by code` trên UI hoặc HTTP file cache để gọi cùng code hai lần và quan sát hit/miss bằng log/metric backend. UI không tự đoán cache status.
- Kafka: create/update/deactivate `master_data` phát `MasterDataChangedEvent` nếu messaging enabled.
- Tệp tin: bấm `Tải lên`/`Tải danh sách` để gọi file-service qua Kong; UI không gọi MinIO trực tiếp.
- Search: dùng `Tìm kiếm nâng cao` để gọi search-service qua Kong; UI không gọi Elasticsearch trực tiếp.
- Activity Log: bấm `Load activity` để đọc activity records qua Kong; tenant 2 không thấy activity tenant 1.
- Observability: Prometheus/Grafana quan sát metric từ `tenant-demo`, không phải UI gọi trực tiếp Prometheus/Grafana.

## Activity Log demo

Activity Log là cách UI nói về audit history. Bên dưới vẫn dùng API audit hiện có:

```text
GET ${VITE_API_BASE_URL}/api/audit-events
```

Expected behavior:

| User | Expected |
|---|---|
| `tenant1-user/password` | Load/create master data được; load activity tenant 1 được. |
| `tenant2-user/password` | Load master data được; create trả `403`; không thấy activity tenant 1. |

UI không kết luận Kafka/audit thành công sau POST. Chỉ khi `GET /api/audit-events` trả event thật thì mới coi audit đã lưu.

## Target screen direction

| Screen | Vai trò |
|---|---|
| Welcome | Login Keycloak, account hint local, không hiển thị token. |
| Dashboard | Business overview: total records, active records, recent changes, current tenant/role. |
| Master Data | Load/list, load by code, create, edit/update, soft delete/tạm ngưng, backend search qua search-service, `401`/`403`/`404`/`409`/unavailable states. |
| Files / Tệp tin | Upload, list metadata, download, delete qua `/api/files`; binary lưu ở MinIO, metadata ở file-service DB schema. |
| Activity Log | Activity table/timeline, tenant2 empty success state. Current API path remains `/api/audit-events`. |
| Account | Username, tenant_id, roles, token status hidden, API base URL và logout. |

Backend/observability links stay in docs or a small secondary demo note, not primary product navigation.

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

## Giới hạn hiện tại

- Browser token handling trong lab này chỉ để học local. Production SPA cần kiểm soát redirect URI, PKCE, CORS, token lifetime và logout kỹ hơn.
- Gateway đang dùng static route tới `tenant-demo`; service discovery/load balancing chỉ ở mức awareness.
- UI không làm authorization decision thật. Backend vẫn là security boundary.
- Không đưa token, password, secret hoặc private file vào repo.
