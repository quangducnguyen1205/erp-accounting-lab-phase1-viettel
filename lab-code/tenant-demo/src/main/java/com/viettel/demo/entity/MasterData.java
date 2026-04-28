package com.viettel.demo.entity;

/*
 * ==============================================================
 * TODO TASK: MasterData Entity — entity nghiệp vụ đầu tiên
 * ==============================================================
 *
 * [Mục tiêu]
 * Tạo entity cho bảng master_data (danh mục vật tư, tài khoản kế toán,
 * hoặc bất kỳ danh mục nào dùng chung trong ERP).
 * Entity này kế thừa TenantAwareEntity → tự có tenant_id.
 *
 * [Nhiệm vụ của tôi]
 * 1. Kế thừa TenantAwareEntity.
 * 2. Đánh dấu là JPA @Entity, mapping với bảng master_data.
 * 3. Khai báo các field: id (PK), code, name, category,
 *    isActive, createdAt.
 * 4. Tạo unique constraint tenant-aware: (tenant_id, code).
 *    Suy nghĩ: annotation nào của JPA dùng cho unique trên nhiều cột?
 *
 * [Kiến thức cần tự research]
 * - @Entity, @Table (JPA)
 * - @Id, @GeneratedValue (strategy)
 * - @Column
 * - @Table(uniqueConstraints = ...) — UniqueConstraint trên nhiều cột
 * - Đọc lại: docs/03-backend-database-mo-rong/postgres-va-bai-toan-multi-tenant.md
 *   (phần "Unique constraint tenant-aware")
 *
 * ==============================================================
 */

public class MasterData {

    // TODO: Kế thừa TenantAwareEntity

    // TODO: Khai báo fields

    // TODO: Unique constraint (tenant_id, code)

}
