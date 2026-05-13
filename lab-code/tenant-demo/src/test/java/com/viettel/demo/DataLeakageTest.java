package com.viettel.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * ==============================================================
 * DataLeakageTest — regression test chống lộ dữ liệu giữa tenant
 * ==============================================================
 *
 * [Mục tiêu học đã áp dụng]
 * Test chứng minh API master_data đã tenant-aware:
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
 * [Fixture dùng trong test]
 * - tenant 1: id = 1, code = "TENANT_A"
 * - tenant 2: id = 2, code = "TENANT_B"
 * - tenant 1 có record id = 101, code = "LAPTOP-01"
 * - tenant 2 có record id = 201, code = "LAPTOP-01"
 *
 * Lưu ý:
 * - Nếu dùng repository.save(...) để tạo MasterData, @PrePersist cần TenantContext.
 * - Với fixture test, JdbcTemplate thường dễ hiểu hơn vì insert explicit tenant_id.
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /*
     * Mỗi test tự chuẩn bị dữ liệu để không phụ thuộc vào DB local đang có gì.
     * Vì test dùng DB local thật của lab, dữ liệu master_data/tenants sẽ được
     * reset về fixture tối thiểu trước từng test case.
     */
    @BeforeEach
    void setup() {
        jdbcTemplate.execute("DELETE FROM master_data");
        jdbcTemplate.execute("DELETE FROM tenants");

        jdbcTemplate.update("INSERT INTO tenants (id, code, name) VALUES (?, ?, ?)", 1L, "TENANT_A", "Tenant A");
        jdbcTemplate.update("INSERT INTO tenants (id, code, name) VALUES (?, ?, ?)", 2L, "TENANT_B", "Tenant B");

        jdbcTemplate.update(
                "INSERT INTO master_data (id, tenant_id, code, name, category, is_active) VALUES (?, ?, ?, ?, ?, ?)",
                101L, 1L, "LAPTOP-01", "Laptop Dell Latitude 5540", "ELECTRONICS", true
        );
        jdbcTemplate.update(
                "INSERT INTO master_data (id, tenant_id, code, name, category, is_active) VALUES (?, ?, ?, ?, ?, ?)",
                201L, 2L, "LAPTOP-01", "Laptop HP EliteBook 840 G9", "ELECTRONICS", true
        );
    }

    /*
     * Tenant 1 list phải có dữ liệu và mọi item đều thuộc tenant 1.
     * Assert size giúp tránh tình huống mảng rỗng nhưng everyItem vẫn pass.
     */
    @Test
    void tenant_1_list_should_only_return_tenant_1_data() throws Exception {
        mockMvc.perform(get("/api/master-data")
                        .header(TENANT_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[*].tenantId", everyItem(is(1))));
    }

    /*
     * Tenant 2 list phải có dữ liệu và mọi item đều thuộc tenant 2.
     */
    @Test
    void tenant_2_list_should_only_return_tenant_2_data() throws Exception {
        mockMvc.perform(get("/api/master-data")
                        .header(TENANT_HEADER, "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[*].tenantId", everyItem(is(2))));
    }

    /*
     * Chứng minh id 201 thật sự tồn tại trong tenant 2, nhưng tenant 1 không
     * truy cập được id đó. Như vậy 404 là do tenant isolation, không phải do
     * fixture thiếu dữ liệu.
     */
    @Test
    void tenant_1_should_not_access_tenant_2_id() throws Exception {
        mockMvc.perform(get("/api/master-data/201")
                        .header(TENANT_HEADER, "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId", is(2)))
                .andExpect(jsonPath("$.id", is(201)));

        mockMvc.perform(get("/api/master-data/201")
                        .header(TENANT_HEADER, "1"))
                .andExpect(status().isNotFound());
    }

    /*
     * Cùng code LAPTOP-01 tồn tại ở cả hai tenant. API phải trả record
     * theo tenant hiện tại, không query theo code toàn cục.
     */
    @Test
    void query_by_code_should_remain_scoped_by_tenant() throws Exception {
        mockMvc.perform(get("/api/master-data/code/LAPTOP-01")
                        .header(TENANT_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId", is(1)))
                .andExpect(jsonPath("$.id", is(101)));

        mockMvc.perform(get("/api/master-data/code/LAPTOP-01")
                        .header(TENANT_HEADER, "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId", is(2)))
                .andExpect(jsonPath("$.id", is(201)));
    }

    /*
     * TenantFilter phải chặn request thiếu tenant trước khi vào controller.
     */
    @Test
    void missing_tenant_header_should_return_400() throws Exception {
        mockMvc.perform(get("/api/master-data"))
                .andExpect(status().isBadRequest());
    }

    /*
     * TenantFilter phải chặn tenant id không parse được thành số hợp lệ.
     */
    @Test
    void invalid_tenant_header_should_return_400() throws Exception {
        mockMvc.perform(get("/api/master-data")
                        .header(TENANT_HEADER, "abc"))
                .andExpect(status().isBadRequest());
    }
}
