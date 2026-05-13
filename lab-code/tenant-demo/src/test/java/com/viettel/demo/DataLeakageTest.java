package com.viettel.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * ==============================================================
 * TODO TASK: DataLeakageTest — test chống lộ dữ liệu giữa tenant
 * ==============================================================
 *
 * [Mục tiêu học]
 * Tự viết test để chứng minh API master_data đã tenant-aware:
 * - Tenant 1 chỉ thấy data tenant 1.
 * - Tenant 2 chỉ thấy data tenant 2.
 * - Tenant 1 không truy cập được id thuộc tenant 2.
 * - Query by code vẫn scoped theo tenant hiện tại.
 * - Missing/invalid X-Tenant-Id bị chặn ở TenantFilter.
 *
 * [Test strategy đề xuất]
 * 1. Dùng @SpringBootTest để load application context thật.
 * 2. Dùng @AutoConfigureMockMvc để gọi API bằng MockMvc, không cần mở port 8080.
 * 3. Dùng @BeforeEach để chuẩn bị fixture cố định cho mỗi test.
 * 4. Dùng JdbcTemplate hoặc repository để tạo dữ liệu test, nhưng KHÔNG phụ thuộc
 *    vào dữ liệu local đang có sẵn trong database.
 * 5. Assert status code trước, sau đó assert JSON body/tenantId.
 *
 * [Vì sao dùng MockMvc?]
 * MockMvc cho request đi qua Spring MVC và filter chain, nên test được:
 *
 * MockMvc -> TenantFilter -> Controller -> Service -> Repository -> DB
 *
 * Đây là đúng mục tiêu hơn so với gọi Service trực tiếp, vì gọi Service trực tiếp
 * sẽ bỏ qua hành vi thiếu/sai X-Tenant-Id ở TenantFilter.
 *
 * [Gợi ý import khi bắt đầu tự code]
 * import org.junit.jupiter.api.BeforeEach;
 * import org.junit.jupiter.api.Test;
 * import org.springframework.beans.factory.annotation.Autowired;
 * import org.springframework.jdbc.core.JdbcTemplate;
 * import org.springframework.test.web.servlet.MockMvc;
 *
 * import static org.hamcrest.Matchers.everyItem;
 * import static org.hamcrest.Matchers.is;
 * import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
 * import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
 * import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
 *
 * [Gợi ý fixture]
 * - tenant 1: id = 1, code = "TENANT_A"
 * - tenant 2: id = 2, code = "TENANT_B"
 * - tenant 1 có record id = 101, code = "LAPTOP-01"
 * - tenant 2 có record id = 201, code = "LAPTOP-01"
 *
 * Gợi ý dùng JdbcTemplate trong @BeforeEach:
 * - DELETE FROM master_data;
 * - DELETE FROM tenants;
 * - INSERT INTO tenants (...);
 * - INSERT INTO master_data (...);
 *
 * Lưu ý:
 * - Nếu dùng repository.save(...) để tạo MasterData, @PrePersist cần TenantContext.
 * - Với fixture test, JdbcTemplate thường dễ hiểu hơn vì insert explicit tenant_id.
 *
 * [TODO cases cần tự implement]
 *
 * TODO 1:
 * tenant_1_list_should_only_return_tenant_1_data()
 * - GET /api/master-data với header X-Tenant-Id = 1
 * - expect 200
 * - expect mọi item trong JSON có tenantId = 1
 * - expect không có tenantId = 2
 *
 * TODO 2:
 * tenant_2_list_should_only_return_tenant_2_data()
 * - GET /api/master-data với header X-Tenant-Id = 2
 * - expect 200
 * - expect mọi item trong JSON có tenantId = 2
 *
 * TODO 3:
 * tenant_1_should_not_access_tenant_2_id()
 * - tenant 2 có record id = 201
 * - GET /api/master-data/201 với header X-Tenant-Id = 1
 * - expect 404
 *
 * TODO 4:
 * query_by_code_should_remain_scoped_by_tenant()
 * - tenant 1 và tenant 2 cùng có code "LAPTOP-01"
 * - GET /api/master-data/code/LAPTOP-01 với tenant 1 -> trả tenantId = 1
 * - GET /api/master-data/code/LAPTOP-01 với tenant 2 -> trả tenantId = 2
 *
 * TODO 5:
 * missing_tenant_header_should_return_400()
 * - GET /api/master-data không có X-Tenant-Id
 * - expect 400
 *
 * TODO 6:
 * invalid_tenant_header_should_return_400()
 * - GET /api/master-data với X-Tenant-Id = "abc"
 * - expect 400
 *
 * [Điều không làm trong task này]
 * - Không thêm JWT/Spring Security.
 * - Không thêm Testcontainers.
 * - Không thêm Swagger/OpenAPI.
 * - Không sửa production service/controller/repository nếu không có lỗi compile.
 *
 * Đọc trước khi code:
 * docs/04-spring-boot/testing-tenant-isolation.md
 *
 * ==============================================================
 */
@SpringBootTest
@AutoConfigureMockMvc
public class DataLeakageTest {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    // TODO: @Autowired MockMvc mockMvc;
    @Autowired
    MockMvc mockMvc;

    // TODO: @Autowired JdbcTemplate jdbcTemplate;
    @Autowired
    JdbcTemplate jdbcTemplate;
    /*
     * TODO: Viết @BeforeEach setUp()
     *
     * Gợi ý:
     * - Xóa master_data trước, rồi xóa tenants.
     * - Insert tenant 1 và tenant 2.
     * - Insert master_data cho cả hai tenant.
     * - Dùng id cố định như 101 và 201 để test cross-tenant dễ hiểu.
     */
    @BeforeEach
    void setup() {
        // Xóa dữ liệu cũ
        jdbcTemplate.execute("DELETE FROM master_data");
        jdbcTemplate.execute("DELETE FROM tenants");

        // Insert tenant 1 và tenant 2
        jdbcTemplate.update("INSERT INTO tenants (id, code, name) VALUES (?, ?, ?)", 1L, "TENANT_A", "Tenant A");
        jdbcTemplate.update("INSERT INTO tenants (id, code, name) VALUES (?, ?, ?)", 2L, "TENANT_B", "Tenant B");

        // Insert master_data cho tenant 1 và tenant 2
        jdbcTemplate.update(
                "INSERT INTO master_data (id, tenant_id, code, name, category, is_active) VALUES (?, ?, ?, ?, ?, ?)",
                101L, 1L, "LAPTOP-01", "Laptop Dell Latitude 5540" , "ELECTRONICS", true
        );
        jdbcTemplate.update(
                "INSERT INTO master_data (id, tenant_id, code, name, category, is_active) VALUES (?, ?, ?, ?, ?, ?)",
                201L, 2L, "LAPTOP-01", "Laptop HP EliteBook 840 G9", "ELECTRONICS", true
        );
    }
    /*
     * TODO: Test case 1
     * tenant 1 list should only return tenant 1 data.
     */
    @Test
    void tenant_1_list_should_only_return_tenant_1_data() throws Exception {
        mockMvc.perform(get("/api/master-data")
                        .header(TENANT_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tenantId", everyItem(is(1))));
    }

    /*
     * TODO: Test case 2
     * tenant 2 list should only return tenant 2 data.
     */
    @Test
    void tenant_2_list_should_only_return_tenant_2_data() throws Exception {
        mockMvc.perform(get("/api/master-data")
                        .header(TENANT_HEADER, "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tenantId", everyItem(is(2))));
    }
    /*
     * TODO: Test case 3
     * tenant 1 cannot access tenant 2 id.
     */
    @Test
    void tenant_1_should_not_access_tenant_2_id() throws Exception {
        mockMvc.perform(get("/api/master-data/201")
                        .header(TENANT_HEADER, "1"))
                .andExpect(status().isNotFound());
    }
    /*
     * TODO: Test case 4
     * query by code remains scoped by tenant.
     */
    @Test
    void query_by_code_should_remain_scoped_by_tenant() throws Exception {
        // Tenant 1 query by code
        mockMvc.perform(get("/api/master-data/code/LAPTOP-01")
                        .header(TENANT_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId", is(1)));
    }
    /*
     * TODO: Test case 5
     * missing X-Tenant-Id returns 400.
     */
    @Test
    void missing_tenant_header_should_return_400() throws Exception {
        mockMvc.perform(get("/api/master-data"))
                .andExpect(status().isBadRequest());
    }
    /*
     * TODO: Test case 6
     * invalid X-Tenant-Id returns 400.
     */
    @Test
    void invalid_tenant_header_should_return_400() throws Exception {
        mockMvc.perform(get("/api/master-data")
                        .header(TENANT_HEADER, "abc"))
                .andExpect(status().isBadRequest());
    }
}
