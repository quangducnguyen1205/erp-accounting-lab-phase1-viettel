package com.viettel.demo.search;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
 * ==============================================================
 * MasterDataSearchController — endpoint skeleton cho search mini-lab
 * ==============================================================
 *
 * [Mục tiêu]
 * Endpoint này chỉ active khi APP_SEARCH_ENABLED=true.
 *
 * [TODO tự code]
 * - Gọi MasterDataSearchService.search(keyword).
 * - Verify bằng token tenant 1/tenant 2.
 * - Đảm bảo cùng keyword không leak data giữa tenant.
 *
 * ==============================================================
 */
@RestController
@RequestMapping("/api/search/master-data")
@ConditionalOnProperty(prefix = "app.search", name = "enabled", havingValue = "true")
public class MasterDataSearchController {

    private final MasterDataSearchService service;

    public MasterDataSearchController(MasterDataSearchService service) {
        this.service = service;
    }

    @GetMapping
    public List<MasterDataSearchDocument> search(
            @RequestParam("keyword") String keyword
    ) {
        return service.search(keyword);
    }
}

