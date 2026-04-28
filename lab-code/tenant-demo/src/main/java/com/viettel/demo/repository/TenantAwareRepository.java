package com.viettel.demo.repository;

/*
 * ==============================================================
 * TODO TASK: Base Repository — tự động filter theo tenant_id
 * ==============================================================
 *
 * [Mục tiêu]
 * Đây là TẦNG PHÒNG THỦ CHÍNH chống data leakage.
 * Mọi repository nghiệp vụ kế thừa class/interface này.
 * Khi gọi findAll(), findById(), ... → tự động thêm
 * WHERE tenant_id = <tenant hiện tại>.
 *
 * [Nhiệm vụ của tôi]
 * 1. Quyết định: dùng abstract class hay interface?
 *    - Nếu dùng Spring Data JPA: có thể dùng custom base repository.
 *    - Nếu dùng JPQL/EntityManager: có thể dùng abstract class.
 * 2. Override hoặc cung cấp method findAll() có tenant filter.
 * 3. Override hoặc cung cấp method findById() có tenant filter.
 *    Suy nghĩ: findById(id) mà KHÔNG check tenant_id → lỗ hổng gì?
 * 4. Tenant_id lấy từ đâu? → TenantContext.getCurrentTenant().
 *
 * [Kiến thức cần tự research]
 * - Spring Data JPA: JpaRepository<T, ID>
 * - @Query annotation (JPQL)
 * - Cách viết custom base repository trong Spring Data
 *   (search: "spring data jpa custom repository base class")
 * - Hoặc đơn giản hơn: dùng @Query("... WHERE e.tenantId = ?1")
 *   trên từng method
 * - Hibernate @Filter và @FilterDef (nâng cao — tùy chọn)
 * - Đọc lại: docs/02-multi-tenant/tinh-huong-va-trade-off.md
 *   (tình huống 2: quên tenant filter)
 *
 * ==============================================================
 */

public interface TenantAwareRepository {

    // TODO: Quyết định thiết kế (interface hay abstract class?)

    // TODO: Method findAll có tenant filter

    // TODO: Method findById có tenant filter

}
