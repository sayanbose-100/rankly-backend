package com.sayan.rankly.schedulers;

import com.sayan.rankly.entities.Domain;
import com.sayan.rankly.entities.Keyword;
import com.sayan.rankly.entities.SearchBatch;
import com.sayan.rankly.repositories.DomainRepository;
import com.sayan.rankly.repositories.KeywordRepository;
import com.sayan.rankly.repositories.SearchBatchRepository;
import com.sayan.rankly.services.KeywordRankWorkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DailyRankMonitorScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyRankMonitorScheduler.class);

    private final DomainRepository domainRepository;
    private final KeywordRepository keywordRepository;
    private final SearchBatchRepository searchBatchRepository;
    private final KeywordRankWorkerService keywordRankWorkerService;

    public DailyRankMonitorScheduler(
            DomainRepository domainRepository,
            KeywordRepository keywordRepository,
            SearchBatchRepository searchBatchRepository,
            KeywordRankWorkerService keywordRankWorkerService
    ) {
        this.domainRepository = domainRepository;
        this.keywordRepository = keywordRepository;
        this.searchBatchRepository = searchBatchRepository;
        this.keywordRankWorkerService = keywordRankWorkerService;
    }

    @Scheduled(
            cron = "${rankly.scheduler.cron:0 0 6 * * *}",
            zone = "${rankly.scheduler.zone:Asia/Kolkata}"
    )
    @Transactional
    public void runDailySixAmRankCheck() {
        log.info("Starting scheduled 6:00 AM daily rank check...");

        List<Domain> domains = domainRepository.findAll();
        if (domains.isEmpty()) {
            log.info("No domains found for scheduled 6:00 AM rank check.");
            return;
        }

        for (Domain domain : domains) {
            if (domain.getUser() == null) {
                continue;
            }

            List<Keyword> keywords = keywordRepository.findByDomainIdOrderByCreatedAtDesc(domain.getId());
            if (keywords.isEmpty()) {
                continue;
            }

            for (Keyword kw : keywords) {
                kw.setCheckStatus("PENDING");
                kw.setLastError(null);
                keywordRepository.save(kw);
            }

            SearchBatch batch = new SearchBatch();
            batch.setUser(domain.getUser());
            batch.setDomain(domain);
            batch.setTotalKeywords(keywords.size());
            batch.setCompletedKeywords(0);
            batch.setFailedKeywords(0);
            batch.setStatus("IN_PROGRESS");
            SearchBatch savedBatch = searchBatchRepository.save(batch);

            List<Long> keywordIds = keywords.stream().map(Keyword::getId).toList();
            log.info("Queued scheduled batchId={} for domain='{}' with {} keywords",
                    savedBatch.getId(), domain.getDomainName(), keywordIds.size());

            keywordRankWorkerService.processKeywordsAsync(savedBatch.getId(), keywordIds, "SCHEDULED");
        }
    }
}
