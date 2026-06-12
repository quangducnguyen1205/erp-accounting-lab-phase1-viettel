package com.viettel.audit.security;

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
 * Maps Keycloak realm/client roles to Spring Security ROLE_* authorities.
 *
 * This is intentionally duplicated instead of imported from tenant-demo:
 * audit-log-service is a separate service boundary in this lab.
 */
@Component
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ROLE_PREFIX = "ROLE_";

    private final AuditSecurityProperties securityProperties;

    public KeycloakRoleConverter(AuditSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        addRoles(authorities, jwt.getClaimAsStringList("roles"));
        addRoles(authorities, extractRoles(jwt.getClaim("realm_access")));

        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            Object clientAccess = resourceAccess.get(securityProperties.getClientId());
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
