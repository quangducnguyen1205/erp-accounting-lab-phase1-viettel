package com.viettel.demo.security;

/*
 * ==============================================================
 * SecurityConfig — TODO skeleton cho cấu hình security tối thiểu
 * ==============================================================
 *
 * [Trạng thái hiện tại]
 * Class này CHƯA active:
 * - chưa có @Configuration;
 * - chưa khai báo SecurityFilterChain;
 * - project chưa thêm Spring Security dependency.
 *
 * Lý do: chỉ cần thêm Spring Security dependency là runtime behavior có thể
 * đổi ngay. Bước này chỉ chuẩn bị skeleton để bạn tự code có kiểm soát.
 *
 * [Thiết kế khuyến nghị]
 * - Dùng Spring Security OAuth2 Resource Server để validate Bearer JWT.
 * - Không tự parse/verify JWT trong custom filter.
 * - Custom filter chỉ đọc JWT đã validate từ SecurityContext và set TenantContext.
 *
 * [Mục tiêu sau khi tự implement]
 * SecurityConfig sẽ hướng dẫn Spring:
 * 1. endpoint nào public trong local/dev;
 * 2. endpoint nào cần token;
 * 3. Spring Security Resource Server validate JWT như thế nào;
 * 4. JwtTenantContextFilter đứng ở đâu sau bước validate JWT;
 * 5. cách trả lỗi 401/403 ở mức đơn giản.
 *
 * [Endpoint public có thể cân nhắc cho lab]
 * - /api/dev/tokens/tenant-1 và /api/dev/tokens/tenant-2 nếu bạn tạo dev token endpoint.
 *
 * [Không làm trong Phase này]
 * - Không tích hợp Keycloak thật.
 * - Không làm role matrix/RBAC đầy đủ.
 * - Không thêm OAuth2 login flow.
 * - Không tạo session-based login.
 *
 * [TODO khi bắt đầu code]
 * TODO 1: Thêm dependency Spring Security/JWT tối thiểu vào pom.xml.
 * TODO 2: Thêm @Configuration cho class này.
 * TODO 3: Khai báo SecurityFilterChain.
 * TODO 4: Cho phép public endpoint dev token nếu thật sự cần.
 * TODO 5: Bật oauth2ResourceServer().jwt(...).
 * TODO 6: Đăng ký JwtTenantContextFilter sau JWT authentication.
 * TODO 7: Chạy lại make app-test và curl valid/invalid token.
 *
 * Gợi ý pseudo-code, chưa implement:
 *
 * @Bean
 * SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
 *     return http
 *         .csrf(...)
 *         .authorizeHttpRequests(...)
 *         .oauth2ResourceServer(oauth2 -> oauth2.jwt(...))
 *         .addFilterAfter(jwtTenantContextFilter, ...)
 *         .build();
 * }
 *
 * ==============================================================
 */
public class SecurityConfig {
    /*
     * TODO: Tự implement ở task sau.
     */
}
