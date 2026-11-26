package com.sayan.rankly.dto.searchApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchApiResponse(
        @JsonProperty("organic_results") List<OrganicItem> organicResponse
) {}
