package com.viettel.audit.security;

import com.viettel.audit.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtTenantContextFilter extends OncePerRequestFilter {

    public static final String TENANT_ID_REQUEST_ATTRIBUTE = "tenantId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                Long tenantId = extractTenantId(jwt);
                if (tenantId == null || tenantId <= 0) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid tenant_id claim");
                    return;
                }

                TenantContext.setCurrentTenant(tenantId);
                request.setAttribute(TENANT_ID_REQUEST_ATTRIBUTE, tenantId);
            }

            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException e) {
            TenantContext.clear();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid tenant_id claim");
        } finally {
            TenantContext.clear();
        }
    }

    private Long extractTenantId(Jwt jwt) {
        Object rawTenantId = jwt.getClaim("tenant_id");
        if (rawTenantId instanceof Number number) {
            return number.longValue();
        }
        if (rawTenantId instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        return null;
    }
}
