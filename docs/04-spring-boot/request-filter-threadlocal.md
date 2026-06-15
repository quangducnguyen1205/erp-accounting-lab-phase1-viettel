# Request, Filter và ThreadLocal trong Spring Boot

## Mục tiêu

Ghi chú này chuẩn bị cho việc tự implement `TenantContext.java` và `TenantFilter.java`. Trọng tâm là hiểu request đi qua đâu, filter làm gì, và vì sao `ThreadLocal` có thể giữ tenant hiện tại trong bài lab này.

Đây là ghi chú mức nền tảng cho servlet-stack Spring Boot, không đi sâu reactive programming hay security production.

## Vòng đời request ở mức đơn giản

Trong app Spring Boot dùng `spring-boot-starter-web`, request HTTP thường đi qua luồng:

```text
Client
  -> Embedded servlet container (Tomcat)
  -> Servlet Filter(s)
  -> DispatcherServlet
  -> Controller
  -> Service
  -> Repository
  -> Database
  -> response quay ngược lại
```

Ý nghĩa thực tế:

| Thành phần | Vai trò |
|---|---|
| Tomcat | Nhận HTTP request và chạy servlet stack |
| Filter | Chạy trước/sau resource chính để xử lý việc cắt ngang |
| DispatcherServlet | Điều phối request tới controller phù hợp |
| Controller | Nhận input HTTP, gọi service, trả response |
| Service | Xử lý nghiệp vụ |
| Repository | Truy cập dữ liệu |

Trong task này, `TenantFilter` nằm trước Controller/Service/Repository. Vì vậy nó là nơi phù hợp để đọc tenant từ request và nạp vào context.

## Servlet Filter là gì?

Theo Jakarta Servlet API, `Filter` là object thực hiện filtering task trên request, response hoặc cả hai. Filter chạy qua method `doFilter`.

Filter thường dùng cho các concern cắt ngang:

- authentication / authorization ở mức request;
- logging/auditing;
- request correlation id;
- CORS;
- compression;
- tenant context trong bài lab này.

Trong Spring Boot, filter có thể được đăng ký như Spring bean hoặc qua cơ chế registration. Với bài lab này, hướng đơn giản là tạo một filter class và để Spring quản lý nó như một component.

## Filter chạy ở đâu so với Controller/Service/Repository?

```text
HTTP request
  -> TenantFilter
       đọc X-Tenant-Id
       set TenantContext
       gọi FilterChain.doFilter(...)
  -> Controller
  -> Service
  -> Repository
  -> Database
  <- response
  -> finally: TenantContext.clear()
```

Điểm quan trọng: Controller/Service/Repository chạy sau filter, nên các tầng đó có thể đọc tenant hiện tại từ `TenantContext`.

## FilterChain là gì?

`FilterChain` là object do servlet container cung cấp để filter gọi tiếp filter kế tiếp hoặc resource cuối cùng.

Trong code, ý tưởng sẽ là:

```text
filter nhận request
  -> xử lý trước request
  -> chain.doFilter(request, response)
  -> xử lý sau request nếu cần
```

Nếu filter không gọi `chain.doFilter(...)`, request sẽ không đi tiếp tới Controller. Điều này có thể dùng để chặn request invalid, ví dụ thiếu hoặc sai tenant header.

## Vì sao filter hữu ích cho cross-cutting concern?

Tenant context không thuộc riêng một controller nào. Mọi API nghiệp vụ đều cần biết tenant hiện tại. Nếu mỗi controller tự đọc header và tự truyền tenant xuống service, code dễ lặp và dễ quên.

Filter giúp đặt logic chung ở một điểm:

- request nào cũng đi qua;
- set tenant trước khi vào business logic;
- clear tenant sau khi request xong;
- giảm nguy cơ một endpoint quên setup tenant.

## ThreadLocal là gì?

`ThreadLocal<T>` là cơ chế Java cho phép mỗi thread có một bản sao riêng của biến. Cùng một `ThreadLocal`, nhưng thread A và thread B đọc/ghi giá trị riêng, không đè lên nhau.

Ví dụ ý tưởng:

```text
Thread A -> TenantContext = 1
Thread B -> TenantContext = 2
```

Hai giá trị này tách biệt vì chúng nằm trong từng thread.

## Vì sao ThreadLocal phù hợp với request context trong lab này?

Trong servlet-stack truyền thống, mỗi request thường được xử lý bởi một thread trong thread pool. Nếu filter set tenant vào `ThreadLocal` ở đầu request, service/repository chạy cùng thread có thể đọc lại tenant đó.

Diễn giải cho lab:

```text
Request tenant 1
  -> Tomcat thread T1
  -> TenantFilter set ThreadLocal = 1
  -> Service đọc TenantContext.getCurrentTenant()
  -> Repository query WHERE tenant_id = 1
```

Đây là cách học phù hợp để hiểu request-scoped context mà không cần đưa tenant id qua mọi method parameter ngay từ đầu.

## Vì sao phải clear ThreadLocal?

Servlet container dùng thread pool. Sau khi request A xong, cùng thread có thể được tái sử dụng cho request B.

Nếu không gọi `remove()`/`clear()`:

```text
Request A tenant 1 chạy trên Thread T1
  -> set ThreadLocal = 1
  -> không clear

Request B chạy lại trên Thread T1
  -> nếu filter lỗi/không set tenant mới
  -> code phía sau có thể đọc nhầm tenant 1
```

Rủi ro:

- data leakage giữa tenant;
- lỗi khó debug vì phụ thuộc thread reuse;
- giữ reference lâu hơn cần thiết;
- request sau đọc context cũ.

Vì vậy `TenantFilter` phải gọi `TenantContext.clear()` trong `finally`, để dù controller/service thành công hay ném exception thì context vẫn được dọn.

## Khác gì production-grade security/context propagation?

Diễn giải cho lab:

- Dùng `X-Tenant-Id` là cách học flow đơn giản, không phải cơ chế xác thực thật.
- Production thường lấy tenant từ token đã xác thực, ví dụ JWT/OIDC qua Spring Security/Keycloak.
- Production cần kiểm tra user có quyền thuộc tenant đó không.
- Nếu dùng async processing, reactive stack, message queue hoặc thread chuyển tiếp, `ThreadLocal` có giới hạn và cần cơ chế context propagation khác.

Out of scope Sprint 2:

- Spring Security đầy đủ;
- Keycloak/OIDC;
- reactive context;
- async executor context propagation.

## Lỗi thường gặp trong bài lab này

| Lỗi | Hậu quả |
|---|---|
| Không gọi `TenantContext.clear()` | Thread được tái sử dụng có thể giữ tenant cũ |
| Đọc `tenant_id` từ request body | Client có thể tự đổi tenant nếu backend tin trực tiếp |
| Cho request thiếu tenant đi tiếp | Service/repository có thể query không scoped |
| Chỉ filter ở frontend | Người dùng vẫn có thể gọi API trực tiếp |
| Nghĩ index tự chống leakage | Index chỉ giúp performance, không thay thế tenant isolation |

## Checklist implement `TenantContext`

- [ ] Tạo một `ThreadLocal<Long>` private/static.
- [ ] Viết method set tenant hiện tại.
- [ ] Viết method get tenant hiện tại.
- [ ] Viết method clear bằng `remove()`, không chỉ set `null`.
- [ ] Không để `TenantContext` tự đọc HTTP request.

## Checklist implement `TenantFilter`

- [ ] Dùng filter chạy một lần mỗi request, ví dụ `OncePerRequestFilter`.
- [ ] Đọc header học tập `X-Tenant-Id`.
- [ ] Xử lý rõ request thiếu header.
- [ ] Xử lý rõ request có tenant id không parse được hoặc không hợp lệ.
- [ ] Set tenant vào `TenantContext` trước khi gọi `chain.doFilter(...)`.
- [ ] Gọi `TenantContext.clear()` trong `finally`.
- [ ] Chưa đưa JWT/Keycloak thật vào task này.

## Test nhanh bằng curl/logs

Sau khi tự code xong, có thể verify theo hướng:

```bash
cd lab-code
make -f Makefile.legacy db-up
make -f Makefile.legacy app-run
```

Ở terminal khác:

```bash
curl -i -H "X-Tenant-Id: 1" http://localhost:8080/...
curl -i -H "X-Tenant-Id: 2" http://localhost:8080/...
curl -i http://localhost:8080/...
curl -i -H "X-Tenant-Id: abc" http://localhost:8080/...
```

Khi chưa có controller thật, có thể cần đợi tới bước service/controller mới kiểm chứng đầy đủ bằng API. Ở bước filter, log tối thiểu có thể giúp thấy request có tenant hợp lệ được set context, còn request thiếu/sai header bị chặn rõ ràng.

Không cần lưu output dài vào repo. Chỉ cần ghi lại pattern: request hợp lệ đi tiếp, request thiếu/sai tenant bị trả lỗi, và context luôn được clear.

## Nguồn tham khảo chuẩn

- Java SE 17 API: [`ThreadLocal`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ThreadLocal.html)
- Jakarta Servlet API: [`Filter`](https://jakarta.ee/specifications/platform/11/apidocs/jakarta/servlet/filter)
- Jakarta Servlet API: [`FilterChain`](https://jakarta.ee/specifications/platform/11/apidocs/jakarta/servlet/filterchain)
- Jakarta Servlet API: [`HttpServletRequest`](https://jakarta.ee/specifications/platform/9/apidocs/jakarta/servlet/http/httpservletrequest)
- Spring Framework API: [`OncePerRequestFilter`](https://docs.spring.io/spring-framework/docs/6.1.14/javadoc-api/org/springframework/web/filter/OncePerRequestFilter.html)
- Spring Boot docs: [Add a Servlet, Filter, or Listener to an Application](https://docs.spring.io/spring-boot/how-to/webserver.html#howto.webserver.add-servlet-filter-listener)
