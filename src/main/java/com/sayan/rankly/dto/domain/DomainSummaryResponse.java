package com.sayan.rankly.dto.domain;

import java.time.OffsetDateTime;

public record DomainSummaryResponse(
        Long id,
        String domainName,
        String displayName,
        String targetCountry,
        String targetDevice,
        boolean isActive,
        int keywordCount,
        int top10Count,
        int top100Count,
        OffsetDateTime lastCheckedAt,
        OffsetDateTime createdAt
) {}
