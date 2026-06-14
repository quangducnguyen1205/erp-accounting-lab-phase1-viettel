package com.viettel.search.search;

import com.viettel.search.event.MasterDataChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MasterDataSearchProjectionService {

    private static final Logger log = LoggerFactory.getLogger(MasterDataSearchProjectionService.class);

    private final MasterDataSearchGateway gateway;

    public MasterDataSearchProjectionService(MasterDataSearchGateway gateway) {
        this.gateway = gateway;
    }

    public void apply(MasterDataChangedEvent event) {
        if (event == null || event.aggregateId() == null || event.tenantId() == null) {
            log.warn("Skip invalid search projection event eventId={}", event == null ? null : event.eventId());
            return;
        }

        MasterDataSearchDocument document = MasterDataSearchDocument.fromEvent(event);
        gateway.indexOne(document);
        log.info(
                "Updated search projection eventId={}, tenantId={}, aggregateId={}, code={}, active={}, changeType={}",
                event.eventId(),
                event.tenantId(),
                event.aggregateId(),
                event.code(),
                document.active(),
                event.changeType()
        );
    }
}
