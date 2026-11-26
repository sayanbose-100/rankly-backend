package com.sayan.rankly.dto.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDomainRequest(
        @NotBlank(message = "Domain name is required")
        @Size(max = 255)
        String domainName,

        @Size(max = 120)
        String displayName,

        @Pattern(regexp = "^[a-zA-Z]{2}$", message = "targetCountry must be a 2-letter country code (e.g., us, in, gb)")
        String targetCountry,

        @Pattern(regexp = "^(?i)(desktop|mobile)$", message = "targetDevice must be desktop or mobile")
        String targetDevice
) {}
