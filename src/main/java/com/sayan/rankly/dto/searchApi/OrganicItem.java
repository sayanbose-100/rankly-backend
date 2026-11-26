package com.sayan.rankly.dto.searchApi;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrganicItem(
        Integer position,
        String domain
) {}
