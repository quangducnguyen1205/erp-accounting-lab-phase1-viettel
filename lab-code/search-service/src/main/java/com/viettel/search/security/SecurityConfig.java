package com.viettel.search.security;

import com.viettel.common.security.JwtAuthenticationConverters;
import com.viettel.common.security.JwtTenantContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtTenantContextFilter jwtTenantContextFilter,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/prometheus", "/actuator/prometheus/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/search/master-data/reindex")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/search/master-data", "/api/search/master-data/**")
                        .hasAnyRole("ADMIN", "ACCOUNTANT", "VIEWER")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
                ))
                .addFilterAfter(jwtTenantContextFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }

    @Bean
    JwtTenantContextFilter jwtTenantContextFilter() {
        return new JwtTenantContextFilter();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(SearchSecurityProperties securityProperties) {
        return JwtAuthenticationConverters.withDefaultScopesAndKeycloakRoles(securityProperties.getClientId());
    }

    @Bean
    JwtDecoder jwtDecoder(SearchSecurityProperties securityProperties) {
        String issuerUri = securityProperties.getIssuerUri();
        String jwkSetUri = securityProperties.getJwkSetUri();

        if (jwkSetUri != null && !jwkSetUri.isBlank()) {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
            decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
            return decoder;
        }

        return JwtDecoders.fromIssuerLocation(issuerUri);
    }
}
