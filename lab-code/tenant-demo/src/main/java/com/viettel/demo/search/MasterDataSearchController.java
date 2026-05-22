package com.viettel.demo.search;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
 * ==============================================================
 * MasterDataSearchController — endpoint cho search mini-lab
 * ==============================================================
 *
 * [Mục tiêu]
 * Endpoint này chỉ active khi APP_SEARCH_ENABLED=true.
 *
 * [Cách hoạt động hiện tại]
 * - GET /api/search/master-data?keyword=... gọi service search tenant-aware.
 * - POST /api/search/master-data/reindex tạo lại search index từ PostgreSQL.
 *
 * [Cảnh báo]
 * Reindex endpoint chỉ dùng local mini-lab/admin flow, không coi là
 * production public API.
 *
 * ==============================================================
 */
@RestController
@RequestMapping("/api/search/master-data")
@ConditionalOnProperty(prefix = "app.search", name = "enabled", havingValue = "true")
public class MasterDataSearchController {

    private final MasterDataSearchService service;
    private final MasterDataSearchIndexer indexer;

    public MasterDataSearchController(MasterDataSearchService service, MasterDataSearchIndexer indexer) {
        this.service = service;
        this.indexer = indexer;
    }

    @GetMapping
    public List<MasterDataSearchDocument> search(
            @RequestParam("keyword") String keyword
    ) {
        return service.search(keyword);
    }

    @PostMapping("/reindex")
    public MasterDataSearchReindexResponse reindex() {
        return indexer.reindexAll();
    }
}
