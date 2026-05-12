package com.viettel.demo.repository;

/*
 * ==============================================================
 * MasterData Repository
 * ==============================================================
 *
 * [Mục tiêu]
 * Repository cụ thể cho entity MasterData.
 * Giai đoạn học này dùng trực tiếp JpaRepository và khai báo
 * các method tenant-aware rõ ràng, tránh generic base repository
 * khi chưa thật sự cần.
 *
 * [Cách hoạt động hiện tại]
 * 1. Repository trỏ trực tiếp tới real entity MasterData.
 * 2. Query nghiệp vụ luôn nhận tenantId rõ ràng.
 * 3. Không khai báo findByCode(...) hoặc findByCategory(...)
 *    vì các method đó thiếu tenant scope và dễ gây data leakage.
 *
 * [Kiến thức đã áp dụng]
 * - Spring Data JPA query derivation (method name → query)
 * - JpaRepository<MasterData, Long>
 * - Optional cho lookup có thể không tìm thấy dữ liệu
 * - Derived query: findByTenantIdAndCategory(...)
 *
 * ==============================================================
 */

import com.viettel.demo.entity.MasterData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MasterDataRepository extends JpaRepository<MasterData, Long> {

    List<MasterData> findByTenantIdAndIsActiveTrue(Long tenantId);

    List<MasterData> findByTenantIdAndCategory(Long tenantId, String category);

    Optional<MasterData> findByTenantIdAndCode(Long tenantId, String code);

    Optional<MasterData> findByTenantIdAndId(Long tenantId, Long id);
}
