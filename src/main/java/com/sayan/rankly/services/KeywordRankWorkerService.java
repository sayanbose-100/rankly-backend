package com.sayan.rankly.services;

import com.sayan.rankly.client.SearchApiClient;
import com.sayan.rankly.client.SearchApiClient.SerpMatchResult;
import com.sayan.rankly.entities.Domain;
import com.sayan.rankly.entities.Keyword;
import com.sayan.rankly.entities.RankHistory;
import com.sayan.rankly.repositories.DomainRepository;
import com.sayan.rankly.repositories.KeywordRepository;
import com.sayan.rankly.repositories.RankHistoryRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class KeywordRankWorkerService {

    private final KeywordRepository keywordRepository;
    private final RankHistoryRepository rankHistoryRepository;
    private final SearchApiClient searchApiClient;
    private final SearchBatchService searchBatchService;
    private final DomainRepository domainRepository;

    public KeywordRankWorkerService(
            KeywordRepository keywordRepository,
            RankHistoryRepository rankHistoryRepository,
            SearchApiClient searchApiClient,
            SearchBatchService searchBatchService,
            DomainRepository domainRepository
    ) {
        this.keywordRepository = keywordRepository;
        this.rankHistoryRepository = rankHistoryRepository;
        this.searchApiClient = searchApiClient;
        this.searchBatchService = searchBatchService;
        this.domainRepository = domainRepository;
    }

    @Async
    public void processKeywordsAsync(Long batchId, List<Long> keywordIds, String source) {
        for (Long keywordId : keywordIds) {
            processSingleKeyword(batchId, keywordId, source);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleKeyword(Long batchId, Long keywordId, String source) {
        Keyword keyword = keywordRepository.findById(keywordId).orElse(null);
        if (keyword == null) {
            searchBatchService.recordKeywordOutcome(batchId, false);
            return;
        }

        try {
            keyword.setCheckStatus("CHECKING");
            keywordRepository.save(keyword);

            Long domainId = keyword.getDomain().getId();
            Domain domain = domainRepository.findById(domainId).orElse(null);
            assert domain != null;

            SerpMatchResult result = searchApiClient.findDomainRank(
                    domain.getDomainName(),
                    domain.getTargetCountry(),
                    domain.getTargetDevice(),
                    keyword.getPhrase()
            );

            // Shift currentRank -> previousRank ONLY on successful fetch
            Integer oldCurrentRank = keyword.getCurrentRank();
            Integer newRank = result.rankPosition();

            keyword.setPreviousRank(oldCurrentRank);
            keyword.setCurrentRank(newRank);
            keyword.setRankingUrl(result.rankingUrl());

            if (newRank != null && (keyword.getBestRank() == null || newRank < keyword.getBestRank())) {
                keyword.setBestRank(newRank);
            }

            OffsetDateTime now = OffsetDateTime.now();
            keyword.setCheckStatus("COMPLETED");
            keyword.setLastError(null);
            keyword.setLastCheckedAt(now);
            keywordRepository.save(keyword);

            // Persist time-series point
            RankHistory history = new RankHistory();
            history.setKeyword(keyword);
            history.setRankPosition(newRank);
            history.setRankingUrl(result.rankingUrl());
            history.setSource(source);
            history.setCheckedAt(now);
            rankHistoryRepository.save(history);

            searchBatchService.recordKeywordOutcome(batchId, true);

        } catch (Exception ex) {
            System.out.println("Exception: " + ex);
            // Do NOT overwrite previousRank or currentRank on API/network failure
            keyword.setCheckStatus("FAILED");
            keyword.setLastError(ex.getMessage() != null ? ex.getMessage() : "Rank check failed");
            keywordRepository.save(keyword);

            searchBatchService.recordKeywordOutcome(batchId, false);
        }
    }
}