package com.viettel.common.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;

public class TenantClaimResolver {

    public Long resolveTenantId(Jwt jwt) {
        Objects.requireNonNull(jwt, "jwt must not be null");

        Object rawTenantId = jwt.getClaim(SecurityConstants.TENANT_ID_CLAIM);
        if (rawTenantId instanceof Number number) {
            return number.longValue();
        }
        if (rawTenantId instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        throw new IllegalArgumentException("tenant_id claim is missing or invalid");
    }
}
