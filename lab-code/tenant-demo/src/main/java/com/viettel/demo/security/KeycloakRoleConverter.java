package com.viettel.demo.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;

/*
 * ==============================================================
 * KeycloakRoleConverter — skeleton cho mini-lab RBAC
 * ==============================================================
 *
 * [Trạng thái]
 * Class này CHƯA được wire vào SecurityConfig, nên không đổi runtime hiện tại.
 * Mục tiêu là để lại hình dạng code cho task tự implement tiếp theo.
 *
 * [Việc cần tự code]
 * - Đọc role từ resource_access.<client-id>.roles hoặc realm_access.roles.
 * - Map role Keycloak thành GrantedAuthority, ví dụ ROLE_VIEWER.
 * - Dùng Set để tránh duplicate authority.
 * - Không đọc tenant_id ở đây; tenant_id vẫn thuộc JwtTenantContextFilter.
 *
 * [Điều không làm trong converter]
 * - Không validate JWT signature/issuer/expiration.
 * - Không query database.
 * - Không quyết định tenant data scope.
 *
 * ==============================================================
 */
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        /*
         * TODO tự implement ở Sprint 12:
         *
         * 1. Chọn client id, ví dụ "tenant-demo-api-client".
         * 2. Đọc jwt.getClaim("resource_access").
         * 3. Lấy roles của client đó.
         * 4. Optional: đọc thêm jwt.getClaim("realm_access").
         * 5. Convert từng role sang SimpleGrantedAuthority("ROLE_" + role).
         * 6. Return collection authorities.
         *
         * Sau đó wire converter này vào JwtAuthenticationConverter trong
         * SecurityConfig.
         */
        return List.of();
    }
}
