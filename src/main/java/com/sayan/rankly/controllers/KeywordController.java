package com.sayan.rankly.controllers;

import com.sayan.rankly.dto.keyword.*;
import com.sayan.rankly.services.KeywordService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class KeywordController {

    private final KeywordService keywordService;

    public KeywordController(KeywordService keywordService) {
        this.keywordService = keywordService;
    }

    @GetMapping("/api/v1/domains/{domainId}/keywords")
    public ResponseEntity<List<KeywordTableResponse>> listKeywords(
            Authentication authentication,
            @PathVariable Long domainId
    ) {
        return ResponseEntity.ok(keywordService.getKeywordsForDomain(authentication, domainId));
    }

    @PostMapping("/api/v1/domains/{domainId}/keywords/search")
    public ResponseEntity<BatchAcceptedResponse> searchKeywords(
            Authentication authentication,
            @PathVariable Long domainId,
            @Valid @RequestBody KeywordSearchRequest request
    ) {
        BatchAcceptedResponse accepted = keywordService.startKeywordSearch(authentication, domainId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(accepted);
    }

    @PostMapping("/api/v1/domains/{domainId}/keywords/refresh")
    public ResponseEntity<BatchAcceptedResponse> refreshKeywords(
            Authentication authentication,
            @PathVariable Long domainId,
            @RequestBody(required = false) KeywordRefreshRequest request
    ) {
        BatchAcceptedResponse accepted = keywordService.refreshKeywords(authentication, domainId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(accepted);
    }

    @GetMapping("/api/v1/keywords/{keywordId}/history")
    public ResponseEntity<List<RankHistoryResponse>> getHistory(
            Authentication authentication,
            @PathVariable Long keywordId
    ) {
        return ResponseEntity.ok(keywordService.getKeywordHistory(authentication, keywordId));
    }

    @DeleteMapping("/api/v1/keywords/{keywordId}")
    public ResponseEntity<Void> deleteKeyword(
            Authentication authentication,
            @PathVariable Long keywordId
    ) {
        keywordService.deleteKeyword(authentication, keywordId);
        return ResponseEntity.noContent().build();
    }
}
