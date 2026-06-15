package com.viettel.search.search;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search/master-data")
public class MasterDataSearchController {

    private final MasterDataSearchService service;

    public MasterDataSearchController(MasterDataSearchService service) {
        this.service = service;
    }

    @GetMapping
    public List<MasterDataSearchDocument> search(@RequestParam("keyword") String keyword) {
        return service.search(keyword);
    }

    @PostMapping("/reindex")
    public MasterDataReindexResponse reindex(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        return service.reindexCurrentTenant(jwt.getTokenValue());
    }
}
