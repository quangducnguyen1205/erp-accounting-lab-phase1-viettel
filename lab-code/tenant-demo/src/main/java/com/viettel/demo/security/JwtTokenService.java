package com.viettel.demo.security;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/*
 * ==============================================================
 * JwtTokenService — tạo dev token và đọc claim đã validate
 * ==============================================================
 *
 * [Vai trò trong lab]
 * Service này KHÔNG tự parse/verify JWT thủ công.
 * Spring Security Resource Server/JwtDecoder chịu trách nhiệm validate
 * chữ ký, expiration và issuer trước.
 *
 * Service này chỉ tạo dev token cho tenant 1/tenant 2.
 * Phần đọc tenant_id từ Jwt đã validate nằm trong common-security.
 *
 * [Quan trọng]
 * - JWT_SECRET không phải password user.
 * - Không hardcode secret thật trong code.
 * - Không nhầm JWT tạm với Keycloak/OIDC production.
 *
 * ==============================================================
 */
@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public JwtTokenService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    public String createDevToken(Long tenantId, String subject) {
        return createDevToken(tenantId, subject, List.of("USER"));
    }

    public String createDevToken(Long tenantId, String subject, List<String> roles) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject cannot be blank");
        }

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(jwtProperties.getExpirationSeconds()))
                .subject(subject)
                .claim("tenant_id", tenantId)
                .claim("roles", roles == null ? List.of() : roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
