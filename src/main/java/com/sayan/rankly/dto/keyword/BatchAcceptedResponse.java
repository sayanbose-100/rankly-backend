package com.sayan.rankly.dto.keyword;

public record BatchAcceptedResponse(
        Long batchId,
        int totalKeywords,
        String status
) {}
