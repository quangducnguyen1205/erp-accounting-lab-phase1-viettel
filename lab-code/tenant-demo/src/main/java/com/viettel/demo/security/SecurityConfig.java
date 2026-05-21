package com.viettel.demo.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/*
 * ==============================================================
 * SecurityConfig — cấu hình security cho JWT tạm và skeleton Keycloak
 * ==============================================================
 *
 * [Vai trò]
 * SecurityConfig khai báo SecurityFilterChain: request nào public,
 * request nào cần JWT, app chạy stateless hay session-based, và custom
 * filter tenant context đứng ở đâu.
 *
 * [JWT tạm trong lab]
 * - Dùng Bearer JWT local, ký bằng HS256 secret từ app.jwt.secret.
 * - Spring Security validate chữ ký, expiration và issuer.
 * - JwtTenantContextFilter chạy sau BearerTokenAuthenticationFilter để
 *   đọc tenant_id từ Jwt đã validate.
 *
 * [Keycloak skeleton]
 * - app.auth.mode=keycloak sẽ là mode dùng token từ Keycloak.
 * - Khi đó JwtDecoder cần chuyển từ HS256 secret local sang issuer-uri/JWKS.
 * - Task này chỉ chuẩn bị cấu hình/TODO để mình tự code phần switch đó.
 *
 * [Không làm trong Phase này]
 * - Không tích hợp Keycloak thật.
 * - Không làm role matrix/RBAC đầy đủ.
 * - Không thêm OAuth2 login flow.
 * - Không tạo session-based login.
 *
 * ==============================================================
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthProperties authProperties,
            JwtProperties jwtProperties,
            JwtTokenService jwtTokenService
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (!jwtProperties.isEnabled()) {
            /*
             * Legacy mode cho học tập: khi app.jwt.enabled=false,
             * TenantFilter cũ dùng X-Tenant-Id sẽ active và Spring Security
             * không ép Bearer token. Không dùng mode này cho production.
             */
            return http
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }

        /*
         * TODO(KEYCLOAK-INTEGRATION):
         * Khi tự code Keycloak mode:
         * - Giữ authorization rules đơn giản như hiện tại cho /api/master-data/**.
         * - Cân nhắc tắt /api/dev/tokens/** khi app.auth.mode=keycloak.
         * - Không đổi JwtTenantContextFilter: filter vẫn đọc tenant_id từ Jwt
         *   đã validate, dù token đến từ local JWT hay Keycloak.
         */
        if (authProperties.isKeycloakMode()) {
            // Skeleton marker: SecurityFilterChain vẫn giống Resource Server JWT.
            // Phần khác biệt thật nằm ở JwtDecoder bean bên dưới.
        }

        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/dev/tokens/**").permitAll()
                        .requestMatchers("/api/master-data/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .addFilterAfter(
                        new JwtTenantContextFilter(jwtTokenService),
                        BearerTokenAuthenticationFilter.class
                )
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(AuthProperties authProperties, JwtProperties jwtProperties) {
        if (authProperties.isKeycloakMode()) {
            /*
             * TODO(KEYCLOAK-INTEGRATION):
             * Tự thay nhánh này bằng decoder dùng issuer-uri/JWKS, ví dụ:
             *
             * - issuer-uri:
             *   JwtDecoders.fromIssuerLocation(authProperties.getKeycloak().getIssuerUri())
             *
             * - hoặc jwk-set-uri:
             *   NimbusJwtDecoder.withJwkSetUri(authProperties.getKeycloak().getJwkSetUri()).build()
             *
             * Lý do chưa implement ngay:
             * - giữ local JWT mode/test hiện tại không bị thay đổi ngầm;
             * - buộc mình tự đọc token metadata và hiểu issuer/JWKS trước khi bật.
             */
            throw new IllegalStateException(
                    "Keycloak mode is prepared but JwtDecoder issuer-uri/JWKS switch is still TODO"
            );
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtProperties.hmacSecretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(jwtProperties.getIssuer())
        );
        return decoder;
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties jwtProperties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtProperties.hmacSecretKey()));
    }
}
