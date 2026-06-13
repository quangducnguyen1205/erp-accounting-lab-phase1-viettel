package com.viettel.audit.observability;

import com.viettel.common.security.TenantContext;
import com.viettel.common.security.JwtTenantContextFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID = "requestId";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod())
                && "/actuator/health".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        long startedAt = System.nanoTime();

        MDC.put(MDC_REQUEST_ID, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            Long tenantId = resolveTenantId(request);
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

            if (tenantId == null) {
                log.info(
                        "HTTP request completed method={} path={} status={} durationMs={} requestId={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        durationMs,
                        requestId
                );
            } else {
                log.info(
                        "HTTP request completed method={} path={} status={} durationMs={} requestId={} tenantId={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        durationMs,
                        requestId,
                        tenantId
                );
            }

            MDC.remove(MDC_REQUEST_ID);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String headerValue = request.getHeader(REQUEST_ID_HEADER);
        if (headerValue != null && !headerValue.isBlank()) {
            return headerValue.trim();
        }
        return UUID.randomUUID().toString();
    }

    private Long resolveTenantId(HttpServletRequest request) {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return tenantId;
        }

        Object tenantIdAttribute = request.getAttribute(JwtTenantContextFilter.TENANT_ID_REQUEST_ATTRIBUTE);
        if (tenantIdAttribute instanceof Long value) {
            return value;
        }
        return null;
    }
}
