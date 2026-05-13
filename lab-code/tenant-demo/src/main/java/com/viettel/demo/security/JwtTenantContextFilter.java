package com.viettel.demo.security;

/*
 * ==============================================================
 * JwtTenantContextFilter — TODO skeleton lấy tenant từ JWT đã validate
 * ==============================================================
 *
 * [Thiết kế khuyến nghị]
 * Không tự parse/verify JWT trong filter này.
 *
 * Flow đúng hơn cho lab:
 *
 * 1. Spring Security Resource Server validate Bearer JWT trước.
 * 2. Nếu token hợp lệ, Spring Security đặt Authentication/JWT vào SecurityContext.
 * 3. Filter này chạy SAU bước validate đó.
 * 4. Filter đọc tenant_id claim từ JWT đã validate.
 * 5. Filter gọi TenantContext.setCurrentTenant(tenantId).
 * 6. Service/repository tiếp tục dùng TenantContext như hiện tại.
 * 7. Filter clear TenantContext trong finally.
 *
 * [Trạng thái hiện tại]
 * Class này CHƯA active:
 * - chưa extends OncePerRequestFilter;
 * - chưa có @Component;
 * - chưa được đăng ký trong SecurityConfig.
 *
 * [Pseudo-code sau khi thêm Spring Security dependency]
 *
 * Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
 * Jwt jwt = (Jwt) authentication.getPrincipal();
 * Long tenantId = jwt.getClaim("tenant_id");
 * TenantContext.setCurrentTenant(tenantId);
 *
 * try {
 *     filterChain.doFilter(request, response);
 * } finally {
 *     TenantContext.clear();
 * }
 *
 * [TODO khi tự code]
 * TODO 1: Cho class này extends OncePerRequestFilter.
 * TODO 2: Đảm bảo filter chạy sau khi JWT đã được Spring Security validate.
 * TODO 3: Đọc Authentication từ SecurityContextHolder.
 * TODO 4: Kiểm tra principal có phải Jwt không.
 * TODO 5: Extract claim tenant_id, validate là số dương.
 * TODO 6: Nếu thiếu tenant_id, trả 401 hoặc 403 theo quyết định học.
 * TODO 7: Set TenantContext và clear trong finally.
 * TODO 8: Chạy lại DataLeakageTest phiên bản JWT.
 *
 * ==============================================================
 */
public class JwtTenantContextFilter {
    /*
     * TODO: Tự implement ở task sau.
     *
     * Không thêm @Component ở bước skeleton để không vô tình bật filter
     * trước khi Spring Security/JWT validation được cấu hình xong.
     */
}
