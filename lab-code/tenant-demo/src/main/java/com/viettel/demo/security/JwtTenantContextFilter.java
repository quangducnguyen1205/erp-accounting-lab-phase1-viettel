package com.viettel.demo.security;

import com.viettel.demo.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/*
 * ==============================================================
 * JwtTenantContextFilter — bridge JWT đã validate sang TenantContext
 * ==============================================================
 *
 * [Vai trò]
 * Không tự parse/verify JWT trong filter này.
 * Filter này chỉ đọc Jwt đã được Spring Security validate và đặt tenant_id
 * vào TenantContext để Service/Repository hiện tại tiếp tục dùng.
 *
 * Flow:
 * 1. Spring Security Resource Server validate Bearer JWT trước.
 * 2. Nếu token hợp lệ, Spring Security đặt Authentication/JWT vào SecurityContext.
 * 3. Filter này chạy SAU bước validate đó.
 * 4. Filter đọc tenant_id claim từ JWT đã validate.
 * 5. Filter gọi TenantContext.setCurrentTenant(tenantId).
 * 6. Service/repository tiếp tục dùng TenantContext như hiện tại.
 * 7. Filter clear TenantContext trong finally.
 *
 * [Lưu ý học tập]
 * - SecurityContext trả lời câu hỏi "request đã authenticated chưa?".
 * - TenantContext trả lời câu hỏi "request hiện tại thuộc tenant nào?".
 * - Hai context này liên quan nhưng không phải cùng một thứ.
 *
 * ==============================================================
 */
public class JwtTenantContextFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    public JwtTenantContextFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            SecurityContext securityContext = SecurityContextHolder.getContext();
            Authentication authentication = securityContext.getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                JwtClaims claims = jwtTokenService.extractClaims(jwt);
                Long tenantId = claims.tenantId();

                if (tenantId == null || tenantId <= 0) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid tenant_id claim");
                    return;
                }

                TenantContext.setCurrentTenant(tenantId);
            }

            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException e) {
            TenantContext.clear();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid tenant_id claim");
        } finally {
            TenantContext.clear();
        }
    }
}
