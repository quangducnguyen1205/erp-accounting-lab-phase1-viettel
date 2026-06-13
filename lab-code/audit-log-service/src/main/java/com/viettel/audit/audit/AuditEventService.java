package com.viettel.audit.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viettel.common.security.TenantContext;
import com.viettel.audit.event.MasterDataChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AuditEventService {

    private static final Logger log = LoggerFactory.getLogger(AuditEventService.class);

    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;

    public AuditEventService(AuditEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recordConsumedEvent(MasterDataChangedEvent event) {
        if (repository.existsByEventId(event.eventId())) {
            log.info("Skip duplicate audit event eventId={}", event.eventId());
            return;
        }

        try {
            AuditEvent auditEvent = AuditEvent.from(event, toJson(event));
            repository.save(auditEvent);
            log.info(
                    "Stored audit event eventId={}, tenantId={}, aggregateId={}, changeType={}",
                    event.eventId(),
                    event.tenantId(),
                    event.aggregateId(),
                    event.changeType()
            );
        } catch (DataIntegrityViolationException e) {
            log.info("Skip duplicate audit event after unique constraint eventId={}", event.eventId());
        }
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> listCurrentTenantEvents(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        Long tenantId = currentTenantId();

        return repository.findByTenantIdOrderByConsumedAtDesc(tenantId, PageRequest.of(0, safeLimit))
                .stream()
                .map(AuditEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AuditEventResponse getCurrentTenantEvent(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "eventId cannot be blank");
        }

        return repository.findByTenantIdAndEventId(currentTenantId(), eventId.trim())
                .map(AuditEventResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit event not found"));
    }

    private String toJson(MasterDataChangedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize audit event payload", e);
        }
    }

    private Long currentTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant context is missing");
        }
        return tenantId;
    }
}
