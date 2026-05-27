package com.viettel.demo.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * ==============================================================
 * KeycloakRoleConverter — map Keycloak roles sang Spring authorities
 * ==============================================================
 *
 * [Vai trò]
 * Converter này chỉ đọc role claims trong Jwt đã được Spring Security
 * validate, rồi chuyển thành GrantedAuthority dạng ROLE_*.
 *
 * [Claim được hỗ trợ]
 * - Keycloak client roles: resource_access.<client-id>.roles
 * - Keycloak realm roles:  realm_access.roles
 * - Local JWT lab roles:   roles
 *
 * [Điều không làm trong converter]
 * - Không validate JWT signature/issuer/expiration.
 * - Không đọc hoặc set tenant_id.
 * - Không query database.
 * - Không quyết định tenant data scope.
 *
 * ==============================================================
 */
@Component
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ROLE_PREFIX = "ROLE_";

    private final AuthProperties authProperties;

    public KeycloakRoleConverter(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        addRoles(authorities, jwt.getClaimAsStringList("roles"));
        addRoles(authorities, extractRoles(jwt.getClaim("realm_access")));

        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            Object clientAccess = resourceAccess.get(authProperties.getKeycloak().getClientId());
            addRoles(authorities, extractRoles(clientAccess));
        }

        return authorities;
    }

    private List<String> extractRoles(Object accessClaim) {
        if (!(accessClaim instanceof Map<?, ?> accessMap)) {
            return List.of();
        }

        Object roles = accessMap.get("roles");
        if (!(roles instanceof List<?> values)) {
            return List.of();
        }

        return values.stream()
                .filter(value -> value != null && !value.toString().isBlank())
                .map(Object::toString)
                .toList();
    }

    private void addRoles(Set<GrantedAuthority> authorities, List<String> roles) {
        if (roles == null) {
            return;
        }

        for (String role : roles) {
            if (role != null && !role.isBlank()) {
                authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + role));
            }
        }
    }
}
