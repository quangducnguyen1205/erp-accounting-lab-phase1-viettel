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
 * SecurityConfig — cấu hình security tối thiểu cho JWT tạm
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
 * - Đây KHÔNG phải Keycloak/OIDC production.
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
    JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
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
