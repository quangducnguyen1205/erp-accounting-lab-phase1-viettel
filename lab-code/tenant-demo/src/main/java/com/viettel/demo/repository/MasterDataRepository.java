package com.viettel.demo.repository;

/*
 * ==============================================================
 * TODO TASK: MasterData Repository
 * ==============================================================
 *
 * [Mục tiêu]
 * Repository cụ thể cho entity MasterData.
 * Kế thừa từ TenantAwareRepository (hoặc JpaRepository)
 * để đảm bảo mọi query đều tenant-aware.
 *
 * [Nhiệm vụ của tôi]
 * 1. Kế thừa đúng base repository đã tạo ở bước trước.
 * 2. Thêm method tìm theo category (trong phạm vi tenant).
 * 3. Thêm method tìm theo code (trong phạm vi tenant).
 *    Suy nghĩ: method signature nên là gì?
 *    findByCode(String code) hay findByTenantIdAndCode(Long tenantId, String code)?
 *
 * [Kiến thức cần tự research]
 * - Spring Data JPA query derivation (method name → query)
 * - @Query annotation với JPQL
 * - Derived query: findByTenantIdAndCategory(Long, String)
 *
 * ==============================================================
 */

public interface MasterDataRepository {

    // TODO: Kế thừa base repository

    // TODO: findByCategory (tenant-aware)

    // TODO: findByCode (tenant-aware)

}
