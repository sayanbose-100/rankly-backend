package com.sayan.rankly.dto.user;

public record UserQuotaResponse(
        int dailyLimit,
        int usedToday,
        int remainingToday
) {}
