package com.sayan.rankly.dto.batch;

import java.time.OffsetDateTime;

public record SearchBatchStatusResponse(
        Long batchId,
        Long domainId,
        int totalKeywords,
        int completedKeywords,
        int failedKeywords,
        int progressPercent,
        String status,          // IN_PROGRESS | COMPLETED | PARTIAL_FAILED
        boolean isFinished,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt
) {}
