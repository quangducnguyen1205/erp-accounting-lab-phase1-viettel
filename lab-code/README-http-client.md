# IntelliJ HTTP Client local workflow

## Mục tiêu

Các file `.http` trong repo dùng chung một bộ tên biến để dễ chạy demo bằng IntelliJ HTTP Client mà không commit token thật.

## File env local

Copy file mẫu:

```bash
cd lab-code
cp http-client.env.example.json http-client.env.json
```

`http-client.env.json` và `http-client.private.env.json` đã được `.gitignore`, không commit hai file này.

## Biến chuẩn

| Biến | Ý nghĩa |
|---|---|
| `keycloak_base_url` | Keycloak local, mặc định `http://localhost:18080`. |
| `keycloak_realm` | Realm demo, mặc định `viettel-lab`. |
| `keycloak_client_id` | Client dùng password grant local, mặc định `tenant-demo-api-client`. |
| `kong_base_url` | Gateway chính của demo, mặc định `http://localhost:18000`. |
| `tenant_demo_base_url` | Direct URL của `tenant-demo`, mặc định `http://localhost:8080`. |
| `audit_log_base_url` | Direct URL của `audit-log-service`, mặc định `http://localhost:8082`. |
| `file_service_base_url` | Direct URL của `file-service`, mặc định `http://localhost:8083`. |
| `search_service_base_url` | Direct URL của `search-service`, mặc định `http://localhost:8084`. |
| `tenant1_token` | Access token của `tenant1-user` role `ACCOUNTANT`. |
| `tenant2_token` | Access token của `tenant2-user` role `VIEWER`. |
| `admin_token` | Access token của `platform-admin` role `ADMIN`. |

## Lấy token

Chạy:

```text
lab-code/keycloak-lab/http/keycloak-token-flow.http
```

File đó có request lấy token cho:

- `tenant1-user / password`;
- `tenant2-user / password`;
- `platform-admin / password`.

Sau khi lấy token, copy `access_token` vào env local tương ứng. Không paste token thật vào file `.http` đã commit.

## Luồng kiểm thử nhanh

1. `cd lab-code && make up`.
2. Lấy token bằng `keycloak-token-flow.http`.
3. Chạy `tenant-demo/http/master-data-api.http`.
4. Chạy `file-service/http/file-api.http`.
5. Chạy `search-service/http/search-api.http`.
6. Chạy `audit-log-service/http/audit-api.http`.

Reindex search chỉ dùng cho vận hành/manual test:

```text
POST {{kong_base_url}}/api/search/master-data/reindex
Authorization: Bearer {{admin_token}}
```

Endpoint này không có nút trên React UI.
