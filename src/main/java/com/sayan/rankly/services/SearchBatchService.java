package com.sayan.rankly.services;

import com.sayan.rankly.dto.batch.SearchBatchStatusResponse;
import com.sayan.rankly.entities.SearchBatch;
import com.sayan.rankly.entities.User;
import com.sayan.rankly.repositories.SearchBatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class SearchBatchService {

    private final SearchBatchRepository searchBatchRepository;
    private final UserService userService;

    public SearchBatchService(SearchBatchRepository searchBatchRepository, UserService userService) {
        this.searchBatchRepository = searchBatchRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public SearchBatchStatusResponse getBatchStatus(Authentication authentication, Long batchId) {
        User user = userService.getAuthenticatedUser(authentication);
        SearchBatch batch = searchBatchRepository.findByIdAndUserId(batchId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Search batch not found"));
        return toResponse(batch);
    }

    @Transactional(readOnly = true)
    public List<SearchBatchStatusResponse> getActiveBatchesForDomain(Authentication authentication, Long domainId) {
        User user = userService.getAuthenticatedUser(authentication);
        return searchBatchRepository
                .findByDomainIdAndUserIdAndStatusOrderByCreatedAtDesc(domainId, user.getId(), "IN_PROGRESS")
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Called by the async SearchApi worker after each keyword finishes (succeeded or failed).
     */
    @Transactional
    public void recordKeywordOutcome(Long batchId, boolean succeeded) {
        SearchBatch batch = searchBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Search batch not found"));
        if (succeeded) {
            batch.setCompletedKeywords(batch.getCompletedKeywords() + 1);
        } else {
            batch.setFailedKeywords(batch.getFailedKeywords() + 1);
        }

        int processed = batch.getCompletedKeywords() + batch.getFailedKeywords();
        if (processed >= batch.getTotalKeywords()) {
            batch.setCompletedAt(OffsetDateTime.now());
            batch.setStatus(batch.getFailedKeywords() > 0 ? "PARTIAL_FAILED" : "COMPLETED");
        }

        searchBatchRepository.save(batch);
    }

    private SearchBatchStatusResponse toResponse(SearchBatch batch) {
        int total = batch.getTotalKeywords() != null ? batch.getTotalKeywords() : 0;
        int completed = batch.getCompletedKeywords() != null ? batch.getCompletedKeywords() : 0;
        int failed = batch.getFailedKeywords() != null ? batch.getFailedKeywords() : 0;
        int processed = completed + failed;

        int progressPercent = total > 0
                ? Math.min(100, (int) Math.round((processed * 100.0) / total))
                : 100;

        boolean isFinished = !"IN_PROGRESS".equalsIgnoreCase(batch.getStatus());

        return new SearchBatchStatusResponse(
                batch.getId(),
                batch.getDomain().getId(),
                total,
                completed,
                failed,
                progressPercent,
                batch.getStatus(),
                isFinished,
                batch.getCreatedAt(),
                batch.getCompletedAt()
        );
    }
}