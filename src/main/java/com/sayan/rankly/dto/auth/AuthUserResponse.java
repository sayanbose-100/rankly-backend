package com.sayan.rankly.dto.auth;

public record AuthUserResponse(
        Long id,
        String email,
        String fullName,
        String themePreference,
        boolean emailAlertsEnabled,
        Integer dailyApiQuota
) {}