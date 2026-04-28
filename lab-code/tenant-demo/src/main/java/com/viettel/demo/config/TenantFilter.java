package com.viettel.demo.config;

/*
 * ==============================================================
 * TODO TASK: Tenant Filter — extract tenant_id từ mỗi request
 * ==============================================================
 *
 * [Mục tiêu]
 * Đây là Servlet Filter chạy TRƯỚC mọi request.
 * Nhiệm vụ: đọc JWT token từ header → giải mã → lấy tenant_id
 * → nạp vào TenantContext để các tầng sau dùng.
 *
 * [Nhiệm vụ của tôi]
 * 1. Kế thừa đúng base class của Spring để tạo một filter
 *    chạy 1 lần per request.
 * 2. Trong method filter:
 *    a. Lấy Authorization header từ request.
 *    b. Parse/giải mã JWT để lấy tenant_id.
 *       (Phase 1 có thể tạm dùng header đơn giản "X-Tenant-Id"
 *        thay vì JWT thật — đơn giản hóa để tập trung học flow.)
 *    c. Gọi TenantContext.setCurrentTenant(tenantId).
 *    d. Cho request đi tiếp qua filter chain.
 *    e. SAU KHI request xong → gọi TenantContext.clear().
 *       Suy nghĩ: bước này nằm ở đâu? try-finally?
 *
 * [Kiến thức cần tự research]
 * - OncePerRequestFilter (Spring Web)
 * - doFilterInternal(HttpServletRequest, HttpServletResponse, FilterChain)
 * - FilterChain.doFilter(request, response)
 * - HttpServletRequest.getHeader(String)
 * - try-finally pattern để đảm bảo cleanup
 * - @Component annotation để Spring tự đăng ký filter
 * - Đọc lại: docs/02-multi-tenant/tong-quan-multi-tenant.md
 *   (sơ đồ sequence: Client → Auth → Context → Service → DB)
 *
 * ==============================================================
 */

public class TenantFilter {

    // TODO: Kế thừa đúng base class

    // TODO: Override method filter

    // TODO: Extract tenant_id từ header

    // TODO: Set vào TenantContext

    // TODO: filterChain.doFilter(...)

    // TODO: Clear TenantContext trong finally block

}
