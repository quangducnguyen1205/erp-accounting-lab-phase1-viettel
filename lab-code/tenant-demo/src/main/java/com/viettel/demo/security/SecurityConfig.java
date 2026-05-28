package com.viettel.demo.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/*
 * ==============================================================
 * SecurityConfig — cấu hình security cho JWT tạm và Keycloak mode
 * ==============================================================
 *
 * [Vai trò]
 * SecurityConfig khai báo SecurityFilterChain: request nào public,
 * request nào cần JWT, app chạy stateless hay session-based, và custom
 * filter tenant context đứng ở đâu.
 *
 * [Local JWT fallback trong lab]
 * - Dùng Bearer JWT local, ký bằng HS256 secret từ app.jwt.secret.
 * - Spring Security validate chữ ký, expiration và issuer.
 * - JwtTenantContextFilter chạy sau BearerTokenAuthenticationFilter để
 *   đọc tenant_id từ Jwt đã validate.
 * - Mode này giữ vai trò fallback/test path, nên chỉ check authenticated
 *   và tenant isolation; RBAC chính nằm ở Keycloak mode.
 *
 * [Keycloak mode]
 * - app.auth.mode=keycloak dùng token do Keycloak phát hành.
 * - JwtDecoder chuyển từ HS256 secret local sang issuer-uri/JWKS.
 * - JwtTenantContextFilter vẫn giữ nguyên vai trò đọc tenant_id sau khi
 *   Spring Security đã validate token.
 *
 * [Không làm trong Phase này]
 * - Không làm role matrix/RBAC đầy đủ.
 * - Không thêm OAuth2 login flow.
 * - Không tạo session-based login.
 *
 * [Sprint 12 - RBAC]
 * JwtAuthenticationConverter map Keycloak roles thành GrantedAuthority.
 * JwtTenantContextFilter vẫn chỉ xử lý tenant_id, không trộn role mapping
 * vào filter đó.
 *
 * ==============================================================
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthProperties authProperties,
            JwtProperties jwtProperties,
            JwtTokenService jwtTokenService,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (authProperties.isLocalJwtMode()) {
            if (!jwtProperties.isEnabled()) {
                /*
                 * Legacy mode cho học tập: khi app.jwt.enabled=false,
                 * TenantFilter cũ dùng X-Tenant-Id sẽ active và Spring Security
                 * không ép Bearer token. Không dùng mode này cho production.
                 */
                return http
                        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                        .build();
            } else {
                /*
                 * Local JWT fallback:
                 * - Dùng cho app-test/DataLeakageTest và dev token local.
                 * - Chỉ yêu cầu request đã authenticated.
                 * - Không ép RBAC ở đây để test tenant isolation không phụ
                 *   thuộc live Keycloak/role setup.
                 *
                 * Keycloak mode bên dưới mới là đường demo chính cho RBAC.
                 */
                return http
                        .authorizeHttpRequests(auth -> auth
                                .requestMatchers("/api/dev/tokens/**").permitAll()
                                .requestMatchers("/api/master-data", "/api/master-data/**").authenticated()
                                .requestMatchers("/api/search/master-data", "/api/search/master-data/**").authenticated()
                                .anyRequest().authenticated()
                        )
                        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
                        ))
                        .addFilterAfter(
                                new JwtTenantContextFilter(jwtTokenService),
                                BearerTokenAuthenticationFilter.class
                        )
                        .build();
            }
        }

        /*
         * Keycloak mode vẫn dùng Resource Server JWT.
         * Phần khác biệt chính so với local-jwt nằm ở JwtDecoder bean:
         * decoder sẽ dùng issuer-uri/JWKS thay vì local HS256 secret.
         *
         * Dev token endpoint không public trong mode này; token phải lấy từ
         * Keycloak mini-lab.
         */
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/master-data", "/api/master-data/**")
                        .hasAnyRole("ADMIN", "ACCOUNTANT", "VIEWER")
                        .requestMatchers("/api/master-data", "/api/master-data/**")
                        .hasAnyRole("ADMIN", "ACCOUNTANT")
                        .requestMatchers(HttpMethod.POST, "/api/search/master-data/reindex")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/search/master-data", "/api/search/master-data/**")
                        .hasAnyRole("ADMIN", "ACCOUNTANT", "VIEWER")
                        .requestMatchers(HttpMethod.GET, "/api/files", "/api/files/**")
                        .hasAnyRole("ADMIN", "ACCOUNTANT", "VIEWER")
                        .requestMatchers("/api/files", "/api/files/**")
                        .hasAnyRole("ADMIN", "ACCOUNTANT")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
                ))
                .addFilterAfter(
                        new JwtTenantContextFilter(jwtTokenService),
                        BearerTokenAuthenticationFilter.class
                )
                .build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(KeycloakRoleConverter keycloakRoleConverter) {
        /*
         * Converter này chạy theo từng Jwt đã validate, không chạy lúc app startup.
         * Scope OAuth2 mặc định vẫn thành SCOPE_*, còn Keycloak/local roles
         * được map thêm thành ROLE_* để dùng hasRole(...) hoặc @PreAuthorize.
         */
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<GrantedAuthority> authorities = new LinkedHashSet<>();
            authorities.addAll(scopeConverter.convert(jwt));
            authorities.addAll(keycloakRoleConverter.convert(jwt));
            return authorities;
        });
        return converter;
    }

    @Bean
    JwtDecoder jwtDecoder(AuthProperties authProperties, JwtProperties jwtProperties) {
        if (authProperties.isKeycloakMode()) {
            /*
             * Keycloak mode:
             * - Ưu tiên issuer-uri để Spring Security tự đọc OIDC metadata
             *   và tìm jwks_uri.
             * - Nếu cấu hình jwk-set-uri trực tiếp, vẫn validate issuer để
             *   tránh nhận token từ realm khác.
             */
            String issuerUri = authProperties.getKeycloak().getIssuerUri();
            if (issuerUri == null || issuerUri.isBlank()) {
                throw new IllegalStateException("KEYCLOAK_ISSUER_URI must be configured when APP_AUTH_MODE=keycloak");
            }

            String jwkSetUri = authProperties.getKeycloak().getJwkSetUri();
            if (jwkSetUri != null && !jwkSetUri.isBlank()) {
                NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
                decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
                return decoder;
            }

            return JwtDecoders.fromIssuerLocation(issuerUri);
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
