package com.sayan.rankly.dto.serp;

import java.util.List;

public record SerpLookupResult(
        Integer rankPosition, // null if > 100 or not found in organic results
        String rankingUrl,
        List<String> serpFeatures
) {
}
