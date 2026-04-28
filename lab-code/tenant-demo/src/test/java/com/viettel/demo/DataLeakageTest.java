package com.viettel.demo;

/*
 * ==============================================================
 * TODO TASK: Integration Test — chứng minh KHÔNG có data leakage
 * ==============================================================
 *
 * [Mục tiêu]
 * Đây là test quan trọng nhất trong multi-tenant.
 * Test phải CHỨNG MINH rằng:
 * - Tenant A gọi API → chỉ thấy data của Tenant A.
 * - Tenant A KHÔNG BAO GIỜ thấy data của Tenant B.
 *
 * [Nhiệm vụ của tôi]
 * 1. Setup test database (có thể dùng Testcontainers hoặc H2).
 * 2. Insert data cho Tenant A và Tenant B.
 * 3. Test case 1: Gọi GET /api/master-data với header tenant A
 *    → assert: tất cả records trả về đều có tenantId == A.
 *    → assert: KHÔNG có record nào có tenantId == B.
 * 4. Test case 2: Gọi GET /api/master-data/{id} với id thuộc tenant B,
 *    nhưng header là tenant A → assert: trả về 404 hoặc 403.
 * 5. Test case 3: Gọi POST /api/master-data với header tenant A
 *    → assert: record được tạo có tenantId == A (auto-set).
 *
 * [Kiến thức cần tự research]
 * - @SpringBootTest
 * - @AutoConfigureMockMvc
 * - MockMvc: perform(), andExpect()
 * - Testcontainers PostgreSQL module (hoặc spring.datasource test config)
 * - org.assertj.core.api.Assertions
 * - Cách set custom header trong MockMvc request
 * - Đọc lại: docs/02-multi-tenant/tinh-huong-va-trade-off.md
 *   (tình huống 2: quên tenant filter)
 *
 * ==============================================================
 */

public class DataLeakageTest {

    // TODO: Setup test environment

    // TODO: Test case 1 — tenant A chỉ thấy data A

    // TODO: Test case 2 — tenant A không truy cập được data B

    // TODO: Test case 3 — create auto-set tenant_id

}
