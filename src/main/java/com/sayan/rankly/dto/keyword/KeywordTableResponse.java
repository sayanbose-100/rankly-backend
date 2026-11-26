package com.sayan.rankly.dto.keyword;

import java.time.OffsetDateTime;

public record KeywordTableResponse(
        Long id,
        String phrase,
        Integer currentRank,
        Integer previousRank,
        Integer rankDelta,       // Positive = moved up (e.g. from 15 to 8 => +7), Negative = dropped (e.g. 8 to 15 => -7)
        String rankDirection,    // "UP" | "DOWN" | "NEW" | "UNCHANGED" | "UNRANKED"
        Integer bestRank,
        String rankingUrl,
        String checkStatus,      // PENDING | CHECKING | COMPLETED | FAILED
        String lastError,
        OffsetDateTime lastCheckedAt
) {}
