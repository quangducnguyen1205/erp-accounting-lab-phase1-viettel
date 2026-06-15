# Keycloak Local Persistence And Bootstrap

## Vì sao cần sửa Keycloak local?

Ban đầu Keycloak chỉ là mini-lab để học OIDC: chạy container, tạo realm/client/user thủ công, lấy token rồi gọi backend. Khi repo bước sang demo end-to-end, Keycloak không còn là phần phụ tạm thời nữa. React Web UI, Gateway và `tenant-demo` đều phụ thuộc token/role/claim từ Keycloak.

Nếu mỗi lần reset container lại phải tạo lại realm, client, user, role và mapper bằng tay thì demo rất dễ gãy. Vì vậy lab cần hai thứ:

- **Persistence**: dữ liệu Keycloak được lưu trong PostgreSQL volume riêng.
- **Bootstrap**: có script tạo lại realm/client/user/role/mapper sau khi reset volume.

## Kiến trúc local mới

```text
lab-code/keycloak-lab/
  keycloak container
    -> dùng PostgreSQL riêng của Keycloak

  keycloak-postgres container
    -> volume viettel-keycloak-postgres-data
```

Keycloak không dùng chung database với `tenant-demo`. Đây là cách sạch hơn vì IAM data và business data là hai loại dữ liệu khác nhau.

## Persistence khác gì bootstrap?

Persistence nghĩa là khi chạy:

```bash
cd lab-code
make keycloak-down
make keycloak-up
```

realm/client/user vẫn còn vì volume PostgreSQL chưa bị xóa.

Bootstrap nghĩa là khi cố ý xóa volume:

```bash
cd lab-code
make -f Makefile.legacy keycloak-reset
make keycloak-up
make keycloak-setup
```

script sẽ tạo lại các cấu hình local demo tối thiểu.

## Bootstrap tạo gì?

Script `lab-code/keycloak-lab/setup-keycloak-demo.sh` tạo/cập nhật:

- realm `viettel-lab`;
- API client `tenant-demo-api-client`;
- Web SPA client `tenant-demo-web`;
- roles `ADMIN`, `ACCOUNTANT`, `VIEWER`;
- users:
  - `tenant1-user / password`, `tenant_id=1`, role `ACCOUNTANT`;
  - `tenant2-user / password`, `tenant_id=2`, role `VIEWER`;
- User Profile attribute `tenant_id` để Keycloak 26.x cho phép lưu custom attribute này;
- mapper `tenant_id` để claim xuất hiện trong access token;
- role mapping ở realm role và API client role để backend role converter đọc được.

Script cố gắng idempotent ở mức lab: chạy lại không nên phá cấu hình hiện có. Đây không phải migration tool production.

## Vì sao vẫn giữ admin/admin và password/password?

Đây là local learning lab. Các credential này chỉ dùng trên máy local và đã được ghi rõ là dev-only. Không dùng credential kiểu này cho môi trường thật.

## Reset an toàn

Lệnh bình thường:

```bash
make keycloak-down
```

chỉ dừng container, không xóa dữ liệu.

Lệnh phá dữ liệu có chủ đích:

```bash
make -f Makefile.legacy keycloak-reset
```

sẽ chạy `docker compose down -v` trong `keycloak-lab`, tức là xóa named volume PostgreSQL của Keycloak. Sau reset phải chạy lại:

```bash
make keycloak-up
make keycloak-setup
```

## Production caveats

- Keycloak production không nên chạy `start-dev`.
- Cần HTTPS, hostname chính xác, backup DB, rotation secret/key, hardening Admin Console.
- Không expose Admin API tùy tiện.
- Realm/client/user nên được quản lý bằng IaC/config migration nghiêm túc hơn.
- User federation/LDAP/SSO, session management và audit IAM là các chủ đề production riêng.
