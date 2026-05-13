package com.viettel.demo.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/*
 * ==============================================================
 * JwtProperties — bind config JWT từ application.yml/env vars
 * ==============================================================
 *
 * [Mục tiêu học]
 * Gom các cấu hình JWT vào một object có type rõ ràng thay vì rải rác
 * nhiều @Value trong các class khác nhau.
 *
 * [Nguồn config]
 * application.yml:
 *
 * app:
 *   jwt:
 *     enabled: ${JWT_ENABLED:false}
 *     secret: ${JWT_SECRET:}
 *     issuer: ${JWT_ISSUER:tenant-demo-local}
 *     expiration-seconds: ${JWT_EXPIRATION_SECONDS:3600}
 *     dev-token-enabled: ${JWT_DEV_TOKEN_ENABLED:false}
 *
 * [Lưu ý]
 * - JWT_SECRET là secret local để ký/verify token tạm.
 * - Trong production/Keycloak, backend thường validate token qua issuer/JWK,
 *   không tự giữ shared secret kiểu lab này.
 * - Class này compile và bind được, nhưng JWT feature chưa active.
 *
 * ==============================================================
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private boolean enabled;
    private String secret;
    private String issuer = "tenant-demo-local";
    private long expirationSeconds = 3600;
    private boolean devTokenEnabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public void setExpirationSeconds(long expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
    }

    public boolean isDevTokenEnabled() {
        return devTokenEnabled;
    }

    public void setDevTokenEnabled(boolean devTokenEnabled) {
        this.devTokenEnabled = devTokenEnabled;
    }
}
