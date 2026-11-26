package com.sayan.rankly.services;

import com.sayan.rankly.dto.keyword.*;
import com.sayan.rankly.entities.Domain;
import com.sayan.rankly.entities.Keyword;
import com.sayan.rankly.entities.SearchBatch;
import com.sayan.rankly.entities.User;
import com.sayan.rankly.repositories.KeywordRepository;
import com.sayan.rankly.repositories.RankHistoryRepository;
import com.sayan.rankly.repositories.SearchBatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class KeywordService {

    private static final int MAX_BATCH_SIZE = 50;

    private final KeywordRepository keywordRepository;
    private final SearchBatchRepository searchBatchRepository;
    private final RankHistoryRepository rankHistoryRepository;
    private final UserService userService;
    private final DomainService domainService;
    private final KeywordRankWorkerService keywordRankWorkerService;

    public KeywordService(
            KeywordRepository keywordRepository,
            SearchBatchRepository searchBatchRepository,
            RankHistoryRepository rankHistoryRepository,
            UserService userService,
            DomainService domainService,
            KeywordRankWorkerService keywordRankWorkerService
    ) {
        this.keywordRepository = keywordRepository;
        this.searchBatchRepository = searchBatchRepository;
        this.rankHistoryRepository = rankHistoryRepository;
        this.userService = userService;
        this.domainService = domainService;
        this.keywordRankWorkerService = keywordRankWorkerService;
    }

    /**
     * 1. Handles both brand-new searches AND re-searching existing phrases (e.g. "Spring Boot Dev, Java").
     */
    @Transactional
    public BatchAcceptedResponse startKeywordSearch(
            Authentication authentication,
            Long domainId,
            KeywordSearchRequest request
    ) {
        User user = userService.getAuthenticatedUser(authentication);
        Domain domain = domainService.getOwnedDomain(authentication, domainId);

        List<String> normalizedPhrases = normalizeAndSplitPhrases(request.keywords());
        if (normalizedPhrases.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No valid keywords provided");
        }
        if (normalizedPhrases.size() > MAX_BATCH_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Maximum " + MAX_BATCH_SIZE + " keywords allowed per batch"
            );
        }

        // Upsert each keyword: reuse existing row if found, otherwise insert fresh row
        List<Keyword> queuedKeywords = new ArrayList<>();
        for (String phrase : normalizedPhrases) {
            Keyword keyword = keywordRepository.findByDomainIdAndPhrase(domain.getId(), phrase)
                    .orElseGet(() -> {
                        Keyword created = new Keyword();
                        created.setDomain(domain);
                        created.setPhrase(phrase);
                        created.setCurrentRank(null);
                        created.setPreviousRank(null);
                        created.setBestRank(null);
                        return created;
                    });

            keyword.setCheckStatus("PENDING");
            keyword.setLastError(null);
            queuedKeywords.add(keywordRepository.save(keyword));
        }

        return createAndDispatchBatch(user, domain, queuedKeywords, "MANUAL");
    }

    /**
     * 2. Refreshes selected keywords (or all domain keywords if keywordIds is empty).
     */
    @Transactional
    public BatchAcceptedResponse refreshKeywords(
            Authentication authentication,
            Long domainId,
            KeywordRefreshRequest request
    ) {
        User user = userService.getAuthenticatedUser(authentication);
        Domain domain = domainService.getOwnedDomain(authentication, domainId);

        List<Keyword> targets = (request != null && request.keywordIds() != null && !request.keywordIds().isEmpty())
                ? keywordRepository.findAllByIdInAndDomainId(request.keywordIds(), domain.getId())
                : keywordRepository.findByDomainIdOrderByCreatedAtDesc(domain.getId());

        if (targets.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No keywords available to refresh");
        }
        if (targets.size() > MAX_BATCH_SIZE) {
            targets = targets.subList(0, MAX_BATCH_SIZE);
        }

        for (Keyword keyword : targets) {
            keyword.setCheckStatus("PENDING");
            keyword.setLastError(null);
        }
        List<Keyword> savedTargets = keywordRepository.saveAll(targets);

        return createAndDispatchBatch(user, domain, savedTargets, "MANUAL");
    }

    private BatchAcceptedResponse createAndDispatchBatch(
            User user,
            Domain domain,
            List<Keyword> keywords,
            String source
    ) {
        SearchBatch batch = new SearchBatch();
        batch.setUser(user);
        batch.setDomain(domain);
        batch.setTotalKeywords(keywords.size());
        batch.setCompletedKeywords(0);
        batch.setFailedKeywords(0);
        batch.setStatus("IN_PROGRESS");

        SearchBatch savedBatch = searchBatchRepository.save(batch);
        Long batchId = savedBatch.getId();
        List<Long> keywordIds = keywords.stream().map(Keyword::getId).toList();

        // Dispatch ONLY AFTER DB commit so worker never hits "Search batch not found"
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                keywordRankWorkerService.processKeywordsAsync(batchId, keywordIds, source);
            }
        });

        return new BatchAcceptedResponse(batchId, keywords.size(), savedBatch.getStatus());
    }

    @Transactional(readOnly = true)
    public List<KeywordTableResponse> getKeywordsForDomain(Authentication authentication, Long domainId) {
        Domain domain = domainService.getOwnedDomain(authentication, domainId);
        return keywordRepository.findByDomainIdOrderByCreatedAtDesc(domain.getId())
                .stream()
                .map(this::toTableResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RankHistoryResponse> getKeywordHistory(Authentication authentication, Long keywordId) {
        Keyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Keyword not found"));
        domainService.getOwnedDomain(authentication, keyword.getDomain().getId());

        return rankHistoryRepository.findByKeywordIdOrderByCheckedAtAsc(keywordId)
                .stream()
                .map(h -> new RankHistoryResponse(
                        h.getId(),
                        h.getRankPosition(),
                        h.getRankingUrl(),
                        h.getSerpFeatures(),
                        h.getSource(),
                        h.getCheckedAt()
                ))
                .toList();
    }

    @Transactional
    public void deleteKeyword(Authentication authentication, Long keywordId) {
        Keyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Keyword not found"));
        domainService.getOwnedDomain(authentication, keyword.getDomain().getId());
        keywordRepository.delete(keyword);
    }

    /**
     * Splits comma/newline strings, trims, collapses whitespace, lowercases, and deduplicates in order.
     */
    private List<String> normalizeAndSplitPhrases(List<String> rawInputs) {
        Set<String> unique = new LinkedHashSet<>();
        if (rawInputs == null) {
            return List.of();
        }
        for (String raw : rawInputs) {
            if (raw == null) continue;
            for (String part : raw.split("[,\\n]+")) {
                String cleaned = part.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
                if (!cleaned.isBlank()) {
                    unique.add(cleaned);
                }
            }
        }
        return new ArrayList<>(unique);
    }

    private KeywordTableResponse toTableResponse(Keyword k) {
        Integer curr = k.getCurrentRank();
        Integer prev = k.getPreviousRank();
        Integer delta = null;
        String direction;

        if (curr == null) {
            direction = "UNRANKED";
        } else if (prev == null) {
            direction = "NEW";
        } else {
            // Lower rank number is better: prev=15, curr=8 => +7 (UP)
            delta = prev - curr;
            if (delta > 0) direction = "UP";
            else if (delta < 0) direction = "DOWN";
            else direction = "UNCHANGED";
        }

        return new KeywordTableResponse(
                k.getId(),
                k.getPhrase(),
                curr,
                prev,
                delta,
                direction,
                k.getBestRank(),
                k.getRankingUrl(),
                k.getCheckStatus(),
                k.getLastError(),
                k.getLastCheckedAt()
        );
    }
}