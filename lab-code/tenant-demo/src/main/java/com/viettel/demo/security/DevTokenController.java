package com.viettel.demo.security;

/*
 * ==============================================================
 * DevTokenController — TODO skeleton tạo token local cho học tập
 * ==============================================================
 *
 * [Trạng thái hiện tại]
 * Class này CHƯA active:
 * - chưa có @RestController;
 * - chưa có @RequestMapping;
 * - chưa tạo token thật.
 *
 * [Mục tiêu nếu cần ở task sau]
 * Tạo endpoint local/demo-only để lấy token tenant 1 và tenant 2,
 * giúp curl/HTTP Client verify JWT flow mà chưa cần Keycloak thật.
 *
 * Ví dụ endpoint có thể cân nhắc:
 * - GET /api/dev/tokens/tenant-1
 * - GET /api/dev/tokens/tenant-2
 *
 * [Cảnh báo]
 * - Endpoint này chỉ dành cho local learning.
 * - Phải bị tắt bằng config như app.jwt.dev-token-enabled=false theo mặc định.
 * - Không đưa dev token endpoint vào production.
 *
 * [TODO khi tự code]
 * TODO 1: Inject JwtProperties và helper tạo token.
 * TODO 2: Chỉ bật controller nếu dev-token-enabled=true.
 * TODO 3: Tạo token có sub, tenant_id, roles, iss, iat, exp.
 * TODO 4: Không log token trong production/report công khai.
 *
 * ==============================================================
 */
public class DevTokenController {
    /*
     * TODO: Tự implement ở task sau nếu bạn chọn có dev token endpoint.
     */
}
