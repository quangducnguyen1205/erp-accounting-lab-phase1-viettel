package com.viettel.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/*
 * Bridges a Spring-validated Jwt into TenantContext for one request.
 *
 * The filter does not validate tokens itself. It must run after Spring
 * Security's BearerTokenAuthenticationFilter.
 */
public class JwtTenantContextFilter extends OncePerRequestFilter {

    public static final String TENANT_ID_REQUEST_ATTRIBUTE = SecurityConstants.TENANT_ID_REQUEST_ATTRIBUTE;

    private final TenantClaimResolver tenantClaimResolver;

    public JwtTenantContextFilter() {
        this(new TenantClaimResolver());
    }

    public JwtTenantContextFilter(TenantClaimResolver tenantClaimResolver) {
        this.tenantClaimResolver = tenantClaimResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                Long tenantId = tenantClaimResolver.resolveTenantId(jwt);
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
}
