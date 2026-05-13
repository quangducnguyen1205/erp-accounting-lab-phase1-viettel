# Testing tenant isolation trong Spring Boot

## Vì sao backend tests quan trọng?

Curl và IntelliJ HTTP Client giúp mình verify thủ công. Nhưng nếu sau này sửa service, repository, filter hoặc security, lỗi data leakage có thể quay lại mà mình không nhận ra.

Backend test giúp biến một bug nguy hiểm thành regression check tự động:

- tenant 1 không được thấy data tenant 2;
- query theo `code` vẫn phải scoped theo tenant;
- request thiếu/sai tenant phải bị chặn;
- service/controller không được vô tình bỏ qua `tenantId`.

Với multi-tenant, đây là test correctness trước khi là test performance.

## Các loại test ở mức thực tế

| Loại test | Test cái gì? | Ví dụ trong repo này |
|---|---|---|
| Unit test | Một class nhỏ, mock dependency | Test riêng `MasterDataService` với repository mock |
| Repository test | Mapping/query JPA với database | Test `findByTenantIdAndCode(...)` |
| Controller/API test | Request HTTP giả lập vào controller/filter | Test `GET /api/master-data` với `X-Tenant-Id` |
| Integration test | Nhiều layer chạy cùng nhau | `TenantFilter -> Controller -> Service -> Repository -> DB` |

Task này nên ưu tiên **API/integration test bằng MockMvc**, vì mục tiêu là chứng minh request đi qua filter và không leak dữ liệu qua API.

## JUnit là gì?

JUnit là framework test phổ biến trong Java. Với Spring Boot hiện tại, mình dùng JUnit Jupiter, tức phần viết test của JUnit 5.

Các annotation hay gặp:

```java
@Test
void some_behavior_should_happen() {
    // arrange
    // act
    // assert
}

@BeforeEach
void setUp() {
    // chạy trước mỗi test
}
```

Ý nghĩa:

- `@Test`: đánh dấu một method là test case.
- `@BeforeEach`: chuẩn bị dữ liệu trước mỗi test để các test độc lập hơn.

## Spring Boot Test là gì?

`@SpringBootTest` yêu cầu Spring Boot load application context thật của app.

Trong task này, nó hữu ích vì test cần nhiều bean thật:

- `TenantFilter`;
- `MasterDataController`;
- `MasterDataService`;
- `MasterDataRepository`;
- datasource/JPA/Flyway.

Đổi lại, test sẽ nặng hơn unit test vì phải khởi động Spring context.

## MockMvc là gì?

`MockMvc` là công cụ test Spring MVC mà không cần mở HTTP server thật trên port `8080`.

Nó giả lập request/response ở server-side nhưng vẫn đi qua phần lớn flow Spring MVC:

```text
MockMvc request
-> Filter
-> DispatcherServlet
-> Controller
-> Service
-> Repository
-> Database
```

Vì `TenantFilter` là Servlet filter, `MockMvc` rất phù hợp để test:

- header `X-Tenant-Id`;
- missing/invalid tenant;
- status code;
- JSON body;
- endpoint có bị leak dữ liệu tenant khác không.

## Annotation nên dùng trong lab này

```java
@SpringBootTest
@AutoConfigureMockMvc
class DataLeakageTest {
    // ...
}
```

Ý nghĩa:

- `@SpringBootTest`: load app context gần giống lúc chạy app.
- `@AutoConfigureMockMvc`: tạo sẵn bean `MockMvc` để gọi API trong test.

Lưu ý: mặc định `@AutoConfigureMockMvc` sẽ add filters từ application context, nên `TenantFilter` có thể tham gia vào request test.

## Cú pháp MockMvc cơ bản

Ví dụ pattern:

```java
mockMvc.perform(get("/api/master-data")
        .header("X-Tenant-Id", "1"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$[*].tenantId").value(everyItem(is(1))));
```

Các phần cần hiểu:

- `mockMvc.perform(...)`: gửi một request giả lập.
- `get("/api/master-data")`: tạo GET request.
- `.header("X-Tenant-Id", "1")`: thêm header.
- `.andExpect(status().isOk())`: kỳ vọng HTTP `200`.
- `.andExpect(status().isBadRequest())`: kỳ vọng HTTP `400`.
- `.andExpect(status().isNotFound())`: kỳ vọng HTTP `404`.
- `jsonPath(...)`: đọc và kiểm tra JSON response.

Nếu chưa quen `jsonPath`, có thể bắt đầu bằng status code trước, rồi thêm JSON assertion sau.

## Test fixture/setup là gì?

Fixture là dữ liệu chuẩn bị trước test.

Ví dụ:

- tenant 1 có record `LAPTOP-01`;
- tenant 2 cũng có record `LAPTOP-01`;
- tenant 2 có id riêng để test cross-tenant access.

Test không nên phụ thuộc vào DB local đang có gì từ hôm trước. Nếu test dựa vào dữ liệu bẩn, hôm nay pass nhưng ngày mai fail rất khó hiểu.

Trong task này, cách dễ hiểu là dùng `@BeforeEach` để:

1. dọn dữ liệu test cũ;
2. insert 2 tenant;
3. insert vài dòng `master_data` cố định cho tenant 1 và tenant 2.

Có thể dùng `JdbcTemplate` để setup dữ liệu rõ ràng bằng SQL. Cách này không phụ thuộc vào `TenantContext` khi insert fixture.

## Strategy đề xuất cho `DataLeakageTest`

Mức vừa đủ cho Sprint này:

1. Dùng `@SpringBootTest + @AutoConfigureMockMvc`.
2. Dùng `JdbcTemplate` trong `@BeforeEach` để tạo fixture cố định.
3. Gọi API bằng `MockMvc`, không gọi service trực tiếp.
4. Assert status code trước, sau đó assert JSON body.
5. Chưa dùng Testcontainers để tránh mở rộng scope.

Các case nên tự code:

- tenant 1 list chỉ trả `tenantId = 1`;
- tenant 2 list chỉ trả `tenantId = 2`;
- tenant 1 không truy cập được id của tenant 2;
- query by code `LAPTOP-01` trả record đúng tenant;
- thiếu `X-Tenant-Id` trả `400`;
- `X-Tenant-Id: abc` trả `400`.

## Vì sao không chỉ dùng curl?

Curl chứng minh hiện tại API chạy đúng. Test tự động chứng minh sau này API vẫn đúng sau khi code thay đổi.

Nói ngắn:

```text
curl = manual verification
MockMvc test = automated regression protection
```

## Common mistakes

- Test phụ thuộc vào local dirty DB.
- Chỉ test happy path, không test missing/invalid tenant.
- Test list endpoint nhưng không kiểm tra `tenantId` trong response body.
- Query/repository method thiếu `tenantId`.
- Viết test gọi service trực tiếp nên bỏ qua `TenantFilter`.
- Dùng `@WebMvcTest` quá sớm rồi phải mock service/repository, trong khi mục tiêu hiện tại là test nhiều layer cùng nhau.
- Nhầm `404` cross-tenant với “record không tồn tại”; cần có case tenant đúng gọi cùng id và nhận `200`.
- Thêm Testcontainers, H2 hoặc profile phức tạp trước khi hiểu flow MockMvc cơ bản.

## Checklist trước khi tự code

- [ ] Biết endpoint cần test: `/api/master-data`, `/api/master-data/{id}`, `/api/master-data/code/{code}`.
- [ ] Biết header hiện tại: `X-Tenant-Id`.
- [ ] Biết fixture tenant 1/tenant 2 sẽ được tạo ở đâu.
- [ ] Biết case nào kỳ vọng `200`, `400`, `404`.
- [ ] Chạy được `cd lab-code && make app-test`.
- [ ] Sau khi test pass, nhờ Codex review: test có thật sự chống leakage không?

## Nguồn tham khảo chuẩn

- JUnit 5 User Guide: https://docs.junit.org/5.9.0/user-guide/
- Spring Boot Testing Reference: https://docs.spring.io/spring-boot/docs/3.2.5/reference/htmlsingle/#features.testing
- Spring Framework MockMvc Reference: https://docs.spring.io/spring-framework/reference/testing/mockmvc.html
