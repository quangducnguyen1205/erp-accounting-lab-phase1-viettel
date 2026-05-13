# curl và HTTP Client khi test API backend

## curl là gì?

`curl` là công cụ dòng lệnh để gửi HTTP request. Backend engineer nên biết mức cơ bản vì `curl` giúp kiểm tra API nhanh mà không cần frontend, Swagger UI hoặc Postman.

Trong repo này, `curl` hữu ích để verify API multi-tenant:

- request có `X-Tenant-Id: 1` chỉ thấy dữ liệu tenant 1;
- request có `X-Tenant-Id: 2` chỉ thấy dữ liệu tenant 2;
- request thiếu hoặc sai `X-Tenant-Id` bị chặn;
- endpoint tìm theo `code` hoặc `category` vẫn scoped theo tenant hiện tại.

## Khi nào dùng curl, Swagger UI, frontend?

| Công cụ | Phù hợp khi | Lưu ý |
|---|---|---|
| `curl` | Cần verify nhanh, copy command vào report, debug header/status code | Ít trực quan, dễ sai quote/header nếu mới dùng |
| Swagger UI | API đã có OpenAPI/Swagger, muốn thử thủ công bằng giao diện | Phase hiện tại chưa thêm Swagger để tránh mở scope |
| Frontend | Cần verify flow người dùng thật | Nếu bug xảy ra, vẫn nên dùng curl để tách lỗi frontend/backend |
| IntelliJ HTTP Client | Cần chạy lại nhiều request giống nhau trong IDE | Rất hợp với learning repo vì lưu request thành file `.http` |

## Cấu trúc lệnh curl cơ bản

```bash
curl [options] "URL"
```

Ví dụ:

```bash
curl -i -H "X-Tenant-Id: 1" "http://localhost:8080/api/master-data"
```

Ý nghĩa:

- `curl`: gửi request.
- `-i`: in cả response headers và response body.
- `-H "X-Tenant-Id: 1"`: gửi header tenant.
- URL: endpoint backend cần test.

## Các option hay dùng

| Option | Ý nghĩa | Khi dùng |
|---|---|---|
| `-i` | In response headers + body | Muốn xem status code/header nhanh |
| `-v` | Verbose: in chi tiết request/response | Debug kết nối/header, nhưng log dài |
| `-X` | Chỉ định HTTP method | `POST`, `PUT`, `DELETE`; GET thường không cần |
| `-H` | Gửi header | `X-Tenant-Id`, `Content-Type`, `Authorization` |
| `-d` | Gửi request body | POST/PUT JSON ngắn |
| `--data-raw` | Gửi raw body, rõ hơn với JSON dài | Khi body có ký tự đặc biệt |
| `-o` | Ghi response ra file | Khi response dài |
| `-s` | Silent mode | Dùng chung với `-w` để lấy output gọn |
| `-w` | In thêm format như status code | Report ngắn, không paste body dài |

Ví dụ lấy status code gọn:

```bash
curl -s -o /tmp/master-data.json -w "%{http_code}\n" \
  -H "X-Tenant-Id: 1" \
  "http://localhost:8080/api/master-data"
```

## Test GET endpoint với `X-Tenant-Id`

List data tenant 1:

```bash
curl -i -H "X-Tenant-Id: 1" "http://localhost:8080/api/master-data"
```

List data tenant 2:

```bash
curl -i -H "X-Tenant-Id: 2" "http://localhost:8080/api/master-data"
```

Tìm theo code:

```bash
curl -i -H "X-Tenant-Id: 1" "http://localhost:8080/api/master-data/code/LAPTOP-01"
```

Tìm theo category:

```bash
curl -i -H "X-Tenant-Id: 1" "http://localhost:8080/api/master-data/category/ASSET"
```

## Test lỗi missing/invalid header

Thiếu tenant header:

```bash
curl -i "http://localhost:8080/api/master-data"
```

Tenant header không hợp lệ:

```bash
curl -i -H "X-Tenant-Id: abc" "http://localhost:8080/api/master-data"
```

Expected pattern:

- thiếu `X-Tenant-Id` -> HTTP `400`;
- `X-Tenant-Id` không phải số dương -> HTTP `400`;
- tenant hợp lệ -> request đi vào controller/service.

## Test POST JSON

Khi gửi JSON, cần `Content-Type: application/json`.

```bash
curl -i -X POST "http://localhost:8080/api/master-data" \
  -H "X-Tenant-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{"code":"DEMO-01","name":"Demo item","category":"ASSET","isActive":true}'
```

Trong lab này, client không cần gửi `tenantId` trong body. Tenant nên đến từ context đáng tin cậy như `TenantContext`, hiện tại được set bởi `TenantFilter`.

## Cách đọc response

Khi dùng `-i`, output thường có:

```text
HTTP/1.1 200
Content-Type: application/json
...

[{"tenantId":1,"id":1,"code":"LAPTOP-01",...}]
```

Đọc theo thứ tự:

1. Status code: `200`, `201`, `400`, `404`.
2. Response headers: `Content-Type`, cache/header khác nếu có.
3. Response body: JSON trả về.

Với report, chỉ cần ghi pattern:

- tenant 1 -> `200`, chỉ có `tenantId = 1`;
- tenant 2 -> `200`, chỉ có `tenantId = 2`;
- thiếu/sai tenant header -> `400`;
- tenant 1 truy cập id của tenant 2 -> `404`.

Không cần paste toàn bộ raw log hoặc Hibernate SQL log dài.

## IntelliJ HTTP Client

IntelliJ IDEA có HTTP Client tích hợp, dùng các file `.http`. Với learning repo, `.http` thường dễ dùng hơn `curl` khi phải chạy lại nhiều request.

Gợi ý file:

```text
lab-code/tenant-demo/http/master-data-api.http
```

Ưu điểm:

- lưu request ngay trong repo;
- chạy từng request bằng nút Run trong IntelliJ;
- dễ đổi biến như `baseUrl`, `tenantId`, `code`;
- không cần Postman cho giai đoạn hiện tại.

Thông thường IntelliJ IDEA đã có HTTP Client built-in. Nếu IDE thiếu tính năng này, kiểm tra plugin HTTP Client của JetBrains trong settings, nhưng không cần thêm dependency vào project.

## Common mistakes

- Quên quote URL khi URL có ký tự đặc biệt.
- Quên `X-Tenant-Id`.
- Nhầm `GET` và `POST`.
- Gửi JSON nhưng quên `Content-Type: application/json`.
- App chưa chạy hoặc chạy sai port.
- Docker PostgreSQL chưa chạy nên app không start.
- Paste log quá dài vào report thay vì tóm tắt status code và behavior.
- Thấy `200` rồi kết luận pass quá sớm: multi-tenant phải kiểm tra body có đúng tenant không.
