package com.sayan.rankly.client;

import com.sayan.rankly.dto.searchApi.SerializedResponse;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Locale;

@Component
public class SearchApiClient {

    public record SerpMatchResult(Integer rankPosition, String rankingUrl) {}

    private final SearchService searchService;

    public SearchApiClient(
            SearchService searchService
    ) {
        this.searchService = searchService;
    }

    public SerpMatchResult findDomainRank(
            String domainName,
            String targetCountry,
            String targetDevice,
            String phrase
    ) {
        String country = (targetCountry == null || targetCountry.isBlank()) ? "us" : targetCountry.toLowerCase(Locale.ROOT);
        String device = (targetDevice == null || targetDevice.isBlank()) ? "desktop" : targetDevice.toLowerCase(Locale.ROOT);

        List<SerializedResponse> response = searchService.search(
                phrase,
                country,
                device
        );

        String normalizedTarget = normalizeHost(domainName);

        for (int i = 0; i < response.size(); i++) {
            SerializedResponse item = response.get(i);
            if(matchesDomain(item.domain(), normalizedTarget)) {
                int position = item.position();
                return new SerpMatchResult(position, "");
            }
        }

        // Not found in top 100
        return new SerpMatchResult(null, null);
    }

    private String extractHost(String url) {
        try {
            String host = URI.create(url.trim()).getHost();
            return host != null ? normalizeHost(host) : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String normalizeHost(String host) {
        String cleaned = host.trim().toLowerCase(Locale.ROOT);
        cleaned = cleaned.replaceFirst("^https?://", "");
        cleaned = cleaned.replaceFirst("/.*$", "");
        return cleaned.startsWith("www.") ? cleaned.substring(4) : cleaned;
    }

    private boolean matchesDomain(String resultHost, String targetDomain) {
        if (resultHost == null || resultHost.isBlank() || targetDomain.isBlank()) return false;
        return resultHost.equals(targetDomain) || resultHost.endsWith("." + targetDomain);
    }
}