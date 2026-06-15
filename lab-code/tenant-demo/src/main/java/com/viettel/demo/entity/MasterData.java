package com.viettel.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/*
 * ==============================================================
 * MasterData Entity — entity nghiệp vụ đầu tiên
 * ==============================================================
 *
 * [Mục tiêu]
 * Tạo entity cho bảng master_data (danh mục vật tư, tài khoản kế toán,
 * hoặc bất kỳ danh mục nào dùng chung trong ERP).
 * Entity này kế thừa TenantAwareEntity → tự có tenant_id.
 *
 * [Cách mapping hiện tại]
 * 1. Kế thừa TenantAwareEntity.
 * 2. Đánh dấu là JPA @Entity, mapping với bảng master_data.
 * 3. Khai báo các field: id (PK), code, name, category,
 *    isActive, createdAt.
 * 4. Tính duy nhất của code được quản lý bằng Flyway partial unique index:
 *    active records unique theo (tenant_id, code), còn record đã tạm ngưng
 *    không chặn tạo lại code mới.
 *
 * [Kiến thức cần tự research]
 * - @Entity, @Table (JPA)
 * - @Id, @GeneratedValue (strategy)
 * - @Column
 * - Partial unique index trong PostgreSQL
 * - Đọc lại: docs/03-backend-database-mo-rong/postgres-va-bai-toan-multi-tenant.md
 *   (phần "Unique constraint tenant-aware")
 *
 * ==============================================================
 */
@Entity
@Table(name = "master_data")
public class MasterData extends TenantAwareEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /*
     * Redis mini-lab:
     * Tạo object chỉ để serialize response khi cache hit. Object này không được
     * quản lý bởi JPA EntityManager và không dùng để save/update DB.
     */
    public static MasterData detachedReadCopy(
            Long id,
            Long tenantId,
            String code,
            String name,
            String category,
            Boolean isActive,
            LocalDateTime createdAt
    ) {
        MasterData data = new MasterData();
        data.id = id;
        data.setTenantId(tenantId);
        data.code = code;
        data.name = name;
        data.category = category;
        data.isActive = isActive;
        data.createdAt = createdAt;
        return data;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
