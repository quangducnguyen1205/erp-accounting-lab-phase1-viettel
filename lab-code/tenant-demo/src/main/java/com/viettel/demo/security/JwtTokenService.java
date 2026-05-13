package com.viettel.demo.security;

/*
 * ==============================================================
 * JwtTokenService — TODO skeleton cho dev token local
 * ==============================================================
 *
 * [Thiết kế khuyến nghị]
 * Không dùng service này để tự viết JWT parser/validator thủ công.
 *
 * JWT validation nên để Spring Security Resource Server/JwtDecoder xử lý.
 * Service này, nếu dùng, chỉ nên phục vụ phần local learning như:
 * 1. tạo dev token cho tenant 1/tenant 2;
 * 2. gom logic tạo claim mẫu;
 * 3. hỗ trợ DevTokenController local-only.
 *
 * [Nguồn config dự kiến]
 * - JwtProperties.enabled
 * - JwtProperties.secret
 * - JwtProperties.issuer
 * - JwtProperties.expirationSeconds
 * - JwtProperties.devTokenEnabled
 *
 * [Quan trọng]
 * - JWT_SECRET không phải password user.
 * - Không hardcode secret thật trong code.
 * - Không nhầm JWT tạm với Keycloak/OIDC production.
 *
 * [TODO khi bắt đầu code]
 * TODO 1: Chọn dependency JWT tối thiểu sau khi đọc lại docs.
 * TODO 2: Inject JwtProperties thay vì rải rác @Value.
 * TODO 3: Viết method tạo dev token cho tenant 1/tenant 2 nếu cần.
 * TODO 4: Không tự validate token ở đây nếu đã dùng Spring Security JwtDecoder.
 * TODO 5: Viết test/curl verification trước khi thay TenantFilter.
 *
 * Gợi ý method signature, chưa cần mở comment ngay:
 *
 * String createDevToken(Long tenantId, String subject)
 *
 * ==============================================================
 */
public class JwtTokenService {
    /*
     * TODO: Tự implement ở task sau.
     *
     * Giữ class rỗng ở bước skeleton để:
     * - không kéo dependency JWT/Spring Security quá sớm;
     * - không làm thay đổi runtime behavior hiện tại;
     * - không phá DataLeakageTest đang dùng X-Tenant-Id.
     */
}
