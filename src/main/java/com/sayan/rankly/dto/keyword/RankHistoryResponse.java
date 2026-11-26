package com.sayan.rankly.dto.keyword;

import java.time.OffsetDateTime;
import java.util.List;

public record RankHistoryResponse(
        Long id,
        Integer rankPosition,
        String rankingUrl,
        List<String> serpFeatures,
        String source,
        OffsetDateTime checkedAt
) {}
