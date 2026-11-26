package com.sayan.rankly.dto.keyword;

import java.util.List;

public record KeywordRefreshRequest(
        List<Long> keywordIds // Optional: if null or empty, refreshes all keywords in the domain
) {}
