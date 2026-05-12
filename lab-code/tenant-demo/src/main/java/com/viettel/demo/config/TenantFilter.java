package com.viettel.demo.config;

/*
 * ==============================================================
 * Tenant Filter — extract tenant_id từ mỗi request
 * ==============================================================
 *
 * [Mục tiêu]
 * Đây là Servlet Filter chạy TRƯỚC mọi request.
 * Nhiệm vụ trong Phase 1: đọc header học tập X-Tenant-Id,
 * validate giá trị tenant_id, rồi nạp vào TenantContext để
 * các tầng sau dùng.
 *
 * [Cách hoạt động hiện tại]
 * 1. Kế thừa OncePerRequestFilter để filter chạy 1 lần per request.
 * 2. Đọc X-Tenant-Id từ request header.
 * 3. Chặn request thiếu/sai tenant bằng HTTP 400.
 * 4. Gọi TenantContext.setCurrentTenant(tenantId).
 * 5. Cho request đi tiếp qua filter chain.
 * 6. SAU KHI request xong → gọi TenantContext.clear() trong finally.
 *
 * [Ghi chú production]
 * Hệ thống thật thường lấy tenant từ JWT/OIDC/session đã xác thực,
 * không tin trực tiếp header giả lập. Phase 1 dùng X-Tenant-Id để
 * tập trung học request flow và tenant isolation trước.
 *
 * [Kiến thức đã áp dụng]
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

import com.viettel.demo.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {
    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(TENANT_HEADER);

        if (header == null || header.isBlank()) {
            TenantContext.clear();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing X-Tenant-Id header");
            return;
        }

        Long tenantId;
        try {
            tenantId = Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            TenantContext.clear();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid X-Tenant-Id header");
            return;
        }

        if (tenantId <= 0) {
            TenantContext.clear();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "X-Tenant-Id must be positive");
            return;
        }

        TenantContext.setCurrentTenant(tenantId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
