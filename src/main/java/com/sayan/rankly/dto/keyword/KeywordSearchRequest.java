package com.sayan.rankly.dto.keyword;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record KeywordSearchRequest(
        @NotEmpty(message = "At least one keyword is required")
        List<String> keywords
) {}
