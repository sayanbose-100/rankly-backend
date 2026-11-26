package com.sayan.rankly.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePreferencesRequest(
        @Pattern(
                regexp = "^(LIGHT|DARK|SYSTEM)$",
                message = "themePreference must be LIGHT, DARK, or SYSTEM"
        )
        String themePreference,

        Boolean emailAlertsEnabled,

        @Size(max = 120)
        String fullName
) {}
