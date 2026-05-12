# Spring Component, Bean và Dependency Injection

## Mục tiêu

Ghi chú này giúp mình hiểu vì sao `TenantFilter` dùng `@Component`, Spring quản lý object như thế nào, và khi nào nên dùng DI thay vì tự `new`.

Đây là ghi chú mức beginner cho Sprint 2, không phải tài liệu Spring Core đầy đủ.

## `@Component` là gì?

`@Component` là annotation đánh dấu một class là component do Spring quản lý.

Khi app start, Spring scan các package nằm dưới package gốc của `@SpringBootApplication`. Trong repo này, package gốc là:

```text
com.viettel.demo
```

Vì vậy các class như `com.viettel.demo.config.TenantFilter` có thể được scan nếu được annotate đúng.

## Spring Bean là gì?

Spring Bean là object được Spring tạo, quản lý vòng đời và inject dependency khi cần.

Nói ngắn:

```text
Class có annotation phù hợp
  -> Spring scan thấy
  -> Spring tạo object
  -> object đó trở thành bean trong ApplicationContext
```

Nếu một class cần tham gia vào Spring lifecycle, ví dụ filter, service, repository, controller, thì nên để Spring quản lý thay vì tự tạo bằng `new`.

## IoC container / ApplicationContext là gì?

IoC container là nơi Spring quản lý các bean.

`ApplicationContext` là interface chính đại diện cho container đó. Ở mức học hiện tại, chỉ cần hiểu:

- nó biết bean nào tồn tại;
- nó tạo bean;
- nó inject dependency;
- nó giữ lifecycle của bean;
- nó giúp các object phối hợp mà không cần tự tìm hoặc tự tạo nhau.

IoC nghĩa là "đảo ngược điều khiển": class không tự đi tạo dependency, mà khai báo nó cần gì, Spring cung cấp dependency đó.

## Dependency Injection là gì?

Dependency Injection là cách đưa dependency vào class từ bên ngoài.

Ví dụ tư duy:

```text
TenantFilter cần một dependency A
  -> không tự new A trong method
  -> khai báo qua constructor
  -> Spring tạo TenantFilter và truyền A vào
```

Với dependency bắt buộc, constructor injection thường dễ đọc và dễ test hơn field injection.

## Vì sao `TenantFilter` dùng `@Component`?

`TenantFilter` cần chạy trong servlet filter chain. Khi `TenantFilter` là Spring Bean, Spring Boot có thể đăng ký filter đó với embedded servlet container.

Trong lab:

```text
@Component
TenantFilter extends OncePerRequestFilter
  -> Spring scan thấy
  -> tạo bean
  -> Spring Boot đăng ký filter
  -> request đi qua filter trước controller
```

Nếu quên `@Component` hoặc class nằm ngoài package scan, filter có thể không chạy, dù code compile không lỗi.

## Khi nào dùng annotation nào?

| Annotation | Dùng cho | Ví dụ trong tenant-demo |
|---|---|---|
| `@Component` | Bean chung không thuộc role cụ thể hơn | `TenantFilter` |
| `@Service` | Business/service layer | `MasterDataService` sau này |
| `@Repository` | Data access layer nếu tự viết repository implementation | repository custom nếu cần |
| `@Controller` / `@RestController` | Web/API layer | `MasterDataController` sau này |

`@Service`, `@Repository`, `@Controller` là stereotype cụ thể hơn của component. Dùng đúng annotation giúp code dễ đọc theo tầng kiến trúc.

## Filter được Spring Boot đăng ký như thế nào?

Ở mức cơ bản: nếu một `Filter` là Spring Bean, Spring Boot sẽ đăng ký nó với embedded servlet container.

Với lab này, cách đơn giản là:

- `TenantFilter` extends `OncePerRequestFilter`;
- annotate `@Component`;
- để Spring Boot scan tự động.

Nếu sau này cần kiểm soát thứ tự filter, URL pattern, hoặc cấu hình nâng cao, có thể dùng `FilterRegistrationBean`. Sprint này chưa cần.

## Constructor injection vs field injection

| Cách inject | Ý nghĩa | Gợi ý |
|---|---|---|
| Constructor injection | Dependency được truyền qua constructor | Nên ưu tiên cho dependency bắt buộc |
| Field injection | Gắn `@Autowired` trực tiếp lên field | Dễ nhanh nhưng khó test/khó thấy dependency hơn |
| Setter injection | Dependency truyền qua setter | Phù hợp dependency optional |

Rule đơn giản cho lab: nếu class cần bean khác, dùng constructor injection trước.

## Static utility và Spring Bean khác nhau thế nào?

Không phải class nào cũng cần là Spring Bean.

Trong code hiện tại:

- `TenantFilter` là Spring Bean vì nó cần được Spring Boot đăng ký vào filter chain.
- `TenantContext` là utility nhỏ giữ `ThreadLocal`, có method static `set/get/clear`.

Điểm cần tránh là trộn lẫn mơ hồ:

- nếu class là Spring Bean, đừng tự `new` nó ở nơi khác;
- nếu class là static utility, đừng kỳ vọng Spring inject dependency vào nó;
- với `ThreadLocal`, điều quan trọng nhất là đúng lifecycle: set ở đầu request, clear trong `finally`.

## Common mistakes

| Lỗi | Hậu quả |
|---|---|
| Class nằm ngoài package scan | Bean không được tạo, filter/service không chạy |
| Quên `@Component` cho filter | App vẫn compile nhưng request không đi qua filter |
| Tự `new` một Spring-managed class | Dependency không được inject, lifecycle không do Spring quản lý |
| Dùng field injection mọi nơi | Khó test, khó thấy dependency bắt buộc |
| Nhầm static utility với Spring Bean | Code kỳ vọng inject nhưng Spring không quản lý object đó |
| Không clear `ThreadLocal` trong `finally` | Tenant có thể rò sang request khác khi thread được reuse |

## Checklist cho `TenantFilter`

- [ ] Class nằm dưới package `com.viettel.demo`.
- [ ] Có `@Component`.
- [ ] Kế thừa filter phù hợp, ví dụ `OncePerRequestFilter`.
- [ ] Không tự tạo filter bằng `new`.
- [ ] Nếu cần dependency bean khác, ưu tiên constructor injection.
- [ ] Luôn clear `TenantContext` trong `finally`.

## Nguồn tham khảo chuẩn

- Spring Framework `@Component`: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/stereotype/Component.html
- Spring Framework stereotype annotations: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/stereotype/package-summary.html
- Spring Framework Dependency Injection: https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html
- Spring Boot Servlet filters/listeners: https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.embedded-container.servlets-filters-listeners
- Jakarta Servlet `Filter`: https://jakarta.ee/specifications/platform/11/apidocs/jakarta/servlet/filter
- Java `ThreadLocal`: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ThreadLocal.html
