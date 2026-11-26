package com.sayan.rankly.controllers;

import com.sayan.rankly.dto.batch.SearchBatchStatusResponse;
import com.sayan.rankly.services.SearchBatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/batches")
public class SearchBatchController {

    private final SearchBatchService searchBatchService;

    public SearchBatchController(SearchBatchService searchBatchService) {
        this.searchBatchService = searchBatchService;
    }

    @GetMapping("/{batchId}")
    public ResponseEntity<SearchBatchStatusResponse> getBatchStatus(
            Authentication authentication,
            @PathVariable Long batchId
    ) {
        return ResponseEntity.ok(searchBatchService.getBatchStatus(authentication, batchId));
    }

    @GetMapping("/domain/{domainId}/active")
    public ResponseEntity<List<SearchBatchStatusResponse>> getActiveBatchesForDomain(
            Authentication authentication,
            @PathVariable Long domainId
    ) {
        return ResponseEntity.ok(searchBatchService.getActiveBatchesForDomain(authentication, domainId));
    }
}
