package com.viettel.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.LinkedHashSet;
import java.util.Set;

public final class JwtAuthenticationConverters {

    private JwtAuthenticationConverters() {
    }

    public static JwtAuthenticationConverter withDefaultScopesAndKeycloakRoles(String clientId) {
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
        KeycloakRoleConverter keycloakRoleConverter = new KeycloakRoleConverter(clientId);

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<GrantedAuthority> authorities = new LinkedHashSet<>();
            authorities.addAll(scopeConverter.convert(jwt));
            authorities.addAll(keycloakRoleConverter.convert(jwt));
            return authorities;
        });
        return converter;
    }
}
