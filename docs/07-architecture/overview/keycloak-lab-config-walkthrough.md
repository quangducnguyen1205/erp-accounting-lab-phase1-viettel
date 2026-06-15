# Keycloak Lab Config Walkthrough

Doc này giải thích các file runtime của Keycloak local lab trong repo. Mục tiêu là đọc được config, không chỉ biết chạy lệnh.

## Big Picture

```text
Browser / HTTP client
  -> http://localhost:18080
  -> Keycloak container
  -> Keycloak PostgreSQL container
  -> named volume viettel-keycloak-postgres-data

setup-keycloak-demo.sh
  -> kcadm.sh inside Keycloak container
  -> create/update realm, clients, roles, users, tenant_id mapper
```

Keycloak phát token. `tenant-demo` chỉ validate issuer/JWKS/token claim, không đọc trực tiếp database Keycloak.

## File Map

| File | Vai trò | Khi nào sửa? | Không nên đặt gì vào đây? |
|---|---|---|---|
| `lab-code/keycloak-lab/docker-compose.yml` | Chạy Keycloak và PostgreSQL riêng của Keycloak | Đổi image/port/DB/volume local | Secret production |
| `lab-code/keycloak-lab/setup-keycloak-demo.sh` | Bootstrap realm/client/user/role/mapper local | Thêm demo user/role/client local | Access token thật |
| `lab-code/keycloak-lab/README.md` | Lệnh chạy và setup nhanh | Khi workflow thay đổi | Log dài/token |
| `lab-code/Makefile` | Target Docker-first cho lab | Khi thêm target setup/reset/status | Lệnh reset mơ hồ |

## `docker-compose.yml`

### Service `keycloak-postgres`

Service này là database riêng cho Keycloak.

Các field chính:

- `image: postgres:16-alpine`: PostgreSQL nhẹ cho local.
- `container_name: viettel-keycloak-postgres`: tên dễ nhận ra khi `docker ps`.
- `POSTGRES_DB=keycloak`: database Keycloak dùng.
- `POSTGRES_USER=keycloak`, `POSTGRES_PASSWORD=keycloak`: credential local-only.
- `volumes: viettel-keycloak-postgres-data:/var/lib/postgresql/data`: giữ dữ liệu sau khi container dừng.
- `healthcheck`: giúp Keycloak chờ DB sẵn sàng.

Không dùng database này cho `tenant-demo`. App business vẫn dùng PostgreSQL riêng trong `lab-code/docker/`.

### Service `keycloak`

Service này chạy Keycloak dev mode.

Các field chính:

- `image: quay.io/keycloak/keycloak:26.6.1`: version Keycloak local.
- `command: start-dev`: tiện cho local lab, không phải production mode.
- `KC_BOOTSTRAP_ADMIN_USERNAME=admin`, `KC_BOOTSTRAP_ADMIN_PASSWORD=admin`: admin local-only.
- `KC_DB=postgres`: Keycloak dùng PostgreSQL thay vì storage disposable.
- `KC_DB_URL=jdbc:postgresql://keycloak-postgres:5432/keycloak`: container Keycloak gọi DB bằng service name trong Docker network.
- `KC_DB_USERNAME`, `KC_DB_PASSWORD`: credential local-only đến DB Keycloak.
- `KC_HEALTH_ENABLED=true`, `KC_METRICS_ENABLED=true`: bật endpoint health/metrics của Keycloak nếu cần inspect.
- `ports: 18080:8080`: browser trên host mở `http://localhost:18080`.
- `depends_on`: chờ PostgreSQL healthy trước khi start Keycloak.

## Named Volume

```yaml
volumes:
  viettel-keycloak-postgres-data:
```

Volume này giữ realm/client/user/role sau khi chạy `make keycloak-down`. Chỉ mất dữ liệu khi chạy target destructive `make -f Makefile.legacy keycloak-reset`.

## `setup-keycloak-demo.sh`

Script dùng `kcadm.sh` bên trong container Keycloak:

```text
host shell
  -> docker exec viettel-keycloak /opt/keycloak/bin/kcadm.sh ...
```

Script tạo/cập nhật:

- realm `viettel-lab`;
- User Profile attribute `tenant_id`;
- client `tenant-demo-api-client` cho backend/API token lab;
- client `tenant-demo-web` cho React Web SPA;
- roles `ADMIN`, `ACCOUNTANT`, `VIEWER`;
- users `tenant1-user` và `tenant2-user`;
- mapper `tenant_id`.

Vì backend hiện đọc được cả realm roles và client roles, script assign role ở cả hai nơi để demo linh hoạt. Trong production, team nên chọn role model rõ ràng hơn.

Keycloak 26.x có Declarative User Profile. Nếu không khai báo `tenant_id`, thao tác update user có thể không lưu custom attribute như mong đợi. Vì vậy script cấu hình `tenant_id` trong User Profile trước khi tạo/cập nhật user.

## Makefile Targets

Từ `lab-code/`:

```bash
make keycloak-up       # start Keycloak + DB, giữ volume
make -f Makefile.legacy keycloak-status   # xem container
make -f Makefile.legacy keycloak-logs     # xem log
make keycloak-setup    # bootstrap realm/client/user/role
make keycloak-down     # dừng container, giữ volume
make -f Makefile.legacy keycloak-reset    # DESTRUCTIVE: xóa volume Keycloak DB
```

`keycloak-reset` cố ý có tên rõ vì nó xóa realm/client/user local. Không dùng target này khi chỉ muốn restart.

## Cách Verify

```bash
cd lab-code
make keycloak-up
make keycloak-setup
make -f Makefile.legacy keycloak-status
```

Sau đó mở:

```text
http://localhost:18080
```

Kiểm tra:

- realm `viettel-lab` tồn tại;
- client `tenant-demo-api-client` và `tenant-demo-web` tồn tại;
- users `tenant1-user`, `tenant2-user` tồn tại;
- access token có `tenant_id`;
- role claim có `ACCOUNTANT` hoặc `VIEWER`.

## Common Mistakes

- Chạy `docker compose down -v` rồi quên chạy lại bootstrap.
- Tạo web client là confidential client nên browser login lỗi `invalid_client`.
- Quên redirect URI `http://localhost:5173/*` cho React Web.
- Quên mapper `tenant_id`, làm backend không set được tenant context.
- Quên User Profile attribute `tenant_id`, làm Admin REST/Console không lưu custom attribute trong Keycloak 26.x.
- Nghĩ Keycloak role thay thế tenant-aware repository query. Role chỉ trả lời "được làm gì", còn tenant filter vẫn nằm ở backend query/service.

## Giới hạn production

- Không dùng `start-dev`, `admin/admin`, `password/password` ở production.
- DB Keycloak cần backup/restore và monitoring riêng.
- Realm/client config nên quản lý bằng quy trình IaC/migration.
- Admin API/Admin Console cần bảo vệ nghiêm túc.
- Web client public cần Authorization Code + PKCE, redirect URI chặt, HTTPS.
