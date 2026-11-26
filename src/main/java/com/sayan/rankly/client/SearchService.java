package com.sayan.rankly.client;

import com.sayan.rankly.dto.searchApi.SearchApiResponse;
import com.sayan.rankly.dto.searchApi.SerializedResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Service
public class SearchService {

    private final RestClient restClient;

    public SearchService(RestClient externalApiClient) {
        this.restClient = externalApiClient;
    }

    public List<SerializedResponse> search (String query, String country, String device) {
        SearchApiResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("engine", "google_rank_tracking")
                        .queryParam("q", query)
                        .queryParam("gl", country)
                        .queryParam("device", device)
                        .queryParam("num", 100)
                        .build())
                .retrieve()
                .body(SearchApiResponse.class);

        if(response == null || response.organicResponse() == null) {
            return Collections.emptyList();
        }
        return response.organicResponse().stream()
                .map(item -> new SerializedResponse(item.position(), item.domain()))
                .toList();
    }
}
