# PostgreSQL và bài toán Multi-tenant

## Vì sao PostgreSQL phù hợp?

PostgreSQL phù hợp cho backend ERP/kế toán SaaS vì hệ thống kế toán cần tính đúng đắn dữ liệu cao, transaction rõ ràng và khả năng kiểm soát schema tốt.

Các điểm quan trọng:

- ACID transaction mạnh.
- Constraint, foreign key và unique constraint tốt.
- Hỗ trợ nhiều schema nếu sau này cần schema per tenant.
- Index đa dạng, query planner mạnh.
- Có extension và tooling phong phú.
- Phù hợp với dữ liệu quan hệ như chứng từ, tài khoản, bút toán, công nợ.

## ACID trong ngữ cảnh kế toán

Kế toán không chấp nhận dữ liệu "gần đúng". Ví dụ một nghiệp vụ ghi sổ phải ghi đủ cả vế nợ và vế có.

```sql
BEGIN;

INSERT INTO journal_entries (tenant_id, account_code, debit, credit)
VALUES (1, '156', 10000000, 0);

INSERT INTO journal_entries (tenant_id, account_code, debit, credit)
VALUES (1, '331', 0, 10000000);

COMMIT;
```

Nếu một dòng fail, cả transaction phải rollback. Đây là lý do database quan hệ như PostgreSQL phù hợp hơn cho phần lõi kế toán so với hướng eventual consistency cho dữ liệu tài chính chính thống.

## Thiết kế bảng tenant-aware

Trong mô hình shared table, các bảng nghiệp vụ nên có `tenant_id`.

```sql
CREATE TABLE customer (
    id bigserial PRIMARY KEY,
    tenant_id bigint NOT NULL,
    code varchar(50) NOT NULL,
    name varchar(255) NOT NULL,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamp NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, code)
);
```

Điểm cần chú ý:

- Unique constraint nên tenant-aware: `UNIQUE (tenant_id, code)`, không phải `UNIQUE (code)`.
- Query theo id cũng nên kiểm tra tenant: `WHERE id = :id AND tenant_id = :tenant_id`.
- Foreign key giữa các bảng nghiệp vụ cần đảm bảo cùng tenant ở tầng thiết kế hoặc tầng application.

## Tenant context trong backend

Một request nên xác định tenant càng sớm càng tốt:

```text
HTTP Request
    -> Auth middleware đọc token
    -> Xác định user_id và tenant_id
    -> Đặt vào request context
    -> Service/repository dùng tenant_id hiện tại
```

Không nên để từng hàm tự đoán tenant. Tenant context phải là một phần của contract backend.

## PostgreSQL không tự giải quyết hết multi-tenant

PostgreSQL cung cấp công cụ tốt, nhưng không tự động làm hệ thống an toàn. Backend vẫn phải thiết kế:

- Query luôn có tenant filter.
- Index phù hợp với query tenant-aware.
- Cache key có tenant prefix.
- Migration không giữ lock lâu.
- Test data isolation.
- Monitoring theo tenant.

Có thể dùng Row Level Security để tăng lớp bảo vệ, nhưng Phase 1 nên hiểu rõ logic tenant-aware trước khi dựa vào cơ chế nâng cao.

## Kết luận

PostgreSQL là nền tảng tốt cho bài toán ERP/kế toán SaaS vì vừa đảm bảo tính đúng đắn dữ liệu, vừa có các cơ chế cần thiết để tiến hóa từ shared table lên schema hoặc partition khi hệ thống lớn hơn.
