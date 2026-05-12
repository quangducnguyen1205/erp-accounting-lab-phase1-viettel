package com.viettel.demo.repository;

/*
 * ==============================================================
 * TenantAwareRepository — ghi chú học tập, chưa dùng trong Phase 1
 * ==============================================================
 *
 * [Mục tiêu]
 * Ban đầu file này dùng để thử ý tưởng generic base repository.
 * Sau khi review, mình tạm de-scope nó vì Phase 1 cần rõ ràng,
 * dễ hiểu và tránh over-design.
 *
 * [Bài học quan trọng]
 * - @MappedSuperclass như TenantAwareEntity KHÔNG phải real @Entity.
 * - JpaRepository phải trỏ tới entity thật, ví dụ MasterData.
 * - Generic base repository muốn dùng đúng thường cần @NoRepositoryBean
 *   và thiết kế custom cẩn thận hơn.
 * - Trong repo này, MasterDataRepository sẽ trực tiếp extends
 *   JpaRepository<MasterData, Long> và khai báo method có tenantId.
 *
 * [Kiến thức cần học sau]
 * - Spring Data JPA: JpaRepository<T, ID>
 * - @NoRepositoryBean
 * - @Query annotation (JPQL)
 * - Custom base repository trong Spring Data
 * - Hibernate @Filter và @FilterDef (nâng cao)
 * - Đọc lại: docs/02-multi-tenant/tinh-huong-va-trade-off.md
 *   (tình huống 2: quên tenant filter)
 *
 * ==============================================================
 */

import com.viettel.demo.entity.TenantAwareEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface TenantAwareRepository<T extends TenantAwareEntity> extends JpaRepository<T, Long> {

    // Hiện tại chưa dùng trong MasterDataRepository.
    // Giữ lại để học pattern generic repository sau, khi thật sự cần.

    List<T> findAllByTenantId(Long tenantId);

    Optional<T> findByTenantIdAndId(Long tenantId, Long id);
}
