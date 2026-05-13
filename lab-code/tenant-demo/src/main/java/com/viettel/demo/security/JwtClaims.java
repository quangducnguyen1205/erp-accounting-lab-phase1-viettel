package com.viettel.demo.security;

import java.util.List;

/*
 * ==============================================================
 * JwtClaims — dữ liệu đã đọc ra từ JWT tạm
 * ==============================================================
 *
 * [Mục tiêu học]
 * Record này chỉ là object trung gian để gom các claim quan trọng
 * sau khi Spring Security đã validate token.
 *
 * [Claim dự kiến dùng trong lab]
 * - tenantId: lấy từ claim "tenant_id", dùng để set TenantContext.
 * - subject: lấy từ claim "sub", đại diện cho user hiện tại.
 * - roles: lấy từ claim "roles", chỉ để chuẩn bị cho RBAC awareness sau này.
 *
 * [Lưu ý]
 * - Đây chưa phải authorization model đầy đủ.
 * - Không lấy tenantId từ request body.
 * - Không log toàn bộ token hoặc claim nhạy cảm trong production.
 *
 * ==============================================================
 */
public record JwtClaims(
        Long tenantId,
        String subject,
        List<String> roles
) {
}
