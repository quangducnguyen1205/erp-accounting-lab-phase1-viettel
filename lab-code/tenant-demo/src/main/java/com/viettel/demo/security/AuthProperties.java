package com.viettel.demo.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/*
 * ==============================================================
 * AuthProperties — cấu hình chọn mode xác thực cho lab
 * ==============================================================
 *
 * [Hiện tại]
 * - local-jwt: mode đang chạy ổn, dùng JWT tạm do tenant-demo tạo.
 *
 * [Chuẩn bị]
 * - keycloak: mode dùng token do Keycloak phát hành.
 *   SecurityConfig sẽ tạo JwtDecoder từ issuer-uri/JWKS.
 *
 * Mục tiêu của class này là gom config chuyển mode vào một nơi rõ ràng,
 * không rải rác string env var trong SecurityConfig.
 *
 * ==============================================================
 */
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    /*
     * Giá trị dự kiến:
     * - local-jwt: dùng JWT tạm HS256 hiện tại.
     * - keycloak: dùng issuer-uri/JWKS từ Keycloak local.
     *
     * Không bật keycloak mode mặc định để DataLeakageTest và demo JWT tạm
     * vẫn chạy ổn khi không khởi động Keycloak.
     */
    private String mode = "local-jwt";

    private Keycloak keycloak = new Keycloak();

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Keycloak getKeycloak() {
        return keycloak;
    }

    public void setKeycloak(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    public boolean isLocalJwtMode() {
        return "local-jwt".equalsIgnoreCase(mode);
    }

    public boolean isKeycloakMode() {
        return "keycloak".equalsIgnoreCase(mode);
    }

    public static class Keycloak {
        /*
         * Ví dụ local:
         * http://localhost:18080/realms/viettel-lab
         *
         * Đây phải khớp claim `iss` trong access token Keycloak.
         */
        private String issuerUri;

        /*
         * Optional. Spring Security thường có thể discovery JWKS từ issuer-uri.
         * Chỉ dùng khi muốn trỏ thẳng tới endpoint certs/JWKS.
         */
        private String jwkSetUri;

        /*
         * Client chứa client roles trong claim:
         * resource_access.<client-id>.roles
         */
        private String clientId = "tenant-demo-api-client";

        public String getIssuerUri() {
            return issuerUri;
        }

        public void setIssuerUri(String issuerUri) {
            this.issuerUri = issuerUri;
        }

        public String getJwkSetUri() {
            return jwkSetUri;
        }

        public void setJwkSetUri(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
    }
}
