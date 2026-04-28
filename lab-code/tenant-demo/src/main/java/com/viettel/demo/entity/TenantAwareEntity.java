package com.viettel.demo.entity;

/*
 * ==============================================================
 * TODO TASK: Base Entity — mọi entity nghiệp vụ kế thừa từ đây
 * ==============================================================
 *
 * [Mục tiêu]
 * Tạo base class mà TẤT CẢ entity nghiệp vụ (MasterData,
 * Invoice, Customer, ...) sẽ kế thừa. Base class này đảm bảo
 * mọi entity đều có cột tenant_id.
 *
 * [Nhiệm vụ của tôi]
 * 1. Đánh dấu class là JPA entity (mapped superclass).
 * 2. Khai báo field tenantId (Long, NOT NULL).
 * 3. Tự động set tenantId từ TenantContext TRƯỚC KHI insert.
 *    Suy nghĩ: nếu developer quên set tenantId, dữ liệu sẽ
 *    thuộc tenant nào? → Cần auto-set.
 *
 * [Kiến thức cần tự research]
 * - @MappedSuperclass (JPA)
 * - @Column(nullable = false)
 * - @PrePersist callback (JPA lifecycle)
 * - Cách gọi TenantContext.getCurrentTenant() trong @PrePersist
 * - Suy nghĩ: có nên cho phép update tenantId không? Tại sao?
 *
 * ==============================================================
 */

public class TenantAwareEntity {

    // TODO: Khai báo field tenantId

    // TODO: @PrePersist method để auto-set tenantId

}
