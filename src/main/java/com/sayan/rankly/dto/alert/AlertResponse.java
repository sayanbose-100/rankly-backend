package com.sayan.rankly.dto.alert;

import java.time.OffsetDateTime;

public record AlertResponse(
        Long id,
        Long domainId,
        String domainName,
        Long keywordId,
        String keywordPhrase,
        String alertType,      // e.g., RANK_ABOVE_100, DROPPED_OUT, RANK_DROP, RANK_GAIN
        Integer oldRank,
        Integer newRank,       // null if unranked / > 100
        boolean outOfTop100,
        String message,
        boolean isRead,
        OffsetDateTime createdAt
) {}
