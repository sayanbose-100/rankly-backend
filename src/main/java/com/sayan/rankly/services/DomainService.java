package com.sayan.rankly.services;

import com.sayan.rankly.dto.domain.CreateDomainRequest;
import com.sayan.rankly.dto.domain.DomainSummaryResponse;
import com.sayan.rankly.entities.Domain;
import com.sayan.rankly.entities.Keyword;
import com.sayan.rankly.entities.User;
import com.sayan.rankly.repositories.DomainRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class DomainService {

    private final DomainRepository domainRepository;
    private final UserService userService;

    public DomainService(DomainRepository domainRepository, UserService userService) {
        this.domainRepository = domainRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<DomainSummaryResponse> getUserDomains(Authentication authentication) {
        User user = userService.getAuthenticatedUser(authentication);
        return domainRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional
    public DomainSummaryResponse addDomain(Authentication authentication, CreateDomainRequest request) {
        User user = userService.getAuthenticatedUser(authentication);

        String normalizedDomain = normalizeDomain(request.domainName());
        String country = (request.targetCountry() == null || request.targetCountry().isBlank())
                ? "us"
                : request.targetCountry().trim().toLowerCase();
        String device = (request.targetDevice() == null || request.targetDevice().isBlank())
                ? "desktop"
                : request.targetDevice().trim().toLowerCase();

        boolean alreadyExists = domainRepository.existsByUserIdAndDomainNameAndTargetCountryAndTargetDevice(
                user.getId(), normalizedDomain, country, device
        );
        if (alreadyExists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Domain is already tracked with the same country and device"
            );
        }

        Domain domain = new Domain();
        domain.setUser(user);
        domain.setDomainName(normalizedDomain);
        domain.setDisplayName(
                (request.displayName() != null && !request.displayName().isBlank())
                        ? request.displayName().trim()
                        : normalizedDomain
        );
        domain.setTargetCountry(country);
        domain.setTargetDevice(device);
        domain.setActive(true);

        Domain saved = domainRepository.save(domain);
        return toSummaryResponse(saved);
    }

    @Transactional(readOnly = true)
    public DomainSummaryResponse getDomainDetails(Authentication authentication, Long domainId) {
        Domain domain = getOwnedDomain(authentication, domainId);
        return toSummaryResponse(domain);
    }

    @Transactional
    public void deleteDomain(Authentication authentication, Long domainId) {
        Domain domain = getOwnedDomain(authentication, domainId);
        // CascadeType.ALL + orphanRemoval = true on Domain deletes associated keywords & rank history
        domainRepository.delete(domain);
    }

    /**
     * Reusable ownership check for DomainController, KeywordController, and AlertController.
     */
    @Transactional(readOnly = true)
    public Domain getOwnedDomain(Authentication authentication, Long domainId) {
        User user = userService.getAuthenticatedUser(authentication);
        return domainRepository.findByIdAndUserId(domainId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));
    }

    private DomainSummaryResponse toSummaryResponse(Domain domain) {
        List<Keyword> keywords = domain.getKeywords();

        int keywordCount = keywords.size();
        int top10Count = (int) keywords.stream()
                .map(Keyword::getCurrentRank)
                .filter(rank -> rank != null && rank >= 1 && rank <= 10)
                .count();
        int top100Count = (int) keywords.stream()
                .map(Keyword::getCurrentRank)
                .filter(rank -> rank != null && rank >= 1 && rank <= 100)
                .count();
        OffsetDateTime lastCheckedAt = keywords.stream()
                .map(Keyword::getLastCheckedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new DomainSummaryResponse(
                domain.getId(),
                domain.getDomainName(),
                domain.getDisplayName(),
                domain.getTargetCountry(),
                domain.getTargetDevice(),
                domain.isActive(),
                keywordCount,
                top10Count,
                top100Count,
                lastCheckedAt,
                domain.getCreatedAt()
        );
    }

    private String normalizeDomain(String rawDomain) {
        String cleaned = rawDomain.trim().toLowerCase()
                .replaceFirst("^https?://", "")
                .replaceFirst("^www\\.", "");
        int slashIdx = cleaned.indexOf('/');
        if (slashIdx != -1) {
            cleaned = cleaned.substring(0, slashIdx);
        }
        return cleaned;
    }
}
