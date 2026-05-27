package com.viettel.demo.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * ==============================================================
 * DevTokenController — endpoint tạo token local cho học tập
 * ==============================================================
 *
 * [Vai trò]
 * Endpoint local/demo-only để lấy token tenant 1 và tenant 2,
 * giúp curl/HTTP Client verify JWT flow khi chưa có Keycloak thật.
 *
 * Ví dụ endpoint có thể cân nhắc:
 * - GET /api/dev/tokens/tenant-1
 * - GET /api/dev/tokens/tenant-2
 * - GET /api/dev/tokens/tenant/{tenantId}
 *
 * [Cảnh báo]
 * - Endpoint này chỉ dành cho local learning.
 * - Có thể tắt bằng app.jwt.dev-token-enabled=false.
 * - Không đưa dev token endpoint vào production.
 *
 * ==============================================================
 */
@RestController
@RequestMapping("/api/dev/tokens")
public class DevTokenController {

    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;

    public DevTokenController(JwtTokenService jwtTokenService, JwtProperties jwtProperties) {
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
    }

    @GetMapping("/tenant-1")
    public Map<String, Object> tenantOneToken(@RequestParam(required = false) List<String> roles) {
        return createTokenResponse(1L, roles);
    }

    @GetMapping("/tenant-2")
    public Map<String, Object> tenantTwoToken(@RequestParam(required = false) List<String> roles) {
        return createTokenResponse(2L, roles);
    }

    @GetMapping("/tenant/{tenantId}")
    public Map<String, Object> tokenForTenant(@PathVariable Long tenantId, @RequestParam(required = false) List<String> roles) {
        return createTokenResponse(tenantId, roles);
    }

    private Map<String, Object> createTokenResponse(Long tenantId, List<String> roles) {
        if (!jwtProperties.isDevTokenEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dev token endpoint is disabled");
        }
        if (tenantId == null || tenantId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenantId must be positive");
        }

        String subject = "dev-user-tenant-" + tenantId;
        String token = jwtTokenService.createDevToken(tenantId, subject, roles);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tokenType", "Bearer");
        response.put("accessToken", token);
        response.put("tenantId", tenantId);
        response.put("subject", subject);
        response.put("expiresIn", jwtProperties.getExpirationSeconds());
        return response;
    }
}
