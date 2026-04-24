# Tổng quan Multi-tenant

## Tenant là gì?

Trong hệ thống SaaS, tenant là một khách hàng tổ chức sử dụng nền tảng. Với bài toán ERP/kế toán, một tenant thường tương ứng với một doanh nghiệp.

Một tenant có:

- Người dùng riêng.
- Dữ liệu kế toán riêng.
- Cấu hình riêng.
- Vai trò và quyền riêng.
- Gói dịch vụ hoặc feature flag riêng.

```text
Nền tảng ERP/kế toán SaaS
├── Tenant A: Công ty A
│   ├── users
│   ├── dữ liệu kế toán
│   └── cấu hình
├── Tenant B: Công ty B
│   ├── users
│   ├── dữ liệu kế toán
│   └── cấu hình
└── Tenant C: Công ty C
    ├── users
    ├── dữ liệu kế toán
    └── cấu hình
```

## Multi-tenant là gì?

Multi-tenant là pattern kiến trúc trong đó nhiều tenant dùng chung một phần hoặc toàn bộ nền tảng, nhưng dữ liệu và quyền truy cập phải được cách ly.

Những thứ thường được chia sẻ:

- Codebase.
- Hạ tầng ứng dụng.
- Database engine hoặc database cluster.
- Pipeline deploy.
- Logic nghiệp vụ lõi.

Những thứ phải được cách ly:

- Dữ liệu.
- Quyền truy cập.
- Cấu hình.
- Cache.
- Log và metrics theo tenant.
- Giới hạn tài nguyên khi cần.

## Vì sao multi-tenant quan trọng trong SaaS?

Nếu mỗi khách hàng có một deployment riêng, hệ thống sẽ dễ hiểu hơn nhưng chi phí vận hành tăng rất nhanh. Với 100 khách hàng, team có thể phải quản lý 100 instance, 100 cấu hình, 100 lịch update và nhiều version khác nhau.

Multi-tenant giúp:

- Giảm chi phí hạ tầng.
- Deploy một lần cho nhiều khách hàng.
- Tận dụng chung code và vận hành.
- Tạo nền tảng để scale số lượng tenant.

Đổi lại, hệ thống phải thiết kế tenant isolation rất kỹ.

## Rủi ro lớn nhất: data leakage

Trong shared table, nếu developer quên filter `tenant_id`, dữ liệu của tenant khác có thể bị trả về.

Ví dụ nguy hiểm:

```sql
SELECT *
FROM invoice
WHERE status = 'PAID';
```

Ví dụ đúng hơn:

```sql
SELECT *
FROM invoice
WHERE tenant_id = :tenant_id
  AND status = 'PAID';
```

Với dữ liệu kế toán, data leakage là lỗi nghiêm trọng vì có thể lộ chứng từ, công nợ, doanh thu, nhà cung cấp hoặc thông tin tài chính nội bộ của doanh nghiệp.

## Tenant-aware everything

Trong hệ thống multi-tenant, không chỉ database query cần biết tenant. Nhiều lớp khác cũng phải tenant-aware:

| Lớp | Cần tenant-aware như thế nào? |
|---|---|
| Auth | Token phải mang tenant context hoặc xác định được tenant hiện tại |
| Authorization | Role và permission phải có scope theo tenant |
| API | Không chỉ check quyền, còn phải check ownership của resource |
| Database | Query, unique constraint và index phải xét `tenant_id` |
| Cache | Cache key phải có tenant prefix |
| Log | Log nên có tenant id để debug theo khách hàng |
| Metrics | Theo dõi latency/error/resource usage theo tenant |

Ví dụ cache key:

```text
Sai:  categories
Đúng: tenant:42:categories
```

Nếu cache key không có tenant prefix, tenant B có thể nhận dữ liệu cache của tenant A.

## Platform admin và tenant admin

Cần phân biệt rõ:

| Vai trò | Phạm vi |
|---|---|
| Tenant admin | Quản trị trong một tenant cụ thể |
| Platform admin | Quản trị toàn nền tảng, có quyền cross-tenant được kiểm soát |

Platform admin không phải chỉ là "tenant admin mạnh hơn". Đây là vai trò vận hành hệ thống, cần audit và giới hạn truy cập rất cẩn thận.

## Kết luận

Multi-tenant giúp SaaS phục vụ nhiều doanh nghiệp với chi phí hợp lý, nhưng đổi lại mọi lớp của backend phải được thiết kế quanh tenant context. Với ERP/kế toán, ưu tiên đầu tiên là không để lộ hoặc lẫn dữ liệu giữa các tenant.
