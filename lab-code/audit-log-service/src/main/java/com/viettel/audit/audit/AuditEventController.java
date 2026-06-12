package com.viettel.audit.audit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-events")
public class AuditEventController {

    private final AuditEventService service;

    public AuditEventController(AuditEventService service) {
        this.service = service;
    }

    @GetMapping
    public List<AuditEventResponse> list(
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        return service.listCurrentTenantEvents(limit);
    }

    @GetMapping("/{eventId}")
    public AuditEventResponse getByEventId(@PathVariable String eventId) {
        return service.getCurrentTenantEvent(eventId);
    }
}
