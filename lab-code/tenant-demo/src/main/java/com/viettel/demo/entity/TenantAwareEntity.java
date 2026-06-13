package com.viettel.demo.entity;

import com.viettel.common.security.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

/*
 * ==============================================================
 * Base Entity — mọi entity nghiệp vụ có tenant_id kế thừa từ đây
 * ==============================================================
 *
 * [Mục tiêu]
 * Tạo base class mà TẤT CẢ entity nghiệp vụ (MasterData,
 * Invoice, Customer, ...) sẽ kế thừa. Base class này đảm bảo
 * mọi entity đều có cột tenant_id.
 *
 * [Nhiệm vụ của tôi]
 * 1. Đánh dấu class là mapped superclass, không phải table riêng.
 * 2. Khai báo field tenantId (Long, NOT NULL).
 * 3. Tự động set tenantId từ TenantContext TRƯỚC KHI insert.
 * 4. Không đặt @Id ở base class này vì mỗi entity nghiệp vụ có thể
 *    có chiến lược định danh riêng.
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
@MappedSuperclass
public abstract class TenantAwareEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    public Long getTenantId() {
        return tenantId;
    }

    protected void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    @PrePersist
    protected void autoSetTenantId() {
        Long currentTenantId = TenantContext.getCurrentTenant();
        if (currentTenantId == null) {
            throw new IllegalStateException("Tenant ID is not set");
        }
        this.tenantId = currentTenantId;
    }
}
