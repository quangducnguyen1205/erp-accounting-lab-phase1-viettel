package com.viettel.search.search;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class MasterDataSourceClient {

    private static final ParameterizedTypeReference<List<MasterDataSourceRecord>> MASTER_DATA_LIST_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    public MasterDataSourceClient(SearchProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.getMasterDataSourceBaseUrl())
                .build();
    }

    public List<MasterDataSourceRecord> listCurrentTenantMasterData(String accessToken) {
        try {
            List<MasterDataSourceRecord> records = restClient.get()
                    .uri("/api/master-data")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(MASTER_DATA_LIST_TYPE);

            return records == null ? List.of() : records;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Cannot load master data source records for search reindex",
                    exception
            );
        }
    }
}
