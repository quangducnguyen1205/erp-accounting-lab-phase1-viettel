package com.viettel.audit.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    boolean existsByEventId(String eventId);

    List<AuditEvent> findByTenantIdOrderByConsumedAtDesc(Long tenantId, Pageable pageable);

    Optional<AuditEvent> findByTenantIdAndEventId(Long tenantId, String eventId);
}
