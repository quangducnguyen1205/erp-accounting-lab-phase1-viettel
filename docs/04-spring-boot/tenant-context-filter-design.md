# Thiết kế TenantContext và TenantFilter

## Mục tiêu

Tài liệu này giúp mình hiểu trước khi tự code `TenantContext.java` và `TenantFilter.java` trong `lab-code/tenant-demo`.

Phạm vi hiện tại:

- dùng `X-Tenant-Id` làm cơ chế học tập đơn giản;
- đặt tenant hiện tại vào `TenantContext`;
- service/repository sau này sẽ dùng tenant hiện tại để truy vấn đúng phạm vi;
- chưa làm Spring Security, Keycloak, JWT thật, RBAC hay phân quyền đầy đủ.

## Vì sao backend multi-tenant cần current tenant context?

Trong shared-table multi-tenant, nhiều tenant cùng nằm trong một bảng, ví dụ `master_data` có cột `tenant_id`.

Nếu backend không biết request hiện tại thuộc tenant nào, các query rất dễ bị viết thiếu điều kiện:

```sql
WHERE code = ...
```

Đúng hơn là phải có tenant scope:

```sql
WHERE tenant_id = ... AND code = ...
```

Vì vậy backend cần một "current tenant context" đáng tin cậy để các tầng sau không lấy tenant tùy tiện từ request body hoặc frontend.

## Vì sao lab dùng X-Tenant-Id?

Trong lab này, `X-Tenant-Id` là cơ chế giả lập đơn giản:

```text
Client
  -> HTTP header: X-Tenant-Id: 1
  -> TenantFilter đọc header
  -> TenantContext lưu tenant hiện tại
  -> Service/Repository dùng tenant hiện tại
```

Đây là cách học luồng xử lý multi-tenant trước khi đưa thêm auth thật vào hệ thống.

Điều quan trọng: `X-Tenant-Id` không phải production authentication. Trong hệ thống thật, tenant thường phải được suy ra từ token đã xác thực, session, claim, hoặc quan hệ user-tenant đã kiểm tra bằng backend.

## Luồng thiết kế trong tenant-demo

```text
HTTP request
  -> TenantFilter
      -> đọc header X-Tenant-Id
      -> validate giá trị tenant
      -> TenantContext.set(...)
      -> FilterChain.doFilter(...)
      -> finally TenantContext.clear()
  -> Controller
  -> Service
      -> đọc tenant hiện tại từ TenantContext
  -> Repository
      -> query có điều kiện tenant_id
  -> PostgreSQL
```

## TenantFilter nên làm gì?

`TenantFilter` là nơi xử lý cross-cutting concern: mỗi request cần được gắn tenant trước khi vào controller.

Ở mức lab, filter nên:

- đọc header `X-Tenant-Id` từ request;
- kiểm tra header có tồn tại hay không;
- parse header thành tenant id hợp lệ;
- nếu header sai, trả lời lỗi rõ ràng thay vì để request đi tiếp mơ hồ;
- set tenant vào `TenantContext`;
- gọi `FilterChain` để request tiếp tục chạy;
- luôn clear context trong `finally`.

Ghi chú cho lab: mình có thể chọn policy lỗi đơn giản, ví dụ request thiếu/sai `X-Tenant-Id` thì trả về lỗi client. Điểm học chính là filter phải chặn được request không có tenant context hợp lệ.

## TenantContext nên làm gì?

`TenantContext` nên là lớp nhỏ, rõ trách nhiệm:

| Hàm | Ý nghĩa |
| --- | --- |
| `set(...)` | Lưu tenant hiện tại cho request đang xử lý |
| `get(...)` | Lấy tenant hiện tại để service/repository sử dụng |
| `clear()` | Xóa tenant hiện tại sau khi request kết thúc |

Trong lab này, `ThreadLocal` phù hợp vì mỗi request servlet thông thường được xử lý trên một thread tại một thời điểm. `TenantContext` không nên tự đọc HTTP request; việc đọc request là trách nhiệm của filter.

## Vì sao clear() phải nằm trong finally?

Nếu request thành công, filter cần clear tenant.

Nếu request bị exception, filter vẫn phải clear tenant.

Vì vậy pattern cần nhớ:

```text
set tenant
try:
  cho request chạy tiếp
finally:
  clear tenant
```

Nếu không clear, thread trong server có thể được tái sử dụng cho request sau. Khi đó request mới có nguy cơ nhìn thấy tenant cũ. Đây là lỗi nghiêm trọng vì nó liên quan trực tiếp đến data isolation.

## Service và Repository sẽ dùng tenant context như thế nào?

Sau khi có filter/context, các tầng tiếp theo không nên lấy `tenant_id` từ request body tùy tiện.

Hướng dùng trong lab:

- Controller nhận request nghiệp vụ;
- Service lấy tenant hiện tại từ `TenantContext`;
- Repository có method tenant-aware, ví dụ tìm theo `tenantId` và `code`;
- PostgreSQL vẫn giữ constraint như `UNIQUE (tenant_id, code)`;
- index giúp query nhanh hơn, nhưng không thay thế tenant isolation.

Cách nghĩ cần nhớ:

```text
Filter xác lập tenant hiện tại.
Service/Repository bắt buộc truy vấn theo tenant hiện tại.
Database ràng buộc tính đúng của dữ liệu.
```

## Những điều không nên làm

- Không tin `tenant_id` từ request body tùy tiện.
- Không để frontend tự ẩn/hiện dữ liệu rồi coi đó là tenant isolation.
- Không viết repository query thiếu `tenantId`, ví dụ chỉ `findByCode(...)` trong bảng shared-table.
- Không để `ThreadLocal` sống qua request mới.
- Không nhầm RBAC với tenant isolation: RBAC trả lời "user được làm gì", tenant isolation trả lời "user đang thao tác trong phạm vi tenant nào".
- Không đưa Spring Security, Keycloak, JWT thật vào Sprint này khi app database-backed baseline chưa chạy vững.

## Diễn giải riêng cho lab này

Phần này là diễn giải học tập dựa trên tài liệu chuẩn.

Trong production, tenant context nên được liên kết với cơ chế xác thực/phân quyền thật. Ví dụ user đăng nhập bằng OIDC/JWT, backend xác minh token, kiểm tra user có quyền trên tenant nào, sau đó mới tạo tenant context.

Trong Sprint 2, mình cố tình rút gọn bằng `X-Tenant-Id` để học rõ:

- request đi qua filter như thế nào;
- tenant được đặt vào context ra sao;
- vì sao phải clear context;
- service/repository phải truy vấn tenant-aware thế nào.

Reactive context propagation, async task propagation và distributed tracing context là chủ đề nâng cao, không nằm trong phạm vi task này.

## Checklist trước khi code

- Đã đọc `request-filter-threadlocal.md`.
- Đã chọn header học tập là `X-Tenant-Id`.
- Đã biết request thiếu/sai tenant sẽ bị xử lý ra sao.
- Đã biết `TenantContext` chỉ cần `set/get/clear`.
- Đã biết `clear()` phải nằm trong `finally`.
- Đã biết service/repository sau này phải dùng tenant từ backend context.
- Đã biết đây chưa phải cơ chế auth production.

## Nguồn tham khảo chuẩn

- Java `ThreadLocal`: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ThreadLocal.html
- Jakarta Servlet `Filter`: https://jakarta.ee/specifications/platform/11/apidocs/jakarta/servlet/filter
- Jakarta Servlet `FilterChain`: https://jakarta.ee/specifications/platform/11/apidocs/jakarta/servlet/filterchain
- Jakarta Servlet `HttpServletRequest`: https://jakarta.ee/specifications/platform/9/apidocs/jakarta/servlet/http/httpservletrequest
- Spring Framework `OncePerRequestFilter`: https://docs.spring.io/spring-framework/docs/6.1.14/javadoc-api/org/springframework/web/filter/OncePerRequestFilter.html
- Spring Boot - add Servlet filters/listeners: https://docs.spring.io/spring-boot/how-to/webserver.html#howto.webserver.add-servlet-filter-listener
