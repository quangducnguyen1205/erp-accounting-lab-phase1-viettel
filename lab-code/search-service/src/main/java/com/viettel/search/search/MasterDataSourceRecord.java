package com.viettel.search.search;

import com.fasterxml.jackson.annotation.JsonAlias;

public record MasterDataSourceRecord(
        Long id,
        String code,
        String name,
        String category,
        @JsonAlias("active")
        Boolean isActive
) {
}
