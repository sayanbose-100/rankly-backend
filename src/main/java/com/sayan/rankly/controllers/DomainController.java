package com.sayan.rankly.controllers;

import com.sayan.rankly.dto.domain.CreateDomainRequest;
import com.sayan.rankly.dto.domain.DomainSummaryResponse;
import com.sayan.rankly.services.DomainService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/domains")
public class DomainController {

    private final DomainService domainService;

    public DomainController(DomainService domainService) {
        this.domainService = domainService;
    }

    @GetMapping
    public ResponseEntity<List<DomainSummaryResponse>> listDomains(Authentication authentication) {
        return ResponseEntity.ok(domainService.getUserDomains(authentication));
    }

    @PostMapping
    public ResponseEntity<DomainSummaryResponse> createDomain(
            Authentication authentication,
            @Valid @RequestBody CreateDomainRequest request
    ) {
        DomainSummaryResponse created = domainService.addDomain(authentication, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{domainId}")
    public ResponseEntity<DomainSummaryResponse> getDomain(
            Authentication authentication,
            @PathVariable Long domainId
    ) {
        return ResponseEntity.ok(domainService.getDomainDetails(authentication, domainId));
    }

    @DeleteMapping("/{domainId}")
    public ResponseEntity<Void> deleteDomain(
            Authentication authentication,
            @PathVariable Long domainId
    ) {
        domainService.deleteDomain(authentication, domainId);
        return ResponseEntity.noContent().build();
    }
}
